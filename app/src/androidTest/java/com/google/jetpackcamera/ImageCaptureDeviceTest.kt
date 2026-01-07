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
import android.view.KeyEvent
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.utils.FILE_PREFIX
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_PREFIX
import com.google.jetpackcamera.utils.MESSAGE_DISAPPEAR_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.PICTURES_DIR_PATH
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.VIDEO_PREFIX
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.doesFileExist
import com.google.jetpackcamera.utils.doesMediaExist
import com.google.jetpackcamera.utils.ensureTagNotAppears
import com.google.jetpackcamera.utils.getMultipleImageCaptureIntent
import com.google.jetpackcamera.utils.getSingleImageCaptureIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.longClickForVideoRecording
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTestForResult
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class ImageCaptureDeviceTest {
    // TODO(b/319733374): Return bitmap for external mediastore capture without URI

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun image_capture_button() = runMainActivityMediaStoreAutoDeleteScenarioTest(
        mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        filePrefix = FILE_PREFIX
    ) {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
    }

    @Test
    fun image_capture_volumeUp() = runMainActivityMediaStoreAutoDeleteScenarioTest(
        mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        filePrefix = FILE_PREFIX
    ) {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()

        uiDevice.pressKeyCode(KeyEvent.KEYCODE_VOLUME_UP)

        composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
    }

    @Test
    fun image_capture_volumeDown() = runMainActivityMediaStoreAutoDeleteScenarioTest(
        mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        filePrefix = FILE_PREFIX
    ) {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        uiDevice.pressKeyCode(KeyEvent.KEYCODE_VOLUME_DOWN)

        composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
    }

    @Test
    fun image_capture_external() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(PICTURES_DIR_PATH, timeStamp, "jpg")
        val result =
            runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
            }

        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)

        Truth.assertThat(
            doesMediaExist(uri, IMAGE_PREFIX)
        ).isTrue()
        deleteFilesInDirAfterTimestamp(PICTURES_DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun image_capture_external_illegal_uri() {
        val uri = Uri.parse("asdfasdf")
        val result =
            runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()

                composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_FAILURE_TAG).isDisplayed()
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        Truth.assertThat(doesFileExist(uri)).isFalse()
    }

    @Test
    fun video_capture_during_image_capture_external() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(PICTURES_DIR_PATH, timeStamp, "mp4")
        val result =
            runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()

                composeTestRule.longClickForVideoRecording()

                composeTestRule.ensureTagNotAppears(
                    VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG,
                    VIDEO_CAPTURE_TIMEOUT_MILLIS
                )

                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        Truth.assertThat(doesMediaExist(uri, VIDEO_PREFIX)).isFalse()
    }

    @Test
    fun multipleImageCaptureExternal_returnsResultOk() {
        val timeStamp = System.currentTimeMillis()
        val uriStrings = arrayListOf<String>()
        for (i in 1..3) {
            val uri = getTestUri(PICTURES_DIR_PATH, timeStamp + i.toLong(), "jpg")
            uriStrings.add(uri.toString())
        }
        val result =
            runMainActivityScenarioTestForResult(
                getMultipleImageCaptureIntent(
                    uriStrings,
                    MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
                )
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                repeat(2) {
                    clickCaptureAndWaitUntilMessageDisappears(
                        IMAGE_CAPTURE_TIMEOUT_MILLIS,
                        IMAGE_CAPTURE_SUCCESS_TAG
                    )
                }
                clickCapture()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        for (string in uriStrings) {
            Truth.assertThat(
                doesMediaExist(Uri.parse(string), IMAGE_PREFIX)
            ).isTrue()
        }
        deleteFilesInDirAfterTimestamp(PICTURES_DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun multipleImageCaptureExternal_withNullUriList_returnsResultOk() {
        val timeStamp = System.currentTimeMillis()
        val result =
            runMainActivityScenarioTestForResult(
                getMultipleImageCaptureIntent(null, MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                repeat(2) {
                    clickCaptureAndWaitUntilMessageDisappears(
                        IMAGE_CAPTURE_TIMEOUT_MILLIS,
                        IMAGE_CAPTURE_SUCCESS_TAG
                    )
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        Truth.assertThat(result.resultData.getStringArrayListExtra(MediaStore.EXTRA_OUTPUT)?.size)
            .isEqualTo(2)
        deleteFilesInDirAfterTimestamp(PICTURES_DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun multipleImageCaptureExternal_withNullUriList_returnsResultCancel() {
        val result =
            runMainActivityScenarioTestForResult(
                getMultipleImageCaptureIntent(null, MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun multipleImageCaptureExternal_withIllegalUri_returnsResultOk() {
        val timeStamp = System.currentTimeMillis()
        val uriStrings = arrayListOf<String>()
        uriStrings.add("illegal_uri")
        uriStrings.add(getTestUri(PICTURES_DIR_PATH, timeStamp, "jpg").toString())
        val result =
            runMainActivityScenarioTestForResult(
                getMultipleImageCaptureIntent(
                    uriStrings,
                    MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
                )
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                clickCaptureAndWaitUntilMessageDisappears(
                    IMAGE_CAPTURE_TIMEOUT_MILLIS,
                    IMAGE_CAPTURE_FAILURE_TAG
                )
                clickCapture()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        Truth.assertThat(
            doesMediaExist(Uri.parse(uriStrings[1]), IMAGE_PREFIX)
        ).isTrue()
        deleteFilesInDirAfterTimestamp(PICTURES_DIR_PATH, instrumentation, timeStamp)
    }

    private fun clickCaptureAndWaitUntilMessageDisappears(msgTimeOut: Long, msgTag: String) {
        clickCapture()
        composeTestRule.waitUntil(timeoutMillis = msgTimeOut) {
            composeTestRule.onNodeWithTag(testTag = msgTag, useUnmergedTree = true).isDisplayed()
        }
        val dismissButtonMatcher =
            hasContentDescription(value = "dismiss", substring = true, ignoreCase = true)
        composeTestRule.waitUntil(timeoutMillis = msgTimeOut) {
            composeTestRule.onNode(dismissButtonMatcher, useUnmergedTree = true).isDisplayed()
        }
        composeTestRule.onNode(dismissButtonMatcher, useUnmergedTree = true)
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = MESSAGE_DISAPPEAR_TIMEOUT_MILLIS) {
            val node = composeTestRule.onNodeWithTag(testTag = msgTag, useUnmergedTree = true)
            node.isNotDisplayed()
        }
    }

    private fun clickCapture() {
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()
    }
}
