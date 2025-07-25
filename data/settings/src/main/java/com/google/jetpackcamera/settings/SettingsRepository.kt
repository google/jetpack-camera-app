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

    suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings

    suspend fun updateDefaultLensFacing(lensFacing: LensFacing)

    suspend fun updateDarkModeStatus(darkMode: DarkMode)

    suspend fun updateFlashModeStatus(flashMode: FlashMode)

    suspend fun updateAspectRatio(aspectRatio: AspectRatio)

    suspend fun updateStreamConfig(streamConfig: StreamConfig)

    suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode)

    suspend fun updateDynamicRange(dynamicRange: DynamicRange)

    suspend fun updateTargetFrameRate(targetFrameRate: Int)

    suspend fun updateImageFormat(imageFormat: ImageOutputFormat)

    suspend fun updateMaxVideoDuration(durationMillis: Long)

    suspend fun updateVideoQuality(videoQuality: VideoQuality)

    suspend fun updateAudioEnabled(isAudioEnabled: Boolean)
}
