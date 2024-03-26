/*
 * Copyright (C) 2023 The Android Open Source Project
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
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_SUCCESS_TOAST
import com.google.jetpackcamera.feature.preview.ui.SCREEN_FLASH_OVERLAY
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_FLASH_BUTTON
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FlashDeviceTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        assertThat(uiDevice.isScreenOn).isTrue()
    }

    @Test
    fun set_flash_on() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        // Click the flash button to switch to ON
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_FLASH_BUTTON)
            .assertExists()
            .performClick()

        // Exit quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        assert(
            UiTestUtil.getPreviewCameraAppSettings(this@runScenarioTest).flashMode ==
                FlashMode.ON
        )
    }

    @Test
    fun set_flash_auto() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        // Click the flash button twice to switch to AUTO
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_FLASH_BUTTON)
            .assertExists()
            .performClick()
            .performClick()

        // Exit quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        assert(
            UiTestUtil.getPreviewCameraAppSettings(this@runScenarioTest).flashMode ==
                FlashMode.AUTO
        )
    }

    @Test
    fun set_flash_off() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        assert(
            UiTestUtil.getPreviewCameraAppSettings(this@runScenarioTest).flashMode ==
                FlashMode.OFF
        )

        // Navigate to quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        // Click the flash button three times to switch to OFF
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_FLASH_BUTTON)
            .assertExists()
            .performClick()
            .performClick()
            .performClick()

        // Exit quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        assert(
            UiTestUtil.getPreviewCameraAppSettings(this@runScenarioTest).flashMode ==
                FlashMode.OFF
        )
    }

    @Test
    fun set_screen_flash_and_capture_successfully() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        // Click the flash button to switch to ON
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_FLASH_BUTTON)
            .assertExists()
            .performClick()

        // Exit quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TOAST).isDisplayed()
        }
    }

    @Test
    fun set_screen_flash_and_capture_with_screen_change_overlay_shown() =
        runScenarioTest<MainActivity> {
            // Wait for the capture button to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }

            // Ensure camera has a front camera and flip to it
            val lensFacing = composeTestRule.getCurrentLensFacing()
            if (lensFacing != LensFacing.FRONT) {
                composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assume(isEnabled()) {
                    "Device does not have a front camera to flip to."
                }.performClick()
            }

            // Navigate to quick settings
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                .assertExists()
                .performClick()

            // Click the flash button to switch to ON
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_FLASH_BUTTON)
                .assertExists()
                .performClick()

            // Exit quick settings
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                .assertExists()
                .performClick()

            // Perform a capture to enable screen flash
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()

            composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(SCREEN_FLASH_OVERLAY).isDisplayed()
            }
        }
}
