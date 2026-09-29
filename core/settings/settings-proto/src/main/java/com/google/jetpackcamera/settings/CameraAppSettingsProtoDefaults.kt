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

import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.model.UNLIMITED_VIDEO_DURATION
import com.google.jetpackcamera.model.proto.AspectRatio
import com.google.jetpackcamera.model.proto.ConcurrentCameraMode
import com.google.jetpackcamera.model.proto.DarkMode
import com.google.jetpackcamera.model.proto.DynamicRange
import com.google.jetpackcamera.model.proto.FlashMode
import com.google.jetpackcamera.model.proto.ImageOutputFormat
import com.google.jetpackcamera.model.proto.LensFacing
import com.google.jetpackcamera.model.proto.LowLightBoostPriority
import com.google.jetpackcamera.model.proto.StabilizationMode
import com.google.jetpackcamera.model.proto.VideoQuality
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto
import com.google.jetpackcamera.settings.proto.cameraAppSettings

/**
 * The default [CameraAppSettingsProto] instance.
 *
 * Every enum field is explicitly populated because a proto3 message that is absent from storage
 * deserializes to its zero-value (`<ENUM_NAME>_UNSPECIFIED`), which does not necessarily map to the
 * corresponding default in
 * [com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS]. Callers that persist this
 * message must seed their storage with this instance so that a first read produces the documented
 * application defaults rather than the proto3 zero-values.
 */
val DEFAULT_CAMERA_APP_SETTINGS_PROTO: CameraAppSettingsProto = cameraAppSettings {
    darkMode = DarkMode.DARK_MODE_DARK
    defaultLensFacing = LensFacing.LENS_FACING_BACK
    flashMode = FlashMode.FLASH_MODE_OFF
    aspectRatio = AspectRatio.ASPECT_RATIO_NINE_SIXTEEN
    stabilizationMode = StabilizationMode.STABILIZATION_MODE_AUTO
    dynamicRange = DynamicRange.DYNAMIC_RANGE_UNSPECIFIED
    imageFormat = ImageOutputFormat.IMAGE_OUTPUT_FORMAT_JPEG
    maxVideoDurationMillis = UNLIMITED_VIDEO_DURATION
    videoQuality = VideoQuality.VIDEO_QUALITY_UNSPECIFIED
    audioEnabled = true
    concurrentCameraMode = ConcurrentCameraMode.CONCURRENT_CAMERA_MODE_OFF
    targetFrameRate = TARGET_FPS_AUTO
    lowLightBoostPriority = LowLightBoostPriority.LOW_LIGHT_BOOST_PRIORITY_UNSPECIFIED
    locationEnabled = false
}
