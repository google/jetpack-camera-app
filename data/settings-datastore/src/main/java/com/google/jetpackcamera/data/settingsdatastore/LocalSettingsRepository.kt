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
package com.google.jetpackcamera.data.settingsdatastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.jetpackcamera.core.common.DefaultCaptureModeOverride
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.UNLIMITED_VIDEO_DURATION
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementation of [SettingsRepository] with locally stored Preferences DataStore.
 */
class LocalSettingsRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @DefaultCaptureModeOverride private val defaultCaptureModeOverride: CaptureMode
) : SettingsRepository {

    override val defaultCameraAppSettings: Flow<CameraAppSettings> = dataStore.data.map { prefs ->
        CameraAppSettings(
            cameraLensFacing = prefs[PreferenceKeys.KEY_LENS_FACING].toEnumOrDefault(LensFacing.BACK),
            darkMode = prefs[PreferenceKeys.KEY_DARK_MODE].toEnumOrDefault(DarkMode.DARK),
            flashMode = prefs[PreferenceKeys.KEY_FLASH_MODE].toEnumOrDefault(FlashMode.OFF),
            aspectRatio = prefs[PreferenceKeys.KEY_ASPECT_RATIO].toEnumOrDefault(AspectRatio.NINE_SIXTEEN),
            stabilizationMode = prefs[PreferenceKeys.KEY_STABILIZATION_MODE].toEnumOrDefault(StabilizationMode.AUTO),
            targetFrameRate = prefs[PreferenceKeys.KEY_TARGET_FRAME_RATE] ?: 0,
            streamConfig = prefs[PreferenceKeys.KEY_STREAM_CONFIG].toEnumOrDefault(StreamConfig.MULTI_STREAM),
            lowLightBoostPriority = prefs[PreferenceKeys.KEY_LOW_LIGHT_BOOST_PRIORITY]
                .toEnumOrDefault(LowLightBoostPriority.PRIORITIZE_AE_MODE),
            dynamicRange = prefs[PreferenceKeys.KEY_DYNAMIC_RANGE].toEnumOrDefault(DynamicRange.SDR),
            imageFormat = prefs[PreferenceKeys.KEY_IMAGE_FORMAT].toEnumOrDefault(ImageOutputFormat.JPEG),
            maxVideoDurationMillis = prefs[PreferenceKeys.KEY_MAX_VIDEO_DURATION] ?: UNLIMITED_VIDEO_DURATION,
            videoQuality = prefs[PreferenceKeys.KEY_VIDEO_QUALITY].toEnumOrDefault(VideoQuality.UNSPECIFIED),
            audioEnabled = prefs[PreferenceKeys.KEY_AUDIO_ENABLED] ?: true,
            captureMode = defaultCaptureModeOverride
        )
    }

    override suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings =
        defaultCameraAppSettings.first()

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_LENS_FACING] = lensFacing.name
        }
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_DARK_MODE] = darkMode.name
        }
    }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_FLASH_MODE] = flashMode.name
        }
    }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_TARGET_FRAME_RATE] = targetFrameRate
        }
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_ASPECT_RATIO] = aspectRatio.name
        }
    }

    override suspend fun updateStreamConfig(streamConfig: StreamConfig) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_STREAM_CONFIG] = streamConfig.name
        }
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_STABILIZATION_MODE] = stabilizationMode.name
        }
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_DYNAMIC_RANGE] = dynamicRange.name
        }
    }

    override suspend fun updateImageFormat(imageFormat: ImageOutputFormat) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_IMAGE_FORMAT] = imageFormat.name
        }
    }

    override suspend fun updateMaxVideoDuration(durationMillis: Long) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_MAX_VIDEO_DURATION] = durationMillis
        }
    }

    override suspend fun updateVideoQuality(videoQuality: VideoQuality) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_VIDEO_QUALITY] = videoQuality.name
        }
    }

    override suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_LOW_LIGHT_BOOST_PRIORITY] = lowLightBoostPriority.name
        }
    }

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[PreferenceKeys.KEY_AUDIO_ENABLED] = isAudioEnabled
        }
    }

    companion object {
        private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T {
            if (this == null) return default
            return enumValues<T>().firstOrNull { it.name == this } ?: default
        }
    }
}
