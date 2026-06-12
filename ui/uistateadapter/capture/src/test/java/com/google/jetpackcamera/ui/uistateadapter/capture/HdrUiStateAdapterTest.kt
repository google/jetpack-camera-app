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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class HdrUiStateAdapterTest {

    private val emptyCameraConstraints = CameraConstraints(
        supportedStabilizationModes = emptySet(),
        supportedFixedFrameRates = emptySet(),
        supportedDynamicRanges = emptySet(),
        supportedVideoQualitiesMap = emptyMap(),
        supportedImageFormatsMap = emptyMap(),
        supportedIlluminants = emptySet(),
        supportedFlashModes = emptySet(),
        supportedZoomRange = null,
        unsupportedStabilizationFpsMap = emptyMap(),
        supportedTestPatterns = emptySet()
    )

    private val defaultCameraAppSettings = CameraAppSettings()

    @Test
    fun from_standardMode_returnsUnavailable() {
        // Given in STANDARD capture mode
        val appSettings = defaultCameraAppSettings.copy(captureMode = CaptureMode.STANDARD)
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10),
                    supportedImageFormatsMap = mapOf(
                        appSettings.streamConfig to setOf(
                            ImageOutputFormat.JPEG,
                            ImageOutputFormat.JPEG_ULTRA_HDR
                        )
                    )
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is unavailable in Standard mode
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Unavailable::class.java)
    }

    @Test
    fun from_imageOnlyMode_hdrSupported_returnsAvailableWithRelevantSettings() {
        // Given in IMAGE_ONLY capture mode, with HDR image supported
        val appSettings = defaultCameraAppSettings.copy(
            captureMode = CaptureMode.IMAGE_ONLY,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
            dynamicRange = DynamicRange.HLG10
        )
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedImageFormatsMap = mapOf(
                        appSettings.streamConfig to setOf(
                            ImageOutputFormat.JPEG,
                            ImageOutputFormat.JPEG_ULTRA_HDR
                        )
                    )
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is available
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Available::class.java)
        val availableState = hdrUiState as HdrUiState.Available
        // Image format should be what is in settings
        assertThat(availableState.selectedImageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
        // Dynamic range should be forced to SDR because we are in IMAGE_ONLY
        assertThat(availableState.selectedDynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(availableState.isSupported).isTrue()
    }

    @Test
    fun from_imageOnlyMode_hdrNotSupported_returnsUnavailable() {
        // Given in IMAGE_ONLY capture mode, but HDR image NOT supported
        val appSettings = defaultCameraAppSettings.copy(captureMode = CaptureMode.IMAGE_ONLY)
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedImageFormatsMap = mapOf(
                        appSettings.streamConfig to setOf(ImageOutputFormat.JPEG)
                    )
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is unavailable
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Unavailable::class.java)
    }

    @Test
    fun from_imageOnlyMode_lowLightBoostOn_returnsAvailableWithIsSupportedFalse() {
        // Given in IMAGE_ONLY capture mode with Low Light Boost ON, even though Ultra HDR is supported
        val appSettings = defaultCameraAppSettings.copy(
            captureMode = CaptureMode.IMAGE_ONLY,
            flashMode = FlashMode.LOW_LIGHT_BOOST
        )
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedImageFormatsMap = mapOf(
                        appSettings.streamConfig to setOf(
                            ImageOutputFormat.JPEG,
                            ImageOutputFormat.JPEG_ULTRA_HDR
                        )
                    )
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is available but not supported because of the flash mode conflict
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Available::class.java)
        val availableState = hdrUiState as HdrUiState.Available
        assertThat(availableState.isSupported).isFalse()
    }

    @Test
    fun from_videoOnlyMode_hdrSupported_returnsAvailableWithRelevantSettings() {
        // Given in VIDEO_ONLY capture mode, with HDR video supported
        val appSettings = defaultCameraAppSettings.copy(
            captureMode = CaptureMode.VIDEO_ONLY,
            dynamicRange = DynamicRange.HLG10,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR
        )
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10)
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is available
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Available::class.java)
        val availableState = hdrUiState as HdrUiState.Available
        // Dynamic range should be what is in settings
        assertThat(availableState.selectedDynamicRange).isEqualTo(DynamicRange.HLG10)
        // Image format should be forced to JPEG because we are in VIDEO_ONLY
        assertThat(availableState.selectedImageFormat).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(availableState.isSupported).isTrue()
    }

    @Test
    fun from_videoOnlyMode_hdrNotSupported_returnsUnavailable() {
        // Given in VIDEO_ONLY capture mode, but HDR video NOT supported
        val appSettings = defaultCameraAppSettings.copy(captureMode = CaptureMode.VIDEO_ONLY)
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR)
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is unavailable
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Unavailable::class.java)
    }

    @Test
    fun from_videoOnlyMode_lowLightBoostOn_returnsAvailableWithIsSupportedFalse() {
        // Given in VIDEO_ONLY capture mode with Low Light Boost ON, even though HDR video is supported
        val appSettings = defaultCameraAppSettings.copy(
            captureMode = CaptureMode.VIDEO_ONLY,
            flashMode = FlashMode.LOW_LIGHT_BOOST
        )
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10)
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is available but not supported because of the flash mode conflict
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Available::class.java)
        val availableState = hdrUiState as HdrUiState.Available
        assertThat(availableState.isSupported).isFalse()
    }

    @Test
    fun from_videoOnlyMode_concurrentCameraOn_returnsAvailableWithIsSupportedFalse() {
        // Given in VIDEO_ONLY capture mode with Concurrent Camera ON, even though HDR video is supported
        val appSettings = defaultCameraAppSettings.copy(
            captureMode = CaptureMode.VIDEO_ONLY,
            concurrentCameraMode = ConcurrentCameraMode.DUAL
        )
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10)
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is available but not supported because of the concurrent camera conflict
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Available::class.java)
        val availableState = hdrUiState as HdrUiState.Available
        assertThat(availableState.isSupported).isFalse()
    }

    @Test
    fun from_imageOnlyMode_hdrUnsupportedOnCurrentLensOnly_returnsDisabled() {
        // Given in IMAGE_ONLY capture mode
        // Current lens (BACK) does NOT support HDR, but FRONT lens DOES support HDR
        val appSettings = defaultCameraAppSettings.copy(
            captureMode = CaptureMode.IMAGE_ONLY,
            cameraLensFacing = LensFacing.BACK
        )
        val systemConstraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK, LensFacing.FRONT),
            perLensConstraints = mapOf(
                LensFacing.BACK to emptyCameraConstraints.copy(
                    supportedImageFormatsMap = mapOf(
                        appSettings.streamConfig to setOf(ImageOutputFormat.JPEG)
                    )
                ),
                LensFacing.FRONT to emptyCameraConstraints.copy(
                    supportedImageFormatsMap = mapOf(
                        appSettings.streamConfig to setOf(
                            ImageOutputFormat.JPEG,
                            ImageOutputFormat.JPEG_ULTRA_HDR
                        )
                    )
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is available but not supported (disabled)
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Available::class.java)
        val availableState = hdrUiState as HdrUiState.Available
        assertThat(availableState.isSupported).isFalse()
    }

    @Test
    fun from_videoOnlyMode_hdrUnsupportedOnCurrentLensOnly_returnsDisabled() {
        // Given in VIDEO_ONLY capture mode
        // Current lens (BACK) does NOT support HDR, but FRONT lens DOES support HDR
        val appSettings = defaultCameraAppSettings.copy(
            captureMode = CaptureMode.VIDEO_ONLY,
            cameraLensFacing = LensFacing.BACK
        )
        val systemConstraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK, LensFacing.FRONT),
            perLensConstraints = mapOf(
                LensFacing.BACK to emptyCameraConstraints.copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR)
                ),
                LensFacing.FRONT to emptyCameraConstraints.copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10)
                )
            )
        )

        // When
        val hdrUiState =
            HdrUiState.from(appSettings, systemConstraints, ExternalCaptureMode.Standard)

        // Then HDR is available but not supported (disabled)
        assertThat(hdrUiState).isInstanceOf(HdrUiState.Available::class.java)
        val availableState = hdrUiState as HdrUiState.Available
        assertThat(availableState.isSupported).isFalse()
    }
}
