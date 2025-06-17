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

package com.google.jetpackcamera.ui.uistate.viewfinder

import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

sealed interface FlashModeUiState {
    data object Unavailable : FlashModeUiState

    data class Available(
        val selectedFlashMode: FlashMode,
        val availableFlashModes: List<SingleSelectableUiState<FlashMode>>,
        val isActive: Boolean
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
}