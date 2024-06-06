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
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.permissions.ui.AUDIO_RECORD_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.CAMERA_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.REQUEST_PERMISSION_BUTTON
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val CAMERA_PERMISSION = "android.permission.CAMERA"

@RunWith(AndroidJUnit4::class)
class PermissionsTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val allPermissionsRule = IndividualTestGrantPermissionRule(
        permissions = APP_REQUIRED_PERMISSIONS.toTypedArray(),
        targetTestNames = arrayOf(
            "noPermissionsScreenWhenAlreadyGranted"
        )
    )

    @get:Rule
    val cameraPermissionRule = IndividualTestGrantPermissionRule(
        permissions = arrayOf(CAMERA_PERMISSION),
        targetTestNames = arrayOf("deniedRecordAudioPermissionClosesPage")
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun noPermissionsScreenWhenAlreadyGranted() {
        runScenarioTest<MainActivity> {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }
        }
    }

    @Test
    fun grantedCameraPermissionClosesPage() = runScenarioTest<MainActivity> {
        // Wait for the camera permission screen to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).isDisplayed()
        }

        // Click button to request permission
        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
            .assertExists()
            .performClick()

        uiDevice.waitForIdle()
        // grant permission
        grantPermissionDialog(uiDevice)

        // Assert we're no longer on camera permission screen
        composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).assertDoesNotExist()

        // next permission should be on screen
        composeTestRule.onNodeWithTag(AUDIO_RECORD_PERMISSION_BUTTON).assertExists()
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun askEveryTimeCameraPermissionClosesPage() {
        uiDevice.waitForIdle()
        runScenarioTest<MainActivity> {
            // Wait for the camera permission screen to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // set permission to ask every time
            askEveryTimeDialog(uiDevice)

            // Assert we're no longer on camera permission screen
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).assertDoesNotExist()

            // next permission should be on screen
            composeTestRule.onNodeWithTag(AUDIO_RECORD_PERMISSION_BUTTON).assertExists()
        }
    }

    @Test
    fun declineCameraPermissionStaysOnScreen() {
        // required permissions should persist on screen
        // Wait for the permission screen to be displayed
        runScenarioTest<MainActivity> {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // deny permission
            denyPermissionDialog(uiDevice)

            uiDevice.waitForIdle()

            // Assert we're still on camera permission screen
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).isDisplayed()

            // request permissions button should now say to navigate to settings
            composeTestRule.onNodeWithText(
                com.google.jetpackcamera.permissions
                    .R.string.navigate_to_settings
            ).assertExists()
        }
    }

    @Test
    fun deniedRecordAudioPermissionClosesPage() {
        // optional permissions should close the screen after declining
        runScenarioTest<MainActivity> {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(AUDIO_RECORD_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // deny permission
            denyPermissionDialog(uiDevice)
            uiDevice.waitForIdle()

            // Assert we're now on preview screen
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().isDisplayed()
            }
        }
    }
}
