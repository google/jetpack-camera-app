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
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_FAILURE_TAG
import com.google.jetpackcamera.feature.preview.ui.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.feature.preview.ui.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG
import com.google.jetpackcamera.utils.APP_START_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.doesImageFileExist
import com.google.jetpackcamera.utils.getMultipleImageCaptureIntent
import com.google.jetpackcamera.utils.getSingleImageCaptureIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.runMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runScenarioTestForResult
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
    fun image_capture() = runMediaStoreAutoDeleteScenarioTest<MainActivity>(
        mediaUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        filePrefix = "JCA"
    ) {
        // Wait for the capture button to be displayed
        composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
        }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertExists()
            .performClick()
        composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
    }

    @Test
    fun image_capture_external() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(DIR_PATH, timeStamp, "jpg")
        val result =
            runScenarioTestForResult<MainActivity>(
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
        Truth.assertThat(doesImageFileExist(uri, "image")).isTrue()
        deleteFilesInDirAfterTimestamp(DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun image_capture_external_illegal_uri() {
        val uri = Uri.parse("asdfasdf")
        val result =
            runScenarioTestForResult<MainActivity>(
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
        Truth.assertThat(doesImageFileExist(uri, "image")).isFalse()
    }

    @Test
    fun video_capture_during_image_capture_external() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(DIR_PATH, timeStamp, "mp4")
        val result =
            runScenarioTestForResult<MainActivity>(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performTouchInput { longClick() }
                composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED_TAG)
                        .isDisplayed()
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
        Truth.assertThat(doesImageFileExist(uri, "video")).isFalse()
    }

    @Test
    fun multiple_image_capture_external() {
        val timeStamp = System.currentTimeMillis()
        val uriStrings = arrayListOf<String>()
        for (i in 1..3) {
            val uri = getTestUri(DIR_PATH, timeStamp + i.toLong(), "jpg")
            uriStrings.add(uri.toString())
        }
        val result =
            runScenarioTestForResult<MainActivity>(
                getMultipleImageCaptureIntent(
                    uriStrings,
                    MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA
                )
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
                composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
                }
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isNotDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
                composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
                }
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isNotDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()

            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        for (string in uriStrings) {
            Truth.assertThat(doesImageFileExist(Uri.parse(string), "image")).isTrue()
        }
        deleteFilesInDirAfterTimestamp(DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun multiple_image_capture_external_null_uri_list() {
        val timeStamp = System.currentTimeMillis()
        val result =
            runScenarioTestForResult<MainActivity>(
                getMultipleImageCaptureIntent(null, MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(CAPTURE_BUTTON).isDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
                composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
                }
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isNotDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()
                composeTestRule.waitUntil(timeoutMillis = IMAGE_CAPTURE_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
                }
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isNotDisplayed()
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        deleteFilesInDirAfterTimestamp(DIR_PATH, instrumentation, timeStamp)
    }

    @Test
    fun multiple_image_capture_external_null_uri_list_cancel() {
        val timeStamp = System.currentTimeMillis()
        val result =
            runScenarioTestForResult<MainActivity>(
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
    fun multiple_image_capture_external_illegal_uri() {
        val timeStamp = System.currentTimeMillis()
        val uriStrings = arrayListOf<String>()
        uriStrings.add("asdfasdf")
        uriStrings.add(getTestUri(DIR_PATH, timeStamp, "jpg").toString())
        val result =
            runScenarioTestForResult<MainActivity>(
                getMultipleImageCaptureIntent(uriStrings, MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
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
                composeTestRule.waitUntil(timeoutMillis = APP_START_TIMEOUT_MILLIS) {
                    composeTestRule.onNodeWithTag(IMAGE_CAPTURE_FAILURE_TAG).isNotDisplayed()
                }

                composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
                    .assertExists()
                    .performClick()

            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_OK)
        Truth.assertThat(doesImageFileExist(Uri.parse(uriStrings[1]), "image")).isTrue()
        deleteFilesInDirAfterTimestamp(DIR_PATH, instrumentation, timeStamp)
    }

    companion object {
        val DIR_PATH: String =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).path
    }
}
