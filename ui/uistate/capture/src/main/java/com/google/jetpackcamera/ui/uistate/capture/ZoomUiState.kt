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

import android.util.Range

/**
 * Defines the UI state for the zoom functionality.
 *
 * This sealed interface represents the different states of the zoom UI, which can be either
 * unavailable or enabled with zoom-related information.
 */
sealed interface ZoomUiState {
    /**
     * The zoom UI is unavailable, for example, if the camera does not support zoom.
     */
    data object Unavailable : ZoomUiState

    /**
     * The zoom UI is enabled and provides information about the current zoom state.
     *
     * @param primaryZoomRange The available zoom range for the primary camera.
     * @param primaryZoomRatio The current zoom ratio of the primary camera.
     * @param primaryLinearZoom The current linear zoom value, normalized between 0.0 and 1.0,
     * representing the position within the available zoom range.
     */
    data class Enabled(
        val primaryZoomRange: Range<Float>,
        val primaryZoomRatio: Float? = null,
        val primaryLinearZoom: Float? = null
    ) : ZoomUiState

    companion object
}
