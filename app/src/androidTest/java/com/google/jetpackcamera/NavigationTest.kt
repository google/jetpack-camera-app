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
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.preview.ui.SETTINGS_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.settings.ui.BACK_BUTTON
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun backAfterReturnFromSettings_doesNotReturnToSettings() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to the settings screen
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
            com.google.jetpackcamera.settings.R.string.settings_title
        ).assertDoesNotExist()
    }

    // Test to ensure we haven't regressed to the cause of
    // https://github.com/google/jetpack-camera-app/pull/28
    @Test
    fun returnFromSettings_afterFlipCamera_returnsToPreview() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // If flipping the camera is available, flip it. Otherwise skip test.
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON)
            .assume(isEnabled()) {
                "Device does not have multiple cameras to flip between."
            }.performClick()

        // Navigate to the settings screen
        composeTestRule.onNodeWithTag(SETTINGS_BUTTON)
            .assertExists()
            .performClick()

        // Navigate back using the button
        composeTestRule.onNodeWithTag(BACK_BUTTON)
            .assertExists()
            .performClick()

        // Assert we're on PreviewScreen by finding the capture button
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
    }

    @Test
    fun backFromQuickSettings_returnToPreview() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

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
    fun backFromQuickSettingsExpended_returnToQuickSettings() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to the quick settings screen
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

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

        // Assert we're on quick settings by finding the ratio button
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_BUTTON).assertExists()
    }
}
