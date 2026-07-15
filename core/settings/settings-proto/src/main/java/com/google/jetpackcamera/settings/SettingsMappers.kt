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

import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.model.proto.toModel
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto

/**
 * Maps a [CameraAppSettingsProto] to a [CameraAppSettings] domain model.
 */
fun CameraAppSettingsProto.toModel(defaultCaptureModeOverride: CaptureMode): CameraAppSettings {
    return CameraAppSettings(
        captureMode = defaultCaptureModeOverride,
        selectedCameraEffect = if (this.selectedCameraEffect.isEmpty()) {
            NONE_EFFECT_ID
        } else {
            CameraEffectId(this.selectedCameraEffect)
        },

        cameraLensFacing = this.defaultLensFacing.toModel(),
        flashMode = this.flashModeStatus.toModel(),
        targetFrameRate = this.targetFrameRate,
        aspectRatio = this.aspectRatioStatus.toModel(),
        stabilizationMode = this.stabilizationMode.toModel(),
        dynamicRange = this.dynamicRangeStatus.toModel(),
        imageFormat = this.imageFormatStatus.toModel(),
        maxVideoDurationMillis = this.maxVideoDurationMillis,
        videoQuality = this.videoQuality.toModel(),
        audioEnabled = this.audioEnabledStatus,
        lowLightBoostPriority = this.lowLightBoostPriority.toModel(),
        darkMode = this.darkModeStatus.toModel(),
        concurrentCameraMode = this.concurrentCameraModeStatus.toModel()
    )
}
