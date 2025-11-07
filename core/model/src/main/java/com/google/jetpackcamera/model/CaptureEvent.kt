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
package com.google.jetpackcamera.model

import android.net.Uri

/**
 * Represents events related to the capture of an image or video by the camera.
 * These events indicate either a successful capture or a failure.
 */
sealed interface CaptureEvent

/**
 * Represents a capture event that is part of a sequence, indicating its current
 * progress within that sequence.
 * This is used for captures that occur as one of many (e.g., a burst capture or
 * handling multiple image requests from an external intent).
 */
interface ProgressCaptureEvent {
    /**
     * The progress of this capture within the overall sequence.
     * [IntProgress.currentValue] indicates the current step or identifier within the
     * sequence, and [IntProgress.range] defines the full span of that sequence
     * (e.g., from a starting value to an ending value, inclusive).
     */
    val progress: IntProgress
}

/**
 * Represents events specifically related to image capture.
 */
sealed interface ImageCaptureEvent : CaptureEvent {

    /**
     * Base interface for events indicating a successful image capture.
     * Concrete implementations will specify whether the capture was a single event
     * or part of a sequence.
     */
    interface ImageCaptured : ImageCaptureEvent {
        /**
         * The [Uri] of the captured image file. This may be `null` if the
         * capture was successful but the image was not saved to a specific Uri
         * (e.g., if only a bitmap was processed in memory without explicit file saving).
         */
        val capturedUri: Uri?
    }

    /**
     * Base interface for events indicating an error during image capture.
     * Concrete implementations will specify whether the error occurred during a single event
     * or as part of a sequence.
     */
    interface ImageCaptureError : ImageCaptureEvent {
        /**
         * The [Exception] that describes the cause of the image capture or saving failure.
         */
        val exception: Exception
    }

    /**
     * Indicates that a single image (not part of an explicit sequence)
     * was successfully captured and saved.
     *
     * @param capturedUri The [Uri] of the saved image file. This may be null if the
     *                 capture was successful but the image was not saved to a specific Uri
     *                 (e.g., if only a bitmap was processed in memory).
     */
    data class SingleImageSaved(override val capturedUri: Uri? = null) : ImageCaptured

    data class SingleImageCached(override val capturedUri: Uri? = null) : ImageCaptured

    /**
     * Indicates that an error occurred during a single image capture or saving
     * (not part of an explicit sequence).
     *
     * @param exception The [Exception] that describes the cause of the failure.
     */
    data class SingleImageCaptureError(override val exception: Exception) : ImageCaptureError

    /**
     * Indicates that an image, as part of a sequence, was successfully captured and saved,
     * along with its progress in that sequence.
     *
     * @param capturedUri The [Uri] of the saved image file. This may be null if the
     *                 capture was successful but the image was not saved to a specific Uri.
     * @param progress The progress of this image capture within the overall sequence.
     *                 The [IntProgress.currentValue] represents the current identifier of this
     *                 image within the sequence, and the [IntProgress.range] indicates the full
     *                 span of the sequence.
     */
    data class SequentialImageSaved(
        override val capturedUri: Uri? = null,
        override val progress: IntProgress
    ) : ImageCaptured, ProgressCaptureEvent

    /**
     * Indicates that an error occurred during the capture or saving of an image
     * that was part of a sequence, along with its progress state at the time of failure.
     *
     * @param exception The [Exception] that describes the cause of the failure.
     * @param progress The progress of this image capture within the overall sequence
     *                 at the time of the error. The [IntProgress.currentValue] represents the
     *                 identifier of the failing image within the sequence, and the
     *                 [IntProgress.range] indicates the full span of the sequence.
     */
    data class SequentialImageCaptureError(
        override val exception: Exception,
        override val progress: IntProgress
    ) : ImageCaptureError, ProgressCaptureEvent
}

/**
 * Represents events specifically related to video capture.
 */
sealed interface VideoCaptureEvent : CaptureEvent {
    /**
     * Indicates that a video was successfully captured and saved.
     *
     * @param savedUri The [Uri] of the saved video file.
     */
    data class VideoSaved(val savedUri: Uri) : VideoCaptureEvent
    data class VideoCached(val capturedUri: Uri) : VideoCaptureEvent

    /**
     * Indicates that an error occurred during video capture or saving.
     *
     * @param error The [Throwable] that describes the cause of the failure.
     */
    data class VideoCaptureError(val error: Throwable?) : VideoCaptureEvent
}
