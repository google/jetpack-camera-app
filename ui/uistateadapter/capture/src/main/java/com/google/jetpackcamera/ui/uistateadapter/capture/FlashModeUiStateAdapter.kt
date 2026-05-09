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
import com.google.jetpackcamera.ui.components.capture.DisabledReason
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState.Available
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState.Unavailable
// Assuming Utils.getSelectableListFromValues is no longer needed with the new logic
// import com.google.jetpackcamera.ui.uistateadapter.Utils

private val ORDERED_UI_SUPPORTED_FLASH_MODES = listOf(
    FlashMode.OFF,
    FlashMode.AUTO,
    FlashMode.ON,
    FlashMode.LOW_LIGHT_BOOST
)

/**
 * Creates the initial [FlashModeUiState] from the given camera settings, system constraints,
 * and a set of flash modes designated to be visible.
 *
 * This factory function determines the set of displayable flash modes based on:
 * 1.  Overall device support.
 * 2.  Developer-defined visibility (via `visibleFlashModes`).
 * 3.  Support by the currently active lens.
 * 4.  Interactions with other settings (e.g., HDR, Concurrent Camera).
 *
 * Modes not supported by the device or not in `visibleFlashModes` are hidden.
 * Modes not supported by the current lens are shown as disabled.
 * Modes supported by the current lens are shown as enabled.
 *
 * @param cameraAppSettings The current settings of the camera.
 * @param systemConstraints The hardware capabilities of the camera system.
 * @return A [FlashModeUiState] which is either [Available] or [Unavailable].
 */
fun FlashModeUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints
    // todo(kc): supply visible flash modes from developer options
    // visibleFlashModes: Set<FlashMode> = ORDERED_UI_SUPPORTED_FLASH_MODES.toSet()
): FlashModeUiState {
    val selectedFlashMode = cameraAppSettings.flashMode

    // All modes potentially supported by the device
    val allDeviceSupportedFlashModes = buildSet<FlashMode> {
        systemConstraints.perLensConstraints.let {
            it.keys.forEach { key ->
                it[key]?.supportedFlashModes?.let { flashModes -> addAll(flashModes) }
            }
        }
    }

    // Modes supported by the CURRENT lens
    val currentLensSupportedFlashModes = systemConstraints.forCurrentLens(cameraAppSettings)
        ?.supportedFlashModes ?: setOf(FlashMode.OFF)

    val displayableModes = mutableListOf<SingleSelectableUiState<FlashMode>>()

    for (mode in ORDERED_UI_SUPPORTED_FLASH_MODES) {
        // 1. Hide if not supported by the device at all.
        if (!allDeviceSupportedFlashModes.contains(mode)) {
            continue
        }

        // 2. Hide if not designated as visible by the developer.
        // todo(kc): supply visible flash modes from developer options
        /*if (!visibleFlashModes.contains(mode)) {
            continue
        }*/

        // 3. Special handling for LOW_LIGHT_BOOST based on other settings.
        if (mode == FlashMode.LOW_LIGHT_BOOST) {
            if (cameraAppSettings.dynamicRange != DynamicRange.SDR ||
                cameraAppSettings.concurrentCameraMode == ConcurrentCameraMode.DUAL
            ) {
                continue // Hide LLB if HDR or Dual Camera is active
            }
        }

        // 4. Determine if Enabled or Disabled based on current lens support.
        if (currentLensSupportedFlashModes.contains(mode)) {
            displayableModes.add(SingleSelectableUiState.SelectableUi(mode)) // Enabled
        } else {
            // todo(kc): add actual disabledreason for flash mode
            displayableModes.add(
                SingleSelectableUiState.Disabled(
                    value = mode,
                    disabledReason = DisabledReason.HDR_IMAGE_UNSUPPORTED_ON_LENS
                )
            ) // Disabled
        }
    }

    // UiState should be Unavailable if no modes are displayable,
    // or if only OFF is displayable and it's selectable.
    val onlyOffSelectable = displayableModes.size == 1 &&
        displayableModes[0].value == FlashMode.OFF &&
        displayableModes[0] is SingleSelectableUiState.SelectableUi

    return if (displayableModes.isEmpty() || onlyOffSelectable) {
        Unavailable
    } else {
        Available(
            selectedFlashMode = selectedFlashMode,
            availableFlashModes = displayableModes,
            isLowLightBoostActive = false // Initial state
        )
    }
}

/**
 * Updates an existing [FlashModeUiState] based on new camera settings and state.
 *
 * @param cameraAppSettings The current application settings for the camera.
 * @param systemConstraints The hardware capabilities of the camera system.
 * @param cameraState The real-time state from the camera, used to check [LowLightBoostState].
 * @return An updated [FlashModeUiState].
 */
fun FlashModeUiState.updateFrom(
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints,
    cameraState: CameraState
): FlashModeUiState {
    return when (this) {
        is Unavailable -> {
            // When previous state was "Unavailable", we'll try to create a new FlashModeUiState
            FlashModeUiState.from(cameraAppSettings, systemConstraints)
        }

        is Available -> {
            // Regenerate the potential new state based on the latest settings
            val newUiState = FlashModeUiState.from(cameraAppSettings, systemConstraints)

            if (newUiState is Unavailable) {
                newUiState // Switch to Unavailable
            } else {
                newUiState as Available // Cast to Available

                // Check if the list of modes or their enabled/disabled states have changed.
                // Data class list comparison works well here.
                if (this.availableFlashModes != newUiState.availableFlashModes) {
                    newUiState
                } else if (this.selectedFlashMode != cameraAppSettings.flashMode) {
                    // Only the selection changed
                    copy(selectedFlashMode = cameraAppSettings.flashMode)
                } else {
                    // Check for Low Light Boost state changes if it's the selected mode
                    if (cameraAppSettings.flashMode == FlashMode.LOW_LIGHT_BOOST) {
                        val strength = when (val llbState = cameraState.lowLightBoostState) {
                            is LowLightBoostState.Active -> llbState.strength
                            else -> LowLightBoostState.MINIMUM_STRENGTH
                        }
                        val newIsLowLightBoostActive = strength > 0.5
                        if (this.isLowLightBoostActive != newIsLowLightBoostActive) {
                            copy(isLowLightBoostActive = newIsLowLightBoostActive)
                        } else {
                            this // Nothing changed
                        }
                    } else {
                        this // Nothing changed
                    }
                }
            }
        }
    }
}
