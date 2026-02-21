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
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.mappers.toDomain
import com.google.jetpackcamera.model.mappers.toProto
import com.google.jetpackcamera.model.proto.DarkMode as DarkModeProto
import com.google.jetpackcamera.settings.JcaSettings
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementation of [SettingsRepository] with locally stored settings.
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
                flashMode = it.flashModeStatus.toDomain(),
                aspectRatio = it.aspectRatioStatus.toDomain(),
                stabilizationMode = it.stabilizationMode.toDomain(),
                targetFrameRate = it.targetFrameRate,
                streamConfig = it.streamConfigStatus.toDomain(),
                lowLightBoostPriority = it.lowLightBoostPriority.toDomain(),
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
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setFlashModeStatus(flashMode.toProto())
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
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setAspectRatioStatus(aspectRatio.toProto())
                .build()
        }
    }

    override suspend fun updateStreamConfig(streamConfig: StreamConfig) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setStreamConfigStatus(streamConfig.toProto())
                .build()
        }
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        val newStatus = stabilizationMode.toProto()
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
