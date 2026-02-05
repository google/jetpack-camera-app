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
package com.google.jetpackcamera.settings

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.model.CameraAppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Data layer for settings.
 */
interface SettingsRepository {

    val defaultCameraAppSettings: Flow<CameraAppSettings>

    /**
     * Returns the current [CameraAppSettings].
     */
    suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings

    /**
     * Updates the default camera lens facing setting.
     *
     * @param lensFacing The new [LensFacing] to set as default.
     */
    suspend fun updateDefaultLensFacing(lensFacing: LensFacing)

    /**
     * Updates the dark mode status setting.
     *
     * @param darkMode The new [DarkMode] status.
     */
    suspend fun updateDarkModeStatus(darkMode: DarkMode)

    /**
     * Updates the flash mode status setting.
     *
     * @param flashMode The new [FlashMode] status.
     */
    suspend fun updateFlashModeStatus(flashMode: FlashMode)

    /**
     * Updates the aspect ratio setting.
     *
     * @param aspectRatio The new [AspectRatio] to set.
     */
    suspend fun updateAspectRatio(aspectRatio: AspectRatio)

    /**
     * Updates the stream configuration setting.
     *
     * @param streamConfig The new [StreamConfig] to set.
     */
    suspend fun updateStreamConfig(streamConfig: StreamConfig)

    /**
     * Updates the low light boost priority setting.
     *
     * @param lowLightBoostPriority The new [LowLightBoostPriority] to set.
     */
    suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority)

    /**
     * Updates the stabilization mode setting.
     *
     * @param stabilizationMode The new [StabilizationMode] to set.
     */
    suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode)

    /**
     * Updates the dynamic range setting.
     *
     * @param dynamicRange The new [DynamicRange] to set.
     */
    suspend fun updateDynamicRange(dynamicRange: DynamicRange)

    /**
     * Updates the target frame rate setting.
     *
     * @param targetFrameRate The new target frame rate to set.
     */
    suspend fun updateTargetFrameRate(targetFrameRate: Int)

    /**
     * Updates the image output format setting.
     *
     * @param imageFormat The new [ImageOutputFormat] to set.
     */
    suspend fun updateImageFormat(imageFormat: ImageOutputFormat)

    /**
     * Updates the maximum video duration setting.
     *
     * @param durationMillis The new maximum video duration in milliseconds.
     */
    suspend fun updateMaxVideoDuration(durationMillis: Long)

    /**
     * Updates the video quality setting.
     *
     * @param videoQuality The new [VideoQuality] to set.
     */
    suspend fun updateVideoQuality(videoQuality: VideoQuality)

    /**
     * Updates whether audio is enabled for video recording.
     *
     * @param isAudioEnabled True to enable audio, false to disable.
     */
    suspend fun updateAudioEnabled(isAudioEnabled: Boolean)
}
