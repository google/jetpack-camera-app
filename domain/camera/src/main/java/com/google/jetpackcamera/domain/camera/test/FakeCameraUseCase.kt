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
package com.google.jetpackcamera.domain.camera.test

import android.content.ContentResolver
import android.net.Uri
import android.view.Display
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import com.google.jetpackcamera.domain.camera.CameraUseCase
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FakeCameraUseCase(
    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)
) : CameraUseCase {
    private val availableLenses =
        listOf(CameraSelector.LENS_FACING_FRONT, CameraSelector.LENS_FACING_BACK)
    private var initialized = false
    private var useCasesBinded = false

    var previewStarted = false
    var numPicturesTaken = 0

    var recordingInProgress = false

    var isLensFacingFront = false
    private var flashMode = FlashMode.OFF
    private var aspectRatio = AspectRatio.THREE_FOUR

    private var isScreenFlash = true
    private var screenFlashEvents = MutableSharedFlow<CameraUseCase.ScreenFlashEvent>()

    override suspend fun initialize(currentCameraSettings: CameraAppSettings): List<Int> {
        initialized = true
        flashMode = currentCameraSettings.flashMode
        isLensFacingFront = currentCameraSettings.isFrontCameraFacing
        aspectRatio = currentCameraSettings.aspectRatio
        return availableLenses
    }

    override suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
        currentCameraSettings: CameraAppSettings
    ) {
        val lensFacing =
            when (currentCameraSettings.isFrontCameraFacing) {
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
        if (isScreenFlash) {
            coroutineScope.launch {
                screenFlashEvents.emit(
                    CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.APPLY_UI) { }
                )
                screenFlashEvents.emit(
                    CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI) { }
                )
            }
        }
        numPicturesTaken += 1
    }
    override suspend fun takePicture(contentResolver: ContentResolver, imageCaptureUri: Uri?) {
        takePicture()
    }

    fun emitScreenFlashEvent(event: CameraUseCase.ScreenFlashEvent) {
        coroutineScope.launch {
            screenFlashEvents.emit(event)
        }
    }

    override suspend fun startVideoRecording() {
        recordingInProgress = true
    }

    override fun stopVideoRecording() {
        recordingInProgress = false
    }

    private val _zoomScale = MutableStateFlow(1f)
    val zoomScale get() = _zoomScale.asStateFlow()
    override fun setZoomScale(scale: Float) {
        _zoomScale.value = scale
    }
    override fun getZoomScale(): StateFlow<Float> = _zoomScale.asStateFlow()

    override fun getScreenFlashEvents() = screenFlashEvents

    override fun setFlashMode(flashMode: FlashMode, isFrontFacing: Boolean) {
        this.flashMode = flashMode
        isLensFacingFront = isFrontFacing

        isScreenFlash =
            isLensFacingFront && (flashMode == FlashMode.AUTO || flashMode == FlashMode.ON)
    }

    override fun isScreenFlashEnabled() = isScreenFlash

    override suspend fun setAspectRatio(aspectRatio: AspectRatio, isFrontFacing: Boolean) {
        this.aspectRatio = aspectRatio
    }

    override suspend fun flipCamera(isFrontFacing: Boolean, flashMode: FlashMode) {
        isLensFacingFront = isFrontFacing
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

    override suspend fun setCaptureMode(captureMode: CaptureMode) {
        TODO("Not yet implemented")
    }
}
