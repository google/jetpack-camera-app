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
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState

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
