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
package com.google.jetpackcamera.ui.uistate.capture

import com.google.jetpackcamera.model.ConcurrentCameraMode

/**
 * Defines the UI state for concurrent camera mode.
 *
 * This sealed interface represents the different states of the concurrent camera UI, which allows
 * the user to see a picture-in-picture preview of another camera.
 */
sealed interface ConcurrentCameraUiState {
    /**
     * Concurrent camera mode is unavailable.
     * This may be because the device does not support it, or it is disabled by a constraint.
     */
    data object Unavailable : ConcurrentCameraUiState

    /**
     * Concurrent camera mode is available.
     *
     * @param selectedConcurrentCameraMode The currently selected [ConcurrentCameraMode].
     * @param isEnabled Whether the concurrent camera mode is currently enabled (e.g. the PiP
     * window is visible).
     */
    data class Available(
        val selectedConcurrentCameraMode: ConcurrentCameraMode,
        val isEnabled: Boolean
    ) : ConcurrentCameraUiState

    companion object
}
