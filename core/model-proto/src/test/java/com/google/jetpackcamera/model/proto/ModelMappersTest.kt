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

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.ConcurrentCameraMode
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
import com.google.jetpackcamera.model.proto.ConcurrentCameraMode as ConcurrentCameraModeProto
import com.google.jetpackcamera.model.proto.DarkMode as DarkModeProto
import com.google.jetpackcamera.model.proto.DynamicRange as DynamicRangeProto
import com.google.jetpackcamera.model.proto.FlashMode as FlashModeProto
import com.google.jetpackcamera.model.proto.ImageOutputFormat as ImageOutputFormatProto
import com.google.jetpackcamera.model.proto.LensFacing as LensFacingProto
import com.google.jetpackcamera.model.proto.LowLightBoostPriority as LowLightBoostPriorityProto
import com.google.jetpackcamera.model.proto.StabilizationMode as StabilizationModeProto
import com.google.jetpackcamera.model.proto.StreamConfig as StreamConfigProto
import com.google.jetpackcamera.model.proto.VideoQuality as VideoQualityProto
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModelMappersTest {

    @Test
    fun aspectRatioMapsCorrectly() {
        assertThat(
            AspectRatio.NINE_SIXTEEN.toProto()
        ).isEqualTo(AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN)
        assertThat(
            AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN.toDomain()
        ).isEqualTo(AspectRatio.NINE_SIXTEEN)

        assertThat(AspectRatio.ONE_ONE.toProto()).isEqualTo(AspectRatioProto.ASPECT_RATIO_ONE_ONE)
        assertThat(AspectRatioProto.ASPECT_RATIO_ONE_ONE.toDomain()).isEqualTo(AspectRatio.ONE_ONE)

        assertThat(
            AspectRatio.THREE_FOUR.toProto()
        ).isEqualTo(AspectRatioProto.ASPECT_RATIO_THREE_FOUR)
        assertThat(
            AspectRatioProto.ASPECT_RATIO_THREE_FOUR.toDomain()
        ).isEqualTo(AspectRatio.THREE_FOUR)

        assertThat(AspectRatioProto.UNRECOGNIZED.toDomain()).isEqualTo(AspectRatio.THREE_FOUR)
    }

    @Test
    fun darkModeMapsCorrectly() {
        assertThat(DarkMode.DARK.toProto()).isEqualTo(DarkModeProto.DARK_MODE_DARK)
        assertThat(DarkModeProto.DARK_MODE_DARK.toDomain()).isEqualTo(DarkMode.DARK)

        assertThat(DarkMode.LIGHT.toProto()).isEqualTo(DarkModeProto.DARK_MODE_LIGHT)
        assertThat(DarkModeProto.DARK_MODE_LIGHT.toDomain()).isEqualTo(DarkMode.LIGHT)

        assertThat(DarkMode.SYSTEM.toProto()).isEqualTo(DarkModeProto.DARK_MODE_SYSTEM)
        assertThat(DarkModeProto.DARK_MODE_SYSTEM.toDomain()).isEqualTo(DarkMode.SYSTEM)

        assertThat(DarkModeProto.UNRECOGNIZED.toDomain()).isEqualTo(DarkMode.SYSTEM)
    }

    @Test
    fun dynamicRangeMapsCorrectly() {
        assertThat(DynamicRange.HLG10.toProto()).isEqualTo(DynamicRangeProto.DYNAMIC_RANGE_HLG10)
        assertThat(DynamicRangeProto.DYNAMIC_RANGE_HLG10.toDomain()).isEqualTo(DynamicRange.HLG10)

        assertThat(DynamicRange.SDR.toProto()).isEqualTo(DynamicRangeProto.DYNAMIC_RANGE_SDR)
        assertThat(DynamicRangeProto.DYNAMIC_RANGE_SDR.toDomain()).isEqualTo(DynamicRange.SDR)

        assertThat(DynamicRangeProto.UNRECOGNIZED.toDomain()).isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun flashModeMapsCorrectly() {
        assertThat(FlashMode.ON.toProto()).isEqualTo(FlashModeProto.FLASH_MODE_ON)
        assertThat(FlashModeProto.FLASH_MODE_ON.toDomain()).isEqualTo(FlashMode.ON)

        assertThat(FlashMode.AUTO.toProto()).isEqualTo(FlashModeProto.FLASH_MODE_AUTO)
        assertThat(FlashModeProto.FLASH_MODE_AUTO.toDomain()).isEqualTo(FlashMode.AUTO)

        assertThat(FlashMode.OFF.toProto()).isEqualTo(FlashModeProto.FLASH_MODE_OFF)
        assertThat(FlashModeProto.FLASH_MODE_OFF.toDomain()).isEqualTo(FlashMode.OFF)

        assertThat(
            FlashMode.LOW_LIGHT_BOOST.toProto()
        ).isEqualTo(FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST)
        assertThat(
            FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST.toDomain()
        ).isEqualTo(FlashMode.LOW_LIGHT_BOOST)

        assertThat(FlashModeProto.UNRECOGNIZED.toDomain()).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun imageOutputFormatMapsCorrectly() {
        assertThat(
            ImageOutputFormat.JPEG.toProto()
        ).isEqualTo(ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG)
        assertThat(
            ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG.toDomain()
        ).isEqualTo(ImageOutputFormat.JPEG)

        assertThat(
            ImageOutputFormat.JPEG_ULTRA_HDR.toProto()
        ).isEqualTo(ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        assertThat(
            ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR.toDomain()
        ).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)

        assertThat(ImageOutputFormatProto.UNRECOGNIZED.toDomain()).isEqualTo(ImageOutputFormat.JPEG)
    }

    @Test
    fun lensFacingMapsCorrectly() {
        assertThat(LensFacing.FRONT.toProto()).isEqualTo(LensFacingProto.LENS_FACING_FRONT)
        assertThat(LensFacingProto.LENS_FACING_FRONT.toDomain()).isEqualTo(LensFacing.FRONT)

        assertThat(LensFacing.BACK.toProto()).isEqualTo(LensFacingProto.LENS_FACING_BACK)
        assertThat(LensFacingProto.LENS_FACING_BACK.toDomain()).isEqualTo(LensFacing.BACK)

        assertThat(LensFacingProto.UNRECOGNIZED.toDomain()).isEqualTo(LensFacing.BACK)
    }

    @Test
    fun lowLightBoostPriorityMapsCorrectly() {
        assertThat(
            LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES.toProto()
        ).isEqualTo(LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES)
        assertThat(
            LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES.toDomain()
        ).isEqualTo(LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES)

        assertThat(
            LowLightBoostPriority.PRIORITIZE_AE_MODE.toProto()
        ).isEqualTo(LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE)
        assertThat(
            LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE.toDomain()
        ).isEqualTo(LowLightBoostPriority.PRIORITIZE_AE_MODE)

        assertThat(
            LowLightBoostPriorityProto.UNRECOGNIZED.toDomain()
        ).isEqualTo(LowLightBoostPriority.PRIORITIZE_AE_MODE)
    }

    @Test
    fun stabilizationModeMapsCorrectly() {
        assertThat(
            StabilizationMode.ON.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_ON)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_ON.toDomain()
        ).isEqualTo(StabilizationMode.ON)

        assertThat(
            StabilizationMode.OFF.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_OFF)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_OFF.toDomain()
        ).isEqualTo(StabilizationMode.OFF)

        assertThat(
            StabilizationMode.HIGH_QUALITY.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY.toDomain()
        ).isEqualTo(StabilizationMode.HIGH_QUALITY)

        assertThat(
            StabilizationMode.OPTICAL.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_OPTICAL)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_OPTICAL.toDomain()
        ).isEqualTo(StabilizationMode.OPTICAL)

        assertThat(
            StabilizationMode.AUTO.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_AUTO)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_AUTO.toDomain()
        ).isEqualTo(StabilizationMode.AUTO)

        assertThat(StabilizationModeProto.UNRECOGNIZED.toDomain()).isEqualTo(StabilizationMode.AUTO)
    }

    @Test
    fun streamConfigMapsCorrectly() {
        assertThat(
            StreamConfig.SINGLE_STREAM.toProto()
        ).isEqualTo(StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM)
        assertThat(
            StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM.toDomain()
        ).isEqualTo(StreamConfig.SINGLE_STREAM)

        assertThat(
            StreamConfig.MULTI_STREAM.toProto()
        ).isEqualTo(StreamConfigProto.STREAM_CONFIG_MULTI_STREAM)
        assertThat(
            StreamConfigProto.STREAM_CONFIG_MULTI_STREAM.toDomain()
        ).isEqualTo(StreamConfig.MULTI_STREAM)

        assertThat(StreamConfigProto.UNRECOGNIZED.toDomain()).isEqualTo(StreamConfig.MULTI_STREAM)
    }

    @Test
    fun videoQualityMapsCorrectly() {
        assertThat(VideoQuality.SD.toProto()).isEqualTo(VideoQualityProto.VIDEO_QUALITY_SD)
        assertThat(VideoQualityProto.VIDEO_QUALITY_SD.toDomain()).isEqualTo(VideoQuality.SD)

        assertThat(VideoQuality.HD.toProto()).isEqualTo(VideoQualityProto.VIDEO_QUALITY_HD)
        assertThat(VideoQualityProto.VIDEO_QUALITY_HD.toDomain()).isEqualTo(VideoQuality.HD)

        assertThat(VideoQuality.FHD.toProto()).isEqualTo(VideoQualityProto.VIDEO_QUALITY_FHD)
        assertThat(VideoQualityProto.VIDEO_QUALITY_FHD.toDomain()).isEqualTo(VideoQuality.FHD)

        assertThat(VideoQuality.UHD.toProto()).isEqualTo(VideoQualityProto.VIDEO_QUALITY_UHD)
        assertThat(VideoQualityProto.VIDEO_QUALITY_UHD.toDomain()).isEqualTo(VideoQuality.UHD)

        assertThat(
            VideoQuality.UNSPECIFIED.toProto()
        ).isEqualTo(VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED)
        assertThat(
            VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED.toDomain()
        ).isEqualTo(VideoQuality.UNSPECIFIED)

        assertThat(VideoQualityProto.UNRECOGNIZED.toDomain()).isEqualTo(VideoQuality.UNSPECIFIED)
    }

    @Test
    fun concurrentCameraModeMapsCorrectly() {
        assertThat(
            ConcurrentCameraMode.DUAL.toProto()
        ).isEqualTo(ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_DUAL)
        assertThat(
            ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_DUAL.toDomain()
        ).isEqualTo(ConcurrentCameraMode.DUAL)

        assertThat(
            ConcurrentCameraMode.OFF.toProto()
        ).isEqualTo(ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_OFF)
        assertThat(
            ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_OFF.toDomain()
        ).isEqualTo(ConcurrentCameraMode.OFF)

        assertThat(
            ConcurrentCameraModeProto.UNRECOGNIZED.toDomain()
        ).isEqualTo(ConcurrentCameraMode.OFF)
    }
}
