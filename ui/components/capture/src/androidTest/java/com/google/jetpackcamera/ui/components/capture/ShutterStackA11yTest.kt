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

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility and ATF ([AccessibilityValidator]) instrumentation tests for [ShutterStack] with
 * real capture controls ([CaptureButton], [FlipCameraButton], and [ImageWell]).
 */
@RunWith(AndroidJUnit4::class)
class ShutterStackA11yTest {
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
    fun shutterStack_verticalStandard_idleAndLockedRecording_passesAccessibilityChecks() {
        var captureUiState by mutableStateOf<CaptureButtonUiState>(
            CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
        )
        var hasThumbnail by mutableStateOf(true)

        composeTestRule.setContent {
            TestMvpShutterStackHarness(
                orientation = Orientation.Vertical,
                compact = false,
                colorScheme = darkColorScheme(),
                layoutDirection = LayoutDirection.Ltr,
                captureButtonUiState = captureUiState,
                hasThumbnail = hasThumbnail
            )
        }

        val expectedWellDesc = composeTestRule.activity.getString(
            R.string.image_well_content_description
        )
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(FLIP_CAMERA_BUTTON).assertExists()
        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG)
            .assertExists()
            .assertContentDescriptionEquals(expectedWellDesc)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        captureUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording
        hasThumbnail = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(IMAGE_WELL_TAG)
            .assertExists()
            .assertContentDescriptionEquals(expectedWellDesc)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun shutterStack_verticalCompactLightTheme_passesAccessibilityChecks() {
        var captureUiState by mutableStateOf<CaptureButtonUiState>(
            CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
        )
        var hasThumbnail by mutableStateOf(true)
        var resolvedContentColor: Color? = null

        composeTestRule.setContent {
            TestMvpShutterStackHarness(
                orientation = Orientation.Vertical,
                compact = true,
                colorScheme = lightColorScheme(),
                layoutDirection = LayoutDirection.Ltr,
                captureButtonUiState = captureUiState,
                hasThumbnail = hasThumbnail,
                onCaptureSlotComposed = { resolvedContentColor = it }
            )
        }

        assertThat(resolvedContentColor).isEqualTo(Color.White)
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON_RING_BORDER).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        captureUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording
        hasThumbnail = false
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun shutterStack_horizontalLtrAndRtl_idleAndLockedRecording_passesAccessibilityChecks() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var captureUiState by mutableStateOf<CaptureButtonUiState>(
            CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
        )
        var hasThumbnail by mutableStateOf(true)

        composeTestRule.setContent {
            TestMvpShutterStackHarness(
                orientation = Orientation.Horizontal,
                compact = false,
                colorScheme = darkColorScheme(),
                layoutDirection = layoutDirection,
                captureButtonUiState = captureUiState,
                hasThumbnail = hasThumbnail
            )
        }

        // Step 1: Horizontal LTR, Idle(STANDARD), non-null thumbnail
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Step 2: Horizontal LTR, LockedRecording, null thumbnail
        captureUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording
        hasThumbnail = false
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Step 3: Horizontal RTL, Idle(STANDARD), non-null thumbnail
        layoutDirection = LayoutDirection.Rtl
        captureUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
        hasThumbnail = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Step 4: Horizontal RTL, LockedRecording, null thumbnail
        captureUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording
        hasThumbnail = false
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}

@Composable
private fun TestMvpShutterStackHarness(
    orientation: Orientation,
    compact: Boolean,
    colorScheme: ColorScheme,
    layoutDirection: LayoutDirection,
    captureButtonUiState: CaptureButtonUiState,
    hasThumbnail: Boolean,
    onCaptureSlotComposed: ((Color) -> Unit)? = null
) {
    val thumbnailBitmap = remember {
        createBitmap(64, 64).apply {
            eraseColor(android.graphics.Color.BLUE)
        }
    }
    val flipLensUiState = remember {
        FlipLensUiState.Available(
            selectedLensFacing = LensFacing.BACK,
            availableLensFacings = listOf(
                SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
            )
        )
    }
    val imageWellUiState = remember(hasThumbnail, thumbnailBitmap) {
        ImageWellUiState.Content(
            mediaDescriptor = MediaDescriptor.Content.Image(
                uri = Uri.fromParts(
                    "content",
                    if (hasThumbnail) "capture_with_thumb" else "capture_no_thumb",
                    null
                ),
                thumbnail = if (hasThumbnail) thumbnailBitmap else null
            )
        )
    }

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalDisableAnimations provides true,
            LocalLayoutDirection provides layoutDirection
        ) {
            Box(
                modifier = Modifier
                    .width(360.dp)
                    .background(Color.Black)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                ShutterStack(
                    orientation = orientation,
                    compact = compact,
                    captureButton = { slotModifier ->
                        onCaptureSlotComposed?.invoke(LocalContentColor.current)
                        CaptureButton(
                            modifier = slotModifier.testTag(CAPTURE_BUTTON),
                            onImageCapture = {},
                            onStartRecording = {},
                            onStopRecording = {},
                            onLockVideoRecording = {},
                            onIncrementZoom = {},
                            captureButtonUiState = captureButtonUiState
                        )
                    },
                    flipCameraButton = { slotModifier ->
                        FlipCameraButton(
                            flipLensUiState = flipLensUiState,
                            onClick = {},
                            modifier = slotModifier.testTag(FLIP_CAMERA_BUTTON)
                        )
                    },
                    imageWell = { slotModifier ->
                        ImageWell(
                            imageWellUiState = imageWellUiState,
                            onClick = {},
                            modifier = slotModifier
                        )
                    }
                )
            }
        }
    }
}
