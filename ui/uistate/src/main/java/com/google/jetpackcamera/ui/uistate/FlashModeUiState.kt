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

import com.google.jetpackcamera.settings.model.FlashMode

sealed interface FlashModeUiState: UiState {
    data object Unavailable : FlashModeUiState

    data class Available(
        val selectedFlashMode: FlashMode,
        val availableFlashModes: List<UiSingleSelectableState<FlashMode>>,
        val isActive: Boolean
    ) : FlashModeUiState {
        init {
            val isSelectedModePresentAndSelectable = availableFlashModes.any { state ->
                state is UiSingleSelectableState.Selectable && state.value == selectedFlashMode
            }

            check(isSelectedModePresentAndSelectable) {
                "Selected flash mode $selectedFlashMode is not among the available and selectable flash modes. " +
                        "Available modes: ${
                            availableFlashModes.mapNotNull {
                                if (it is UiSingleSelectableState.Selectable) it.value else null
                            }
                        }"
            }
        }
    }

    companion object {
        private val ORDERED_UI_SUPPORTED_FLASH_MODES = listOf(
            FlashMode.OFF,
            FlashMode.ON,
            FlashMode.AUTO,
            FlashMode.LOW_LIGHT_BOOST
        )

        /**
         * Creates a FlashModeUiState from a selected flash mode and a set of supported flash modes
         * that may not include flash modes supported by the UI.
         */
        fun createFrom(
            selectedFlashMode: FlashMode,
            supportedFlashModes: Set<FlashMode>
        ): FlashModeUiState {
            // Ensure we at least support one flash mode
            check(supportedFlashModes.isNotEmpty()) {
                "No flash modes supported. Should at least support OFF."
            }

            // Convert available flash modes to list we support in the UI in our desired order
            val availableModes = ORDERED_UI_SUPPORTED_FLASH_MODES.filter {
                it in supportedFlashModes
            }.map { supportedFlashMode ->
                UiSingleSelectableState.Selectable(supportedFlashMode)
            }

            return if (availableModes.isEmpty() || availableModes == listOf(FlashMode.OFF)) {
                // If we only support OFF, then return "Unavailable".
                Unavailable
            } else {
                Available(
                    selectedFlashMode = selectedFlashMode,
                    availableFlashModes = availableModes,
                    isActive = false
                )
            }
        }
    }
}