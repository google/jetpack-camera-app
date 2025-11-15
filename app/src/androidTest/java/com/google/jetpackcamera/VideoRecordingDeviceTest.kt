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

import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.utils.IMAGE_PREFIX
import com.google.jetpackcamera.utils.MOVIES_DIR_PATH
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.VIDEO_PREFIX
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.doesMediaExist
import com.google.jetpackcamera.utils.getSingleImageCaptureIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.longClickForVideoRecording
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.pressAndDragToLockVideoRecording
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runScenarioTestForResult
import com.google.jetpackcamera.utils.tapStartLockedVideoRecording
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class VideoRecordingDeviceTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun pressed_video_capture(): Unit = runMainActivityMediaStoreAutoDeleteScenarioTest(
        mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ) {
        val timeStamp = System.currentTimeMillis()
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        composeTestRule.longClickForVideoRecordingCheckingElapsedTime()
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun drag_to_lock_pressed_video_capture(): Unit =
        runMainActivityMediaStoreAutoDeleteScenarioTest(
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ) {
            val timeStamp = System.currentTimeMillis()
            // Wait for the capture button to be displayed
            composeTestRule.waitForCaptureButton()
            composeTestRule.pressAndDragToLockVideoRecording()

            // stop recording
            // fixme: this shouldn't need two clicks
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

            composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
            }

            deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timeStamp)
        }

    @Test
    fun pressed_video_capture_external_intent() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(MOVIES_DIR_PATH, timeStamp, "mp4")
        val result =
            runScenarioTestForResult<MainActivity>(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                composeTestRule.longClickForVideoRecordingCheckingElapsedTime()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        Truth.assertThat(doesMediaExist(uri, VIDEO_PREFIX)).isTrue()
        deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun tap_video_capture_external_intent() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(MOVIES_DIR_PATH, timeStamp, "mp4")
        val result =
            runScenarioTestForResult<MainActivity>(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                // start recording
                composeTestRule.tapStartLockedVideoRecording()

                // stop recording
                composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        Truth.assertThat(doesMediaExist(uri, IMAGE_PREFIX)).isFalse()
        Truth.assertThat(doesMediaExist(uri, VIDEO_PREFIX)).isTrue()
        deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun video_capture_external_illegal_uri() {
        val uri = Uri.parse("asdfasdf")
        val result =
            runScenarioTestForResult<MainActivity>(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                composeTestRule.longClickForVideoRecording()
                composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(VIDEO_CAPTURE_FAILURE_TAG).isDisplayed()
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        Truth.assertThat(doesMediaExist(uri, VIDEO_PREFIX)).isFalse()
    }
}
