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
package com.google.jetpackcamera.model.proto

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.proto.AspectRatio as AspectRatioProto
import com.google.jetpackcamera.model.proto.DarkMode as DarkModeProto
import com.google.jetpackcamera.model.proto.DynamicRange as DynamicRangeProto
import com.google.jetpackcamera.model.proto.FlashMode as FlashModeProto
import com.google.jetpackcamera.model.proto.ImageOutputFormat as ImageOutputFormatProto
import com.google.jetpackcamera.model.proto.LensFacing as LensFacingProto
import com.google.jetpackcamera.model.proto.LowLightBoostPriority as LowLightBoostPriorityProto
import com.google.jetpackcamera.model.proto.StabilizationMode as StabilizationModeProto
import com.google.jetpackcamera.model.proto.StreamConfig as StreamConfigProto
import com.google.jetpackcamera.model.proto.VideoQuality as VideoQualityProto

fun AspectRatioProto.toDomain(): AspectRatio {
    return when (this) {
        AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN -> AspectRatio.NINE_SIXTEEN
        AspectRatioProto.ASPECT_RATIO_ONE_ONE -> AspectRatio.ONE_ONE
        AspectRatioProto.ASPECT_RATIO_THREE_FOUR,
        AspectRatioProto.ASPECT_RATIO_UNDEFINED,
        AspectRatioProto.UNRECOGNIZED -> AspectRatio.THREE_FOUR
    }
}

fun AspectRatio.toProto(): AspectRatioProto {
    return when (this) {
        AspectRatio.NINE_SIXTEEN -> AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN
        AspectRatio.ONE_ONE -> AspectRatioProto.ASPECT_RATIO_ONE_ONE
        AspectRatio.THREE_FOUR -> AspectRatioProto.ASPECT_RATIO_THREE_FOUR
    }
}

fun DarkModeProto.toDomain(): DarkMode {
    return when (this) {
        DarkModeProto.DARK_MODE_DARK -> DarkMode.DARK
        DarkModeProto.DARK_MODE_LIGHT -> DarkMode.LIGHT
        DarkModeProto.DARK_MODE_SYSTEM,
        DarkModeProto.UNRECOGNIZED -> DarkMode.SYSTEM
    }
}

fun DarkMode.toProto(): DarkModeProto {
    return when (this) {
        DarkMode.DARK -> DarkModeProto.DARK_MODE_DARK
        DarkMode.LIGHT -> DarkModeProto.DARK_MODE_LIGHT
        DarkMode.SYSTEM -> DarkModeProto.DARK_MODE_SYSTEM
    }
}

fun DynamicRangeProto.toDomain(): DynamicRange {
    return when (this) {
        DynamicRangeProto.DYNAMIC_RANGE_HLG10 -> DynamicRange.HLG10
        DynamicRangeProto.DYNAMIC_RANGE_SDR,
        DynamicRangeProto.DYNAMIC_RANGE_UNSPECIFIED,
        DynamicRangeProto.UNRECOGNIZED -> DynamicRange.SDR
    }
}

fun DynamicRange.toProto(): DynamicRangeProto {
    return when (this) {
        DynamicRange.HLG10 -> DynamicRangeProto.DYNAMIC_RANGE_HLG10
        DynamicRange.SDR -> DynamicRangeProto.DYNAMIC_RANGE_SDR
    }
}

fun FlashModeProto.toDomain(): FlashMode {
    return when (this) {
        FlashModeProto.FLASH_MODE_ON -> FlashMode.ON
        FlashModeProto.FLASH_MODE_AUTO -> FlashMode.AUTO
        FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST -> FlashMode.LOW_LIGHT_BOOST
        FlashModeProto.FLASH_MODE_OFF,
        FlashModeProto.UNRECOGNIZED -> FlashMode.OFF
    }
}

fun FlashMode.toProto(): FlashModeProto {
    return when (this) {
        FlashMode.ON -> FlashModeProto.FLASH_MODE_ON
        FlashMode.AUTO -> FlashModeProto.FLASH_MODE_AUTO
        FlashMode.LOW_LIGHT_BOOST -> FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST
        FlashMode.OFF -> FlashModeProto.FLASH_MODE_OFF
    }
}

fun ImageOutputFormatProto.toDomain(): ImageOutputFormat {
    return when (this) {
        ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR ->
            ImageOutputFormat.JPEG_ULTRA_HDR
        ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG,
        ImageOutputFormatProto.UNRECOGNIZED -> ImageOutputFormat.JPEG
    }
}

fun ImageOutputFormat.toProto(): ImageOutputFormatProto {
    return when (this) {
        ImageOutputFormat.JPEG_ULTRA_HDR ->
            ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
        ImageOutputFormat.JPEG -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG
    }
}

fun LensFacingProto.toDomain(): LensFacing {
    return when (this) {
        LensFacingProto.LENS_FACING_FRONT -> LensFacing.FRONT
        LensFacingProto.LENS_FACING_BACK,
        LensFacingProto.UNRECOGNIZED -> LensFacing.BACK
    }
}

fun LensFacing.toProto(): LensFacingProto {
    return when (this) {
        LensFacing.FRONT -> LensFacingProto.LENS_FACING_FRONT
        LensFacing.BACK -> LensFacingProto.LENS_FACING_BACK
    }
}

fun LowLightBoostPriorityProto.toDomain(): LowLightBoostPriority {
    return when (this) {
        LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES ->
            LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES
        LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE,
        LowLightBoostPriorityProto.UNRECOGNIZED -> LowLightBoostPriority.PRIORITIZE_AE_MODE
    }
}

fun LowLightBoostPriority.toProto(): LowLightBoostPriorityProto {
    return when (this) {
        LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES ->
            LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES
        LowLightBoostPriority.PRIORITIZE_AE_MODE ->
            LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE
    }
}

fun StabilizationModeProto.toDomain(): StabilizationMode {
    return when (this) {
        StabilizationModeProto.STABILIZATION_MODE_ON -> StabilizationMode.ON
        StabilizationModeProto.STABILIZATION_MODE_OFF -> StabilizationMode.OFF
        StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY -> StabilizationMode.HIGH_QUALITY
        StabilizationModeProto.STABILIZATION_MODE_OPTICAL -> StabilizationMode.OPTICAL
        StabilizationModeProto.STABILIZATION_MODE_AUTO,
        StabilizationModeProto.STABILIZATION_MODE_UNDEFINED,
        StabilizationModeProto.UNRECOGNIZED -> StabilizationMode.AUTO
    }
}

fun StabilizationMode.toProto(): StabilizationModeProto {
    return when (this) {
        StabilizationMode.ON -> StabilizationModeProto.STABILIZATION_MODE_ON
        StabilizationMode.OFF -> StabilizationModeProto.STABILIZATION_MODE_OFF
        StabilizationMode.HIGH_QUALITY -> StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY
        StabilizationMode.OPTICAL -> StabilizationModeProto.STABILIZATION_MODE_OPTICAL
        StabilizationMode.AUTO -> StabilizationModeProto.STABILIZATION_MODE_AUTO
    }
}

fun StreamConfigProto.toDomain(): StreamConfig {
    return when (this) {
        StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM -> StreamConfig.SINGLE_STREAM
        StreamConfigProto.STREAM_CONFIG_MULTI_STREAM,
        StreamConfigProto.STREAM_CONFIG_UNDEFINED,
        StreamConfigProto.UNRECOGNIZED -> StreamConfig.MULTI_STREAM
    }
}

fun StreamConfig.toProto(): StreamConfigProto {
    return when (this) {
        StreamConfig.SINGLE_STREAM -> StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM
        StreamConfig.MULTI_STREAM -> StreamConfigProto.STREAM_CONFIG_MULTI_STREAM
    }
}

fun VideoQualityProto.toDomain(): VideoQuality {
    return when (this) {
        VideoQualityProto.VIDEO_QUALITY_SD -> VideoQuality.SD
        VideoQualityProto.VIDEO_QUALITY_HD -> VideoQuality.HD
        VideoQualityProto.VIDEO_QUALITY_FHD -> VideoQuality.FHD
        VideoQualityProto.VIDEO_QUALITY_UHD -> VideoQuality.UHD
        VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED,
        VideoQualityProto.UNRECOGNIZED -> VideoQuality.UNSPECIFIED
    }
}

fun VideoQuality.toProto(): VideoQualityProto {
    return when (this) {
        VideoQuality.SD -> VideoQualityProto.VIDEO_QUALITY_SD
        VideoQuality.HD -> VideoQualityProto.VIDEO_QUALITY_HD
        VideoQuality.FHD -> VideoQualityProto.VIDEO_QUALITY_FHD
        VideoQuality.UHD -> VideoQualityProto.VIDEO_QUALITY_UHD
        VideoQuality.UNSPECIFIED -> VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED
    }
}
