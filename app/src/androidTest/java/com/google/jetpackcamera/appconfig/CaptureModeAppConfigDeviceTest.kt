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
package com.google.jetpackcamera.appconfig

import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.AppModule
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.settings.model.CameraAppConfig
import com.google.jetpackcamera.settings.model.OptionAvailabilityConfig
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_IMAGE_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_VIDEO_ONLY
import com.google.jetpackcamera.ui.components.capture.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_CAPTURE_MODE
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.getCaptureModeToggleState
import com.google.jetpackcamera.utils.getCurrentCaptureMode
import com.google.jetpackcamera.utils.isCaptureModeToggleEnabled
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.visitQuickSettings
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CaptureModeAppConfigDeviceTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @After
    fun tearDown() {
        AppModule.testCameraAppConfig = null
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section A: Default Values
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun defaultCaptureMode_videoOnly_startsInVideoOnly() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            captureMode = SettingConfig(defaultValue = CaptureMode.VIDEO_ONLY)
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            assertThat(
                composeTestRule.getCaptureModeToggleState()
            ).isEqualTo(CaptureMode.VIDEO_ONLY)
            composeTestRule.visitQuickSettings {
                assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.VIDEO_ONLY)
            }
        }
    }

    @Test
    fun defaultCaptureMode_imageOnly_startsInImageOnly() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            captureMode = SettingConfig(defaultValue = CaptureMode.IMAGE_ONLY)
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            assertThat(
                composeTestRule.getCaptureModeToggleState()
            ).isEqualTo(CaptureMode.IMAGE_ONLY)
            composeTestRule.visitQuickSettings {
                assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.IMAGE_ONLY)
            }
        }
    }

    @Test
    fun defaultCaptureMode_standard_startsInStandard() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            captureMode = SettingConfig(defaultValue = CaptureMode.STANDARD)
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.visitQuickSettings {
                assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.STANDARD)
                onNodeWithTag(BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_STANDARD).assertIsOn()
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section B: Hidden
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun hiddenCaptureMode_standard_removesToggleAndQuickSettingsControl() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.STANDARD,
                uiVisibility = OptionAvailabilityConfig.Hidden
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).assertDoesNotExist()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
            }
        }
    }

    @Test
    fun hiddenCaptureMode_imageOnly_removesToggleAndQuickSettingsControl() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.IMAGE_ONLY,
                uiVisibility = OptionAvailabilityConfig.Hidden
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).assertDoesNotExist()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
            }
        }
    }

    // /////////////////////////////////////////////////////////////////////////
    //
    // Section C: OptionsEnabled / Filtering
    //
    // /////////////////////////////////////////////////////////////////////////

    @Test
    fun optionsEnabled_standardAndImageOnly_disablesVideoOnlyAndHidesToggle() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.STANDARD,
                uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                    setOf(CaptureMode.STANDARD, CaptureMode.IMAGE_ONLY)
                )
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).assertDoesNotExist()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertExists()
                onNodeWithTag(
                    BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_VIDEO_ONLY
                ).assertIsNotEnabled()
            }
        }
    }

    @Test
    fun optionsEnabled_standardAndVideoOnly_disablesImageOnlyAndHidesToggle() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.STANDARD,
                uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                    setOf(CaptureMode.STANDARD, CaptureMode.VIDEO_ONLY)
                )
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).assertDoesNotExist()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertExists()
                onNodeWithTag(
                    BTN_QUICK_SETTINGS_CAPTURE_MODE_OPTION_IMAGE_ONLY
                ).assertIsNotEnabled()
            }
        }
    }

    @Test
    fun optionsEnabled_imageOnlyAndVideoOnly_disablesStandardInQuickSettingsAndShowsToggle() {
        AppModule.testCameraAppConfig = CameraAppConfig(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.IMAGE_ONLY,
                uiVisibility = OptionAvailabilityConfig.OptionsEnabled(
                    setOf(CaptureMode.IMAGE_ONLY, CaptureMode.VIDEO_ONLY)
                )
            )
        )

        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            assertThat(composeTestRule.isCaptureModeToggleEnabled()).isTrue()
            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
            }
        }
    }
}
