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

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.components.capture.AUDIO_INPUT_TOGGLE
import com.google.jetpackcamera.ui.components.capture.AudioInputState
import com.google.jetpackcamera.ui.components.capture.AudioStateProperty
import com.google.jetpackcamera.ui.components.capture.CAPTURE_BUTTON
import com.google.jetpackcamera.ui.uistateadapter.capture.R
import com.google.jetpackcamera.utils.TEST_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.utils.VIDEO_CAPTURE_TIMEOUT_MILLIS
import com.google.jetpackcamera.utils.debugExtra
import com.google.jetpackcamera.utils.pressAndDragToLockVideoRecording
import com.google.jetpackcamera.utils.runMainActivityScenarioTest
import com.google.jetpackcamera.utils.waitForCaptureButton
import com.google.jetpackcamera.utils.waitForNodeWithTagAndSemantics
import com.google.jetpackcamera.utils.waitForSnackbarWithText
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoAudioTest {
    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(TEST_REQUIRED_PERMISSIONS).toTypedArray())

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        assertThat(uiDevice.isScreenOn).isTrue()
    }

    @Test
    fun audioIncomingWhenEnabled() {
        runMainActivityScenarioTest(debugExtra) {
            // check audio visualizer composable for muted/unmuted icon.
            // icon will only be unmuted if audio is nonzero
            composeTestRule.waitForCaptureButton()

            // Start video recording and lock it
            composeTestRule.pressAndDragToLockVideoRecording()

            // assert hot amplitude tag visible
            composeTestRule.waitForNodeWithTagAndSemantics(
                tag = AUDIO_INPUT_TOGGLE,
                semanticsProperty = AudioStateProperty to AudioInputState.INCOMING
            )

            // Stop recording
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).performClick()

            composeTestRule.waitForSnackbarWithText(
                R.string.toast_video_capture_success,
                VIDEO_CAPTURE_TIMEOUT_MILLIS
            )
        }
    }

    @Test
    fun muteAndUnmuteDuringRecording() {
        runMainActivityScenarioTest(debugExtra) {
            composeTestRule.waitForCaptureButton()

            // verify audio button is initially set to enable audio
            composeTestRule.waitForNodeWithTagAndSemantics(
                tag = AUDIO_INPUT_TOGGLE,
                semanticsProperty = AudioStateProperty to AudioInputState.READY
            )

            // Start video recording and lock it
            composeTestRule.pressAndDragToLockVideoRecording()

            // Verify amplitude button is hot initially (audio is enabled by default)
            composeTestRule.waitForNodeWithTagAndSemantics(
                tag = AUDIO_INPUT_TOGGLE,
                semanticsProperty = AudioStateProperty to AudioInputState.INCOMING
            )

            // Tap the amplitude button to mute
            composeTestRule.onNodeWithTag(AUDIO_INPUT_TOGGLE).performClick()

            // Verify amplitude button is now showing "none/muted" tag
            composeTestRule.waitForNodeWithTagAndSemantics(
                tag = AUDIO_INPUT_TOGGLE,
                semanticsProperty = AudioStateProperty to AudioInputState.OFF
            )

            // Tap the amplitude button to unmute
            composeTestRule.onNodeWithTag(AUDIO_INPUT_TOGGLE).performClick()

            // Verify amplitude button is hot again
            composeTestRule.waitForNodeWithTagAndSemantics(
                tag = AUDIO_INPUT_TOGGLE,
                semanticsProperty = AudioStateProperty to AudioInputState.INCOMING
            )

            // Stop recording
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).performClick()

            composeTestRule.waitForSnackbarWithText(
                R.string.toast_video_capture_success,
                VIDEO_CAPTURE_TIMEOUT_MILLIS
            )
        }
    }

    @Test
    fun startRecordingMuted() {
        runMainActivityScenarioTest(debugExtra) {
            composeTestRule.waitForCaptureButton()

            // Verify amplitude button is hot initially (audio is enabled by default)
            composeTestRule.waitForNodeWithTagAndSemantics(
                tag = AUDIO_INPUT_TOGGLE,
                semanticsProperty = AudioStateProperty to AudioInputState.READY
            )

            // Tap the amplitude button to mute
            composeTestRule.onNodeWithTag(AUDIO_INPUT_TOGGLE).performClick()

            // Verify amplitude button is now showing "none/muted" tag
            composeTestRule.waitForNodeWithTagAndSemantics(
                tag = AUDIO_INPUT_TOGGLE,
                semanticsProperty = AudioStateProperty to AudioInputState.OFF
            )

            // Start video recording
            composeTestRule.pressAndDragToLockVideoRecording()

            // Verify it remains muted during recording
            // Verify amplitude button is now showing "none/muted" tag
            composeTestRule.waitForNodeWithTagAndSemantics(
                tag = AUDIO_INPUT_TOGGLE,
                semanticsProperty = AudioStateProperty to AudioInputState.OFF
            )

            // Stop recording
            composeTestRule.onNodeWithTag(CAPTURE_BUTTON).performClick()
            composeTestRule.waitForSnackbarWithText(
                R.string.toast_video_capture_success,
                VIDEO_CAPTURE_TIMEOUT_MILLIS
            )
        }
    }
}
