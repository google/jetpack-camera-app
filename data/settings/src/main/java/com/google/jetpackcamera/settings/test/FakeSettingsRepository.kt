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

import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DarkModeStatus
import com.google.jetpackcamera.settings.model.FlashModeStatus
import com.google.jetpackcamera.settings.model.getDefaultSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object FakeSettingsRepository : SettingsRepository {
    var currentCameraSettings: CameraAppSettings = getDefaultSettings()

    override val cameraAppSettings: Flow<CameraAppSettings> = flow { emit(currentCameraSettings) }

    override suspend fun updateDefaultToFrontCamera() {
        val newLensFacing = !currentCameraSettings.default_front_camera
        currentCameraSettings = currentCameraSettings.copy(default_front_camera = newLensFacing)
    }

    override suspend fun updateDarkModeStatus(darkmodeStatus: DarkModeStatus) {
        currentCameraSettings = currentCameraSettings.copy(dark_mode_status = darkmodeStatus)
    }

    override suspend fun updateFlashModeStatus(flashModeStatus: FlashModeStatus) {
        currentCameraSettings = currentCameraSettings.copy(flash_mode_status = flashModeStatus)
    }

    override suspend fun getSettings(): CameraAppSettings {
        return currentCameraSettings
    }
}