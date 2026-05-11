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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.ui.BTN_SWITCH_SETTING_CONCURRENT_CAMERA_TAG
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_HDR_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_STREAM_CONFIG_BUTTON
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.VIDEO_CAPTURE_SUCCESS_TAG
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.assume
import com.google.jetpackcamera.utils.getResString
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.setConcurrentCameraModeInSettings
import com.google.jetpackcamera.utils.stateDescriptionMatches
import com.google.jetpackcamera.utils.visitSettingsScreen
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForNodeWithTag
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
            // Enable concurrent camera in settings
            setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)

            // Assert that the flip camera button is visible on preview
            onNodeWithTag(FLIP_CAMERA_BUTTON)
                .assertIsDisplayed()

            // Double check by visiting settings again to verify it is persisted as ON
            visitSettingsScreen {
                onNodeWithTag(BTN_SWITCH_SETTING_CONCURRENT_CAMERA_TAG)
                    .assertExists()
                    .apply {
                        val isCurrentlyOn = fetchSemanticsNode().config.getOrNull(
                            SemanticsProperties.ToggleableState
                        ) == ToggleableState.On
                        assertThat(isCurrentlyOn).isTrue()
                    }
            }
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canBeDisabled() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            // Enable concurrent camera in settings
            setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)

            // Disable it
            setConcurrentCameraModeInSettings(ConcurrentCameraMode.OFF)

            // Verify switch is OFF in settings screen
            visitSettingsScreen {
                onNodeWithTag(BTN_SWITCH_SETTING_CONCURRENT_CAMERA_TAG)
                    .assertExists()
                    .apply {
                        val isCurrentlyOn = fetchSemanticsNode().config.getOrNull(
                            SemanticsProperties.ToggleableState
                        ) == ToggleableState.On
                        assertThat(isCurrentlyOn).isFalse()
                    }
            }
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canFlipCamera() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            // Enable concurrent camera in settings
            setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)

            // Flip camera from preview screen directly (it should be available)
            onNodeWithTag(FLIP_CAMERA_BUTTON)
                .assertExists()
                .performClick()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canSwitchAspectRatio() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            // Enable concurrent camera in settings
            setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)

            // Enter quick settings
            onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                .assertExists()
                .performClick()

            // Click the ratio button inside Quick Settings
            onNodeWithTag(QUICK_SETTINGS_RATIO_BUTTON)
                .assertExists()
                .performClick()

            // Click the 1:1 ratio button
            onNodeWithTag(QUICK_SETTINGS_RATIO_1_1_BUTTON)
                .assertExists()
                .performClick()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_disablesOtherSettings() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            // Enable concurrent camera in settings
            setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)

            // Open Quick Settings bottom sheet
            onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                .assertExists()
                .performClick()

            // Assert the stream config button is disabled
            onNodeWithTag(QUICK_SETTINGS_STREAM_CONFIG_BUTTON)
                .assertExists()
                .assert(isNotEnabled())

            // Assert the HDR button is disabled
            onNodeWithTag(QUICK_SETTINGS_HDR_BUTTON)
                .assertExists()
                .assert(isNotEnabled())

            // Assert the capture mode toggle button is disabled and set to video-only
            onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                .assertExists()
                .assert(isNotEnabled())
                .assert(
                    stateDescriptionMatches(
                        getResString(
                            R.string.quick_settings_description_capture_mode_video_only
                        )
                    )
                )
        }
    }

    @Test
    fun concurrentCameraMode_canRecordVideo() = runConcurrentCameraScenarioTest(
        mediaUriForSavedFiles = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
    ) {
        with(composeTestRule) {
            // Enable concurrent camera in settings
            setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)

            longClickForVideoRecordingCheckingElapsedTime()

            waitForNodeWithTag(VIDEO_CAPTURE_SUCCESS_TAG, VIDEO_CAPTURE_TIMEOUT_MILLIS)
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

            // Navigate to settings screen to check if concurrent camera is supported/enabled
            var isSupported = false
            composeTestRule.visitSettingsScreen {
                onNodeWithTag(BTN_SWITCH_SETTING_CONCURRENT_CAMERA_TAG)
                    .assertExists()
                    .apply {
                        isSupported = isEnabled().matches(fetchSemanticsNode())
                    }
            }

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
