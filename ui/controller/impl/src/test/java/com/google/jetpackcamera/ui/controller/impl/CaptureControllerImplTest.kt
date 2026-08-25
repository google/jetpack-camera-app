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
package com.google.jetpackcamera.ui.controller.impl

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.ImageCaptureEvent
import com.google.jetpackcamera.model.IntProgress
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.SaveMode
import com.google.jetpackcamera.model.VideoCaptureEvent
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.ui.controller.ImageWellController
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CaptureControllerImplTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    private val fakeCameraSystem = FakeCameraSystem()
    private val testCameraSystem = TestCameraSystem(fakeCameraSystem)
    private val trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState())
    private val captureEvents = Channel<CaptureEvent>(capacity = Channel.UNLIMITED)
    private val fakeImageWellController = FakeImageWellController()
    private lateinit var contentResolver: ContentResolver

    private val testImageUri = Uri.parse("content://media/external/images/media/1")
    private val testVideoUri = Uri.parse("content://media/external/video/media/2")
    private val testCacheDir = Uri.parse("file:///tmp/cache")

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        contentResolver = context.contentResolver
        testCameraSystem.savedImageUri = testImageUri
        testCameraSystem.savedVideoUri = testVideoUri
    }

    private fun createCaptureController(
        saveMode: SaveMode = SaveMode.Immediate,
        externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard,
        externalCapturesCallback: () -> Pair<SaveLocation, IntProgress?> = {
            Pair(SaveLocation.Default, null)
        },
        imageWellController: ImageWellController? = fakeImageWellController,
        onImageCached: ((Uri) -> Unit)? = null,
        onVideoCached: ((Uri) -> Unit)? = null
    ): CaptureControllerImpl {
        return CaptureControllerImpl(
            trackedCaptureUiState = trackedCaptureUiState,
            cameraSystem = testCameraSystem,
            saveMode = saveMode,
            externalCaptureMode = externalCaptureMode,
            externalCapturesCallback = externalCapturesCallback,
            captureEvents = captureEvents,
            imageWellController = imageWellController,
            onImageCached = onImageCached,
            onVideoCached = onVideoCached,
            coroutineContext = testScope.coroutineContext
        )
    }

    @Test
    fun captureImage_standardSaveLocation_updatesImageWellAndEmitsSingleImageSaved() =
        runCameraTest {
            var imageCachedUri: Uri? = null
            val controller = createCaptureController(
                saveMode = SaveMode.Immediate,
                onImageCached = { imageCachedUri = it }
            )

            controller.captureImage(contentResolver)
            advanceUntilIdle()

            assertThat(fakeImageWellController.updateLastCapturedMediaCallCount).isEqualTo(1)
            assertThat(imageCachedUri).isNull()
            val event = captureEvents.receive()
            assertThat(event).isInstanceOf(ImageCaptureEvent.SingleImageSaved::class.java)
            assertThat(
                (event as ImageCaptureEvent.SingleImageSaved).capturedUri
            ).isEqualTo(testImageUri)
        }

    @Test
    fun captureImage_cacheSaveLocation_invokesOnImageCachedAndEmitsSingleImageCached() =
        runCameraTest {
            var imageCachedUri: Uri? = null
            val controller = createCaptureController(
                saveMode = SaveMode.CacheAndReview(cacheDir = testCacheDir),
                onImageCached = { imageCachedUri = it }
            )

            controller.captureImage(contentResolver)
            advanceUntilIdle()

            assertThat(fakeImageWellController.updateLastCapturedMediaCallCount).isEqualTo(0)
            assertThat(imageCachedUri).isEqualTo(testImageUri)
            val event = captureEvents.receive()
            assertThat(event).isInstanceOf(ImageCaptureEvent.SingleImageCached::class.java)
            assertThat(
                (event as ImageCaptureEvent.SingleImageCached).capturedUri
            ).isEqualTo(testImageUri)
        }

    @Test
    fun captureImage_nullOptionalDependencies_succeedsWithoutError() = runCameraTest {
        val controller = createCaptureController(
            saveMode = SaveMode.Immediate,
            imageWellController = null,
            onImageCached = null
        )

        controller.captureImage(contentResolver)
        advanceUntilIdle()

        val event = captureEvents.receive()
        assertThat(event).isInstanceOf(ImageCaptureEvent.SingleImageSaved::class.java)
    }

    @Test
    fun captureImage_cacheModeWithNullOnImageCached_succeedsWithoutError() = runCameraTest {
        val controller = createCaptureController(
            saveMode = SaveMode.CacheAndReview(cacheDir = testCacheDir),
            imageWellController = null,
            onImageCached = null
        )

        controller.captureImage(contentResolver)
        advanceUntilIdle()

        val event = captureEvents.receive()
        assertThat(event).isInstanceOf(ImageCaptureEvent.SingleImageCached::class.java)
    }

    @Test
    fun captureImage_externalVideoCaptureMode_emitsImageCaptureExternalUnsupported() =
        runCameraTest {
            val controller = createCaptureController(
                externalCaptureMode = ExternalCaptureMode.VideoCapture
            )

            controller.captureImage(contentResolver)
            advanceUntilIdle()

            val event = captureEvents.receive()
            assertThat(event).isEqualTo(ImageCaptureEvent.ImageCaptureExternalUnsupported)
        }

    @Test
    fun captureImage_multipleImageCaptureMode_emitsSequentialImageSaved() = runCameraTest {
        val progress = IntProgress(1, 1..3)
        val controller = createCaptureController(
            externalCaptureMode = ExternalCaptureMode.MultipleImageCapture,
            externalCapturesCallback = {
                Pair(SaveLocation.Explicit(testImageUri), progress)
            }
        )

        controller.captureImage(contentResolver)
        advanceUntilIdle()

        val event = captureEvents.receive()
        assertThat(event).isInstanceOf(ImageCaptureEvent.SequentialImageSaved::class.java)
        val sequentialEvent = event as ImageCaptureEvent.SequentialImageSaved
        assertThat(sequentialEvent.capturedUri).isEqualTo(testImageUri)
        assertThat(sequentialEvent.progress).isEqualTo(progress)
    }

    @Test
    fun startVideoRecording_standardSaveLocation_updatesImageWellAndEmitsVideoSaved() =
        runCameraTest {
            var videoCachedUri: Uri? = null
            val controller = createCaptureController(
                saveMode = SaveMode.Immediate,
                onVideoCached = { videoCachedUri = it }
            )

            controller.startVideoRecording()
            advanceUntilIdle()

            assertThat(fakeImageWellController.updateLastCapturedMediaCallCount).isEqualTo(1)
            assertThat(videoCachedUri).isNull()
            val event = captureEvents.receive()
            assertThat(event).isInstanceOf(VideoCaptureEvent.VideoSaved::class.java)
            assertThat((event as VideoCaptureEvent.VideoSaved).savedUri).isEqualTo(testVideoUri)
        }

    @Test
    fun startVideoRecording_cacheSaveLocation_invokesOnVideoCachedAndEmitsVideoCached() =
        runCameraTest {
            var videoCachedUri: Uri? = null
            val controller = createCaptureController(
                saveMode = SaveMode.CacheAndReview(cacheDir = testCacheDir),
                onVideoCached = { videoCachedUri = it }
            )

            controller.startVideoRecording()
            advanceUntilIdle()

            assertThat(fakeImageWellController.updateLastCapturedMediaCallCount).isEqualTo(0)
            assertThat(videoCachedUri).isEqualTo(testVideoUri)
            val event = captureEvents.receive()
            assertThat(event).isInstanceOf(VideoCaptureEvent.VideoCached::class.java)
            assertThat((event as VideoCaptureEvent.VideoCached).capturedUri).isEqualTo(testVideoUri)
        }

    @Test
    fun startVideoRecording_nullOptionalDependencies_succeedsWithoutError() = runCameraTest {
        val controller = createCaptureController(
            saveMode = SaveMode.Immediate,
            imageWellController = null,
            onVideoCached = null
        )

        controller.startVideoRecording()
        advanceUntilIdle()

        val event = captureEvents.receive()
        assertThat(event).isInstanceOf(VideoCaptureEvent.VideoSaved::class.java)
    }

    @Test
    fun startVideoRecording_externalImageCaptureMode_emitsVideoCaptureExternalUnsupported() =
        runCameraTest {
            val controller = createCaptureController(
                externalCaptureMode = ExternalCaptureMode.ImageCapture
            )

            controller.startVideoRecording()
            advanceUntilIdle()

            val event = captureEvents.receive()
            assertThat(event).isEqualTo(VideoCaptureEvent.VideoCaptureExternalUnsupported)
        }

    @Test
    fun startVideoRecording_onError_emitsVideoCaptureError() = runCameraTest {
        val expectedError = RuntimeException("Recording failed")
        testCameraSystem.videoRecordError = expectedError

        val controller = createCaptureController()
        controller.startVideoRecording()
        advanceUntilIdle()

        val event = captureEvents.receive()
        assertThat(event).isInstanceOf(VideoCaptureEvent.VideoCaptureError::class.java)
        assertThat((event as VideoCaptureEvent.VideoCaptureError).error).isEqualTo(expectedError)
    }

    @Test
    fun setLockedRecording_updatesTrackedCaptureUiState() {
        val controller = createCaptureController()
        controller.setLockedRecording(true)
        assertThat(trackedCaptureUiState.value.isRecordingLocked).isTrue()

        controller.setLockedRecording(false)
        assertThat(trackedCaptureUiState.value.isRecordingLocked).isFalse()
    }

    @Test
    fun setPaused_pausesAndResumesRecording() = runTest(testDispatcher) {
        val controller = createCaptureController()

        controller.setPaused(true)
        advanceUntilIdle()
        assertThat(fakeCameraSystem.isRecordingPaused).isTrue()

        controller.setPaused(false)
        advanceUntilIdle()
        assertThat(fakeCameraSystem.isRecordingPaused).isFalse()
    }

    private fun runCameraTest(testBody: suspend TestScope.() -> Unit) = runTest(testDispatcher) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            fakeCameraSystem.initialize(DEFAULT_CAMERA_APP_SETTINGS) {}
            fakeCameraSystem.runCamera()
        }
        testBody()
    }
}

private class TestCameraSystem(private val delegate: FakeCameraSystem) :
    CameraSystem by delegate {
    var savedImageUri: Uri? = null
    var savedVideoUri: Uri = Uri.EMPTY
    var videoRecordError: Throwable? = null

    override suspend fun takePicture(
        contentResolver: ContentResolver,
        saveLocation: SaveLocation,
        onCaptureStarted: () -> Unit
    ): ImageCapture.OutputFileResults {
        delegate.takePicture(onCaptureStarted)
        return ImageCapture.OutputFileResults(savedImageUri)
    }

    override suspend fun startVideoRecording(
        saveLocation: SaveLocation,
        onVideoRecord: (OnVideoRecordEvent) -> Unit
    ) {
        delegate.startVideoRecording(saveLocation, onVideoRecord)
        val error = videoRecordError
        if (error != null) {
            onVideoRecord(OnVideoRecordEvent.OnVideoRecordError(error))
        } else {
            onVideoRecord(OnVideoRecordEvent.OnVideoRecorded(savedVideoUri))
        }
    }
}

private class FakeImageWellController : ImageWellController {
    var updateLastCapturedMediaCallCount = 0
    var lastMediaDescriptor: MediaDescriptor? = null

    override fun imageWellToRepository(mediaDescriptor: MediaDescriptor) {
        lastMediaDescriptor = mediaDescriptor
    }

    override fun updateLastCapturedMedia() {
        updateLastCapturedMediaCallCount++
    }
}
