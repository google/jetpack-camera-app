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

import com.google.common.truth.Truth
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.mappers.toDomain
import com.google.jetpackcamera.model.mappers.toProto
import com.google.jetpackcamera.model.proto.debugSettings
import com.google.jetpackcamera.model.proto.testPattern
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
                DynamicRange.SDR -> com.google.jetpackcamera.model.proto.DynamicRange.DYNAMIC_RANGE_SDR
                DynamicRange.HLG10 -> com.google.jetpackcamera.model.proto.DynamicRange.DYNAMIC_RANGE_HLG10
            }
        }

        enumValues<DynamicRange>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun dynamicRangeProto_convertsToCorrectDynamicRange() {
        val correctConversions = { dynamicRangeProto: com.google.jetpackcamera.model.proto.DynamicRange ->
            when (dynamicRangeProto) {
                com.google.jetpackcamera.model.proto.DynamicRange.DYNAMIC_RANGE_SDR,
                com.google.jetpackcamera.model.proto.DynamicRange.UNRECOGNIZED,
                com.google.jetpackcamera.model.proto.DynamicRange.DYNAMIC_RANGE_UNSPECIFIED
                -> DynamicRange.SDR

                com.google.jetpackcamera.model.proto.DynamicRange.DYNAMIC_RANGE_HLG10 -> DynamicRange.HLG10
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.DynamicRange>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun imageOutputFormat_convertsToCorrectProto() {
        val correctConversions = { imageOutputFormat: ImageOutputFormat ->
            when (imageOutputFormat) {
                ImageOutputFormat.JPEG -> com.google.jetpackcamera.model.proto.ImageOutputFormat.IMAGE_OUTPUT_FORMAT_JPEG
                ImageOutputFormat.JPEG_ULTRA_HDR
                -> com.google.jetpackcamera.model.proto.ImageOutputFormat.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
            }
        }

        enumValues<ImageOutputFormat>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun imageOutputFormatProto_convertsToCorrectImageOutputFormat() {
        val correctConversions = { imageOutputFormatProto: com.google.jetpackcamera.model.proto.ImageOutputFormat ->
            when (imageOutputFormatProto) {
                com.google.jetpackcamera.model.proto.ImageOutputFormat.IMAGE_OUTPUT_FORMAT_JPEG,
                com.google.jetpackcamera.model.proto.ImageOutputFormat.UNRECOGNIZED
                -> ImageOutputFormat.JPEG

                com.google.jetpackcamera.model.proto.ImageOutputFormat.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
                -> ImageOutputFormat.JPEG_ULTRA_HDR
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.ImageOutputFormat>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun aspectRatio_convertsToCorrectProto() {
        val correctConversions = { aspectRatio: AspectRatio ->
            when (aspectRatio) {
                AspectRatio.NINE_SIXTEEN -> com.google.jetpackcamera.model.proto.AspectRatio.ASPECT_RATIO_NINE_SIXTEEN
                AspectRatio.THREE_FOUR -> com.google.jetpackcamera.model.proto.AspectRatio.ASPECT_RATIO_THREE_FOUR
                AspectRatio.ONE_ONE -> com.google.jetpackcamera.model.proto.AspectRatio.ASPECT_RATIO_ONE_ONE
            }
        }

        enumValues<AspectRatio>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun aspectRatioProto_convertsToCorrectAspectRatio() {
        val correctConversions = { aspectRatioProto: com.google.jetpackcamera.model.proto.AspectRatio ->
            when (aspectRatioProto) {
                com.google.jetpackcamera.model.proto.AspectRatio.ASPECT_RATIO_NINE_SIXTEEN -> AspectRatio.NINE_SIXTEEN
                com.google.jetpackcamera.model.proto.AspectRatio.ASPECT_RATIO_THREE_FOUR -> AspectRatio.THREE_FOUR
                com.google.jetpackcamera.model.proto.AspectRatio.ASPECT_RATIO_ONE_ONE -> AspectRatio.ONE_ONE
                else -> AspectRatio.THREE_FOUR // Default value
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.AspectRatio>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun lensFacing_convertsToCorrectProto() {
        val correctConversions = { lensFacing: LensFacing ->
            when (lensFacing) {
                LensFacing.FRONT -> com.google.jetpackcamera.model.proto.LensFacing.LENS_FACING_FRONT
                LensFacing.BACK -> com.google.jetpackcamera.model.proto.LensFacing.LENS_FACING_BACK
            }
        }

        enumValues<LensFacing>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun lensFacingProto_convertsToCorrectLensFacing() {
        val correctConversions = { lensFacingProto: com.google.jetpackcamera.model.proto.LensFacing ->
            when (lensFacingProto) {
                com.google.jetpackcamera.model.proto.LensFacing.LENS_FACING_FRONT -> LensFacing.FRONT
                com.google.jetpackcamera.model.proto.LensFacing.LENS_FACING_BACK -> LensFacing.BACK
                else -> LensFacing.BACK // Default value
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.LensFacing>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun lowLightBoostPriority_convertsToCorrectProto() {
        val correctConversions = { lowLightBoostPriority: LowLightBoostPriority ->
            when (lowLightBoostPriority) {
                LowLightBoostPriority.PRIORITIZE_AE_MODE ->
                    com.google.jetpackcamera.model.proto.LowLightBoostPriority.LOW_LIGHT_BOOST_PRIORITY_AE_MODE
                LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES ->
                    com.google.jetpackcamera.model.proto.LowLightBoostPriority.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES
            }
        }

        enumValues<LowLightBoostPriority>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun lowLightBoostPriorityProto_convertsToCorrectLowLightBoostPriority() {
        val correctConversions = { lowLightBoostPriorityProto: com.google.jetpackcamera.model.proto.LowLightBoostPriority ->
            when (lowLightBoostPriorityProto) {
                com.google.jetpackcamera.model.proto.LowLightBoostPriority.LOW_LIGHT_BOOST_PRIORITY_AE_MODE ->
                    LowLightBoostPriority.PRIORITIZE_AE_MODE
                com.google.jetpackcamera.model.proto.LowLightBoostPriority.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES ->
                    LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES
                else -> LowLightBoostPriority.PRIORITIZE_AE_MODE // Default value
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.LowLightBoostPriority>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun stabilizationMode_convertsToCorrectProto() {
        val correctConversions = { stabilizationMode: StabilizationMode ->
            when (stabilizationMode) {
                StabilizationMode.OFF -> com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_OFF
                StabilizationMode.ON -> com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_ON
                StabilizationMode.HIGH_QUALITY ->
                    com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_HIGH_QUALITY
                StabilizationMode.OPTICAL -> com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_OPTICAL
                StabilizationMode.AUTO -> com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_AUTO
            }
        }

        enumValues<StabilizationMode>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun stabilizationModeProto_convertsToCorrectStabilizationMode() {
        val correctConversions = { stabilizationModeProto: com.google.jetpackcamera.model.proto.StabilizationMode ->
            when (stabilizationModeProto) {
                com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_OFF -> StabilizationMode.OFF
                com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_ON -> StabilizationMode.ON
                com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_HIGH_QUALITY ->
                    StabilizationMode.HIGH_QUALITY
                com.google.jetpackcamera.model.proto.StabilizationMode.STABILIZATION_MODE_OPTICAL -> StabilizationMode.OPTICAL
                else -> StabilizationMode.AUTO // Default value
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.StabilizationMode>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun videoQuality_convertsToCorrectProto() {
        val correctConversions = { videoQuality: VideoQuality ->
            when (videoQuality) {
                VideoQuality.SD -> com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_SD
                VideoQuality.HD -> com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_HD
                VideoQuality.FHD -> com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_FHD
                VideoQuality.UHD -> com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_UHD
                VideoQuality.UNSPECIFIED -> com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_UNSPECIFIED
            }
        }

        enumValues<VideoQuality>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun videoQualityProto_convertsToCorrectVideoQuality() {
        val correctConversions = { videoQualityProto: com.google.jetpackcamera.model.proto.VideoQuality ->
            when (videoQualityProto) {
                com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_SD -> VideoQuality.SD
                com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_HD -> VideoQuality.HD
                com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_FHD -> VideoQuality.FHD
                com.google.jetpackcamera.model.proto.VideoQuality.VIDEO_QUALITY_UHD -> VideoQuality.UHD
                else -> VideoQuality.UNSPECIFIED // Default value
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.VideoQuality>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun flashMode_convertsToCorrectProto() {
        val correctConversions = { flashMode: FlashMode ->
            when (flashMode) {
                FlashMode.AUTO -> com.google.jetpackcamera.model.proto.FlashMode.FLASH_MODE_AUTO
                FlashMode.ON -> com.google.jetpackcamera.model.proto.FlashMode.FLASH_MODE_ON
                FlashMode.OFF -> com.google.jetpackcamera.model.proto.FlashMode.FLASH_MODE_OFF
                FlashMode.LOW_LIGHT_BOOST -> com.google.jetpackcamera.model.proto.FlashMode.FLASH_MODE_LOW_LIGHT_BOOST
            }
        }

        enumValues<FlashMode>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun flashModeProto_convertsToCorrectFlashMode() {
        val correctConversions = { flashModeProto: com.google.jetpackcamera.model.proto.FlashMode ->
            when (flashModeProto) {
                com.google.jetpackcamera.model.proto.FlashMode.FLASH_MODE_AUTO -> FlashMode.AUTO
                com.google.jetpackcamera.model.proto.FlashMode.FLASH_MODE_ON -> FlashMode.ON
                com.google.jetpackcamera.model.proto.FlashMode.FLASH_MODE_OFF -> FlashMode.OFF
                com.google.jetpackcamera.model.proto.FlashMode.FLASH_MODE_LOW_LIGHT_BOOST -> FlashMode.LOW_LIGHT_BOOST
                else -> FlashMode.OFF // Default value
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.FlashMode>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun streamConfig_convertsToCorrectProto() {
        val correctConversions = { streamConfig: StreamConfig ->
            when (streamConfig) {
                StreamConfig.MULTI_STREAM -> com.google.jetpackcamera.model.proto.StreamConfig.STREAM_CONFIG_MULTI_STREAM
                StreamConfig.SINGLE_STREAM -> com.google.jetpackcamera.model.proto.StreamConfig.STREAM_CONFIG_SINGLE_STREAM
            }
        }

        enumValues<StreamConfig>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun streamConfigProto_convertsToCorrectStreamConfig() {
        val correctConversions = { streamConfigProto: com.google.jetpackcamera.model.proto.StreamConfig ->
            when (streamConfigProto) {
                com.google.jetpackcamera.model.proto.StreamConfig.STREAM_CONFIG_MULTI_STREAM -> StreamConfig.MULTI_STREAM
                com.google.jetpackcamera.model.proto.StreamConfig.STREAM_CONFIG_SINGLE_STREAM -> StreamConfig.SINGLE_STREAM
                else -> StreamConfig.MULTI_STREAM // Default value
            }
        }

        enumValues<com.google.jetpackcamera.model.proto.StreamConfig>().forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }

    @Test
    fun debugSettings_convertsToCorrectProto() {
        val correctConversions = { debugSettings: DebugSettings ->
            debugSettings {
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
        val correctConversions = { debugSettingsProto: com.google.jetpackcamera.model.proto.DebugSettings ->
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

        val debugSettingsProtosToTest: List<com.google.jetpackcamera.model.proto.DebugSettings> = listOf(
            debugSettings { },
            debugSettings { isDebugModeEnabled = true },
            debugSettings {
                singleLensMode = com.google.jetpackcamera.model.proto.LensFacing.LENS_FACING_FRONT
            },
            debugSettings {
                testPattern = testPattern {
                    colorBars = testPatternColorBars { }
                }
            },
            debugSettings {
                isDebugModeEnabled = true
                singleLensMode = com.google.jetpackcamera.model.proto.LensFacing.LENS_FACING_BACK
                testPattern = testPattern {
                    pn9 = testPatternPN9 { }
                }
            }
        )

        debugSettingsProtosToTest.forEach {
            Truth.assertThat(correctConversions(it)).isEqualTo(it.toDomain())
        }
    }
}