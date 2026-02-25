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

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

/**
 * Defines the UI state for the aspect ratio setting.
 *
 * This sealed interface represents the different states the aspect ratio selection UI can be in,
 * such as being unavailable or available with a list of choices.
 */
sealed interface AspectRatioUiState {
    /**
     * The aspect ratio setting is unavailable.
     *
     * This state is used when the camera system does not support changing the aspect ratio or
     * when it is otherwise disabled.
     */
    data object Unavailable : AspectRatioUiState

    /**
     * The aspect ratio setting is available.
     *
     * @param selectedAspectRatio The currently selected [AspectRatio].
     * @param availableAspectRatios A list of all selectable aspect ratios, each represented by a
     * [SingleSelectableUiState] to indicate its selection status.
     */
    data class Available(
        val selectedAspectRatio: AspectRatio,
        val availableAspectRatios: List<SingleSelectableUiState<AspectRatio>>
    ) : AspectRatioUiState {
        init {
            val isSelectedModePresentAndSelectable = availableAspectRatios.any { state ->
                state is SingleSelectableUiState.SelectableUi && state.value == selectedAspectRatio
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected ratio $selectedAspectRatio is not among the available and selectable " +
                    "ratios. Available ratios: ${
                        availableAspectRatios.mapNotNull {
                            if (it is SingleSelectableUiState.SelectableUi) it.value else null
                        }
                    }"
            }
        }
    }

    companion object
}
