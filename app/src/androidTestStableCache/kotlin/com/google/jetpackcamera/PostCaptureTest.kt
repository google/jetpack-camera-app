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
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_DELETE
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_EXIT
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_SAVE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS
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
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class PostCaptureTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var timestamp: Long = Long.MAX_VALUE

    @Before
    fun setup() {
        timestamp = System.currentTimeMillis()
    }

    @After
    fun cleanup() {
        deleteFilesInDirAfterTimestamp(MEDIA_DIR_PATH, instrumentation, timestamp)
        deleteFilesInDirAfterTimestamp(PICTURES_DIR_PATH, instrumentation, timestamp)
        deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timestamp)
    }

    @Test
    fun postcapture_canSaveCachedImage() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

        // navigate to postcapture screen
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_IMAGE).isDisplayed()
        }

        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
        }
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).performClick()

        // Wait for image save success message
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onNodeWithTag(SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS).isDisplayed()
        }
        assertTrue(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp))
    }

    @Test
    fun postcapture_canSaveCachedVideo(): Unit = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        composeTestRule.longClickForVideoRecordingCheckingElapsedTime()

        // navigate to postcapture screen
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_VIDEO).isDisplayed()
        }
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).isDisplayed()
        }

        assertFalse(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp))

        // save video
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
        }
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).performClick()

        // Wait for video save success message
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onNodeWithTag(SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS).isDisplayed()
        }

        assertTrue(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp))

        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()
        composeTestRule.waitForCaptureButton()
    }

    @Test
    fun postcapture_canDeleteSavedImage() = runMainActivityScenarioTest(extras = debugExtra) {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

        // navigate to postcapture screen
        composeTestRule.waitUntil(timeoutMillis = 5_000L) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_IMAGE).isDisplayed()
        }

        assertFalse(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp))

        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
        }
        // save image
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).performClick()

        // Wait for image save success message
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onNodeWithTag(SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS).isDisplayed()
        }

        // exit postcapture
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()

        // wait for capture button after navigating out of postcapture
        composeTestRule.waitForCaptureButton()
        assertTrue(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp))

        // enter postcapture via imagewell
        composeTestRule.waitUntil { composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).isDisplayed() }
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertExists().performClick()

        // most recent capture should be image
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_IMAGE).isDisplayed()
        }

        // delete most recent capture
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).isDisplayed()
        }
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).assertExists().performClick()

        // wait for capture button after automatically exiting post capture
        composeTestRule.waitForCaptureButton()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp)
        }
    }

    @Test
    fun postcapture_canDeleteSavedVideo(): Unit = runMainActivityScenarioTest(extras = debugExtra) {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        composeTestRule.longClickForVideoRecordingCheckingElapsedTime()

        // navigate to postcapture screen
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_VIDEO).isDisplayed()
        }

        assertFalse(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp))

        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
        }

        // save video
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).performClick()

        // Wait for video save success message
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            composeTestRule.onNodeWithTag(SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS).isDisplayed()
        }

        // exit postcapture
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()

        // wait for capture button after navigating out of postcapture
        composeTestRule.waitForCaptureButton()
        assertTrue(filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp))

        // enter postcapture via imagewell
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).isDisplayed()
        }
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertExists().performClick()

        // most recent capture should be video
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_VIDEO).isDisplayed()
        }

        // delete most recent capture
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).isDisplayed()
        }
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).assertExists().performClick()

        // wait for capture button after automatically exiting post capture
        composeTestRule.waitForCaptureButton()
        composeTestRule.waitUntil(timeoutMillis = 5_000) {
            !filesExistInDirAfterTimestamp(MEDIA_DIR_PATH, timestamp)
        }
    }
}
