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
package com.google.jetpackcamera.settings

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.getSupportedMimeTypes
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ConstraintsTest {

    @Test
    fun getSupportedMimeTypes_returnsEmptyMap_whenNoLensesAvailable() {
        val constraints = CameraSystemConstraints(availableLenses = emptyList())
        val result = constraints.getSupportedMimeTypes()
        assertThat(result).isEmpty()
    }

    @Test
    fun getSupportedMimeTypes_returnsJpegAndMp4_forTypicalLens() {
        val constraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK),
            perLensConstraints = mapOf(
                LensFacing.BACK to createConstraints(
                    imageFormats = setOf(ImageOutputFormat.JPEG),
                    dynamicRanges = setOf(DynamicRange.SDR)
                )
            )
        )

        val result = constraints.getSupportedMimeTypes()

        assertThat(result[LensFacing.BACK]).containsExactly("image/jpeg", "video/mp4")
    }

    @Test
    fun getSupportedMimeTypes_onlyReturnsJpeg_whenNoDynamicRangesSupported() {
        val constraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK),
            perLensConstraints = mapOf(
                LensFacing.BACK to createConstraints(
                    imageFormats = setOf(ImageOutputFormat.JPEG),
                    dynamicRanges = emptySet()
                )
            )
        )

        val result = constraints.getSupportedMimeTypes()

        assertThat(result[LensFacing.BACK]).containsExactly("image/jpeg")
    }

    @Test
    fun getSupportedMimeTypes_onlyReturnsMp4_whenJpegNotSupported() {
        val constraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK),
            perLensConstraints = mapOf(
                LensFacing.BACK to createConstraints(
                    imageFormats = emptySet(),
                    dynamicRanges = setOf(DynamicRange.SDR)
                )
            )
        )

        val result = constraints.getSupportedMimeTypes()

        assertThat(result[LensFacing.BACK]).containsExactly("video/mp4")
    }

    @Test
    fun getSupportedMimeTypes_returnsEmptySet_whenNeitherSupported() {
        val constraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK),
            perLensConstraints = mapOf(
                LensFacing.BACK to createConstraints(
                    imageFormats = emptySet(),
                    dynamicRanges = emptySet()
                )
            )
        )

        val result = constraints.getSupportedMimeTypes()

        assertThat(result[LensFacing.BACK]).isEmpty()
    }

    @Test
    fun getSupportedMimeTypes_handlesMultipleLensesSeparately() {
        val constraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK, LensFacing.FRONT),
            perLensConstraints = mapOf(
                LensFacing.BACK to createConstraints(
                    imageFormats = setOf(ImageOutputFormat.JPEG),
                    dynamicRanges = setOf(DynamicRange.SDR)
                ),
                LensFacing.FRONT to createConstraints(
                    imageFormats = setOf(ImageOutputFormat.JPEG),
                    dynamicRanges = emptySet()
                )
            )
        )

        val result = constraints.getSupportedMimeTypes()

        assertThat(result[LensFacing.BACK]).containsExactly("image/jpeg", "video/mp4")
        assertThat(result[LensFacing.FRONT]).containsExactly("image/jpeg")
    }

    /**
     * Helper to create CameraConstraints with specific formats/ranges for testing.
     */
    private fun createConstraints(
        imageFormats: Set<ImageOutputFormat>,
        dynamicRanges: Set<DynamicRange>
    ): CameraConstraints {
        return CameraConstraints(
            supportedStabilizationModes = emptySet(),
            supportedFixedFrameRates = emptySet(),
            supportedDynamicRanges = dynamicRanges,
            supportedVideoQualitiesMap = emptyMap(),
            supportedImageFormatsMap = mapOf(StreamConfig.SINGLE_STREAM to imageFormats),
            supportedIlluminants = emptySet(),
            supportedFlashModes = emptySet(),
            supportedZoomRange = null,
            unsupportedStabilizationFpsMap = emptyMap(),
            supportedTestPatterns = emptySet(),
            supportedStreamConfigs = emptySet()
        )
    }
}
