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

    /**
     * Takes a picture with the camera. The result of this operation is returned in
     * [CameraState.viewfinderResult].
     *
     * @param onCaptureStarted A callback that is invoked when the capture starts.
     */
    suspend fun takePicture(onCaptureStarted: (() -> Unit) = {})

    /**
     * Takes a picture with the camera and saves it to a specified [SaveLocation].
     *
     * @param contentResolver The [ContentResolver] to use for saving the image.
     * @param saveLocation The location to save the captured image.
     * @param onCaptureStarted A callback that is invoked when the capture starts.
     * @return An [ImageCapture.OutputFileResults] object containing the result of the capture.
     */
    suspend fun takePicture(
        contentResolver: ContentResolver,
        saveLocation: SaveLocation,
        onCaptureStarted: (() -> Unit) = {}
    ): ImageCapture.OutputFileResults

    /**
     * Starts video recording.
     *
     * @param saveLocation The location to save the recorded video.
     * @param onVideoRecord A callback to handle video recording events.
     */
    suspend fun startVideoRecording(
        saveLocation: SaveLocation,
        onVideoRecord: (OnVideoRecordEvent) -> Unit
    )

    /**
     * Pauses the current video recording.
     */
    suspend fun pauseVideoRecording()

    /**
     * Resumes the current video recording.
     */
    suspend fun resumeVideoRecording()

    /**
     * Stops the current video recording.
     */
    suspend fun stopVideoRecording()

    /**
     * Sets the zoom ratio for the camera.
     *
     * @param newZoomState The new zoom state to apply.
     */
    fun changeZoomRatio(newZoomState: CameraZoomRatio)

    /**
     * Sets the test pattern for the camera.
     *
     * @param newTestPattern The new test pattern to apply.
     */
    fun setTestPattern(newTestPattern: TestPattern)

    /**
     * Returns a [StateFlow] of the current [CameraState].
     */
    fun getCurrentCameraState(): StateFlow<CameraState>

    /**
     * Returns a [StateFlow] of the current [SurfaceRequest].
     */
    fun getSurfaceRequest(): StateFlow<SurfaceRequest?>

    /**
     * Returns a [ReceiveChannel] for [ScreenFlashEvent]s.
     */
    fun getScreenFlashEvents(): ReceiveChannel<ScreenFlashEvent>

    /**
     * Returns a [StateFlow] of the current [CameraAppSettings].
     */
    fun getCurrentSettings(): StateFlow<CameraAppSettings?>

    /**
     * Sets the flash mode for the camera.
     *
     * @param flashMode The [FlashMode] to set.
     */
    fun setFlashMode(flashMode: FlashMode)

    /**
     * Returns whether screen flash is currently enabled.
     */
    fun isScreenFlashEnabled(): Boolean

    /**
     * Sets the aspect ratio for the camera.
     *
     * @param aspectRatio The [AspectRatio] to set.
     */
    suspend fun setAspectRatio(aspectRatio: AspectRatio)

    /**
     * Sets the video quality for the camera.
     *
     * @param videoQuality The [VideoQuality] to set.
     */
    suspend fun setVideoQuality(videoQuality: VideoQuality)

    /**
     * Sets the low light boost priority.
     *
     * @param lowLightBoostPriority The [LowLightBoostPriority] to set.
     */
    suspend fun setLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority)

    /**
     * Sets the lens facing for the camera.
     *
     * @param lensFacing The [LensFacing] to set.
     */
    suspend fun setLensFacing(lensFacing: LensFacing)

    /**
     * Initiates a tap-to-focus action at the specified coordinates.
     *
     * @param x The x-coordinate of the focus point.
     * @param y The y-coordinate of the focus point.
     */
    suspend fun tapToFocus(x: Float, y: Float)

    /**
     * Sets the stream configuration for the camera.
     *
     * @param streamConfig The [StreamConfig] to set.
     */
    suspend fun setStreamConfig(streamConfig: StreamConfig)

    /**
     * Sets the dynamic range for the camera.
     *
     * @param dynamicRange The [DynamicRange] to set.
     */
    suspend fun setDynamicRange(dynamicRange: DynamicRange)

    /**
     * Sets the device rotation.
     *
     * @param deviceRotation The [DeviceRotation] to set.
     */
    fun setDeviceRotation(deviceRotation: DeviceRotation)

    /**
     * Sets the concurrent camera mode.
     *
     * @param concurrentCameraMode The [ConcurrentCameraMode] to set.
     */
    suspend fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode)

    /**
     * Sets the image output format.
     *
     * @param imageFormat The [ImageOutputFormat] to set.
     */
    suspend fun setImageFormat(imageFormat: ImageOutputFormat)

    /**
     * Sets whether audio is enabled for video recording.
     *
     * @param isAudioEnabled Whether audio should be enabled.
     */
    suspend fun setAudioEnabled(isAudioEnabled: Boolean)

    /**
     * Sets the video stabilization mode.
     *
     * @param stabilizationMode The [StabilizationMode] to set.
     */
    suspend fun setStabilizationMode(stabilizationMode: StabilizationMode)

    /**
     * Sets the target frame rate for video recording.
     *
     * @param targetFrameRate The target frame rate in frames per second.
     */
    suspend fun setTargetFrameRate(targetFrameRate: Int)

    /**
     * Sets the maximum video duration.
     *
     * @param durationInMillis The maximum duration in milliseconds.
     */
    suspend fun setMaxVideoDuration(durationInMillis: Long)

    /**
     * Sets the capture mode for the camera.
     *
     * @param captureMode The [CaptureMode] to set.
     */
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

    companion object {
        /**
         * Applies an individual camera app setting with the given [settingExtractor] and
         * [settingApplicator] if the new setting differs from the old setting.
         */
        private suspend inline fun <R> CameraAppSettings.applyDiff(
            new: CameraAppSettings,
            settingExtractor: CameraAppSettings.() -> R,
            crossinline settingApplicator: suspend (R) -> Unit
        ) {
            val oldSetting = settingExtractor.invoke(this)
            val newSetting = settingExtractor.invoke(new)
            if (oldSetting != newSetting) {
                settingApplicator(newSetting)
            }
        }

        /**
         * Checks whether each actionable individual setting has changed and applies them to
         * [CameraSystem].
         */
        suspend fun CameraAppSettings.applyDiffs(
            new: CameraAppSettings,
            cameraSystem: CameraSystem
        ) {
            applyDiff(new, CameraAppSettings::cameraLensFacing, cameraSystem::setLensFacing)
            applyDiff(new, CameraAppSettings::flashMode, cameraSystem::setFlashMode)
            applyDiff(new, CameraAppSettings::streamConfig, cameraSystem::setStreamConfig)
            applyDiff(new, CameraAppSettings::aspectRatio, cameraSystem::setAspectRatio)
            applyDiff(new, CameraAppSettings::stabilizationMode, cameraSystem::setStabilizationMode)
            applyDiff(new, CameraAppSettings::targetFrameRate, cameraSystem::setTargetFrameRate)
            applyDiff(
                new,
                CameraAppSettings::maxVideoDurationMillis,
                cameraSystem::setMaxVideoDuration
            )
            applyDiff(new, CameraAppSettings::videoQuality, cameraSystem::setVideoQuality)
            applyDiff(new, CameraAppSettings::audioEnabled, cameraSystem::setAudioEnabled)
            applyDiff(
                new,
                CameraAppSettings::lowLightBoostPriority,
                cameraSystem::setLowLightBoostPriority
            )
        }
    }
}
