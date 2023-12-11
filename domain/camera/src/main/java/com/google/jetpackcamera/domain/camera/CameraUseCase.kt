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
import android.content.ContentValues
import android.util.Rational
import android.view.Display
import androidx.camera.core.Preview
import com.google.jetpackcamera.settings.model.AspectRatio as SettingsAspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode as SettingsCaptureMode
import com.google.jetpackcamera.settings.model.FlashMode as SettingsFlashMode

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
     * Starts the camera with lensFacing with the provided [Preview.SurfaceProvider].
     *
     * The camera will run until the calling coroutine is cancelled.
     */
    suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
        currentCameraSettings: CameraAppSettings
    )

    suspend fun takePicture(
        contentResolver: ContentResolver,
        contentValues: ContentValues?,
        takePictureCallback: TakePictureCallback
    )

    suspend fun startVideoRecording()

    fun stopVideoRecording()

    fun setZoomScale(scale: Float): Float

    fun setFlashMode(flashMode: SettingsFlashMode)

    suspend fun setAspectRatio(aspectRatio: SettingsAspectRatio, isFrontFacing: Boolean)

    suspend fun flipCamera(isFrontFacing: Boolean)

    fun tapToFocus(display: Display, surfaceWidth: Int, surfaceHeight: Int, x: Float, y: Float)

    suspend fun setCaptureMode(captureMode: SettingsCaptureMode)

    companion object {
        const val INVALID_ZOOM_SCALE = -1f
    }

    /**
     * Data class holding information used for configuring [CameraUseCase].
     */
    data class Config(
        val lensFacing: LensFacing = LensFacing.FRONT,
        val captureMode: CaptureMode = CaptureMode.SINGLE_STREAM,
        val aspectRatio: AspectRatio = AspectRatio.ASPECT_RATIO_4_3,
        val flashMode: FlashMode = FlashMode.OFF
    )

    /**
     * Represents the lens used by [CameraUseCase].
     */
    enum class LensFacing {
        FRONT,
        BACK
    }

    /**
     * Represents the capture mode used by [CameraUseCase].
     */
    enum class CaptureMode {
        MULTI_STREAM,
        SINGLE_STREAM
    }

    /**
     * Represents the aspect ratio used by [CameraUseCase].
     */
    enum class AspectRatio(val rational: Rational) {
        ASPECT_RATIO_4_3(Rational(4, 3)),
        ASPECT_RATIO_16_9(Rational(16, 9)),
        ASPECT_RATIO_1_1(Rational(1, 1))
    }

    /**
     * Represents the flash mode used by [CameraUseCase].
     */
    enum class FlashMode {
        OFF,
        ON,
        AUTO
    }
}
