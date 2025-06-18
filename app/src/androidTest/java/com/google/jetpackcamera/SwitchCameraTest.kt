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

import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.ui.uistate.viewfinder.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.uistate.viewfinder.PREVIEW_DISPLAY
import com.google.jetpackcamera.ui.uistate.viewfinder.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.uistate.viewfinder.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.getCurrentLensFacing
import com.google.jetpackcamera.utils.runScenarioTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SwitchCameraTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun canFlipCamera_fromPreviewScreenButton() = runFlipCameraTest(composeTestRule) {
        val lensFacingStates = mutableListOf<LensFacing>()
        // Get initial lens facing
        val initialLensFacing = composeTestRule.getCurrentLensFacing()
        lensFacingStates.add(initialLensFacing)

        // Press the flip camera button
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).performClick()

        // Get lens facing after first flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        // Press the flip camera button again
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).performClick()

        // Get lens facing after second flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        assertThat(lensFacingStates).containsExactly(
            initialLensFacing,
            initialLensFacing.flip(),
            initialLensFacing.flip().flip()
        ).inOrder()
    }

    @Test
    fun canFlipCamera_fromPreviewScreenDoubleTap() = runFlipCameraTest(composeTestRule) {
        val lensFacingStates = mutableListOf<LensFacing>()
        // Get initial lens facing
        val initialLensFacing = composeTestRule.getCurrentLensFacing()
        lensFacingStates.add(initialLensFacing)

        // Double click display to flip camera
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY)
            .performTouchInput { doubleClick() }

        // Get lens facing after first flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        // Double click display to flip camera again
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY)
            .performTouchInput { doubleClick() }

        // Get lens facing after second flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        assertThat(lensFacingStates).containsExactly(
            initialLensFacing,
            initialLensFacing.flip(),
            initialLensFacing.flip().flip()
        ).inOrder()
    }

    @Test
    fun canFlipCamera_fromQuickSettings() = runFlipCameraTest(composeTestRule) {
        // Navigate to quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        val lensFacingStates = mutableListOf<LensFacing>()
        // Get initial lens facing
        val initialLensFacing = composeTestRule.getCurrentLensFacing()
        lensFacingStates.add(initialLensFacing)

        // Double click quick settings button to flip camera
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON).performClick()

        // Get lens facing after first flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        // Double click quick settings button to flip camera again
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON).performClick()

        // Get lens facing after second flip
        lensFacingStates.add(composeTestRule.getCurrentLensFacing())

        assertThat(lensFacingStates).containsExactly(
            initialLensFacing,
            initialLensFacing.flip(),
            initialLensFacing.flip().flip()
        ).inOrder()
    }
}

inline fun runFlipCameraTest(
    composeTestRule: ComposeTestRule,
    crossinline block: ActivityScenario<MainActivity>.() -> Unit
) = runScenarioTest {
    // Wait for the preview display to be visible
    composeTestRule.waitUntil(APP_START_TIMEOUT_MILLIS) {
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).isDisplayed()
    }

    // If flipping the camera is available, flip it. Otherwise skip test.
    composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assume(isEnabled()) {
        "Device does not have multiple cameras to flip between."
    }

    block()
}
