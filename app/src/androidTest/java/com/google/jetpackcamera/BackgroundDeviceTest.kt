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

import android.os.Build
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_CAPTURE_MODE_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.feature.quicksettings.ui.QUICK_SETTINGS_RATIO_BUTTON
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BackgroundDeviceTest {
    @get:Rule
    val cameraPermissionRule: GrantPermissionRule =
        GrantPermissionRule.grant(android.Manifest.permission.CAMERA)

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    private fun backgroundThenForegroundApp() {
        uiDevice.pressHome()
        uiDevice.pressRecentApps()
        uiDevice.pressRecentApps()

        // Wait for the app to return to the foreground
        uiDevice.wait(
            Until.hasObject(By.pkg("com.google.jetpackcamera")),
            APP_START_TIMEOUT_MILLIS
        )
    }

    @Before
    fun setUp() {
        assertThat(uiDevice.isScreenOn).isTrue()
    }

    @Test
    fun background_foreground() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        backgroundThenForegroundApp()
    }

    @Test
    fun flipCamera_then_background_foreground() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        // Click the flip camera button
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON)
            .assertExists()
            .performClick()

        // Exit quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        backgroundThenForegroundApp()
    }

    @Test
    fun setAspectRatio_then_background_foreground() = runScenarioTest<MainActivity> {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        // Click the ratio button
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_BUTTON)
            .assertExists()
            .performClick()

        // Click the 1:1 ratio button
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_RATIO_1_1_BUTTON)
            .assertExists()
            .performClick()

        // Exit quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        backgroundThenForegroundApp()
    }

    private fun assumeSupportsSingleStream() {
        // The GMD emulators with API <=28 do not support single-stream configs.
        assume().that(Build.HARDWARE == "ranchu" && Build.VERSION.SDK_INT <= 28).isFalse()
    }

    @Test
    fun toggleCaptureMode_then_background_foreground() = runScenarioTest<MainActivity> {
        // Skip this test on devices that don't support single stream
        assumeSupportsSingleStream()

        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Navigate to quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        // Click the flip camera button
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_CAPTURE_MODE_BUTTON)
            .assertExists()
            .performClick()

        // Exit quick settings
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
            .assertExists()
            .performClick()

        backgroundThenForegroundApp()
    }
}
