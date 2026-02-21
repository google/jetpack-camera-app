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

import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

/**
 * Defines the UI state for the flash mode setting.
 *
 * This sealed interface represents the different states of the flash mode UI, such as being
 * unavailable or available with a set of options.
 */
sealed interface FlashModeUiState {
    /**
     * The flash mode setting is unavailable.
     * This may be because the current camera does not support flash or it is disabled by other
     * settings.
     */
    data object Unavailable : FlashModeUiState

    /**
     * The flash mode setting is available for user interaction.
     *
     * @param selectedFlashMode The currently selected [FlashMode].
     * @param availableFlashModes A list of all supported flash modes, each represented by a
     * [SingleSelectableUiState] to indicate its selection status.
     * @param isLowLightBoostActive Indicates whether the low light boost feature is currently
     * active, which may affect flash behavior.
     */
    data class Available(
        val selectedFlashMode: FlashMode,
        val availableFlashModes: List<SingleSelectableUiState<FlashMode>>,
        val isLowLightBoostActive: Boolean
    ) : FlashModeUiState {
        init {
            val isSelectedModePresentAndSelectable = availableFlashModes.any { state ->
                state is SingleSelectableUiState.SelectableUi && state.value == selectedFlashMode
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected flash mode $selectedFlashMode is not among the available and selectable" +
                    "flash modes. Available modes: ${
                        availableFlashModes.mapNotNull {
                            if (it is SingleSelectableUiState.SelectableUi) it.value else null
                        }
                    }"
            }
        }
    }

    companion object
}
