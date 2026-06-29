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

import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto
import com.google.jetpackcamera.model.proto.toDomain

fun CameraAppSettingsProto.toDomain(): CameraAppSettings {
    return CameraAppSettings(
        cameraLensFacing = this.defaultLensFacing.toDomain(),
        flashMode = this.flashModeStatus.toDomain(),
        targetFrameRate = this.targetFrameRate,
        aspectRatio = this.aspectRatioStatus.toDomain(),
        streamConfig = this.streamConfigStatus.toDomain(),
        stabilizationMode = this.stabilizationMode.toDomain(),
        dynamicRange = this.dynamicRangeStatus.toDomain(),
        imageFormat = this.imageFormatStatus.toDomain(),
        maxVideoDurationMillis = this.maxVideoDurationMillis,
        videoQuality = this.videoQuality.toDomain(),
        audioEnabled = this.audioEnabledStatus,
        lowLightBoostPriority = this.lowLightBoostPriority.toDomain(),
        darkMode = this.darkModeStatus.toDomain()
    )
}
