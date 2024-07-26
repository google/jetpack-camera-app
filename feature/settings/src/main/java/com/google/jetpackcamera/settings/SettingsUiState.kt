/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera.settings
import com.google.jetpackcamera.settings.DisabledRationale.DeviceUnsupportedRationale
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.ui.FPS_60

/**
 * Defines the current state of the [SettingsScreen].
 */
sealed interface SettingsUiState {
    data object Disabled : SettingsUiState
    data class Enabled(
        val aspectRatioUiState: AspectRatioUiState,
        val captureModeUiState: CaptureModeUiState,
        val darkModeUiState: DarkModeUiState,
        val flashUiState: FlashUiState,
        val fpsUiState: FpsUiState,
        val lensFlipUiState: FlipLensUiState,
        val stabilizationUiState: StabilizationUiState
    ) : SettingsUiState
}

/**
 * Settings Ui State for testing, based on Typical System Constraints.
 * @see[com.google.jetpackcamera.settings.model.SystemConstraints]
 */
val TYPICAL_SETTINGS_UISTATE = SettingsUiState.Enabled(
    aspectRatioUiState = AspectRatioUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
    captureModeUiState = CaptureModeUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
    darkModeUiState = DarkModeUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.darkMode),
    flashUiState =
    FlashUiState.Enabled(currentFlashMode = DEFAULT_CAMERA_APP_SETTINGS.flashMode),
    fpsUiState = FpsUiState.Enabled(
        currentSelection = DEFAULT_CAMERA_APP_SETTINGS.targetFrameRate,
        fpsAutoState = SingleSelectableState.Selectable,
        fpsFifteenState = SingleSelectableState.Selectable,
        fpsThirtyState = SingleSelectableState.Selectable,
        fpsSixtyState = SingleSelectableState.Disabled(setOf(DeviceUnsupportedRationale(
            FORMAT_FPS_PREFIX.format(FPS_60)))
    )),
    lensFlipUiState = FlipLensUiState.Enabled(DEFAULT_CAMERA_APP_SETTINGS.cameraLensFacing),
    stabilizationUiState =
    StabilizationUiState.Disabled(setOf(DeviceUnsupportedRationale(STABILIZATION_SETTING_PREFIX)))
)