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
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.TARGET_FPS_15
import com.google.jetpackcamera.model.TARGET_FPS_30
import com.google.jetpackcamera.model.TARGET_FPS_60
import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.model.VideoQuality
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CameraFeaturePolicyTest {

    @Test
    fun settingConfig_whenOptionVisibilityOnlyMissingDefaultValue_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Only(
                    setOf(FlashMode.ON, FlashMode.AUTO)
                )
            )
        }
    }

    @Test
    fun settingConfig_whenOptionVisibilityOnlyHasSingleOption_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Only(
                    setOf(FlashMode.OFF)
                )
            )
        }
    }

    @Test
    fun optionVisibilityOnly_whenLessThanTwoOptions_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OptionVisibility.Only<FlashMode>(emptySet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            OptionVisibility.Only(setOf(FlashMode.OFF))
        }
    }

    @Test
    fun optionVisibility_from_withTwoOrMoreOptions_returnsOnly() {
        val visibility = OptionVisibility.from(FlashMode.OFF, FlashMode.ON)
        assertThat(visibility).isEqualTo(
            OptionVisibility.Only(setOf(FlashMode.OFF, FlashMode.ON))
        )
    }

    @Test
    fun optionVisibility_from_withSingleOption_returnsHidden() {
        val visibility = OptionVisibility.from(FlashMode.OFF)
        assertThat(visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun optionVisibility_from_withEmptySet_returnsHidden() {
        val visibility = OptionVisibility.from<FlashMode>(emptySet())
        assertThat(visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun cameraFeaturePolicy_defaultConstructor_allPropertiesNull() {
        val config = CameraFeaturePolicy()
        assertThat(config.aspectRatio).isNull()
        assertThat(config.flashMode).isNull()
        assertThat(config.captureMode).isNull()
        assertThat(config.imageFormat).isNull()
        assertThat(config.dynamicRange).isNull()
        assertThat(config.stabilizationMode).isNull()
        assertThat(config.targetFrameRate).isNull()
    }

    @Test
    fun cameraFeaturePolicy_whenFlashModeExcludesOff_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            CameraFeaturePolicy(
                flashMode = SettingConfig(
                    defaultValue = FlashMode.ON,
                    visibility = OptionVisibility.Only(
                        setOf(FlashMode.ON, FlashMode.AUTO)
                    )
                )
            )
        }
    }

    @Test
    fun cameraFeaturePolicy_whenFlashModeHiddenAndNotOff_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            CameraFeaturePolicy(
                flashMode = SettingConfig(
                    defaultValue = FlashMode.ON,
                    visibility = OptionVisibility.Hidden
                )
            )
        }
    }

    @Test
    fun cameraFeaturePolicy_whenFlashModeHiddenAndOff_succeeds() {
        val config = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Hidden
            )
        )
        assertThat(config.flashMode?.defaultValue).isEqualTo(FlashMode.OFF)
        assertThat(config.flashMode?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun cameraFeaturePolicy_whenImageFormatHiddenAndNotJpeg_succeeds() {
        val config = CameraFeaturePolicy(
            imageFormat = SettingConfig(
                defaultValue = ImageOutputFormat.JPEG_ULTRA_HDR,
                visibility = OptionVisibility.Hidden
            )
        )
        assertThat(config.imageFormat?.defaultValue).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
        assertThat(config.imageFormat?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun cameraFeaturePolicy_whenImageFormatHiddenAndJpeg_succeeds() {
        val config = CameraFeaturePolicy(
            imageFormat = SettingConfig(
                defaultValue = ImageOutputFormat.JPEG,
                visibility = OptionVisibility.Hidden
            )
        )
        assertThat(config.imageFormat?.defaultValue).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(config.imageFormat?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun cameraFeaturePolicy_whenDynamicRangeHiddenAndNotSdr_succeeds() {
        val config = CameraFeaturePolicy(
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.HLG10,
                visibility = OptionVisibility.Hidden
            )
        )
        assertThat(config.dynamicRange?.defaultValue).isEqualTo(DynamicRange.HLG10)
        assertThat(config.dynamicRange?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun cameraFeaturePolicy_whenDynamicRangeHiddenAndSdr_succeeds() {
        val config = CameraFeaturePolicy(
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.SDR,
                visibility = OptionVisibility.Hidden
            )
        )
        assertThat(config.dynamicRange?.defaultValue).isEqualTo(DynamicRange.SDR)
        assertThat(config.dynamicRange?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun settingConfig_structuralEquality_matches() {
        val config1 = SettingConfig(FlashMode.OFF, OptionVisibility.Visible)
        val config2 = SettingConfig(FlashMode.OFF, OptionVisibility.Visible)
        assertThat(config1).isEqualTo(config2)

        val policy1 = CameraFeaturePolicy()
        val policy2 = CameraFeaturePolicy()
        assertThat(policy1).isEqualTo(policy2)
    }

    @Test
    fun toCameraAppSettings_overridesDefaults() {
        val developerConfig = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.THREE_FOUR),
            flashMode = SettingConfig(FlashMode.ON),
            captureMode = SettingConfig(CaptureMode.VIDEO_ONLY),
            imageFormat = SettingConfig(ImageOutputFormat.JPEG_ULTRA_HDR),
            dynamicRange = SettingConfig(DynamicRange.HLG10),
            stabilizationMode = SettingConfig(StabilizationMode.OFF),
            targetFrameRate = SettingConfig(TARGET_FPS_30)
        )

        val appSettings = developerConfig.toCameraAppSettings()

        assertThat(appSettings.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
        assertThat(appSettings.flashMode).isEqualTo(FlashMode.ON)
        assertThat(appSettings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(appSettings.imageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
        assertThat(appSettings.dynamicRange).isEqualTo(DynamicRange.HLG10)
        assertThat(appSettings.stabilizationMode).isEqualTo(StabilizationMode.OFF)
        assertThat(appSettings.targetFrameRate).isEqualTo(TARGET_FPS_30)
    }

    @Test
    fun toCameraAppSettings_withCustomDefaults_preservesUnoverriddenSettings() {
        val customDefaults = DEFAULT_CAMERA_APP_SETTINGS.copy(
            aspectRatio = AspectRatio.ONE_ONE,
            flashMode = FlashMode.AUTO,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
            dynamicRange = DynamicRange.HLG10,
            maxVideoDurationMillis = 60_000L
        )
        val developerConfig = CameraFeaturePolicy(
            captureMode = SettingConfig(CaptureMode.VIDEO_ONLY)
        )

        val appSettings = developerConfig.toCameraAppSettings(customDefaults)

        assertThat(appSettings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(appSettings.aspectRatio).isEqualTo(AspectRatio.ONE_ONE)
        assertThat(appSettings.flashMode).isEqualTo(FlashMode.AUTO)
        assertThat(appSettings.imageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
        assertThat(appSettings.dynamicRange).isEqualTo(DynamicRange.HLG10)
        assertThat(appSettings.maxVideoDurationMillis).isEqualTo(60_000L)
    }

    @Test
    fun cameraFeaturePolicy_whenFlashModeOnlyIncludesOff_succeeds() {
        val config = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Only(FlashMode.OFF, FlashMode.ON)
            )
        )
        assertThat(config.flashMode?.defaultValue).isEqualTo(FlashMode.OFF)
        assertThat(config.flashMode?.visibility).isEqualTo(
            OptionVisibility.Only(setOf(FlashMode.OFF, FlashMode.ON))
        )
    }

    @Test
    fun toCameraAppSettings_withDefaultConfig_returnsDefaultSettings() {
        val config = CameraFeaturePolicy()
        assertThat(config.toCameraAppSettings()).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS)
    }

    @Test
    fun enforceRestrictions_whenSettingViolatesHidden_clampsToDefaultValue() {
        val policy = CameraFeaturePolicy(
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.SDR,
                visibility = OptionVisibility.Hidden
            ),
            imageFormat = SettingConfig(
                defaultValue = ImageOutputFormat.JPEG,
                visibility = OptionVisibility.Hidden
            ),
            stabilizationMode = SettingConfig(
                defaultValue = StabilizationMode.OFF,
                visibility = OptionVisibility.Hidden
            ),
            targetFrameRate = SettingConfig(
                defaultValue = TARGET_FPS_15,
                visibility = OptionVisibility.Hidden
            )
        )
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(
            dynamicRange = DynamicRange.HLG10,
            imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
            stabilizationMode = StabilizationMode.ON,
            targetFrameRate = TARGET_FPS_60,
            aspectRatio = AspectRatio.ONE_ONE
        )

        val enforced = policy.enforceRestrictions(settings)

        assertThat(enforced.dynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(enforced.imageFormat).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(enforced.stabilizationMode).isEqualTo(StabilizationMode.OFF)
        assertThat(enforced.targetFrameRate).isEqualTo(TARGET_FPS_15)
        assertThat(enforced.aspectRatio).isEqualTo(AspectRatio.ONE_ONE)
    }

    @Test
    fun enforceRestrictions_whenSettingPermittedByOnly_preservesValue() {
        val policy = CameraFeaturePolicy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.THREE_FOUR,
                visibility = OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.NINE_SIXTEEN)
            )
        )
        val permitted = DEFAULT_CAMERA_APP_SETTINGS.copy(aspectRatio = AspectRatio.NINE_SIXTEEN)
        val disallowed = DEFAULT_CAMERA_APP_SETTINGS.copy(aspectRatio = AspectRatio.ONE_ONE)

        assertThat(policy.enforceRestrictions(permitted).aspectRatio)
            .isEqualTo(AspectRatio.NINE_SIXTEEN)
        assertThat(policy.enforceRestrictions(disallowed).aspectRatio)
            .isEqualTo(AspectRatio.THREE_FOUR)
    }

    @Test
    fun isCompatibleWith_whenDisjointConstraints_returnsFalse() {
        val hostPolicy = CameraFeaturePolicy(
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.HLG10,
                visibility = OptionVisibility.Hidden
            )
        )
        val subModePolicy = CameraFeaturePolicy(
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.SDR,
                visibility = OptionVisibility.Hidden
            )
        )

        assertThat(hostPolicy.isCompatibleWith(subModePolicy)).isFalse()
    }

    @Test
    fun isCompatibleWith_whenOverlappingConstraints_returnsTrue() {
        val hostPolicy = CameraFeaturePolicy(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.IMAGE_ONLY,
                visibility = OptionVisibility.Only(CaptureMode.IMAGE_ONLY, CaptureMode.VIDEO_ONLY)
            )
        )
        val subModePolicy = CameraFeaturePolicy(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.IMAGE_ONLY,
                visibility = OptionVisibility.Hidden
            ),
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.SDR,
                visibility = OptionVisibility.Hidden
            )
        )

        assertThat(hostPolicy.isCompatibleWith(subModePolicy)).isTrue()
    }

    @Test
    fun intersect_narrowsVisibilityToCommonAllowedOptions() {
        val hostPolicy = CameraFeaturePolicy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.THREE_FOUR,
                visibility = OptionVisibility.Only(
                    AspectRatio.THREE_FOUR,
                    AspectRatio.NINE_SIXTEEN,
                    AspectRatio.ONE_ONE
                )
            )
        )
        val subModePolicy = CameraFeaturePolicy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.NINE_SIXTEEN,
                visibility = OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.NINE_SIXTEEN)
            ),
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.SDR,
                visibility = OptionVisibility.Hidden
            )
        )

        val intersected = hostPolicy.intersect(subModePolicy)

        assertThat(intersected.aspectRatio?.defaultValue).isEqualTo(AspectRatio.NINE_SIXTEEN)
        assertThat(intersected.aspectRatio?.visibility).isEqualTo(
            OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.NINE_SIXTEEN)
        )
        assertThat(intersected.dynamicRange?.defaultValue).isEqualTo(DynamicRange.SDR)
        assertThat(intersected.dynamicRange?.visibility).isEqualTo(OptionVisibility.Hidden)
    }

    @Test
    fun permits_followsVisibility() {
        val visible = SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Visible)
        val hidden = SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Hidden)
        val only = SettingConfig(
            defaultValue = AspectRatio.THREE_FOUR,
            visibility = OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.NINE_SIXTEEN)
        )

        assertThat(visible.permits(AspectRatio.ONE_ONE)).isTrue()
        assertThat(hidden.permits(AspectRatio.THREE_FOUR)).isTrue()
        assertThat(hidden.permits(AspectRatio.ONE_ONE)).isFalse()
        assertThat(only.permits(AspectRatio.NINE_SIXTEEN)).isTrue()
        assertThat(only.permits(AspectRatio.ONE_ONE)).isFalse()
    }

    @Test
    fun isCompatibleWith_whenHostVisibleOrOptionsOverlap_returnsTrue() {
        val hostOnly = CameraFeaturePolicy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.THREE_FOUR,
                visibility = OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.NINE_SIXTEEN)
            )
        )
        val hostVisible = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.NINE_SIXTEEN, OptionVisibility.Visible)
        )
        val subModeHidden = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Hidden)
        )
        val subModeVisible = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Visible)
        )
        val subModeOnly = CameraFeaturePolicy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.NINE_SIXTEEN,
                visibility = OptionVisibility.Only(AspectRatio.NINE_SIXTEEN, AspectRatio.ONE_ONE)
            )
        )

        assertThat(hostVisible.isCompatibleWith(subModeHidden)).isTrue()
        assertThat(hostOnly.isCompatibleWith(subModeVisible)).isTrue()
        assertThat(hostOnly.isCompatibleWith(subModeOnly)).isTrue()
    }

    @Test
    fun intersect_whenSubModeSettingUnset_keepsHostSetting() {
        val hostAspectRatio = SettingConfig(
            defaultValue = AspectRatio.THREE_FOUR,
            visibility = OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.NINE_SIXTEEN)
        )

        val intersected = CameraFeaturePolicy(aspectRatio = hostAspectRatio)
            .intersect(CameraFeaturePolicy())

        assertThat(intersected.aspectRatio).isEqualTo(hostAspectRatio)
    }

    @Test
    fun intersect_whenSubModeHidden_locksToSubModeDefault() {
        val subModePolicy = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Hidden)
        )
        val expected = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Hidden)

        val sameDefault = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Visible)
        ).intersect(subModePolicy)
        val differentDefault = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.NINE_SIXTEEN, OptionVisibility.Visible)
        ).intersect(subModePolicy)

        assertThat(sameDefault.aspectRatio).isEqualTo(expected)
        assertThat(differentDefault.aspectRatio).isEqualTo(expected)
    }

    @Test
    fun intersect_whenSubModeVisible_keepsHostRestrictionOrUsesSubModeDefault() {
        val subModePolicy = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Visible)
        )
        val hostHidden = SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Hidden)
        val hostOnly = SettingConfig(
            defaultValue = AspectRatio.THREE_FOUR,
            visibility = OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.NINE_SIXTEEN)
        )
        val hostOnlyPermittingSubModeDefault = SettingConfig(
            defaultValue = AspectRatio.THREE_FOUR,
            visibility = OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.ONE_ONE)
        )
        val hostVisible = SettingConfig(AspectRatio.NINE_SIXTEEN, OptionVisibility.Visible)

        assertThat(
            CameraFeaturePolicy(aspectRatio = hostHidden).intersect(subModePolicy).aspectRatio
        ).isEqualTo(hostHidden)
        assertThat(
            CameraFeaturePolicy(aspectRatio = hostOnly).intersect(subModePolicy).aspectRatio
        ).isEqualTo(hostOnly)
        assertThat(
            CameraFeaturePolicy(aspectRatio = hostOnlyPermittingSubModeDefault)
                .intersect(subModePolicy).aspectRatio
        ).isEqualTo(hostOnlyPermittingSubModeDefault.copy(defaultValue = AspectRatio.ONE_ONE))
        assertThat(
            CameraFeaturePolicy(aspectRatio = hostVisible).intersect(subModePolicy).aspectRatio
        ).isEqualTo(SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Visible))
    }

    @Test
    fun intersect_whenSubModeOnlyAndHostHiddenOrVisible_narrowsHostSetting() {
        val subModeAspectRatio = SettingConfig(
            defaultValue = AspectRatio.NINE_SIXTEEN,
            visibility = OptionVisibility.Only(AspectRatio.NINE_SIXTEEN, AspectRatio.ONE_ONE)
        )
        val subModePolicy = CameraFeaturePolicy(aspectRatio = subModeAspectRatio)

        val hostHiddenPermitted = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Hidden)
        ).intersect(subModePolicy)
        val hostHiddenNotPermitted = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Hidden)
        ).intersect(subModePolicy)
        val hostVisible = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Visible)
        ).intersect(subModePolicy)

        assertThat(hostHiddenPermitted.aspectRatio)
            .isEqualTo(SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Hidden))
        assertThat(hostHiddenNotPermitted.aspectRatio)
            .isEqualTo(SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Hidden))
        assertThat(hostVisible.aspectRatio).isEqualTo(subModeAspectRatio)
    }

    @Test
    fun intersect_whenBothOnlyAndSubModeDefaultNotCommon_fallsBackWithinCommonOptions() {
        val hostPolicy = CameraFeaturePolicy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.THREE_FOUR,
                visibility = OptionVisibility.Only(AspectRatio.THREE_FOUR, AspectRatio.NINE_SIXTEEN)
            )
        )

        // The host default is the only common option.
        val hostDefaultCommon = hostPolicy.intersect(
            CameraFeaturePolicy(
                aspectRatio = SettingConfig(
                    defaultValue = AspectRatio.ONE_ONE,
                    visibility = OptionVisibility.Only(AspectRatio.ONE_ONE, AspectRatio.THREE_FOUR)
                )
            )
        )
        // Neither default is a common option.
        val neitherDefaultCommon = hostPolicy.intersect(
            CameraFeaturePolicy(
                aspectRatio = SettingConfig(
                    defaultValue = AspectRatio.ONE_ONE,
                    visibility = OptionVisibility.Only(
                        AspectRatio.ONE_ONE,
                        AspectRatio.NINE_SIXTEEN
                    )
                )
            )
        )

        assertThat(hostDefaultCommon.aspectRatio)
            .isEqualTo(SettingConfig(AspectRatio.THREE_FOUR, OptionVisibility.Hidden))
        assertThat(neitherDefaultCommon.aspectRatio)
            .isEqualTo(SettingConfig(AspectRatio.NINE_SIXTEEN, OptionVisibility.Hidden))
    }

    @Test
    fun toCameraAppSettings_appliesCameraEffectDefault() {
        val effect = CameraEffectId("single_stream")
        val policy = CameraFeaturePolicy(cameraEffect = SettingConfig(defaultValue = effect))

        assertThat(policy.toCameraAppSettings().selectedCameraEffect).isEqualTo(effect)
        assertThat(CameraFeaturePolicy().toCameraAppSettings().selectedCameraEffect)
            .isEqualTo(NONE_EFFECT_ID)
    }

    @Test
    fun enforceRestrictions_whenCameraEffectHidden_clampsToDefaultValue() {
        val policy = CameraFeaturePolicy(
            cameraEffect = SettingConfig(NONE_EFFECT_ID, OptionVisibility.Hidden)
        )
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(
            selectedCameraEffect = CameraEffectId("single_stream")
        )

        assertThat(policy.enforceRestrictions(settings).selectedCameraEffect)
            .isEqualTo(NONE_EFFECT_ID)
    }

    @Test
    fun isCompatibleWith_comparesStabilizationFrameRateAndCameraEffect() {
        val subModePolicy = CameraFeaturePolicy(
            stabilizationMode = SettingConfig(StabilizationMode.OFF, OptionVisibility.Hidden),
            targetFrameRate = SettingConfig(TARGET_FPS_AUTO, OptionVisibility.Hidden),
            cameraEffect = SettingConfig(NONE_EFFECT_ID, OptionVisibility.Hidden)
        )
        val hostPermittingSubMode = CameraFeaturePolicy(
            stabilizationMode = SettingConfig(
                defaultValue = StabilizationMode.AUTO,
                visibility = OptionVisibility.Only(StabilizationMode.AUTO, StabilizationMode.OFF)
            ),
            targetFrameRate = SettingConfig(
                defaultValue = TARGET_FPS_30,
                visibility = OptionVisibility.Only(TARGET_FPS_AUTO, TARGET_FPS_30)
            ),
            cameraEffect = SettingConfig(NONE_EFFECT_ID, OptionVisibility.Visible)
        )

        assertThat(hostPermittingSubMode.isCompatibleWith(subModePolicy)).isTrue()
        assertThat(
            CameraFeaturePolicy(
                stabilizationMode = SettingConfig(StabilizationMode.ON, OptionVisibility.Hidden)
            ).isCompatibleWith(subModePolicy)
        ).isFalse()
        assertThat(
            CameraFeaturePolicy(
                targetFrameRate = SettingConfig(TARGET_FPS_60, OptionVisibility.Hidden)
            ).isCompatibleWith(subModePolicy)
        ).isFalse()
        assertThat(
            CameraFeaturePolicy(
                cameraEffect = SettingConfig(
                    CameraEffectId("single_stream"),
                    OptionVisibility.Hidden
                )
            ).isCompatibleWith(subModePolicy)
        ).isFalse()
    }

    @Test
    fun intersect_narrowsStabilizationFrameRateAndCameraEffect() {
        val hostPolicy = CameraFeaturePolicy(
            stabilizationMode = SettingConfig(StabilizationMode.AUTO, OptionVisibility.Visible),
            targetFrameRate = SettingConfig(
                defaultValue = TARGET_FPS_30,
                visibility = OptionVisibility.Only(TARGET_FPS_AUTO, TARGET_FPS_15, TARGET_FPS_30)
            )
        )
        val subModePolicy = CameraFeaturePolicy(
            stabilizationMode = SettingConfig(StabilizationMode.OFF, OptionVisibility.Hidden),
            targetFrameRate = SettingConfig(
                defaultValue = TARGET_FPS_AUTO,
                visibility = OptionVisibility.Only(TARGET_FPS_AUTO, TARGET_FPS_30, TARGET_FPS_60)
            ),
            cameraEffect = SettingConfig(NONE_EFFECT_ID, OptionVisibility.Hidden)
        )

        val intersected = hostPolicy.intersect(subModePolicy)

        assertThat(intersected.stabilizationMode)
            .isEqualTo(SettingConfig(StabilizationMode.OFF, OptionVisibility.Hidden))
        assertThat(intersected.targetFrameRate).isEqualTo(
            SettingConfig(
                defaultValue = TARGET_FPS_AUTO,
                visibility = OptionVisibility.Only(TARGET_FPS_AUTO, TARGET_FPS_30)
            )
        )
        assertThat(intersected.cameraEffect)
            .isEqualTo(SettingConfig(NONE_EFFECT_ID, OptionVisibility.Hidden))
    }

    @Test
    fun toCameraAppSettings_appliesVideoQualityDefault() {
        val policy = CameraFeaturePolicy(
            videoQuality = SettingConfig(defaultValue = VideoQuality.FHD)
        )

        assertThat(policy.toCameraAppSettings().videoQuality).isEqualTo(VideoQuality.FHD)
        assertThat(CameraFeaturePolicy().toCameraAppSettings().videoQuality)
            .isEqualTo(DEFAULT_CAMERA_APP_SETTINGS.videoQuality)
    }

    @Test
    fun enforceRestrictions_whenVideoQualityHidden_clampsToDefaultValue() {
        val policy = CameraFeaturePolicy(
            videoQuality = SettingConfig(VideoQuality.UNSPECIFIED, OptionVisibility.Hidden)
        )
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(videoQuality = VideoQuality.UHD)

        assertThat(policy.enforceRestrictions(settings).videoQuality)
            .isEqualTo(VideoQuality.UNSPECIFIED)
        assertThat(CameraFeaturePolicy().enforceRestrictions(settings).videoQuality)
            .isEqualTo(VideoQuality.UHD)
    }

    @Test
    fun isCompatibleWith_comparesVideoQuality() {
        val subModePolicy = CameraFeaturePolicy(
            videoQuality = SettingConfig(VideoQuality.UNSPECIFIED, OptionVisibility.Hidden)
        )

        assertThat(
            CameraFeaturePolicy(
                videoQuality = SettingConfig(
                    defaultValue = VideoQuality.FHD,
                    visibility = OptionVisibility.Only(VideoQuality.UNSPECIFIED, VideoQuality.FHD)
                )
            ).isCompatibleWith(subModePolicy)
        ).isTrue()
        assertThat(
            CameraFeaturePolicy(
                videoQuality = SettingConfig(VideoQuality.UHD, OptionVisibility.Hidden)
            ).isCompatibleWith(subModePolicy)
        ).isFalse()
    }

    @Test
    fun intersect_narrowsVideoQuality() {
        val hostPolicy = CameraFeaturePolicy(
            videoQuality = SettingConfig(
                defaultValue = VideoQuality.FHD,
                visibility = OptionVisibility.Only(
                    VideoQuality.HD,
                    VideoQuality.FHD,
                    VideoQuality.UHD
                )
            )
        )
        val subModePolicy = CameraFeaturePolicy(
            videoQuality = SettingConfig(
                defaultValue = VideoQuality.HD,
                visibility = OptionVisibility.Only(
                    VideoQuality.SD,
                    VideoQuality.HD,
                    VideoQuality.FHD
                )
            )
        )

        assertThat(hostPolicy.intersect(subModePolicy).videoQuality).isEqualTo(
            SettingConfig(
                defaultValue = VideoQuality.HD,
                visibility = OptionVisibility.Only(VideoQuality.HD, VideoQuality.FHD)
            )
        )
        assertThat(CameraFeaturePolicy().intersect(subModePolicy).videoQuality)
            .isEqualTo(subModePolicy.videoQuality)
    }
}
