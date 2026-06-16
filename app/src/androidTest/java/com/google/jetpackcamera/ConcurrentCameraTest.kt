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
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.isEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.R as SettingsR
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_FLASH_OPTION_LLB_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FLASH_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FPS_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_VIDEO_STABILIZATION_TAG
import com.google.jetpackcamera.settings.ui.BTN_SWITCH_SETTING_CONCURRENT_CAMERA_TAG
import com.google.jetpackcamera.settings.ui.CLOSE_BUTTON
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_HDR_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.ui.components.capture.R as CaptureR
import com.google.jetpackcamera.ui.uistateadapter.capture.R as StateR
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.getResString
import com.google.jetpackcamera.utils.longClickForVideoRecordingCheckingElapsedTime
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.searchForQuickSetting
import com.google.jetpackcamera.utils.setConcurrentCameraModeInSettings
import com.google.jetpackcamera.utils.stateDescriptionMatches
import com.google.jetpackcamera.utils.visitSettingsScreen
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForSnackbarWithText
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
            // Visit settings, enable it, and assert it is ON in settings
            visitSettingsScreen {
                setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)
                onNodeWithTag(BTN_SWITCH_SETTING_CONCURRENT_CAMERA_TAG)
                    .assertExists()
                    .assertIsOn()
            }

            // Assert that the flip camera button is visible on preview
            onNodeWithTag(FLIP_CAMERA_BUTTON)
                .assertIsDisplayed()
        }
    }

    @Test
    fun concurrentCameraMode_whenEnabled_canBeDisabled() = runConcurrentCameraScenarioTest {
        with(composeTestRule) {
            // Visit settings, toggle it on, then off, and assert off.
            visitSettingsScreen {
                setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)
                setConcurrentCameraModeInSettings(ConcurrentCameraMode.OFF)
                onNodeWithTag(BTN_SWITCH_SETTING_CONCURRENT_CAMERA_TAG)
                    .assertExists()
                    .assertIsOff()
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
    fun concurrentCameraMode_whenEnabled_disablesOtherQuickSettings() =
        runConcurrentCameraScenarioTest {
            with(composeTestRule) {
                // Visit settings, toggle concurrent camera, and assert stream config is disabled
                visitSettingsScreen {
                    setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)
                    onNodeWithTag(BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG)
                        .assertExists()
                        .assert(isNotEnabled())
                }

                // Open Quick Settings bottom sheet
                onNodeWithTag(QUICK_SETTINGS_DROP_DOWN)
                    .assertExists()
                    .performClick()

                // Assert the HDR button is disabled
                searchForQuickSetting(QUICK_SETTINGS_HDR_BUTTON)
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
                                CaptureR.string.quick_settings_description_capture_mode_video_only
                            )
                        )
                    )
            }
        }

    @Test
    fun concurrentCameraMode_whenEnabled_disablesOtherSettingsInSettingsScreen() =
        runConcurrentCameraScenarioTest {
            with(composeTestRule) {
                // Visit settings, enable concurrent camera, and assert other settings are disabled
                visitSettingsScreen {
                    setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)

                    // 1. Assert Stream Config is disabled
                    onNodeWithTag(BTN_OPEN_DIALOG_SETTING_STREAM_CONFIG_TAG)
                        .assertExists()
                        .assert(isNotEnabled())

                    // 2. Assert Video Stabilization is disabled
                    onNodeWithTag(BTN_OPEN_DIALOG_SETTING_VIDEO_STABILIZATION_TAG)
                        .assertExists()
                        .assert(isNotEnabled())

                    // 3. Assert Flash Low Light Boost option is disabled in dialog
                    onNodeWithTag(BTN_OPEN_DIALOG_SETTING_FLASH_TAG)
                        .assertExists()
                        .assert(isEnabled())
                        .performClick()

                    onNodeWithTag(BTN_DIALOG_FLASH_OPTION_LLB_TAG)
                        .assertExists()
                        .assert(isNotEnabled())

                    onNodeWithTag(CLOSE_BUTTON)
                        .assertExists()
                        .performClick()

                    // 4. Assert FPS setting is disabled
                    onNodeWithTag(BTN_OPEN_DIALOG_SETTING_FPS_TAG)
                        .assertExists()
                        .assert(isNotEnabled())
                }
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

            // Navigate to settings screen to check if concurrent camera is supported/enabled
            var isSupported = false
            var isDeviceUnsupported = false
            composeTestRule.visitSettingsScreen {
                onNodeWithTag(BTN_SWITCH_SETTING_CONCURRENT_CAMERA_TAG)
                    .assertExists()
                    .apply {
                        isSupported = isEnabled().matches(fetchSemanticsNode())
                    }

                if (!isSupported) {
                    val deviceUnsupportedMessage = getResString(SettingsR.string.device_unsupported)
                    val concurrentPrefix =
                        getResString(SettingsR.string.concurrent_camera_rationale_prefix)
                    val expectedMessage = String.format(deviceUnsupportedMessage, concurrentPrefix)

                    // Check if the "device unsupported" message is displayed
                    isDeviceUnsupported = onAllNodesWithText(expectedMessage)
                        .fetchSemanticsNodes()
                        .isNotEmpty()
                }
            }

            if (!isSupported) {
                // Skip the test if the device physically doesn't support concurrent camera
                assume().that(isDeviceUnsupported).isFalse()
                throw AssertionError(
                    "Concurrent camera is disabled in settings, but the device supports it (disabled due to conflict)."
                )
            }

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
