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
        val state = CameraErrorUiState.from(CameraState(cameraError = null))
        assertThat(state).isEqualTo(CameraErrorUiState.Hidden)
    }

    @Test
    fun from_whenCameraErrorPresent_returnsShowing() {
        val state = CameraErrorUiState.from(CameraState(cameraError = CameraError.CameraInUse))
        assertThat(state).isEqualTo(CameraErrorUiState.Showing(CameraError.CameraInUse))
    }
}
