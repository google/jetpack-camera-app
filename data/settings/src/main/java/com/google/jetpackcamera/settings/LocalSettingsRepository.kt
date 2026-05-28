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

import android.content.SharedPreferences
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Implementation of [SettingsRepository] with locally stored settings.
 */
class LocalSettingsRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    @DefaultCaptureModeOverride private val defaultCaptureModeOverride: CaptureMode
) : SettingsRepository {

    private val _defaultCameraAppSettings = MutableStateFlow(readSettings())
    override val defaultCameraAppSettings: Flow<CameraAppSettings> =
        _defaultCameraAppSettings.asStateFlow()

    private fun readSettings(): CameraAppSettings {
        return CameraAppSettings(
            cameraLensFacing = sharedPreferences.getString(KEY_LENS_FACING, null)
                .toEnumOrDefault(LensFacing.BACK),
            darkMode = sharedPreferences.getString(KEY_DARK_MODE, null)
                .toEnumOrDefault(DarkMode.DARK),
            flashMode = sharedPreferences.getString(KEY_FLASH_MODE, null)
                .toEnumOrDefault(FlashMode.OFF),
            aspectRatio = sharedPreferences.getString(KEY_ASPECT_RATIO, null)
                .toEnumOrDefault(AspectRatio.NINE_SIXTEEN),
            stabilizationMode = sharedPreferences.getString(KEY_STABILIZATION_MODE, null)
                .toEnumOrDefault(StabilizationMode.AUTO),
            targetFrameRate = if (sharedPreferences.contains(KEY_TARGET_FRAME_RATE)) {
                sharedPreferences.getInt(KEY_TARGET_FRAME_RATE, 0)
            } else {
                0
            },
            streamConfig = sharedPreferences.getString(KEY_STREAM_CONFIG, null)
                .toEnumOrDefault(StreamConfig.MULTI_STREAM),
            lowLightBoostPriority = sharedPreferences.getString(KEY_LOW_LIGHT_BOOST_PRIORITY, null)
                .toEnumOrDefault(LowLightBoostPriority.PRIORITIZE_AE_MODE),
            dynamicRange = sharedPreferences.getString(KEY_DYNAMIC_RANGE, null)
                .toEnumOrDefault(DynamicRange.SDR),
            imageFormat = sharedPreferences.getString(KEY_IMAGE_FORMAT, null)
                .toEnumOrDefault(ImageOutputFormat.JPEG),
            maxVideoDurationMillis = if (sharedPreferences.contains(KEY_MAX_VIDEO_DURATION)) {
                sharedPreferences.getLong(KEY_MAX_VIDEO_DURATION, UNLIMITED_VIDEO_DURATION)
            } else {
                UNLIMITED_VIDEO_DURATION
            },
            videoQuality = sharedPreferences.getString(KEY_VIDEO_QUALITY, null)
                .toEnumOrDefault(VideoQuality.UNSPECIFIED),
            audioEnabled = sharedPreferences.getBoolean(KEY_AUDIO_ENABLED, true),
            captureMode = defaultCaptureModeOverride
        )
    }

    private fun updateSetting(update: SharedPreferences.Editor.() -> SharedPreferences.Editor) {
        sharedPreferences.edit().update().apply()
        _defaultCameraAppSettings.value = readSettings()
    }

    override suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings =
        defaultCameraAppSettings.first()

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) =
        updateSetting { putString(KEY_LENS_FACING, lensFacing.name) }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) =
        updateSetting { putString(KEY_DARK_MODE, darkMode.name) }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) =
        updateSetting { putString(KEY_FLASH_MODE, flashMode.name) }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) =
        updateSetting { putInt(KEY_TARGET_FRAME_RATE, targetFrameRate) }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) =
        updateSetting { putString(KEY_ASPECT_RATIO, aspectRatio.name) }

    override suspend fun updateStreamConfig(streamConfig: StreamConfig) =
        updateSetting { putString(KEY_STREAM_CONFIG, streamConfig.name) }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) =
        updateSetting { putString(KEY_STABILIZATION_MODE, stabilizationMode.name) }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) =
        updateSetting { putString(KEY_DYNAMIC_RANGE, dynamicRange.name) }

    override suspend fun updateImageFormat(imageFormat: ImageOutputFormat) =
        updateSetting { putString(KEY_IMAGE_FORMAT, imageFormat.name) }

    override suspend fun updateMaxVideoDuration(durationMillis: Long) =
        updateSetting { putLong(KEY_MAX_VIDEO_DURATION, durationMillis) }

    override suspend fun updateVideoQuality(videoQuality: VideoQuality) =
        updateSetting { putString(KEY_VIDEO_QUALITY, videoQuality.name) }

    override suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) =
        updateSetting { putString(KEY_LOW_LIGHT_BOOST_PRIORITY, lowLightBoostPriority.name) }

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) =
        updateSetting { putBoolean(KEY_AUDIO_ENABLED, isAudioEnabled) }

    companion object {
        private const val KEY_LENS_FACING = "lens_facing"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_FLASH_MODE = "flash_mode"
        private const val KEY_ASPECT_RATIO = "aspect_ratio"
        private const val KEY_STREAM_CONFIG = "stream_config"
        private const val KEY_STABILIZATION_MODE = "stabilization_mode"
        private const val KEY_DYNAMIC_RANGE = "dynamic_range"
        private const val KEY_VIDEO_QUALITY = "video_quality"
        private const val KEY_IMAGE_FORMAT = "image_format"
        private const val KEY_MAX_VIDEO_DURATION = "max_video_duration"
        private const val KEY_AUDIO_ENABLED = "audio_enabled"
        private const val KEY_LOW_LIGHT_BOOST_PRIORITY = "low_light_boost_priority"
        private const val KEY_TARGET_FRAME_RATE = "target_frame_rate"

        private inline fun <reified T : Enum<T>> String?.toEnumOrDefault(default: T): T {
            if (this == null) return default
            return enumValues<T>().firstOrNull { it.name == this } ?: default
        }
    }
}
