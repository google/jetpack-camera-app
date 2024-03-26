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
package com.google.jetpackcamera.settings.model
const val TARGET_FPS_AUTO = 0

/**
 * Data layer representation for settings.
 */
data class CameraAppSettings(
    val cameraLensFacing: LensFacing = LensFacing.BACK,
    val isFrontCameraAvailable: Boolean = true,
    val isBackCameraAvailable: Boolean = true,
    val darkMode: DarkMode = DarkMode.SYSTEM,
    val flashMode: FlashMode = FlashMode.OFF,
    val captureMode: CaptureMode = CaptureMode.MULTI_STREAM,
    val aspectRatio: AspectRatio = AspectRatio.NINE_SIXTEEN,
    val previewStabilization: Stabilization = Stabilization.UNDEFINED,
    val videoCaptureStabilization: Stabilization = Stabilization.UNDEFINED,
    val supportedStabilizationModes: List<SupportedStabilizationMode> = emptyList(),
    val dynamicRange: DynamicRange = DynamicRange.SDR,
    val supportedDynamicRanges: List<DynamicRange> = listOf(DynamicRange.SDR),
    val zoomScale: Float = 1f,
    val targetFrameRate: Int = TARGET_FPS_AUTO,
    val supportedFixedFrameRates: List<Int> = emptyList()
)

val DEFAULT_CAMERA_APP_SETTINGS = CameraAppSettings()
