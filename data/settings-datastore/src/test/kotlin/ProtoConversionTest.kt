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
package com.google.jetpackcamera.settings

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.DynamicRange.Companion.toProto
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.ImageOutputFormat.Companion.toProto
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LensFacing.Companion.toProto
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.LowLightBoostPriority.Companion.toProto
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.VideoQuality.Companion.toProto
import com.google.jetpackcamera.model.proto.AspectRatio as AspectRatioProto
import com.google.jetpackcamera.model.proto.DynamicRange as DynamicRangeProto
import com.google.jetpackcamera.model.proto.ImageOutputFormat as ImageOutputFormatProto
import com.google.jetpackcamera.model.proto.LensFacing as LensFacingProto
import com.google.jetpackcamera.model.proto.LowLightBoostPriority as LowLightBoostPriorityProto
import com.google.jetpackcamera.model.proto.StabilizationMode as StabilizationModeProto
import com.google.jetpackcamera.model.proto.VideoQuality as VideoQualityProto
import kotlin.enums.enumEntries
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ProtoConversionTest {

    // --- DynamicRange ---
    @Test
    fun dynamicRange_convertsToCorrectProto() {
        val correctConversions = { dynamicRange: DynamicRange ->
            when (dynamicRange) {
                DynamicRange.SDR -> DynamicRangeProto.DYNAMIC_RANGE_SDR
                DynamicRange.HLG10 -> DynamicRangeProto.DYNAMIC_RANGE_HLG10
            }
        }

        enumEntries<DynamicRange>().forEach {
            assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun dynamicRangeProto_convertsToCorrectDynamicRange() {
        val correctConversions = { dynamicRangeProto: DynamicRangeProto ->
            when (dynamicRangeProto) {
                DynamicRangeProto.DYNAMIC_RANGE_SDR,
                DynamicRangeProto.UNRECOGNIZED,
                DynamicRangeProto.DYNAMIC_RANGE_UNSPECIFIED -> DynamicRange.SDR

                DynamicRangeProto.DYNAMIC_RANGE_HLG10 -> DynamicRange.HLG10
            }
        }

        enumEntries<DynamicRangeProto>().forEach {
            assertThat(correctConversions(it))
                .isEqualTo(DynamicRange.fromProto(it))
        }
    }

    // --- ImageOutputFormat ---
    @Test
    fun imageOutputFormat_convertsToCorrectProto() {
        val correctConversions = { imageOutputFormat: ImageOutputFormat ->
            when (imageOutputFormat) {
                ImageOutputFormat.JPEG -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG
                ImageOutputFormat.JPEG_ULTRA_HDR ->
                    ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
            }
        }

        enumEntries<ImageOutputFormat>().forEach {
            assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun imageOutputFormatProto_convertsToCorrectImageOutputFormat() {
        val correctConversions = { imageOutputFormatProto: ImageOutputFormatProto ->
            when (imageOutputFormatProto) {
                ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG,
                ImageOutputFormatProto.UNRECOGNIZED -> ImageOutputFormat.JPEG

                ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR ->
                    ImageOutputFormat.JPEG_ULTRA_HDR
            }
        }

        enumEntries<ImageOutputFormatProto>().forEach {
            assertThat(correctConversions(it))
                .isEqualTo(ImageOutputFormat.fromProto(it))
        }
    }

    // --- LensFacing ---
    @Test
    fun lensFacing_convertsToCorrectProto() {
        val correctConversions = { lensFacing: LensFacing ->
            when (lensFacing) {
                LensFacing.BACK -> LensFacingProto.LENS_FACING_BACK
                LensFacing.FRONT -> LensFacingProto.LENS_FACING_FRONT
            }
        }

        enumEntries<LensFacing>().forEach {
            assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun lensFacingProto_convertsToCorrectLensFacing() {
        val correctConversions = { lensFacingProto: LensFacingProto ->
            when (lensFacingProto) {
                LensFacingProto.LENS_FACING_BACK -> LensFacing.BACK
                LensFacingProto.LENS_FACING_FRONT,
                LensFacingProto.UNRECOGNIZED -> LensFacing.FRONT
            }
        }

        enumEntries<LensFacingProto>().forEach {
            assertThat(correctConversions(it))
                .isEqualTo(LensFacing.fromProto(it))
        }
    }

    // --- LowLightBoostPriority ---
    @Test
    fun lowLightBoostPriority_convertsToCorrectProto() {
        val correctConversions = { priority: LowLightBoostPriority ->
            when (priority) {
                LowLightBoostPriority.PRIORITIZE_AE_MODE ->
                    LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE

                LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES ->
                    LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES
            }
        }

        enumEntries<LowLightBoostPriority>().forEach {
            assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun lowLightBoostPriorityProto_convertsToCorrectPriority() {
        val correctConversions = { priorityProto: LowLightBoostPriorityProto ->
            when (priorityProto) {
                LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE,
                LowLightBoostPriorityProto.UNRECOGNIZED -> LowLightBoostPriority.PRIORITIZE_AE_MODE

                LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES ->
                    LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES
            }
        }

        enumEntries<LowLightBoostPriorityProto>().forEach {
            assertThat(correctConversions(it))
                .isEqualTo(LowLightBoostPriority.fromProto(it))
        }
    }

    // --- VideoQuality ---
    @Test
    fun videoQuality_convertsToCorrectProto() {
        val correctConversions = { videoQuality: VideoQuality ->
            when (videoQuality) {
                VideoQuality.UNSPECIFIED -> VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED
                VideoQuality.SD -> VideoQualityProto.VIDEO_QUALITY_SD
                VideoQuality.HD -> VideoQualityProto.VIDEO_QUALITY_HD
                VideoQuality.FHD -> VideoQualityProto.VIDEO_QUALITY_FHD
                VideoQuality.UHD -> VideoQualityProto.VIDEO_QUALITY_UHD
            }
        }

        enumEntries<VideoQuality>().forEach {
            assertThat(correctConversions(it)).isEqualTo(it.toProto())
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
                VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED,
                VideoQualityProto.UNRECOGNIZED -> VideoQuality.UNSPECIFIED
            }
        }

        enumEntries<VideoQualityProto>().forEach {
            assertThat(correctConversions(it))
                .isEqualTo(VideoQuality.fromProto(it))
        }
    }

    // --- AspectRatio (fromProto only) ---
    @Test
    fun aspectRatioProto_convertsToCorrectAspectRatio() {
        val correctConversions = { aspectRatioProto: AspectRatioProto ->
            when (aspectRatioProto) {
                AspectRatioProto.ASPECT_RATIO_THREE_FOUR,
                AspectRatioProto.ASPECT_RATIO_UNDEFINED,
                AspectRatioProto.UNRECOGNIZED -> AspectRatio.THREE_FOUR

                AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN -> AspectRatio.NINE_SIXTEEN
                AspectRatioProto.ASPECT_RATIO_ONE_ONE -> AspectRatio.ONE_ONE
            }
        }

        enumEntries<AspectRatioProto>().forEach {
            assertThat(correctConversions(it))
                .isEqualTo(AspectRatio.fromProto(it))
        }
    }

    // --- StabilizationMode (fromProto only) ---
    @Test
    fun stabilizationModeProto_convertsToCorrectStabilizationMode() {
        val correctConversions = { stabilizationModeProto: StabilizationModeProto ->
            when (stabilizationModeProto) {
                StabilizationModeProto.STABILIZATION_MODE_OFF -> StabilizationMode.OFF
                StabilizationModeProto.STABILIZATION_MODE_ON -> StabilizationMode.ON
                StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY ->
                    StabilizationMode.HIGH_QUALITY

                StabilizationModeProto.STABILIZATION_MODE_OPTICAL -> StabilizationMode.OPTICAL
                StabilizationModeProto.STABILIZATION_MODE_UNDEFINED,
                StabilizationModeProto.UNRECOGNIZED,
                StabilizationModeProto.STABILIZATION_MODE_AUTO -> StabilizationMode.AUTO
            }
        }

        enumEntries<StabilizationModeProto>().forEach {
            assertThat(correctConversions(it))
                .isEqualTo(StabilizationMode.fromProto(it))
        }
    }
}
