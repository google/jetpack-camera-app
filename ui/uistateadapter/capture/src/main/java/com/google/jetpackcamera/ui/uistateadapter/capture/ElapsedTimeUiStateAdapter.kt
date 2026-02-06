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
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState

/**
 * Creates an [ElapsedTimeUiState] based on the current [CameraState].
 *
 * This function translates the [VideoRecordingState] from the core camera layer into a UI-specific
 * state for displaying the elapsed time of a video recording. It handles different recording
 * phases: active recording, the final time after recording stops, and the initial state when
 * a recording is starting.
 *
 * @param cameraState The real-time state from the camera, which includes the video recording status.
 *
 * @return An [ElapsedTimeUiState.Enabled] state containing the elapsed time in nanoseconds.
 *         - For [VideoRecordingState.Active], it provides the ongoing elapsed time.
 *         - For [VideoRecording_State.Inactive], it provides the final duration of the last recording.
 *         - For [VideoRecordingState.Starting], it provides an initial value of 0.
 */
fun ElapsedTimeUiState.Companion.from(cameraState: CameraState): ElapsedTimeUiState {
    val videoRecordingState = cameraState.videoRecordingState
    return when (videoRecordingState) {
        is VideoRecordingState.Active ->
            ElapsedTimeUiState.Enabled(videoRecordingState.elapsedTimeNanos)

        is VideoRecordingState.Inactive ->
            ElapsedTimeUiState.Enabled(videoRecordingState.finalElapsedTimeNanos)

        is VideoRecordingState.Starting -> ElapsedTimeUiState.Enabled(0L)
    }
}
