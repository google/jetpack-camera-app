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
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent.OnVideoRecordError
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent.OnVideoRecorded
import com.google.jetpackcamera.core.camera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.Illuminant
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.SettableConstraintsRepository
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import java.io.File
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
class CameraXCameraSystemTest {

    companion object {
        private const val GENERAL_TIMEOUT_MS = 3_000L
        private const val RECORDING_TIMEOUT_MS = 10_000L
        private const val RECORDING_START_DURATION_MS = 500L
    }

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(APP_REQUIRED_PERMISSIONS).toTypedArray())

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.context
    private val application = context.applicationContext as Application
    private val videosToDelete = mutableSetOf<Uri>()
    private lateinit var cameraSystemScope: CoroutineScope

    @Before
    fun setup() {
        cameraSystemScope = CoroutineScope(Dispatchers.Default)
    }

    @After
    fun tearDown() {
        cameraSystemScope.cancel()
        deleteVideos()
    }

    @Test
    fun canRecordVideo(): Unit = runBlocking {
        // Arrange.
        val cameraSystem = createAndInitCameraXCameraSystem()
        cameraSystem.runCameraOnMain()

        // Act.
        val recordingComplete = CompletableDeferred<Unit>()
        cameraSystem.startRecording {
            when (it) {
                is OnVideoRecorded -> {
                    recordingComplete.complete(Unit)
                }
                is OnVideoRecordError -> recordingComplete.completeExceptionally(it.error)
            }
        }

        cameraSystem.stopVideoRecording()

        // Assert.
        recordingComplete.await()
    }

    @Test
    fun recordVideoWithFlashModeOn_shouldEnableTorch(): Unit = runBlocking {
        // Arrange.
        val lensFacing = LensFacing.BACK
        val constraintsRepository = SettableConstraintsRepositoryImpl()
        val cameraSystem = createAndInitCameraXCameraSystem(
            constraintsRepository = constraintsRepository
        )
        assumeTrue("No flash unit, skip the test.", constraintsRepository.hasFlashUnit(lensFacing))
        cameraSystem.runCameraOnMain()

        // Arrange: Create a ReceiveChannel to observe the torch enabled state.
        val torchEnabled: ReceiveChannel<Boolean> = cameraSystem.getCurrentCameraState()
            .map { it.isTorchEnabled }
            .produceIn(this)

        // Assert: The initial torch enabled should be false.
        torchEnabled.awaitValue(false)

        // Act: Start recording with FlashMode.ON
        val recordingComplete = CompletableDeferred<Unit>()
        cameraSystem.setFlashMode(FlashMode.ON)
        cameraSystem.startRecording {
            when (it) {
                is OnVideoRecorded -> {
                    recordingComplete.complete(Unit)
                }
                is OnVideoRecordError -> recordingComplete.completeExceptionally(it.error)
            }
        }

        // Assert: Torch enabled transitions to true.
        torchEnabled.awaitValue(true)

        cameraSystem.stopVideoRecording()

        // Assert: Torch enabled transitions to false.
        torchEnabled.awaitValue(false)

        // Clean-up.
        recordingComplete.await()
        torchEnabled.cancel()
    }

    private suspend fun createAndInitCameraXCameraSystem(
        appSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
        constraintsRepository: SettableConstraintsRepository = SettableConstraintsRepositoryImpl()
    ) = CameraXCameraSystem(
        application = application,
        defaultDispatcher = Dispatchers.Default,
        iODispatcher = Dispatchers.IO,
        constraintsRepository = constraintsRepository,
        availabilityCheckers = emptyMap(),
        effectProviders = emptyMap()
    ).apply {
        initialize(appSettings) {}
        providePreviewSurface()
    }

    private suspend fun <T> ReceiveChannel<T>.awaitValue(
        expectedValue: T,
        timeoutMs: Long = GENERAL_TIMEOUT_MS
    ) = withTimeoutOrNull(timeoutMs) {
        for (value in this@awaitValue) {
            if (value == expectedValue) return@withTimeoutOrNull
        }
    } ?: fail("Timeout while waiting for expected value: $expectedValue")

    private suspend fun CameraXCameraSystem.startRecording(
        onVideoRecord: (OnVideoRecordEvent) -> Unit
    ) {
        // Start recording
        startVideoRecording(SaveLocation.Default) { event ->
            // Track files that need to be deleted
            if (event is OnVideoRecorded) {
                val videoUri = event.savedUri
                if (videoUri != Uri.EMPTY) {
                    videosToDelete.add(videoUri)
                }
            }

            // Forward event to provided callback
            onVideoRecord(event)
        }

        // Wait for recording duration to reach start duration to consider it started
        withTimeout(RECORDING_TIMEOUT_MS) {
            getCurrentCameraState().transform { cameraState ->
                (cameraState.videoRecordingState as? VideoRecordingState.Active)?.let {
                    emit(
                        it.elapsedTimeNanos.toDuration(DurationUnit.NANOSECONDS).inWholeMilliseconds
                    )
                }
            }.first { elapsedTimeMs ->
                elapsedTimeMs >= RECORDING_START_DURATION_MS
            }
        }
    }

    private fun CameraXCameraSystem.providePreviewSurface() {
        cameraSystemScope.launch {
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

    private fun CameraXCameraSystem.runCameraOnMain() {
        cameraSystemScope.launch(Dispatchers.Main) { runCamera() }
        instrumentation.waitForIdleSync()
    }

    private suspend fun ConstraintsRepository.hasFlashUnit(lensFacing: LensFacing): Boolean =
        Illuminant.FLASH_UNIT in
            systemConstraints.first()!!.perLensConstraints[lensFacing]!!.supportedIlluminants

    private fun deleteVideos() {
        for (uri in videosToDelete) {
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (_: RuntimeException) {
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
