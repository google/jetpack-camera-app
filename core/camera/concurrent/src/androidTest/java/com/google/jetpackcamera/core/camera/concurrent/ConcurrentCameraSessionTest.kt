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
import com.google.jetpackcamera.core.camera.InternalCameraApi
import com.google.jetpackcamera.core.camera.PerpetualSessionSettings
import com.google.jetpackcamera.core.camera.TransientSessionSettings
import com.google.jetpackcamera.core.camera.appLensFacing
import com.google.jetpackcamera.core.common.testing.FakeFilePathGenerator
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CameraError
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.TestPattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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

        val hasFrontAndBack =
            cameraProvider.availableCameraInfos.map { it.appLensFacing }.toSet() ==
                setOf(LensFacing.FRONT, LensFacing.BACK)
        if (hasFrontAndBack) {
            val providerWithInjectedPair = ConcurrentCameraSubModeProvider(
                availableConcurrentCameraInfos = { listOf(it.availableCameraInfos) }
            )
            assertThat(
                providerWithInjectedPair.isSupported(
                    context = context,
                    cameraProvider = cameraProvider,
                    cameraInfo = firstCameraInfo
                )
            ).isTrue()
        }
    }

    @Test
    fun sessionBinding_whenNoConcurrentCamerasAvailable_reportsStreamConfigError(): Unit =
        runBlocking {
            val cameraProvider = ProcessCameraProvider.awaitInstance(context)
            assume()
                .withMessage("Device supports concurrent cameras, skip empty-pair error test.")
                .that(cameraProvider.availableConcurrentCameraInfos)
                .isEmpty()

            val sessionContext = createTestSessionContext(cameraProvider)
            val provider = ConcurrentCameraSubModeProvider()
            val sessionJob = launch(Dispatchers.Main) {
                provider.sessionBinding.runSession(sessionContext)
            }

            try {
                val errorState = withTimeout(5_000) {
                    sessionContext.currentCameraState.first {
                        it.cameraError == CameraError.StreamConfigError
                    }
                }
                assertThat(errorState.isCameraRunning).isFalse()
            } finally {
                sessionJob.cancel()
            }
        }

    @Test
    fun runConcurrentCameraSession_withCandidatePair_bindsOrReportsError(): Unit = runBlocking {
        val cameraProvider = ProcessCameraProvider.awaitInstance(context)
        val hasFrontAndBack =
            cameraProvider.availableCameraInfos.map { it.appLensFacing }.toSet() ==
                setOf(LensFacing.FRONT, LensFacing.BACK)
        assume()
            .withMessage("Device does not have both front and back cameras, skip the test.")
            .that(hasFrontAndBack)
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
                    cameraConstraints = null,
                    availableConcurrentCameraInfos = listOf(cameraProvider.availableCameraInfos)
                )
            }
        }

        try {
            if (cameraProvider.availableConcurrentCameraInfos.isEmpty()) {
                val errorState = withTimeout(5_000) {
                    sessionContext.currentCameraState.first {
                        it.cameraError == CameraError.StreamConfigError
                    }
                }
                assertThat(errorState.isCameraRunning).isFalse()
            } else {
                withTimeout(5_000) {
                    sessionContext.surfaceRequests.first { it != null }
                }
            }
        } finally {
            sessionJob.cancel()
        }
    }

    private fun createTestSessionContext(
        cameraProvider: ProcessCameraProvider
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
        transientSettings = MutableStateFlow(
            TransientSessionSettings(
                isAudioEnabled = true,
                deviceRotation = DeviceRotation.Natural,
                flashMode = FlashMode.OFF,
                primaryLensFacing = LensFacing.BACK,
                zoomRatios = emptyMap(),
                testPattern = TestPattern.Off
            )
        )
    )
}
