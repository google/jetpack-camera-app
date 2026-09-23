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
import com.google.jetpackcamera.model.CameraError
import com.google.jetpackcamera.ui.uistate.capture.CameraErrorUiState
import org.junit.Test

class CameraErrorUiStateAdapterTest {

    @Test
    fun from_whenNoCameraError_returnsHidden() {
        val state = CameraErrorUiState.from(
            cameraState = CameraState(cameraError = null),
            acknowledgedError = null
        )
        assertThat(state).isEqualTo(CameraErrorUiState.Hidden)
    }

    @Test
    fun from_whenErrorMatchesAcknowledgedError_returnsHidden() {
        val state = CameraErrorUiState.from(
            cameraState = CameraState(cameraError = CameraError.CameraInUse),
            acknowledgedError = CameraError.CameraInUse
        )
        assertThat(state).isEqualTo(CameraErrorUiState.Hidden)
    }

    @Test
    fun from_whenDifferentErrorOccursAfterAcknowledgement_returnsShowing() {
        val state = CameraErrorUiState.from(
            cameraState = CameraState(cameraError = CameraError.FatalCameraError),
            acknowledgedError = CameraError.CameraInUse
        )
        assertThat(state).isInstanceOf(CameraErrorUiState.Showing::class.java)
        val showing = state as CameraErrorUiState.Showing
        assertThat(showing.error).isEqualTo(CameraError.FatalCameraError)
        assertThat(showing.shouldExitAppOnConfirm).isTrue()
    }

    @Test
    fun from_cameraInUse_isRecoverableAndDoesNotExitApp() {
        val state = CameraErrorUiState.from(
            cameraState = CameraState(cameraError = CameraError.CameraInUse),
            acknowledgedError = null
        ) as CameraErrorUiState.Showing

        assertThat(state.titleResId).isEqualTo(R.string.picker_camera_error_in_use_title)
        assertThat(state.bodyResId).isEqualTo(R.string.picker_camera_error_in_use_body)
        assertThat(state.shouldExitAppOnConfirm).isFalse()
    }

    @Test
    fun from_cameraDisabledByPolicy_hasNullBodyAndExitsApp() {
        val state = CameraErrorUiState.from(
            cameraState = CameraState(cameraError = CameraError.CameraDisabledByPolicy),
            acknowledgedError = null
        ) as CameraErrorUiState.Showing

        assertThat(state.titleResId).isEqualTo(R.string.picker_camera_error_disabled_title)
        assertThat(state.bodyResId).isNull()
        assertThat(state.shouldExitAppOnConfirm).isTrue()
    }

    @Test
    fun from_insufficientStorage_doesNotExitApp() {
        val state = CameraErrorUiState.from(
            cameraState = CameraState(cameraError = CameraError.InsufficientStorage),
            acknowledgedError = null
        ) as CameraErrorUiState.Showing

        assertThat(state.titleResId).isEqualTo(R.string.picker_camera_error_storage_title)
        assertThat(state.bodyResId).isEqualTo(R.string.picker_camera_error_storage_body)
        assertThat(state.shouldExitAppOnConfirm).isFalse()
    }
}
