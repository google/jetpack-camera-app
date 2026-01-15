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
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CaptureButtonUiStateAdapterTest {
    private val defaultCameraAppSettings = CameraAppSettings(captureMode = CaptureMode.STANDARD)
    private val defaultCameraState = CameraState(isCameraRunning = true)

    @Test
    fun from_cameraNotRunning_returnsIdleAndDisabled() {
        val cameraState = CameraState(isCameraRunning = false)
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Available.Idle::class.java)
        assertThat(uiState.isEnabled).isFalse()
        assertThat((uiState as CaptureButtonUiState.Available.Idle).captureMode)
            .isEqualTo(CaptureMode.STANDARD)
    }

    @Test
    fun from_cameraRunning_recordingInactive_returnsIdleAndEnabled() {
        val cameraState = defaultCameraState.copy(
            videoRecordingState = VideoRecordingState.Inactive()
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Available.Idle::class.java)
        assertThat(uiState.isEnabled).isTrue()
        assertThat((uiState as CaptureButtonUiState.Available.Idle).captureMode)
            .isEqualTo(CaptureMode.STANDARD)
    }

    @Test
    fun from_cameraRunning_recordingPressed_returnsPressedRecording() {
        val cameraState = defaultCameraState.copy(
            videoRecordingState = VideoRecordingState.Active.Recording(0L, 0.0, 0L)
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState)
            .isInstanceOf(CaptureButtonUiState.Available.Recording.PressedRecording::class.java)
        assertThat(uiState.isEnabled).isTrue()
    }

    @Test
    fun from_cameraRunning_recordingLocked_returnsLockedRecording() {
        val cameraState = defaultCameraState.copy(
            videoRecordingState = VideoRecordingState.Active.Recording(0L, 0.0, 0L)
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = true
        )

        assertThat(uiState)
            .isInstanceOf(CaptureButtonUiState.Available.Recording.LockedRecording::class.java)
        assertThat(uiState.isEnabled).isTrue()
    }

    @Test
    fun from_cameraRunning_recordingStarting_returnsIdleAndEnabled() {
        val cameraState = defaultCameraState.copy(
            videoRecordingState = VideoRecordingState.Starting(null)
        )
        val uiState = CaptureButtonUiState.from(
            defaultCameraAppSettings,
            cameraState,
            lockedState = false
        )

        assertThat(uiState).isInstanceOf(CaptureButtonUiState.Available.Idle::class.java)
        assertThat(uiState.isEnabled).isTrue()
    }
}
