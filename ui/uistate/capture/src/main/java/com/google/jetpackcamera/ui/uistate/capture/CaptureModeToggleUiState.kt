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

import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

sealed interface CaptureModeToggleUiState {
    data object Unavailable : CaptureModeToggleUiState
    data class Available(
        val selectedCaptureMode: CaptureMode,
        val availableCaptureModes: Pair<
            SingleSelectableUiState<CaptureMode>,
            SingleSelectableUiState<CaptureMode>
            >
    ) : CaptureModeToggleUiState {
        init {
            val isSelectedModePresentAndSelectable = availableCaptureModes.let { (first, second) ->
                (
                    first is SingleSelectableUiState.SelectableUi &&
                        first.value == selectedCaptureMode
                    ) ||
                    (
                        second is SingleSelectableUiState.SelectableUi &&
                            second.value == selectedCaptureMode
                        )
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected capture mode $selectedCaptureMode is not among the available and " +
                    "selectable capture modes."
            }
        }
    }

    fun CaptureModeToggleUiState.isCaptureModeSelectable(captureMode: CaptureMode): Boolean {
        return when (this) {
            is Available -> {
                availableCaptureModes.let { (first, second) ->
                    (
                        first is SingleSelectableUiState.SelectableUi &&
                            first.value == selectedCaptureMode
                        ) ||
                        (
                            second is SingleSelectableUiState.SelectableUi &&
                                second.value == selectedCaptureMode
                            )
                }
            }

            Unavailable -> false
        }
    }

    fun CaptureModeToggleUiState.findSelectableStateFor(
        targetCaptureMode: CaptureMode
    ): SingleSelectableUiState<CaptureMode>? {
        if (this is Available) {
            return availableCaptureModes.let { (first, second) ->
                if (first is SingleSelectableUiState.SelectableUi &&
                    first.value == targetCaptureMode
                ) {
                    first
                } else {
                    second
                }
            }
        }
        return null
    }

    companion object
}
