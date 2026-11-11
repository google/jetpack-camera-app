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

import android.content.Context
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.CustomSnackbarVisuals
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val TEST_COORDINATES = Offset(100f, 100f)
private val TEST_CHROME_PADDING = 16.dp
private const val SWITCH_TEST_TAG = "switch_test_toggle"

@RunWith(AndroidJUnit4::class)
class CaptureScreenComponentsTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator().setRunChecksFromRootView(true).also {
                it.setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
            }
        )
    }

    @Test
    fun elapsedTimeText_unavailableState_doesNotRender() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Unavailable }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertDoesNotExist()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledActive_displaysFormattedTime() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                // 0:30
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Enabled(30_000_000_000L) }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertTextEquals("0:30")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledActive_setsContentDescription() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                // 1:05
                elapsedTimeUiStateProvider = { ElapsedTimeUiState.Enabled(65_000_000_000L) }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG)
            .assertContentDescriptionEquals("Recording time: 1 minute and 5 seconds")
    }

    @Test
    fun elapsedTimeText_enabledPaused_displaysPausedFormattedTime() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = {
                    // PAUSED 0:30
                    ElapsedTimeUiState.Enabled(30_000_000_000L, isPaused = true)
                }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG).assertTextEquals("PAUSED 0:30")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun elapsedTimeText_enabledPaused_setsContentDescription() {
        composeTestRule.setContent {
            ElapsedTimeText(
                modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                elapsedTimeUiStateProvider = {
                    // PAUSED 1:05
                    ElapsedTimeUiState.Enabled(65_000_000_000L, isPaused = true)
                }
            )
        }
        composeTestRule.onNodeWithTag(ELAPSED_TIME_TAG)
            .assertContentDescriptionEquals("Recording paused: 1 minute and 5 seconds")
    }

    @Test
    fun focusMeteringIndicator_unspecifiedState_doesNotRender() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Unspecified,
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun focusMeteringIndicator_runningState_renders() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.RUNNING
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_successState_persistsWithoutAutoDismiss() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.SUCCESS
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_failureState_renders() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.FAILURE
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertExists()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_cancelledState_doesNotRender() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.CANCELLED
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).assertDoesNotExist()
    }

    @Test
    fun focusMeteringIndicator_hasAccessibilityContentDescription_whenRunning() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.RUNNING
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG)
            .assertContentDescriptionEquals("Focusing")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusMeteringIndicator_hasAccessibilityContentDescription_whenLocked() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                FocusMeteringIndicator(
                    focusMeteringUiState = FocusMeteringUiState.Specified(
                        surfaceCoordinates = TEST_COORDINATES,
                        status = FocusMeteringUiState.Status.SUCCESS
                    ),
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
        composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG)
            .assertContentDescriptionEquals("Focused")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun pillSnackbar_dismissButtonClick_callsOnDismissAndUsesStringResource() {
        var dismissed = false
        val dismissDescription =
            ApplicationProvider.getApplicationContext<Context>().getString(
                R.string.snackbar_dismiss_content_description
            )

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image capture failed",
                        actionLabel = null,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short,
                        isError = true
                    ),
                    onDismiss = { dismissed = true }
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Image capture failed").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription(dismissDescription)
            .assertIsDisplayed()
            .performClick()
        assertThat(dismissed).isTrue()
    }

    @Test
    fun pillSnackbar_actionButtonClick_callsOnAction() {
        var actionPerformed = false

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image capture failed",
                        actionLabel = "Retry",
                        withDismissAction = false,
                        duration = SnackbarDuration.Short,
                        isError = true
                    ),
                    onAction = { actionPerformed = true }
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Retry")
            .assertIsDisplayed()
            .performClick()
        assertThat(actionPerformed).isTrue()
    }

    @Test
    fun pillSnackbar_statusIcon_hasNoContentDescription() {
        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                PillSnackbar(
                    visuals = CustomSnackbarVisuals(
                        message = "Image saved",
                        actionLabel = null,
                        withDismissAction = false,
                        duration = SnackbarDuration.Short,
                        isError = false
                    )
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithText("Image saved").assertIsDisplayed()
        composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true
        ).assertCountEquals(0)
    }

    @Test
    fun toggleSwitch_horizontalAndVertical_standardAndCompact_passesAccessibilityChecks() {
        var orientation by mutableStateOf(Orientation.Horizontal)
        var compact by mutableStateOf(false)
        var checked by mutableStateOf(false)

        composeTestRule.setContent {
            DarkCameraChrome(darkTheme = true) {
                ToggleSwitch(
                    checked = checked,
                    onCheckedChange = { checked = it },
                    orientation = orientation,
                    compact = compact,
                    startIcon = R.drawable.ic_camera_alt_filled,
                    endIcon = R.drawable.ic_videocam_outline,
                    contentDescription = stringResource(
                        R.string.capture_mode_toggle_content_description
                    ),
                    onClickLabel = stringResource(
                        R.string.capture_mode_action_switch_to_video
                    ),
                    startIconDescription = stringResource(
                        R.string.capture_mode_image_capture_content_description
                    ),
                    endIconDescription = stringResource(
                        R.string.capture_mode_video_recording_content_description
                    ),
                    modifier = Modifier.testTag(SWITCH_TEST_TAG)
                )
            }
        }

        // Horizontal Standard
        composeTestRule.onNodeWithTag(SWITCH_TEST_TAG).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Horizontal Compact (checked)
        compact = true
        checked = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Vertical Standard
        orientation = Orientation.Vertical
        compact = false
        checked = false
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Vertical Compact (checked)
        compact = true
        checked = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun toggleSwitch_lightAndDarkThemes_enabledAndDisabled_passesAccessibilityChecks() {
        var darkTheme by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var provideRationale by mutableStateOf(true)

        composeTestRule.setContent {
            DarkCameraChrome(darkTheme = darkTheme) {
                ToggleSwitch(
                    checked = false,
                    onCheckedChange = {},
                    onToggleWhenDisabled = if (provideRationale) {
                        {}
                    } else {
                        null
                    },
                    enabled = enabled,
                    startIcon = R.drawable.ic_camera_alt_filled,
                    endIcon = R.drawable.ic_videocam_outline,
                    contentDescription = stringResource(
                        R.string.capture_mode_toggle_content_description
                    ),
                    onClickLabel = if (enabled) {
                        stringResource(R.string.capture_mode_action_switch_to_video)
                    } else if (provideRationale) {
                        stringResource(R.string.capture_mode_action_explain_unavailable)
                    } else {
                        null
                    },
                    startIconDescription = stringResource(
                        R.string.capture_mode_image_capture_content_description
                    ),
                    modifier = Modifier.testTag(SWITCH_TEST_TAG)
                )
            }
        }

        // Light M3 theme, enabled over dark camera chrome
        composeTestRule.onNodeWithTag(SWITCH_TEST_TAG).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Light M3 theme, disabled with rationale over dark camera chrome
        enabled = false
        provideRationale = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Light M3 theme, inert disabled over dark camera chrome
        provideRationale = false
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Dark M3 theme, disabled with rationale over dark camera chrome
        darkTheme = true
        provideRationale = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun captureModeToggleButton_enabledStates_passesAccessibilityChecks() {
        var selectedMode by mutableStateOf(CaptureMode.IMAGE_ONLY)
        var darkTheme by mutableStateOf(true)

        composeTestRule.setContent {
            DarkCameraChrome(darkTheme = darkTheme) {
                CaptureModeToggleButton(
                    uiState = CaptureModeToggleUiState.Available(
                        selectedCaptureMode = selectedMode,
                        imageOnlyUiState = SingleSelectableUiState.SelectableUi(
                            CaptureMode.IMAGE_ONLY
                        ),
                        videoOnlyUiState = SingleSelectableUiState.SelectableUi(
                            CaptureMode.VIDEO_ONLY
                        )
                    ),
                    onChangeCaptureMode = { selectedMode = it }
                )
            }
        }

        // Dark M3 theme, Photo mode
        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Capture mode")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Dark M3 theme, Video mode
        selectedMode = CaptureMode.VIDEO_ONLY
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Light M3 theme, Photo mode
        darkTheme = false
        selectedMode = CaptureMode.IMAGE_ONLY
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun captureModeToggleButton_disabledStates_passesAccessibilityChecks() {
        val rationale = object : DisableRationale {
            override val reasonTextResId: Int =
                R.string.capture_mode_video_recording_content_description_disabled
        }
        var darkTheme by mutableStateOf(false)
        var provideRationaleHandler by mutableStateOf(true)

        composeTestRule.setContent {
            DarkCameraChrome(darkTheme = darkTheme) {
                CaptureModeToggleButton(
                    uiState = CaptureModeToggleUiState.Available(
                        selectedCaptureMode = CaptureMode.IMAGE_ONLY,
                        imageOnlyUiState = SingleSelectableUiState.SelectableUi(
                            CaptureMode.IMAGE_ONLY
                        ),
                        videoOnlyUiState = SingleSelectableUiState.Disabled(
                            CaptureMode.VIDEO_ONLY,
                            rationale
                        )
                    ),
                    onChangeCaptureMode = {},
                    onToggleWhenDisabled = if (provideRationaleHandler) {
                        {}
                    } else {
                        null
                    }
                )
            }
        }

        // Light M3 theme, disabled with rationale
        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
            .assertIsDisplayed()
            .assertContentDescriptionEquals("Capture mode")
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Light M3 theme, inert disabled
        provideRationaleHandler = false
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Dark M3 theme, disabled with rationale
        darkTheme = true
        provideRationaleHandler = true
        composeTestRule.waitForIdle()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}

@Composable
private fun DarkCameraChrome(darkTheme: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDisableAnimations provides true) {
        MaterialTheme(colorScheme = if (darkTheme) darkColorScheme() else lightColorScheme()) {
            Box(
                modifier = Modifier
                    .background(Color.Black)
                    .padding(TEST_CHROME_PADDING)
            ) {
                content()
            }
        }
    }
}
