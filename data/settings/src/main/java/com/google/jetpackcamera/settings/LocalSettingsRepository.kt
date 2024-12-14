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
import com.google.jetpackcamera.settings.AspectRatio as AspectRatioProto
import com.google.jetpackcamera.settings.DarkMode as DarkModeProto
import com.google.jetpackcamera.settings.FlashMode as FlashModeProto
import com.google.jetpackcamera.settings.StabilizationMode as StabilizationModeProto
import com.google.jetpackcamera.settings.StreamConfig as StreamConfigProto
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.DynamicRange.Companion.toProto
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.ImageOutputFormat.Companion.toProto
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LensFacing.Companion.toProto
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.StreamConfig
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementation of [SettingsRepository] with locally stored settings.
 */
class LocalSettingsRepository @Inject constructor(private val jcaSettings: DataStore<JcaSettings>) :
    SettingsRepository {

    override val defaultCameraAppSettings = jcaSettings.data
        .map {
            CameraAppSettings(
                cameraLensFacing = LensFacing.fromProto(it.defaultLensFacing),
                darkMode = when (it.darkModeStatus) {
                    DarkModeProto.DARK_MODE_DARK -> DarkMode.DARK
                    DarkModeProto.DARK_MODE_LIGHT -> DarkMode.LIGHT
                    DarkModeProto.DARK_MODE_SYSTEM -> DarkMode.SYSTEM
                    else -> DarkMode.SYSTEM
                },
                flashMode = when (it.flashModeStatus) {
                    FlashModeProto.FLASH_MODE_AUTO -> FlashMode.AUTO
                    FlashModeProto.FLASH_MODE_ON -> FlashMode.ON
                    FlashModeProto.FLASH_MODE_OFF -> FlashMode.OFF
                    FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST -> FlashMode.LOW_LIGHT_BOOST
                    else -> FlashMode.OFF
                },
                aspectRatio = AspectRatio.fromProto(it.aspectRatioStatus),
                stabilizationMode = StabilizationMode.fromProto(it.stabilizationMode),
                targetFrameRate = it.targetFrameRate,
                streamConfig = when (it.streamConfigStatus) {
                    StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM -> StreamConfig.SINGLE_STREAM
                    StreamConfigProto.STREAM_CONFIG_MULTI_STREAM -> StreamConfig.MULTI_STREAM
                    else -> StreamConfig.MULTI_STREAM
                },
                dynamicRange = DynamicRange.fromProto(it.dynamicRangeStatus),
                imageFormat = ImageOutputFormat.fromProto(it.imageFormatStatus),
                maxVideoDurationMillis = it.maxVideoDurationMillis
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

    override suspend fun updateCaptureMode(streamConfig: StreamConfig) {
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
}
