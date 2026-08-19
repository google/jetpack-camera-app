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
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ElapsedTimeUiStateAdapterTest {

    @Test
    fun from_activeRecordingState_returnsEnabledWithCorrectNanosAndNotPaused() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Recording(
                maxDurationMillis = 0L,
                audioAmplitude = 0.0,
                elapsedTimeNanos = 123456L
            )
        )
        val state = ElapsedTimeUiState.from(cameraState)
        assertThat(state).isInstanceOf(ElapsedTimeUiState.Enabled::class.java)
        val enabledState = state as ElapsedTimeUiState.Enabled
        assertThat(enabledState.elapsedTimeNanos).isEqualTo(123456L)
        assertThat(enabledState.isPaused).isFalse()
    }

    @Test
    fun from_pausedRecordingState_returnsEnabledWithCorrectNanosAndPaused() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Paused(
                maxDurationMillis = 0L,
                audioAmplitude = 0.0,
                elapsedTimeNanos = 654321L
            )
        )
        val state = ElapsedTimeUiState.from(cameraState)
        assertThat(state).isInstanceOf(ElapsedTimeUiState.Enabled::class.java)
        val enabledState = state as ElapsedTimeUiState.Enabled
        assertThat(enabledState.elapsedTimeNanos).isEqualTo(654321L)
        assertThat(enabledState.isPaused).isTrue()
    }

    @Test
    fun from_inactiveRecordingState_returnsEnabledWithFinalNanosAndNotPaused() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Inactive(finalElapsedTimeNanos = 7890L)
        )
        val state = ElapsedTimeUiState.from(cameraState)
        assertThat(state).isInstanceOf(ElapsedTimeUiState.Enabled::class.java)
        val enabledState = state as ElapsedTimeUiState.Enabled
        assertThat(enabledState.elapsedTimeNanos).isEqualTo(7890L)
        assertThat(enabledState.isPaused).isFalse()
    }

    @Test
    fun from_startingRecordingState_returnsEnabledWithZeroNanosAndNotPaused() {
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Starting()
        )
        val state = ElapsedTimeUiState.from(cameraState)
        assertThat(state).isInstanceOf(ElapsedTimeUiState.Enabled::class.java)
        val enabledState = state as ElapsedTimeUiState.Enabled
        assertThat(enabledState.elapsedTimeNanos).isEqualTo(0L)
        assertThat(enabledState.isPaused).isFalse()
    }
}
