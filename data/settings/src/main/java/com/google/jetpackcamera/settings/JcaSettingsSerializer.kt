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
import com.google.protobuf.InvalidProtocolBufferException
import java.io.InputStream
import java.io.OutputStream
/**
 * This constant is `0L` because the `DURATION_UNLIMITED`
 * constant in the `OutputOptions` API [documentation](https://developer.android.com/reference/androidx/camera/video/OutputOptions#DURATION_UNLIMITED()) is `0`.
 */
const val UNLIMITED_VIDEO_DURATION = 0L
object JcaSettingsSerializer : Serializer<JcaSettings> {

    override val defaultValue: JcaSettings = JcaSettings.newBuilder()
        .setDarkModeStatus(DarkMode.DARK_MODE_SYSTEM)
        .setDefaultLensFacing(LensFacing.LENS_FACING_BACK)
        .setFlashModeStatus(FlashMode.FLASH_MODE_OFF)
        .setAspectRatioStatus(AspectRatio.ASPECT_RATIO_NINE_SIXTEEN)
        .setStreamConfigStatus(StreamConfig.STREAM_CONFIG_MULTI_STREAM)
        .setStabilizationMode(StabilizationMode.STABILIZATION_MODE_AUTO)
        .setDynamicRangeStatus(DynamicRange.DYNAMIC_RANGE_UNSPECIFIED)
        .setImageFormatStatus(ImageOutputFormat.IMAGE_OUTPUT_FORMAT_JPEG)
        .setMaxVideoDurationMillis(UNLIMITED_VIDEO_DURATION)
        .setVideoQuality(VideoQuality.VIDEO_QUALITY_UNSPECIFIED)
        .setAudioEnabledStatus(true)
        .build()

    override suspend fun readFrom(input: InputStream): JcaSettings {
        try {
            return JcaSettings.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Cannot read proto.", exception)
        }
    }

    override suspend fun writeTo(t: JcaSettings, output: OutputStream) = t.writeTo(output)
}
