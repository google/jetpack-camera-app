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
import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.proto.toProto
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto
import com.google.jetpackcamera.settings.proto.copy
import java.io.File
import java.io.IOException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Settings data source using Proto DataStore.
 */
class ProtoDataStoreSettingsDataSource(
    private val jcaSettings: DataStore<CameraAppSettingsProto>,
    private val defaultCaptureModeOverride: CaptureMode
) : SettingsDataSource {

    private val jcaSettingsFlow: Flow<CameraAppSettingsProto> =
        jcaSettings.data.catch { exception ->
            if (exception is IOException) {
                // Fall back to the serializer's default value rather than the proto3 zero-value
                // instance, which would otherwise resolve to unintended defaults (e.g. audio
                // disabled, 3:4 aspect ratio, and system dark mode).
                emit(ProtoCameraAppSettingsSerializer.defaultValue)
            } else {
                throw exception
            }
        }

    override val defaultCameraAppSettings: Flow<CameraAppSettings> = jcaSettingsFlow.map {
        it.toModel(defaultCaptureModeOverride)
    }

    override suspend fun getCurrentDefaultCameraAppSettings(): CameraAppSettings =
        jcaSettingsFlow.first().toModel(defaultCaptureModeOverride)

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.defaultLensFacing = lensFacing.toProto() }
        }
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.darkMode = darkMode.toProto() }
        }
    }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.flashMode = flashMode.toProto() }
        }
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.aspectRatio = aspectRatio.toProto() }
        }
    }

    override suspend fun updateSelectedCameraEffect(selectedCameraEffect: CameraEffectId) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.selectedCameraEffect = selectedCameraEffect.value }
        }
    }

    override suspend fun updateLowLightBoostPriority(lowLightBoostPriority: LowLightBoostPriority) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.lowLightBoostPriority = lowLightBoostPriority.toProto() }
        }
    }

    override suspend fun updateStabilizationMode(stabilizationMode: StabilizationMode) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.stabilizationMode = stabilizationMode.toProto() }
        }
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.dynamicRange = dynamicRange.toProto() }
        }
    }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.targetFrameRate = targetFrameRate }
        }
    }

    override suspend fun updateImageFormat(imageFormat: ImageOutputFormat) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.imageFormat = imageFormat.toProto() }
        }
    }

    override suspend fun updateMaxVideoDuration(durationMillis: Long) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.maxVideoDurationMillis = durationMillis }
        }
    }

    override suspend fun updateVideoQuality(videoQuality: VideoQuality) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.videoQuality = videoQuality.toProto() }
        }
    }

    override suspend fun updateAudioEnabled(isAudioEnabled: Boolean) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.audioEnabled = isAudioEnabled }
        }
    }

    override suspend fun updateConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.concurrentCameraMode = concurrentCameraMode.toProto() }
        }
    }

    override suspend fun updateLocationEnabled(locationEnabled: Boolean) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.copy { this.locationEnabled = locationEnabled }
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
         * @param defaultCaptureModeOverride The [CaptureMode] reported by every [CameraAppSettings]
         * emitted by the returned data source.
         * @param coroutineContext An optional [CoroutineContext] for the DataStore's background
         * work. The work runs on [Dispatchers.IO] unless this context contains a
         * [kotlinx.coroutines.CoroutineDispatcher]. If this context contains a [Job], the
         * DataStore stops when that [Job] is cancelled; otherwise, it remains active for the
         * lifetime of the process.
         * @return A [SettingsDataSource] instance.
         */
        fun create(
            context: Context,
            defaultCaptureModeOverride: CaptureMode,
            coroutineContext: CoroutineContext = EmptyCoroutineContext
        ): SettingsDataSource {
            val scope = CoroutineScope(
                Dispatchers.IO + coroutineContext + SupervisorJob(coroutineContext[Job])
            )
            val dataStore = DataStoreFactory.create(
                serializer = ProtoCameraAppSettingsSerializer,
                scope = scope,
                produceFile = { File(context.filesDir, "datastore/$FILE_LOCATION") }
            )
            return ProtoDataStoreSettingsDataSource(dataStore, defaultCaptureModeOverride)
        }
    }
}
