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
package com.google.jetpackcamera

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.jetpackcamera.ui.components.capture.GRID_OVERLAY
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_GRID_BUTTON
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.visitQuickSettings
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PreviewScreenTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun can_toggle_grid_on_and_off() = runMainActivityScenarioTest {
        composeTestRule.waitForCaptureButton()
        composeTestRule.visitQuickSettings(QUICK_SETTINGS_GRID_BUTTON) {
            // Click the grid button to turn it on
            onNodeWithTag(QUICK_SETTINGS_GRID_BUTTON).performClick()
        }

        // Verify the grid is displayed
        composeTestRule.onNodeWithTag(GRID_OVERLAY).assertIsDisplayed()

        composeTestRule.visitQuickSettings(QUICK_SETTINGS_GRID_BUTTON) {
            // Click the grid button to turn it off
            onNodeWithTag(QUICK_SETTINGS_GRID_BUTTON).performClick()
        }
        // Verify the grid is not displayed
        composeTestRule.onNodeWithTag(GRID_OVERLAY).assertDoesNotExist()
    }
}
