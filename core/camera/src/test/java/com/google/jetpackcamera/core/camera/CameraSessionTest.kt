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
package com.google.jetpackcamera.core.camera

import android.graphics.Rect
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.util.Size
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.CaptureRequestOptions
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.Illuminant
import com.google.jetpackcamera.model.VideoQuality
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCamera2Interop::class)
@RunWith(RobolectricTestRunner::class)
class CameraSessionTest {

    @Test
    fun getVideoQualityFromResolution_landscapeResolutions_returnsExpectedQuality() {
        assertThat(getVideoQualityFromResolution(Size(640, 480))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(1280, 720))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1920, 1080))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(2560, 1440))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(3840, 2160))).isEqualTo(VideoQuality.UHD)
    }

    @Test
    fun getVideoQualityFromResolution_portraitResolutions_returnsExpectedQuality() {
        assertThat(getVideoQualityFromResolution(Size(480, 640))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(720, 1280))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1080, 1920))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(1440, 2560))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(2160, 3840))).isEqualTo(VideoQuality.UHD)
    }

    @Test
    fun getVideoQualityFromResolution_rangeBoundaries_returnsExpectedQuality() {
        assertThat(getVideoQualityFromResolution(Size(320, 240)))
            .isEqualTo(VideoQuality.UNSPECIFIED)
        assertThat(getVideoQualityFromResolution(Size(320, 241))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(1280, 719))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(1280, 720))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1920, 1079))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1920, 1080))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(3840, 2159))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(3840, 2160))).isEqualTo(VideoQuality.UHD)
        assertThat(getVideoQualityFromResolution(Size(7680, 4319))).isEqualTo(VideoQuality.UHD)
        assertThat(getVideoQualityFromResolution(Size(7680, 4320)))
            .isEqualTo(VideoQuality.UNSPECIFIED)
    }

    @Test
    fun getVideoQualityFromResolution_nullResolution_returnsUnspecified() {
        assertThat(getVideoQualityFromResolution(null)).isEqualTo(VideoQuality.UNSPECIFIED)
    }

    @Test
    fun cropRectDimensions_rotatedLandscapeCropRect_swapsToPortrait() {
        val landscapeCropRect = Rect(0, 0, 1280, 720)
        assertThat(getWidthFromCropRect(landscapeCropRect, rotationDegrees = 90)).isEqualTo(720)
        assertThat(getHeightFromCropRect(landscapeCropRect, rotationDegrees = 90)).isEqualTo(1280)
        assertThat(getWidthFromCropRect(landscapeCropRect, rotationDegrees = 270)).isEqualTo(720)
        assertThat(getHeightFromCropRect(landscapeCropRect, rotationDegrees = 270)).isEqualTo(1280)
    }

    @Test
    fun cropRectDimensions_unrotatedPortraitCropRect_preservesPortrait() {
        val portraitCropRect = Rect(0, 0, 720, 1280)
        assertThat(getWidthFromCropRect(portraitCropRect, rotationDegrees = 0)).isEqualTo(720)
        assertThat(getHeightFromCropRect(portraitCropRect, rotationDegrees = 0)).isEqualTo(1280)
        assertThat(getWidthFromCropRect(portraitCropRect, rotationDegrees = 180)).isEqualTo(720)
        assertThat(getHeightFromCropRect(portraitCropRect, rotationDegrees = 180)).isEqualTo(1280)
    }

    @Test
    fun cropRectDimensions_unrotatedLandscapeCropRect_preservesLandscape() {
        val landscapeCropRect = Rect(0, 0, 1280, 720)
        assertThat(getWidthFromCropRect(landscapeCropRect, rotationDegrees = 0)).isEqualTo(1280)
        assertThat(getHeightFromCropRect(landscapeCropRect, rotationDegrees = 0)).isEqualTo(720)
        assertThat(getWidthFromCropRect(landscapeCropRect, rotationDegrees = 180)).isEqualTo(1280)
        assertThat(getHeightFromCropRect(landscapeCropRect, rotationDegrees = 180)).isEqualTo(720)
    }

    @Test
    fun cropRectDimensions_nullCropRect_returnsZero() {
        assertThat(getWidthFromCropRect(null, rotationDegrees = 0)).isEqualTo(0)
        assertThat(getHeightFromCropRect(null, rotationDegrees = 0)).isEqualTo(0)
        assertThat(getWidthFromCropRect(null, rotationDegrees = 90)).isEqualTo(0)
        assertThat(getHeightFromCropRect(null, rotationDegrees = 90)).isEqualTo(0)
    }

    @Test
    fun updateLowLightBoostAeMode_lowLightBoostWithAeModeSupport_setsLowLightBoostAeMode() {
        val builder = CaptureRequestOptions.Builder()

        builder.updateLowLightBoostAeMode(
            flashMode = FlashMode.LOW_LIGHT_BOOST,
            supportedIlluminants = setOf(Illuminant.LOW_LIGHT_BOOST_AE_MODE)
        )

        assertThat(builder.build().getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY)
    }

    @Test
    fun updateLowLightBoostAeMode_lowLightBoostWithCameraEffect_doesNotSetAeMode() {
        val builder = CaptureRequestOptions.Builder()

        builder.updateLowLightBoostAeMode(
            flashMode = FlashMode.LOW_LIGHT_BOOST,
            supportedIlluminants = setOf(Illuminant.LOW_LIGHT_BOOST_CAMERA_EFFECT)
        )

        assertThat(builder.build().getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE))
            .isNull()
    }

    @Test
    fun updateLowLightBoostAeMode_otherFlashMode_clearsAeMode() {
        val builder = CaptureRequestOptions.Builder()
        builder.updateLowLightBoostAeMode(
            flashMode = FlashMode.LOW_LIGHT_BOOST,
            supportedIlluminants = setOf(Illuminant.LOW_LIGHT_BOOST_AE_MODE)
        )

        builder.updateLowLightBoostAeMode(
            flashMode = FlashMode.OFF,
            supportedIlluminants = setOf(Illuminant.LOW_LIGHT_BOOST_AE_MODE)
        )

        assertThat(builder.build().getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE))
            .isNull()
    }

    @Test
    fun updateLowLightBoostAeMode_keepsOtherOptions() {
        val builder = CaptureRequestOptions.Builder().setCaptureRequestOption(
            CaptureRequest.SENSOR_TEST_PATTERN_MODE,
            CameraMetadata.SENSOR_TEST_PATTERN_MODE_COLOR_BARS
        )

        builder.updateLowLightBoostAeMode(
            flashMode = FlashMode.LOW_LIGHT_BOOST,
            supportedIlluminants = setOf(Illuminant.LOW_LIGHT_BOOST_AE_MODE)
        )

        val options = builder.build()
        assertThat(options.getCaptureRequestOption(CaptureRequest.SENSOR_TEST_PATTERN_MODE))
            .isEqualTo(CameraMetadata.SENSOR_TEST_PATTERN_MODE_COLOR_BARS)
        assertThat(options.getCaptureRequestOption(CaptureRequest.CONTROL_AE_MODE))
            .isEqualTo(CameraMetadata.CONTROL_AE_MODE_ON_LOW_LIGHT_BOOST_BRIGHTNESS_PRIORITY)
    }
}
