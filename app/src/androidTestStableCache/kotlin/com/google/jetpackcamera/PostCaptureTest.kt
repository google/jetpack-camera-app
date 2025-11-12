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
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_DELETE
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_EXIT
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_SAVE
import com.google.jetpackcamera.feature.postcapture.ui.VIEWER_POST_CAPTURE_IMAGE
import com.google.jetpackcamera.feature.postcapture.ui.VIEWER_POST_CAPTURE_VIDEO
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.IMAGE_WELL_TAG
import com.google.jetpackcamera.utils.MEDIA_DIR_PATH
import com.google.jetpackcamera.utils.MOVIES_DIR_PATH
import com.google.jetpackcamera.utils.PICTURES_DIR_PATH
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.debugExtra
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.filesExistInDirAfterTimestamp
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.wait
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PostCaptureTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun postcapture_canSaveCachedImage() = runMainActivityScenarioTest {
        val timeStamp = System.currentTimeMillis()

        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()

        // navigate to postcapture screen
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_IMAGE).isDisplayed()
        }


        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
        }
        //TODO(kc): wait for save success

        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).performClick()
        assertTrue(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timeStamp))
        deleteFilesInDirAfterTimestamp(MEDIA_DIR_PATH, instrumentation, timeStamp )
    }

    @Test
    fun postcapture_canSaveCachedVideo(): Unit = runMainActivityScenarioTest {
        val timeStamp = System.currentTimeMillis()

        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        composeTestRule.longClickForVideoRecordingCheckingElapsedTime()

        // navigate to postcapture screen
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_VIDEO).isDisplayed()
        }
        composeTestRule.waitUntil { composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).isDisplayed() }

        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
        }
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).performClick()
        //TODO(kc): wait for save success
        composeTestRule.wait(500L)


        assertTrue(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timeStamp))

        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()
        composeTestRule.waitForCaptureButton()

        deleteFilesInDirAfterTimestamp(MEDIA_DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun postcapture_canDeleteSavedImage() = runMainActivityScenarioTest(extras = debugExtra) {
        val timeStamp = System.currentTimeMillis()

        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()

        // navigate to postcapture screen
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_IMAGE).isDisplayed()
        }


        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
        }

        assertFalse(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timeStamp))

        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).performClick()

        //TODO(kc): wait for save success
        composeTestRule.wait(500L)
        assertTrue(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timeStamp))

        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()
        composeTestRule.waitForCaptureButton()

        composeTestRule.waitUntil { composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).isDisplayed() }
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertExists().performClick()
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).assertExists().performClick()

        composeTestRule.wait(1000L)

        composeTestRule.waitForCaptureButton()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timeStamp)
        }
    }

    @Test
    fun postcapture_canDeleteCachedVideo(): Unit = runMainActivityScenarioTest(extras = debugExtra) {
        val timeStamp = System.currentTimeMillis()

        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        composeTestRule.longClickForVideoRecordingCheckingElapsedTime()

        // navigate to postcapture screen
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_VIDEO).isDisplayed()
        }
        composeTestRule.waitUntil { composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).isDisplayed() }

        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
        }
        //TODO(kc): wait for save success
        composeTestRule.wait(500L)

        assertTrue(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timeStamp))

        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()
        composeTestRule.waitForCaptureButton()

        composeTestRule.waitUntil { composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).isDisplayed() }
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertExists().performClick()
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).assertExists().performClick()
        composeTestRule.wait(500L)

        composeTestRule.waitForCaptureButton()
        assertFalse(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timeStamp))

        deleteFilesInDirAfterTimestamp(MEDIA_DIR_PATH, instrumentation, timeStamp)
    }
}