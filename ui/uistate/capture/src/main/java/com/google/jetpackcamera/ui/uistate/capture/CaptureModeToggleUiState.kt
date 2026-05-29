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
package com.google.jetpackcamera.ui.uistate.capture

import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

/**
 * Defines the UI state for the capture mode toggle (e.g., photo vs. video).
 *
 * This sealed interface represents the possible states of the UI component that allows switching
 * between different capture modes like image and video.
 */
sealed interface CaptureModeToggleUiState {
    /**
     * The capture mode toggle is unavailable and should be hidden or disabled.
     * This may occur when the camera is busy or if the capture mode is fixed by an external
     * intent.
     */
    data object Unavailable : CaptureModeToggleUiState

    /**
     * The capture mode toggle is available for user interaction.
     *
     * @param selectedCaptureMode The currently active [CaptureMode].
     * @param imageOnlyUiState The UI state for the "Image" option in the toggle.
     * @param videoOnlyUiState The UI state for the "Video" option in the toggle.
     */
    data class Available(
        val selectedCaptureMode: CaptureMode,
        val imageOnlyUiState: SingleSelectableUiState<CaptureMode>,
        val videoOnlyUiState: SingleSelectableUiState<CaptureMode>
    ) : CaptureModeToggleUiState {
        init {
            val isSelectedModePresentAndSelectable =
                selectedCaptureMode == CaptureMode.IMAGE_ONLY ||
                    selectedCaptureMode == CaptureMode.VIDEO_ONLY

            check(isSelectedModePresentAndSelectable) {
                "Selected capture mode $selectedCaptureMode is not among video or image only }"
            }
        }
    }

    /**
     * Checks if a given [CaptureMode] is currently selectable in the UI.
     *
     * @param captureMode The capture mode to check.
     * @return `true` if the UI is in the [Available] state and the corresponding mode is
     * selectable, `false` otherwise.
     */
    fun CaptureModeToggleUiState.isCaptureModeSelectable(captureMode: CaptureMode): Boolean {
        return when (this) {
            is Available -> {
                (
                    captureMode == CaptureMode.IMAGE_ONLY &&
                        imageOnlyUiState is SingleSelectableUiState.SelectableUi
                    ) ||
                    (
                        captureMode == CaptureMode.VIDEO_ONLY &&
                            videoOnlyUiState is SingleSelectableUiState.SelectableUi
                        )
            }

            Unavailable -> false
        }
    }

    /**
     * Finds the [SingleSelectableUiState] for a specific [CaptureMode].
     *
     * @param targetCaptureMode The capture mode to find the UI state for.
     * @return The corresponding [SingleSelectableUiState] if the UI is in the [Available] state,
     * or `null` otherwise.
     */
    fun CaptureModeToggleUiState.findSelectableStateFor(
        targetCaptureMode: CaptureMode
    ): SingleSelectableUiState<CaptureMode>? {
        if (this is Available) {
            return if (targetCaptureMode == CaptureMode.IMAGE_ONLY) {
                imageOnlyUiState
            } else {
                videoOnlyUiState
            }
        }
        return null
    }

    companion object
}
