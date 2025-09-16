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
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.model.CameraAppSettings
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Data layer for camera.
 */
interface CameraSystem {
    /**
     * Initializes the camera.
     *
     * @return list of available lenses.
     */
    suspend fun initialize(
        cameraAppSettings: CameraAppSettings,
        cameraPropertiesJSONCallback: (result: String) -> Unit
    )

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
        contentResolver: ContentResolver,
        saveLocation: SaveLocation,
        onCaptureStarted: (() -> Unit) = {}
    ): ImageCapture.OutputFileResults

    suspend fun startVideoRecording(
        saveLocation: SaveLocation,
        onVideoRecord: (OnVideoRecordEvent) -> Unit
    )

    suspend fun pauseVideoRecording()

    suspend fun resumeVideoRecording()

    suspend fun stopVideoRecording()

    fun changeZoomRatio(newZoomState: CameraZoomRatio)

    fun setTestPattern(newTestPattern: TestPattern)

    fun getCurrentCameraState(): StateFlow<CameraState>

    fun getSurfaceRequest(): StateFlow<SurfaceRequest?>

    fun getScreenFlashEvents(): ReceiveChannel<ScreenFlashEvent>

    fun getCurrentSettings(): StateFlow<CameraAppSettings?>

    fun setFlashMode(flashMode: FlashMode)

    fun isScreenFlashEnabled(): Boolean

    suspend fun setAspectRatio(aspectRatio: AspectRatio)

    suspend fun setVideoQuality(videoQuality: VideoQuality)

    suspend fun setLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority)

    fun getLowLightBoostEvents(): MutableSharedFlow<LowLightBoostEvent>

    suspend fun setLensFacing(lensFacing: LensFacing)

    suspend fun tapToFocus(x: Float, y: Float)

    suspend fun setStreamConfig(streamConfig: StreamConfig)

    suspend fun setDynamicRange(dynamicRange: DynamicRange)

    fun setDeviceRotation(deviceRotation: DeviceRotation)

    suspend fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode)

    suspend fun setImageFormat(imageFormat: ImageOutputFormat)

    suspend fun setAudioEnabled(isAudioEnabled: Boolean)

    suspend fun setStabilizationMode(stabilizationMode: StabilizationMode)

    suspend fun setTargetFrameRate(targetFrameRate: Int)

    suspend fun setMaxVideoDuration(durationInMillis: Long)

    suspend fun setCaptureMode(captureMode: CaptureMode)

    /**
     * Represents the events required for screen flash.
     */
    data class ScreenFlashEvent(val type: Type, val onComplete: () -> Unit) {
        enum class Type {
            APPLY_UI,
            CLEAR_UI
        }
    }
}
