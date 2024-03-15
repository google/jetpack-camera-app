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
import com.google.jetpackcamera.settings.PreviewStabilization as PreviewStabilizationProto
import com.google.jetpackcamera.settings.VideoStabilization as VideoStabilizationProto
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.DynamicRange.Companion.toProto
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LensFacing.Companion.toProto
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SupportedStabilizationMode
import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

const val TARGET_FPS_NONE = 0
const val TARGET_FPS_15 = 15
const val TARGET_FPS_30 = 30
const val TARGET_FPS_60 = 60

/**
 * Implementation of [SettingsRepository] with locally stored settings.
 */
class LocalSettingsRepository @Inject constructor(
    private val jcaSettings: DataStore<JcaSettings>
) : SettingsRepository {

    override val cameraAppSettings = jcaSettings.data
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
                    else -> FlashMode.OFF
                },
                isFrontCameraAvailable = it.frontCameraAvailable,
                isBackCameraAvailable = it.backCameraAvailable,
                aspectRatio = AspectRatio.fromProto(it.aspectRatioStatus),
                previewStabilization = Stabilization.fromProto(it.stabilizePreview),
                videoCaptureStabilization = Stabilization.fromProto(it.stabilizeVideo),
                supportedStabilizationModes = getSupportedStabilization(
                    previewSupport = it.stabilizePreviewSupported,
                    videoSupport = it.stabilizeVideoSupported
                ),
                targetFrameRate = it.targetFrameRate,
                captureMode = when (it.captureModeStatus) {
                    CaptureModeProto.CAPTURE_MODE_SINGLE_STREAM -> CaptureMode.SINGLE_STREAM
                    CaptureModeProto.CAPTURE_MODE_MULTI_STREAM -> CaptureMode.MULTI_STREAM
                    else -> CaptureMode.MULTI_STREAM
                },
                dynamicRange = DynamicRange.fromProto(it.dynamicRangeStatus),
                supportedDynamicRanges = it.supportedDynamicRangesList.map { dynRngProto ->
                    DynamicRange.fromProto(dynRngProto)
                },
                supportedFixedFrameRates = it.supportedFrameRatesList
            )
        }

    override suspend fun getCameraAppSettings(): CameraAppSettings = cameraAppSettings.first()

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

    override suspend fun updateTargetFrameRate(targetFrameRate: Int) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setTargetFrameRate(targetFrameRate)
                .build()
        }
    }

    override suspend fun updateSupportedFixedFrameRate(
        supportedFrameRates: Set<Int>,
        currentTargetFrameRate: Int
    ) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .clearSupportedFrameRates()
                .addAllSupportedFrameRates(supportedFrameRates)
                .build()
        }
        when (currentTargetFrameRate) {
            TARGET_FPS_NONE -> {}
            TARGET_FPS_15 -> {
                if (!supportedFrameRates.contains(TARGET_FPS_15)) {
                    updateTargetFrameRate(TARGET_FPS_NONE)
                }
            }

            TARGET_FPS_30 -> {
                if (!supportedFrameRates.contains(30)) {
                    updateTargetFrameRate(TARGET_FPS_NONE)
                }
            }

            TARGET_FPS_60 -> {
                if (!supportedFrameRates.contains(60)) {
                    updateTargetFrameRate(TARGET_FPS_NONE)
                }
            }
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

    override suspend fun updatePreviewStabilization(stabilization: Stabilization) {
        val newStatus = when (stabilization) {
            Stabilization.ON -> PreviewStabilizationProto.PREVIEW_STABILIZATION_ON
            Stabilization.OFF -> PreviewStabilizationProto.PREVIEW_STABILIZATION_OFF
            else -> PreviewStabilizationProto.PREVIEW_STABILIZATION_UNDEFINED
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setStabilizePreview(newStatus)
                .build()
        }
    }

    override suspend fun updateVideoStabilization(stabilization: Stabilization) {
        val newStatus = when (stabilization) {
            Stabilization.ON -> VideoStabilizationProto.VIDEO_STABILIZATION_ON
            Stabilization.OFF -> VideoStabilizationProto.VIDEO_STABILIZATION_OFF
            else -> VideoStabilizationProto.VIDEO_STABILIZATION_UNDEFINED
        }
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setStabilizeVideo(newStatus)
                .build()
        }
    }

    override suspend fun updateVideoStabilizationSupported(isSupported: Boolean) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setStabilizeVideoSupported(isSupported)
                .build()
        }
    }

    override suspend fun updatePreviewStabilizationSupported(isSupported: Boolean) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setStabilizePreviewSupported(isSupported)
                .build()
        }
    }

    private fun getSupportedStabilization(
        previewSupport: Boolean,
        videoSupport: Boolean
    ): List<SupportedStabilizationMode> {
        return buildList {
            if (previewSupport) {
                add(SupportedStabilizationMode.ON)
            }
            if (videoSupport) {
                add(SupportedStabilizationMode.HIGH_QUALITY)
            }
        }
    }

    override suspend fun updateDynamicRange(dynamicRange: DynamicRange) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setDynamicRangeStatus(dynamicRange.toProto())
                .build()
        }
    }

    override suspend fun updateSupportedDynamicRanges(supportedDynamicRanges: List<DynamicRange>) {
        jcaSettings.updateData { currentSettings ->
            currentSettings.toBuilder()
                .clearSupportedDynamicRanges()
                .addAllSupportedDynamicRanges(
                    supportedDynamicRanges.map {
                        it.toProto()
                    }
                )
                .build()
        }
    }
}
