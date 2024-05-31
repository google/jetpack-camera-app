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

import android.app.UiAutomation
import android.os.Build
import android.os.SystemClock
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.permissions.ui.AUDIO_RECORD_PERMISSION_TAG
import com.google.jetpackcamera.permissions.ui.CAMERA_PERMISSION_TAG
import com.google.jetpackcamera.permissions.ui.REQUEST_PERMISSION_BUTTON_TAG
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

const val CAMERA_PERMISSION = "android.permission.CAMERA"
const val RECORD_AUDIO_PERMISSION = "android.permission.RECORD_AUDIO"

@RunWith(AndroidJUnit4::class)
class PermissionsTest {
    // todo: auto nav to camera permission from preview screen when camera permission revoked when app is backgrounded
    // todo: permission screen is skipped when permission is granted in background.

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @get:Rule
    val allPermissionsRule = IndividualTestGrantPermissionRule(
        permissions = APP_REQUIRED_PERMISSIONS.toTypedArray(),
        targetTestNames = listOf(
            "noPermissionsScreenWhenAlreadyGranted",
            "minimizeRevokeCameraPermissionReopensPermissionScreen"
        )
    )

    @get:Rule
    val cameraPermissionRule = IndividualTestGrantPermissionRule(
        CAMERA_PERMISSION,
        targetTestNames = listOf("deniedRecordAudioPermissionClosesPage")
    )

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)
    private val packageName = "com.google.jetpackcamera"

    @After
    fun tearDown() {
        clearPermissions(uiAutomation = instrumentation.uiAutomation)
    }

    @Test
    fun noPermissionsScreenWhenAlreadyGranted() {
        runScenarioTest<MainActivity> {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }
        }
    }

    @Test
    fun grantedCameraPermissionClosesPage() =
        runScenarioTest<MainActivity> {
            // Wait for the camera permission screen to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAMERA_PERMISSION_TAG).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON_TAG)
                .assertExists()
                .performClick()

            uiDevice.waitForIdle()
            // grant permission
            grantPermissionDialog(uiDevice)

            // Assert we're no longer on camera permission screen
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_TAG).assertDoesNotExist()

            // next permission should be on screen
            composeTestRule.onNodeWithTag(AUDIO_RECORD_PERMISSION_TAG).assertExists()
        }

    @SdkSuppress(minSdkVersion = 30)
    @Test
    fun askEveryTimeCameraPermissionClosesPage() {
        uiDevice.waitForIdle()
        runScenarioTest<MainActivity> {
            // Wait for the camera permission screen to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAMERA_PERMISSION_TAG).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON_TAG)
                .assertExists()
                .performClick()

            // set permission to ask every time
            askEveryTimeDialog(uiDevice)

            // Assert we're no longer on camera permission screen
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_TAG).assertDoesNotExist()

            // next permission should be on screen
            composeTestRule.onNodeWithTag(AUDIO_RECORD_PERMISSION_TAG).assertExists()
        }
    }

    @Test
    fun declineCameraPermissionStaysOnScreen() {
        //required permissions should persist on screen
        // Wait for the permission screen to be displayed
        runScenarioTest<MainActivity> {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAMERA_PERMISSION_TAG).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON_TAG)
                .assertExists()
                .performClick()

            // deny permission
            denyPermissionDialog(uiDevice)

            uiDevice.waitForIdle()

            // Assert we're still on camera permission screen
            composeTestRule.onNodeWithTag(CAMERA_PERMISSION_TAG).isDisplayed()

            // request permissions button should now say to navigate to settings
            composeTestRule.onNodeWithText(
                com.google.jetpackcamera.permissions
                    .R.string.navigate_to_settings
            ).assertExists()
        }
    }

    @Test
    fun deniedRecordAudioPermissionClosesPage() {
        //optional permissions should close the screen after declining
        uiDevice.waitForIdle()

        runScenarioTest<MainActivity> {
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(AUDIO_RECORD_PERMISSION_TAG).isDisplayed()
            }

            // Click button to request permission
            composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON_TAG)
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

    //@Test
    // can't revoke permissions in middle of instrumentation
    fun minimizeRevokeCameraPermissionReopensPermissionScreen() {

        runScenarioTest<MainActivity> {
            // ensure preview is visible
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }
            uiDevice.pressHome()
            SystemClock.sleep(1000)
            revokePermissions(
                packageName = packageName,
                uiAutomation = instrumentation.uiAutomation,
                permissions = listOf(
                    CAMERA_PERMISSION
                )
            )
            println("reopening")
            runScenarioTest<MainActivity> {
                //ensure preview is visible
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAMERA_PERMISSION_TAG).assertExists()
                        .isDisplayed()
                }
            }
        }
    }
}

fun clearPermissions(uiAutomation: UiAutomation) {
    uiAutomation.executeShellCommand("pm clear com.google.jetpackcamera") //pm reset-permissions
// uiAutomation.executeShellCommand("pm reset-permissions")
    println("clearing perms")

}

/**
 * function to revoke only specific permissions in the instrumentation. revoked permissions are different from permissions that have been neither granted nor revoked.
 *
 * @param permissions all permissions to be REVOKED.
 */
fun revokePermissions(packageName: String, uiAutomation: UiAutomation, permissions: List<String>) {
    permissions.forEach { perm ->
        if (Build.VERSION.SDK_INT >= 34)
            uiAutomation.revokeRuntimePermission(packageName, perm)
        else
            uiAutomation.executeShellCommand("pm revoke $packageName $perm") //pm reset-permissions

    }
}

