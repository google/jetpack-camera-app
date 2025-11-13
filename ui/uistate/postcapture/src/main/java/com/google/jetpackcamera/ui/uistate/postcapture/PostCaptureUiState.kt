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
package com.google.jetpackcamera.postcapture

import android.graphics.Bitmap
import androidx.media3.exoplayer.ExoPlayer
import com.google.jetpackcamera.ui.uistate.capture.SnackBarUiState


/**
 * Defines the overall UI state for the PostCaptureScreen.
 */
sealed interface PostCaptureUiState {
    /**
     * The screen is loading.
     */
    object Loading : PostCaptureUiState

    /**
     * The screen is ready to display content and interact.
     */
    data class Ready(
        val viewerUiState: MediaViewerUiState,
        val snackBarUiState: SnackBarUiState
    ) : PostCaptureUiState
}

/**
 * Defines the UI state for the content viewer within the PostCaptureScreen.
 */
sealed interface MediaViewerUiState {
    /**
     * Viewer is in a loading state.
     */
    object Loading : MediaViewerUiState

    /**
     * Viewer has content to display.
     */
    interface Content : MediaViewerUiState {
        // val media: Media
        data class Video(val player: ExoPlayer) : Content
        data class Image(val imageBitmap: Bitmap) : Content
    }
}
