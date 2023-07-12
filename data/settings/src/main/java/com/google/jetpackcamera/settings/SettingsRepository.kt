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

import com.google.jetpackcamera.settings.model.DarkModeStatus
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashModeStatus
import kotlinx.coroutines.flow.Flow

/**
 * Data layer for settings.
 */
interface SettingsRepository {

    val cameraAppSettings : Flow<CameraAppSettings>

    suspend fun updateDefaultToFrontCamera()

    suspend fun updateDarkModeStatus(darkmodeStatus: DarkModeStatus)

    suspend fun updateFlashModeStatus(flashModeStatus: FlashModeStatus)

    suspend fun getCameraAppSettings(): CameraAppSettings

// set device values from cameraUseCase
    suspend fun updateAvailableCameraLens(frontLensAvailable: Boolean, backLensAvailable: Boolean)

}