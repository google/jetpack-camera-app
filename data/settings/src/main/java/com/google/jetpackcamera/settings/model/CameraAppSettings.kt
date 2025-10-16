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

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality

const val TARGET_FPS_AUTO = 0
const val UNLIMITED_VIDEO_DURATION = 0L

/**
 * Data layer representation for settings.
 */
data class CameraAppSettings(
    val captureMode: CaptureMode = CaptureMode.STANDARD,
    val cameraLensFacing: LensFacing = LensFacing.BACK,
    val darkMode: DarkMode = DarkMode.DARK,
    val flashMode: FlashMode = FlashMode.OFF,
    val streamConfig: StreamConfig = StreamConfig.MULTI_STREAM,
    val aspectRatio: AspectRatio = AspectRatio.NINE_SIXTEEN,
    val stabilizationMode: StabilizationMode = StabilizationMode.AUTO,
    val dynamicRange: DynamicRange = DynamicRange.SDR,
    val videoQuality: VideoQuality = VideoQuality.UNSPECIFIED,
    val defaultZoomRatios: Map<LensFacing, Float> = mapOf(),
    val targetFrameRate: Int = TARGET_FPS_AUTO,
    val imageFormat: ImageOutputFormat = ImageOutputFormat.JPEG,
    val audioEnabled: Boolean = true,
    val deviceRotation: DeviceRotation = DeviceRotation.Natural,
    val concurrentCameraMode: ConcurrentCameraMode = ConcurrentCameraMode.OFF,
    val maxVideoDurationMillis: Long = UNLIMITED_VIDEO_DURATION,
    val debugSettings: DebugSettings = DebugSettings()
)

fun CameraSystemConstraints.forCurrentLens(
    cameraAppSettings: CameraAppSettings
): CameraConstraints? = perLensConstraints[cameraAppSettings.cameraLensFacing]

val DEFAULT_CAMERA_APP_SETTINGS = CameraAppSettings()
