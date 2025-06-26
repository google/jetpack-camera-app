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
package com.google.jetpackcamera.settings.model

import android.net.Uri

/**
 * This interface is determined before the Preview UI is launched and passed into PreviewScreen. The
 * UX differs depends on which mode the Preview is launched under.
 */
sealed interface ExternalCaptureMode {
    /**
     * The default mode for the app.
     */
    data class StandardMode(
        val onImageCapture: (ImageCaptureEvent) -> Unit
    ) : ExternalCaptureMode

    /**
     * Under this mode, the app is launched by an external intent to capture one image.
     */
    data class ExternalImageCaptureMode(
        val imageCaptureUri: Uri?,
        val onImageCapture: (ImageCaptureEvent) -> Unit
    ) : ExternalCaptureMode

    /**
     * Under this mode, the app is launched by an external intent to capture a video.
     */
    data class ExternalVideoCaptureMode(
        val videoCaptureUri: Uri?,
        val onVideoCapture: (VideoCaptureEvent) -> Unit
    ) : ExternalCaptureMode

    /**
     * Under this mode, the app is launched by an external intent to capture multiple images.
     */
    data class ExternalMultipleImageCaptureMode(
        val imageCaptureUris: List<Uri>?,
        val onImageCapture: (ImageCaptureEvent, Int) -> Unit
    ) : ExternalCaptureMode

    sealed interface ImageCaptureEvent {
        data class ImageSaved(val savedUri: Uri? = null) : ImageCaptureEvent

        data class ImageCaptureError(val exception: Exception) : ImageCaptureEvent
    }

    sealed interface VideoCaptureEvent {
        data class VideoSaved(val savedUri: Uri) : VideoCaptureEvent

        data class VideoCaptureError(val error: Throwable?) : VideoCaptureEvent
    }
}
