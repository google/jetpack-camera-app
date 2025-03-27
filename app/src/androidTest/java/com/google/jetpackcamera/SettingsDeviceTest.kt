/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera

import android.util.Log
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.SETTINGS_BUTTON
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_ASPECT_RATIO_OPTION_9_16_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_DARK_MODE_OPTION_ON_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FLASH_OPTION_AUTO_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FPS_OPTION_AUTO_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_DURATION_OPTION_UNLIMITED_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_VIDEO_QUALITY_OPTION_UNSPECIFIED_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_ASPECT_RATIO_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_DARK_MODE_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FLASH_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FPS_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_VIDEO_DURATION_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_VIDEO_QUALITY_TAG
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.runScenarioTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TAG = "SettingsDeviceTest"

@RunWith(AndroidJUnit4::class)
class SettingsDeviceTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private fun openSettings_clickSettingComponent_verifyDialog(
        componentTestTag: String,
        dialogTestTag: String,
        componentDisabledMessage: String
    ) = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to the settings screen
        composeTestRule.onNodeWithTag(SETTINGS_BUTTON)
            .assertExists()
            .performClick()

        composeTestRule.onNodeWithTag(componentTestTag)
            .assertExists()
            .performScrollTo()

        // Check if the settings dialog is displayed after the component is clicked
        try {
            composeTestRule.onNodeWithTag(componentTestTag)
                .assertIsEnabled()
            // Verify that UiAutomator object is also enabled
            assert(uiDevice.findObject(By.res(componentTestTag)).isEnabled)

            composeTestRule.onNodeWithTag(componentTestTag).performClick()
            composeTestRule.onNodeWithTag(dialogTestTag)
                .assertExists()
            uiDevice.pressBack()
        } catch (_: AssertionError) {
            // Verify that UiAutomator object is also disabled
            assert(!uiDevice.findObject(By.res(componentTestTag)).isEnabled)
            // The settings component is disabled. Display componentDisabledMessage
            Log.d(TAG, componentDisabledMessage)
        } finally {
            uiDevice.pressBack()
        }
    }

    @Test
    fun openSettings_openSetFlashModeDialog() = runScenarioTest<MainActivity> {
        openSettings_clickSettingComponent_verifyDialog(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_FLASH_TAG,
            dialogTestTag = BTN_DIALOG_FLASH_OPTION_AUTO_TAG,
            componentDisabledMessage = "Flash mode component is disabled"
        )
    }

    @Test
    fun openSettings_openSetFrameRateDialog() = runScenarioTest<MainActivity> {
        openSettings_clickSettingComponent_verifyDialog(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_FPS_TAG,
            dialogTestTag = BTN_DIALOG_FPS_OPTION_AUTO_TAG,
            componentDisabledMessage = "Frame rate component is disabled"
        )
    }

    @Test
    fun openSettings_openSetAspectRatioDialog() = runScenarioTest<MainActivity> {
        openSettings_clickSettingComponent_verifyDialog(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_ASPECT_RATIO_TAG,
            dialogTestTag = BTN_DIALOG_ASPECT_RATIO_OPTION_9_16_TAG,
            componentDisabledMessage = "Aspect ratio component is disabled"
        )
    }

    @Test
    fun openSettings_openSetStreamConfigDialog() = runScenarioTest<MainActivity> {
        openSettings_clickSettingComponent_verifyDialog(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG,
            dialogTestTag = BTN_DIALOG_STREAM_CONFIG_OPTION_SINGLE_STREAM_TAG,
            componentDisabledMessage = "Stream configuration component is disabled"
        )
    }

    @Test
    fun openSettings_openSetVideoDurationDialog() = runScenarioTest<MainActivity> {
        openSettings_clickSettingComponent_verifyDialog(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_VIDEO_DURATION_TAG,
            dialogTestTag = BTN_DIALOG_VIDEO_DURATION_OPTION_UNLIMITED_TAG,
            componentDisabledMessage = "Video duration component is disabled"
        )
    }

    @Test
    fun openSettings_openSetVideoQualityDialog() = runScenarioTest<MainActivity> {
        openSettings_clickSettingComponent_verifyDialog(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_VIDEO_QUALITY_TAG,
            dialogTestTag = BTN_DIALOG_VIDEO_QUALITY_OPTION_UNSPECIFIED_TAG,
            componentDisabledMessage = "Video quality component is disabled"
        )
    }

    @Test
    fun openSettings_openSetDarkModeDialog() = runScenarioTest<MainActivity> {
        openSettings_clickSettingComponent_verifyDialog(
            componentTestTag = BTN_OPEN_DIALOG_SETTING_DARK_MODE_TAG,
            dialogTestTag = BTN_DIALOG_DARK_MODE_OPTION_ON_TAG,
            componentDisabledMessage = "Dark mode component is disabled"
        )
    }
}
