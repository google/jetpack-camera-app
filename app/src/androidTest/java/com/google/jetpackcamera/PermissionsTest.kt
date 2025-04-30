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
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.feature.preview.ui.IMAGE_WELL_TAG
import com.google.jetpackcamera.permissions.AUDIO_RECORD_PERMISSION
import com.google.jetpackcamera.permissions.ui.CAMERA_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.READ_EXTERNAL_STORAGE_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.RECORD_AUDIO_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.REQUEST_PERMISSION_BUTTON
import com.google.jetpackcamera.permissions.ui.WRITE_EXTERNAL_STORAGE_PERMISSION_BUTTON
import com.google.jetpackcamera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IndividualTestGrantPermissionRule
import com.google.jetpackcamera.utils.askEveryTimeDialog
import com.google.jetpackcamera.utils.denyPermissionDialog
import com.google.jetpackcamera.utils.ensureTagNotAppears
import com.google.jetpackcamera.utils.grantPermissionDialog
import com.google.jetpackcamera.utils.onNodeWithText
import com.google.jetpackcamera.utils.runScenarioTest
import com.google.jetpackcamera.utils.waitForStartup
import org.junit.Assume.assumeTrue
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
        targetTestNames = arrayOf("allPermissions_alreadyGranted_screenNotShown")
    )

    @get:Rule
    val cameraAudioPermissionRule = IndividualTestGrantPermissionRule(
        permissions = arrayOf(CAMERA_PERMISSION, AUDIO_RECORD_PERMISSION),
        targetTestNames = arrayOf(
            "writeStoragePermission_granted_skipsReadPermission",
            "writeStoragePermission_granted_ReadPermission_granted",
            "writeStoragePermission_denied_ReadPermission_granted",
            "writeStoragePermission_denied_skips_ReadPermission",
            "writeStoragePermission_granted_ReadPermission_granted",
            "writeStoragePermission_denied_ReadPermission_granted",
            "writeStoragePermission_denied_ReadPermission_denied"

        )
    )

    @get:Rule
    val cameraPermissionRule = IndividualTestGrantPermissionRule(
        permissions = arrayOf(CAMERA_PERMISSION),
        targetTestNames = arrayOf(
            "recordAudioPermission_granted_closesPage",
            "recordAudioPermission_denied_closesPage"
        )
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun allPermissions_alreadyGranted_screenNotShown() {
        runScenarioTest<MainActivity> {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }
        }
    }

    @Test
    fun cameraPermission_granted_closesPage() = runScenarioTest<MainActivity> {
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
            uiDevice.askEveryTimeDialog()

            // Assert we're no longer on camera permission screen
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_BUTTON).assertDoesNotExist()
        }
    }

    @Test
    fun cameraPermission_declined_staysOnScreen() {
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
        runScenarioTest<MainActivity> {
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
        runScenarioTest<MainActivity> {
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
    fun writeStoragePermission_granted_skipsReadPermission() {
        uiDevice.waitForIdle()
        runScenarioTest<MainActivity> {
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

            composeTestRule
                .onNodeWithTag(WRITE_EXTERNAL_STORAGE_PERMISSION_BUTTON)
                .assertDoesNotExist()
            try {
                composeTestRule.ensureTagNotAppears(READ_EXTERNAL_STORAGE_PERMISSION_BUTTON)
            } catch (e: AssertionError) {
                assumeTrue("read external storage permission was not skipped", false)
            }

            // If we are on preview screen, then read permission was automatically granted
            composeTestRule.waitForStartup()

            // check for imagewell
            composeTestRule.waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).isDisplayed()
            }
            // check for image capture success
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()
            composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
            }
        }
    }

    @SdkSuppress(maxSdkVersion = 28)
    @Test
    fun writeStoragePermission_granted_ReadPermission_granted() {
        uiDevice.waitForIdle()
        runScenarioTest<MainActivity> {
            // Wait for the permission screen to be displayed
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

            try {
                composeTestRule.ensureTagNotAppears(CAPTURE_BUTTON, timeoutMillis = 5_000)
            } catch (e: AssertionError) {
                assumeTrue("read external storage permission was skipped", false)
            }

            composeTestRule.waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(READ_EXTERNAL_STORAGE_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            composeTestRule.waitForStartup()
        }
    }

    @SdkSuppress(maxSdkVersion = 28)
    @Test
    fun writeStoragePermission_denied_ReadPermission_granted() {
        uiDevice.waitForIdle()
        runScenarioTest<MainActivity> {
            // Wait for the write permission screen to be displayed
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

            try {
                composeTestRule.ensureTagNotAppears(CAPTURE_BUTTON, timeoutMillis = 5_000)
            } catch (e: AssertionError) {
                assumeTrue("read external storage permission was skipped", false)
            }

            composeTestRule.waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(READ_EXTERNAL_STORAGE_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            uiDevice.grantPermissionDialog()

            composeTestRule.waitForStartup()

            // check for imagewell
            composeTestRule.waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).isDisplayed()
            }

            // check for image capture failure
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()
            composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_CAPTURE_FAILURE_TAG).isDisplayed()
            }
        }
    }

    @SdkSuppress(maxSdkVersion = 28)
    @Test
    fun writeStoragePermission_denied_ReadPermission_denied() {
        uiDevice.waitForIdle()
        runScenarioTest<MainActivity> {
            // Wait for the write permission screen to be displayed
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

            try {
                composeTestRule.ensureTagNotAppears(CAPTURE_BUTTON, 5_000)
            } catch (e: AssertionError) {
                assumeTrue("read external storage permission was skipped", false)
            }

            composeTestRule.waitUntil(timeoutMillis = DEFAULT_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(READ_EXTERNAL_STORAGE_PERMISSION_BUTTON).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON)
                .assertExists()
                .performClick()

            uiDevice.denyPermissionDialog()

            composeTestRule.waitForStartup()

            // check for imagewell
            try {
                composeTestRule.ensureTagNotAppears(IMAGE_WELL_TAG)
            } catch (e: AssertionError) {
                assumeTrue("image well should not be visible", false)
            }

            // check for image capture failure
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()
            composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_CAPTURE_FAILURE_TAG).isDisplayed()
            }
        }
    }

    @SdkSuppress(maxSdkVersion = 28)
    @Test
    fun writeStoragePermission_denied_skips_ReadPermission() {
        uiDevice.waitForIdle()
        runScenarioTest<MainActivity> {
            // Wait for the write permission screen to be displayed
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

            try {
                composeTestRule.ensureTagNotAppears(
                    READ_EXTERNAL_STORAGE_PERMISSION_BUTTON,
                    timeoutMillis = 5_000
                )
            } catch (e: AssertionError) {
                assumeTrue("read external storage permission is displayed", false)
            }

            composeTestRule.waitForStartup()

            // check for imagewell
            try {
                composeTestRule.ensureTagNotAppears(IMAGE_WELL_TAG)
            } catch (e: AssertionError) {
                assumeTrue("image well should not be visible", false)
            }

            // check for image capture failure
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()
            composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_CAPTURE_FAILURE_TAG).isDisplayed()
            }
        }
    }
}
