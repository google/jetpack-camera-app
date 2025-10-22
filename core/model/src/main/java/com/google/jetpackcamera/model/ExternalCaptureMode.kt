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
 * This interface is determined before the Preview UI is launched and passed into PreviewScreen. The
 * UX differs depends on which mode the Preview is launched under.
 */
enum class ExternalCaptureMode {
    /**
     * The default mode for the app.
     */
    Standard,

    /**
     * Under this mode, the app is launched by an external intent to capture one image.
     */
    ImageCapture,

    /**
     * Under this mode, the app is launched by an external intent to capture a video.
     */
    VideoCapture,

    /**
     * Under this mode, the app is launched by an external intent to capture multiple images.
     */
    MultipleImageCapture
}
