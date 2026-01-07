/*
 * Copyright (C) 2024 The Android Open Source Project
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

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.jetpackcamera.settings.R
import com.google.jetpackcamera.settings.ui.BACK_BUTTON
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.onNodeWithText
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.searchForQuickSetting
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun backAfterReturnFromSettings_doesNotReturnToSettings() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        // open quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).assertExists().performClick()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertExists()

        // Navigate to the settings screen
        composeTestRule.searchForQuickSetting(SETTINGS_BUTTON)
        composeTestRule.onNodeWithTag(SETTINGS_BUTTON)
            .assertExists()
            .performClick()

        // Navigate back using the button
        composeTestRule.onNodeWithTag(BACK_BUTTON)
            .assertExists()
            .performClick()

        // Assert we're on PreviewScreen by finding the capture button
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()

        // Press the device's back button
        uiDevice.pressBack()

        // Assert we do not see the settings screen based on the title
        composeTestRule.onNodeWithText(
            R.string.settings_title
        ).assertDoesNotExist()
    }

    // Test to ensure we haven't regressed to the cause of
    // https://github.com/google/jetpack-camera-app/pull/28
    @Test
    fun returnFromSettings_afterFlipCamera_returnsToPreview() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        // If flipping the camera is available, flip it. Otherwise skip test.
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON)
            .assume(isEnabled()) {
                "Device does not have multiple cameras to flip between."
            }.performClick()

        // open quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).assertExists().performClick()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertExists()

        // Navigate to the settings screen
        composeTestRule.searchForQuickSetting(SETTINGS_BUTTON)
        composeTestRule.onNodeWithTag(SETTINGS_BUTTON)
            .assertExists()
            .performClick()

        // Navigate back using the button
        composeTestRule.onNodeWithTag(BACK_BUTTON)
            .assertExists()
            .performClick()

        // Assert we're on PreviewScreen by finding the capture button
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()

        // Assert bottom sheet is not open
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()
    }

    @Test
    fun backFromQuickSettings_returnToPreview() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        // Navigate to the quick settings screen
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        // Wait for the quick settings to be displayed
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_BUTTON).isDisplayed()
        }

        // Press the device's back button
        uiDevice.pressBack()

        // Assert we're on PreviewScreen by finding the flip camera button
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertExists()
    }

    @Test
    fun backFromQuickSettingsExpended_returnToQuickSettings() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        // Navigate to the quick settings screen
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        composeTestRule.searchForQuickSetting(QUICK_SETTINGS_RATIO_BUTTON)

        // Navigate to the expanded quick settings ratio screen
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_BUTTON)
            .assertExists()
            .performClick()

        // Wait for the 1:1 ratio button to be displayed
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_1_1_BUTTON).isDisplayed()
        }

        // Press the device's back button
        uiDevice.pressBack()

        // Assert bottom sheet closed
        composeTestRule.waitUntil(DEFAULT_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).isNotDisplayed()
        }
    }
}
