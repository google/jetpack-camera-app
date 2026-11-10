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

import android.view.KeyEvent as NativeKeyEvent
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyPress
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutDefaults
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolver
import com.google.jetpackcamera.ui.components.capture.layout.CameraWindow
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SWITCH_TAG = "switch_test_target"
private const val BOUNDS_TOLERANCE_DP = 0.5f

/**
 * Unit tests for [ToggleSwitch] and [CaptureModeToggleButton] covering tap gestures, stale-callback
 * guards, accessibility semantics, directional drag handling across LTR/RTL and
 * horizontal/vertical orientations, standard vs. compact track dimensions, adaptive toolbar
 * compaction scaling, and drag cancel/settle behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ToggleSwitchTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tap_whenEnabled_togglesCheckedState() {
        var checked by mutableStateOf(false)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SWITCH_TAG).performClick()
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performClick()
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
    }

    @Test
    fun tap_whenDisabled_invokesUpdatedOnToggleWhenDisabledCallback() {
        var checkedChangeCalled = false
        var callbackLabel by mutableStateOf("first")
        val recordedDisabledTaps = mutableListOf<String>()

        composeTestRule.setContent {
            val labelSnapshot = callbackLabel
            ToggleSwitch(
                checked = false,
                onCheckedChange = { checkedChangeCalled = true },
                onToggleWhenDisabled = { recordedDisabledTaps.add(labelSnapshot) },
                enabled = false,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SWITCH_TAG).performClick()
        composeTestRule.waitForIdle()
        assertThat(checkedChangeCalled).isFalse()
        assertThat(recordedDisabledTaps).containsExactly("first")

        callbackLabel = "second"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performClick()
        composeTestRule.waitForIdle()
        assertThat(checkedChangeCalled).isFalse()
        assertThat(recordedDisabledTaps).containsExactly("first", "second").inOrder()
    }

    @Test
    fun tap_whenEnabled_invokesUpdatedOnCheckedChangeCallback() {
        var callbackLabel by mutableStateOf("first")
        val recordedChanges = mutableListOf<Pair<String, Boolean>>()

        composeTestRule.setContent {
            val labelSnapshot = callbackLabel
            ToggleSwitch(
                checked = false,
                onCheckedChange = { recordedChanges.add(labelSnapshot to it) },
                enabled = true,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        callbackLabel = "second"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performClick()
        composeTestRule.waitForIdle()
        assertThat(recordedChanges).containsExactly("second" to true)
    }

    @Test
    fun semantics_exposesSwitchRoleToggleableStateFocusAndOnClick() {
        var checked by mutableStateOf(false)
        var enabled by mutableStateOf(true)
        var provideRationaleCallback by mutableStateOf(true)
        var disabledClicks = 0

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                onToggleWhenDisabled = if (provideRationaleCallback) {
                    { disabledClicks++ }
                } else {
                    null
                },
                enabled = enabled,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val initialConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(initialConfig.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.Switch)
        assertThat(initialConfig.getOrNull(SemanticsProperties.ToggleableState))
            .isEqualTo(ToggleableState.Off)
        assertThat(initialConfig.getOrNull(SemanticsProperties.Focused)).isNotNull()
        assertThat(initialConfig.getOrNull(SemanticsProperties.Disabled)).isNull()
        assertThat(initialConfig.getOrNull(SemanticsActions.OnClick)).isNotNull()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()

        val checkedConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(checkedConfig.getOrNull(SemanticsProperties.ToggleableState))
            .isEqualTo(ToggleableState.On)

        enabled = false
        composeTestRule.waitForIdle()

        val rationaleConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(rationaleConfig.getOrNull(SemanticsProperties.Focused)).isNotNull()
        assertThat(rationaleConfig.getOrNull(SemanticsProperties.Disabled)).isNull()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
        assertThat(disabledClicks).isEqualTo(1)

        provideRationaleCallback = false
        composeTestRule.waitForIdle()

        val inertConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(inertConfig.getOrNull(SemanticsProperties.Focused)).isNull()
        assertThat(inertConfig.getOrNull(SemanticsProperties.Disabled)).isNotNull()
    }

    @Test
    fun semantics_stateDescription_isOmittedWhenNullAndSetWhenProvided() {
        var checked by mutableStateOf(false)
        var startDesc by mutableStateOf<String?>(null)
        var endDesc by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                startIconDescription = startDesc,
                endIconDescription = endDesc,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        assertThat(
            composeTestRule.onNodeWithTag(SWITCH_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        ).isNull()

        checked = true
        composeTestRule.waitForIdle()
        assertThat(
            composeTestRule.onNodeWithTag(SWITCH_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        ).isNull()

        startDesc = "Photo mode"
        endDesc = "Video mode"
        checked = false
        composeTestRule.waitForIdle()
        assertThat(
            composeTestRule.onNodeWithTag(SWITCH_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        ).isEqualTo("Photo mode")

        checked = true
        composeTestRule.waitForIdle()
        assertThat(
            composeTestRule.onNodeWithTag(SWITCH_TAG)
                .fetchSemanticsNode()
                .config
                .getOrNull(SemanticsProperties.StateDescription)
        ).isEqualTo("Video mode")
    }

    @Test
    fun horizontalLtr_dragRightTogglesOn_andDragLeftTogglesOff() {
        var checked by mutableStateOf(false)
        var callbackCount = 0

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ToggleSwitch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        callbackCount++
                    },
                    orientation = Orientation.Horizontal,
                    modifier = Modifier.testTag(SWITCH_TAG)
                )
            }
        }

        // Dragging left when already unchecked must not toggle.
        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(0)

        // Dragging right toggles false -> true.
        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
        assertThat(callbackCount).isEqualTo(1)

        // Dragging left toggles true -> false.
        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(2)
    }

    @Test
    fun horizontalRtl_dragLeftTogglesOn_andDragRightDoesNotToggleFromFalse() {
        var checked by mutableStateOf(false)
        var callbackCount = 0

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ToggleSwitch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        callbackCount++
                    },
                    orientation = Orientation.Horizontal,
                    modifier = Modifier.testTag(SWITCH_TAG)
                )
            }
        }

        // In RTL, unchecked thumb rests on the right; dragging right must NOT toggle false -> true.
        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(0)

        // Dragging left in RTL moves Start -> End and toggles false -> true.
        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
        assertThat(callbackCount).isEqualTo(1)

        // Dragging right in RTL toggles true -> false.
        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(2)
    }

    @Test
    fun verticalLtr_dragDownTogglesOn_andDragUpTogglesOff() {
        var checked by mutableStateOf(false)
        var callbackCount = 0

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ToggleSwitch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        callbackCount++
                    },
                    orientation = Orientation.Vertical,
                    modifier = Modifier.testTag(SWITCH_TAG)
                )
            }
        }

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(0)

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
        assertThat(callbackCount).isEqualTo(1)

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(2)
    }

    @Test
    fun verticalRtl_keepsTopToBottomNonInverted() {
        var checked by mutableStateOf(false)
        var callbackCount = 0

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                ToggleSwitch(
                    checked = checked,
                    onCheckedChange = {
                        checked = it
                        callbackCount++
                    },
                    orientation = Orientation.Vertical,
                    modifier = Modifier.testTag(SWITCH_TAG)
                )
            }
        }

        // Upward drag from unchecked must not toggle even in RTL.
        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeUp() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(0)

        // Downward drag toggles false -> true in RTL just like LTR.
        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
        assertThat(callbackCount).isEqualTo(1)
    }

    @Test
    fun bounds_standardAndCompact_horizontalAndVertical_matchExpectedDimensions() {
        var orientation by mutableStateOf(Orientation.Horizontal)
        var compact by mutableStateOf(false)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = false,
                onCheckedChange = {},
                orientation = orientation,
                compact = compact,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        // Horizontal Standard: 116 x 64
        val horizontalStandardBounds =
            composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(horizontalStandardBounds.width).isEqualTo(116.dp)
        assertThat(horizontalStandardBounds.height).isEqualTo(64.dp)

        // Horizontal Compact: 92 x 48
        compact = true
        composeTestRule.waitForIdle()
        val horizontalCompactBounds =
            composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(horizontalCompactBounds.width).isEqualTo(92.dp)
        assertThat(horizontalCompactBounds.height).isEqualTo(48.dp)

        // Vertical Standard: 64 x 116
        orientation = Orientation.Vertical
        compact = false
        composeTestRule.waitForIdle()
        val verticalStandardBounds =
            composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(verticalStandardBounds.width).isEqualTo(64.dp)
        assertThat(verticalStandardBounds.height).isEqualTo(116.dp)

        // Vertical Compact: 48 x 92
        compact = true
        composeTestRule.waitForIdle()
        val verticalCompactBounds =
            composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(verticalCompactBounds.width).isEqualTo(48.dp)
        assertThat(verticalCompactBounds.height).isEqualTo(92.dp)
    }

    @Test
    fun customDimensions_zeroDragRange_guardsDivisionByZeroOnDrag() {
        var checked by mutableStateOf(false)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                switchWidth = 64.dp,
                switchHeight = 64.dp,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val bounds = composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(bounds.width).isEqualTo(64.dp)
        assertThat(bounds.height).isEqualTo(64.dp)

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
    }

    @Test
    fun disableAnimations_withRotatedIcons_rendersAndSnapsWithoutAdvancingClock() {
        var checked by mutableStateOf(false)

        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.setContent {
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    ToggleSwitch(
                        checked = checked,
                        onCheckedChange = { checked = it },
                        startIcon = R.drawable.ic_camera_alt_filled,
                        endIcon = R.drawable.ic_videocam_outline,
                        iconRotationDegrees = 90f,
                        modifier = Modifier.testTag(SWITCH_TAG)
                    )
                }
            }

            composeTestRule.mainClock.advanceTimeByFrame()
            composeTestRule.onNodeWithTag(SWITCH_TAG)
                .performSemanticsAction(SemanticsActions.OnClick)
            composeTestRule.mainClock.advanceTimeByFrame()
            assertThat(checked).isTrue()

            // Probe with a partial -24px leftward drag: because thumbProgress snapped to 1f
            // immediately without advancing the clock, -24px stays above 0.5f and keeps checked.
            composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
                down(center)
                moveBy(Offset(-24f, 0f))
                up()
            }
            composeTestRule.mainClock.advanceTimeByFrame()
            assertThat(checked).isTrue()
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }

    @Test
    fun midDragOrientationChange_resetsDraggingSoSubsequentChangesUpdateSwitch() {
        var checked by mutableStateOf(false)
        var orientation by mutableStateOf(Orientation.Horizontal)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                orientation = orientation,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(centerLeft)
            moveBy(Offset(30f, 0f))
        }

        orientation = Orientation.Vertical
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()

        checked = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(center)
            moveBy(Offset(0f, -24f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
    }

    @Test
    fun midDragDisabled_snapsThumbBackToCheckedEndpoint() {
        var checked by mutableStateOf(false)
        var enabled by mutableStateOf(true)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                enabled = enabled,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(centerLeft)
            moveBy(Offset(30f, 0f))
        }

        enabled = false
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()

        enabled = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(center)
            moveBy(Offset(24f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
    }

    @Test
    fun dragPastMidpoint_whenOnCheckedChangeDeclined_revertsThumbToCheckedEndpoint() {
        var checked by mutableStateOf(false)
        var allowToggle by mutableStateOf(false)
        val requestedValues = mutableListOf<Boolean>()

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { requested ->
                    requestedValues.add(requested)
                    if (allowToggle) {
                        checked = requested
                    }
                },
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(requestedValues).containsExactly(true)
        assertThat(checked).isFalse()

        allowToggle = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(center)
            moveBy(Offset(24f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
    }

    @Test
    fun dragPastMidpoint_whenOnCheckedChangeUpdatesAsyncNextFrame_settlesWithoutReverting() {
        var checked by mutableStateOf(false)

        composeTestRule.setContent {
            val scope = rememberCoroutineScope()
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { requested ->
                    scope.launch {
                        withFrameNanos {}
                        checked = requested
                    }
                },
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performTouchInput {
            down(center)
            moveBy(Offset(-24f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
    }

    @Test
    fun captureModeToggleButton_verticalCompact_forwardsDimensionsSemanticsAndModeChange() {
        var selectedMode by mutableStateOf(CaptureMode.IMAGE_ONLY)

        composeTestRule.setContent {
            CaptureModeToggleButton(
                uiState = CaptureModeToggleUiState.Available(
                    selectedCaptureMode = selectedMode,
                    imageOnlyUiState = SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY),
                    videoOnlyUiState = SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY)
                ),
                onChangeCaptureMode = { selectedMode = it },
                onToggleWhenDisabled = {},
                orientation = Orientation.Vertical,
                compact = true,
                iconRotationDegrees = 90f
            )
        }

        val bounds =
            composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).getUnclippedBoundsInRoot()
        assertThat(bounds.width).isEqualTo(48.dp)
        assertThat(bounds.height).isEqualTo(92.dp)

        val config =
            composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).fetchSemanticsNode().config
        assertThat(config.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.Switch)
        assertThat(config.getOrNull(SemanticsProperties.ToggleableState))
            .isEqualTo(ToggleableState.Off)
        assertThat(config.getOrNull(SemanticsProperties.StateDescription)).isEqualTo("Photo mode")

        composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON).performTouchInput { swipeDown() }
        composeTestRule.waitForIdle()
        assertThat(selectedMode).isEqualTo(CaptureMode.VIDEO_ONLY)
    }

    @Test
    fun semantics_contentDescriptionAndOnClickLabel_exposedWhenProvidedAndOmittedWhenNull() {
        var contentDesc by mutableStateOf<String?>(null)
        var clickLabel by mutableStateOf<String?>(null)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = false,
                onCheckedChange = {},
                contentDescription = contentDesc,
                onClickLabel = clickLabel,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val initialConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(initialConfig.getOrNull(SemanticsProperties.ContentDescription)).isNull()
        assertThat(initialConfig.getOrNull(SemanticsActions.OnClick)?.label).isNull()

        contentDesc = "Capture mode"
        clickLabel = "Switch to video mode"
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(SWITCH_TAG).assertContentDescriptionEquals("Capture mode")
        val updatedConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(updatedConfig.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo("Switch to video mode")
    }

    @Test
    fun disabledWithoutRationale_marksDisabledSemanticsAndIsNotFocusable() {
        var checked by mutableStateOf(false)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                onToggleWhenDisabled = null,
                enabled = false,
                contentDescription = "Capture mode",
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val node = composeTestRule.onNodeWithTag(SWITCH_TAG)
        node.assertIsNotEnabled()
        val config = node.fetchSemanticsNode().config
        assertThat(config.getOrNull(SemanticsProperties.Focused)).isNull()
        assertThat(config.getOrNull(SemanticsActions.OnClick)?.action?.invoke()).isFalse()
        assertThat(checked).isFalse()
    }

    @Test
    fun disabledWithRationale_remainsFocusableAndInvokesRationaleViaSemanticsAndKeyboard() {
        var checked by mutableStateOf(false)
        var rationaleCount = 0

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                onToggleWhenDisabled = { rationaleCount++ },
                enabled = false,
                contentDescription = "Capture mode",
                onClickLabel = "Show why other mode is unavailable",
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val node = composeTestRule.onNodeWithTag(SWITCH_TAG)
        node.assertIsEnabled()
        val config = node.fetchSemanticsNode().config
        assertThat(config.getOrNull(SemanticsProperties.Focused)).isEqualTo(false)
        assertThat(config.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo("Show why other mode is unavailable")

        node.performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        assertThat(rationaleCount).isEqualTo(1)
        assertThat(checked).isFalse()

        node.requestFocus()
        node.assertIsFocused()

        node.pressHardwareKey(NativeKeyEvent.KEYCODE_SPACE)
        composeTestRule.waitForIdle()
        assertThat(rationaleCount).isEqualTo(2)
        assertThat(checked).isFalse()

        node.pressHardwareKey(NativeKeyEvent.KEYCODE_ENTER)
        composeTestRule.waitForIdle()
        assertThat(rationaleCount).isEqualTo(3)
        assertThat(checked).isFalse()
    }

    @Test
    fun keyboardActivation_whenEnabled_togglesWithSpacebarEnterNumPadEnterAndDirectionCenter() {
        var checked by mutableStateOf(false)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                contentDescription = "Capture mode",
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val node = composeTestRule.onNodeWithTag(SWITCH_TAG)
        node.requestFocus()
        node.assertIsFocused()

        node.pressHardwareKey(NativeKeyEvent.KEYCODE_SPACE)
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()

        node.pressHardwareKey(NativeKeyEvent.KEYCODE_ENTER)
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()

        node.pressHardwareKey(NativeKeyEvent.KEYCODE_NUMPAD_ENTER)
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()

        node.pressHardwareKey(NativeKeyEvent.KEYCODE_DPAD_CENTER)
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
    }

    @Test
    fun minimumInteractiveComponentSize_expandsUndersizedCustomSwitchTo48Dp() {
        composeTestRule.setContent {
            ToggleSwitch(
                checked = false,
                onCheckedChange = {},
                switchWidth = 32.dp,
                switchHeight = 32.dp,
                thumbDiameter = 24.dp,
                iconSize = 16.dp,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val bounds = composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(bounds.width).isEqualTo(48.dp)
        assertThat(bounds.height).isEqualTo(48.dp)
    }

    @Test
    fun controlledDragVeto_withNoOpOnCheckedChange_keepsToggleableStateOffAndSnapsThumbBack() {
        var checked by mutableStateOf(false)
        var allowToggle by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                ToggleSwitch(
                    checked = checked,
                    onCheckedChange = { requested ->
                        if (allowToggle) {
                            checked = requested
                        }
                    },
                    modifier = Modifier.testTag(SWITCH_TAG)
                )
            }
        }

        val node = composeTestRule.onNodeWithTag(SWITCH_TAG)
        node.performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()

        val configAfterVeto = node.fetchSemanticsNode().config
        assertThat(configAfterVeto.getOrNull(SemanticsProperties.ToggleableState))
            .isEqualTo(ToggleableState.Off)
        assertThat(checked).isFalse()

        // Allow toggling and perform a sub-midpoint (+24px) drag: because thumbProgress reconciled
        // back to 0f on veto, +24px stays below 0.5f and does not flip checked to true.
        allowToggle = true
        composeTestRule.waitForIdle()

        node.performTouchInput {
            down(center)
            moveBy(Offset(24f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
    }

    @Test
    fun captureModeToggleButton_exposesContentDescriptionStateDescriptionAndContextualLabels() {
        val rationale = object : DisableRationale {
            override val reasonTextResId: Int =
                R.string.capture_mode_video_recording_content_description_disabled
        }
        var selectedMode by mutableStateOf(CaptureMode.IMAGE_ONLY)
        var isVideoSelectable by mutableStateOf(true)
        var provideRationaleHandler by mutableStateOf(true)
        val recordedRationales = mutableListOf<DisableRationale>()

        composeTestRule.setContent {
            CaptureModeToggleButton(
                uiState = CaptureModeToggleUiState.Available(
                    selectedCaptureMode = selectedMode,
                    imageOnlyUiState = SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY),
                    videoOnlyUiState = if (isVideoSelectable) {
                        SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY)
                    } else {
                        SingleSelectableUiState.Disabled(CaptureMode.VIDEO_ONLY, rationale)
                    }
                ),
                onChangeCaptureMode = { selectedMode = it },
                onToggleWhenDisabled = if (provideRationaleHandler) {
                    { recordedRationales.add(it) }
                } else {
                    null
                }
            )
        }

        val node = composeTestRule.onNodeWithTag(CAPTURE_MODE_TOGGLE_BUTTON)
        node.assertContentDescriptionEquals("Capture mode")

        val photoConfig = node.fetchSemanticsNode().config
        assertThat(photoConfig.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.Switch)
        assertThat(photoConfig.getOrNull(SemanticsProperties.ToggleableState))
            .isEqualTo(ToggleableState.Off)
        assertThat(photoConfig.getOrNull(SemanticsProperties.StateDescription))
            .isEqualTo("Photo mode")
        assertThat(photoConfig.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo("Switch to video mode")

        node.performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        assertThat(selectedMode).isEqualTo(CaptureMode.VIDEO_ONLY)

        val videoConfig = node.fetchSemanticsNode().config
        assertThat(videoConfig.getOrNull(SemanticsProperties.ToggleableState))
            .isEqualTo(ToggleableState.On)
        assertThat(videoConfig.getOrNull(SemanticsProperties.StateDescription))
            .isEqualTo("Video mode")
        assertThat(videoConfig.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo("Switch to photo mode")

        selectedMode = CaptureMode.IMAGE_ONLY
        isVideoSelectable = false
        composeTestRule.waitForIdle()

        node.assertIsEnabled()
        val lockedWithRationaleConfig = node.fetchSemanticsNode().config
        assertThat(lockedWithRationaleConfig.getOrNull(SemanticsProperties.StateDescription))
            .isEqualTo("Photo mode only")
        assertThat(lockedWithRationaleConfig.getOrNull(SemanticsActions.OnClick)?.label)
            .isEqualTo("Show why other mode is unavailable")

        node.performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        assertThat(recordedRationales).containsExactly(rationale)

        node.requestFocus()
        node.assertIsFocused()
        node.pressHardwareKey(NativeKeyEvent.KEYCODE_SPACE)
        composeTestRule.waitForIdle()
        assertThat(recordedRationales).hasSize(2)

        provideRationaleHandler = false
        composeTestRule.waitForIdle()

        node.assertIsNotEnabled()
        val inertLockedConfig = node.fetchSemanticsNode().config
        assertThat(inertLockedConfig.getOrNull(SemanticsProperties.Focused)).isNull()
        assertThat(inertLockedConfig.getOrNull(SemanticsActions.OnClick)?.label).isNull()
        assertThat(inertLockedConfig.getOrNull(SemanticsProperties.StateDescription))
            .isEqualTo("Photo mode only")
    }

    @Test
    fun dragCancel_afterDraggingPastMidpoint_doesNotToggleAndSettlesThumbBack() {
        var checked by mutableStateOf(false)
        var callbackCount = 0

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = {
                    checked = it
                    callbackCount++
                },
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val node = composeTestRule.onNodeWithTag(SWITCH_TAG)

        // Drag well past midpoint to the right, then cancel the pointer stream.
        node.performTouchInput {
            down(center)
            moveBy(Offset(80f, 0f))
            cancel()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(0)

        // Probe with a sub-midpoint (+20px) drag: because DragInteraction.Cancel settled the thumb
        // back to 0f, +20px stays below 0.5f and must not toggle checked to true.
        node.performTouchInput {
            down(center)
            moveBy(Offset(20f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()
        assertThat(callbackCount).isEqualTo(0)

        // Verify cancelling a leftward drag from checked = true also settles back to 1f.
        checked = true
        composeTestRule.waitForIdle()

        node.performTouchInput {
            down(center)
            moveBy(Offset(-80f, 0f))
            cancel()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
        assertThat(callbackCount).isEqualTo(0)

        node.performTouchInput {
            down(center)
            moveBy(Offset(-20f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
        assertThat(callbackCount).isEqualTo(0)
    }

    @Test
    fun toolbarCompaction_scalesStandardHorizontalSwitch_andDoesNotShrinkCompactOrVertical() {
        val compactBandWindow = CameraWindow(
            width = 460.dp,
            height = 920.dp,
            topInset = 42.dp,
            navInset = 48.dp,
            gestureInset = 48.dp
        )
        val compactSolution = CameraLayoutSolver.solve(
            spec = CameraLayoutDefaults.spec(),
            window = compactBandWindow
        )
        assertThat(compactSolution.toolbarHeight.value).isWithin(BOUNDS_TOLERANCE_DP).of(46f)

        var orientation by mutableStateOf(Orientation.Horizontal)
        var compact by mutableStateOf(false)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalCameraLayoutSolution provides compactSolution) {
                ToggleSwitch(
                    checked = false,
                    onCheckedChange = {},
                    orientation = orientation,
                    compact = compact,
                    modifier = Modifier.testTag(SWITCH_TAG)
                )
            }
        }
        composeTestRule.waitForIdle()

        // Standard Horizontal scales proportionally with compacted toolbarHeight (46 / 64).
        val expectedScale = 46f / 64f
        val horizontalStandardBounds =
            composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(horizontalStandardBounds.width.value)
            .isWithin(BOUNDS_TOLERANCE_DP)
            .of(116f * expectedScale)
        assertThat(horizontalStandardBounds.height.value)
            .isWithin(BOUNDS_TOLERANCE_DP)
            .of(64f * expectedScale)

        // Compact Horizontal keeps 92 x 48 without double-shrinking.
        compact = true
        composeTestRule.waitForIdle()
        val horizontalCompactBounds =
            composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(horizontalCompactBounds.width).isEqualTo(92.dp)
        assertThat(horizontalCompactBounds.height).isEqualTo(48.dp)

        // Standard Vertical keeps 64 x 116 without toolbar shrinking.
        orientation = Orientation.Vertical
        compact = false
        composeTestRule.waitForIdle()
        val verticalStandardBounds =
            composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(verticalStandardBounds.width).isEqualTo(64.dp)
        assertThat(verticalStandardBounds.height).isEqualTo(116.dp)

        // Compact Vertical keeps 48 x 92 without double-shrinking.
        compact = true
        composeTestRule.waitForIdle()
        val verticalCompactBounds =
            composeTestRule.onNodeWithTag(SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(verticalCompactBounds.width).isEqualTo(48.dp)
        assertThat(verticalCompactBounds.height).isEqualTo(92.dp)
    }

    @Test
    fun dragPastMidpoint_withNoOpOnCheckedChange_settlesBackToOriginalCheckedSide() {
        var checked by mutableStateOf(false)
        var useNoOpCallback by mutableStateOf(true)

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = if (useNoOpCallback) {
                    {}
                } else {
                    { checked = it }
                },
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val node = composeTestRule.onNodeWithTag(SWITCH_TAG)

        // Drag past midpoint while onCheckedChange = {}; after 1 frame + settle animation, thumb
        // returns to the unchecked (0f) side.
        node.performTouchInput { swipeRight() }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()

        // Enable state updates and verify a sub-midpoint (+20px) drag stays below 0.5f, proving
        // the thumb settled back to 0f rather than staying stranded at 1f.
        useNoOpCallback = false
        composeTestRule.waitForIdle()

        node.performTouchInput {
            down(center)
            moveBy(Offset(20f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isFalse()

        // Verify the symmetric case from checked = true with a no-op callback.
        checked = true
        useNoOpCallback = true
        composeTestRule.waitForIdle()

        node.performTouchInput { swipeLeft() }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()

        useNoOpCallback = false
        composeTestRule.waitForIdle()

        node.performTouchInput {
            down(center)
            moveBy(Offset(-20f, 0f))
            up()
        }
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
    }

    @Test
    fun dragPastMidpoint_whenAsyncUpdateArrivesNextFrame_doesNotBounceOnReleaseFrame() {
        var checked by mutableStateOf(false)
        val requestedValues = mutableListOf<Boolean>()

        composeTestRule.mainClock.autoAdvance = false
        try {
            composeTestRule.setContent {
                val scope = rememberCoroutineScope()
                CompositionLocalProvider(LocalDisableAnimations provides true) {
                    ToggleSwitch(
                        checked = checked,
                        onCheckedChange = { requested ->
                            requestedValues.add(requested)
                            scope.launch {
                                withFrameNanos {}
                                checked = requested
                            }
                        },
                        modifier = Modifier.testTag(SWITCH_TAG)
                    )
                }
            }
            composeTestRule.mainClock.advanceTimeByFrame()

            val node = composeTestRule.onNodeWithTag(SWITCH_TAG)

            // Drag all the way to 1f and release; checked has not updated yet because the clock
            // has not advanced to the next frame.
            node.performTouchInput {
                down(centerLeft)
                moveBy(Offset(80f, 0f))
                up()
            }
            assertThat(requestedValues).containsExactly(true)
            assertThat(checked).isFalse()

            // Probe immediately before advancing the frame clock: if the thumb had bounced back to
            // 0f on release, a -20px drag would stay at 0f and would not request `true`. Staying
            // at 1f means 1f - 20/52 > 0.5f, so `true` is requested again.
            node.performTouchInput {
                down(center)
                moveBy(Offset(-20f, 0f))
                up()
            }
            assertThat(requestedValues).containsExactly(true, true)

            composeTestRule.mainClock.advanceTimeByFrame()
            assertThat(checked).isTrue()
        } finally {
            composeTestRule.mainClock.autoAdvance = true
        }
    }
}

private fun SemanticsNodeInteraction.pressHardwareKey(keyCode: Int) {
    assertThat(
        performKeyPress(KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_DOWN, keyCode)))
    ).isTrue()
    assertThat(
        performKeyPress(KeyEvent(NativeKeyEvent(NativeKeyEvent.ACTION_UP, keyCode)))
    ).isTrue()
}
