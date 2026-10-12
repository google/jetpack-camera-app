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

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import org.junit.Test

class AudioUiStateAdapterTest {

    @Test
    fun audioDisabled_returnsMutedState() {
        val appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(audioEnabled = false)
        val state = AudioUiState.Companion.from(appSettings, CameraState())

        assertThat(state).isEqualTo(AudioUiState.Enabled.Mute)
    }

    @Test
    fun audioEnabled_videoInactive_returnsOnButNotActive() {
        val appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(audioEnabled = true)
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Inactive()
        )
        val state = AudioUiState.Companion.from(appSettings, cameraState)

        assertThat(state).isEqualTo(AudioUiState.Enabled.On(0.0, false))
    }

    @Test
    fun audioEnabled_videoActive_audioActive_returnsOnWithAmplitude() {
        val appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(audioEnabled = true)
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Recording(
                maxDurationMillis = 0L,
                audioStreamState = AudioStreamState.Active(amplitude = 0.5),
                elapsedTimeNanos = 0L
            )
        )
        val state = AudioUiState.Companion.from(appSettings, cameraState)

        assertThat(state).isEqualTo(AudioUiState.Enabled.On(0.5, true))
    }

    @Test
    fun audioEnabled_videoActive_audioSilenced_returnsOnButSilenced() {
        val appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(audioEnabled = true)
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Recording(
                maxDurationMillis = 0L,
                audioStreamState = AudioStreamState.Silenced,
                elapsedTimeNanos = 0L
            )
        )
        val state = AudioUiState.Companion.from(appSettings, cameraState)

        assertThat(state).isEqualTo(AudioUiState.Enabled.On(0.0, true))
    }

    @Test
    fun audioEnabled_videoActive_audioError_returnsOnButNotActive() {
        val appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(audioEnabled = true)
        val cameraState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Recording(
                maxDurationMillis = 0L,
                audioStreamState = AudioStreamState.Error,
                elapsedTimeNanos = 0L
            )
        )
        val state = AudioUiState.Companion.from(appSettings, cameraState)

        assertThat(state).isEqualTo(AudioUiState.Enabled.On(0.0, false))
    }
}
