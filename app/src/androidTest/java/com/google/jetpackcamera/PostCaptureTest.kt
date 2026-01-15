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

import android.provider.MediaStore
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_DELETE
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_EXIT
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_SAVE
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS
import com.google.jetpackcamera.feature.postcapture.ui.SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS
import com.google.jetpackcamera.feature.postcapture.ui.VIEWER_POST_CAPTURE_IMAGE
import com.google.jetpackcamera.feature.postcapture.ui.VIEWER_POST_CAPTURE_VIDEO
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.IMAGE_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.ui.components.capture.IMAGE_WELL_TAG
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.utils.IMAGE_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.IMAGE_WELL_LOAD_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.JCA_MEDIA_DIR_PATH
import com.google.jetpackcamera.utils.MOVIES_DIR_PATH
import com.google.jetpackcamera.utils.PICTURES_DIR_PATH
import com.google.jetpackcamera.utils.SAVE_MEDIA_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.cacheExtra
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.mediaStoreEntryExistsAfterTimestamp
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.wait
import com.google.jetpackcamera.utils.waitForCaptureButton
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PostCaptureTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private var testTimestamp: Long = Long.MAX_VALUE

    private fun newImageMediaExists(timestamp: Long = testTimestamp): Boolean =
        mediaStoreEntryExistsAfterTimestamp(
            instrumentation,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            timestamp
        )

    private fun newVideoMediaExists(timestamp: Long = testTimestamp): Boolean =
        mediaStoreEntryExistsAfterTimestamp(
            instrumentation,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            timestamp
        )

    @Before
    fun setup() {
        testTimestamp = System.currentTimeMillis()
    }

    @After
    fun cleanup() {
        deleteFilesInDirAfterTimestamp(JCA_MEDIA_DIR_PATH, instrumentation, testTimestamp)
        deleteFilesInDirAfterTimestamp(PICTURES_DIR_PATH, instrumentation, testTimestamp)
        deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, testTimestamp)
    }

    private fun enterImageWellAndDelete(recentCaptureViewerTag: String) {
        // enter postcapture via imagewell
        composeTestRule.waitUntil(IMAGE_WELL_LOAD_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).isDisplayed()
        }
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertExists().performClick()

        // most recent capture tag
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(recentCaptureViewerTag).isDisplayed()
        }

        // delete most recent capture
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).isDisplayed()
        }
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).assertExists().performClick()

        // wait for capture button after automatically exiting post capture
        composeTestRule.waitForCaptureButton()
    }

    private fun enterImageWellAndSave(recentCaptureViewerTag: String) {
        // enter postcapture via imagewell
        composeTestRule.waitUntil(IMAGE_WELL_LOAD_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).isDisplayed()
        }
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG).assertExists().performClick()

        // most recent capture tag
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(recentCaptureViewerTag).isDisplayed()
        }

        // delete most recent capture
        composeTestRule.waitUntil {
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_DELETE).isDisplayed()
        }
        composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).assertExists().performClick()
    }

    @Test
    fun captureImage_navigatesPostcapture_canSaveCachedImage() =
        runMainActivityScenarioTest(cacheExtra) {
            // Wait for the capture button to be displayed
            composeTestRule.waitForCaptureButton()

            assertThat(newImageMediaExists()).isFalse()

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
            composeTestRule.waitUntil(timeoutMillis = SAVE_MEDIA_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS)
                    .isDisplayed()
            }
            assertThat(newImageMediaExists()).isTrue()
        }

    @Test
    fun captureVideo_navigatesPostcapture_canSaveCachedVideo(): Unit =
        runMainActivityScenarioTest(extras = cacheExtra) {
            // Wait for the capture button to be displayed
            composeTestRule.waitForCaptureButton()
            composeTestRule.wait(500L)
            composeTestRule.longClickForVideoRecordingCheckingElapsedTime()

            // navigate to postcapture screen
            composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(VIEWER_POST_CAPTURE_VIDEO).isDisplayed()
            }
            composeTestRule.waitUntil {
                composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).isDisplayed()
            }

            assertThat(newVideoMediaExists()).isFalse()
            // save video
            composeTestRule.waitUntil {
                composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).isDisplayed()
            }
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_SAVE).performClick()

            // Wait for video save success message
            composeTestRule.waitUntil(timeoutMillis = SAVE_MEDIA_TIMEOUT_MILLIS) {
                composeTestRule.onNodeWithTag(SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS)
                    .isDisplayed()
            }

            assertThat(newVideoMediaExists()).isTrue()
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()
            composeTestRule.waitForCaptureButton()
        }

    @Test
    fun postcapture_canDeleteSavedImage() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        assertThat(newImageMediaExists()).isFalse()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()
        composeTestRule.waitUntil(IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        assertThat(newImageMediaExists()).isTrue()
        // enter postcapture via imagewell and delete recent capture
        enterImageWellAndDelete(VIEWER_POST_CAPTURE_IMAGE)

        composeTestRule.waitUntil(timeoutMillis = SAVE_MEDIA_TIMEOUT_MILLIS) {
            !newImageMediaExists()
        }
    }

    @Test
    fun postcapture_canDeleteSavedVideo(): Unit = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        assertThat(newVideoMediaExists()).isFalse()
        composeTestRule.longClickForVideoRecordingCheckingElapsedTime()

        composeTestRule.waitUntil(VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        assertThat(newVideoMediaExists()).isTrue()
        // enter postcapture via imagewell and delete recent capture
        enterImageWellAndDelete(VIEWER_POST_CAPTURE_VIDEO)
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            !newVideoMediaExists()
        }
    }

    @Test
    fun postcapture_canCopySavedImage() = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        assertThat(newImageMediaExists()).isFalse()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()
        composeTestRule.waitUntil(IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        assertThat(newImageMediaExists()).isTrue()
        // enter postcapture via imagewell and save recent capture
        val newTimestamp = System.currentTimeMillis()

        enterImageWellAndSave(VIEWER_POST_CAPTURE_IMAGE)
        composeTestRule.waitUntil(SAVE_MEDIA_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(SNACKBAR_POST_CAPTURE_IMAGE_SAVE_SUCCESS).isDisplayed()
        }
        composeTestRule.waitUntil(timeoutMillis = SAVE_MEDIA_TIMEOUT_MILLIS) {
            newImageMediaExists(newTimestamp)
        }
    }

    @Test
    fun postcapture_canCopySavedVideo(): Unit = runMainActivityScenarioTest {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        assertThat(newVideoMediaExists()).isFalse()
        composeTestRule.longClickForVideoRecordingCheckingElapsedTime()

        composeTestRule.waitUntil(VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        assertThat(newVideoMediaExists()).isTrue()
        // enter postcapture via imagewell and save recent capture
        val newTimestamp = System.currentTimeMillis()
        enterImageWellAndSave(VIEWER_POST_CAPTURE_VIDEO)
        composeTestRule.waitUntil(SAVE_MEDIA_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(SNACKBAR_POST_CAPTURE_VIDEO_SAVE_SUCCESS).isDisplayed()
        }
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            newVideoMediaExists(newTimestamp)
        }
    }
}
