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
    fun cameraAppConfig_defaultConstructor_allPropertiesNull() {
        val config = CameraFeaturePolicy()
        assertThat(config.aspectRatio).isNull()
        assertThat(config.flashMode).isNull()
        assertThat(config.captureMode).isNull()
        assertThat(config.imageFormat).isNull()
        assertThat(config.dynamicRange).isNull()
    }

    @Test
    fun cameraAppConfig_whenFlashModeExcludesOff_throwsException() {
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
    fun cameraAppConfig_whenFlashModeHiddenAndNotOff_throwsException() {
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
    fun cameraAppConfig_whenFlashModeHiddenAndOff_succeeds() {
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
    fun cameraAppConfig_whenImageFormatHiddenAndNotJpeg_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            CameraFeaturePolicy(
                imageFormat = SettingConfig(
                    defaultValue = ImageOutputFormat.JPEG_ULTRA_HDR,
                    visibility = OptionVisibility.Hidden
                )
            )
        }
    }

    @Test
    fun cameraAppConfig_whenImageFormatHiddenAndJpeg_succeeds() {
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
    fun cameraAppConfig_whenDynamicRangeHiddenAndNotSdr_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            CameraFeaturePolicy(
                dynamicRange = SettingConfig(
                    defaultValue = DynamicRange.HLG10,
                    visibility = OptionVisibility.Hidden
                )
            )
        }
    }

    @Test
    fun cameraAppConfig_whenDynamicRangeHiddenAndSdr_succeeds() {
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

        val appConfig1 = CameraFeaturePolicy()
        val appConfig2 = CameraFeaturePolicy()
        assertThat(appConfig1).isEqualTo(appConfig2)
    }

    @Test
    fun toCameraAppSettings_overridesDefaults() {
        val developerConfig = CameraFeaturePolicy(
            aspectRatio = SettingConfig(AspectRatio.THREE_FOUR),
            flashMode = SettingConfig(FlashMode.ON),
            captureMode = SettingConfig(CaptureMode.VIDEO_ONLY),
            imageFormat = SettingConfig(ImageOutputFormat.JPEG_ULTRA_HDR),
            dynamicRange = SettingConfig(DynamicRange.HLG10)
        )

        val appSettings = developerConfig.toCameraAppSettings()

        assertThat(appSettings.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
        assertThat(appSettings.flashMode).isEqualTo(FlashMode.ON)
        assertThat(appSettings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(appSettings.imageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
        assertThat(appSettings.dynamicRange).isEqualTo(DynamicRange.HLG10)
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
    fun cameraAppConfig_whenFlashModeOnlyIncludesOff_succeeds() {
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
}
