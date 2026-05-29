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
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState

/**
 * Creates a [ConcurrentCameraUiState] based on the current camera and system state.
 *
 * This function determines the availability and current selection of the concurrent camera feature.
 * It synthesizes various states and settings to decide if the concurrent camera mode can be
 * enabled. The feature is considered disabled if any of the following conditions are true:
 * - The device hardware does not support concurrent cameras.
 * - The camera was launched with an external intent for single image capture.
 * - The selected capture mode is exclusively for single image capture (`IMAGE_ONLY`).
 * - An HDR mode (either HLG10 for video or ULTRA_HDR for images) is active.
 * - The stream configuration is set to `SINGLE_STREAM`.
 * - Low Light Boost flash mode is active.
 *
 * @param cameraAppSettings The current application-level camera settings.
 * @param systemConstraints The capabilities and limitations of the device's camera hardware.
 * @param externalCaptureMode The mode indicating if the camera was launched by an external intent.
 * @param captureModeUiState The current state of the capture mode selection UI.
 * @param streamConfigUiState The current state of the stream configuration UI.
 * @return A [ConcurrentCameraUiState.Available] object containing the currently selected
 * concurrent camera mode and a boolean indicating if the feature is currently enabled and
 * can be interacted with.
 */
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
                ) && (
                cameraAppSettings.flashMode != FlashMode.LOW_LIGHT_BOOST
                )
    )
}
