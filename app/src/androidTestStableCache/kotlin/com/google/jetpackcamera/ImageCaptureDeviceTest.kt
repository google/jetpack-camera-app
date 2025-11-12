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

import android.app.Activity
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_EXIT
import com.google.jetpackcamera.feature.postcapture.ui.VIEWER_POST_CAPTURE_IMAGE
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_PREFIX
import com.google.jetpackcamera.utils.MESSAGE_DISAPPEAR_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.PICTURES_DIR_PATH
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.doesFileExist
import com.google.jetpackcamera.utils.doesMediaExist
import com.google.jetpackcamera.utils.getMultipleImageCaptureIntent
import com.google.jetpackcamera.utils.getSingleImageCaptureIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTestForResult
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.Rule
import org.junit.Test

class CachedImageCaptureDeviceTest {

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)

    @Test
    fun imageCapture_success_navigatesPostCapture() = runMainActivityScenarioTest {
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
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).isDisplayed()
        }

        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()
        composeTestRule.waitForCaptureButton()
    }

    //identical to image_capture_external
    @Test
    fun singleImageCapture_withIntent_savesImmediate() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(PICTURES_DIR_PATH, timeStamp, "jpg")
        val result =
            runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

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
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

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
    fun multipleImageCapture_ExternalIntent_doesntNavigatePostCapture_returnsResultOk() {
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
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }
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
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }
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
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }
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
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }
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