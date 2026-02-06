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

package com.google.jetpackcamera.data.settings_datastore

import androidx.datastore.core.DataStore
import com.google.jetpackcamera.core.common.DefaultCaptureModeOverride
import com.google.jetpackcamera.model.mappers.toDomain
import com.google.jetpackcamera.model.mappers.toProto
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
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.JcaSettings
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import com.google.jetpackcamera.model.proto.AspectRatio as AspectRatioProto
import com.google.jetpackcamera.model.proto.DarkMode as DarkModeProto
import com.google.jetpackcamera.model.proto.FlashMode as FlashModeProto
import com.google.jetpackcamera.model.proto.StabilizationMode as StabilizationModeProto
import com.google.jetpackcamera.model.proto.StreamConfig as StreamConfigProto


/**
 * Implementation of [com.google.jetpackcamera.settings.SettingsRepository] with locally stored settings.
 */
class LocalSettingsRepository @Inject constructor(
    private val jcaSettings: DataStore<JcaSettings>,
    @DefaultCaptureModeOverride private val defaultCaptureModeOverride: CaptureMode
) :
    SettingsRepository {

    override val defaultCameraAppSettings = jcaSettings.data
        .map {
            CameraAppSettings(
                cameraLensFacing = it.defaultLensFacing.toDomain(),
                darkMode = when (it.darkModeStatus) {
                    DarkModeProto.DARK_MODE_DARK -> DarkMode.DARK
                    DarkModeProto.DARK_MODE_LIGHT -> DarkMode.LIGHT
                    DarkModeProto.DARK_MODE_SYSTEM -> DarkMode.SYSTEM
                    else -> DarkMode.DARK
                },
                flashMode = when (it.flashModeStatus) {
                    FlashModeProto.FLASH_MODE_AUTO -> FlashMode.AUTO
                    FlashModeProto.FLASH_MODE_ON -> FlashMode.ON
                    FlashModeProto.FLASH_MODE_OFF -> FlashMode.OFF
                    FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST -> FlashMode.LOW_LIGHT_BOOST
                    else -> FlashMode.OFF
                },
                aspectRatio = it.aspectRatioStatus.toDomain(),
                stabilizationMode = it.stabilizationMode.toDomain(),
                targetFrameRate = it.targetFrameRate,
                streamConfig = when (it.streamConfigStatus) {
                    StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM -> StreamConfig.SINGLE_STREAM
                    StreamConfigProto.STREAM_CONFIG_MULTI_STREAM -> StreamConfig.MULTI_STREAM
                    else -> StreamConfig.MULTI_STREAM
                },
                lowLightBoostPriority = toDomain(it.lowLightBoostPriority),
                dynamicRange = it.dynamicRangeStatus.toDomain(),
                imageFormat = it.imageFormatStatus.toDomain(),
                maxVideoDurationMillis = it.maxVideoDurationMillis,
                videoQuality = it.videoQuality.toDomain(),
                audioEnabled = it.audioEnabledStatus,
                captureMode = defaultCaptureModeOverride
            )
        }

    override suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings =
        defaultCameraAppSettings.first()

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDefaultLensFacing(lensFacing.toProto())
                .build()
        }
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        val newStatus = when (darkMode) {
            DarkMode.DARK -> DarkModeProto.DARK_MODE_DARK
            DarkMode.LIGHT -> DarkModeProto.DARK_MODE_LIGHT
            DarkMode.SYSTEM -> DarkModeProto.DARK_MODE_SYSTEM
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDarkModeStatus(newStatus)
                .build()
        }
    }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) {
        val newStatus = when (flashMode) {
            FlashMode.AUTO -> FlashModeProto.FLASH_MODE_AUTO
            FlashMode.ON -> FlashModeProto.FLASH_MODE_ON
            FlashMode.OFF -> FlashModeProto.FLASH_MODE_OFF
            FlashMode.LOW_LIGHT_BOOST -> FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setFlashModeStatus(newStatus)
                .build()
        }
    }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setTargetFrameRate(targetFrameRate)
                .build()
        }
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) {
        val newStatus = when (aspectRatio) {
            AspectRatio.NINE_SIXTEEN -> AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN
            AspectRatio.THREE_FOUR -> AspectRatioProto.ASPECT_RATIO_THREE_FOUR
            AspectRatio.ONE_ONE -> AspectRatioProto.ASPECT_RATIO_ONE_ONE
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setAspectRatioStatus(newStatus)
                .build()
        }
    }

    override suspend fun updateStreamConfig(streamConfig: StreamConfig) {
        val newStatus = when (streamConfig) {
            StreamConfig.MULTI_STREAM -> StreamConfigProto.STREAM_CONFIG_MULTI_STREAM
            StreamConfig.SINGLE_STREAM -> StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setStreamConfigStatus(newStatus)
                .build()
        }
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        val newStatus = when (stabilizationMode) {
            StabilizationMode.OFF -> StabilizationModeProto.STABILIZATION_MODE_OFF
            StabilizationMode.AUTO -> StabilizationModeProto.STABILIZATION_MODE_AUTO
            StabilizationMode.ON -> StabilizationModeProto.STABILIZATION_MODE_ON
            StabilizationMode.HIGH_QUALITY -> StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY
            StabilizationMode.OPTICAL -> StabilizationModeProto.STABILIZATION_MODE_OPTICAL
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setStabilizationMode(newStatus)
                .build()
        }
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDynamicRangeStatus(dynamicRange.toProto())
                .build()
        }
    }

    override suspend fun updateImageFormat(imageFormat: ImageOutputFormat) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setImageFormatStatus(imageFormat.toProto())
                .build()
        }
    }

    override suspend fun updateMaxVideoDuration(durationMillis: Long) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setMaxVideoDurationMillis(durationMillis)
                .build()
        }
    }

    override suspend fun updateVideoQuality(videoQuality: VideoQuality) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setVideoQuality(videoQuality.toProto())
                .build()
        }
    }

    override suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setLowLightBoostPriority(lowLightBoostPriority.toProto())
                .build()
        }
    }

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setAudioEnabledStatus(isAudioEnabled)
                .build()
        }
    }
}