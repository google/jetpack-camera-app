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
import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.TARGET_FPS_30
import com.google.jetpackcamera.model.TARGET_FPS_60
import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.model.VideoQuality
import org.junit.Assert.assertThrows
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
        dynamicRange = DynamicRange.HLG10,
        stabilizationMode = StabilizationMode.ON,
        targetFrameRate = TARGET_FPS_60
    )

    private val restrictivePolicy = CameraFeaturePolicy(
        aspectRatio = SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Hidden),
        flashMode = SettingConfig(FlashMode.OFF, OptionVisibility.Hidden),
        imageFormat = SettingConfig(ImageOutputFormat.JPEG, OptionVisibility.Hidden),
        dynamicRange = SettingConfig(DynamicRange.SDR, OptionVisibility.Hidden),
        stabilizationMode = SettingConfig(StabilizationMode.OFF, OptionVisibility.Hidden),
        targetFrameRate = SettingConfig(TARGET_FPS_AUTO, OptionVisibility.Hidden)
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
        assertThat(enforced.stabilizationMode).isEqualTo(StabilizationMode.OFF)
        assertThat(enforced.targetFrameRate).isEqualTo(TARGET_FPS_AUTO)

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
            dynamicRange = SettingConfig(DynamicRange.SDR, OptionVisibility.Hidden),
            stabilizationMode = SettingConfig(
                defaultValue = StabilizationMode.OFF,
                visibility = OptionVisibility.from(StabilizationMode.OFF, StabilizationMode.AUTO)
            ),
            targetFrameRate = SettingConfig(
                defaultValue = TARGET_FPS_AUTO,
                visibility = OptionVisibility.from(TARGET_FPS_AUTO, TARGET_FPS_30)
            )
        )
        val (enforced, overrides) = enforce(policy, userSettings)
        assertThat(enforced.flashMode).isEqualTo(FlashMode.OFF)
        assertThat(enforced.stabilizationMode).isEqualTo(StabilizationMode.OFF)
        assertThat(enforced.targetFrameRate).isEqualTo(TARGET_FPS_AUTO)

        // The user picks another permitted value while the policy applies.
        val restored = overrides.restore(
            enforced.copy(
                flashMode = FlashMode.AUTO,
                stabilizationMode = StabilizationMode.AUTO,
                targetFrameRate = TARGET_FPS_30
            )
        )

        assertThat(restored.flashMode).isEqualTo(FlashMode.AUTO)
        assertThat(restored.stabilizationMode).isEqualTo(StabilizationMode.AUTO)
        assertThat(restored.targetFrameRate).isEqualTo(TARGET_FPS_30)
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
    fun restore_returnsCameraEffectAndVideoQualityToTheirPreviousValues() {
        val effect = CameraEffectId("single_stream")
        val policy = CameraFeaturePolicy(
            cameraEffect = SettingConfig(NONE_EFFECT_ID, OptionVisibility.Hidden),
            videoQuality = SettingConfig(VideoQuality.UNSPECIFIED, OptionVisibility.Hidden)
        )
        val settings =
            userSettings.copy(selectedCameraEffect = effect, videoQuality = VideoQuality.UHD)
        val (enforced, overrides) = enforce(policy, settings)
        assertThat(enforced.selectedCameraEffect).isEqualTo(NONE_EFFECT_ID)
        assertThat(enforced.videoQuality).isEqualTo(VideoQuality.UNSPECIFIED)

        val restored = overrides.restore(enforced)

        assertThat(restored.selectedCameraEffect).isEqualTo(effect)
        assertThat(restored.videoQuality).isEqualTo(VideoQuality.UHD)
    }

    @Test
    fun withRequestedChange_whenUserPicksTheEnforcedValue_restoresTheUserChoice() {
        val (enforced, overrides) = enforce(restrictivePolicy, userSettings)
        assertThat(enforced.flashMode).isEqualTo(FlashMode.OFF)

        // The user picks the value the policy already set, so the setting doesn't change.
        val updated = overrides.withRequestedChange { it.copy(flashMode = FlashMode.OFF) }

        assertThat(updated.restore(enforced).flashMode).isEqualTo(FlashMode.OFF)
        assertThat(overrides.restore(enforced).flashMode).isEqualTo(FlashMode.ON)
    }

    @Test
    fun withRequestedChange_whenPolicyBlocksTheChoice_restoresTheUserChoice() {
        val policy = restrictivePolicy.copy(
            videoQuality = SettingConfig(VideoQuality.UNSPECIFIED, OptionVisibility.Hidden)
        )
        val settings = userSettings.copy(videoQuality = VideoQuality.UHD)
        val (enforced, overrides) = enforce(policy, settings)

        // The policy keeps the frame rate at auto and the quality unspecified, but the user's
        // choices return afterwards.
        val updated = overrides.withRequestedChange {
            it.copy(targetFrameRate = TARGET_FPS_30, videoQuality = VideoQuality.HD)
        }

        val restored = updated.restore(enforced)
        assertThat(restored.targetFrameRate).isEqualTo(TARGET_FPS_30)
        assertThat(restored.videoQuality).isEqualTo(VideoQuality.HD)
        assertThat(restored.aspectRatio).isEqualTo(userSettings.aspectRatio)
        assertThat(restored.selectedCameraEffect).isEqualTo(userSettings.selectedCameraEffect)
    }

    @Test
    fun withRequestedChange_whenOnlyUnrestoredSettingsChange_returnsAnEqualRecord() {
        val (_, overrides) = enforce(restrictivePolicy, userSettings)

        assertThat(overrides.withRequestedChange { it.copy(darkMode = DarkMode.LIGHT) })
            .isEqualTo(overrides)
    }

    @Test
    fun withRequestedChange_whenPolicyReplacesThePermittedEntryValue_restoresTheUserChoice() {
        // The policy permits the entry value (1:1), so it is kept on entry.
        val policy = CameraFeaturePolicy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.NINE_SIXTEEN,
                visibility = OptionVisibility.from(AspectRatio.NINE_SIXTEEN, AspectRatio.ONE_ONE)
            )
        )
        val (enforced, overrides) = enforce(policy, userSettings)
        assertThat(enforced.aspectRatio).isEqualTo(AspectRatio.ONE_ONE)

        // The user requests a blocked value, so the policy replaces it with its default.
        val requested = enforced.copy(aspectRatio = AspectRatio.THREE_FOUR)
        val reEnforced = policy.enforceRestrictions(requested)
        assertThat(reEnforced.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
        val updated =
            overrides.withRequestedChange { it.copy(aspectRatio = AspectRatio.THREE_FOUR) }

        assertThat(updated.restore(reEnforced).aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
    }

    @Test
    fun constructor_whenCaptureModeChanged_throwsException() {
        val policy = restrictivePolicy.copy(
            captureMode = SettingConfig(CaptureMode.VIDEO_ONLY, OptionVisibility.Hidden)
        )
        val enforced = policy.enforceRestrictions(userSettings)

        assertThrows(IllegalArgumentException::class.java) {
            PolicyOverrides(userSettings, enforced)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CameraSystemConstraints(
                captureSubModePolicies = mapOf(CaptureSubModeId("video") to policy)
            )
        }
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

    @Test
    fun subModeModelAndConstraints_preserveValuesAndDefaults() {
        val subModeId = CaptureSubModeId("night")
        assertThat(CaptureSubModeId.DEFAULT.value).isEqualTo("default")
        assertThat(subModeId.value).isEqualTo("night")

        val descriptor = CaptureSubModeDescriptor(
            id = subModeId,
            parentCaptureMode = CaptureMode.IMAGE_ONLY,
            labelResId = 42,
            sortOrder = 50,
            quickSettingsTitleResId = 99
        )
        val defaultDescriptor = CaptureSubModeDescriptor(
            id = subModeId,
            parentCaptureMode = CaptureMode.IMAGE_ONLY,
            labelResId = 42
        )
        assertThat(descriptor.id).isEqualTo(subModeId)
        assertThat(descriptor.parentCaptureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(descriptor.labelResId).isEqualTo(42)
        assertThat(descriptor.sortOrder).isEqualTo(50)
        assertThat(descriptor.quickSettingsTitleResId).isEqualTo(99)
        assertThat(defaultDescriptor.sortOrder).isEqualTo(100)
        assertThat(defaultDescriptor.quickSettingsTitleResId).isNull()

        val (_, overrides) = enforce(restrictivePolicy, userSettings)
        val settingsWithSubMode = DEFAULT_CAMERA_APP_SETTINGS.copy(
            captureSubModeId = subModeId,
            activeCaptureSubModeId = subModeId,
            captureSubModeOverrides = overrides
        )
        assertThat(settingsWithSubMode.captureSubModeId).isEqualTo(subModeId)
        assertThat(settingsWithSubMode.activeCaptureSubModeId).isEqualTo(subModeId)
        assertThat(settingsWithSubMode.captureSubModeOverrides).isEqualTo(overrides)

        val backConstraints = CameraConstraints(
            supportedStabilizationModes = emptySet(),
            supportedFixedFrameRates = emptySet(),
            supportedDynamicRanges = setOf(DynamicRange.SDR),
            supportedVideoQualitiesMap = emptyMap(),
            supportedImageFormatsMap = mapOf(
                true to setOf(ImageOutputFormat.JPEG),
                false to setOf(ImageOutputFormat.JPEG, ImageOutputFormat.JPEG_ULTRA_HDR)
            ),
            supportedIlluminants = emptySet(),
            supportedFlashModes = setOf(FlashMode.OFF),
            supportedZoomRange = null,
            unsupportedStabilizationFpsMap = emptyMap(),
            supportedTestPatterns = emptySet(),
            supportedCaptureSubModes = setOf(subModeId),
            defaultCaptureSubModes = mapOf(CaptureMode.IMAGE_ONLY to subModeId),
            supportedImageFormatsBySubMode = mapOf(
                subModeId to setOf(ImageOutputFormat.JPEG)
            )
        )
        val systemConstraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK),
            perLensConstraints = mapOf(LensFacing.BACK to backConstraints),
            captureSubModeDescriptors = mapOf(subModeId to descriptor),
            captureSubModePolicies = mapOf(subModeId to restrictivePolicy)
        )
        assertThat(systemConstraints.captureSubModeDescriptors).containsEntry(subModeId, descriptor)
        assertThat(systemConstraints.captureSubModePolicies)
            .containsEntry(subModeId, restrictivePolicy)
        assertThat(backConstraints.supportedCaptureSubModes).containsExactly(subModeId)
        assertThat(backConstraints.defaultCaptureSubModes)
            .containsEntry(CaptureMode.IMAGE_ONLY, subModeId)
        assertThat(backConstraints.supportedImageFormatsFor(subModeId, affectsImageCapture = false))
            .containsExactly(ImageOutputFormat.JPEG)
        assertThat(
            backConstraints.supportedImageFormatsFor(
                CaptureSubModeId.DEFAULT,
                affectsImageCapture = false
            )
        ).containsExactly(ImageOutputFormat.JPEG, ImageOutputFormat.JPEG_ULTRA_HDR)
        assertThat(backConstraints.supportedImageFormatsFor(subModeId, affectsImageCapture = true))
            .containsExactly(ImageOutputFormat.JPEG)

        assertThrows(IllegalArgumentException::class.java) {
            backConstraints.copy(
                defaultCaptureSubModes = mapOf(
                    CaptureMode.IMAGE_ONLY to CaptureSubModeId("unsupported")
                )
            )
        }
    }

    @Test
    fun supportedImageFormatsFor_withImageCaptureEffect_narrowsToSubModeFormats() {
        val subModeId = CaptureSubModeId("night")
        val constraints = CameraConstraints(
            supportedStabilizationModes = emptySet(),
            supportedFixedFrameRates = emptySet(),
            supportedDynamicRanges = setOf(DynamicRange.SDR),
            supportedVideoQualitiesMap = emptyMap(),
            supportedImageFormatsMap = mapOf(
                true to setOf(ImageOutputFormat.JPEG, ImageOutputFormat.JPEG_ULTRA_HDR),
                false to setOf(ImageOutputFormat.JPEG, ImageOutputFormat.JPEG_ULTRA_HDR)
            ),
            supportedIlluminants = emptySet(),
            supportedFlashModes = setOf(FlashMode.OFF),
            supportedZoomRange = null,
            unsupportedStabilizationFpsMap = emptyMap(),
            supportedTestPatterns = emptySet(),
            supportedCaptureSubModes = setOf(subModeId),
            supportedImageFormatsBySubMode = mapOf(subModeId to setOf(ImageOutputFormat.JPEG))
        )

        assertThat(constraints.supportedImageFormatsFor(subModeId, affectsImageCapture = true))
            .containsExactly(ImageOutputFormat.JPEG)
        assertThat(
            constraints.supportedImageFormatsFor(
                CaptureSubModeId.DEFAULT,
                affectsImageCapture = true
            )
        ).containsExactly(ImageOutputFormat.JPEG, ImageOutputFormat.JPEG_ULTRA_HDR)
    }
}
