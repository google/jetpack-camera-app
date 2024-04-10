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

import android.content.ContentResolver
import android.net.Uri
import android.view.Display
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
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
    suspend fun initialize()

    /**
     * Starts the camera.
     *
     * This will start to configure the camera, but frames won't stream until a [SurfaceRequest]
     * from [getSurfaceRequest] has been fulfilled.
     *
     * The camera will run until the calling coroutine is cancelled.
     */
    suspend fun runCamera()

    suspend fun takePicture()

    /**
     * Takes a picture with the camera. If ignoreUri is set to true, the picture taken will be saved
     * at the default directory for pictures on device. Otherwise, it will be saved at the uri
     * location if the uri is not null. If it is null, an error will be thrown.
     */
    suspend fun takePicture(
        contentResolver: ContentResolver,
        imageCaptureUri: Uri?,
        ignoreUri: Boolean = false
    ): ImageCapture.OutputFileResults

    suspend fun startVideoRecording(onVideoRecord: (OnVideoRecordEvent) -> Unit)

    fun stopVideoRecording()

    fun setZoomScale(scale: Float)

    fun getZoomScale(): StateFlow<Float>

    fun getSurfaceRequest(): StateFlow<SurfaceRequest?>

    fun getScreenFlashEvents(): SharedFlow<ScreenFlashEvent>

    fun getCurrentSettings(): StateFlow<CameraAppSettings?>

    fun setFlashMode(flashMode: FlashMode)

    fun isScreenFlashEnabled(): Boolean

    suspend fun setAspectRatio(aspectRatio: AspectRatio)

    suspend fun setLensFacing(lensFacing: LensFacing)

    fun tapToFocus(display: Display, surfaceWidth: Int, surfaceHeight: Int, x: Float, y: Float)

    suspend fun setCaptureMode(captureMode: CaptureMode)

    suspend fun setDynamicRange(dynamicRange: DynamicRange)

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
        object OnVideoRecorded : OnVideoRecordEvent

        object OnVideoRecordError : OnVideoRecordEvent
    }
}
