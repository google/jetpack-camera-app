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

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
import com.google.jetpackcamera.settings.model.CameraAppSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementation of [SettingsRepository] with locally stored settings.
 */
class LocalSettingsRepository @Inject constructor(
    private val jcaSettings: DataStore<Preferences>,
    @DefaultCaptureModeOverride private val defaultCaptureModeOverride: CaptureMode
) : SettingsRepository {

    companion object {
        private val KEY_LENS_FACING = stringPreferencesKey("lens_facing")
        private val KEY_DARK_MODE = stringPreferencesKey("dark_mode")
        private val KEY_FLASH_MODE = stringPreferencesKey("flash_mode")
        private val KEY_ASPECT_RATIO = stringPreferencesKey("aspect_ratio")
        private val KEY_STREAM_CONFIG = stringPreferencesKey("stream_config")
        private val KEY_STABILIZATION_MODE = stringPreferencesKey("stabilization_mode")
        private val KEY_DYNAMIC_RANGE = stringPreferencesKey("dynamic_range")
        private val KEY_VIDEO_QUALITY = stringPreferencesKey("video_quality")
        private val KEY_IMAGE_FORMAT = stringPreferencesKey("image_format")
        private val KEY_MAX_VIDEO_DURATION = longPreferencesKey("max_video_duration")
        private val KEY_AUDIO_ENABLED = booleanPreferencesKey("audio_enabled")
        private val KEY_LOW_LIGHT_BOOST_PRIORITY = stringPreferencesKey("low_light_boost_priority")
        private val KEY_TARGET_FRAME_RATE = intPreferencesKey("target_frame_rate")
    }

    override val defaultCameraAppSettings = jcaSettings.data
        .map { preferences ->
            CameraAppSettings(
                cameraLensFacing = preferences[KEY_LENS_FACING]?.let { LensFacing.valueOf(it) } ?: LensFacing.BACK,
                darkMode = preferences[KEY_DARK_MODE]?.let { DarkMode.valueOf(it) } ?: DarkMode.DARK,
                flashMode = preferences[KEY_FLASH_MODE]?.let { FlashMode.valueOf(it) } ?: FlashMode.OFF,
                aspectRatio = preferences[KEY_ASPECT_RATIO]?.let { AspectRatio.valueOf(it) } ?: AspectRatio.NINE_SIXTEEN,
                stabilizationMode = preferences[KEY_STABILIZATION_MODE]?.let { StabilizationMode.valueOf(it) } ?: StabilizationMode.AUTO,
                targetFrameRate = preferences[KEY_TARGET_FRAME_RATE] ?: 0,
                streamConfig = preferences[KEY_STREAM_CONFIG]?.let { StreamConfig.valueOf(it) } ?: StreamConfig.MULTI_STREAM,
                lowLightBoostPriority = preferences[KEY_LOW_LIGHT_BOOST_PRIORITY]?.let { LowLightBoostPriority.valueOf(it) } ?: LowLightBoostPriority.PRIORITIZE_AE_MODE,
                dynamicRange = preferences[KEY_DYNAMIC_RANGE]?.let { DynamicRange.valueOf(it) } ?: DynamicRange.SDR,
                imageFormat = preferences[KEY_IMAGE_FORMAT]?.let { ImageOutputFormat.valueOf(it) } ?: ImageOutputFormat.JPEG,
                maxVideoDurationMillis = preferences[KEY_MAX_VIDEO_DURATION] ?: UNLIMITED_VIDEO_DURATION,
                videoQuality = preferences[KEY_VIDEO_QUALITY]?.let { VideoQuality.valueOf(it) } ?: VideoQuality.UNSPECIFIED,
                audioEnabled = preferences[KEY_AUDIO_ENABLED] ?: true,
                captureMode = defaultCaptureModeOverride
            )
        }

    override suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings =
        defaultCameraAppSettings.first()

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        jcaSettings.edit { preferences ->
            preferences[KEY_LENS_FACING] = lensFacing.name
        }
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        jcaSettings.edit { preferences ->
            preferences[KEY_DARK_MODE] = darkMode.name
        }
    }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) {
        jcaSettings.edit { preferences ->
            preferences[KEY_FLASH_MODE] = flashMode.name
        }
    }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        jcaSettings.edit { preferences ->
            preferences[KEY_TARGET_FRAME_RATE] = targetFrameRate
        }
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) {
        jcaSettings.edit { preferences ->
            preferences[KEY_ASPECT_RATIO] = aspectRatio.name
        }
    }

    override suspend fun updateStreamConfig(streamConfig: StreamConfig) {
        jcaSettings.edit { preferences ->
            preferences[KEY_STREAM_CONFIG] = streamConfig.name
        }
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        jcaSettings.edit { preferences ->
            preferences[KEY_STABILIZATION_MODE] = stabilizationMode.name
        }
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        jcaSettings.edit { preferences ->
            preferences[KEY_DYNAMIC_RANGE] = dynamicRange.name
        }
    }

    override suspend fun updateImageFormat(imageFormat: ImageOutputFormat) {
        jcaSettings.edit { preferences ->
            preferences[KEY_IMAGE_FORMAT] = imageFormat.name
        }
    }

    override suspend fun updateMaxVideoDuration(durationMillis: Long) {
        jcaSettings.edit { preferences ->
            preferences[KEY_MAX_VIDEO_DURATION] = durationMillis
        }
    }

    override suspend fun updateVideoQuality(videoQuality: VideoQuality) {
        jcaSettings.edit { preferences ->
            preferences[KEY_VIDEO_QUALITY] = videoQuality.name
        }
    }

    override suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) {
        jcaSettings.edit { preferences ->
            preferences[KEY_LOW_LIGHT_BOOST_PRIORITY] = lowLightBoostPriority.name
        }
    }

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) {
        jcaSettings.edit { preferences ->
            preferences[KEY_AUDIO_ENABLED] = isAudioEnabled
        }
    }
}
