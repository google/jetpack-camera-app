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

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
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
import com.google.jetpackcamera.model.proto.toProto
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Settings data source using Proto DataStore.
 */
class ProtoDataStoreSettingsDataSource(
    private val jcaSettings: DataStore<CameraAppSettingsProto>
) : SettingsDataSource {

    private val jcaSettingsFlow: Flow<CameraAppSettingsProto> =
        jcaSettings.data.catch { exception ->
            if (exception is java.io.IOException) {
                emit(CameraAppSettingsProto.getDefaultInstance())
            } else {
                throw exception
            }
        }

    override val defaultCameraAppSettings: Flow<CameraAppSettings> = jcaSettingsFlow.map {
        it.toDomain()
    }

    override suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings =
        jcaSettingsFlow.first().toDomain()

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDefaultLensFacing(lensFacing.toProto())
                .build()
        }
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDarkModeStatus(darkMode.toProto())
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

    override suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setLowLightBoostPriority(lowLightBoostPriority.toProto())
                .build()
        }
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setStabilizationMode(stabilizationMode.toProto())
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

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setTargetFrameRate(targetFrameRate)
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

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setAudioEnabledStatus(isAudioEnabled)
                .build()
        }
    }

    override suspend fun updateConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setConcurrentCameraModeStatus(concurrentCameraMode.toProto())
                .build()
        }
    }

    companion object {
        private const val FILE_LOCATION = "CameraAppSettings.pb"

        /**
         * Creates an instance of [SettingsDataSource] backed by Proto DataStore.
         *
         * Note: To avoid breaking DataStore functionality, ensure that only a single instance
         * of [DataStore] is active for the settings file at any time (e.g., by managing this
         * instance as a Singleton via dependency injection).
         *
         * @param context The application context.
         * @return A [SettingsDataSource] instance.
         */
        fun create(context: Context): SettingsDataSource {
            val dataStore = DataStoreFactory.create(
                serializer = ProtoCameraAppSettingsSerializer,
                produceFile = { File(context.filesDir, "datastore/$FILE_LOCATION") }
            )
            return ProtoDataStoreSettingsDataSource(dataStore)
        }
    }
}
