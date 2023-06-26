/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.google.jetpackcamera.domain.camera

import androidx.camera.core.Preview
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashModeStatus

/**
 * Data layer for camera.
 */
interface CameraUseCase {

    /**
     * Initializes the camera.
     *
     * @return list of available lenses.
     */
    suspend fun initialize(currentCameraSettings: CameraAppSettings): List<Int>

    /**
     * Starts the camera with [lensFacing] with the provided [Preview.SurfaceProvider].
     *
     * The camera will run until the calling coroutine is cancelled.
     */
    suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
        currentCameraSettings: CameraAppSettings
    )

    suspend fun takePicture()

    suspend fun startVideoRecording()

    fun stopVideoRecording()

    fun setZoomScale(scale: Float)

    fun setFlashMode(flashModeStatus: FlashModeStatus)
    suspend fun flipCamera(isFrontFacing: Boolean)
}