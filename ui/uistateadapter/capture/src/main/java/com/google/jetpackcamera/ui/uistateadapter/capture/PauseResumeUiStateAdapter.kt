/*
 * Copyright (C) 2026 The Android Open Source Project
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
import com.google.jetpackcamera.ui.uistate.capture.PauseResumeUiState

/**
 * Creates a [PauseResumeUiState] based on the current [CameraState] and whether recording is locked.
 *
 * The pause/resume button is only [PauseResumeUiState.Visible] when a video recording is active
 * and locked for hands-free recording.
 *
 * @param cameraState The real-time state from the camera, which includes the video recording status.
 * @param isRecordingLocked Whether the current video recording is locked.
 * @return A [PauseResumeUiState] representing the visibility and paused state of the button.
 */
fun PauseResumeUiState.Companion.from(
    cameraState: CameraState,
    isRecordingLocked: Boolean
): PauseResumeUiState {
    val videoRecordingState = cameraState.videoRecordingState
    return if (isRecordingLocked && videoRecordingState is VideoRecordingState.Active) {
        PauseResumeUiState.Visible(
            isPaused = videoRecordingState is VideoRecordingState.Active.Paused
        )
    } else {
        PauseResumeUiState.Hidden
    }
}
