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
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.model.CameraAppSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * Implementation of [SettingsRepository] delegating to [SettingsDataSource].
 */
class LocalSettingsRepository @Inject constructor(
    private val settingsDataSource: SettingsDataSource
) : SettingsRepository {

    override val defaultCameraAppSettings: Flow<CameraAppSettings> =
        settingsDataSource.defaultCameraAppSettings

    override suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings =
        settingsDataSource.getCurrentDefaultCameraAppSettings()

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        settingsDataSource.updateDefaultLensFacing(lensFacing)
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        settingsDataSource.updateDarkModeStatus(darkMode)
    }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) {
        settingsDataSource.updateFlashModeStatus(flashMode)
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) {
        settingsDataSource.updateAspectRatio(aspectRatio)
    }

    override suspend fun updateSelectedCameraEffect(selectedCameraEffect: String) {
        settingsDataSource.updateSelectedCameraEffect(selectedCameraEffect)
    }

    override suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) {
        settingsDataSource.updateLowLightBoostPriority(lowLightBoostPriority)
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        settingsDataSource.updateStabilizationMode(stabilizationMode)
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        settingsDataSource.updateDynamicRange(dynamicRange)
    }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        settingsDataSource.updateTargetFrameRate(targetFrameRate)
    }

    override suspend fun updateImageFormat(imageFormat: ImageOutputFormat) {
        settingsDataSource.updateImageFormat(imageFormat)
    }

    override suspend fun updateMaxVideoDuration(durationMillis: Long) {
        settingsDataSource.updateMaxVideoDuration(durationMillis)
    }

    override suspend fun updateVideoQuality(videoQuality: VideoQuality) {
        settingsDataSource.updateVideoQuality(videoQuality)
    }

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) {
        settingsDataSource.updateAudioEnabled(isAudioEnabled)
    }

    override suspend fun updateConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        settingsDataSource.updateConcurrentCameraMode(concurrentCameraMode)
    }
}
