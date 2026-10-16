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
package com.google.jetpackcamera.core.camera.concurrent

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.TARGET_FPS_30
import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.OptionVisibility
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ConcurrentCameraSubModeProviderTest {

    private val provider = ConcurrentCameraSubModeProvider()

    @Test
    fun descriptor_hasExpectedProperties() {
        assertThat(ConcurrentCameraSubModeKey.id).isEqualTo(CONCURRENT_SUB_MODE_ID)
        assertThat(provider.descriptor.id).isEqualTo(CONCURRENT_SUB_MODE_ID)
        assertThat(provider.descriptor.parentCaptureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(provider.descriptor.quickSettingsTitleResId)
            .isEqualTo(R.string.quick_settings_title_concurrent_dual_settings)
    }

    @Test
    fun isCompatibleWith_videoOnlyAndStandardExternalMode_returnsTrue() {
        assertThat(isCompatible(CameraAppSettings(captureMode = CaptureMode.VIDEO_ONLY))).isTrue()
    }

    @Test
    fun isCompatibleWith_nonVideoOnlyCaptureMode_returnsFalse() {
        assertThat(isCompatible(CameraAppSettings(captureMode = CaptureMode.STANDARD))).isFalse()
        assertThat(isCompatible(CameraAppSettings(captureMode = CaptureMode.IMAGE_ONLY))).isFalse()
    }

    @Test
    fun isCompatibleWith_nonStandardExternalCaptureMode_returnsFalse() {
        assertThat(
            isCompatible(
                CameraAppSettings(captureMode = CaptureMode.VIDEO_ONLY),
                externalCaptureMode = ExternalCaptureMode.VideoCapture
            )
        ).isFalse()
    }

    /** A selected effect is turned off by the policy rather than preventing dual camera. */
    @Test
    fun isCompatibleWith_activeCameraEffect_returnsTrue() {
        val settings = CameraAppSettings(
            captureMode = CaptureMode.VIDEO_ONLY,
            selectedCameraEffect = CameraEffectId("custom_effect")
        )

        assertThat(isCompatible(settings)).isTrue()
    }

    @Test
    fun featurePolicy_enforcesExpectedConstraints() {
        val policy = provider.featurePolicy

        assertThat(policy.captureMode).isNull()

        assertThat(policy.dynamicRange?.defaultValue).isEqualTo(DynamicRange.SDR)
        assertThat(policy.dynamicRange?.visibility).isEqualTo(OptionVisibility.Hidden)

        assertThat(policy.flashMode?.defaultValue).isEqualTo(FlashMode.OFF)
        assertThat(policy.flashMode?.visibility).isEqualTo(
            OptionVisibility.from(
                FlashMode.OFF,
                FlashMode.ON,
                FlashMode.AUTO
            )
        )

        assertThat(policy.stabilizationMode?.defaultValue).isEqualTo(StabilizationMode.OFF)
        assertThat(policy.stabilizationMode?.visibility).isEqualTo(OptionVisibility.Hidden)

        assertThat(policy.targetFrameRate?.defaultValue).isEqualTo(TARGET_FPS_AUTO)
        assertThat(policy.targetFrameRate?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun featurePolicy_locksCameraEffectToNone() {
        val cameraEffect = provider.featurePolicy.cameraEffect

        assertThat(cameraEffect?.defaultValue).isEqualTo(NONE_EFFECT_ID)
        assertThat(cameraEffect?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    /**
     * Verifies that settings that used to prevent dual camera are accepted and then overridden
     * by the policy while dual camera is active.
     */
    @Test
    fun featurePolicy_overridesSettingsThatDualCameraDoesNotSupport() {
        val settings = CameraAppSettings(
            captureMode = CaptureMode.VIDEO_ONLY,
            dynamicRange = DynamicRange.HLG10,
            flashMode = FlashMode.LOW_LIGHT_BOOST,
            stabilizationMode = StabilizationMode.ON,
            targetFrameRate = TARGET_FPS_30,
            selectedCameraEffect = CameraEffectId("custom_effect")
        )

        assertThat(isCompatible(settings)).isTrue()

        val enforced = provider.featurePolicy.enforceRestrictions(settings)
        assertThat(enforced.dynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(enforced.flashMode).isEqualTo(FlashMode.OFF)
        assertThat(enforced.stabilizationMode).isEqualTo(StabilizationMode.OFF)
        assertThat(enforced.targetFrameRate).isEqualTo(TARGET_FPS_AUTO)
        assertThat(enforced.selectedCameraEffect).isEqualTo(NONE_EFFECT_ID)
    }

    @Test
    fun featurePolicy_locksVideoQualityToTheConcurrentSessionQuality() {
        val settings = CameraAppSettings(
            captureMode = CaptureMode.VIDEO_ONLY,
            videoQuality = VideoQuality.UHD
        )

        assertThat(isCompatible(settings)).isTrue()
        assertThat(provider.featurePolicy.enforceRestrictions(settings).videoQuality)
            .isEqualTo(VideoQuality.UNSPECIFIED)
    }

    private fun isCompatible(
        settings: CameraAppSettings,
        externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard
    ): Boolean = provider.isCompatibleWith(settings, CameraSystemConstraints(), externalCaptureMode)
}
