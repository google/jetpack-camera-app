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
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.VIDEO_DURATION_MILLIS
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.doesImageFileExist
import com.google.jetpackcamera.utils.getIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.runScenarioTest
import com.google.jetpackcamera.utils.runScenarioTestForResult
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class VideoRecordingDeviceTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(APP_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun video_capture() = runScenarioTest<MainActivity> {
        val timeStamp = System.currentTimeMillis()
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }
        longClickForVideoRecording()
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        Truth.assertThat(File(DIR_PATH).lastModified() > timeStamp).isTrue()
        deleteFilesInDirAfterTimestamp(DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun video_capture_external_intent() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(DIR_PATH, timeStamp, "mp4")
        val result =
            runScenarioTestForResult<MainActivity>(
                getIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }
                longClickForVideoRecording()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        Truth.assertThat(doesImageFileExist(uri, "video")).isTrue()
        deleteFilesInDirAfterTimestamp(DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun video_capture_external_illegal_uri() {
        val uri = Uri.parse("asdfasdf")
        val result =
            runScenarioTestForResult<MainActivity>(
                getIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }
                longClickForVideoRecording()
                composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(VIDEO_CAPTURE_FAILURE_TAG).isDisplayed()
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        Truth.assertThat(doesImageFileExist(uri, "video")).isFalse()
    }

    @Test
    fun image_capture_during_video_capture_external() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(ImageCaptureDeviceTest.DIR_PATH, timeStamp, "mp4")
        val result =
            runScenarioTestForResult<MainActivity>(
                getIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
                composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(
                        IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
                    ).isDisplayed()
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        Truth.assertThat(doesImageFileExist(uri, "image")).isFalse()
    }

    private fun longClickForVideoRecording() {
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performTouchInput {
                down(center)
            }
        idleForVideoDuration()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performTouchInput {
                up()
            }
    }

    private fun idleForVideoDuration() {
        // TODO: replace with a check for the timestamp UI of the video duration
        try {
            composeTestRule.waitUntil(timeoutMillis = VIDEO_DURATION_MILLIS) {
                composeTestRule.onNodeWithTag("dummyTagForLongPress").isDisplayed()
            }
        } catch (e: ComposeTimeoutException) {
        }
    }

    companion object {
        val DIR_PATH: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES).path
    }
}
