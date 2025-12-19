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

import androidx.compose.ui.geometry.Offset
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.FocusState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState

fun FocusMeteringUiState.updateFrom(cameraState: CameraState): FocusMeteringUiState {
    val focusState = cameraState.focusState
    return when (this) {
        is FocusMeteringUiState.Unspecified -> {
            if (focusState is FocusState.Unspecified) {
                this
            } else {
                FocusMeteringUiState.from(cameraState)
            }
        }

        is FocusMeteringUiState.Specified -> {
            if (focusState is FocusState.Specified &&
                this.surfaceCoordinates.x == focusState.x &&
                this.surfaceCoordinates.y == focusState.y &&
                this.status.name == focusState.status.name
            ) {
                this
            } else {
                FocusMeteringUiState.from(cameraState)
            }
        }
    }
}

fun FocusMeteringUiState.Companion.from(cameraState: CameraState): FocusMeteringUiState {
    return when (val focusState = cameraState.focusState) {
        is FocusState.Unspecified -> FocusMeteringUiState.Unspecified
        is FocusState.Specified -> {
            val status = when (focusState.status) {
                FocusState.Status.RUNNING -> FocusMeteringUiState.Status.RUNNING
                FocusState.Status.SUCCESS -> FocusMeteringUiState.Status.SUCCESS
                FocusState.Status.FAILURE -> FocusMeteringUiState.Status.FAILURE
                FocusState.Status.CANCELLED -> FocusMeteringUiState.Status.CANCELLED
            }
            FocusMeteringUiState.Specified(
                surfaceCoordinates = Offset(focusState.x, focusState.y),
                status = status
            )
        }
    }
}
