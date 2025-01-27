/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.core.camera

import androidx.camera.core.CameraInfo
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.DeviceRotation
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.settings.model.VideoQuality

/**
 * Camera settings that persist as long as a camera is running.
 *
 * Any change in these settings will require calling [ProcessCameraProvider.runWith] with
 * updates [CameraSelector] and/or [UseCaseGroup]
 */
internal sealed interface PerpetualSessionSettings {
    val aspectRatio: AspectRatio

    data class SingleCamera(
        override val aspectRatio: AspectRatio,
        val streamConfig: StreamConfig,
        val targetFrameRate: Int,
        val stabilizationMode: StabilizationMode,
        val dynamicRange: DynamicRange,
        val videoQuality: VideoQuality,
        val imageFormat: ImageOutputFormat
    ) : PerpetualSessionSettings

    data class ConcurrentCamera(
        val primaryCameraInfo: CameraInfo,
        val secondaryCameraInfo: CameraInfo,
        override val aspectRatio: AspectRatio
    ) : PerpetualSessionSettings
}

/**
 * Camera settings that can change while the camera is running.
 *
 * Any changes in these settings can be applied either directly to use cases via their
 * setter methods or to [androidx.camera.core.CameraControl].
 * The use cases typically will not need to be re-bound.
 */
internal data class TransientSessionSettings(
    val isAudioEnabled: Boolean,
    val deviceRotation: DeviceRotation,
    val flashMode: FlashMode,
    val primaryLensFacing: LensFacing,
    val zoomRatios: Map<LensFacing, Float>
)
