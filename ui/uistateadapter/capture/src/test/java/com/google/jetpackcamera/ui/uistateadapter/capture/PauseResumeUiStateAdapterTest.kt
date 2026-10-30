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

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.ui.uistate.capture.PauseResumeUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PauseResumeUiStateAdapterTest {

    @Test
    fun from_inactiveRecording_returnsHidden() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Inactive()
        )
        assertThat(PauseResumeUiState.from(cameraState, isRecordingLocked = false))
            .isEqualTo(PauseResumeUiState.Hidden)
        assertThat(PauseResumeUiState.from(cameraState, isRecordingLocked = true))
            .isEqualTo(PauseResumeUiState.Hidden)
    }

    @Test
    fun from_startingRecording_returnsHidden() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Starting()
        )
        assertThat(PauseResumeUiState.from(cameraState, isRecordingLocked = false))
            .isEqualTo(PauseResumeUiState.Hidden)
        assertThat(PauseResumeUiState.from(cameraState, isRecordingLocked = true))
            .isEqualTo(PauseResumeUiState.Hidden)
    }

    @Test
    fun from_activeRecordingAndNotLocked_returnsHidden() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Recording(
                maxDurationMillis = 0L,
                audioStreamState = AudioStreamState.Active(0.0),
                elapsedTimeNanos = 1000L
            )
        )
        assertThat(PauseResumeUiState.from(cameraState, isRecordingLocked = false))
            .isEqualTo(PauseResumeUiState.Hidden)
    }

    @Test
    fun from_activeRecordingAndLocked_returnsVisibleNotPaused() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Recording(
                maxDurationMillis = 0L,
                audioStreamState = AudioStreamState.Active(0.0),
                elapsedTimeNanos = 1000L
            )
        )
        assertThat(PauseResumeUiState.from(cameraState, isRecordingLocked = true))
            .isEqualTo(PauseResumeUiState.Visible(isPaused = false))
    }

    @Test
    fun from_pausedRecordingAndLocked_returnsVisiblePaused() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Paused(
                maxDurationMillis = 0L,
                audioStreamState = AudioStreamState.Active(0.0),
                elapsedTimeNanos = 1000L
            )
        )
        assertThat(PauseResumeUiState.from(cameraState, isRecordingLocked = true))
            .isEqualTo(PauseResumeUiState.Visible(isPaused = true))
    }
}
