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

import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.components.capture.AMPLITUDE_HOT_TAG
import com.google.jetpackcamera.ui.components.capture.AMPLITUDE_NONE_TAG
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.DEBUG_OVERLAY_BUTTON
import com.google.jetpackcamera.ui.components.capture.BTN_DEBUG_HIDE_COMPONENTS_TAG
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.LOGICAL_CAMERA_ID_TAG
import com.google.jetpackcamera.ui.components.capture.PHYSICAL_CAMERA_ID_TAG
import com.google.jetpackcamera.ui.components.capture.ZOOM_BUTTON_ROW_TAG
import com.google.jetpackcamera.ui.components.capture.ZOOM_RATIO_TAG
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.debugExtra
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.waitForStartup
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class DebugHideComponentsTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        assertThat(uiDevice.isScreenOn).isTrue()
    }

    @Test
    fun hideComponentsButton_togglesUiVisibility() {
        runMainActivityScenarioTest(debugExtra) {
            composeTestRule.waitForStartup()
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(DEBUG_OVERLAY_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(LOGICAL_CAMERA_ID_TAG).assertExists()
            composeTestRule.onNodeWithTag(PHYSICAL_CAMERA_ID_TAG).assertExists()
            composeTestRule.onNodeWithTag(ZOOM_RATIO_TAG).assertExists()

            composeTestRule.onNodeWithTag(BTN_DEBUG_HIDE_COMPONENTS_TAG).performClick()

            composeTestRule.waitUntil {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isNotDisplayed()
            }
            composeTestRule.onNodeWithTag(ZOOM_BUTTON_ROW_TAG).assertDoesNotExist()
            composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertDoesNotExist()
            composeTestRule.onNodeWithTag(AMPLITUDE_NONE_TAG).assertDoesNotExist()
            composeTestRule.onNodeWithTag(AMPLITUDE_HOT_TAG).assertDoesNotExist()
            composeTestRule.onNodeWithTag(DEBUG_OVERLAY_BUTTON).assertDoesNotExist()
            composeTestRule.onNodeWithTag(LOGICAL_CAMERA_ID_TAG).assertDoesNotExist()
            composeTestRule.onNodeWithTag(PHYSICAL_CAMERA_ID_TAG).assertDoesNotExist()
            composeTestRule.onNodeWithTag(ZOOM_RATIO_TAG).assertDoesNotExist()

            composeTestRule.onNodeWithTag(BTN_DEBUG_HIDE_COMPONENTS_TAG).performClick()

            composeTestRule.waitUntil {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }
            composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(DEBUG_OVERLAY_BUTTON).assertExists()
            composeTestRule.onNodeWithTag(LOGICAL_CAMERA_ID_TAG).assertExists()
            composeTestRule.onNodeWithTag(PHYSICAL_CAMERA_ID_TAG).assertExists()
            composeTestRule.onNodeWithTag(ZOOM_RATIO_TAG).assertExists()

        }
    }
}