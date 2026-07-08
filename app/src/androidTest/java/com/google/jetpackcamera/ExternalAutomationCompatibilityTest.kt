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

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import com.google.common.truth.Truth.assertWithMessage
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_BUTTON
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_SET_ZOOM_RATIO_SET_BUTTON
import com.google.jetpackcamera.ui.debug.DEBUG_OVERLAY_SET_ZOOM_RATIO_TEXT_FIELD
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.debugExtra
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalAutomationCompatibilityTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    private lateinit var device: UiDevice

    @Before
    fun setUp() {
        device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
    }

    @Test
    fun verifyUiAutomator_canFindCaptureControls() {
        runMainActivityScenarioTest {
            // Wait for the capture button to be visible to UI Automator via Resource ID
            val captureButton = device.wait(Until.findObject(By.res(CAPTURE_BUTTON)), TIMEOUT_MS)
            assertWithMessage("Capture button not found by UI Automator via Resource ID")
                .that(captureButton)
                .isNotNull()

            // Verify flip camera button is visible via Resource ID
            val flipButton = device.findObject(By.res(FLIP_CAMERA_BUTTON))
            assertWithMessage("Flip camera button not found by UI Automator via Resource ID")
                .that(flipButton)
                .isNotNull()
        }
    }

    @Test
    fun verifyUiAutomator_canFindDebugOverlayControls() {
        runMainActivityScenarioTest(debugExtra) {
            // Verify debug overlay button is visible via Resource ID
            val debugButton = device.wait(
                Until.findObject(By.res(DEBUG_OVERLAY_BUTTON)),
                TIMEOUT_MS
            )
            assertWithMessage("Debug overlay button not found by UI Automator via Resource ID")
                .that(debugButton)
                .isNotNull()
        }
    }

    @Test
    fun verifyUiAutomator_canFindDebugZoomDialogControls() {
        runMainActivityScenarioTest(debugExtra) {
            // Open debug menu using UI Automator
            val debugButton = device.wait(
                Until.findObject(By.res(DEBUG_OVERLAY_BUTTON)),
                TIMEOUT_MS
            )
            assertWithMessage("Debug overlay button not found").that(debugButton).isNotNull()
            debugButton.click()

            // Click "Set Zoom Ratio" button using UI Automator
            val setZoomButton = device.wait(
                Until.findObject(By.res(DEBUG_OVERLAY_SET_ZOOM_RATIO_BUTTON)),
                TIMEOUT_MS
            )
            assertWithMessage("Set Zoom Ratio button not found").that(setZoomButton).isNotNull()
            setZoomButton.click()

            // Verify the zoom dialog text field is visible to UI Automator
            val textField = device.wait(
                Until.findObject(By.res(DEBUG_OVERLAY_SET_ZOOM_RATIO_TEXT_FIELD)),
                TIMEOUT_MS
            )
            assertWithMessage(
                "Zoom text field not found by UI Automator"
            ).that(textField).isNotNull()

            // Verify the confirm button is visible to UI Automator
            val confirmButton = device.findObject(By.res(DEBUG_OVERLAY_SET_ZOOM_RATIO_SET_BUTTON))
            assertWithMessage(
                "Confirm button not found by UI Automator"
            ).that(confirmButton).isNotNull()
        }
    }

    companion object {
        private const val TIMEOUT_MS = 5000L
    }
}
