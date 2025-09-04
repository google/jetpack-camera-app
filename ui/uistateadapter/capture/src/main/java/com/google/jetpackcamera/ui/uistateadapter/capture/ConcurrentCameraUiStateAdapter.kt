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

import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DEFAULT_HDR_DYNAMIC_RANGE
import com.google.jetpackcamera.model.DEFAULT_HDR_IMAGE_OUTPUT
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState

fun ConcurrentCameraUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints,
    externalCaptureMode: ExternalCaptureMode,
    captureModeUiState: CaptureModeUiState,
    streamConfigUiState: StreamConfigUiState
): ConcurrentCameraUiState {
    return ConcurrentCameraUiState.Available(
        selectedConcurrentCameraMode = cameraAppSettings.concurrentCameraMode,
        isEnabled = systemConstraints.concurrentCamerasSupported &&
            externalCaptureMode != ExternalCaptureMode.ImageCapture && (
                (
                    captureModeUiState as?
                        CaptureModeUiState.Available
                    )
                    ?.selectedCaptureMode !=
                    CaptureMode.IMAGE_ONLY
                ) && (
                cameraAppSettings.dynamicRange !=
                    DEFAULT_HDR_DYNAMIC_RANGE &&
                    cameraAppSettings.imageFormat !=
                    DEFAULT_HDR_IMAGE_OUTPUT
                ) && !(
                streamConfigUiState is StreamConfigUiState.Available &&
                    streamConfigUiState.selectedStreamConfig == StreamConfig.SINGLE_STREAM
                )
    )
}
