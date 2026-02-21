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
package com.google.jetpackcamera.model.mappers

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.DynamicRange.HLG10
import com.google.jetpackcamera.model.DynamicRange.SDR
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.ImageOutputFormat.JPEG
import com.google.jetpackcamera.model.ImageOutputFormat.JPEG_ULTRA_HDR
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LensFacing.BACK
import com.google.jetpackcamera.model.LensFacing.FRONT
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.LowLightBoostPriority.PRIORITIZE_AE_MODE
import com.google.jetpackcamera.model.LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StabilizationMode.AUTO
import com.google.jetpackcamera.model.StabilizationMode.HIGH_QUALITY
import com.google.jetpackcamera.model.StabilizationMode.OFF
import com.google.jetpackcamera.model.StabilizationMode.ON
import com.google.jetpackcamera.model.StabilizationMode.OPTICAL
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.TestPattern.ColorBars
import com.google.jetpackcamera.model.TestPattern.ColorBarsFadeToGray
import com.google.jetpackcamera.model.TestPattern.Custom1
import com.google.jetpackcamera.model.TestPattern.Off
import com.google.jetpackcamera.model.TestPattern.PN9
import com.google.jetpackcamera.model.TestPattern.SolidColor
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.VideoQuality.FHD
import com.google.jetpackcamera.model.VideoQuality.HD
import com.google.jetpackcamera.model.VideoQuality.SD
import com.google.jetpackcamera.model.VideoQuality.UHD
import com.google.jetpackcamera.model.VideoQuality.UNSPECIFIED
import com.google.jetpackcamera.model.proto.AspectRatio as AspectRatioProto
import com.google.jetpackcamera.model.proto.DebugSettings as DebugSettingsProto
import com.google.jetpackcamera.model.proto.DynamicRange as DynamicRangeProto
import com.google.jetpackcamera.model.proto.FlashMode as FlashModeProto
import com.google.jetpackcamera.model.proto.ImageOutputFormat as ImageOutputFormatProto
import com.google.jetpackcamera.model.proto.LensFacing as LensFacingProto
import com.google.jetpackcamera.model.proto.LowLightBoostPriority as LowLightBoostPriorityProto
import com.google.jetpackcamera.model.proto.StabilizationMode as StabilizationModeProto
import com.google.jetpackcamera.model.proto.StreamConfig as StreamConfigProto
import com.google.jetpackcamera.model.proto.TestPattern as ProtoTestPattern
import com.google.jetpackcamera.model.proto.TestPattern.PatternCase
import com.google.jetpackcamera.model.proto.VideoQuality as VideoQualityProto
import com.google.jetpackcamera.model.proto.debugSettings as debugSettingsProto
import com.google.jetpackcamera.model.proto.testPattern as protoTestPattern
import com.google.jetpackcamera.model.proto.testPatternColorBars
import com.google.jetpackcamera.model.proto.testPatternColorBarsFadeToGray
import com.google.jetpackcamera.model.proto.testPatternCustom1
import com.google.jetpackcamera.model.proto.testPatternOff
import com.google.jetpackcamera.model.proto.testPatternPN9
import com.google.jetpackcamera.model.proto.testPatternSolidColor

/**
 * Converts an [AspectRatio] enum to its corresponding [AspectRatioProto] representation.
 */
fun AspectRatio.toProto(): AspectRatioProto = when (this) {
    AspectRatio.NINE_SIXTEEN -> AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN
    AspectRatio.THREE_FOUR -> AspectRatioProto.ASPECT_RATIO_THREE_FOUR
    AspectRatio.ONE_ONE -> AspectRatioProto.ASPECT_RATIO_ONE_ONE
}

/** returns the AspectRatio enum equivalent of a provided AspectRatioProto */
fun AspectRatioProto.toDomain(): AspectRatio {
    return when (this) {
        AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN -> AspectRatio.NINE_SIXTEEN
        AspectRatioProto.ASPECT_RATIO_ONE_ONE -> AspectRatio.ONE_ONE

        // defaults to 3:4 aspect ratio
        AspectRatioProto.ASPECT_RATIO_THREE_FOUR,
        AspectRatioProto.ASPECT_RATIO_UNDEFINED,
        AspectRatioProto.UNRECOGNIZED
        -> AspectRatio.THREE_FOUR
    }
}

/**
 * Creates a [DebugSettings] domain model from its protobuf representation.
 *
 * @return The corresponding [DebugSettings] instance.
 */
fun DebugSettingsProto.toDomain(): DebugSettings {
    return DebugSettings(
        isDebugModeEnabled = this.isDebugModeEnabled,
        singleLensMode = if (this.hasSingleLensMode()) {
            this.singleLensMode.toDomain()
        } else {
            null
        },
        testPattern = this.testPattern.toDomain()
    )
}

/**
 * Converts a [DebugSettings] domain model to its protobuf representation.
 *
 * @receiver The [DebugSettings] instance to convert.
 * @return The corresponding [DebugSettingsProto] instance.
 */
fun DebugSettings.toProto(): DebugSettingsProto = debugSettingsProto {
    isDebugModeEnabled = this@toProto.isDebugModeEnabled
    this@toProto.singleLensMode?.let { lensFacing ->
        singleLensMode = lensFacing.toProto()
    }
    testPattern = this@toProto.testPattern.toProto()
}

/** returns the DynamicRangeType enum equivalent of a provided DynamicRangeTypeProto */
fun DynamicRangeProto.toDomain(): DynamicRange {
    return when (this) {
        DynamicRangeProto.DYNAMIC_RANGE_HLG10 -> HLG10

        // Treat unrecognized and unspecified as SDR as a fallback
        DynamicRangeProto.DYNAMIC_RANGE_SDR,
        DynamicRangeProto.DYNAMIC_RANGE_UNSPECIFIED,
        DynamicRangeProto.UNRECOGNIZED -> SDR
    }
}

/**
 * Converts a [DynamicRange] enum to its corresponding [DynamicRangeProto] representation.
 */
fun DynamicRange.toProto(): DynamicRangeProto {
    return when (this) {
        SDR -> DynamicRangeProto.DYNAMIC_RANGE_SDR
        HLG10 -> DynamicRangeProto.DYNAMIC_RANGE_HLG10
    }
}

/**
 * Converts a [FlashMode] enum to its corresponding [FlashModeProto] representation.
 */
fun FlashMode.toProto(): FlashModeProto = when (this) {
    FlashMode.AUTO -> FlashModeProto.FLASH_MODE_AUTO
    FlashMode.ON -> FlashModeProto.FLASH_MODE_ON
    FlashMode.OFF -> FlashModeProto.FLASH_MODE_OFF
    FlashMode.LOW_LIGHT_BOOST -> FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST
}

/**
 * Converts a [FlashModeProto] to its corresponding [FlashMode] enum representation.
 */
fun FlashModeProto.toDomain(): FlashMode = when (this) {
    FlashModeProto.FLASH_MODE_AUTO -> FlashMode.AUTO
    FlashModeProto.FLASH_MODE_ON -> FlashMode.ON
    FlashModeProto.FLASH_MODE_OFF -> FlashMode.OFF
    FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST -> FlashMode.LOW_LIGHT_BOOST
    else -> FlashMode.OFF
}

/** returns the DynamicRangeType enum equivalent of a provided DynamicRangeTypeProto */
fun ImageOutputFormatProto.toDomain(): ImageOutputFormat {
    return when (this) {
        ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR -> JPEG_ULTRA_HDR

        // Treat unrecognized as JPEG as a fallback
        ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG,
        ImageOutputFormatProto.UNRECOGNIZED -> JPEG
    }
}

/**
 * Converts an [ImageOutputFormat] enum to its corresponding [ImageOutputFormatProto] representation.
 */
fun ImageOutputFormat.toProto(): ImageOutputFormatProto {
    return when (this) {
        JPEG -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG
        JPEG_ULTRA_HDR -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
    }
}

/** returns the LensFacing enum equivalent of a provided LensFacingProto */
fun LensFacingProto.toDomain(): LensFacing {
    return when (this) {
        LensFacingProto.LENS_FACING_FRONT -> FRONT

        // Treat unrecognized as back as a fallback
        LensFacingProto.LENS_FACING_BACK,
        LensFacingProto.UNRECOGNIZED -> BACK
    }
}

/**
 * Converts a [LensFacing] enum to its corresponding [LensFacingProto] representation.
 */
fun LensFacing.toProto(): LensFacingProto {
    return when (this) {
        BACK -> LensFacingProto.LENS_FACING_BACK
        FRONT -> LensFacingProto.LENS_FACING_FRONT
    }
}

/**
 * Returns the [LowLightBoostPriority] enum equivalent of a provided [LowLightBoostPriorityProto].
 *
 * @return The converted [LowLightBoostPriority].
 */
fun LowLightBoostPriorityProto.toDomain(): LowLightBoostPriority {
    return when (this) {
        LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE -> PRIORITIZE_AE_MODE
        LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES ->
            PRIORITIZE_GOOGLE_PLAY_SERVICES

        LowLightBoostPriorityProto.UNRECOGNIZED -> PRIORITIZE_AE_MODE // Default to AE mode
    }
}

/**
 * Converts a [LowLightBoostPriority] enum to its corresponding [LowLightBoostPriorityProto]
 * representation.
 */
fun LowLightBoostPriority.toProto(): LowLightBoostPriorityProto {
    return when (this) {
        PRIORITIZE_AE_MODE -> LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE
        PRIORITIZE_GOOGLE_PLAY_SERVICES ->
            LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES
    }
}

/**
 * Converts a [StabilizationMode] enum to its corresponding [StabilizationModeProto] representation.
 */
fun StabilizationMode.toProto(): StabilizationModeProto = when (this) {
    StabilizationMode.OFF -> StabilizationModeProto.STABILIZATION_MODE_OFF
    StabilizationMode.AUTO -> StabilizationModeProto.STABILIZATION_MODE_AUTO
    StabilizationMode.ON -> StabilizationModeProto.STABILIZATION_MODE_ON
    StabilizationMode.HIGH_QUALITY -> StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY
    StabilizationMode.OPTICAL -> StabilizationModeProto.STABILIZATION_MODE_OPTICAL
}

/** returns the AspectRatio enum equivalent of a provided AspectRatioProto */
fun StabilizationModeProto.toDomain(): StabilizationMode = when (this) {
    StabilizationModeProto.STABILIZATION_MODE_OFF -> OFF
    StabilizationModeProto.STABILIZATION_MODE_ON -> ON
    StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY -> HIGH_QUALITY
    StabilizationModeProto.STABILIZATION_MODE_OPTICAL -> OPTICAL

    // Default to AUTO
    StabilizationModeProto.STABILIZATION_MODE_UNDEFINED,
    StabilizationModeProto.UNRECOGNIZED,
    StabilizationModeProto.STABILIZATION_MODE_AUTO
    -> AUTO
}

/**
 * Converts a [StreamConfig] enum to its corresponding [StreamConfigProto] representation.
 */
fun StreamConfig.toProto(): StreamConfigProto = when (this) {
    StreamConfig.MULTI_STREAM -> StreamConfigProto.STREAM_CONFIG_MULTI_STREAM
    StreamConfig.SINGLE_STREAM -> StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM
}

/**
 * Converts a [StreamConfigProto] to its corresponding [StreamConfig] enum representation.
 */
fun StreamConfigProto.toDomain(): StreamConfig = when (this) {
    StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM -> StreamConfig.SINGLE_STREAM
    StreamConfigProto.STREAM_CONFIG_MULTI_STREAM -> StreamConfig.MULTI_STREAM
    else -> StreamConfig.MULTI_STREAM
}

/**
 * Converts a [TestPattern] sealed interface instance to its Protocol Buffer representation
 * ([ProtoTestPattern]).
 */
fun TestPattern.toProto(): ProtoTestPattern {
    return protoTestPattern {
        when (val pattern = this@toProto) {
            is Off -> off = testPatternOff {}
            is ColorBars -> colorBars = testPatternColorBars {}
            is ColorBarsFadeToGray ->
                colorBarsFadeToGray = testPatternColorBarsFadeToGray {}

            is PN9 -> pn9 = testPatternPN9 {}
            is Custom1 -> custom1 = testPatternCustom1 {}
            is SolidColor -> solidColor = testPatternSolidColor {
                red = pattern.red.toInt()
                greenEven = pattern.greenEven.toInt()
                greenOdd = pattern.greenOdd.toInt()
                blue = pattern.blue.toInt()
            }
        }
    }
}

/**
 * Converts a [ProtoTestPattern] Protocol Buffer message to its Kotlin [TestPattern] sealed
 * interface representation.
 */
fun ProtoTestPattern.toDomain(): TestPattern {
    return when (this.patternCase) {
        PatternCase.OFF,
        PatternCase.PATTERN_NOT_SET -> {
            // Default to Off if the oneof is not set
            Off
        }

        PatternCase.COLOR_BARS -> ColorBars
        PatternCase.COLOR_BARS_FADE_TO_GRAY -> ColorBarsFadeToGray
        PatternCase.PN9 -> PN9
        PatternCase.CUSTOM1 -> Custom1
        PatternCase.SOLID_COLOR -> {
            val protoSolidColor = this.solidColor
            SolidColor(
                red = protoSolidColor.red.toUInt(),
                greenEven = protoSolidColor.greenEven.toUInt(),
                greenOdd = protoSolidColor.greenOdd.toUInt(),
                blue = protoSolidColor.blue.toUInt()
            )
        }
    }
}

/** returns the VideoQuality enum equivalent of a provided VideoQualityProto */
fun VideoQualityProto.toDomain(): VideoQuality {
    return when (this) {
        VideoQualityProto.VIDEO_QUALITY_SD -> SD
        VideoQualityProto.VIDEO_QUALITY_HD -> HD
        VideoQualityProto.VIDEO_QUALITY_FHD -> FHD
        VideoQualityProto.VIDEO_QUALITY_UHD -> UHD
        VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED,
        VideoQualityProto.UNRECOGNIZED
        -> UNSPECIFIED
    }
}

fun VideoQuality.toProto(): VideoQualityProto {
    return when (this) {
        UNSPECIFIED -> VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED
        SD -> VideoQualityProto.VIDEO_QUALITY_SD
        HD -> VideoQualityProto.VIDEO_QUALITY_HD
        FHD -> VideoQualityProto.VIDEO_QUALITY_FHD
        UHD -> VideoQualityProto.VIDEO_QUALITY_UHD
    }
}
