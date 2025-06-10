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

package com.google.jetpackcamera.ui.uistate

import com.google.jetpackcamera.settings.model.StreamConfig

sealed interface StreamConfigUiState: UiState {
    data object Unavailable : StreamConfigUiState

    data class Available(
        val selectedStreamConfig: StreamConfig,
        val availableStreamConfigs: List<UiSingleSelectableState<StreamConfig>>,
        val isActive: Boolean
    ) : StreamConfigUiState {
        init {
            val isSelectedModePresentAndSelectable = availableStreamConfigs.any { state ->
                state is UiSingleSelectableState.Selectable && state.value == selectedStreamConfig
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected stream config $selectedStreamConfig is not among the available and selectable configs. " +
                        "Available configs: ${
                            availableStreamConfigs.mapNotNull {
                                if (it is UiSingleSelectableState.Selectable) it.value else null
                            }
                        }"
            }
        }
    }
}