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
package com.google.jetpackcamera.settings.model

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class PolicyOverridesTest {

    private val userSettings = CameraAppSettings(
        captureMode = CaptureMode.IMAGE_ONLY,
        aspectRatio = AspectRatio.ONE_ONE,
        flashMode = FlashMode.ON,
        imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
        dynamicRange = DynamicRange.HLG10
    )

    private val restrictivePolicy = CameraFeaturePolicy(
        aspectRatio = SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Hidden),
        flashMode = SettingConfig(FlashMode.OFF, OptionVisibility.Hidden),
        imageFormat = SettingConfig(ImageOutputFormat.JPEG, OptionVisibility.Hidden),
        dynamicRange = SettingConfig(DynamicRange.SDR, OptionVisibility.Hidden)
    )

    private fun enforce(
        policy: CameraFeaturePolicy,
        settings: CameraAppSettings
    ): Pair<CameraAppSettings, PolicyOverrides> {
        val enforced = policy.enforceRestrictions(settings)
        return enforced to PolicyOverrides(settings, enforced)
    }

    @Test
    fun restore_returnsOverriddenValuesToTheirPreviousValues() {
        val (enforced, overrides) = enforce(restrictivePolicy, userSettings)
        assertThat(enforced.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
        assertThat(enforced.flashMode).isEqualTo(FlashMode.OFF)
        assertThat(enforced.imageFormat).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(enforced.dynamicRange).isEqualTo(DynamicRange.SDR)

        val restored = overrides.restore(enforced)

        assertThat(restored).isEqualTo(userSettings)
    }

    @Test
    fun restore_keepsValuesChangedAfterEnforcement() {
        val policy = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.from(FlashMode.OFF, FlashMode.AUTO)
            ),
            dynamicRange = SettingConfig(DynamicRange.SDR, OptionVisibility.Hidden)
        )
        val (enforced, overrides) = enforce(policy, userSettings)
        assertThat(enforced.flashMode).isEqualTo(FlashMode.OFF)

        // The user picks another permitted value while the policy applies.
        val restored = overrides.restore(enforced.copy(flashMode = FlashMode.AUTO))

        assertThat(restored.flashMode).isEqualTo(FlashMode.AUTO)
        assertThat(restored.dynamicRange).isEqualTo(DynamicRange.HLG10)
    }

    @Test
    fun restore_leavesValuesThePolicyDidNotChange() {
        val policy = CameraFeaturePolicy(
            dynamicRange = SettingConfig(DynamicRange.SDR, OptionVisibility.Hidden)
        )
        val (enforced, overrides) = enforce(policy, userSettings)

        val restored = overrides.restore(enforced.copy(aspectRatio = AspectRatio.NINE_SIXTEEN))

        assertThat(restored.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
        assertThat(restored.flashMode).isEqualTo(FlashMode.ON)
        assertThat(restored.dynamicRange).isEqualTo(DynamicRange.HLG10)
    }

    @Test
    fun restore_doesNotRestoreCaptureMode() {
        val policy = restrictivePolicy.copy(
            captureMode = SettingConfig(CaptureMode.VIDEO_ONLY, OptionVisibility.Hidden)
        )
        val (enforced, overrides) = enforce(policy, userSettings)
        assertThat(enforced.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)

        val restored = overrides.restore(enforced)

        assertThat(restored.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(restored.dynamicRange).isEqualTo(DynamicRange.HLG10)
    }

    @Test
    fun recordsOfTheSameEnforcement_areEqual() {
        val (enforced, overrides) = enforce(restrictivePolicy, userSettings)

        val sameRecord = PolicyOverrides(userSettings, enforced)

        assertThat(sameRecord).isEqualTo(overrides)
        assertThat(sameRecord.hashCode()).isEqualTo(overrides.hashCode())
    }

    @Test
    fun recordsOfDifferentEnforcements_areNotEqual() {
        val (_, overrides) = enforce(restrictivePolicy, userSettings)
        val (_, otherOverrides) = enforce(
            restrictivePolicy,
            userSettings.copy(aspectRatio = AspectRatio.NINE_SIXTEEN)
        )

        assertThat(otherOverrides).isNotEqualTo(overrides)
    }
}
