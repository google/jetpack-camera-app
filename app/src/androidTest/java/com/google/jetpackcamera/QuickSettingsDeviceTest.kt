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

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_SCRIM
import com.google.jetpackcamera.utils.CameraXResetRule
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForNodeWithTag
import com.google.jetpackcamera.utils.waitForNodeWithTagToDisappear
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class QuickSettingsDeviceTest {
    @get:Rule
    val cameraXResetRule = CameraXResetRule()

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val uiDevice: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun quickSettings_toggleOpenAndClose() = runMainActivityScenarioTest {
        composeTestRule.waitForCaptureButton()

        // Toggle open
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()
        composeTestRule.waitForNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)
            .assertIsDisplayed()

        // Toggle closed via drop-down button
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .performClick()
        composeTestRule.waitForNodeWithTagToDisappear(QUICK_SETTINGS_BOTTOM_SHEET)
    }

    @Test
    fun quickSettings_dismissViaScrim() = runMainActivityScenarioTest {
        composeTestRule.waitForCaptureButton()

        // Open quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()
        composeTestRule.waitForNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertExists()

        // Tap scrim to dismiss
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .performClick()
        composeTestRule.waitForNodeWithTagToDisappear(QUICK_SETTINGS_BOTTOM_SHEET)
    }

    @Test
    fun quickSettings_dismissViaBackPress() = runMainActivityScenarioTest {
        composeTestRule.waitForCaptureButton()

        // Open quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()
        composeTestRule.waitForNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)

        // Press back key
        uiDevice.pressBack()

        // Sheet should dismiss and preview capture button remains displayed
        composeTestRule.waitForNodeWithTagToDisappear(QUICK_SETTINGS_BOTTOM_SHEET)
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertIsDisplayed()
    }

    @Test
    fun quickSettings_dismissOnCaptureButtonClick() = runMainActivityScenarioTest {
        composeTestRule.waitForCaptureButton()

        // Open quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()
        composeTestRule.waitForNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)

        // Tapping capture button dismisses quick settings
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .performClick()
        composeTestRule.waitForNodeWithTagToDisappear(QUICK_SETTINGS_BOTTOM_SHEET)
    }
}
