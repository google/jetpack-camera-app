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
package com.google.jetpackcamera.ui.uistate.capture.compound

import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState

/**
 * Defines the UI state for the quick settings panel.
 *
 */
sealed interface QuickSettingsUiState {
    /**
     * The quick settings are unavailable and should be hidden or disabled.
     * This is typically the case when the camera is in a state that does not allow settings
     * adjustment, such as during video recording.
     */
    data object Unavailable : QuickSettingsUiState

    /**
     * The quick settings are available for user interaction.
     *
     * @param aspectRatioUiState The UI state for the aspect ratio setting.
     * @param captureModeUiState The UI state for the capture mode (e.g., photo vs. video) setting.
     * @param concurrentCameraUiState The UI state for concurrent camera mode.
     * @param flashModeUiState The UI state for the flash mode setting.
     * @param flipLensUiState The UI state for the flip lens (front/back camera) button.
     * @param hdrUiState The UI state for the HDR (High Dynamic Range) setting.
     * @param streamConfigUiState The UI state for stream configuration.
     * @param quickSettingsIsOpen Indicates whether the quick settings panel is currently open.
     * @param focusedQuickSetting The specific quick setting that is currently focused by the user,
     * allowing for more detailed interaction (e.g., showing a sub-menu).
     */
    data class Available(
        val aspectRatioUiState: AspectRatioUiState,
        val captureModeUiState: CaptureModeUiState,
        val concurrentCameraUiState: ConcurrentCameraUiState,
        val flashModeUiState: FlashModeUiState,
        val flipLensUiState: FlipLensUiState,
        val hdrUiState: HdrUiState,
        val streamConfigUiState: StreamConfigUiState,
        val quickSettingsIsOpen: Boolean = false,
        val focusedQuickSetting: FocusedQuickSetting = FocusedQuickSetting.NONE
    ) : QuickSettingsUiState

    companion object
}

/**
 * Represents which individual quick setting is currently focused by the user.
 *
 * When a quick setting is focused, the UI may highlight it or show a sub-panel with more options
 * related to that setting.
 */
enum class FocusedQuickSetting {
    NONE,
    ASPECT_RATIO,
    CAPTURE_MODE
}
