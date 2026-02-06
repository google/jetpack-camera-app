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

/**
 * Updates an existing [FocusMeteringUiState] based on a new [CameraState].
 *
 * This function provides an efficient way to update the focus UI state by comparing the incoming
 * camera state with the current UI state. It avoids creating a new object if the underlying
 * focus parameters (like coordinates and status) have not changed, reducing unnecessary
 * recompositions. If the state has meaningfully changed, it delegates to the [from] factory
 * function to create a new, updated state.
 *
 * @param cameraState The new, real-time state from the camera.
 * @return The existing [FocusMeteringUiState] instance if no change is detected, or a new
 *         [FocusMeteringUiState] reflecting the updated camera focus state.
 */
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

/**
 * Creates a [FocusMeteringUiState] from the given [CameraState].
 *
 * This factory function translates the low-level [FocusState] from the core camera layer into its
 * corresponding UI representation. It maps the coordinates and status (e.g., RUNNING, SUCCESS)
 * to the appropriate [FocusMeteringUiState] subtype, which can be either [FocusMeteringUiState.Unspecified]
 * or [FocusMeteringUiState.Specified].
 *
 * @param cameraState The real-time state from the camera containing the focus information.
 * @return A [FocusMeteringUiState] that represents the current focus state for the UI.
 */
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
