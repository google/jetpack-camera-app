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

import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.accessibility.AccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        AccessibilityChecks.enable()
    }

    @Test
    fun captureButton_standard_exists() {
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag("CaptureButtonTestTag"),
                onImageCapture = {},
                onStartRecording = {},
                onStopRecording = {},
                onLockVideoRecording = {},
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
            )
        }

        composeTestRule.onNodeWithTag("CaptureButtonTestTag").assertExists()
        composeTestRule.onNodeWithTag(
            "CaptureButtonTestTag"
        ).assertContentDescriptionEquals("Capture Photo")
        composeTestRule.onNodeWithTag("CaptureButtonTestTag", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun captureButton_imageOnly_exists() {
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag("CaptureButtonImageOnly"),
                onImageCapture = {},
                onStartRecording = {},
                onStopRecording = {},
                onLockVideoRecording = {},
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
            )
        }
        composeTestRule.onNodeWithTag("CaptureButtonImageOnly").assertExists()
        composeTestRule.onNodeWithTag(
            "CaptureButtonImageOnly"
        ).assertContentDescriptionEquals("Capture Photo")
        composeTestRule.onNodeWithTag("CaptureButtonImageOnly", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun captureButton_videoOnly_exists() {
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag("CaptureButtonVideoOnly"),
                onImageCapture = {},
                onStartRecording = {},
                onStopRecording = {},
                onLockVideoRecording = {},
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY)
            )
        }
        composeTestRule.onNodeWithTag("CaptureButtonVideoOnly").assertExists()
        composeTestRule.onNodeWithTag(
            "CaptureButtonVideoOnly"
        ).assertContentDescriptionEquals("Start Video Recording")
        composeTestRule.onNodeWithTag("CaptureButtonVideoOnly", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun captureButton_lockedRecording_exists() {
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag("CaptureButtonLocked"),
                onImageCapture = {},
                onStartRecording = {},
                onStopRecording = {},
                onLockVideoRecording = {},
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording
            )
        }
        composeTestRule.onNodeWithTag("CaptureButtonLocked").assertExists()
        composeTestRule.onNodeWithTag(
            "CaptureButtonLocked"
        ).assertContentDescriptionEquals("Stop Video Recording")
        composeTestRule.onNodeWithTag("CaptureButtonLocked", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }
}
