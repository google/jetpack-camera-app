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
package com.google.jetpackcamera.ui.uistateadapter.capture.compound

import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.from

fun QuickSettingsUiState.Companion.from(
    captureModeUiState: CaptureModeUiState,
    flashModeUiState: FlashModeUiState,
    flipLensUiState: FlipLensUiState,
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints,
    aspectRatioUiState: AspectRatioUiState,
    hdrUiState: HdrUiState,
    quickSettingsIsOpen: Boolean,
    focusedQuickSetting: FocusedQuickSetting,
    externalCaptureMode: ExternalCaptureMode
): QuickSettingsUiState {
    val streamConfigUiState = StreamConfigUiState.from(cameraAppSettings)
    return QuickSettingsUiState.Available(
        aspectRatioUiState = aspectRatioUiState,
        captureModeUiState = captureModeUiState,
        concurrentCameraUiState = ConcurrentCameraUiState.from(
            cameraAppSettings,
            systemConstraints,
            externalCaptureMode,
            captureModeUiState,
            streamConfigUiState
        ),
        flashModeUiState = flashModeUiState,
        flipLensUiState = flipLensUiState,
        hdrUiState = hdrUiState,
        streamConfigUiState = streamConfigUiState,
        quickSettingsIsOpen = quickSettingsIsOpen,
        focusedQuickSetting = focusedQuickSetting
    )
}
