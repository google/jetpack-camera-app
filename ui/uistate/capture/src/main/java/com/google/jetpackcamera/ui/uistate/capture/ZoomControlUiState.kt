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

import com.google.jetpackcamera.model.LensFacing

/**
 * Defines the UI state for the zoom control buttons (e.g., 0.5x, 1x, 2x).
 *
 * This sealed interface represents the different states of the zoom control buttons, which allow
 * for quick switching between preset zoom levels.
 */
sealed interface ZoomControlUiState {
    /**
     * The zoom control buttons are unavailable. This may be because the device does not support
     * multiple zoom levels or the feature is otherwise disabled.
     */
    data object Unavailable : ZoomControlUiState

    /**
     * The zoom control buttons are disabled. This may occur during an operation that prevents
     * changing the zoom, such as video recording on some devices.
     */
    data object Disabled : ZoomControlUiState

    /**
     * The zoom control buttons are enabled and ready for user interaction.
     *
     * @param zoomLevels A list of preset zoom ratios that the user can select.
     * @param primaryLensFacing The [LensFacing] of the primary camera.
     * @param initialZoomRatio The zoom ratio at the beginning of the current camera session.
     * @param primaryZoomRatio The current zoom ratio of the primary camera.
     * @param primarySettingZoomRatio The target zoom ratio that has been set and is being applied.
     * @param animatingToValue The target zoom ratio for an ongoing animation.
     */
    data class Enabled(
        val zoomLevels: List<Float>,
        val primaryLensFacing: LensFacing,
        val initialZoomRatio: Float? = null,
        val primaryZoomRatio: Float? = null,
        val primarySettingZoomRatio: Float? = null,
        val animatingToValue: Float? = null
    ) : ZoomControlUiState

    companion object
}
