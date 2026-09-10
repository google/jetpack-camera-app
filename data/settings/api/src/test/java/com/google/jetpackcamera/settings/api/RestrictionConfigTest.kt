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
package com.google.jetpackcamera.settings.api

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class RestrictionConfigTest {

    @Test
    fun settingConfig_whenOptionsEnabledMissingDefaultValue_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingConfig(
                defaultValue = FlashMode.OFF,
                uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                    setOf(FlashMode.ON, FlashMode.AUTO)
                )
            )
        }
    }

    @Test
    fun settingConfig_whenOptionsEnabledHasSingleOption_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingConfig(
                defaultValue = FlashMode.OFF,
                uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                    setOf(FlashMode.OFF)
                )
            )
        }
    }

    @Test
    fun optionsEnabled_whenLessThanTwoOptions_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            OptionAvailabilityConfig.OptionsEnabled<FlashMode>(emptySet())
        }
        assertThrows(IllegalArgumentException::class.java) {
            OptionAvailabilityConfig.OptionsEnabled(setOf(FlashMode.OFF))
        }
    }

    @Test
    fun developerAppConfig_defaultConstructor_usesDefaultSettings() {
        val config = DeveloperAppConfig()
        assertThat(
            config.aspectRatio.defaultValue
        ).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio)
        assertThat(config.flashMode.defaultValue).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS.flashMode)
        assertThat(
            config.captureMode.defaultValue
        ).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS.captureMode)
        assertThat(
            config.imageOutputFormat.defaultValue
        ).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS.imageFormat)
        assertThat(
            config.videoDynamicRange.defaultValue
        ).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS.dynamicRange)
        assertThat(
            config.aspectRatio.uiVisibility
        ).isEqualTo(OptionAvailabilityConfig.NotRestricted)
    }

    @Test
    fun developerAppConfig_whenFlashModeExcludesOff_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeveloperAppConfig(
                flashMode = SettingConfig(
                    defaultValue = FlashMode.ON,
                    uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                        setOf(FlashMode.ON, FlashMode.AUTO)
                    )
                )
            )
        }
    }

    @Test
    fun developerAppConfig_whenFlashModeHiddenAndNotOff_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeveloperAppConfig(
                flashMode = SettingConfig(
                    defaultValue = FlashMode.ON,
                    uiVisibility = OptionAvailabilityConfig.Hidden
                )
            )
        }
    }

    @Test
    fun developerAppConfig_whenFlashModeHiddenAndOff_succeeds() {
        val config = DeveloperAppConfig(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                uiVisibility = OptionAvailabilityConfig.Hidden
            )
        )
        assertThat(config.flashMode.defaultValue).isEqualTo(FlashMode.OFF)
        assertThat(config.flashMode.uiVisibility).isEqualTo(OptionAvailabilityConfig.Hidden)
    }

    @Test
    fun settingConfig_structuralEquality_matches() {
        val config1 = SettingConfig(FlashMode.OFF, OptionAvailabilityConfig.NotRestricted)
        val config2 = SettingConfig(FlashMode.OFF, OptionAvailabilityConfig.NotRestricted)
        assertThat(config1).isEqualTo(config2)

        val appConfig1 = DeveloperAppConfig()
        val appConfig2 = DeveloperAppConfig()
        assertThat(appConfig1).isEqualTo(appConfig2)
    }

    @Test
    fun toCameraAppSettings_overridesDefaults() {
        val developerConfig = DeveloperAppConfig(
            aspectRatio = SettingConfig(AspectRatio.NINE_SIXTEEN),
            flashMode = SettingConfig(FlashMode.ON),
            captureMode = SettingConfig(CaptureMode.VIDEO_ONLY),
            imageOutputFormat = SettingConfig(ImageOutputFormat.JPEG),
            videoDynamicRange = SettingConfig(DynamicRange.SDR)
        )

        val appSettings = developerConfig.toCameraAppSettings()

        assertThat(appSettings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
        assertThat(appSettings.flashMode).isEqualTo(FlashMode.ON)
        assertThat(appSettings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(appSettings.imageFormat).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(appSettings.dynamicRange).isEqualTo(DynamicRange.SDR)
    }

    @Test
    fun toCameraAppSettings_withCustomDefaults_preservesUnoverriddenSettings() {
        val customDefaults = DEFAULT_CAMERA_APP_SETTINGS.copy(
            maxVideoDurationMillis = 60_000L
        )
        val developerConfig = DeveloperAppConfig(
            captureMode = SettingConfig(CaptureMode.VIDEO_ONLY)
        )

        val appSettings = developerConfig.toCameraAppSettings(customDefaults)

        assertThat(appSettings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(appSettings.maxVideoDurationMillis).isEqualTo(60_000L)
    }
}
