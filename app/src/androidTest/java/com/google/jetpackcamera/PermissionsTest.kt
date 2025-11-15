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

import android.Manifest.permission.CAMERA
import android.Manifest.permission.RECORD_AUDIO
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.jetpackcamera.permissions.ui.CAMERA_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.RECORD_AUDIO_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.REQUEST_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.WRITE_EXTERNAL_STORAGE_PERMISSION_BUTTON
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_WELL_TAG
import com.google.jetpackcamera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IndividualTestGrantPermissionRule
import com.google.jetpackcamera.utils.PICTURES_DIR_PATH
import com.google.jetpackcamera.utils.askEveryTimeDialog
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.denyPermissionDialog
import com.google.jetpackcamera.utils.ensureTagNotAppears
import com.google.jetpackcamera.utils.grantPermissionDialog
import com.google.jetpackcamera.utils.onNodeWithText
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PermissionsTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val allPermissionsRule = IndividualTestGrantPermissionRule(
        permissions = APP_REQUIRED_PERMISSIONS.toTypedArray(),
        targetTestNames = arrayOf("allPermissions_alreadyGranted_screenNotShown")
    )

    @get:Rule
    val cameraAudioPermissionRule = IndividualTestGrantPermissionRule(
        permissions = arrayOf(CAMERA, RECORD_AUDIO),
        targetTestNames = arrayOf(
            "writeStoragePermission_granted",
            "writeStoragePermission_denied"
        )
    )

    @get:Rule
    val cameraPermissionRule = IndividualTestGrantPermissionRule(
        permissions = arrayOf(CAMERA),
        targetTestNames = arrayOf(
            "recordAudioPermission_granted_closesPage",
            "recordAudioPermission_denied_closesPage"
        )
    )

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Test
    fun allPermissions_alreadyGranted_screenNotShown() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
        }
    }

    @Test
    fun cameraPermission_granted_closesPage() = runMainActivityScenarioTest {
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
        uiDevice.grantPermissionDialog()

        // Assert we're no longer on camera permission screen
        composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).assertDoesNotExist()
    }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun cameraPermission_askEveryTime_closesPage() {
        uiDevice.waitForIdle()
        runMainActivityScenarioTest {
            // Wait for the camera permission screen to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // set permission to ask every time
            uiDevice.askEveryTimeDialog()

            // Assert we're no longer on camera permission screen
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).assertDoesNotExist()
        }
    }

    @Test
    fun cameraPermission_declined_staysOnScreen() {
        // required permissions should persist on screen
        // Wait for the permission screen to be displayed
        runMainActivityScenarioTest {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // deny permission
            uiDevice.denyPermissionDialog()

            uiDevice.waitForIdle()

            // Assert we're still on camera permission screen
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).isDisplayed()

            // text changed after permission denied
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithText(
                    com.google.jetpackcamera.permissions.R.string
                        .camera_permission_declined_rationale
                )
                    .isDisplayed()
            }
            // request permissions button should now say to navigate to settings
            composeTestRule.onNodeWithText(
                com.google.jetpackcamera.permissions
                    .R.string.navigate_to_settings
            ).assertExists()
        }
    }

    @Test
    fun recordAudioPermission_granted_closesPage() {
        // optional permissions should close the screen after declining
        runMainActivityScenarioTest {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(RECORD_AUDIO_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // grant permission
            uiDevice.grantPermissionDialog()
            uiDevice.waitForIdle()

            // Assert we're on a different page
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(RECORD_AUDIO_PERMISSION_BUTTON).isNotDisplayed()
            }
        }
    }

    @Test
    fun recordAudioPermission_denied_closesPage() {
        // optional permissions should close the screen after declining
        runMainActivityScenarioTest {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(RECORD_AUDIO_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // deny permission
            uiDevice.denyPermissionDialog()
            uiDevice.waitForIdle()

            // Assert we're on a different page
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(RECORD_AUDIO_PERMISSION_BUTTON).isNotDisplayed()
            }
        }
    }

    @SdkSuppress(maxSdkVersion = 28)
    @Test
    fun writeStoragePermission_granted() {
        uiDevice.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        val timeStamp = System.currentTimeMillis()
        runMainActivityScenarioTest {
            // Wait for the camera permission screen to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(
                    WRITE_EXTERNAL_STORAGE_PERMISSION_BUTTON
                ).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // grant permission
            uiDevice.grantPermissionDialog()

            // permission screen should close
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                composeTestRule
                    .onNodeWithTag(WRITE_EXTERNAL_STORAGE_PERMISSION_BUTTON)
                    .isNotDisplayed()
            }

            composeTestRule.waitForCaptureButton()

            // check for image capture success
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()
            composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
            }
        }

        deleteFilesInDirAfterTimestamp(PICTURES_DIR_PATH, instrumentation, timeStamp)
    }

    @SdkSuppress(maxSdkVersion = 28)
    @Test
    fun writeStoragePermission_denied() {
        uiDevice.waitForIdle()
        runMainActivityScenarioTest {
            // Wait for the camera permission screen to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(
                    WRITE_EXTERNAL_STORAGE_PERMISSION_BUTTON
                ).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            // deny permission
            uiDevice.denyPermissionDialog()

            // storage permission is optional and the screen should close
            composeTestRule.waitUntil {
                composeTestRule
                    .onNodeWithTag(WRITE_EXTERNAL_STORAGE_PERMISSION_BUTTON)
                    .isNotDisplayed()
            }

            composeTestRule.waitForCaptureButton()

            // check for image capture failure
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

            composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_CAPTURE_FAILURE_TAG).isDisplayed()
            }
            // imageWell shouldn't appear
            composeTestRule.ensureTagNotAppears(IMAGE_WELL_TAG)
        }
    }
}
