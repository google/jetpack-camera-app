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

import com.google.jetpackcamera.data.media.MediaDescriptor

/**
 * Defines the UI state for the image well, a component used to display the thumbnail of a captured media.
 */
sealed interface ImageWellUiState {
    /**
     * The image well is unavailable and should not be displayed.
     * This may be the case if no camera is available or if the feature is disabled.
     */
    data object Unavailable : ImageWellUiState

    data class Content(val mediaDescriptor: MediaDescriptor.Content) : ImageWellUiState

    companion object
}
