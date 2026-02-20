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
package com.google.jetpackcamera.ui.components.capture.debug

import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * This file contains the data class [DebugCallbacks] and a helper function to create it.
 * [DebugCallbacks] is used to handle debug-related UI events on the capture screen.
 */

/**
 * Data class holding callbacks for debug-related UI events.
 *
 * @param toggleDebugHidingComponents Toggles the visibility of components for debugging purposes.
 * @param toggleDebugOverlay Toggles the visibility of the debug overlay.
 * @param setTestPattern Sets a test pattern on the camera.
 */
data class DebugCallbacks(
    val toggleDebugHidingComponents: () -> Unit,
    val toggleDebugOverlay: () -> Unit,
    val setTestPattern: (TestPattern) -> Unit
)

/**
 * Creates a [DebugCallbacks] instance with implementations that interact with the camera system
 * and update the UI state.
 *
 * @param trackedCaptureUiState The mutable state flow for the tracked capture UI state.
 * @param cameraSystem The [CameraSystem] to interact with the camera hardware.
 * @return An instance of [DebugCallbacks].
 */
fun getDebugCallbacks(
    trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    cameraSystem: CameraSystem
): DebugCallbacks {
    return DebugCallbacks(
        toggleDebugHidingComponents = {
            trackedCaptureUiState.update { old ->
                old.copy(debugHidingComponents = !old.debugHidingComponents)
            }
        },
        toggleDebugOverlay = {
            trackedCaptureUiState.update { old ->
                old.copy(isDebugOverlayOpen = !old.isDebugOverlayOpen)
            }
        },
        setTestPattern = { newTestPattern ->
            cameraSystem.setTestPattern(
                newTestPattern = newTestPattern
            )
        }
    )
}
