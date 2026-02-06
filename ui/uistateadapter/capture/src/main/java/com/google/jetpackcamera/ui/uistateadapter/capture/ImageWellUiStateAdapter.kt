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
 * Creates an [ImageWellUiState] based on the most recently captured media and video recording status.
 *
 * This function determines the state of the image well component, which displays a thumbnail of
 * the last captured photo or video. The image well is only shown if there is a valid media
 * thumbnail available and if a video recording is not currently in progress.
 *
 * @param mediaDescriptor The descriptor for the most recently captured media, which may contain a
 *   thumbnail.
 * @param videoRecordingState The current state of video recording.
 * @return [ImageWellUiState.LastCapture] containing the [mediaDescriptor] if a thumbnail exists
 *   and recording is inactive, otherwise returns [ImageWellUiState.Unavailable].
 */
fun ImageWellUiState.Companion.from(
    mediaDescriptor: MediaDescriptor,
    videoRecordingState: VideoRecordingState
): ImageWellUiState {
    return if (mediaDescriptor is MediaDescriptor.Content &&
        mediaDescriptor.thumbnail != null &&
        videoRecordingState is VideoRecordingState.Inactive
    ) {
        ImageWellUiState.LastCapture(mediaDescriptor = mediaDescriptor)
    } else {
        ImageWellUiState.Unavailable
    }
}
