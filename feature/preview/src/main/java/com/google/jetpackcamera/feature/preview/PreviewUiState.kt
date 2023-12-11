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

import android.widget.Toast
import androidx.camera.core.CameraSelector
import com.google.jetpackcamera.feature.preview.ui.ToastMessage
import com.google.jetpackcamera.settings.model.CameraAppSettings

/**
 * Defines the current state of the [PreviewScreen].
 */
data class PreviewUiState(
    val cameraState: CameraState = CameraState.NOT_READY,
    // "quick" settings
    val currentCameraSettings: CameraAppSettings,
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val videoRecordingState: VideoRecordingState = VideoRecordingState.INACTIVE,
    val quickSettingsIsOpen: Boolean = false,
    //todo: remove after implementing post capture screen
    val toastMessageToShow: ToastMessage? = null
)

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

/**
 * Defines the current state of the camera.
 */
enum class CameraState {
    /**
     * Camera hasn't been initialized.
     */
    NOT_READY,

    /**
     * Camera is open and presenting a preview stream.
     */
    READY
}
