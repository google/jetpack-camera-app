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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LowLightBoostState
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState.Available
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState.Unavailable
import com.google.jetpackcamera.ui.uistateadapter.Utils

private val ORDERED_UI_SUPPORTED_FLASH_MODES = listOf(
    FlashMode.OFF,
    FlashMode.ON,
    FlashMode.AUTO,
    FlashMode.LOW_LIGHT_BOOST
)

fun FlashModeUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints
): FlashModeUiState {
    val selectedFlashMode = cameraAppSettings.flashMode
    val supportedFlashModes = (
        systemConstraints.forCurrentLens(cameraAppSettings)
            ?.supportedFlashModes
            ?: setOf(FlashMode.OFF)
        ).toMutableSet()

    if (cameraAppSettings.dynamicRange != DynamicRange.SDR) {
        supportedFlashModes.remove(FlashMode.LOW_LIGHT_BOOST)
    }

    if (cameraAppSettings.concurrentCameraMode == ConcurrentCameraMode.DUAL) {
        supportedFlashModes.remove(FlashMode.LOW_LIGHT_BOOST)
    }

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

    return if (availableModes.isEmpty() ||
        availableModes == listOf(SingleSelectableUiState.SelectableUi(FlashMode.OFF))
    ) {
        // If we only support OFF, then return "Unavailable".
        Unavailable
    } else {
        Available(
            selectedFlashMode = selectedFlashMode,
            availableFlashModes = availableModes,
            isLowLightBoostActive = false
        )
    }
}

fun FlashModeUiState.updateFrom(
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints,
    cameraState: CameraState
): FlashModeUiState {
    return when (this) {
        is Unavailable -> {
            // When previous state was "Unavailable", we'll try to create a new FlashModeUiState
            FlashModeUiState.Companion.from(cameraAppSettings, systemConstraints)
        }

        is Available -> {
            val supportedFlashModes =
                systemConstraints.forCurrentLens(cameraAppSettings)?.supportedFlashModes
                    ?: setOf(FlashMode.OFF)

            // check if supported flash modes have changed without allocating a new list/set
            val availableModesChanged =
                this.availableFlashModes.size != supportedFlashModes.size ||
                    this.availableFlashModes.any { !supportedFlashModes.contains(it.value) }

            if (availableModesChanged) {
                // Supported flash modes have changed, generate a new FlashModeUiState
                FlashModeUiState.Companion.from(cameraAppSettings, systemConstraints)
            } else if (this.selectedFlashMode != cameraAppSettings.flashMode) {
                // Only the selected flash mode has changed, just update the flash mode
                copy(selectedFlashMode = cameraAppSettings.flashMode)
            } else {
                if (cameraAppSettings.flashMode == FlashMode.LOW_LIGHT_BOOST) {
                    val strength = when (val llbState = cameraState.lowLightBoostState) {
                        is LowLightBoostState.Active -> llbState.strength
                        else -> LowLightBoostState.MINIMUM_STRENGTH
                    }
                    copy(
                        isLowLightBoostActive = strength > 0.5
                    )
                } else {
                    // Nothing has changed
                    this
                }
            }
        }
    }
}
