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

sealed interface CaptureModeToggleUiState {
    data object Unavailable : CaptureModeToggleUiState
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
