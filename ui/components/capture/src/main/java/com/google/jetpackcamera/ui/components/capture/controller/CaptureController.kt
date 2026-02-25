/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.ui.components.capture.controller

import android.content.ContentResolver

/**
 * Interface for controlling capture operations like taking photos and recording videos.
 */
interface CaptureController {
    /**
     * Captures a single image.
     *
     * @param contentResolver The [ContentResolver] to use for saving the image.
     */
    fun captureImage(contentResolver: ContentResolver)

    /**
     * Starts video recording.
     */
    fun startVideoRecording()

    /**
     * Stops the current video recording.
     */
    fun stopVideoRecording()

    /**
     * Sets whether the recording is locked.
     *
     * @param isLocked True if the recording should be locked, false otherwise.
     */
    fun setLockedRecording(isLocked: Boolean)
}
