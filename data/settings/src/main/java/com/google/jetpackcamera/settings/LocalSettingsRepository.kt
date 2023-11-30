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
import com.google.jetpackcamera.settings.CaptureMode as CaptureModeProto
import com.google.jetpackcamera.settings.DarkMode as DarkModeProto
import com.google.jetpackcamera.settings.FlashMode as FlashModeProto
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Implementation of [SettingsRepository] with locally stored settings.
 */
class LocalSettingsRepository @Inject constructor(
    private val jcaSettings: DataStore<JcaSettings>
) : SettingsRepository {

    override val cameraAppSettings = jcaSettings.data
        .map {
            CameraAppSettings(
                isFrontCameraFacing = it.defaultFrontCamera,
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
                    else -> FlashMode.OFF
                },
                isFrontCameraAvailable = it.frontCameraAvailable,
                isBackCameraAvailable = it.backCameraAvailable,
                aspectRatio = AspectRatio.fromProto(it.aspectRatioStatus),
                captureMode = when (it.captureModeStatus) {
                    CaptureModeProto.CAPTURE_MODE_SINGLE_STREAM -> CaptureMode.SINGLE_STREAM
                    CaptureModeProto.CAPTURE_MODE_MULTI_STREAM -> CaptureMode.MULTI_STREAM
                    else -> CaptureMode.MULTI_STREAM
                }
            )
        }

    override suspend fun getCameraAppSettings(): CameraAppSettings = cameraAppSettings.first()

    override suspend fun updateDefaultToFrontCamera() {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDefaultFrontCamera(!currentSettings.defaultFrontCamera)
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
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setFlashModeStatus(newStatus)
                .build()
        }
    }

    override suspend fun updateAvailableCameraLens(
        frontLensAvailable: Boolean,
        backLensAvailable: Boolean
    ) {
        // if a front or back lens is not present, the option to change
        // the direction of the camera should be disabled
        jcaSettings.updateData { currentSettings ->
            val newLensFacing = if (currentSettings.defaultFrontCamera) {
                frontLensAvailable
            } else {
                false
            }
            currentSettings.toBuilder()
                .setDefaultFrontCamera(newLensFacing)
                .setFrontCameraAvailable(frontLensAvailable)
                .setBackCameraAvailable(backLensAvailable)
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

    override suspend fun updateCaptureMode(captureMode: CaptureMode) {
        val newStatus = when (captureMode) {
            CaptureMode.MULTI_STREAM -> CaptureModeProto.CAPTURE_MODE_MULTI_STREAM
            CaptureMode.SINGLE_STREAM -> CaptureModeProto.CAPTURE_MODE_SINGLE_STREAM
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setCaptureModeStatus(newStatus)
                .build()
        }
    }
}
