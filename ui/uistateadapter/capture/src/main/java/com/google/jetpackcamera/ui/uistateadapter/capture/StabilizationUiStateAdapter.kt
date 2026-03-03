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
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState

/**
 * Creates a [StabilizationUiState] based on the current camera settings and real-time camera state.
 *
 * This function translates the user's intended stabilization setting (`expectedMode`) and the
 * camera's actual, currently active stabilization (`actualMode`) into a UI-specific state.
 * It handles the differences between what the user has selected and what the hardware is
 * currently providing.
 *
 * @param cameraAppSettings The current application settings, which contain the user's desired
 *   [StabilizationMode].
 * @param cameraState The real-time state from the camera, which reports the currently active
 *   [StabilizationMode].
 *
 * @return A [StabilizationUiState] representing the current stabilization status.
 * - [StabilizationUiState.Disabled] if the user setting is `OFF`, or if `AUTO` is selected but
 *   the camera is not actively using `ON` or `OPTICAL` stabilization.
 * - [StabilizationUiState.Auto] if the user setting is `AUTO` and the camera has resolved it
 *   to an active stabilization mode (e.g., `ON` or `OPTICAL`).
 * - [StabilizationUiState.Specific] if the user has selected a specific mode like `ON`,
 *   `OPTICAL`, or `HIGH_QUALITY`. The `active` property will indicate if the camera's
 *   `actualMode` matches the user's `expectedMode`.
 *
 * @throws IllegalStateException if the `cameraState` reports `StabilizationMode.AUTO`, as the
 *   camera implementation should always resolve `AUTO` to a specific, active mode (`ON`,
 *   `OPTICAL`, or `OFF`).
 */
fun StabilizationUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    cameraState: CameraState
): StabilizationUiState {
    val expectedMode = cameraAppSettings.stabilizationMode
    val actualMode = cameraState.stabilizationMode
    check(actualMode != StabilizationMode.AUTO) {
        "CameraState should never resolve to AUTO stabilization mode"
    }
    return when (expectedMode) {
        StabilizationMode.OFF -> StabilizationUiState.Disabled
        StabilizationMode.AUTO -> {
            if (actualMode !in setOf(StabilizationMode.ON, StabilizationMode.OPTICAL)) {
                StabilizationUiState.Disabled
            } else {
                StabilizationUiState.Auto(actualMode)
            }
        }

        StabilizationMode.ON,
        StabilizationMode.HIGH_QUALITY,
        StabilizationMode.OPTICAL ->
            StabilizationUiState.Specific(
                stabilizationMode = expectedMode,
                active = expectedMode == actualMode
            )
    }
}
