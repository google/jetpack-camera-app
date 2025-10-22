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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.isNotDisplayed
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.MOVIES_DIR_PATH
import com.google.jetpackcamera.utils.PICTURES_DIR_PATH
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.getCaptureModeToggleState
import com.google.jetpackcamera.utils.getSingleImageCaptureIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.isCaptureModeToggleEnabled
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTestForResult
import com.google.jetpackcamera.utils.searchForQuickSetting
import com.google.jetpackcamera.utils.setCaptureMode
import com.google.jetpackcamera.utils.setConcurrentCameraMode
import com.google.jetpackcamera.utils.setHdrEnabled
import com.google.jetpackcamera.utils.tapStartLockedVideoRecording
import com.google.jetpackcamera.utils.unFocusQuickSetting
import com.google.jetpackcamera.utils.visitQuickSettings
import com.google.jetpackcamera.utils.wait
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForNodeWithTag
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
        visitQuickSettings(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE) {
            captureMode?.let {
                assertThat(getCaptureModeToggleState()).isEqualTo(captureMode)
            }
        }

    private fun switchStaysUnchanged(initialCaptureMode: CaptureMode) {
        check(initialCaptureMode != CaptureMode.STANDARD) {
            "capture mode should be IMAGE_ONLY or VIDEO_ONLY."
        }
        assertThat(composeTestRule.getCaptureModeToggleState() == initialCaptureMode).isTrue()
    }

    private fun ComposeTestRule.initializeCaptureSwitch(
        captureMode: CaptureMode = CaptureMode.IMAGE_ONLY
    ) {
        // Test that the JCA switch is visible on the screen
        composeTestRule.waitForCaptureButton()

        check(
            captureMode != CaptureMode.STANDARD
        ) { "capture mode should be IMAGE_ONLY or VIDEO_ONLY." }
        waitForCaptureButton()

        if ((getCaptureModeToggleState()) != captureMode) {
            setCaptureMode(captureMode)
        }

        waitUntil(DEFAULT_TIMEOUT_MILLIS) {
            onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).isDisplayed()
        }

        onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).assertExists()
    }

    @Test
    fun can_set_capture_mode_in_quick_settings() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.visitQuickSettings(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE) {
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
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.visitQuickSettings(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE) {
                // verify concurrent is supported. if not supported, skip test
                waitForNodeWithTag(tag = QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                setConcurrentCameraMode(ConcurrentCameraMode.DUAL)

                // capture mode should now be video only
                checkCaptureMode(CaptureMode.VIDEO_ONLY)

                // should not be able to switch between capture modes
                onNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                    .assertExists()
                    .assertIsNotEnabled()
            }
            // verify switch is disabled and locked on video only
            assertThat(composeTestRule.isCaptureModeToggleEnabled()).isFalse()
            assertThat(
                composeTestRule.getCaptureModeToggleState()
            ).isEqualTo(CaptureMode.VIDEO_ONLY)

            composeTestRule.visitQuickSettings(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE) {
                // set concurrent camera mode back to off
                setConcurrentCameraMode(ConcurrentCameraMode.OFF)

                // capture mode should reset to standard
                checkCaptureMode(CaptureMode.STANDARD)
            }
        }
    }

    @Test
    fun image_only_disables_concurrent_camera() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.visitQuickSettings {
                // verify concurrent is supported. if not supported, skip test
                setConcurrentCameraMode(ConcurrentCameraMode.OFF)

                // capture mode should now be image only
                setCaptureMode(CaptureMode.IMAGE_ONLY)
                checkCaptureMode(CaptureMode.IMAGE_ONLY)

                searchForQuickSetting(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                // should not be able to enable concurrent
                onNodeWithTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                    .assertExists()
                    .assertIsNotEnabled()

                // reset capture mode to standard
                setCaptureMode(CaptureMode.STANDARD)
                checkCaptureMode(CaptureMode.STANDARD)

                // concurrent should be enabled again
                searchForQuickSetting(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON)
                onNodeWithTag(
                    QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
                ).assertExists().assertIsEnabled()
            }
        }
    }

    @Test
    fun hdr_supports_image_only() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.initializeCaptureSwitch()
            composeTestRule.setHdrEnabled(true)

            // check that switch is disabled and only supports image
            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            assume().that(composeTestRule.isCaptureModeToggleEnabled()).isFalse()
            assume().that(composeTestRule.getCaptureModeToggleState())
                .isEqualTo(CaptureMode.IMAGE_ONLY)

            // capture mode should be image only
            composeTestRule.visitQuickSettings(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE) {
                assertThat(getCaptureModeToggleState()).isEqualTo(CaptureMode.IMAGE_ONLY)
            }
            // should not be able to change capture mode
            assertThat(composeTestRule.isCaptureModeToggleEnabled()).isFalse()
            composeTestRule.setHdrEnabled(false)
            composeTestRule.checkCaptureMode(CaptureMode.STANDARD)
        }
    }

    @Test
    fun hdr_supports_video_only() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.setHdrEnabled(true)
            // check that switch is disabled and only supports video
            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            // should not be able use capture toggle
            assume().that(composeTestRule.isCaptureModeToggleEnabled()).isFalse()
            assume().that(composeTestRule.getCaptureModeToggleState())
                .isEqualTo(CaptureMode.VIDEO_ONLY)

            composeTestRule.visitQuickSettings {
                waitForNodeWithTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                // capture mode should be image only
                checkCaptureMode(CaptureMode.VIDEO_ONLY)
            }
            assertThat(composeTestRule.isCaptureModeToggleEnabled()).isFalse()

            composeTestRule.setHdrEnabled(false)
            composeTestRule.checkCaptureMode(CaptureMode.STANDARD)
        }
    }

    @Test
    fun hdr_supports_image_and_video() {
        runMainActivityScenarioTest {
            with(composeTestRule) {
                composeTestRule.waitForCaptureButton()

                // enable hdr
                setHdrEnabled(true)

                // check that switch supports both image and video
                waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
                assume().that(isCaptureModeToggleEnabled()).isTrue()

                // should default to video when both are available
                assertThat(getCaptureModeToggleState()).isEqualTo(CaptureMode.VIDEO_ONLY)

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
                assertThat(getCaptureModeToggleState()).isEqualTo(CaptureMode.IMAGE_ONLY)

                // turn on video only hdr
                onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performClick()
                assertThat(getCaptureModeToggleState()).isEqualTo(CaptureMode.VIDEO_ONLY)

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
        val uri = getTestUri(PICTURES_DIR_PATH, timeStamp, "jpg")
        val result =
            runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
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
    fun image_intent_disables_capture_mode_toggle() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(PICTURES_DIR_PATH, timeStamp, "jpg")
        val result =
            runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_IMAGE_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                composeTestRule.visitQuickSettings {
                    checkCaptureMode(CaptureMode.IMAGE_ONLY)
                }
                assertThat(composeTestRule.isCaptureModeToggleEnabled()).isFalse()
                assertThat(
                    composeTestRule.getCaptureModeToggleState()
                ).isEqualTo(CaptureMode.IMAGE_ONLY)

                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun video_intent_disables_capture_settings() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(MOVIES_DIR_PATH, timeStamp, "mp4")
        val result =
            runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
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
    fun video_intent_disables_capture_mode_toggle() {
        val timeStamp = System.currentTimeMillis()
        val uri = getTestUri(MOVIES_DIR_PATH, timeStamp, "mp4")
        val result =
            runMainActivityScenarioTestForResult(
                getSingleImageCaptureIntent(uri, MediaStore.ACTION_VIDEO_CAPTURE)
            ) {
                // Wait for the capture button to be displayed
                composeTestRule.waitForCaptureButton()
                composeTestRule.visitQuickSettings {
                    checkCaptureMode(CaptureMode.VIDEO_ONLY)
                }
                assertThat(composeTestRule.isCaptureModeToggleEnabled()).isFalse()
                assertThat(
                    composeTestRule.getCaptureModeToggleState()
                ).isEqualTo(CaptureMode.VIDEO_ONLY)

                uiDevice.pressBack()
            }
        Truth.assertThat(result.resultCode).isEqualTo(Activity.RESULT_CANCELED)
    }

    @Test
    fun jcaSwitch_stateChangesOnTap() = runMainActivityScenarioTest {
        composeTestRule.waitForCaptureButton()

        composeTestRule.initializeCaptureSwitch()
        val initialCaptureMode = composeTestRule.getCaptureModeToggleState()

        // should be different from initial capture mode
        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performClick()
        composeTestRule.waitUntil {
            composeTestRule.getCaptureModeToggleState() != initialCaptureMode
        }

        // should now be  she same as the initial capture mode.
        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performClick()
        composeTestRule.waitUntil {
            composeTestRule.getCaptureModeToggleState() == initialCaptureMode
        }
    }

    @Test
    fun jcaSwitch_stateDoesNotChangeWhenDragging() = runMainActivityScenarioTest {
        // Test that the state of the JCA switch does not change while dragged
        composeTestRule.waitForCaptureButton()
        composeTestRule.initializeCaptureSwitch()
        val initialCaptureMode = composeTestRule.getCaptureModeToggleState()
        val captureToggleNode = composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
        val offsetToSwitch = when (initialCaptureMode) {
            CaptureMode.STANDARD -> TODO("App should not be in Standard Capture Mode")
            CaptureMode.VIDEO_ONLY -> -400f // move left to switch to image
            CaptureMode.IMAGE_ONLY -> 400f // move right to switch to video
        }

        captureToggleNode.assertExists()
            .performTouchInput {
                down(center)
            }

        composeTestRule.wait(500L)

        switchStaysUnchanged(initialCaptureMode)

        // should not change value while dragging
        captureToggleNode.performTouchInput {
            moveBy(delta = Offset(offsetToSwitch, 0f))
        }

        composeTestRule.wait(500L)
        switchStaysUnchanged(initialCaptureMode)

        // should change value after release
        captureToggleNode.performTouchInput {
            up()
        }
        composeTestRule.waitUntil {
            initialCaptureMode != composeTestRule.getCaptureModeToggleState()
        }
    }

    @Test
    fun jcaSwitch_isNotVisibleWhileRecording(): Unit =
        runMainActivityMediaStoreAutoDeleteScenarioTest(
            mediaUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ) {
            val timeStamp = System.currentTimeMillis()

            composeTestRule.waitForCaptureButton()
            composeTestRule.initializeCaptureSwitch(captureMode = CaptureMode.VIDEO_ONLY)
            // start recording
            composeTestRule.tapStartLockedVideoRecording()
            // check that recording
            composeTestRule.waitUntil {
                composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).isNotDisplayed()
            }

            // stop recording
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

            composeTestRule.waitUntil {
                composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).isDisplayed() &&
                    composeTestRule.getCaptureModeToggleState() == CaptureMode.VIDEO_ONLY
            }

            deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timeStamp)
        }
}
