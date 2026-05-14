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

import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat

/**
 * Defines the UI state for the HDR (High Dynamic Range) setting.
 *
 * This sealed interface represents the different states of the HDR UI, such as being unavailable,
 * or available with a specific mode selected.
 */
sealed interface HdrUiState {
    /**
     * The HDR setting is unavailable.
     * This may be because the current camera or mode does not support HDR.
     */
    data object Unavailable : HdrUiState

    /**
     * The HDR setting is available.
     *
     * @param selectedImageFormat The currently selected image output format, which may be related to HDR.
     * @param selectedDynamicRange The currently selected [DynamicRange].
     */
    data class Available(
        val selectedImageFormat: ImageOutputFormat,
        val selectedDynamicRange: DynamicRange
    ) : HdrUiState

    companion object
}
