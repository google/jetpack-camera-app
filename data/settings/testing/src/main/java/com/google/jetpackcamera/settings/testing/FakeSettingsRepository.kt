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
package com.google.jetpackcamera.settings.testing

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSettingsRepository(
    initialSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
) : SettingsRepository {
    private val _defaultCameraAppSettings = MutableStateFlow(initialSettings)
    override val defaultCameraAppSettings: Flow<CameraAppSettings> =
        _defaultCameraAppSettings.asStateFlow()

    override suspend fun getCurrentDefaultCameraAppSettings() = _defaultCameraAppSettings.value

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(cameraLensFacing = lensFacing)
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        _defaultCameraAppSettings.value = _defaultCameraAppSettings.value.copy(darkMode = darkMode)
    }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) {
        _defaultCameraAppSettings.value = _defaultCameraAppSettings.value.copy(
            flashMode = flashMode
        )
    }

    override suspend fun updateStreamConfig(streamConfig: StreamConfig) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(streamConfig = streamConfig)
    }

    override suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(lowLightBoostPriority = lowLightBoostPriority)
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(stabilizationMode = stabilizationMode)
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(dynamicRange = dynamicRange)
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(aspectRatio = aspectRatio)
    }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(targetFrameRate = targetFrameRate)
    }

    override suspend fun updateImageFormat(imageFormat: ImageOutputFormat) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(imageFormat = imageFormat)
    }

    override suspend fun updateMaxVideoDuration(durationMillis: Long) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(maxVideoDurationMillis = durationMillis)
    }

    override suspend fun updateVideoQuality(videoQuality: VideoQuality) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(videoQuality = videoQuality)
    }

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(audioEnabled = isAudioEnabled)
    }

    override suspend fun updateConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        _defaultCameraAppSettings.value =
            _defaultCameraAppSettings.value.copy(concurrentCameraMode = concurrentCameraMode)
    }
}
