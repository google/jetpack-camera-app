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

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.TARGET_FPS_60
import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.settings.model.CameraConstraints
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class StabilizationModeResolutionTest {

    @Test
    fun auto_standard_previewAndOpticalSupported_resolvesToOn() {
        val constraints = cameraConstraints(
            StabilizationMode.ON,
            StabilizationMode.OPTICAL,
            StabilizationMode.OFF
        )

        assertThat(resolve(StabilizationMode.AUTO, CaptureMode.STANDARD, constraints))
            .isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun auto_videoOnly_previewAndOpticalSupported_resolvesToOn() {
        val constraints = cameraConstraints(
            StabilizationMode.ON,
            StabilizationMode.OPTICAL,
            StabilizationMode.OFF
        )

        assertThat(resolve(StabilizationMode.AUTO, CaptureMode.VIDEO_ONLY, constraints))
            .isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun auto_standard_onlyOpticalSupported_resolvesToOptical() {
        val constraints = cameraConstraints(StabilizationMode.OPTICAL, StabilizationMode.OFF)

        assertThat(resolve(StabilizationMode.AUTO, CaptureMode.STANDARD, constraints))
            .isEqualTo(StabilizationMode.OPTICAL)
    }

    @Test
    fun auto_standard_noStabilizationSupported_resolvesToOff() {
        val constraints = cameraConstraints(StabilizationMode.OFF)

        assertThat(resolve(StabilizationMode.AUTO, CaptureMode.STANDARD, constraints))
            .isEqualTo(StabilizationMode.OFF)
    }

    @Test
    fun auto_standard_previewUnsupportedAtFrameRate_resolvesToOptical() {
        val constraints = cameraConstraints(
            StabilizationMode.ON,
            StabilizationMode.OPTICAL,
            StabilizationMode.OFF,
            unsupportedStabilizationFpsMap = mapOf(StabilizationMode.ON to setOf(TARGET_FPS_60))
        )

        assertThat(
            resolve(
                StabilizationMode.AUTO,
                CaptureMode.STANDARD,
                constraints,
                targetFrameRate = TARGET_FPS_60
            )
        ).isEqualTo(StabilizationMode.OPTICAL)
    }

    @Test
    fun auto_imageOnly_previewAndOpticalSupported_resolvesToOptical() {
        val constraints = cameraConstraints(
            StabilizationMode.ON,
            StabilizationMode.OPTICAL,
            StabilizationMode.OFF
        )

        assertThat(resolve(StabilizationMode.AUTO, CaptureMode.IMAGE_ONLY, constraints))
            .isEqualTo(StabilizationMode.OPTICAL)
    }

    @Test
    fun auto_imageOnly_onlyPreviewSupported_resolvesToOff() {
        val constraints = cameraConstraints(StabilizationMode.ON, StabilizationMode.OFF)

        assertThat(resolve(StabilizationMode.AUTO, CaptureMode.IMAGE_ONLY, constraints))
            .isEqualTo(StabilizationMode.OFF)
    }

    @Test
    fun auto_imageOnly_opticalUnsupportedAtFrameRate_resolvesToOff() {
        val constraints = cameraConstraints(
            StabilizationMode.ON,
            StabilizationMode.OPTICAL,
            StabilizationMode.OFF,
            unsupportedStabilizationFpsMap = mapOf(
                StabilizationMode.OPTICAL to setOf(TARGET_FPS_60)
            )
        )

        assertThat(
            resolve(
                StabilizationMode.AUTO,
                CaptureMode.IMAGE_ONLY,
                constraints,
                targetFrameRate = TARGET_FPS_60
            )
        ).isEqualTo(StabilizationMode.OFF)
    }

    @Test
    fun on_imageOnly_previewSupported_resolvesToOn() {
        val constraints = cameraConstraints(
            StabilizationMode.ON,
            StabilizationMode.OPTICAL,
            StabilizationMode.OFF
        )

        assertThat(resolve(StabilizationMode.ON, CaptureMode.IMAGE_ONLY, constraints))
            .isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun highQuality_imageOnly_supported_resolvesToHighQuality() {
        val constraints = cameraConstraints(StabilizationMode.HIGH_QUALITY, StabilizationMode.OFF)

        assertThat(resolve(StabilizationMode.HIGH_QUALITY, CaptureMode.IMAGE_ONLY, constraints))
            .isEqualTo(StabilizationMode.HIGH_QUALITY)
    }

    @Test
    fun explicitMode_unsupportedByLens_resolvesToOff() {
        val constraints = cameraConstraints(StabilizationMode.OPTICAL, StabilizationMode.OFF)

        assertThat(resolve(StabilizationMode.ON, CaptureMode.STANDARD, constraints))
            .isEqualTo(StabilizationMode.OFF)
    }

    @Test
    fun explicitMode_unsupportedAtFrameRate_resolvesToOff() {
        val constraints = cameraConstraints(
            StabilizationMode.ON,
            StabilizationMode.OFF,
            unsupportedStabilizationFpsMap = mapOf(StabilizationMode.ON to setOf(TARGET_FPS_60))
        )

        assertThat(
            resolve(
                StabilizationMode.ON,
                CaptureMode.STANDARD,
                constraints,
                targetFrameRate = TARGET_FPS_60
            )
        ).isEqualTo(StabilizationMode.OFF)
    }

    @Test
    fun off_resolvesToOff() {
        val constraints = cameraConstraints(
            StabilizationMode.ON,
            StabilizationMode.OPTICAL,
            StabilizationMode.OFF
        )

        assertThat(resolve(StabilizationMode.OFF, CaptureMode.STANDARD, constraints))
            .isEqualTo(StabilizationMode.OFF)
    }

    private fun resolve(
        requestedStabilizationMode: StabilizationMode,
        captureMode: CaptureMode,
        cameraConstraints: CameraConstraints,
        targetFrameRate: Int = TARGET_FPS_AUTO
    ): StabilizationMode = resolveStabilizationMode(
        requestedStabilizationMode = requestedStabilizationMode,
        captureMode = captureMode,
        targetFrameRate = targetFrameRate,
        cameraConstraints = cameraConstraints
    )

    private fun cameraConstraints(
        vararg supportedStabilizationModes: StabilizationMode,
        unsupportedStabilizationFpsMap: Map<StabilizationMode, Set<Int>> = emptyMap()
    ) = CameraConstraints(
        supportedStabilizationModes = supportedStabilizationModes.toSet(),
        supportedFixedFrameRates = emptySet(),
        supportedDynamicRanges = emptySet(),
        supportedVideoQualitiesMap = emptyMap(),
        supportedImageFormatsMap = emptyMap(),
        supportedIlluminants = emptySet(),
        supportedFlashModes = emptySet(),
        supportedZoomRange = null,
        unsupportedStabilizationFpsMap = unsupportedStabilizationFpsMap,
        supportedTestPatterns = emptySet()
    )
}
