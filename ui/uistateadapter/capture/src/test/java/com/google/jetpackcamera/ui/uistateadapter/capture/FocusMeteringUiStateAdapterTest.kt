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

import androidx.compose.ui.geometry.Offset
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.FocusState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FocusMeteringUiStateAdapterTest {

    @Test
    fun from_unspecifiedFocusState_returnsUnspecifiedUiState() {
        val cameraState = CameraState(focusState = FocusState.Unspecified)
        val uiState = FocusMeteringUiState.from(cameraState)
        assertThat(uiState).isEqualTo(FocusMeteringUiState.Unspecified)
    }

    @Test
    fun from_runningFocusState_returnsRunningUiState() {
        val cameraState = CameraState(
            focusState = FocusState.Specified(
                x = 100f,
                y = 200f,
                status = FocusState.Status.RUNNING
            )
        )
        val uiState = FocusMeteringUiState.from(cameraState)
        assertThat(uiState).isEqualTo(
            FocusMeteringUiState.Specified(
                surfaceCoordinates = Offset(100f, 200f),
                status = FocusMeteringUiState.Status.RUNNING
            )
        )
    }

    @Test
    fun from_successFocusState_returnsSuccessUiState() {
        val cameraState = CameraState(
            focusState = FocusState.Specified(
                x = 100f,
                y = 200f,
                status = FocusState.Status.SUCCESS
            )
        )
        val uiState = FocusMeteringUiState.from(cameraState)
        assertThat(uiState).isEqualTo(
            FocusMeteringUiState.Specified(
                surfaceCoordinates = Offset(100f, 200f),
                status = FocusMeteringUiState.Status.SUCCESS
            )
        )
    }

    @Test
    fun from_failureFocusState_returnsFailureUiState() {
        val cameraState = CameraState(
            focusState = FocusState.Specified(
                x = 100f,
                y = 200f,
                status = FocusState.Status.FAILURE
            )
        )
        val uiState = FocusMeteringUiState.from(cameraState)
        assertThat(uiState).isEqualTo(
            FocusMeteringUiState.Specified(
                surfaceCoordinates = Offset(100f, 200f),
                status = FocusMeteringUiState.Status.FAILURE
            )
        )
    }

    @Test
    fun from_cancelledFocusState_returnsCancelledUiState() {
        val cameraState = CameraState(
            focusState = FocusState.Specified(
                x = 100f,
                y = 200f,
                status = FocusState.Status.CANCELLED
            )
        )
        val uiState = FocusMeteringUiState.from(cameraState)
        assertThat(uiState).isEqualTo(
            FocusMeteringUiState.Specified(
                surfaceCoordinates = Offset(100f, 200f),
                status = FocusMeteringUiState.Status.CANCELLED
            )
        )
    }

    @Test
    fun updateFrom_unspecifiedToUnspecified_returnsSameInstance() {
        val initialUiState = FocusMeteringUiState.Unspecified
        val cameraState = CameraState(focusState = FocusState.Unspecified)
        val updatedUiState = initialUiState.updateFrom(cameraState)
        assertThat(updatedUiState).isSameInstanceAs(initialUiState)
    }

    @Test
    fun updateFrom_unspecifiedToSpecified_returnsNewSpecifiedUiState() {
        val initialUiState = FocusMeteringUiState.Unspecified
        val cameraState = CameraState(
            focusState = FocusState.Specified(x = 50f, y = 75f, status = FocusState.Status.RUNNING)
        )
        val updatedUiState = initialUiState.updateFrom(cameraState)
        assertThat(updatedUiState).isEqualTo(
            FocusMeteringUiState.Specified(
                surfaceCoordinates = Offset(50f, 75f),
                status = FocusMeteringUiState.Status.RUNNING
            )
        )
    }

    @Test
    fun updateFrom_sameSpecifiedState_returnsSameInstance() {
        val initialUiState = FocusMeteringUiState.Specified(
            surfaceCoordinates = Offset(50f, 75f),
            status = FocusMeteringUiState.Status.RUNNING
        )
        val cameraState = CameraState(
            focusState = FocusState.Specified(x = 50f, y = 75f, status = FocusState.Status.RUNNING)
        )
        val updatedUiState = initialUiState.updateFrom(cameraState)
        assertThat(updatedUiState).isSameInstanceAs(initialUiState)
    }

    @Test
    fun updateFrom_specifiedStatusChanged_returnsNewSpecifiedUiState() {
        val initialUiState = FocusMeteringUiState.Specified(
            surfaceCoordinates = Offset(50f, 75f),
            status = FocusMeteringUiState.Status.RUNNING
        )
        val cameraState = CameraState(
            focusState = FocusState.Specified(x = 50f, y = 75f, status = FocusState.Status.SUCCESS)
        )
        val updatedUiState = initialUiState.updateFrom(cameraState)
        assertThat(updatedUiState).isEqualTo(
            FocusMeteringUiState.Specified(
                surfaceCoordinates = Offset(50f, 75f),
                status = FocusMeteringUiState.Status.SUCCESS
            )
        )
    }

    @Test
    fun updateFrom_specifiedCoordinatesChanged_returnsNewSpecifiedUiState() {
        val initialUiState = FocusMeteringUiState.Specified(
            surfaceCoordinates = Offset(50f, 75f),
            status = FocusMeteringUiState.Status.RUNNING
        )
        val cameraState = CameraState(
            focusState = FocusState.Specified(
                x = 100f,
                y = 150f,
                status = FocusState.Status.RUNNING
            )
        )
        val updatedUiState = initialUiState.updateFrom(cameraState)
        assertThat(updatedUiState).isEqualTo(
            FocusMeteringUiState.Specified(
                surfaceCoordinates = Offset(100f, 150f),
                status = FocusMeteringUiState.Status.RUNNING
            )
        )
    }

    @Test
    fun updateFrom_specifiedToUnspecified_returnsUnspecifiedUiState() {
        val initialUiState = FocusMeteringUiState.Specified(
            surfaceCoordinates = Offset(50f, 75f),
            status = FocusMeteringUiState.Status.RUNNING
        )
        val cameraState = CameraState(focusState = FocusState.Unspecified)
        val updatedUiState = initialUiState.updateFrom(cameraState)
        assertThat(updatedUiState).isEqualTo(FocusMeteringUiState.Unspecified)
    }
}
