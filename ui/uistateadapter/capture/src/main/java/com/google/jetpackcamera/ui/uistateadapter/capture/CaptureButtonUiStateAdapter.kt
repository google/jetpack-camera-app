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
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState

/**
 * Creates a [CaptureButtonUiState] based on the current camera settings and state.
 *
 * This function determines the UI state for the capture button based on the capture mode, video
 * recording status, and whether the recording has been locked.
 *
 * @param cameraAppSettings The current application settings, used to determine the capture mode.
 * @param cameraState The current state of the camera, used to check video recording status.
 * @param lockedState A boolean indicating whether the video recording is currently in a locked state.
 *
 * @return A [CaptureButtonUiState] representing the current state of the capture button.
 *         - [CaptureButtonUiState.Enabled.Idle] if not recording.
 *         - [CaptureButtonUiState.Enabled.Recording.PressedRecording] if recording is active but not locked.
 *         - [CaptureButtonUiState.Enabled.Recording.LockedRecording] if recording is active and locked.
 */
fun CaptureButtonUiState.Companion.from(
    cameraAppSettings: CameraAppSettings,
    cameraState: CameraState,
    lockedState: Boolean
): CaptureButtonUiState = when (cameraState.videoRecordingState) {
    // if not currently recording, check capturemode to determine idle capture button UI
    is VideoRecordingState.Inactive ->
        CaptureButtonUiState
            .Enabled.Idle(captureMode = cameraAppSettings.captureMode)

    // display different capture button UI depending on if recording is pressed or locked
    is VideoRecordingState.Active.Recording, is VideoRecordingState.Active.Paused ->
        if (lockedState) {
            CaptureButtonUiState.Enabled.Recording.LockedRecording
        } else {
            CaptureButtonUiState.Enabled.Recording.PressedRecording
        }

    is VideoRecordingState.Starting ->
        CaptureButtonUiState
            .Enabled.Idle(captureMode = cameraAppSettings.captureMode)
}
