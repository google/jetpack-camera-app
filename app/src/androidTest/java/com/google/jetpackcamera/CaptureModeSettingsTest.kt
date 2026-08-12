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
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.components.capture.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.ui.components.capture.ROW_QUICK_SETTINGS_CAPTURE_MODE
import com.google.jetpackcamera.utils.DEFAULT_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.MOVIES_DIR_PATH
import com.google.jetpackcamera.utils.PICTURES_DIR_PATH
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.deleteFilesInDirAfterTimestamp
import com.google.jetpackcamera.utils.getCaptureModeToggleState
import com.google.jetpackcamera.utils.getCurrentCaptureMode
import com.google.jetpackcamera.utils.getSingleImageCaptureIntent
import com.google.jetpackcamera.utils.getTestUri
import com.google.jetpackcamera.utils.isCaptureModeToggleEnabled
import com.google.jetpackcamera.utils.runMainActivityMediaStoreAutoDeleteScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.runMainActivityScenarioTestForResult
import com.google.jetpackcamera.utils.setCaptureMode
import com.google.jetpackcamera.utils.setConcurrentCameraModeInSettings
import com.google.jetpackcamera.utils.setHdrEnabled
import com.google.jetpackcamera.utils.tapStartLockedVideoRecording
import com.google.jetpackcamera.utils.visitQuickSettings
import com.google.jetpackcamera.utils.wait
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForCaptureModeToggleState
import com.google.jetpackcamera.utils.waitForNodeWithTag
import com.google.jetpackcamera.utils.waitForNodeWithTagToDisappear
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
    private fun ComposeTestRule.checkCaptureModeSettingState(captureMode: CaptureMode? = null) =
        visitQuickSettings {
            captureMode?.let {
                assertThat(getCurrentCaptureMode()).isEqualTo(captureMode)
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

        if ((getCurrentCaptureMode()) != captureMode) {
            setCaptureMode(captureMode)
        }

        waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON, DEFAULT_TIMEOUT_MILLIS)

        onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).assertExists()
    }

    private fun flip(mode: CaptureMode): CaptureMode {
        require(mode == CaptureMode.IMAGE_ONLY || mode == CaptureMode.VIDEO_ONLY)
        return if (mode == CaptureMode.IMAGE_ONLY) {
            CaptureMode.VIDEO_ONLY
        } else {
            CaptureMode.IMAGE_ONLY
        }
    }

    @Test
    fun can_set_capture_mode_to_image_only_in_quick_settings() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.setCaptureMode(CaptureMode.IMAGE_ONLY)
            composeTestRule.checkCaptureModeSettingState(CaptureMode.IMAGE_ONLY)

            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
            }
        }
    }

    @Test
    fun can_set_capture_mode_to_video_only_in_quick_settings() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.setCaptureMode(CaptureMode.VIDEO_ONLY)
            composeTestRule.checkCaptureModeSettingState(CaptureMode.VIDEO_ONLY)

            composeTestRule.visitQuickSettings {
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
            }
        }
    }

    @Test
    fun concurrent_only_supports_video_capture_mode() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()

            // Enable concurrent camera in settings
            composeTestRule.setConcurrentCameraModeInSettings(ConcurrentCameraMode.DUAL)

            composeTestRule.visitQuickSettings {
                // capture mode should now be video only
                assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.VIDEO_ONLY)

                // should not be able to switch between capture modes
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
            }
            // verify switch is disabled and locked on video only
            assertThat(composeTestRule.isCaptureModeToggleEnabled()).isFalse()
            assertThat(
                composeTestRule.getCaptureModeToggleState()
            ).isEqualTo(CaptureMode.VIDEO_ONLY)

            // set concurrent camera mode back to off in settings
            composeTestRule.setConcurrentCameraModeInSettings(ConcurrentCameraMode.OFF)

            // capture mode should reset to standard
            composeTestRule.checkCaptureModeSettingState(CaptureMode.STANDARD)
        }
    }

    @Test
    fun hdr_toggle_maintains_image_only_capture_mode() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.initializeCaptureSwitch()
            composeTestRule.setHdrEnabled(true)

            // check that switch is disabled and only supports image
            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            assertThat(composeTestRule.getCaptureModeToggleState())
                .isEqualTo(CaptureMode.IMAGE_ONLY)

            assertThat(composeTestRule.getCurrentCaptureMode()).isEqualTo(CaptureMode.IMAGE_ONLY)
            composeTestRule.setHdrEnabled(false)
            assertThat(composeTestRule.isCaptureModeToggleEnabled()).isTrue()
            composeTestRule.checkCaptureModeSettingState(CaptureMode.IMAGE_ONLY)
        }
    }

    @Test
    fun hdr_toggle_maintains_video_capture_mode() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.initializeCaptureSwitch(captureMode = CaptureMode.VIDEO_ONLY)
            composeTestRule.setHdrEnabled(true)

            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            assertThat(composeTestRule.getCaptureModeToggleState())
                .isEqualTo(CaptureMode.VIDEO_ONLY)

            assertThat(composeTestRule.getCurrentCaptureMode()).isEqualTo(CaptureMode.VIDEO_ONLY)
            composeTestRule.setHdrEnabled(false)
            assertThat(composeTestRule.isCaptureModeToggleEnabled()).isTrue()
            composeTestRule.checkCaptureModeSettingState(CaptureMode.VIDEO_ONLY)
        }
    }

    @Test
    fun hdr_toggle_change_from_image_to_video_capture_mode() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()
            composeTestRule.initializeCaptureSwitch()
            composeTestRule.setHdrEnabled(true)

            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)

            assume().that(composeTestRule.isCaptureModeToggleEnabled()).isTrue()
            // Switch to video mode while HDR is on (which means Photo Ultra HDR was enabled)
            composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performClick()

            assertThat(composeTestRule.getCaptureModeToggleState())
                .isEqualTo(CaptureMode.VIDEO_ONLY)
        }
    }

    @Test
    fun hdr_supports_video_only() {
        runMainActivityScenarioTest {
            composeTestRule.waitForCaptureButton()

            // Switch to VIDEO_ONLY first since STANDARD doesn't support HDR
            composeTestRule.setCaptureMode(CaptureMode.VIDEO_ONLY)
            composeTestRule.setHdrEnabled(true)

            // check that switch is disabled and only supports video
            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            // should not be able use capture toggle
            assume().that(composeTestRule.isCaptureModeToggleEnabled()).isFalse()
            assume().that(composeTestRule.getCaptureModeToggleState())
                .isEqualTo(CaptureMode.VIDEO_ONLY)

            composeTestRule.visitQuickSettings {
                // capture mode should be video only
                assertThat(getCurrentCaptureMode()).isEqualTo(CaptureMode.VIDEO_ONLY)
                onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
            }
            assertThat(composeTestRule.isCaptureModeToggleEnabled()).isFalse()

            composeTestRule.setHdrEnabled(false)
            // Remains VIDEO_ONLY since we explicitly switched to it
            composeTestRule.checkCaptureModeSettingState(CaptureMode.VIDEO_ONLY)
        }
    }

    @Test
    fun hdr_supports_image_and_video() {
        runMainActivityScenarioTest {
            with(composeTestRule) {
                composeTestRule.waitForCaptureButton()

                // Switch to a mode supporting HDR first
                setCaptureMode(CaptureMode.IMAGE_ONLY)

                // Enable HDR
                setHdrEnabled(true)

                // check that switch supports both image and video
                waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
                assume().that(isCaptureModeToggleEnabled()).isTrue()

                // should default to IMAGE_ONLY since we switched to it
                assertThat(getCaptureModeToggleState()).isEqualTo(CaptureMode.IMAGE_ONLY)

                visitQuickSettings {
                    checkCaptureModeSettingState(CaptureMode.IMAGE_ONLY)
                    setHdrEnabled(false)

                    // capture mode remains IMAGE_ONLY since we explicitly switched to it
                    checkCaptureModeSettingState(CaptureMode.IMAGE_ONLY)

                    setHdrEnabled(true)
                    checkCaptureModeSettingState(CaptureMode.IMAGE_ONLY)
                }
                assertThat(getCaptureModeToggleState()).isEqualTo(CaptureMode.IMAGE_ONLY)

                // turn on video only hdr
                onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performClick()
                assertThat(getCaptureModeToggleState()).isEqualTo(CaptureMode.VIDEO_ONLY)

                visitQuickSettings {
                    // capture mode should be video only now
                    checkCaptureModeSettingState(CaptureMode.VIDEO_ONLY)
                    // Standard option should not exist at all because the row is hidden in VIDEO_ONLY
                    onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()

                    setHdrEnabled(false)
                    // capture mode remains VIDEO_ONLY since we explicitly switched to it
                    checkCaptureModeSettingState(CaptureMode.VIDEO_ONLY)
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
                    checkCaptureModeSettingState(CaptureMode.IMAGE_ONLY)

                    // should not be able to change quick settings (row is hidden)
                    onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
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
                    checkCaptureModeSettingState(CaptureMode.IMAGE_ONLY)
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
                    checkCaptureModeSettingState(CaptureMode.VIDEO_ONLY)

                    // should not be able to change quick settings (row is hidden)
                    onNodeWithTag(ROW_QUICK_SETTINGS_CAPTURE_MODE).assertDoesNotExist()
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
                    checkCaptureModeSettingState(CaptureMode.VIDEO_ONLY)
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
        val targetCaptureMode = flip(initialCaptureMode)

        // should be different from initial capture mode
        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performClick()
        composeTestRule.waitForCaptureModeToggleState(targetCaptureMode)

        // should now be  she same as the initial capture mode.
        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performClick()
        composeTestRule.waitForCaptureModeToggleState(initialCaptureMode)
    }

    @Test
    fun jcaSwitch_stateDoesNotChangeWhenDragging() = runMainActivityScenarioTest {
        // Test that the state of the JCA switch does not change while dragged
        composeTestRule.waitForCaptureButton()
        composeTestRule.initializeCaptureSwitch()
        val initialCaptureMode = composeTestRule.getCaptureModeToggleState()
        val targetCaptureMode = flip(initialCaptureMode)
        val captureToggleNode = composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
        val toggleNodeWidth = captureToggleNode.fetchSemanticsNode().size.width.toFloat()
        val offsetToSwitch = when (initialCaptureMode) {
            CaptureMode.STANDARD -> TODO("App should not be in Standard Capture Mode")
            CaptureMode.VIDEO_ONLY -> -(toggleNodeWidth) // move left to switch to image
            CaptureMode.IMAGE_ONLY -> toggleNodeWidth // move right to switch to video
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
        composeTestRule.waitForCaptureModeToggleState(targetCaptureMode)
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
            composeTestRule.waitForNodeWithTagToDisappear(CAPTURE_MODE_TOGGLE_BUTTON)

            // stop recording
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists().performClick()

            composeTestRule.waitForNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            composeTestRule.waitForCaptureModeToggleState(CaptureMode.VIDEO_ONLY)

            deleteFilesInDirAfterTimestamp(MOVIES_DIR_PATH, instrumentation, timeStamp)
        }
}
