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
package com.google.jetpackcamera.ui.uistate.postcapture

import com.google.jetpackcamera.ui.uistate.capture.SnackBarUiState

/**
 * Defines the overall UI state for the PostCaptureScreen.
 *
 * This sealed interface represents the different states of the post-capture screen, which is
 * displayed after a photo or video has been taken.
 */
sealed interface PostCaptureUiState {
    /**
     * The screen is in a loading state. This is the initial state before the media is ready to be
     * displayed.
     */
    object Loading : PostCaptureUiState

    /**
     * The screen is ready to display content and interact with the user.
     *
     * @param viewerUiState The UI state for the media viewer, which displays the captured photo or
     *   video.
     * @param deleteButtonUiState The UI state for the delete button.
     * @param shareButtonUiState The UI state for the share button.
     * @param snackBarUiState The UI state for displaying snack-bar messages.
     */
    data class Ready(
        val viewerUiState: MediaViewerUiState = MediaViewerUiState.Loading,
        val deleteButtonUiState: DeleteButtonUiState = DeleteButtonUiState.Unavailable,
        val shareButtonUiState: ShareButtonUiState = ShareButtonUiState.Unavailable,
        val snackBarUiState: SnackBarUiState = SnackBarUiState()
    ) : PostCaptureUiState

    companion object
}
