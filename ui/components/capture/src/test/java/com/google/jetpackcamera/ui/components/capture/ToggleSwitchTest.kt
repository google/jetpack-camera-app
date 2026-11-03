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

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
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
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import kotlinx.coroutines.launch
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val SWITCH_TAG = "switch_test_target"

/**
 * Unit tests for [ToggleSwitch] and [CaptureModeToggleButton] covering tap gestures, stale-callback
 * guards, accessibility semantics, directional drag handling across LTR/RTL and
 * horizontal/vertical orientations, and standard vs. compact track dimensions.
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
        var disabledClicks = 0

        composeTestRule.setContent {
            ToggleSwitch(
                checked = checked,
                onCheckedChange = { checked = it },
                onToggleWhenDisabled = { disabledClicks++ },
                enabled = enabled,
                modifier = Modifier.testTag(SWITCH_TAG)
            )
        }

        val initialConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(initialConfig.getOrNull(SemanticsProperties.Role)).isEqualTo(Role.Switch)
        assertThat(initialConfig.getOrNull(SemanticsProperties.ToggleableState))
            .isEqualTo(ToggleableState.Off)
        assertThat(initialConfig.getOrNull(SemanticsProperties.Focused)).isNotNull()
        assertThat(initialConfig.getOrNull(SemanticsActions.OnClick)).isNotNull()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()

        val checkedConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(checkedConfig.getOrNull(SemanticsProperties.ToggleableState))
            .isEqualTo(ToggleableState.On)

        enabled = false
        composeTestRule.waitForIdle()

        val disabledConfig = composeTestRule.onNodeWithTag(SWITCH_TAG).fetchSemanticsNode().config
        assertThat(disabledConfig.getOrNull(SemanticsProperties.Focused)).isNull()

        composeTestRule.onNodeWithTag(SWITCH_TAG).performSemanticsAction(SemanticsActions.OnClick)
        composeTestRule.waitForIdle()
        assertThat(checked).isTrue()
        assertThat(disabledClicks).isEqualTo(1)
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
}
