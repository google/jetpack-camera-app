/*
 * Copyright (C) 2026 The Android Open Source Project
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

import com.google.jetpackcamera.model.DeviceRotation

/**
 * Interface for controlling actions on the capture screen.
 */
interface CaptureScreenController {
    /**
     * Sets the display rotation.
     *
     * @param deviceRotation The device rotation to set.
     */
    fun setDisplayRotation(deviceRotation: DeviceRotation)

    /**
     * Initiates a tap-to-focus action at the given coordinates.
     *
     * @param x The x-coordinate of the tap.
     * @param y The y-coordinate of the tap.
     */
    fun tapToFocus(x: Float, y: Float)

    /**
     * Enables or disables audio recording.
     *
     * @param shouldEnableAudio Whether audio should be enabled.
     */
    fun setAudioEnabled(shouldEnableAudio: Boolean)

    /**
     * Updates the UI with the last captured media.
     */
    fun updateLastCapturedMedia()

    /**
     * Transfers the media from the image well to the repository.
     */
    fun imageWellToRepository()

    /**
     * Pauses or resumes video recording.
     *
     * @param shouldBePaused Whether the recording should be paused.
     */
    fun setPaused(shouldBePaused: Boolean)
}
