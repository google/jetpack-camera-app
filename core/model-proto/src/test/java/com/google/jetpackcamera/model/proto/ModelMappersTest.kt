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
            AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN.toModel()
        ).isEqualTo(AspectRatio.NINE_SIXTEEN)

        assertThat(AspectRatio.ONE_ONE.toProto()).isEqualTo(AspectRatioProto.ASPECT_RATIO_ONE_ONE)
        assertThat(AspectRatioProto.ASPECT_RATIO_ONE_ONE.toModel()).isEqualTo(AspectRatio.ONE_ONE)

        assertThat(
            AspectRatio.THREE_FOUR.toProto()
        ).isEqualTo(AspectRatioProto.ASPECT_RATIO_THREE_FOUR)
        assertThat(
            AspectRatioProto.ASPECT_RATIO_THREE_FOUR.toModel()
        ).isEqualTo(AspectRatio.THREE_FOUR)

        assertThat(AspectRatioProto.UNRECOGNIZED.toModel()).isEqualTo(AspectRatio.THREE_FOUR)
    }

    @Test
    fun darkModeMapsCorrectly() {
        assertThat(DarkMode.DARK.toProto()).isEqualTo(DarkModeProto.DARK_MODE_DARK)
        assertThat(DarkModeProto.DARK_MODE_DARK.toModel()).isEqualTo(DarkMode.DARK)

        assertThat(DarkMode.LIGHT.toProto()).isEqualTo(DarkModeProto.DARK_MODE_LIGHT)
        assertThat(DarkModeProto.DARK_MODE_LIGHT.toModel()).isEqualTo(DarkMode.LIGHT)

        assertThat(DarkMode.SYSTEM.toProto()).isEqualTo(DarkModeProto.DARK_MODE_SYSTEM)
        assertThat(DarkModeProto.DARK_MODE_SYSTEM.toModel()).isEqualTo(DarkMode.SYSTEM)

        assertThat(DarkModeProto.UNRECOGNIZED.toModel()).isEqualTo(DarkMode.SYSTEM)
    }

    @Test
    fun dynamicRangeMapsCorrectly() {
        assertThat(DynamicRange.HLG10.toProto()).isEqualTo(DynamicRangeProto.DYNAMIC_RANGE_HLG10)
        assertThat(DynamicRangeProto.DYNAMIC_RANGE_HLG10.toModel()).isEqualTo(DynamicRange.HLG10)

        assertThat(DynamicRange.SDR.toProto()).isEqualTo(DynamicRangeProto.DYNAMIC_RANGE_SDR)
        assertThat(DynamicRangeProto.DYNAMIC_RANGE_SDR.toModel()).isEqualTo(DynamicRange.SDR)

        assertThat(DynamicRangeProto.UNRECOGNIZED.toModel()).isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun flashModeMapsCorrectly() {
        assertThat(FlashMode.ON.toProto()).isEqualTo(FlashModeProto.FLASH_MODE_ON)
        assertThat(FlashModeProto.FLASH_MODE_ON.toModel()).isEqualTo(FlashMode.ON)

        assertThat(FlashMode.AUTO.toProto()).isEqualTo(FlashModeProto.FLASH_MODE_AUTO)
        assertThat(FlashModeProto.FLASH_MODE_AUTO.toModel()).isEqualTo(FlashMode.AUTO)

        assertThat(FlashMode.OFF.toProto()).isEqualTo(FlashModeProto.FLASH_MODE_OFF)
        assertThat(FlashModeProto.FLASH_MODE_OFF.toModel()).isEqualTo(FlashMode.OFF)

        assertThat(
            FlashMode.LOW_LIGHT_BOOST.toProto()
        ).isEqualTo(FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST)
        assertThat(
            FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST.toModel()
        ).isEqualTo(FlashMode.LOW_LIGHT_BOOST)

        assertThat(FlashModeProto.UNRECOGNIZED.toModel()).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun imageOutputFormatMapsCorrectly() {
        assertThat(
            ImageOutputFormat.JPEG.toProto()
        ).isEqualTo(ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG)
        assertThat(
            ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG.toModel()
        ).isEqualTo(ImageOutputFormat.JPEG)

        assertThat(
            ImageOutputFormat.JPEG_ULTRA_HDR.toProto()
        ).isEqualTo(ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR)
        assertThat(
            ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR.toModel()
        ).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)

        assertThat(ImageOutputFormatProto.UNRECOGNIZED.toModel()).isEqualTo(ImageOutputFormat.JPEG)
    }

    @Test
    fun lensFacingMapsCorrectly() {
        assertThat(LensFacing.FRONT.toProto()).isEqualTo(LensFacingProto.LENS_FACING_FRONT)
        assertThat(LensFacingProto.LENS_FACING_FRONT.toModel()).isEqualTo(LensFacing.FRONT)

        assertThat(LensFacing.BACK.toProto()).isEqualTo(LensFacingProto.LENS_FACING_BACK)
        assertThat(LensFacingProto.LENS_FACING_BACK.toModel()).isEqualTo(LensFacing.BACK)

        assertThat(LensFacingProto.UNRECOGNIZED.toModel()).isEqualTo(LensFacing.BACK)
    }

    @Test
    fun lowLightBoostPriorityMapsCorrectly() {
        assertThat(
            LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES.toProto()
        ).isEqualTo(LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES)
        assertThat(
            LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_GOOGLE_PLAY_SERVICES.toModel()
        ).isEqualTo(LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES)

        assertThat(
            LowLightBoostPriority.PRIORITIZE_AE_MODE.toProto()
        ).isEqualTo(LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE)
        assertThat(
            LowLightBoostPriorityProto.LOW_LIGHT_BOOST_PRIORITY_AE_MODE.toModel()
        ).isEqualTo(LowLightBoostPriority.PRIORITIZE_AE_MODE)

        assertThat(
            LowLightBoostPriorityProto.UNRECOGNIZED.toModel()
        ).isEqualTo(LowLightBoostPriority.PRIORITIZE_AE_MODE)
    }

    @Test
    fun stabilizationModeMapsCorrectly() {
        assertThat(
            StabilizationMode.ON.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_ON)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_ON.toModel()
        ).isEqualTo(StabilizationMode.ON)

        assertThat(
            StabilizationMode.OFF.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_OFF)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_OFF.toModel()
        ).isEqualTo(StabilizationMode.OFF)

        assertThat(
            StabilizationMode.HIGH_QUALITY.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_HIGH_QUALITY.toModel()
        ).isEqualTo(StabilizationMode.HIGH_QUALITY)

        assertThat(
            StabilizationMode.OPTICAL.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_OPTICAL)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_OPTICAL.toModel()
        ).isEqualTo(StabilizationMode.OPTICAL)

        assertThat(
            StabilizationMode.AUTO.toProto()
        ).isEqualTo(StabilizationModeProto.STABILIZATION_MODE_AUTO)
        assertThat(
            StabilizationModeProto.STABILIZATION_MODE_AUTO.toModel()
        ).isEqualTo(StabilizationMode.AUTO)

        assertThat(StabilizationModeProto.UNRECOGNIZED.toModel()).isEqualTo(StabilizationMode.AUTO)
    }

    @Test
    fun videoQualityMapsCorrectly() {
        assertThat(VideoQuality.SD.toProto()).isEqualTo(VideoQualityProto.VIDEO_QUALITY_SD)
        assertThat(VideoQualityProto.VIDEO_QUALITY_SD.toModel()).isEqualTo(VideoQuality.SD)

        assertThat(VideoQuality.HD.toProto()).isEqualTo(VideoQualityProto.VIDEO_QUALITY_HD)
        assertThat(VideoQualityProto.VIDEO_QUALITY_HD.toModel()).isEqualTo(VideoQuality.HD)

        assertThat(VideoQuality.FHD.toProto()).isEqualTo(VideoQualityProto.VIDEO_QUALITY_FHD)
        assertThat(VideoQualityProto.VIDEO_QUALITY_FHD.toModel()).isEqualTo(VideoQuality.FHD)

        assertThat(VideoQuality.UHD.toProto()).isEqualTo(VideoQualityProto.VIDEO_QUALITY_UHD)
        assertThat(VideoQualityProto.VIDEO_QUALITY_UHD.toModel()).isEqualTo(VideoQuality.UHD)

        assertThat(
            VideoQuality.UNSPECIFIED.toProto()
        ).isEqualTo(VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED)
        assertThat(
            VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED.toModel()
        ).isEqualTo(VideoQuality.UNSPECIFIED)

        assertThat(VideoQualityProto.UNRECOGNIZED.toModel()).isEqualTo(VideoQuality.UNSPECIFIED)
    }

    @Test
    fun concurrentCameraModeMapsCorrectly() {
        assertThat(
            ConcurrentCameraMode.DUAL.toProto()
        ).isEqualTo(ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_DUAL)
        assertThat(
            ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_DUAL.toModel()
        ).isEqualTo(ConcurrentCameraMode.DUAL)

        assertThat(
            ConcurrentCameraMode.OFF.toProto()
        ).isEqualTo(ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_OFF)
        assertThat(
            ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_OFF.toModel()
        ).isEqualTo(ConcurrentCameraMode.OFF)

        assertThat(
            ConcurrentCameraModeProto.UNRECOGNIZED.toModel()
        ).isEqualTo(ConcurrentCameraMode.OFF)
    }
}
