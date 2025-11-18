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
package com.google.jetpackcamera.ui.uistateadapter.capture

import android.net.Uri
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class ImageWellUiStateAdapterTest {
    private val testThumbnailUri: Uri = mock(Uri::class.java)

    @Test
    fun from_mediaContentAndVideoInactive_returnsLastCapture() {
        // Given
        val mediaDescriptor = MediaDescriptor.Content.Image(testThumbnailUri, null, false)
        val videoRecordingState = VideoRecordingState.Inactive()

        // When
        val result = ImageWellUiState.from(mediaDescriptor, videoRecordingState)

        // Then
        assertEquals(ImageWellUiState.LastCapture(mediaDescriptor), result)
    }

    @Test
    fun from_mediaNone_returnsUnavailable() {
        // Given
        val mediaDescriptor = MediaDescriptor.None
        val videoRecordingState = VideoRecordingState.Inactive()

        // When
        val result = ImageWellUiState.from(mediaDescriptor, videoRecordingState)

        // Then
        assertEquals(ImageWellUiState.Unavailable, result)
    }

    @Test
    fun from_videoRecording_returnsUnavailable() {
        // Given
        val mediaDescriptor = MediaDescriptor.Content.Image(testThumbnailUri, null, false)
        val videoRecordingState = VideoRecordingState.Active.Recording(0, 0.0, 0)

        // When
        val result = ImageWellUiState.from(mediaDescriptor, videoRecordingState)

        // Then
        assertEquals(ImageWellUiState.Unavailable, result)
    }

    @Test
    fun from_videoPaused_returnsUnavailable() {
        // Given
        val mediaDescriptor = MediaDescriptor.Content.Image(testThumbnailUri, null, false)
        val videoRecordingState = VideoRecordingState.Active.Paused(0, 0.0, 0)

        // When
        val result = ImageWellUiState.from(mediaDescriptor, videoRecordingState)

        // Then
        assertEquals(ImageWellUiState.Unavailable, result)
    }

    @Test
    fun from_mediaContentAndVideoStarting_returnsUnavailable() {
        // Given
        val mediaDescriptor = MediaDescriptor.Content.Image(testThumbnailUri, null, false)
        val videoRecordingState = VideoRecordingState.Starting()

        // When
        val result = ImageWellUiState.from(mediaDescriptor, videoRecordingState)

        // Then
        assertEquals(ImageWellUiState.Unavailable, result)
    }
}
