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
import com.google.jetpackcamera.utils.debugExtra
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.mediaStoreEntryExistsAfterTimestamp
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.wait
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

    private fun newImageMediaExists(): Boolean = mediaStoreEntryExistsAfterTimestamp(
        instrumentation,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        timestamp
    )

    private fun newVideoMediaExists(): Boolean = mediaStoreEntryExistsAfterTimestamp(
        instrumentation,
        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        timestamp
    )

    @Before
    fun setup() {
        timestamp = System.currentTimeMillis()
    }

    @After
    fun cleanup() {
        deleteFilesInDirAfterTimestamp(JCA_MEDIA_DIR_PATH, instrumentation, timestamp)
        deleteFilesInDirAfterTimestamp(PICTURES_DIR_PATH, instrumentation, timestamp)
        deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timestamp)
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

    @Test
    fun captureImage_navigatesPostcapture_canSaveCachedImage() =
        runMainActivityScenarioTest(cacheExtra) {
            // Wait for the capture button to be displayed
            composeTestRule.waitForCaptureButton()

            assertFalse(newImageMediaExists())

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
            assertTrue(newImageMediaExists())
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

            assertFalse(newVideoMediaExists())
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

            assertTrue(newVideoMediaExists())
            composeTestRule.onNodeWithTag(BUTTON_POST_CAPTURE_EXIT).performClick()
            composeTestRule.waitForCaptureButton()
        }

    @Test
    fun postcapture_canDeleteSavedImage() = runMainActivityScenarioTest(extras = debugExtra) {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        assertFalse(newImageMediaExists())
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()
        composeTestRule.waitUntil(IMAGE_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(IMAGE_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        assertTrue(newImageMediaExists())
        // enter postcapture via imagewell and delete recent capture
        enterImageWellAndDelete(VIEWER_POST_CAPTURE_IMAGE)

        composeTestRule.waitUntil(timeoutMillis = SAVE_MEDIA_TIMEOUT_MILLIS) {
            !newImageMediaExists()
        }
    }

    @Test
    fun postcapture_canDeleteSavedVideo(): Unit = runMainActivityScenarioTest(extras = debugExtra) {
        // Wait for the capture button to be displayed
        composeTestRule.waitForCaptureButton()
        assertFalse(newVideoMediaExists())
        composeTestRule.longClickForVideoRecordingCheckingElapsedTime()

        composeTestRule.waitUntil(VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            composeTestRule.onNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG).isDisplayed()
        }
        assertTrue(newVideoMediaExists())
        // enter postcapture via imagewell and delete recent capture
        enterImageWellAndDelete(VIEWER_POST_CAPTURE_VIDEO)
        composeTestRule.waitUntil(timeoutMillis = VIDEO_CAPTURE_TIMEOUT_MILLIS) {
            !newVideoMediaExists()
        }
    }
}
