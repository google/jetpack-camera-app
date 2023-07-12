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

package com.google.jetpackcamera.domain.camera.test

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import com.google.jetpackcamera.domain.camera.CameraUseCase
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashModeStatus

class FakeCameraUseCase : CameraUseCase {

    private val availableLenses =
        listOf(CameraSelector.LENS_FACING_FRONT, CameraSelector.LENS_FACING_BACK)
    private var initialized = false
    private var useCasesBinded = false

    var previewStarted = false
    var numPicturesTaken = 0

    var recordingInProgress = false

    var isLensFacingFront = false
    private var flashMode = FlashModeStatus.OFF

    override suspend fun initialize(currentCameraSettings: CameraAppSettings): List<Int> {
        initialized = true
        flashMode = currentCameraSettings.flash_mode_status
        isLensFacingFront = currentCameraSettings.default_front_camera
        return availableLenses
    }

    override suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
        currentCameraSettings: CameraAppSettings,
    ) {
        val lensFacing = when (currentCameraSettings.default_front_camera) {
            true -> CameraSelector.LENS_FACING_FRONT
            false -> CameraSelector.LENS_FACING_BACK
        }

        if (!initialized) {
            throw IllegalStateException("CameraProvider not initialized")
        }
        if (!availableLenses.contains(lensFacing)) {
            throw IllegalStateException("Requested lens not available")
        }
        useCasesBinded = true
        previewStarted = true
    }

    override suspend fun takePicture() {
        if (!useCasesBinded) {
            throw IllegalStateException("Usecases not binded")
        }
        numPicturesTaken += 1
    }

    override suspend fun startVideoRecording() {
        recordingInProgress = true
    }

    override fun stopVideoRecording() {
        recordingInProgress = false
    }

    override fun setZoomScale(scale: Float) {
    }

    override fun setFlashMode(flashModeStatus: FlashModeStatus) {
        flashMode = flashModeStatus
    }

    override suspend fun flipCamera(isFrontFacing: Boolean) {
        isLensFacingFront = isFrontFacing
    }
}