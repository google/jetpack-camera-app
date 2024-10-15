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

import com.google.jetpackcamera.feature.preview.ui.SnackbarData
import com.google.jetpackcamera.feature.preview.ui.ToastMessage
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.SystemConstraints

/**
 * Defines the current state of the [PreviewScreen].
 */
sealed interface PreviewUiState {
    data object NotReady : PreviewUiState

    data class Ready(
        // "quick" settings
        val currentCameraSettings: CameraAppSettings,
        val systemConstraints: SystemConstraints,
        val zoomScale: Float = 1f,
        val videoRecordingState: VideoRecordingState = VideoRecordingState.INACTIVE,
        val quickSettingsIsOpen: Boolean = false,
        val audioAmplitude: Double = 0.0,
        val audioMuted: Boolean = false,
        val recordingElapsedTimeNanos: Long = 0L,

        // todo: remove after implementing post capture screen
        val toastMessageToShow: ToastMessage? = null,
        val snackBarToShow: SnackbarData? = null,
        val lastBlinkTimeStamp: Long = 0,
        val previewMode: PreviewMode,
        val captureModeToggleUiState: CaptureModeToggleUiState,
        val sessionFirstFrameTimestamp: Long = 0L,
        val currentPhysicalCameraId: String? = null,
        val currentLogicalCameraId: String? = null,
        val isDebugMode: Boolean = false,
    ) : PreviewUiState
}
//todo(kc): add
/**
 * Defines the current state of Video Recording
 */
enum class VideoRecordingState {
    /**
     * Camera is not currently recording a video
     */
    INACTIVE,

    /**
     * Camera is currently recording a video
     */
    ACTIVE
}
