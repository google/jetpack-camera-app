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
package com.google.jetpackcamera.data.camera

import androidx.camera.core.SurfaceRequest
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository that manages camera system lifecycle, lazy initialization, and proxies camera data streams.
 */
interface CameraSystemRepository {

    /**
     * A [StateFlow] emitting the current [SurfaceRequest] when the camera is active.
     */
    val surfaceRequest: StateFlow<SurfaceRequest?>

    /**
     * A [StateFlow] emitting the current [CameraSystemConstraints] supported by the device.
     */
    val systemConstraints: StateFlow<CameraSystemConstraints?>

    /**
     * A [StateFlow] emitting the current [CameraAppSettings].
     */
    val currentSettings: StateFlow<CameraAppSettings?>

    /**
     * A [StateFlow] emitting the current [CameraState].
     */
    val currentCameraState: StateFlow<CameraState>

    /**
     * A [StateFlow] emitting a JSON string representation of the camera properties.
     */
    val cameraPropertiesJSON: StateFlow<String?>

    /**
     * Returns the initialized [CameraSystem], suspending until initialization completes.
     */
    suspend fun getCameraSystem(): CameraSystem

    /**
     * Returns supported MIME types once initialized.
     */
    suspend fun getSupportedMimeTypes(): List<String>
}
