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

sealed interface ZoomControlUiState {
    data object Unavailable : ZoomControlUiState
    data object Disabled : ZoomControlUiState
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
