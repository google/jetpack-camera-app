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
package com.google.jetpackcamera.feature.preview

import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.feature.preview.ui.SnackbarData
import com.google.jetpackcamera.feature.preview.ui.ToastMessage
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.SystemConstraints

/**
 * Defines the current state of the [PreviewScreen].
 */
sealed interface PreviewUiState {
    data object NotReady : PreviewUiState

    data class Ready(
        // "quick" settings
        val currentCameraSettings: CameraAppSettings = CameraAppSettings(),
        val systemConstraints: SystemConstraints = SystemConstraints(),
        val zoomScale: Float = 1f,
        val videoRecordingState: VideoRecordingState = VideoRecordingState.Inactive(),
        val quickSettingsIsOpen: Boolean = false,
        val audioMuted: Boolean = false,

        // todo: remove after implementing post capture screen
        val toastMessageToShow: ToastMessage? = null,
        val snackBarToShow: SnackbarData? = null,
        val lastBlinkTimeStamp: Long = 0,
        val previewMode: PreviewMode = PreviewMode.StandardMode {},
        val captureModeToggleUiState: CaptureModeToggleUiState = CaptureModeToggleUiState.Invisible,
        val sessionFirstFrameTimestamp: Long = 0L,
        val currentPhysicalCameraId: String? = null,
        val currentLogicalCameraId: String? = null,
        val isDebugMode: Boolean = false,
        val stabilizationUiState: StabilizationUiState = StabilizationUiState.Disabled,
        val flashModeUiState: FlashModeUiState = FlashModeUiState.Unavailable
    ) : PreviewUiState
}
// todo(kc): add ElapsedTimeUiState class

sealed interface StabilizationUiState {
    data object Disabled : StabilizationUiState

    data class Set(
        val stabilizationMode: StabilizationMode,
        val active: Boolean = true
    ) : StabilizationUiState
}

sealed interface FlashModeUiState {
    data object Unavailable : FlashModeUiState

    data class Available(
        val currentFlashMode: FlashMode,
        val availableFlashModes: List<FlashMode>
    ) : FlashModeUiState
}
