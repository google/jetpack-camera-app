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
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.ImageCaptureDeviceTest.Companion.DIR_PATH
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.getCurrentCaptureMode
import com.google.jetpackcamera.utils.getHdrToggleState
import com.google.jetpackcamera.utils.getSingleImageCaptureIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.isHdrToggleEnabled
import com.google.jetpackcamera.utils.runScenarioTest
import com.google.jetpackcamera.utils.runScenarioTestForResult
import com.google.jetpackcamera.utils.setCaptureMode
import com.google.jetpackcamera.utils.setConcurrentCameraMode
import com.google.jetpackcamera.utils.setHdrEnabled
import com.google.jetpackcamera.utils.unFocusQuickSetting
import com.google.jetpackcamera.utils.visitQuickSettings
import com.google.jetpackcamera.utils.waitForNodeWithTag
import com.google.jetpackcamera.utils.waitForStartup
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class CaptureModeSettingsTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val uiDevice = UiDevice.getInstance(instrumentation)
    private fun ComposeTestRule.checkCaptureMode(captureMode: CaptureMode? = null) =
        visitQuickSettings {
            waitUntil(timeoutMillis = 1000) {
                onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE).isDisplayed()
            }
            captureMode?.let {
                assertThat(getCurrentCaptureMode()).isEqualTo(captureMode)
            }
        }

    @Test
    fun can_set_capture_mode_in_quick_settings() {
        runScenarioTest<MainActivity> {
            composeTestRule.waitForStartup()
            composeTestRule.visitQuickSettings {
                setCaptureMode(CaptureMode.IMAGE_ONLY)
                checkCaptureMode(CaptureMode.IMAGE_ONLY)

                setCaptureMode(CaptureMode.VIDEO_ONLY)
                checkCaptureMode(CaptureMode.VIDEO_ONLY)

                setCaptureMode(CaptureMode.STANDARD)
                checkCaptureMode(CaptureMode.STANDARD)
            }
        }
    }

    @Test
    fun concurrent_only_supports_video_capture_mode() {
        runScenarioTest<MainActivity> {
            composeTestRule.waitForStartup()
            composeTestRule.visitQuickSettings {
                // verify concurrent is supported. if not supported, skip test
                waitForNodeWithTag(tag = QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                setConcurrentCameraMode(ConcurrentCameraMode.DUAL)

                // capture mode should now be video only
                checkCaptureMode(CaptureMode.VIDEO_ONLY)

                // should not be able to switch between capture modes
                onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                    .assertExists()
                    .assertIsNotEnabled()

                // set concurrent camera mode back to off
                setConcurrentCameraMode(ConcurrentCameraMode.OFF)

                // capture mode should reset to standard
                checkCaptureMode(CaptureMode.STANDARD)
            }
        }
    }

    @Test
    fun image_only_disables_concurrent_camera() {
        runScenarioTest<MainActivity> {
            composeTestRule.waitForStartup()
            composeTestRule.visitQuickSettings {
                // verify concurrent is supported. if not supported, skip test
                waitForNodeWithTag(tag = QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                setConcurrentCameraMode(ConcurrentCameraMode.OFF)

                // capture mode should now be image only
                setCaptureMode(CaptureMode.IMAGE_ONLY)
                checkCaptureMode(CaptureMode.IMAGE_ONLY)

                // should not be able to enable concurrent
                onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                    .assertExists()
                    .assertIsNotEnabled()

                // reset caputre mode to standard
                setCaptureMode(CaptureMode.STANDARD)
                checkCaptureMode(CaptureMode.STANDARD)

                // concurrent should be enabled again
                onNodeWithTag(
                    QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
                ).assertExists().assertIsEnabled()
            }
        }
    }

    @Test
    fun hdr_supports_image_only() {
        runScenarioTest<MainActivity> {
            composeTestRule.waitForStartup()
            composeTestRule.setHdrEnabled(true)
            // check that switch only supports image
            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            assume().that(composeTestRule.isHdrToggleEnabled()).isFalse()
            assume().that(composeTestRule.getHdrToggleState()).isEqualTo(CaptureMode.IMAGE_ONLY)

            composeTestRule.visitQuickSettings {
                waitForNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                // capture mode should be image only
                assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.IMAGE_ONLY)
            }
            // should not be able to change capture mode
            assertThat(composeTestRule.isHdrToggleEnabled()).isFalse()
            composeTestRule.setHdrEnabled(false)
            composeTestRule.checkCaptureMode(CaptureMode.STANDARD)
        }
    }

    @Test
    fun hdr_supports_video_only() {
        runScenarioTest<MainActivity> {
            composeTestRule.waitForStartup()
            composeTestRule.setHdrEnabled(true)
            // check that switch only supports image
            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            // should not be able use capture toggle
            assume().that(composeTestRule.isHdrToggleEnabled()).isFalse()
            assume().that(composeTestRule.getHdrToggleState()).isEqualTo(CaptureMode.VIDEO_ONLY)

            composeTestRule.visitQuickSettings {
                waitForNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                // capture mode should be image only
                checkCaptureMode(CaptureMode.VIDEO_ONLY)
            }
            assertThat(composeTestRule.isHdrToggleEnabled()).isFalse()

            composeTestRule.setHdrEnabled(false)
            composeTestRule.checkCaptureMode(CaptureMode.STANDARD)
        }
    }

    @Test
    fun hdr_supports_image_and_video() {
        runScenarioTest<MainActivity> {
            with(composeTestRule) {
                composeTestRule.waitForStartup()

                // enable hdr
                setHdrEnabled(true)

                // check that switch supports both image and video
                waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
                assume().that(isHdrToggleEnabled()).isTrue()

                // should default to video when both are available
                assertThat(getHdrToggleState()).isEqualTo(CaptureMode.VIDEO_ONLY)

                visitQuickSettings {
                    checkCaptureMode(CaptureMode.VIDEO_ONLY)
                    setHdrEnabled(false)

                    // capture mode should return to standard when we turn off hdr
                    checkCaptureMode(CaptureMode.STANDARD)

                    setCaptureMode(CaptureMode.IMAGE_ONLY)
                    setHdrEnabled(true)
                    // capture mode should remain as image only, since device supports ultrahdr image
                    checkCaptureMode(CaptureMode.IMAGE_ONLY)
                }
                // if both are supported, should keep the current, non-standard capture mode
                assertThat(getHdrToggleState()).isEqualTo(CaptureMode.IMAGE_ONLY)

                // turn on video only hdr
                onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performClick()
                assertThat(getHdrToggleState()).isEqualTo(CaptureMode.VIDEO_ONLY)

                visitQuickSettings {
                    // capture mode should be video only now
                    checkCaptureMode(CaptureMode.VIDEO_ONLY)
                    onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE).performClick()
                    onNodeWithTag(
                        BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_OPTION_STANDARD
                    ).assertIsNotEnabled()
                    unFocusQuickSetting()

                    setHdrEnabled(false)
                    checkCaptureMode(CaptureMode.STANDARD)
                }
            }
        }
    }

    @Test
    fun image_intent_disables_capture_settings() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(DIR_PATH, timeStamp, "jpg")
        val result =
            runScenarioTestForResult<MainActivity>(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForStartup()
                composeTestRule.visitQuickSettings {
                    checkCaptureMode(CaptureMode.IMAGE_ONLY)

                    // should not be able to change quick settings
                    onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                        .assertExists()
                        .assertIsNotEnabled()
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun image_intent_disables_hdr_toggle() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(DIR_PATH, timeStamp, "jpg")
        val result =
            runScenarioTestForResult<MainActivity>(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForStartup()
                composeTestRule.visitQuickSettings {
                    setHdrEnabled(true)
                    checkCaptureMode(CaptureMode.IMAGE_ONLY)
                }
                assertThat(composeTestRule.isHdrToggleEnabled()).isFalse()
                assertThat(
                    composeTestRule.getHdrToggleState()
                ).isEqualTo(CaptureMode.IMAGE_ONLY)

                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun video_intent_disables_capture_settings() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(VideoRecordingDeviceTest.Companion.DIR_PATH, timeStamp, "mp4")
        val result =
            runScenarioTestForResult<MainActivity>(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForStartup()
                composeTestRule.visitQuickSettings {
                    checkCaptureMode(CaptureMode.VIDEO_ONLY)

                    // should not be able to change quick settings
                    onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                        .assertExists()
                        .assertIsNotEnabled()
                }
                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun video_intent_disables_hdr_toggle() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(VideoRecordingDeviceTest.Companion.DIR_PATH, timeStamp, "mp4")
        val result =
            runScenarioTestForResult<MainActivity>(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForStartup()
                composeTestRule.visitQuickSettings {
                    setHdrEnabled(true)
                    checkCaptureMode(CaptureMode.VIDEO_ONLY)
                }
                assertThat(composeTestRule.isHdrToggleEnabled()).isFalse()
                assertThat(
                    composeTestRule.getHdrToggleState()
                ).isEqualTo(CaptureMode.VIDEO_ONLY)

                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }
}
