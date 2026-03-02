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
 * Defines the UI state for the capture mode setting.
 *
 * This sealed interface represents the possible states of the UI component that allows switching
 * between different capture modes, such as single stream or multi-stream.
 */
sealed interface CaptureModeUiState {
    /**
     * The capture mode selection is unavailable.
     * This may occur when the camera is busy or if the capture mode is fixed by an external
     * constraint.
     */
    data object Unavailable : CaptureModeUiState

    /**
     * The capture mode selection is available.
     *
     * @param selectedCaptureMode The currently active [CaptureMode].
     * @param availableCaptureModes A list of all available capture modes, each represented by a
     * [SingleSelectableUiState] to indicate its current selection and interaction status.
     */
    data class Available(
        val selectedCaptureMode: CaptureMode,
        val availableCaptureModes: List<SingleSelectableUiState<CaptureMode>>
    ) : CaptureModeUiState {
        init {
            val isSelectedModePresentAndSelectable = availableCaptureModes.any { state ->
                state is SingleSelectableUiState.SelectableUi && state.value == selectedCaptureMode
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected capture mode $selectedCaptureMode is not among the available and " +
                    "selectable capture modes. Available modes: ${
                        availableCaptureModes.mapNotNull {
                            if (it is SingleSelectableUiState.SelectableUi) it.value else null
                        }
                    }"
            }
        }
    }

    /**
     * Checks if a specific [CaptureMode] is currently selectable in the UI.
     *
     * @param captureMode The capture mode to check.
     * @return `true` if the UI is in the [Available] state and the specified mode is selectable,
     * `false` otherwise.
     */
    fun CaptureModeUiState.isCaptureModeSelectable(captureMode: CaptureMode): Boolean {
        return when (this) {
            is Available -> {
                availableCaptureModes.any {
                    it is SingleSelectableUiState.SelectableUi && it.value == captureMode
                }
            }

            Unavailable -> false
        }
    }

    /**
     * Finds the [SingleSelectableUiState] for a specific [CaptureMode].
     *
     * This is useful for getting the complete UI state for a mode, including whether it is
     * selectable, disabled, or currently selected.
     *
     * @param targetCaptureMode The capture mode to find the UI state for.
     * @return The corresponding [SingleSelectableUiState] if found within the available modes,
     * or `null` otherwise.
     */
    fun CaptureModeUiState.findSelectableStateFor(
        targetCaptureMode: CaptureMode
    ): SingleSelectableUiState<CaptureMode>? {
        if (this is Available) {
            return this.availableCaptureModes.firstOrNull { state ->
                (
                    state is SingleSelectableUiState.SelectableUi &&
                        state.value == targetCaptureMode
                    ) ||
                    (
                        state is SingleSelectableUiState.Disabled &&
                            state.value == targetCaptureMode
                        )
            }
        }
        return null
    }

    companion object
}
