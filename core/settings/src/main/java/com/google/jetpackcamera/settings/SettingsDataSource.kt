/*
 * Copyright (C) 2026 The Android Open Source Project
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
 * Data source interface for fetching and updating persistent camera application settings.
 */
interface SettingsDataSource {

    /**
     * A [Flow] emitting the current default [CameraAppSettings].
     */
    val defaultCameraAppSettings: Flow<CameraAppSettings>

    /**
     * Retrieves the current default [CameraAppSettings] as a single snapshot.
     */
    suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings

    /**
     * Updates the default camera lens facing selection.
     */
    suspend fun updateDefaultLensFacing(lensFacing: LensFacing)

    /**
     * Updates the user-preferred dark mode setting.
     */
    suspend fun updateDarkModeStatus(darkMode: DarkMode)

    /**
     * Updates the default flash mode selection.
     */
    suspend fun updateFlashModeStatus(flashMode: FlashMode)

    /**
     * Updates the default capture aspect ratio.
     */
    suspend fun updateAspectRatio(aspectRatio: AspectRatio)

    /**
     * Updates the default stream configuration mode.
     */
    suspend fun updateStreamConfig(streamConfig: StreamConfig)

    /**
     * Updates the low light boost execution priority setting.
     */
    suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority)

    /**
     * Updates the default video stabilization mode.
     */
    suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode)

    /**
     * Updates the default capture dynamic range format.
     */
    suspend fun updateDynamicRange(dynamicRange: DynamicRange)

    /**
     * Updates the target frames-per-second setting for video recording.
     */
    suspend fun updateTargetFrameRate(targetFrameRate: Int)

    /**
     * Updates the default image capture output format.
     */
    suspend fun updateImageFormat(imageFormat: ImageOutputFormat)

    /**
     * Updates the maximum video duration limit in milliseconds.
     */
    suspend fun updateMaxVideoDuration(durationMillis: Long)

    /**
     * Updates the default video recording output quality setting.
     */
    suspend fun updateVideoQuality(videoQuality: VideoQuality)

    /**
     * Updates whether audio recording is enabled by default during video capture.
     */
    suspend fun updateAudioEnabled(isAudioEnabled: Boolean)
}
