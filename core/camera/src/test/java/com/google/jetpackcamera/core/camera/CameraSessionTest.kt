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
import android.util.Rational
import android.util.Size
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.VideoQuality
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraSessionTest {

    @Test
    fun isSensorOutputLandscape_landscapeSensor_returnsExpectedOrientation() {
        val landscapeSensorRect = Rect(0, 0, 4000, 3000)

        assertThat(isSensorOutputLandscape(0, landscapeSensorRect)).isTrue()
        assertThat(isSensorOutputLandscape(180, landscapeSensorRect)).isTrue()
        assertThat(isSensorOutputLandscape(90, landscapeSensorRect)).isFalse()
        assertThat(isSensorOutputLandscape(270, landscapeSensorRect)).isFalse()
    }

    @Test
    fun isSensorOutputLandscape_portraitSensor_returnsExpectedOrientation() {
        val portraitSensorRect = Rect(0, 0, 3000, 4000)

        assertThat(isSensorOutputLandscape(0, portraitSensorRect)).isFalse()
        assertThat(isSensorOutputLandscape(180, portraitSensorRect)).isFalse()
        assertThat(isSensorOutputLandscape(90, portraitSensorRect)).isTrue()
        assertThat(isSensorOutputLandscape(270, portraitSensorRect)).isTrue()
    }

    @Test
    fun getViewPortAspectRatio_landscapeSensorRotated90Or270_returnsPortraitAspectRatio() {
        val landscapeSensorRect = Rect(0, 0, 4000, 3000)

        for (rotationDegrees in listOf(90, 270)) {
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    landscapeSensorRect,
                    AspectRatio.THREE_FOUR
                )
            ).isEqualTo(Rational(3, 4))
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    landscapeSensorRect,
                    AspectRatio.NINE_SIXTEEN
                )
            ).isEqualTo(Rational(9, 16))
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    landscapeSensorRect,
                    AspectRatio.ONE_ONE
                )
            ).isEqualTo(Rational(1, 1))
        }
    }

    @Test
    fun getViewPortAspectRatio_landscapeSensorRotated0Or180_returnsLandscapeAspectRatio() {
        val landscapeSensorRect = Rect(0, 0, 4000, 3000)

        for (rotationDegrees in listOf(0, 180)) {
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    landscapeSensorRect,
                    AspectRatio.THREE_FOUR
                )
            ).isEqualTo(Rational(4, 3))
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    landscapeSensorRect,
                    AspectRatio.NINE_SIXTEEN
                )
            ).isEqualTo(Rational(16, 9))
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    landscapeSensorRect,
                    AspectRatio.ONE_ONE
                )
            ).isEqualTo(Rational(1, 1))
        }
    }

    @Test
    fun getViewPortAspectRatio_portraitSensorRotated0Or180_returnsPortraitAspectRatio() {
        val portraitSensorRect = Rect(0, 0, 3000, 4000)

        for (rotationDegrees in listOf(0, 180)) {
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    portraitSensorRect,
                    AspectRatio.THREE_FOUR
                )
            ).isEqualTo(Rational(3, 4))
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    portraitSensorRect,
                    AspectRatio.NINE_SIXTEEN
                )
            ).isEqualTo(Rational(9, 16))
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    portraitSensorRect,
                    AspectRatio.ONE_ONE
                )
            ).isEqualTo(Rational(1, 1))
        }
    }

    @Test
    fun getViewPortAspectRatio_portraitSensorRotated90Or270_returnsLandscapeAspectRatio() {
        val portraitSensorRect = Rect(0, 0, 3000, 4000)

        for (rotationDegrees in listOf(90, 270)) {
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    portraitSensorRect,
                    AspectRatio.THREE_FOUR
                )
            ).isEqualTo(Rational(4, 3))
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    portraitSensorRect,
                    AspectRatio.NINE_SIXTEEN
                )
            ).isEqualTo(Rational(16, 9))
            assertThat(
                getViewPortAspectRatio(
                    rotationDegrees,
                    portraitSensorRect,
                    AspectRatio.ONE_ONE
                )
            ).isEqualTo(Rational(1, 1))
        }
    }

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
}
