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

package com.google.jetpackcamera.ui.uistateadapter.viewfinder

import com.example.uistateadapter.Utils
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LowLightBoostState
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.viewfinder.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.FlashModeUiState.Available
import com.google.jetpackcamera.ui.uistate.viewfinder.FlashModeUiState.Unavailable
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState.*


object FlashModeUiStateAdapter {

    private val ORDERED_UI_SUPPORTED_FLASH_MODES = listOf(
        FlashMode.OFF,
        FlashMode.ON,
        FlashMode.AUTO,
        FlashMode.LOW_LIGHT_BOOST
    )

    fun getUiState(
        cameraAppSettings: CameraAppSettings,
        systemConstraints: SystemConstraints
    ): FlashModeUiState {
        val selectedFlashMode = cameraAppSettings.flashMode
        val supportedFlashModes = systemConstraints.forCurrentLens(cameraAppSettings)
            ?.supportedFlashModes
            ?: setOf(FlashMode.OFF)
        return createFrom(
            selectedFlashMode = selectedFlashMode,
            supportedFlashModes = supportedFlashModes
        )
    }

    private fun createFrom(
        selectedFlashMode: FlashMode,
        supportedFlashModes: Set<FlashMode>
    ): FlashModeUiState {
        // Ensure we at least support one flash mode
        check(supportedFlashModes.isNotEmpty()) {
            "No flash modes supported. Should at least support OFF."
        }

        // Convert available flash modes to list we support in the UI in our desired order
        val availableModes =
            Utils.getSelectableListFromValues(
                supportedFlashModes,
                ORDERED_UI_SUPPORTED_FLASH_MODES
            )

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

    fun FlashModeUiState.updateFrom(
        currentCameraSettings: CameraAppSettings,
        currentConstraints: SystemConstraints,
        cameraState: CameraState
    ): FlashModeUiState {
        val currentFlashMode = currentCameraSettings.flashMode
        val currentSupportedFlashModes =
            currentConstraints.forCurrentLens(currentCameraSettings)?.supportedFlashModes
        return when (this) {
            is Unavailable -> {
                // When previous state was "Unavailable", we'll try to create a new FlashModeUiState
                createFrom(
                    selectedFlashMode = currentFlashMode,
                    supportedFlashModes = currentSupportedFlashModes ?: setOf(FlashMode.OFF)
                )
            }

            is Available -> {
                val previousFlashMode = this.selectedFlashMode
                val previousAvailableFlashModes = this.availableFlashModes
                val currentAvailableFlashModes =
                    previousAvailableFlashModes.map { supportedFlashMode ->
                        SelectableUi(supportedFlashMode)
                    }
                if (previousAvailableFlashModes != currentAvailableFlashModes) {
                    // Supported flash modes have changed, generate a new FlashModeUiState
                    createFrom(
                        selectedFlashMode = currentFlashMode,
                        supportedFlashModes = currentSupportedFlashModes ?: setOf(FlashMode.OFF)
                    )
                } else if (previousFlashMode != currentFlashMode) {
                    // Only the selected flash mode has changed, just update the flash mode
                    copy(selectedFlashMode = currentFlashMode)
                } else {
                    if (currentFlashMode == FlashMode.LOW_LIGHT_BOOST) {
                        copy(
                            isActive = cameraState.lowLightBoostState == LowLightBoostState.ACTIVE
                        )
                    } else {
                        // Nothing has changed
                        this
                    }
                }
            }
        }
    }
}
