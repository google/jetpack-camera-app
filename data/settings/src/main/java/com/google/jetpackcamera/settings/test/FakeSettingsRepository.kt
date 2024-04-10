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
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SupportedStabilizationMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

object FakeSettingsRepository : SettingsRepository {
    private var currentCameraSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
    private var isPreviewStabilizationSupported: Boolean = false
    private var isVideoStabilizationSupported: Boolean = false

    override val cameraAppSettings: Flow<CameraAppSettings> = flow { emit(currentCameraSettings) }

    override suspend fun updateDefaultLensFacing(lensFacing: LensFacing) {
        currentCameraSettings = currentCameraSettings.copy(cameraLensFacing = lensFacing)
    }

    override suspend fun updateDarkModeStatus(darkMode: DarkMode) {
        currentCameraSettings = currentCameraSettings.copy(darkMode = darkMode)
    }

    override suspend fun updateFlashModeStatus(flashMode: FlashMode) {
        currentCameraSettings = currentCameraSettings.copy(flashMode = flashMode)
    }

    override suspend fun getCameraAppSettings(): CameraAppSettings {
        return currentCameraSettings
    }

    override suspend fun updateAvailableCameraLens(
        frontLensAvailable: Boolean,
        backLensAvailable: Boolean
    ) {
        currentCameraSettings = currentCameraSettings.copy(
            isFrontCameraAvailable = frontLensAvailable,
            isBackCameraAvailable = backLensAvailable
        )
    }

    override suspend fun updateCaptureMode(captureMode: CaptureMode) {
        currentCameraSettings =
            currentCameraSettings.copy(captureMode = captureMode)
    }

    override suspend fun updatePreviewStabilization(stabilization: Stabilization) {
        currentCameraSettings =
            currentCameraSettings.copy(previewStabilization = stabilization)
    }

    override suspend fun updateVideoStabilization(stabilization: Stabilization) {
        currentCameraSettings =
            currentCameraSettings.copy(videoCaptureStabilization = stabilization)
    }

    override suspend fun updateVideoStabilizationSupported(isSupported: Boolean) {
        isVideoStabilizationSupported = isSupported
        setSupportedStabilizationMode()
    }

    override suspend fun updatePreviewStabilizationSupported(isSupported: Boolean) {
        isPreviewStabilizationSupported = isSupported
        setSupportedStabilizationMode()
    }

    private fun setSupportedStabilizationMode() {
        val stabilizationModes =
            buildList {
                if (isPreviewStabilizationSupported) {
                    add(SupportedStabilizationMode.ON)
                }
                if (isVideoStabilizationSupported) {
                    add(SupportedStabilizationMode.HIGH_QUALITY)
                }
            }

        currentCameraSettings =
            currentCameraSettings.copy(supportedStabilizationModes = stabilizationModes)
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        currentCameraSettings =
            currentCameraSettings.copy(dynamicRange = dynamicRange)
    }

    override suspend fun updateSupportedDynamicRanges(supportedDynamicRanges: List<DynamicRange>) {
        currentCameraSettings =
            currentCameraSettings.copy(supportedDynamicRanges = supportedDynamicRanges)
    }

    override suspend fun updateAspectRatio(aspectRatio: AspectRatio) {
        currentCameraSettings =
            currentCameraSettings.copy(aspectRatio = aspectRatio)
    }

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        currentCameraSettings =
            currentCameraSettings.copy(targetFrameRate = targetFrameRate)
    }

    override suspend fun updateSupportedFixedFrameRate(
        supportedFrameRates: Set<Int>,
        currentTargetFrameRate: Int
    ) {
        currentCameraSettings =
            currentCameraSettings.copy(supportedFixedFrameRates = supportedFrameRates.toList())
    }
}
