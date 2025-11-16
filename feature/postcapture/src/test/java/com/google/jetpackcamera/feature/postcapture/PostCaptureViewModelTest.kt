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
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_DELETE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_SAVE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_DELETE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_SAVE_FAILURE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS
import com.google.jetpackcamera.ui.uistate.postcapture.MediaViewerUiState
import com.google.jetpackcamera.ui.uistate.postcapture.PostCaptureUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class PostCaptureViewModelTest {

    private val testContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val testDispatcher = StandardTestDispatcher()
    private val testExternalScope = TestScope(testDispatcher)

    private lateinit var viewModel: PostCaptureViewModel
    private lateinit var mockMediaRepository: MediaRepository

    // Fakes

    private val currentMediaFlow = MutableStateFlow<MediaDescriptor>(MediaDescriptor.None)

    // --- SHARED TEST DATA ---
    val mockThumbnail: Bitmap = Mockito.mock(Bitmap::class.java)

    private val testImageUri = Uri.parse("file:///test.jpg")
    private val testCacheImageDesc = MediaDescriptor.Content.Image(
        testImageUri,
        mockThumbnail,
        true
    )

    private val testImageDesc = MediaDescriptor.Content.Image(testImageUri, mockThumbnail)
    private val testImageMedia = Media.Image(mockThumbnail)

    private val testVideoUri = Uri.parse("file:///test.mp4")
    private val testVideoDesc = MediaDescriptor.Content.Video(testVideoUri, mockThumbnail)
    private val testCacheVideoDesc = MediaDescriptor.Content.Video(
        testVideoUri,
        mockThumbnail,
        true
    )
    private val testVideoMedia = Media.Video(testVideoUri)

    @Before
    fun setup() = runTest(testDispatcher) {
        Dispatchers.setMain(testDispatcher)

        mockMediaRepository = mock()

        // Default Stubbing
        `when`(mockMediaRepository.currentMedia).thenReturn(currentMediaFlow)

        `when`(
            mockMediaRepository.saveToMediaStore(
                safeAny(MediaDescriptor.Content.Image::class.java),
                safeAny(String::class.java)
            )
        ).thenReturn(testImageUri)

        // custom stub output for mediarepository load calls
        `when`(
            mockMediaRepository.load(
                any(MediaDescriptor::class.java) ?: MediaDescriptor.None
            )
        ).thenAnswer { invocation ->
            // Get the argument passed to .load()
            // Return dynamic result based on input class
            when (invocation.arguments[0] as MediaDescriptor) {
                is MediaDescriptor.Content.Video -> testVideoMedia
                is MediaDescriptor.Content.Image -> testImageMedia
                else -> Media.None
            }
        }

        viewModel = PostCaptureViewModel(
            mediaRepository = mockMediaRepository,
            context = testContext
        )
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        testExternalScope.cancel()
    }

    @Test
    fun getUiState_image_viewerHasContent() = runTest(testDispatcher) {
        // Act
        currentMediaFlow.emit(testImageDesc)

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
        currentMediaFlow.emit(testVideoDesc)

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
        currentMediaFlow.emit(testVideoDesc)
        viewModel.postCaptureUiState.first {
            it is PostCaptureUiState.Ready &&
                it.viewerUiState is MediaViewerUiState.Content.Video.Ready
        }

        // Act: Switch to an image
        currentMediaFlow.emit(testImageDesc)

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
        currentMediaFlow.emit(testCacheImageDesc)
        advanceUntilIdle()

        // Act
        callOnCleared(viewModel)
        testExternalScope.advanceUntilIdle() // Run the external scope job

        // Assert
        verify(mockMediaRepository).deleteMedia(
            safeEq(testCacheImageDesc)
        )
    }

    @Test
    fun onCleared_keepsSavedMedia() = runTest(testDispatcher) {
        // Arrange
        currentMediaFlow.emit(testImageDesc)
        advanceUntilIdle()

        // Act
        callOnCleared(viewModel)
        testExternalScope.advanceUntilIdle()

        // Assert
        verify(mockMediaRepository, never()).deleteMedia(
            safeAny(MediaDescriptor.Content::class.java)
        )
    }

    @Test
    fun saveCurrentMedia_image_onSuccess_showsSuccessSnackbar() = runTest(testDispatcher) {
        // Given
        currentMediaFlow.emit(testImageDesc)
        advanceUntilIdle()

        // When
        viewModel.saveCurrentMedia()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.postCaptureUiState.value.asReady()

        assertThat(uiState.snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(uiState.snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS)
    }

    @Test
    fun saveCurrentMedia_video_onSuccess_showsSuccessSnackbar() = runTest(testDispatcher) {
        // Given
        currentMediaFlow.emit(testVideoDesc)
        advanceUntilIdle()
        `when`(
            mockMediaRepository.saveToMediaStore(
                safeAny(MediaDescriptor.Content::class.java),
                safeAny(String::class.java)
            )
        ).thenReturn(testVideoUri)

        // When
        viewModel.saveCurrentMedia()
        advanceUntilIdle()

        // Then
        val uiState: PostCaptureUiState.Ready = viewModel.postCaptureUiState.first {
            (it is PostCaptureUiState.Ready)
        } as PostCaptureUiState.Ready
        assertThat(uiState.snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(uiState.snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS)
    }

    @Test
    fun saveCurrentMedia_image_onFailure_showsFailureSnackbar() = runTest(testDispatcher) {
        // Given
        currentMediaFlow.emit(testImageDesc)
        advanceUntilIdle()
        `when`(
            mockMediaRepository.saveToMediaStore(
                safeAny(MediaDescriptor.Content::class.java),
                safeAny(String::class.java)
            )
        ).thenReturn(null) // Simulate failure

        // When
        viewModel.saveCurrentMedia()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.postCaptureUiState.value.asReady()
        assertThat(uiState.snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(uiState.snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_IMAGE_SAVE_FAILURE)
    }

    @Test
    fun saveCurrentMedia_video_onFailure_showsFailureSnackbar() = runTest(testDispatcher) {
        // Given
        currentMediaFlow.emit(testVideoDesc)
        advanceUntilIdle()
        `when`(
            mockMediaRepository.saveToMediaStore(
                safeAny(MediaDescriptor.Content::class.java),
                safeAny(String::class.java)
            )
        ).thenReturn(null) // Simulate failure

        // When
        viewModel.saveCurrentMedia()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.postCaptureUiState.value.asReady()
        assertThat(uiState.snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(uiState.snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_VIDEO_SAVE_FAILURE)
    }

    @Test
    fun deleteMedia_image_onFailure_showsFailureSnackbar() = runTest(testDispatcher) {
        // Given
        currentMediaFlow.emit(testImageDesc)
        advanceUntilIdle()
        `when`(
            mockMediaRepository.deleteMedia(
                safeAny(MediaDescriptor.Content::class.java)
            )
        ).thenThrow(RuntimeException("Test Exception")) // Simulate failure

        // When
        viewModel.deleteCurrentMedia()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.postCaptureUiState.value.asReady()
        assertThat(uiState.snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(uiState.snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_IMAGE_DELETE_FAILURE)
    }

    @Test
    fun deleteMedia_video_onFailure_showsFailureSnackbar() = runTest(testDispatcher) {
        // Given
        currentMediaFlow.emit(testVideoDesc)
        advanceUntilIdle()
        `when`(
            mockMediaRepository.deleteMedia(
                safeAny(MediaDescriptor.Content::class.java)
            )
        ).thenThrow(RuntimeException("Test Exception")) // Simulate failure

        // When
        viewModel.deleteCurrentMedia()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.postCaptureUiState.value.asReady()
        assertThat(uiState.snackBarUiState.snackBarQueue).hasSize(1)
        assertThat(uiState.snackBarUiState.snackBarQueue.first().testTag)
            .isEqualTo(SNACKBAR_POST_CAPTURE_VIDEO_DELETE_FAILURE)
    }

    @Test
    fun onSnackBarResult_removesFromQueue() = runTest(testDispatcher) {
        // Given
        currentMediaFlow.emit(testImageDesc)
        advanceUntilIdle()
        viewModel.saveCurrentMedia() // This will add a snackbar for the image
        advanceUntilIdle()
        val uiStateWithSnackbar = viewModel.postCaptureUiState.value.asReady()
        val cookie = uiStateWithSnackbar.snackBarUiState.snackBarQueue.first().cookie

        // When
        viewModel.onSnackBarResult(cookie)
        advanceUntilIdle()

        // Then
        val updatedUiState = viewModel.postCaptureUiState.value.asReady()
        assertThat(updatedUiState.snackBarUiState.snackBarQueue).isEmpty()
    }

    @Test
    fun onSnackBarResult_withIncorrectCookie_doesNotChangeQueue() = runTest(testDispatcher) {
        currentMediaFlow.emit(testImageDesc)
        advanceUntilIdle()

        // Given
        viewModel.saveCurrentMedia() // This will add a snackbar
        advanceUntilIdle()

        // When
        viewModel.onSnackBarResult("incorrect_cookie")
        advanceUntilIdle()

        // Then
        val updatedUiState = viewModel.postCaptureUiState.value.asReady()
        assertThat(updatedUiState.snackBarUiState.snackBarQueue).hasSize(1)
    }

    // Helper to access protected method without extending class
    private fun callOnCleared(viewModel: ViewModel) {
        val onClearedMethod = ViewModel::class.java.getDeclaredMethod("onCleared")
        onClearedMethod.isAccessible = true
        onClearedMethod.invoke(viewModel)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> safeAny(type: Class<T>): T {
        any(type)
        return null as T
    }

    private fun <T> safeEq(value: T): T {
        eq(value)
        return value
    }

    private fun PostCaptureUiState.asReady() = this as PostCaptureUiState.Ready
}
