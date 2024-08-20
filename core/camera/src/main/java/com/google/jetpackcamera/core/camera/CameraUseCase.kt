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
package com.google.jetpackcamera.core.camera

import android.content.ContentResolver
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Data layer for camera.
 */
interface CameraUseCase {
    /**
     * Initializes the camera.
     *
     * @return list of available lenses.
     */
    suspend fun initialize(cameraAppSettings: CameraAppSettings, useCaseMode: UseCaseMode)

    /**
     * Starts the camera.
     *
     * This will start to configure the camera, but frames won't stream until a [SurfaceRequest]
     * from [getSurfaceRequest] has been fulfilled.
     *
     * The camera will run until the calling coroutine is cancelled.
     */
    suspend fun runCamera()

    suspend fun takePicture(onCaptureStarted: (() -> Unit) = {})

    /**
     * Takes a picture with the camera. If ignoreUri is set to true, the picture taken will be saved
     * at the default directory for pictures on device. Otherwise, it will be saved at the uri
     * location if the uri is not null. If it is null, an error will be thrown.
     */
    suspend fun takePicture(
        onCaptureStarted: (() -> Unit) = {},
        contentResolver: ContentResolver,
        imageCaptureUri: Uri?,
        ignoreUri: Boolean = false
    ): ImageCapture.OutputFileResults

    suspend fun startVideoRecording(
        videoCaptureUri: Uri?,
        shouldUseUri: Boolean,
        onVideoRecord: (OnVideoRecordEvent) -> Unit
    )

    fun stopVideoRecording()

    fun setZoomScale(scale: Float)

    fun getCurrentCameraState(): StateFlow<CameraState>

    fun getSurfaceRequest(): StateFlow<SurfaceRequest?>

    fun getScreenFlashEvents(): SharedFlow<ScreenFlashEvent>

    fun getCurrentSettings(): StateFlow<CameraAppSettings?>

    fun setFlashMode(flashMode: FlashMode)

    fun isScreenFlashEnabled(): Boolean

    suspend fun setAspectRatio(aspectRatio: AspectRatio)

    suspend fun setLensFacing(lensFacing: LensFacing)

    suspend fun tapToFocus(x: Float, y: Float)

    suspend fun setCaptureMode(captureMode: CaptureMode)

    suspend fun setDynamicRange(dynamicRange: DynamicRange)

    fun setDeviceRotation(deviceRotation: DeviceRotation)

    suspend fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode)

    suspend fun setLowLightBoost(lowLightBoost: LowLightBoost)

    suspend fun setImageFormat(imageFormat: ImageOutputFormat)

    suspend fun setAudioMuted(isAudioMuted: Boolean)

    suspend fun setVideoCaptureStabilization(videoCaptureStabilization: Stabilization)

    suspend fun setPreviewStabilization(previewStabilization: Stabilization)

    suspend fun setTargetFrameRate(targetFrameRate: Int)

    /**
     * Represents the events required for screen flash.
     */
    data class ScreenFlashEvent(val type: Type, val onComplete: () -> Unit) {
        enum class Type {
            APPLY_UI,
            CLEAR_UI
        }
    }

    /**
     * Represents the events for video recording.
     */
    sealed interface OnVideoRecordEvent {
        data class OnVideoRecorded(val savedUri: Uri) : OnVideoRecordEvent

        data class OnVideoRecordStatus(val audioAmplitude: Double) : OnVideoRecordEvent

        data class OnVideoRecordError(val error: Throwable?) : OnVideoRecordEvent
    }

    enum class UseCaseMode {
        STANDARD,
        IMAGE_ONLY,
        VIDEO_ONLY
    }
}

data class CameraState(
    val zoomScale: Float = 1f,
    val sessionFirstFrameTimestamp: Long = 0L,
    val torchEnabled: Boolean = false
)
