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
import com.google.jetpackcamera.model.proto.AspectRatio as AspectRatioProto
import com.google.jetpackcamera.model.proto.DynamicRange as DynamicRangeProto
import com.google.jetpackcamera.model.proto.FlashMode as FlashModeProto
import com.google.jetpackcamera.model.proto.ImageOutputFormat as ImageOutputFormatProto
import com.google.jetpackcamera.model.proto.toModel
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto

/**
 * Maps a [CameraAppSettingsProto] to a [CameraAppSettings] domain model.
 *
 * Fields that can be configured through
 * [com.google.jetpackcamera.settings.model.CameraFeaturePolicy] (flash mode, aspect ratio, dynamic
 * range, and image format) are resolved from [baselineSettings] when the stored value is
 * unspecified, so that stored user preferences take precedence over baseline defaults.
 *
 * @param defaultCaptureModeOverride The [CaptureMode] reported by the returned settings.
 * @param baselineSettings The settings used for policy-configurable fields that have no stored
 * value. Defaults to [DEFAULT_CAMERA_APP_SETTINGS].
 */
fun CameraAppSettingsProto.toModel(
    defaultCaptureModeOverride: CaptureMode,
    baselineSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
): CameraAppSettings {
    return CameraAppSettings(
        captureMode = defaultCaptureModeOverride,
        selectedCameraEffect = if (this.selectedCameraEffect.isEmpty()) {
            NONE_EFFECT_ID
        } else {
            CameraEffectId(this.selectedCameraEffect)
        },

        cameraLensFacing = this.defaultLensFacing.toModel(),
        flashMode = when (this.flashMode) {
            FlashModeProto.FLASH_MODE_UNSPECIFIED,
            FlashModeProto.UNRECOGNIZED -> baselineSettings.flashMode
            else -> this.flashMode.toModel()
        },
        targetFrameRate = this.targetFrameRate,
        aspectRatio = when (this.aspectRatio) {
            AspectRatioProto.ASPECT_RATIO_UNSPECIFIED,
            AspectRatioProto.UNRECOGNIZED -> baselineSettings.aspectRatio
            else -> this.aspectRatio.toModel()
        },
        stabilizationMode = this.stabilizationMode.toModel(),
        dynamicRange = when (this.dynamicRange) {
            DynamicRangeProto.DYNAMIC_RANGE_UNSPECIFIED,
            DynamicRangeProto.UNRECOGNIZED -> baselineSettings.dynamicRange
            else -> this.dynamicRange.toModel()
        },
        imageFormat = when (this.imageFormat) {
            ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_UNSPECIFIED,
            ImageOutputFormatProto.UNRECOGNIZED -> baselineSettings.imageFormat
            else -> this.imageFormat.toModel()
        },
        maxVideoDurationMillis = this.maxVideoDurationMillis,
        videoQuality = this.videoQuality.toModel(),
        audioEnabled = this.audioEnabled,
        lowLightBoostPriority = this.lowLightBoostPriority.toModel(),
        darkMode = this.darkMode.toModel(),
        concurrentCameraMode = this.concurrentCameraMode.toModel(),
        locationEnabled = this.locationEnabled
    )
}
