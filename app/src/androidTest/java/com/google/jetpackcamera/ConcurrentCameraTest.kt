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

import android.net.Uri
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.ui.components.capture.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_HDR
import com.google.jetpackcamera.ui.components.capture.carouselOptionTag
import com.google.jetpackcamera.ui.uistateadapter.capture.R as StateR
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.getCurrentCaptureMode
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.selectConcurrentCameraSubMode
import com.google.jetpackcamera.utils.setCaptureMode
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForNodeWithTag
import com.google.jetpackcamera.utils.waitForSnackbarWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val CONCURRENT_SUB_MODE_ID = CaptureSubModeId("concurrent_dual")

@RunWith(AndroidJUnit4::class)
class ConcurrentCameraTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun concurrentCameraMode_canBeEnabled() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            selectConcurrentCameraSubMode(enabled = true)
            onNodeWithTag(CONCURRENT_SUB_MODE_ID.carouselOptionTag)
                .assertExists()
                .assertIsSelected()

            // Assert that the flip camera button is visible on preview
            onNodeWithTag(FLIP_CAMERA_BUTTON)
                .assertIsDisplayed()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canBeDisabled() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            selectConcurrentCameraSubMode(enabled = true)
            selectConcurrentCameraSubMode(enabled = false)
            onNodeWithTag(CaptureSubModeId.DEFAULT.carouselOptionTag)
                .assertExists()
                .assertIsSelected()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canFlipCamera() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            selectConcurrentCameraSubMode(enabled = true)

            // Flip camera from preview screen directly (it should be available)
            onNodeWithTag(FLIP_CAMERA_BUTTON)
                .assertExists()
                .performClick()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_disablesOtherQuickSettings() =
        runConcurrentCameraScenarioTest {
            with(composeTestRule) {
                selectConcurrentCameraSubMode(enabled = true)

                // Open Quick Settings bottom sheet
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                // Assert the HDR row is hidden when concurrent camera sub-mode is active
                onNodeWithTag(ROW_QUICK_SETTINGS_HDR).assertDoesNotExist()

                // Assert the capture mode row does not exist (hidden because locked to video-only)
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
                assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.VIDEO_ONLY)
            }
        }

    @Test
    fun concurrentCameraMode_canRecordVideo() = runConcurrentCameraScenarioTest(
        mediaUriForSavedFiles = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ) {
        with(composeTestRule) {
            selectConcurrentCameraSubMode(enabled = true)

            // The concurrent camera session starts asynchronously after returning from
            // settings. The capture button is disabled until it is running, so wait for it
            // before pressing.
            waitForCaptureButton()

            longClickForVideoRecordingCheckingElapsedTime()

            waitForSnackbarWithText(
                StateR.string.toast_video_capture_success,
                VIDEO_CAPTURE_TIMEOUT_MILLIS
            )
        }
    }

    // Ensures the app has launched and checks that the device supports concurrent camera before
    // running the test.
    private inline fun runConcurrentCameraScenarioTest(
        mediaUriForSavedFiles: Uri? = null,
        expectedMediaFiles: Int = 1,
        crossinline block: ActivityScenario<MainActivity>.() -> Unit
    ) {
        val wrappedBlock: ActivityScenario<MainActivity>.() -> Unit = {
            // Wait for the capture button to be displayed
            composeTestRule.waitForCaptureButton()

            // Switch to VIDEO_ONLY capture mode so the concurrent camera sub-mode option is visible
            // if supported by the device.
            if (composeTestRule.getCurrentCaptureMode() != CaptureMode.VIDEO_ONLY) {
                composeTestRule.setCaptureMode(CaptureMode.VIDEO_ONLY)
                composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            }

            val isSupported = composeTestRule
                .onAllNodesWithTag(CONCURRENT_SUB_MODE_ID.carouselOptionTag)
                .fetchSemanticsNodes()
                .isNotEmpty()

            // Skip the test if the device does not support concurrent camera
            assume().that(isSupported).isTrue()

            block()
        }

        if (mediaUriForSavedFiles != null) {
            runMainActivityMediaStoreAutoDeleteScenarioTest(
                mediaUri = mediaUriForSavedFiles,
                expectedNumFiles = expectedMediaFiles,
                block = wrappedBlock
            )
        } else {
            runMainActivityScenarioTest(block = wrappedBlock)
        }
    }
}
