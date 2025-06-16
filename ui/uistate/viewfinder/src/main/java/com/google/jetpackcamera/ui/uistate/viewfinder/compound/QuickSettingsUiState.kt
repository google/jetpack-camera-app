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

package com.google.jetpackcamera.ui.uistate.viewfinder.compound

import com.google.jetpackcamera.ui.uistate.viewfinder.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.HdrUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.StreamConfigUiState

sealed interface QuickSettingsUiState {
    data object Unavailable : QuickSettingsUiState

    data class Available(
        val aspectRatioUiState: AspectRatioUiState,
        val captureModeUiState: CaptureModeUiState,
        val concurrentCameraUiState: ConcurrentCameraUiState,
        val flashModeUiState: FlashModeUiState,
        val flipLensUiState: FlipLensUiState,
        val hdrUiState: HdrUiState,
        val streamConfigUiState: StreamConfigUiState,
        val quickSettingsIsOpen: Boolean = false
    ) : QuickSettingsUiState
}