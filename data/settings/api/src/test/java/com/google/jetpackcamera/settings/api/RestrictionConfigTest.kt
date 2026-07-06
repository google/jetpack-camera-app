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

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RestrictionConfigTest {

    @Test
    fun settingConfig_whenOptionsEnabledMissingDefaultValue_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            SettingConfig(
                defaultValue = FlashMode.OFF,
                uiRestriction = OptionRestrictionConfig.OptionsEnabled(
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
                uiRestriction = OptionRestrictionConfig.OptionsEnabled(
                    setOf(FlashMode.OFF)
                )
            )
        }
    }

    @Test
    fun developerAppConfig_whenFlashModeExcludesOff_throwsException() {
        assertThrows(IllegalArgumentException::class.java) {
            DeveloperAppConfig(
                aspectRatio = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
                flashMode = SettingConfig(
                    defaultValue = FlashMode.ON,
                    uiRestriction = OptionRestrictionConfig.OptionsEnabled(
                        setOf(FlashMode.ON, FlashMode.AUTO)
                    )
                ),
                captureMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
                imageOutputFormat = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.imageFormat),
                videoDynamicRange = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.dynamicRange)
            )
        }
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

        assertEquals(AspectRatio.NINE_SIXTEEN, appSettings.aspectRatio)
        assertEquals(FlashMode.ON, appSettings.flashMode)
        assertEquals(CaptureMode.VIDEO_ONLY, appSettings.captureMode)
        assertEquals(ImageOutputFormat.JPEG, appSettings.imageFormat)
        assertEquals(DynamicRange.SDR, appSettings.dynamicRange)
    }
}
