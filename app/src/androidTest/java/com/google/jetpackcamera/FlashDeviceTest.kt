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
import android.provider.MediaStore
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
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.SCREEN_FLASH_OVERLAY
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.SCREEN_FLASH_OVERLAY_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.getCurrentLensFacing
import com.google.jetpackcamera.utils.longClickForVideoRecording
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.setFlashMode
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class FlashDeviceTest {

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
    fun set_flash_on() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        composeTestRule.setFlashMode(FlashMode.ON)
    }

    @Test
    fun set_flash_auto() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        composeTestRule.setFlashMode(FlashMode.AUTO)
    }

    @Test
    fun set_flash_off() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        composeTestRule.setFlashMode(FlashMode.OFF)
    }

    @Test
    fun set_flash_low_light_boost() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        composeTestRule.setFlashMode(FlashMode.LOW_LIGHT_BOOST)
    }

    private fun assumeHalStableOnImageCapture() {
        // The GMD emulators with API <=31 will often crash the HAL when taking an image capture.
        // See b/195122056
        assume().that(Build.HARDWARE == "ranchu" && Build.VERSION.SDK_INT <= 31).isFalse()
    }

    @Test
    fun set_flash_and_capture_successfully() = runMainActivityMediaStoreAutoDeleteScenarioTest(
        mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        filePrefix = "JCA"
    ) {
        // Skip test on unstable devices
        assumeHalStableOnImageCapture()

        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        // Ensure camera has a back camera and flip to it
        val lensFacing = composeTestRule.getCurrentLensFacing()
        if (lensFacing != LensFacing.BACK) {
            composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assume(isEnabled()) {
                "Device does not have a back camera to flip to."
            }.performClick()
        }

        composeTestRule.setFlashMode(FlashMode.ON)

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()

        composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
    }

    @Test
    fun set_screen_flash_and_capture_with_screen_change_overlay_shown() =
        runMainActivityMediaStoreAutoDeleteScenarioTest(
            mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            filePrefix = "JCA"
        ) {
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

            composeTestRule.setFlashMode(FlashMode.ON)

            // Perform a capture to enable screen flash
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                .assertExists()
                .performClick()

            composeTestRule.waitUntil(timeoutMillis = SCREEN_FLASH_OVERLAY_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(SCREEN_FLASH_OVERLAY).isDisplayed()
            }

            composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
            }
        }

    @Test
    fun set_flash_and_capture_rear_video_successfully() =
        set_flash_and_capture_video_successfully(LensFacing.BACK)

    @Test
    fun set_flash_and_capture_front_video_successfully() =
        set_flash_and_capture_video_successfully(LensFacing.FRONT)

    private fun set_flash_and_capture_video_successfully(targetLensFacing: LensFacing) =
        runMainActivityMediaStoreAutoDeleteScenarioTest(
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ) {
            // Wait for the capture button to be displayed
            composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
            }

            // Ensure camera has the target lens facing camera and flip to it
            val lensFacing = composeTestRule.getCurrentLensFacing()
            if (lensFacing != targetLensFacing) {
                composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assume(isEnabled()) {
                    "Device does not have a $targetLensFacing camera to flip to."
                }.performClick()
            }

            composeTestRule.setFlashMode(FlashMode.ON)

            composeTestRule.longClickForVideoRecording()
            composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
            }
        }
}
