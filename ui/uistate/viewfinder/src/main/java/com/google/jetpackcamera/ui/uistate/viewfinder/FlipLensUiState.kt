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

import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

sealed interface FlipLensUiState {
    data object Unavailable : FlipLensUiState

    data class Available(
        val selectedLensFacing: LensFacing,
        val availableLensFacings: List<SingleSelectableUiState<LensFacing>>,
    ) : FlipLensUiState {
        init {
            val isSelectedModePresentAndSelectable = availableLensFacings.any { state ->
                state is SingleSelectableUiState.SelectableUi && state.value == selectedLensFacing
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected lens $selectedLensFacing is not among the available and selectable " +
                    "lenses. Available modes: ${
                        availableLensFacings.mapNotNull {
                            if (it is SingleSelectableUiState.SelectableUi) it.value else null
                        }
                    }"
            }
        }
    }
}