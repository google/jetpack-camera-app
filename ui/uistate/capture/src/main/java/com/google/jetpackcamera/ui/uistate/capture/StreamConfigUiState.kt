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

import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

/**
 * Defines the UI state for stream configuration, which includes settings for resolution and aspect
 * ratio.
 *
 * This sealed interface represents the different states of the stream configuration UI.
 */
sealed interface StreamConfigUiState {
    /**
     * Stream configuration is unavailable.
     */
    data object Unavailable : StreamConfigUiState

    /**
     * Stream configuration is available.
     *
     * @param selectedStreamConfig The currently selected stream configuration.
     * @param availableStreamConfigs A list of all available stream configurations, each represented
     * by a [SingleSelectableUiState] to indicate its current selection and interaction status.
     * @param isActive Indicates whether the stream configuration UI is currently active (e.g., visible).
     */
    data class Available(
        val selectedStreamConfig: StreamConfig,
        val availableStreamConfigs: List<SingleSelectableUiState<StreamConfig>>,
        val isActive: Boolean
    ) : StreamConfigUiState {
        init {
            val isSelectedModePresentAndSelectable = availableStreamConfigs.any { state ->
                state is SingleSelectableUiState.SelectableUi && state.value == selectedStreamConfig
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected stream config $selectedStreamConfig is not among the available and" +
                    "selectable configs. Available configs: ${
                        availableStreamConfigs.mapNotNull {
                            if (it is SingleSelectableUiState.SelectableUi) it.value else null
                        }
                    }"
            }
        }
    }

    companion object
}
