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
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality

/**
 * Camera settings that persist as long as a camera is running.
 *
 * Any change in these settings will require calling [ProcessCameraProvider.runWith] with
 * updates [CameraSelector] and/or [UseCaseGroup]
 */
internal sealed interface PerpetualSessionSettings {
    val aspectRatio: AspectRatio
    val captureMode: CaptureMode

    data class SingleCamera(
        override val aspectRatio: AspectRatio,
        override val captureMode: CaptureMode,
        val streamConfig: StreamConfig,
        val targetFrameRate: Int,
        val stabilizationMode: StabilizationMode,
        val dynamicRange: DynamicRange,
        val videoQuality: VideoQuality,
        val imageFormat: ImageOutputFormat
    ) : PerpetualSessionSettings

    /**
     * @property captureMode is always [com.google.jetpackcamera.model.CaptureMode.VIDEO_ONLY] in Concurrent Camera mode.
     * Concurrent Camera currently only supports video capture
     */
    data class ConcurrentCamera(
        val primaryCameraInfo: CameraInfo,
        val secondaryCameraInfo: CameraInfo,
        override val aspectRatio: AspectRatio
    ) : PerpetualSessionSettings {
        override val captureMode: CaptureMode = CaptureMode.VIDEO_ONLY
    }
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

data class InitialRecordingSettings(
    val isAudioEnabled: Boolean,
    val lensFacing: LensFacing,
    val zoomRatios: Map<LensFacing, Float>
)
