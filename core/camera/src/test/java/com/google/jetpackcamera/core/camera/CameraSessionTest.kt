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

import android.util.Size
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.VideoQuality
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CameraSessionTest {

    @Test
    fun getVideoQualityFromResolution_landscapeResolutions_returnsExpectedQuality() {
        assertThat(getVideoQualityFromResolution(Size(640, 480))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(1280, 720))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1920, 1080))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(3840, 2160))).isEqualTo(VideoQuality.UHD)
    }

    @Test
    fun getVideoQualityFromResolution_portraitResolutions_returnsExpectedQuality() {
        assertThat(getVideoQualityFromResolution(Size(480, 640))).isEqualTo(VideoQuality.SD)
        assertThat(getVideoQualityFromResolution(Size(720, 1280))).isEqualTo(VideoQuality.HD)
        assertThat(getVideoQualityFromResolution(Size(1080, 1920))).isEqualTo(VideoQuality.FHD)
        assertThat(getVideoQualityFromResolution(Size(2160, 3840))).isEqualTo(VideoQuality.UHD)
    }

    @Test
    fun getVideoQualityFromResolution_nullResolution_returnsUnspecified() {
        assertThat(getVideoQualityFromResolution(null)).isEqualTo(VideoQuality.UNSPECIFIED)
    }
}
