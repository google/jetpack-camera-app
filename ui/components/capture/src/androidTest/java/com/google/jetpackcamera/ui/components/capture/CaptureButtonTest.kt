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

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureButtonTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator().setRunChecksFromRootView(true).also {
                it.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
            }
        )
    }

    @Test
    fun captureButton_standard_exists() {
        var imageCaptured = false
        var recordingStarted = false
        var recordingLocked = false
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag(CAPTURE_BUTTON),
                onImageCapture = { imageCaptured = true },
                onStartRecording = { recordingStarted = true },
                onStopRecording = {},
                onLockVideoRecording = { recordingLocked = it },
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
            )
        }

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(
            CAPTURE_BUTTON
        ).assertContentDescriptionEquals("Capture Photo")
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick)
        assertThat(imageCaptured).isTrue()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .performSemanticsAction(SemanticsActions.OnLongClick)
        assertThat(recordingLocked).isTrue()
        assertThat(recordingStarted).isTrue()
    }

    @Test
    fun captureButton_imageOnly_exists() {
        var imageCaptured = false
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag(CAPTURE_BUTTON),
                onImageCapture = { imageCaptured = true },
                onStartRecording = {},
                onStopRecording = {},
                onLockVideoRecording = {},
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
            )
        }
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(
            CAPTURE_BUTTON
        ).assertContentDescriptionEquals("Capture Photo")
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick)
        assertThat(imageCaptured).isTrue()
    }

    @Test
    fun captureButton_videoOnly_exists() {
        var recordingStarted = false
        var recordingLocked = false
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag(CAPTURE_BUTTON),
                onImageCapture = {},
                onStartRecording = { recordingStarted = true },
                onStopRecording = {},
                onLockVideoRecording = { recordingLocked = it },
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY)
            )
        }
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(
            CAPTURE_BUTTON
        ).assertContentDescriptionEquals("Start Video Recording")
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick)
        assertThat(recordingLocked).isTrue()
        assertThat(recordingStarted).isTrue()
    }

    @Test
    fun captureButton_lockedRecording_exists() {
        var recordingStopped = false
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag(CAPTURE_BUTTON),
                onImageCapture = {},
                onStartRecording = {},
                onStopRecording = { recordingStopped = true },
                onLockVideoRecording = {},
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording
            )
        }
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(
            CAPTURE_BUTTON
        ).assertContentDescriptionEquals("Stop Video Recording")
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON)
            .performSemanticsAction(SemanticsActions.OnClick)
        assertThat(recordingStopped).isTrue()
    }

    @Test
    fun captureButton_pressedRecording_exists() {
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag(CAPTURE_BUTTON),
                onImageCapture = {},
                onStartRecording = {},
                onStopRecording = {},
                onLockVideoRecording = {},
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording
            )
        }
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(
            CAPTURE_BUTTON
        ).assertContentDescriptionEquals("Recording Video")
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON, useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
    }

    @Test
    fun captureButton_disabled_exists() {
        composeTestRule.setContent {
            CaptureButton(
                modifier = Modifier.testTag(CAPTURE_BUTTON),
                onImageCapture = {},
                onStartRecording = {},
                onStopRecording = {},
                onLockVideoRecording = {},
                onIncrementZoom = {},
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(
                    CaptureMode.STANDARD,
                    isEnabled = false
                )
            )
        }
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(
            CAPTURE_BUTTON
        ).assert(isNotEnabled())
    }
}
