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
package com.google.jetpackcamera.model

import com.google.common.truth.Truth
import com.google.jetpackcamera.model.mappers.toDomain
import com.google.jetpackcamera.model.mappers.toProto
import com.google.jetpackcamera.model.proto.AspectRatio as AspectRatioProto
import com.google.jetpackcamera.model.proto.DebugSettings as DebugSettingsProto
import com.google.jetpackcamera.model.proto.DynamicRange as DynamicRangeProto
import com.google.jetpackcamera.model.proto.FlashMode as FlashModeProto
import com.google.jetpackcamera.model.proto.ImageOutputFormat as ImageOutputFormatProto
import com.google.jetpackcamera.model.proto.LensFacing as LensFacingProto
import com.google.jetpackcamera.model.proto.LowLightBoostPriority as LowLightBoostPriorityProto
import com.google.jetpackcamera.model.proto.StabilizationMode as StabilizationModeProto
import com.google.jetpackcamera.model.proto.StreamConfig as StreamConfigProto
import com.google.jetpackcamera.model.proto.VideoQuality as VideoQualityProto
import com.google.jetpackcamera.model.proto.debugSettings as debugSettingsProto
import com.google.jetpackcamera.model.proto.testPattern as protoTestPattern
import com.google.jetpackcamera.model.proto.testPatternColorBars
import com.google.jetpackcamera.model.proto.testPatternPN9
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProtoConversionTest {
    @Test
    fun dynamicRange_convertsToCorrectProto() {
        val correctConversions = { dynamicRange: DynamicRange ->
            when (dynamicRange) {
                DynamicRange.SDR -> DynamicRangeProto.DYNAMIC_RANGE_SDR
                DynamicRange.HLG10 -> DynamicRangeProto.DYNAMIC_RANGE_HLG10
            }
        }

        enumValues<DynamicRange>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun dynamicRangeProto_convertsToCorrectDynamicRange() {
        val correctConversions = { dynamicRangeProto: DynamicRangeProto ->
            when (dynamicRangeProto) {
                DynamicRangeProto.DYNAMIC_RANGE_SDR,
                DynamicRangeProto.UNRECOGNIZED,
                DynamicRangeProto.DYNAMIC_RANGE_UNSPECIFIED
                -> DynamicRange.SDR

                DynamicRangeProto.DYNAMIC_RANGE_HLG10 -> DynamicRange.HLG10
            }
        }

        enumValues<DynamicRangeProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun imageOutputFormat_convertsToCorrectProto() {
        val correctConversions = { imageOutputFormat: ImageOutputFormat ->
            when (imageOutputFormat) {
                ImageOutputFormat.JPEG -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG
                ImageOutputFormat.JPEG_ULTRA_HDR
                -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
            }
        }

        enumValues<ImageOutputFormat>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun imageOutputFormatProto_convertsToCorrectImageOutputFormat() {
        val correctConversions = { imageOutputFormatProto: ImageOutputFormatProto ->
            when (imageOutputFormatProto) {
                ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG,
                ImageOutputFormatProto.UNRECOGNIZED
                -> ImageOutputFormat.JPEG

                ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
                -> ImageOutputFormat.JPEG_ULTRA_HDR
            }
        }

        enumValues<ImageOutputFormatProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun aspectRatio_convertsToCorrectProto() {
        val correctConversions = { aspectRatio: AspectRatio ->
            when (aspectRatio) {
                AspectRatio.NINE_SIXTEEN -> AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN
                AspectRatio.THREE_FOUR -> AspectRatioProto.ASPECT_RATIO_THREE_FOUR
                AspectRatio.ONE_ONE -> AspectRatioProto.ASPECT_RATIO_ONE_ONE
            }
        }

        enumValues<AspectRatio>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun aspectRatioProto_convertsToCorrectAspectRatio() {
        val correctConversions = { aspectRatioProto: AspectRatioProto ->
            when (aspectRatioProto) {
                AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN -> AspectRatio.NINE_SIXTEEN
                AspectRatioProto.ASPECT_RATIO_THREE_FOUR -> AspectRatio.THREE_FOUR
                AspectRatioProto.ASPECT_RATIO_ONE_ONE -> AspectRatio.ONE_ONE
                else -> AspectRatio.THREE_FOUR // Default value
            }
        }

        enumValues<AspectRatioProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun lensFacing_convertsToCorrectProto() {
        val correctConversions = { lensFacing: LensFacing ->
            when (lensFacing) {
                LensFacing.FRONT -> LensFacingProto.LENS_FACING_FRONT
                LensFacing.BACK -> LensFacingProto.LENS_FACING_BACK
            }
        }

        enumValues<LensFacing>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun lensFacingProto_convertsToCorrectLensFacing() {
        val correctConversions = { lensFacingProto: LensFacingProto ->
            when (lensFacingProto) {
                LensFacingProto.LENS_FACING_FRONT -> LensFacing.FRONT
                LensFacingProto.LENS_FACING_BACK -> LensFacing.BACK
                else -> LensFacing.BACK // Default value
            }
        }

        enumValues<LensFacingProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun lowLightBoostPriority_convertsToCorrectProto() {
        val correctConversions = { lowLightBoostPriority: LowLightBoostPriority ->
            when (lowLightBoostPriority) {
                LowLightBoostPriority.PRIORITIZE_AE_MODE ->
                    LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE
                LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES ->
                    LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES
            }
        }

        enumValues<LowLightBoostPriority>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun lowLightBoostPriorityProto_convertsToCorrectLowLightBoostPriority() {
        val correctConversions = { lowLightBoostPriorityProto: LowLightBoostPriorityProto ->
            when (lowLightBoostPriorityProto) {
                LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE ->
                    LowLightBoostPriority.PRIORITIZE_AE_MODE
                LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES ->
                    LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES
                else -> LowLightBoostPriority.PRIORITIZE_AE_MODE // Default value
            }
        }

        enumValues<LowLightBoostPriorityProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun stabilizationMode_convertsToCorrectProto() {
        val correctConversions = { stabilizationMode: StabilizationMode ->
            when (stabilizationMode) {
                StabilizationMode.OFF -> StabilizationModeProto.STABILIZATION_MODE_OFF
                StabilizationMode.ON -> StabilizationModeProto.STABILIZATION_MODE_ON
                StabilizationMode.HIGH_QUALITY ->
                    StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY
                StabilizationMode.OPTICAL -> StabilizationModeProto.STABILIZATION_MODE_OPTICAL
                StabilizationMode.AUTO -> StabilizationModeProto.STABILIZATION_MODE_AUTO
            }
        }

        enumValues<StabilizationMode>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun stabilizationModeProto_convertsToCorrectStabilizationMode() {
        val correctConversions = { stabilizationModeProto: StabilizationModeProto ->
            when (stabilizationModeProto) {
                StabilizationModeProto.STABILIZATION_MODE_OFF -> StabilizationMode.OFF
                StabilizationModeProto.STABILIZATION_MODE_ON -> StabilizationMode.ON
                StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY ->
                    StabilizationMode.HIGH_QUALITY
                StabilizationModeProto.STABILIZATION_MODE_OPTICAL -> StabilizationMode.OPTICAL
                else -> StabilizationMode.AUTO // Default value
            }
        }

        enumValues<StabilizationModeProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun videoQuality_convertsToCorrectProto() {
        val correctConversions = { videoQuality: VideoQuality ->
            when (videoQuality) {
                VideoQuality.SD -> VideoQualityProto.VIDEO_QUALITY_SD
                VideoQuality.HD -> VideoQualityProto.VIDEO_QUALITY_HD
                VideoQuality.FHD -> VideoQualityProto.VIDEO_QUALITY_FHD
                VideoQuality.UHD -> VideoQualityProto.VIDEO_QUALITY_UHD
                VideoQuality.UNSPECIFIED -> VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED
            }
        }

        enumValues<VideoQuality>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun videoQualityProto_convertsToCorrectVideoQuality() {
        val correctConversions = { videoQualityProto: VideoQualityProto ->
            when (videoQualityProto) {
                VideoQualityProto.VIDEO_QUALITY_SD -> VideoQuality.SD
                VideoQualityProto.VIDEO_QUALITY_HD -> VideoQuality.HD
                VideoQualityProto.VIDEO_QUALITY_FHD -> VideoQuality.FHD
                VideoQualityProto.VIDEO_QUALITY_UHD -> VideoQuality.UHD
                else -> VideoQuality.UNSPECIFIED // Default value
            }
        }

        enumValues<VideoQualityProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun flashMode_convertsToCorrectProto() {
        val correctConversions = { flashMode: FlashMode ->
            when (flashMode) {
                FlashMode.AUTO -> FlashModeProto.FLASH_MODE_AUTO
                FlashMode.ON -> FlashModeProto.FLASH_MODE_ON
                FlashMode.OFF -> FlashModeProto.FLASH_MODE_OFF
                FlashMode.LOW_LIGHT_BOOST -> FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST
            }
        }

        enumValues<FlashMode>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun flashModeProto_convertsToCorrectFlashMode() {
        val correctConversions = { flashModeProto: FlashModeProto ->
            when (flashModeProto) {
                FlashModeProto.FLASH_MODE_AUTO -> FlashMode.AUTO
                FlashModeProto.FLASH_MODE_ON -> FlashMode.ON
                FlashModeProto.FLASH_MODE_OFF -> FlashMode.OFF
                FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST -> FlashMode.LOW_LIGHT_BOOST
                else -> FlashMode.OFF // Default value
            }
        }

        enumValues<FlashModeProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun streamConfig_convertsToCorrectProto() {
        val correctConversions = { streamConfig: StreamConfig ->
            when (streamConfig) {
                StreamConfig.MULTI_STREAM -> StreamConfigProto.STREAM_CONFIG_MULTI_STREAM
                StreamConfig.SINGLE_STREAM -> StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM
            }
        }

        enumValues<StreamConfig>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun streamConfigProto_convertsToCorrectStreamConfig() {
        val correctConversions = { streamConfigProto: StreamConfigProto ->
            when (streamConfigProto) {
                StreamConfigProto.STREAM_CONFIG_MULTI_STREAM -> StreamConfig.MULTI_STREAM
                StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM -> StreamConfig.SINGLE_STREAM
                else -> StreamConfig.MULTI_STREAM // Default value
            }
        }

        enumValues<StreamConfigProto>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun debugSettings_convertsToCorrectProto() {
        val correctConversions = { debugSettings: DebugSettings ->
            debugSettingsProto {
                isDebugModeEnabled = debugSettings.isDebugModeEnabled
                debugSettings.singleLensMode?.let {
                    singleLensMode = it.toProto()
                }
                testPattern = debugSettings.testPattern.toProto()
            }
        }

        val debugSettingsToTest = listOf(
            DebugSettings(),
            DebugSettings(isDebugModeEnabled = true),
            DebugSettings(singleLensMode = LensFacing.FRONT),
            DebugSettings(testPattern = TestPattern.ColorBars),
            DebugSettings(
                isDebugModeEnabled = true,
                singleLensMode = LensFacing.BACK,
                testPattern = TestPattern.PN9
            )
        )

        debugSettingsToTest.forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun debugSettingsProto_convertsToCorrectDebugSettings() {
        val correctConversions = { debugSettingsProto: DebugSettingsProto ->
            DebugSettings(
                isDebugModeEnabled = debugSettingsProto.isDebugModeEnabled,
                singleLensMode = if (debugSettingsProto.hasSingleLensMode()) {
                    debugSettingsProto.singleLensMode.toDomain()
                } else {
                    null
                },
                testPattern = debugSettingsProto.testPattern.toDomain()
            )
        }

        val debugSettingsProtosToTest: List<DebugSettingsProto> = listOf(
            debugSettingsProto { },
            debugSettingsProto { isDebugModeEnabled = true },
            debugSettingsProto { singleLensMode = LensFacingProto.LENS_FACING_FRONT },
            debugSettingsProto {
                testPattern = protoTestPattern {
                    colorBars = testPatternColorBars { }
                }
            },
            debugSettingsProto {
                isDebugModeEnabled = true
                singleLensMode = LensFacingProto.LENS_FACING_BACK
                testPattern = protoTestPattern {
                    pn9 = testPatternPN9 { }
                }
            }
        )

        debugSettingsProtosToTest.forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }
}
