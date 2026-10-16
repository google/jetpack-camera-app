/*
 * Copyright (C) 2026 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:kotlin.OptIn(InternalCameraApi::class)

package com.google.jetpackcamera.core.camera.concurrent

import android.Manifest
import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.core.camera.CameraSessionContext
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.CaptureSubModeState
import com.google.jetpackcamera.core.camera.InternalCameraApi
import com.google.jetpackcamera.core.camera.PerpetualSessionSettings
import com.google.jetpackcamera.core.camera.TransientSessionSettings
import com.google.jetpackcamera.core.camera.appLensFacing
import com.google.jetpackcamera.core.camera.runWith
import com.google.jetpackcamera.core.common.testing.FakeFilePathGenerator
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.TestPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class ConcurrentCameraSessionTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule = GrantPermissionRule.grant(
        Manifest.permission.CAMERA,
        Manifest.permission.RECORD_AUDIO
    )

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun isSupported_matchesAvailableConcurrentCameraInfos(): Unit = runBlocking {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val firstCameraInfo = cameraProvider.availableCameraInfos.firstOrNull()
        assume()
            .withMessage("No cameras available on device, skip the test.")
            .that(firstCameraInfo != null)
            .isTrue()

        val expectedSupported = cameraProvider.availableConcurrentCameraInfos.any { group ->
            group.map { it.appLensFacing }.toSet() == setOf(LensFacing.FRONT, LensFacing.BACK)
        }

        val provider = ConcurrentCameraSubModeProvider()
        val actualSupported = provider.isSupported(
            context = context,
            cameraProvider = cameraProvider,
            cameraInfo = firstCameraInfo!!
        )

        assertThat(actualSupported).isEqualTo(expectedSupported)

        val oppositeCameraInfo = fakeOppositeLensCameraInfo(firstCameraInfo)
        val providerWithInjectedPair = ConcurrentCameraSubModeProvider(
            availableConcurrentCameraInfos = {
                listOf(listOf(firstCameraInfo, oppositeCameraInfo))
            }
        )
        assertThat(
            providerWithInjectedPair.isSupported(
                context = context,
                cameraProvider = cameraProvider,
                cameraInfo = firstCameraInfo
            )
        ).isTrue()

        val providerWithSingleLensGroup = ConcurrentCameraSubModeProvider(
            availableConcurrentCameraInfos = { listOf(listOf(firstCameraInfo)) }
        )
        assertThat(
            providerWithSingleLensGroup.isSupported(
                context = context,
                cameraProvider = cameraProvider,
                cameraInfo = firstCameraInfo
            )
        ).isFalse()
    }

    @Test
    fun sessionBinding_whenNoConcurrentCamerasAvailable_reportsSubModeError(): Unit =
        runBlocking {
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            val sessionContext = createTestSessionContext(cameraProvider)
            val provider = ConcurrentCameraSubModeProvider(
                availableConcurrentCameraInfos = { emptyList() }
            )
            val sessionJob = launch(Dispatchers.Main) {
                provider.sessionBinding.runSession(sessionContext)
            }

            try {
                val errorState = withTimeout(5_000) {
                    sessionContext.currentCameraState.first {
                        it.captureSubModeState is CaptureSubModeState.Error
                    }
                }
                assertThat(errorState.isCameraRunning).isFalse()
                assertThat(errorState.cameraError).isNull()
                assertThat((errorState.captureSubModeState as CaptureSubModeState.Error).subModeId)
                    .isEqualTo(CONCURRENT_SUB_MODE_ID)
            } finally {
                withContext(NonCancellable) {
                    sessionJob.cancelAndJoin()
                    withContext(Dispatchers.Main) {
                        cameraProvider.unbindAll()
                    }
                }
            }
        }

    @Test
    fun runConcurrentCameraSession_withDeviceConcurrentPair_bindsAndRequestsSurface(): Unit =
        runBlocking {
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            val hasConcurrentFrontAndBack =
                cameraProvider.availableConcurrentCameraInfos.any { group ->
                    group.map { it.appLensFacing }.toSet() ==
                        setOf(LensFacing.FRONT, LensFacing.BACK)
                }
            assume()
                .withMessage("Device does not support concurrent front+back cameras, skip test.")
                .that(hasConcurrentFrontAndBack)
                .isTrue()

            val sessionContext = createTestSessionContext(cameraProvider)
            val sessionJob = launch(Dispatchers.Main) {
                with(sessionContext) {
                    runConcurrentCameraSession(
                        sessionSettings = PerpetualSessionSettings.CustomSession(
                            subModeKey = ConcurrentCameraSubModeKey,
                            aspectRatio = AspectRatio.NINE_SIXTEEN,
                            captureMode = CaptureMode.VIDEO_ONLY
                        )
                    )
                }
            }

            try {
                withTimeout(5_000) {
                    sessionContext.surfaceRequests.first { it != null }
                }
            } finally {
                withContext(NonCancellable) {
                    sessionJob.cancelAndJoin()
                    withContext(Dispatchers.Main) {
                        cameraProvider.unbindAll()
                    }
                }
            }
        }

    @SuppressLint("Recycle")
    @Test
    fun runConcurrentCameraSession_whenLensFlipped_updatesStateAndSurface(): Unit = runBlocking {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val backCameraInfo = cameraProvider.availableCameraInfos.firstOrNull {
            it.appLensFacing == LensFacing.BACK
        }
        val frontCameraInfo = cameraProvider.availableCameraInfos.firstOrNull {
            it.appLensFacing == LensFacing.FRONT
        }
        assume()
            .withMessage("Both FRONT and BACK cameras required on device, skip the test.")
            .that(backCameraInfo != null && frontCameraInfo != null)
            .isTrue()

        val boundPrimaryLensFacings = mutableListOf<LensFacing>()
        val transientSettingsFlow = MutableStateFlow<TransientSessionSettings?>(
            TransientSessionSettings(
                isAudioEnabled = true,
                deviceRotation = DeviceRotation.Natural,
                flashMode = FlashMode.OFF,
                primaryLensFacing = LensFacing.BACK,
                zoomRatios = emptyMap(),
                testPattern = TestPattern.Off
            )
        )
        val sessionContext = createTestSessionContext(
            cameraProvider,
            transientSettings = transientSettingsFlow
        )
        val sessionJob = launch(Dispatchers.Main) {
            with(sessionContext) {
                runConcurrentCameraSession(
                    sessionSettings = PerpetualSessionSettings.CustomSession(
                        subModeKey = ConcurrentCameraSubModeKey,
                        aspectRatio = AspectRatio.NINE_SIXTEEN,
                        captureMode = CaptureMode.VIDEO_ONLY
                    ),
                    availableConcurrentCameraInfos = listOf(
                        listOf(backCameraInfo!!, frontCameraInfo!!)
                    ),
                    bindConcurrentCameras = { cameraConfigs, useCaseGroup, block ->
                        val primarySelector = cameraConfigs[0].first
                        val secondarySelector = cameraConfigs[1].first
                        val secondaryInfo = secondarySelector
                            .filter(listOf(backCameraInfo, frontCameraInfo))
                            .first()
                        cameraProvider.runWith(
                            primarySelector,
                            useCaseGroup
                        ) { primaryCamera ->
                            boundPrimaryLensFacings += primaryCamera.cameraInfo.appLensFacing
                            block(
                                listOf(
                                    primaryCamera,
                                    fakeOppositeLensCamera(primaryCamera, secondaryInfo)
                                )
                            )
                        }
                    }
                )
            }
        }

        try {
            val initialSurfaceRequest = withTimeout(5_000) {
                sessionContext.surfaceRequests.first { it != null }!!
            }
            provideSurfaceForTest(initialSurfaceRequest)
            val runningState = withTimeout(10_000) {
                sessionContext.currentCameraState.first { it.isCameraRunning }
            }
            assertThat(runningState.cameraError).isNull()
            assertThat(boundPrimaryLensFacings).containsExactly(LensFacing.BACK)

            // Flip primaryLensFacing mid-session and verify a new SurfaceRequest is emitted.
            sessionContext.surfaceRequests.value = null
            transientSettingsFlow.value = transientSettingsFlow.value?.copy(
                primaryLensFacing = LensFacing.FRONT
            )
            val flippedSurfaceRequest = withTimeout(5_000) {
                sessionContext.surfaceRequests.first {
                    it != null && it !== initialSurfaceRequest
                }!!
            }
            provideSurfaceForTest(flippedSurfaceRequest)
            val flippedRunningState = withTimeout(10_000) {
                sessionContext.currentCameraState.first { it.isCameraRunning }
            }
            assertThat(flippedRunningState.cameraError).isNull()
            assertThat(boundPrimaryLensFacings)
                .containsExactly(LensFacing.BACK, LensFacing.FRONT)
                .inOrder()
        } finally {
            withContext(NonCancellable) {
                sessionJob.cancelAndJoin()
                withContext(Dispatchers.Main) {
                    cameraProvider.unbindAll()
                }
            }
        }
    }

    @Test
    fun runConcurrentCameraSession_whenBindingFails_reportsSubModeError(): Unit = runBlocking {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val backCameraInfo = cameraProvider.availableCameraInfos.firstOrNull {
            it.appLensFacing == LensFacing.BACK
        }
        val frontCameraInfo = cameraProvider.availableCameraInfos.firstOrNull {
            it.appLensFacing == LensFacing.FRONT
        }
        assume()
            .withMessage("Both FRONT and BACK cameras required on device, skip the test.")
            .that(backCameraInfo != null && frontCameraInfo != null)
            .isTrue()

        val sessionContext = createTestSessionContext(cameraProvider)
        val sessionJob = launch(Dispatchers.Main) {
            with(sessionContext) {
                runConcurrentCameraSession(
                    sessionSettings = PerpetualSessionSettings.CustomSession(
                        subModeKey = ConcurrentCameraSubModeKey,
                        aspectRatio = AspectRatio.NINE_SIXTEEN,
                        captureMode = CaptureMode.VIDEO_ONLY
                    ),
                    availableConcurrentCameraInfos = listOf(
                        listOf(backCameraInfo!!, frontCameraInfo!!)
                    ),
                    bindConcurrentCameras = { _, _, _ ->
                        throw IllegalArgumentException("Unsupported stream combination")
                    }
                )
            }
        }

        try {
            val errorState = withTimeout(5_000) {
                sessionContext.currentCameraState.first {
                    it.captureSubModeState is CaptureSubModeState.Error
                }
            }
            assertThat(errorState.isCameraRunning).isFalse()
            assertThat(errorState.cameraError).isNull()
            assertThat((errorState.captureSubModeState as CaptureSubModeState.Error).subModeId)
                .isEqualTo(CONCURRENT_SUB_MODE_ID)
        } finally {
            withContext(NonCancellable) {
                sessionJob.cancelAndJoin()
                withContext(Dispatchers.Main) {
                    cameraProvider.unbindAll()
                }
            }
        }
    }

    @SuppressLint("Recycle")
    private fun provideSurfaceForTest(surfaceRequest: androidx.camera.core.SurfaceRequest) {
        val surfaceTexture = SurfaceTexture(0).apply {
            setDefaultBufferSize(
                surfaceRequest.resolution.width,
                surfaceRequest.resolution.height
            )
            detachFromGLContext()
        }
        val surface = Surface(surfaceTexture)
        surfaceRequest.provideSurface(surface, Runnable::run) {
            surface.release()
            surfaceTexture.release()
        }
    }

    private fun fakeOppositeLensCamera(
        delegate: androidx.camera.core.Camera,
        oppositeInfo: CameraInfo
    ): androidx.camera.core.Camera = object : androidx.camera.core.Camera by delegate {
        override fun getCameraInfo(): CameraInfo = oppositeInfo
    }

    private fun fakeOppositeLensCameraInfo(delegate: CameraInfo): CameraInfo =
        object : CameraInfo by delegate {
            override fun getLensFacing(): Int =
                if (delegate.lensFacing == CameraSelector.LENS_FACING_FRONT) {
                    CameraSelector.LENS_FACING_BACK
                } else {
                    CameraSelector.LENS_FACING_FRONT
                }
        }

    private fun createTestSessionContext(
        cameraProvider: ProcessCameraProvider,
        primaryLensFacing: LensFacing = LensFacing.BACK,
        transientSettings: MutableStateFlow<TransientSessionSettings?> = MutableStateFlow(
            TransientSessionSettings(
                isAudioEnabled = true,
                deviceRotation = DeviceRotation.Natural,
                flashMode = FlashMode.OFF,
                primaryLensFacing = primaryLensFacing,
                zoomRatios = emptyMap(),
                testPattern = TestPattern.Off
            )
        )
    ): CameraSessionContext = CameraSessionContext(
        context = context,
        cameraProvider = cameraProvider,
        backgroundDispatcher = Dispatchers.Default,
        screenFlashEvents = Channel(Channel.UNLIMITED),
        filePathGenerator = FakeFilePathGenerator(),
        focusMeteringEvents = Channel(Channel.UNLIMITED),
        videoCaptureControlEvents = Channel(Channel.UNLIMITED),
        currentCameraState = MutableStateFlow(CameraState()),
        surfaceRequests = MutableStateFlow(null),
        transientSettings = transientSettings,
        sessionSettings = PerpetualSessionSettings.CustomSession(
            subModeKey = ConcurrentCameraSubModeKey,
            aspectRatio = AspectRatio.NINE_SIXTEEN,
            captureMode = CaptureMode.VIDEO_ONLY
        )
    )
}
