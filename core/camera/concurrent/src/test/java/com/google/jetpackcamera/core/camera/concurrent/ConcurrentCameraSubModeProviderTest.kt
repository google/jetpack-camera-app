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
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.OptionVisibility
import org.junit.Test

class ConcurrentCameraSubModeProviderTest {

    private val provider = ConcurrentCameraSubModeProvider()

    @Test
    fun descriptor_hasExpectedProperties() {
        assertThat(provider.descriptor.id).isEqualTo(CONCURRENT_SUB_MODE_ID)
        assertThat(provider.descriptor.parentCaptureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(provider.descriptor.quickSettingsTitleResId)
            .isEqualTo(R.string.quick_settings_title_concurrent_dual_settings)
    }

    @Test
    fun isCompatibleWith_videoOnlyAndStandardExternalMode_returnsTrue() {
        val settings = CameraAppSettings(captureMode = CaptureMode.VIDEO_ONLY)
        val constraints = CameraSystemConstraints()

        assertThat(
            provider.isCompatibleWith(
                settings = settings,
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isTrue()
    }

    @Test
    fun isCompatibleWith_nonVideoOnlyCaptureMode_returnsFalse() {
        val constraints = CameraSystemConstraints()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.STANDARD),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isFalse()

        assertThat(
            provider.isCompatibleWith(
                settings = CameraAppSettings(captureMode = CaptureMode.IMAGE_ONLY),
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.Standard
            )
        ).isFalse()
    }

    @Test
    fun isCompatibleWith_nonStandardExternalCaptureMode_returnsFalse() {
        val settings = CameraAppSettings(captureMode = CaptureMode.VIDEO_ONLY)
        val constraints = CameraSystemConstraints()

        assertThat(
            provider.isCompatibleWith(
                settings = settings,
                systemConstraints = constraints,
                externalCaptureMode = ExternalCaptureMode.VideoCapture
            )
        ).isFalse()
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
    }
}
