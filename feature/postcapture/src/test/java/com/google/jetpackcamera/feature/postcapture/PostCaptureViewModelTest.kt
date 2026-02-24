/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.feature.postcapture

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.data.media.FakeMediaRepository
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_DELETE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_SAVE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_DELETE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_SAVE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS
import com.google.jetpackcamera.ui.uistate.capture.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.postcapture.MediaViewerUiState
import com.google.jetpackcamera.ui.uistate.postcapture.PostCaptureUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PostCaptureViewModelTest {

    private val testContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testDispatcher = StandardTestDispatcher()
    private val testExternalScope = TestScope(testDispatcher)

    private lateinit var mediaRepository: FakeMediaRepository
    private lateinit var viewModel: PostCaptureViewModel

    // --- SHARED TEST DATA ---
    val testThumbnail: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

    private val testImageUri = Uri.parse("file:///test.jpg")
    private val testCacheImageDesc = MediaDescriptor.Content.Image(
        testImageUri,
        testThumbnail,
        true
    )

    private val testImageDesc = MediaDescriptor.Content.Image(testImageUri, testThumbnail)
    private val testImageMedia = Media.Image(testThumbnail)

    private val testVideoUri = Uri.parse("file:///test.mp4")
    private val testVideoDesc = MediaDescriptor.Content.Video(testVideoUri, testThumbnail)
    private val testCacheVideoDesc = MediaDescriptor.Content.Video(
        testVideoUri,
        testThumbnail,
        true
    )
    private val testVideoMedia = Media.Video(testVideoUri)

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        mediaRepository = FakeMediaRepository()

        // Configure default behavior
        mediaRepository.loadHandler = { mediaDescriptor ->
            when (mediaDescriptor) {
                is MediaDescriptor.Content.Video -> testVideoMedia
                is MediaDescriptor.Content.Image -> testImageMedia
                else -> Media.None
            }
        }
        mediaRepository.saveToMediaStoreHandler = { mediaDescriptor ->
            when (mediaDescriptor) {
                is MediaDescriptor.Content.Image -> testImageUri
                is MediaDescriptor.Content.Video -> testVideoUri
            }
        }
        mediaRepository.deleteMediaHandler = { true }

        viewModel = PostCaptureViewModel(
            mediaRepository = mediaRepository,
            context = testContext
        )
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        testExternalScope.cancel()
    }

    @Test
    fun onShareCurrentMedia_emitsShareMediaEvent() = runTest(testDispatcher) {
        mediaRepository.setCurrentMedia(testImageDesc)
        advanceUntilIdle()

        viewModel.onShareCurrentMedia()
        advanceUntilIdle()

        val receivedEvent = viewModel.uiEvents.receive()

        assertThat(
            receivedEvent
        ).isInstanceOf(PostCaptureViewModel.PostCaptureEvent.ShareMedia::class.java)
        val shareEvent = receivedEvent as PostCaptureViewModel.PostCaptureEvent.ShareMedia
        assertThat(shareEvent.media).isEqualTo(testImageDesc)
    }

    @Test
    fun getUiState_image_viewerHasContent() = runTest(testDispatcher) {
        // Act
        mediaRepository.setCurrentMedia(testImageDesc)

        // Wait for state update to show content
        val uiState = viewModel.postCaptureUiState.first {
            it is PostCaptureUiState.Ready &&
                it.viewerUiState is MediaViewerUiState.Content.Image
        } as PostCaptureUiState.Ready

        // Assert
        val viewerState = uiState.viewerUiState as MediaViewerUiState.Content.Image
        assertThat(viewerState.imageBitmap).isEqualTo(testImageMedia.bitmap)
    }

    @Test
    fun getUiState_video_playerIsInitializedAndTransitionsToContent() = runTest(testDispatcher) {
        // Act
        mediaRepository.setCurrentMedia(testVideoDesc)

        // Wait for the final state to show content for the video
        val uiState = viewModel.postCaptureUiState.first {
            it is PostCaptureUiState.Ready &&
                it.viewerUiState is MediaViewerUiState.Content.Video.Ready
        } as PostCaptureUiState.Ready

        // Assert
        val viewerState = uiState.viewerUiState as MediaViewerUiState.Content.Video.Ready
        assertThat(viewerState.player).isNotNull()
    }

    @Test
    fun switchVideoToImage_playerIsReleased() = runTest(testDispatcher) {
        // Arrange: Start with a video, ensuring player is initialized
        mediaRepository.setCurrentMedia(testVideoDesc)
        viewModel.postCaptureUiState.first {
            it is PostCaptureUiState.Ready &&
                it.viewerUiState is MediaViewerUiState.Content.Video.Ready
        }

        // Act: Switch to an image
        mediaRepository.setCurrentMedia(testImageDesc)

        // Assert: Wait for the state to become Image content
        val finalState = viewModel.postCaptureUiState.first {
            it is PostCaptureUiState.Ready &&
                it.viewerUiState is MediaViewerUiState.Content.Image
        } as PostCaptureUiState.Ready

        val finalViewerState = finalState.viewerUiState as MediaViewerUiState.Content.Image
        assertThat(finalViewerState.imageBitmap).isEqualTo((testImageMedia.bitmap))
    }

    @Test
    fun onCleared_deletesCachedMedia() = runTest(testDispatcher) {
        // Arrange
        mediaRepository.setCurrentMedia(testCacheImageDesc)
        advanceUntilIdle()

        // Act
        callOnCleared(viewModel)
        testExternalScope.advanceUntilIdle() // Run the external scope job

        // Assert
        assertThat(mediaRepository.currentMedia.value).isEqualTo(MediaDescriptor.None)
    }

    @Test
    fun onCleared_deleteCachedMediaFails_mediaNotCleared() = runTest(testDispatcher) {
        // Arrange
        mediaRepository.setCurrentMedia(testCacheImageDesc)
        mediaRepository.deleteMediaHandler = { false } // Simulate failure
        advanceUntilIdle()

        // Act
        callOnCleared(viewModel)
        testExternalScope.advanceUntilIdle() // Run the external scope job

        // Assert
        // The ViewModel should have attempted to delete, but the fake repository
        // should not have cleared the media on failure.
        assertThat(mediaRepository.currentMedia.value).isEqualTo(testCacheImageDesc)
    }

    @Test
    fun onCleared_keepsSavedMedia() = runTest(testDispatcher) {
        // Arrange
        mediaRepository.setCurrentMedia(testImageDesc)
        advanceUntilIdle()

        // Act
        callOnCleared(viewModel)
        testExternalScope.advanceUntilIdle()

        // Assert
        assertThat(mediaRepository.currentMedia.value).isEqualTo(testImageDesc)
    }

    @Test
    fun saveCurrentMedia_image_onSuccess_showsSuccessSnackbar() = runTest(testDispatcher) {
        // Given
        mediaRepository.setCurrentMedia(testImageDesc)
        advanceUntilIdle()

        // When
        viewModel.saveCurrentMedia()
        advanceUntilIdle()

        // Then
        val snackBarUiState = viewModel.snackBarUiState.value.asEnabled()

        assertThat(snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS)
    }

    @Test
    fun saveCurrentMedia_video_onSuccess_showsSuccessSnackbar() = runTest(testDispatcher) {
        // Given
        mediaRepository.setCurrentMedia(testVideoDesc)
        advanceUntilIdle()

        // When
        viewModel.saveCurrentMedia()
        advanceUntilIdle()

        // Then
        val snackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS)
    }

    @Test
    fun saveCurrentMedia_image_onFailure_showsFailureSnackbar() = runTest(testDispatcher) {
        // Given
        mediaRepository.setCurrentMedia(testImageDesc)
        advanceUntilIdle()
        mediaRepository.saveToMediaStoreHandler = { null } // Simulate failure

        // When
        viewModel.saveCurrentMedia()
        advanceUntilIdle()

        // Then
        val snackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_IMAGE_SAVE_FAILURE)
    }

    @Test
    fun saveCurrentMedia_video_onFailure_showsFailureSnackbar() = runTest(testDispatcher) {
        // Given
        mediaRepository.setCurrentMedia(testVideoDesc)
        advanceUntilIdle()
        mediaRepository.saveToMediaStoreHandler = { null } // Simulate failure

        // When
        viewModel.saveCurrentMedia()
        advanceUntilIdle()

        // Then
        val snackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_VIDEO_SAVE_FAILURE)
    }

    @Test
    fun deleteMedia_image_onFailure_showsFailureSnackbar() = runTest(testDispatcher) {
        // Given
        mediaRepository.setCurrentMedia(testImageDesc)
        advanceUntilIdle()
        mediaRepository.deleteMediaHandler = { throw RuntimeException("Test Exception") }

        // When
        viewModel.deleteCurrentMedia()
        advanceUntilIdle()

        // Then
        val snackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_IMAGE_DELETE_FAILURE)
    }

    @Test
    fun deleteMedia_video_onFailure_showsFailureSnackbar() = runTest(testDispatcher) {
        // Given
        mediaRepository.setCurrentMedia(testVideoDesc)
        advanceUntilIdle()
        mediaRepository.deleteMediaHandler = { throw RuntimeException("Test Exception") }

        // When
        viewModel.deleteCurrentMedia()
        advanceUntilIdle()

        // Then
        val snackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_VIDEO_DELETE_FAILURE)
    }

    @Test
    fun onSnackBarResult_removesFromQueue() = runTest(testDispatcher) {
        // Given
        mediaRepository.setCurrentMedia(testImageDesc)
        advanceUntilIdle()
        viewModel.saveCurrentMedia() // This will add a snackbar for the image
        advanceUntilIdle()
        val snackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        val cookie = snackBarUiState.snackBarQueue.first().cookie

        // When
        viewModel.snackBarController.onSnackBarResult(cookie)
        advanceUntilIdle()

        // Then
        val updatedSnackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(updatedSnackBarUiState.snackBarQueue).isEmpty()
    }

    @Test
    fun onSnackBarResult_withIncorrectCookie_doesNotChangeQueue() = runTest(testDispatcher) {
        mediaRepository.setCurrentMedia(testImageDesc)
        advanceUntilIdle()

        // Given
        viewModel.saveCurrentMedia() // This will add a snackbar
        advanceUntilIdle()

        // When
        viewModel.snackBarController.onSnackBarResult("incorrect_cookie")
        advanceUntilIdle()

        // Then
        val updatedSnackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(updatedSnackBarUiState.snackBarQueue).hasSize(1)
    }

    @Test
    fun onSnackBarResult_withEmptyQueue_doesNotChangeQueue() = runTest(testDispatcher) {
        // Given an empty snackbar queue (initial state)
        val snackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(snackBarUiState.snackBarQueue).isEmpty()

        // When
        viewModel.snackBarController.onSnackBarResult("any_cookie")
        advanceUntilIdle()

        // Then
        val updatedSnackBarUiState = viewModel.snackBarUiState.value.asEnabled()
        assertThat(updatedSnackBarUiState.snackBarQueue).isEmpty()
    }

    // Helper to access protected method without extending class
    private fun callOnCleared(viewModel: ViewModel) {
        val onClearedMethod = ViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(viewModel)
    }

    private fun SnackBarUiState.asEnabled() = this as SnackBarUiState.Enabled
}
