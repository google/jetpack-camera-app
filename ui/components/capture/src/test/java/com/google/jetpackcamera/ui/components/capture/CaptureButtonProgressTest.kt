/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureProgressUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureButtonProgressTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun capturing_disablesButtonAndAnnouncesCaptureInProgress() {
        var imagesCaptured = 0
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureButton(
                    modifier = Modifier.testTag(CAPTURE_BUTTON),
                    onImageCapture = { imagesCaptured++ },
                    onStartRecording = {},
                    onStopRecording = {},
                    onLockVideoRecording = {},
                    onIncrementZoom = {},
                    captureButtonUiState = CaptureButtonUiState.Enabled.Capturing(
                        CaptureProgressUiState.Determinate(0.5f)
                    )
                )
            }
        }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertIsNotEnabled()
            .assertContentDescriptionEquals("Capture photo")
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.StateDescription,
                    "Capture in progress"
                )
            )
            .performClick()
        composeTestRule.waitForIdle()

        assertThat(imagesCaptured).isEqualTo(0)
    }

    @Test
    fun capturingThenIdle_acceptsCaptureAgain() {
        var imagesCaptured = 0
        var uiState: CaptureButtonUiState by mutableStateOf(
            CaptureButtonUiState.Enabled.Capturing(CaptureProgressUiState.Indeterminate)
        )
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureButton(
                    modifier = Modifier.testTag(CAPTURE_BUTTON),
                    onImageCapture = { imagesCaptured++ },
                    onStartRecording = {},
                    onStopRecording = {},
                    onLockVideoRecording = {},
                    onIncrementZoom = {},
                    captureButtonUiState = uiState
                )
            }
        }

        uiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .assertIsEnabled()
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.StateDescription))
            .performClick()
        composeTestRule.waitForIdle()

        assertThat(imagesCaptured).isEqualTo(1)
    }
}
