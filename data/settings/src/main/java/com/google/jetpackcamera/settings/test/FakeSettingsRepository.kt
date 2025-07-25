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
package com.google.jetpackcamera.settings.test

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow

object FakeSettingsRepository : SettingsRepository {
    private var currentCameraSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS

    override val defaultCameraAppSettings: Flow<CameraAppSettings> =
        flow { emit(currentCameraSettings) }

    override suspend fun getCurrentDefaultCameraAppSettings() = defaultCameraAppSettings.first()

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        currentCameraSettings = currentCameraSettings.copy(cameraLensFacing = lensFacing)
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        currentCameraSettings = currentCameraSettings.copy(darkMode = darkMode)
    }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) {
        currentCameraSettings = currentCameraSettings.copy(flashMode = flashMode)
    }

    override suspend fun updateStreamConfig(streamConfig: StreamConfig) {
        currentCameraSettings =
            currentCameraSettings.copy(streamConfig = streamConfig)
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        currentCameraSettings =
            currentCameraSettings.copy(stabilizationMode = stabilizationMode)
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        currentCameraSettings =
            currentCameraSettings.copy(dynamicRange = dynamicRange)
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) {
        currentCameraSettings =
            currentCameraSettings.copy(aspectRatio = aspectRatio)
    }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        currentCameraSettings =
            currentCameraSettings.copy(targetFrameRate = targetFrameRate)
    }

    override suspend fun updateImageFormat(imageFormat: ImageOutputFormat) {
        currentCameraSettings = currentCameraSettings.copy(imageFormat = imageFormat)
    }

    override suspend fun updateMaxVideoDuration(durationMillis: Long) {
        currentCameraSettings = currentCameraSettings.copy(maxVideoDurationMillis = durationMillis)
    }

    override suspend fun updateVideoQuality(videoQuality: VideoQuality) {
        currentCameraSettings = currentCameraSettings.copy(videoQuality = videoQuality)
    }

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) {
        currentCameraSettings =
            currentCameraSettings.copy(audioEnabled = isAudioEnabled)
    }
}
