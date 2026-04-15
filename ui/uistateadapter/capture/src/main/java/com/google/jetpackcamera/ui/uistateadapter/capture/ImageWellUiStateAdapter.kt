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

import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState

/**
 * Creates an [ImageWellUiState] for a given [MediaDescriptor] and [VideoRecordingState].
 *
 * This function determines the state of the image well, a component that displays the
 * [MediaDescriptor]'s thumbnail. This state will be unavailable if any of the following are true:
 * - mediaDescriptor is not MediaDescriptor.Content
 * - mediaDescriptor does not have a thumbnail
 *
 * @param mediaDescriptor the media's correlating [MediaDescriptor]
 * @param videoRecordingState The current state of video recording.
 * @return [ImageWellUiState.Content] only if the mediaDescriptor is MediaDescriptor.Content,
 *  has a non-null thumbnail, and video recording state is inactive.
 *  otherwise return [ImageWellUiState.Unavailable]
 */
fun ImageWellUiState.Companion.from(
    mediaDescriptor: MediaDescriptor,
    videoRecordingState: VideoRecordingState
): ImageWellUiState {
    return if (mediaDescriptor is MediaDescriptor.Content &&
        mediaDescriptor.thumbnail != null &&
        videoRecordingState is VideoRecordingState.Inactive
    ) {
        ImageWellUiState.Content(mediaDescriptor = mediaDescriptor)
    } else {
        ImageWellUiState.Unavailable
    }
}
