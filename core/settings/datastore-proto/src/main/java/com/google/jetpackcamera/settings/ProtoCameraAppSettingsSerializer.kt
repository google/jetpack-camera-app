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

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.model.UNLIMITED_VIDEO_DURATION
import com.google.jetpackcamera.model.proto.AspectRatio
import com.google.jetpackcamera.model.proto.ConcurrentCameraMode
import com.google.jetpackcamera.model.proto.DarkMode
import com.google.jetpackcamera.model.proto.DynamicRange
import com.google.jetpackcamera.model.proto.FlashMode
import com.google.jetpackcamera.model.proto.ImageOutputFormat
import com.google.jetpackcamera.model.proto.LensFacing
import com.google.jetpackcamera.model.proto.LowLightBoostPriority as LowLightBoostPriorityProto
import com.google.jetpackcamera.model.proto.StabilizationMode
import com.google.jetpackcamera.model.proto.VideoQuality
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream

/**
 * Serializer for the [CameraAppSettingsProto] DataStore.
 */
internal object ProtoCameraAppSettingsSerializer : Serializer<CameraAppSettingsProto> {

    override val defaultValue: CameraAppSettingsProto = CameraAppSettingsProto.newBuilder()
        .setDarkModeStatus(DarkMode.DARK_MODE_DARK)
        .setDefaultLensFacing(LensFacing.LENS_FACING_BACK)
        .setFlashModeStatus(FlashMode.FLASH_MODE_OFF)
        .setAspectRatioStatus(AspectRatio.ASPECT_RATIO_NINE_SIXTEEN)
        .setStabilizationMode(StabilizationMode.STABILIZATION_MODE_AUTO)
        .setDynamicRangeStatus(DynamicRange.DYNAMIC_RANGE_UNSPECIFIED)
        .setImageFormatStatus(ImageOutputFormat.IMAGE_OUTPUT_FORMAT_JPEG)
        .setMaxVideoDurationMillis(UNLIMITED_VIDEO_DURATION)
        .setVideoQuality(VideoQuality.VIDEO_QUALITY_UNSPECIFIED)
        .setAudioEnabledStatus(true)
        .setConcurrentCameraModeStatus(ConcurrentCameraMode.CONCURRENT_CAMERA_MODE_OFF)
        .setTargetFrameRate(TARGET_FPS_AUTO)
        .setLowLightBoostPriority(
            LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_UNSPECIFIED
        )
        .build()

    override suspend fun readFrom(input: InputStream): CameraAppSettingsProto {
        try {
            return CameraAppSettingsProto.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: CameraAppSettingsProto, output: OutputStream) =
        t.writeTo(output)
}
