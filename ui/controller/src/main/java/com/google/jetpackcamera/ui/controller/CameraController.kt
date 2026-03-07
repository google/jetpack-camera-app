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
package com.google.jetpackcamera.ui.controller

import com.google.jetpackcamera.model.DeviceRotation

/**
 * Interface for controlling camera lifecycle and basic operations.
 */
interface CameraController {
    /**
     * Starts the camera, initializing resources and beginning the preview stream.
     */
    fun startCamera()

    /**
     * Stops the camera and releases its resources.
     */
    fun stopCamera()

    /**
     * Initiates a tap-to-focus action at the given coordinates on the preview surface.
     *
     * @param x The x-coordinate of the tap.
     * @param y The y-coordinate of the tap.
     */
    fun tapToFocus(x: Float, y: Float)

    /**
     * Informs the camera system of the current device display rotation.
     *
     * @param deviceRotation The device rotation to set.
     */
    fun setDisplayRotation(deviceRotation: DeviceRotation)
}
