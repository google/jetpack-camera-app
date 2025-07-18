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

/**
 * Class representing the app's configuration to capture an image
 */
enum class CaptureMode {

    /**
     * Both Image and Video use cases will be bound.
     *
     * Tap the Capture Button to take an image.
     *
     * Hold the Capture button to start recording, and release to complete the recording.
     */
    STANDARD,

    /**
     * Video use case will be bound. Image use case will not be bound.
     *
     * Tap the Capture Button to start recording.
     * Hold the Capture button to start recording; releasing will not stop the recording.
     *
     * Tap the capture button again after recording has started to complete the recording.
     */
    VIDEO_ONLY,

    /**
     * Image use case will be bound. Video use case will not be bound.
     *
     * Tap the Capture Button to capture an Image.
     * Holding the Capture Button will do nothing. Subsequent release of the Capture button will also do nothing.
     */
    IMAGE_ONLY
}