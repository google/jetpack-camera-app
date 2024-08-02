/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.core.camera

import android.app.Application
import android.content.ContentResolver
import android.graphics.SurfaceTexture
import android.net.Uri
import android.view.Surface
import androidx.concurrent.futures.DirectExecutor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.jetpackcamera.core.camera.CameraUseCase.OnVideoRecordEvent.OnVideoRecordError
import com.google.jetpackcamera.core.camera.CameraUseCase.OnVideoRecordEvent.OnVideoRecordStatus
import com.google.jetpackcamera.core.camera.CameraUseCase.OnVideoRecordEvent.OnVideoRecorded
import com.google.jetpackcamera.core.camera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.SettableConstraintsRepository
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Assert.fail
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraXCameraUseCaseTest {

    companion object {
        private const val STATUS_VERIFY_COUNT = 5
        private const val GENERAL_TIMEOUT_MS = 3_000L
        private const val STATUS_VERIFY_TIMEOUT_MS = 10_000L
    }

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(APP_REQUIRED_PERMISSIONS).toTypedArray())

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.context
    private val application = context.applicationContext as Application
    private val videosToDelete = mutableSetOf<Uri>()
    private lateinit var useCaseScope: CoroutineScope

    @Before
    fun setup() {
        useCaseScope = CoroutineScope(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        useCaseScope.cancel()
        deleteVideos()
    }

    @Test
    fun canRecordVideo(): Unit = runBlocking {
        // Arrange.
        val cameraUseCase = createAndInitCameraXUseCase()
        cameraUseCase.runCameraOnMain()

        // Act.
        val recordEvent = cameraUseCase.startRecordingAndGetEvents()

        // Assert.
        recordEvent.onRecordStatus.await(STATUS_VERIFY_TIMEOUT_MS)

        // Act.
        cameraUseCase.stopVideoRecording()

        // Assert.
        recordEvent.onRecorded.await()
    }

    @Test
    fun recordVideoWithFlashModeOn_shouldEnableTorch(): Unit = runBlocking {
        // Arrange.
        val lensFacing = LensFacing.BACK
        val constraintsRepository = SettableConstraintsRepositoryImpl()
        val cameraUseCase = createAndInitCameraXUseCase(
            constraintsRepository = constraintsRepository
        )
        assumeTrue("No flash unit, skip the test.", constraintsRepository.hasFlashUnit(lensFacing))
        cameraUseCase.runCameraOnMain()

        // Arrange: Create a ReceiveChannel to observe the torch enabled state.
        val torchEnabled: ReceiveChannel<Boolean> = cameraUseCase.getCurrentCameraState()
            .map { it.torchEnabled }
            .produceIn(this)

        // Assert: The initial torch enabled should be false.
        torchEnabled.awaitValue(false)

        // Act: Start recording with FlashMode.ON
        cameraUseCase.setFlashMode(FlashMode.ON)
        val recordEvent = cameraUseCase.startRecordingAndGetEvents()

        // Assert: Torch enabled transitions to true.
        torchEnabled.awaitValue(true)

        // Act: Ensure enough data is received and stop recording.
        recordEvent.onRecordStatus.await(STATUS_VERIFY_TIMEOUT_MS)
        cameraUseCase.stopVideoRecording()

        // Assert: Torch enabled transitions to false.
        torchEnabled.awaitValue(false)

        // Clean-up.
        torchEnabled.cancel()
    }

    private suspend fun createAndInitCameraXUseCase(
        appSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
        constraintsRepository: SettableConstraintsRepository = SettableConstraintsRepositoryImpl()
    ) = CameraXCameraUseCase(
        application,
        useCaseScope,
        Dispatchers.Default,
        constraintsRepository
    ).apply {
        initialize(appSettings, false)
        providePreviewSurface()
    }

    private data class RecordEvents(
        val onRecorded: CompletableDeferred<Unit>,
        val onRecordStatus: CompletableDeferred<Unit>
    )

    private suspend fun CompletableDeferred<*>.await(timeoutMs: Long = GENERAL_TIMEOUT_MS) =
        withTimeoutOrNull(timeoutMs) {
            await()
            Unit
        } ?: fail("Timeout while waiting for the Deferred to complete")

    private suspend fun <T> ReceiveChannel<T>.awaitValue(
        expectedValue: T,
        timeoutMs: Long = GENERAL_TIMEOUT_MS
    ) = withTimeoutOrNull(timeoutMs) {
        for (value in this@awaitValue) {
            if (value == expectedValue) return@withTimeoutOrNull
        }
    } ?: fail("Timeout while waiting for expected value: $expectedValue")

    private suspend fun CameraXCameraUseCase.startRecordingAndGetEvents(
        statusVerifyCount: Int = STATUS_VERIFY_COUNT
    ): RecordEvents {
        val onRecorded = CompletableDeferred<Unit>()
        val onRecordStatus = CompletableDeferred<Unit>()
        var statusCount = 0
        startVideoRecording {
            when (it) {
                is OnVideoRecorded -> {
                    val videoUri = it.savedUri
                    if (videoUri != Uri.EMPTY) {
                        videosToDelete.add(videoUri)
                    }
                    onRecorded.complete(Unit)
                }
                is OnVideoRecordError -> onRecorded.complete(Unit)
                is OnVideoRecordStatus -> {
                    statusCount++
                    if (statusCount == statusVerifyCount) {
                        onRecordStatus.complete(Unit)
                    }
                }
            }
        }
        return RecordEvents(onRecorded, onRecordStatus)
    }

    private fun CameraXCameraUseCase.providePreviewSurface() {
        useCaseScope.launch {
            getSurfaceRequest().filterNotNull().first().let { surfaceRequest ->
                val surfaceTexture = SurfaceTexture(0)
                surfaceTexture.setDefaultBufferSize(640, 480)
                val surface = Surface(surfaceTexture)
                surfaceRequest.provideSurface(surface, DirectExecutor.INSTANCE) {
                    surface.release()
                    surfaceTexture.release()
                }
            }
        }
    }

    private suspend fun CameraXCameraUseCase.runCameraOnMain() {
        useCaseScope.launch(Dispatchers.Main) { runCamera() }
        instrumentation.waitForIdleSync()
    }

    private suspend fun ConstraintsRepository.hasFlashUnit(lensFacing: LensFacing): Boolean =
        systemConstraints.first()!!.perLensConstraints[lensFacing]!!.hasFlashUnit

    private fun deleteVideos() {
        for (uri in videosToDelete) {
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (e: RuntimeException) {
                        // Ignore any exception.
                    }
                }
                ContentResolver.SCHEME_FILE -> {
                    File(uri.path!!).delete()
                }
            }
        }
    }
}
