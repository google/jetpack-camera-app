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
package com.google.jetpackcamera.core.camera.test

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.CameraUseCase
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DeviceRotation
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LowLightBoost
import com.google.jetpackcamera.settings.model.Stabilization
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FakeCameraUseCase(
    private val coroutineScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default),
    defaultCameraSettings: CameraAppSettings = CameraAppSettings()
) : CameraUseCase {
    private val availableLenses = listOf(LensFacing.FRONT, LensFacing.BACK)
    private var initialized = false
    private var useCasesBinded = false

    var previewStarted = false
    var numPicturesTaken = 0

    var recordingInProgress = false

    var isLensFacingFront = false

    private var isScreenFlash = true
    private var screenFlashEvents = MutableSharedFlow<CameraUseCase.ScreenFlashEvent>()

    private val currentSettings = MutableStateFlow(defaultCameraSettings)

    override suspend fun initialize(
        cameraAppSettings: CameraAppSettings,
        useCaseMode: CameraUseCase.UseCaseMode
    ) {
        initialized = true
    }

    override suspend fun runCamera() {
        val lensFacing = currentSettings.value.cameraLensFacing

        if (!initialized) {
            throw IllegalStateException("CameraProvider not initialized")
        }
        if (!availableLenses.contains(lensFacing)) {
            throw IllegalStateException("Requested lens not available")
        }

        currentSettings
            .onCompletion {
                useCasesBinded = false
                previewStarted = false
                recordingInProgress = false
            }.collectLatest {
                useCasesBinded = true
                previewStarted = true

                isLensFacingFront = it.cameraLensFacing == LensFacing.FRONT
                isScreenFlash =
                    isLensFacingFront &&
                    (it.flashMode == FlashMode.AUTO || it.flashMode == FlashMode.ON)

                _currentCameraState.update { old ->
                    old.copy(zoomScale = it.zoomScale)
                }
            }
    }

    override suspend fun takePicture(onCaptureStarted: (() -> Unit)) {
        if (!useCasesBinded) {
            throw IllegalStateException("Usecases not bound")
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

    @SuppressLint("RestrictedApi")
    override suspend fun takePicture(
        onCaptureStarted: (() -> Unit),
        contentResolver: ContentResolver,
        imageCaptureUri: Uri?,
        ignoreUri: Boolean
    ): ImageCapture.OutputFileResults {
        takePicture(onCaptureStarted)
        return ImageCapture.OutputFileResults(null)
    }

    fun emitScreenFlashEvent(event: CameraUseCase.ScreenFlashEvent) {
        coroutineScope.launch {
            screenFlashEvents.emit(event)
        }
    }

    override suspend fun startVideoRecording(
        videoCaptureUri: Uri?,
        shouldUseUri: Boolean,
        onVideoRecord: (CameraUseCase.OnVideoRecordEvent) -> Unit
    ) {
        if (!useCasesBinded) {
            throw IllegalStateException("Usecases not bound")
        }
        recordingInProgress = true
    }

    override fun stopVideoRecording() {
        recordingInProgress = false
    }

    private val _currentCameraState = MutableStateFlow(CameraState())
    override fun setZoomScale(scale: Float) {
        currentSettings.update { old ->
            old.copy(zoomScale = scale)
        }
    }
    override fun getCurrentCameraState(): StateFlow<CameraState> = _currentCameraState.asStateFlow()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    override fun getSurfaceRequest(): StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    override fun getScreenFlashEvents() = screenFlashEvents
    override fun getCurrentSettings(): StateFlow<CameraAppSettings?> = currentSettings.asStateFlow()

    override fun setFlashMode(flashMode: FlashMode) {
        currentSettings.update { old ->
            old.copy(flashMode = flashMode)
        }
    }

    override fun isScreenFlashEnabled() = isScreenFlash

    fun isPreviewStarted() = previewStarted

    override suspend fun setAspectRatio(aspectRatio: AspectRatio) {
        currentSettings.update { old ->
            old.copy(aspectRatio = aspectRatio)
        }
    }

    override suspend fun setLensFacing(lensFacing: LensFacing) {
        currentSettings.update { old ->
            old.copy(cameraLensFacing = lensFacing)
        }
    }

    override suspend fun tapToFocus(x: Float, y: Float) {
        TODO("Not yet implemented")
    }

    override suspend fun setCaptureMode(captureMode: CaptureMode) {
        currentSettings.update { old ->
            old.copy(captureMode = captureMode)
        }
    }

    override suspend fun setDynamicRange(dynamicRange: DynamicRange) {
        currentSettings.update { old ->
            old.copy(dynamicRange = dynamicRange)
        }
    }

    override fun setDeviceRotation(deviceRotation: DeviceRotation) {
        currentSettings.update { old ->
            old.copy(deviceRotation = deviceRotation)
        }
    }

    override suspend fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        currentSettings.update { old ->
            old.copy(concurrentCameraMode = concurrentCameraMode)
        }
    }

    override suspend fun setImageFormat(imageFormat: ImageOutputFormat) {
        currentSettings.update { old ->
            old.copy(imageFormat = imageFormat)
        }
    }

    override suspend fun setLowLightBoost(lowLightBoost: LowLightBoost) {
        currentSettings.update { old ->
            old.copy(lowLightBoost = lowLightBoost)
        }
    }

    override suspend fun setAudioMuted(isAudioMuted: Boolean) {
        currentSettings.update { old ->
            old.copy(audioMuted = isAudioMuted)
        }
    }

    override suspend fun setVideoCaptureStabilization(videoCaptureStabilization: Stabilization) {
        currentSettings.update { old ->
            old.copy(videoCaptureStabilization = videoCaptureStabilization)
        }
    }

    override suspend fun setPreviewStabilization(previewStabilization: Stabilization) {
        currentSettings.update { old ->
            old.copy(previewStabilization = previewStabilization)
        }
    }

    override suspend fun setTargetFrameRate(targetFrameRate: Int) {
        currentSettings.update { old ->
            old.copy(targetFrameRate = targetFrameRate)
        }
    }
}
