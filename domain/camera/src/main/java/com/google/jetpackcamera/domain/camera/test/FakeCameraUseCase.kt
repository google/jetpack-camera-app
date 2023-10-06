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

import android.view.Display
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import com.google.jetpackcamera.domain.camera.CameraUseCase
import kotlinx.coroutines.flow.MutableStateFlow

class FakeCameraUseCase : CameraUseCase {
    private val availableLenses =
        listOf(CameraSelector.LENS_FACING_FRONT, CameraSelector.LENS_FACING_BACK)
    private var initialized = false
    private var useCasesBinded = false

    var previewStarted = false
    var numPicturesTaken = 0

    var recordingInProgress = false

    override val config = MutableStateFlow(CameraUseCase.Config())

    private var lensFacing = CameraUseCase.LensFacing.FRONT
    private var flashMode = CameraUseCase.FlashMode.OFF
    private var aspectRatio = CameraUseCase.AspectRatio.ASPECT_RATIO_4_3

    override suspend fun initialize(initialConfig: CameraUseCase.Config) {
        initialized = true
        flashMode = initialConfig.flashMode
        lensFacing = initialConfig.lensFacing
        aspectRatio = initialConfig.aspectRatio
    }

    override suspend fun runCamera(surfaceProvider: Preview.SurfaceProvider) {
        if (!initialized) {
            throw IllegalStateException("CameraProvider not initialized")
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

    override fun setZoomScale(scale: Float): Float {
        return -1f
    }

    override suspend fun setFlashMode(flashMode: CameraUseCase.FlashMode) {
        this.flashMode = flashMode
    }

    override suspend fun setAspectRatio(aspectRatio: CameraUseCase.AspectRatio) {
        this.aspectRatio = aspectRatio
    }

    override suspend fun setLensFacing(lensFacing: CameraUseCase.LensFacing) {
        this.lensFacing = lensFacing
    }

    override suspend fun setCaptureMode(captureMode: CameraUseCase.CaptureMode) {
    }

    override fun tapToFocus(
        display: Display,
        surfaceWidth: Int,
        surfaceHeight: Int,
        x: Float,
        y: Float
    ) {
        TODO("Not yet implemented")
    }
}
