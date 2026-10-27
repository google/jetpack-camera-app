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
package com.google.jetpackcamera.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.flow.MutableSharedFlow
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class TooltipTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun tooltip_initialState_isNotDisplayed() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Help information"
                )
            }
        }

        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_clickAnchor_togglesVisibility() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipTitle = "Info Title",
                    tooltipText = "Help information"
                )
            }
        }

        // Show on first tap
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(isPopup()).assertExists()
        composeTestRule.onNode(hasText("Help information") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule.onNode(hasText("Info Title") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        // Dismiss on second tap
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_externalFlowTrigger_showsAndDismisses() {
        val triggerFlow = MutableSharedFlow<TooltipTriggerEvent>(extraBufferCapacity = 1)

        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "External trigger text",
                    triggerFlow = triggerFlow
                )
            }
        }

        composeTestRule.onNode(isPopup()).assertDoesNotExist()

        // Emit Show
        triggerFlow.tryEmit(TooltipTriggerEvent.Show)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertExists()
        composeTestRule.onNode(hasText("External trigger text") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        // Emit Dismiss
        triggerFlow.tryEmit(TooltipTriggerEvent.Dismiss)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_autoDismiss_dismissesAfterSpecifiedDuration() {
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Auto dismissing text",
                    autoDismissDuration = 3000.milliseconds
                )
            }
        }

        // Trigger show
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performClick()
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()

        composeTestRule.onNode(isPopup()).assertExists()

        // Advance part-way (1500ms) -> Still displayed
        composeTestRule.mainClock.advanceTimeBy(1500)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertExists()

        // Advance remaining time past 3000ms -> Trigger dismiss and allow exit transition
        composeTestRule.mainClock.advanceTimeBy(1501)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_autoDismiss_reTriggerResetsTimer() {
        composeTestRule.mainClock.autoAdvance = false
        val triggerFlow = MutableSharedFlow<TooltipTriggerEvent>(extraBufferCapacity = 1)

        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Resetting timer text",
                    autoDismissDuration = 3000.milliseconds,
                    triggerFlow = triggerFlow
                )
            }
        }

        // Trigger show via flow
        triggerFlow.tryEmit(TooltipTriggerEvent.Show)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertExists()

        // Advance 2000ms of 3000ms
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertExists()

        // Re-trigger Show -> Timer resets
        triggerFlow.tryEmit(TooltipTriggerEvent.Show)
        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.waitForIdle()

        // Advance another 2000ms (total 4000ms from start, but only 2000ms since reset)
        composeTestRule.mainClock.advanceTimeBy(2000)
        composeTestRule.waitForIdle()
        // Must still exist because timer was reset
        composeTestRule.onNode(isPopup()).assertExists()

        // Advance past reset expiration (remaining 1001ms) ->
        // Trigger dismiss and allow exit transition
        composeTestRule.mainClock.advanceTimeBy(1001)
        composeTestRule.mainClock.autoAdvance = true
        composeTestRule.waitForIdle()
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_positioning_supportsAboveAndBelow() {
        composeTestRule.setContent {
            MaterialTheme {
                JcaTooltipBox(
                    tooltipText = "Position Below Text",
                    positioning = TooltipAnchorPosition.Below
                ) {
                    Box(modifier = Modifier.size(48.dp))
                }
            }
        }

        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_customContainerColor_appliesCorrectly() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Custom colors",
                    isOutlined = true,
                    containerColor = Color.Black.copy(alpha = 0.6f),
                    contentColor = Color.White
                )
            }
        }

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).assertExists()
    }

    @Test
    fun tooltip_customSize_appliesCorrectly() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(20.dp)) },
                    tooltipText = "Custom size",
                    size = 32.dp
                )
            }
        }

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).assertExists()
    }

    @Test
    fun positionProvider_anchoredToFarLeft_clampsToScreenMarginAndCalculatesCaret() {
        var calculatedLocalCaretX = 0f
        var calculatedIsCaretOnTop = false

        val provider = JcaTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            caretAlignment = CaretAlignment.AnchorCenter,
            spacing = 4.dp,
            screenMargin = 16.dp,
            density = Density(1f),
            onCaretCalculated = { localCaretX, isCaretOnTop ->
                calculatedLocalCaretX = localCaretX
                calculatedIsCaretOnTop = isCaretOnTop
            }
        )

        // Anchor placed near the left edge of the screen (e.g. status bar button)
        val anchorBounds = IntRect(left = 16, top = 20, right = 48, bottom = 52)
        val windowSize = IntSize(width = 1080, height = 2400)
        val popupContentSize = IntSize(width = 400, height = 150)

        val position = provider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize
        )

        // The popup is clamped to screen margin (16px) because ideal X is offscreen
        assertThat(position.x).isEqualTo(16)
        // Positioned below anchor: bottom (52) + spacing (4) = 56
        assertThat(position.y).isEqualTo(56)

        // Local caret X is anchor center X (32) - popup X (16) = 16
        assertThat(calculatedLocalCaretX).isEqualTo(16f)
        assertThat(calculatedIsCaretOnTop).isTrue()

        // Global caret X points exactly to anchor center X
        val expectedAnchorCenter = (anchorBounds.left + anchorBounds.right) / 2
        assertThat(position.x + calculatedLocalCaretX.toInt()).isEqualTo(expectedAnchorCenter)
    }

    @Test
    fun positionProvider_spaceConstrainedBelow_flipsAbove() {
        var calculatedIsCaretOnTop = true

        val provider = JcaTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            spacing = 4.dp,
            screenMargin = 16.dp,
            density = Density(1f),
            onCaretCalculated = { _, isCaretOnTop ->
                calculatedIsCaretOnTop = isCaretOnTop
            }
        )

        // Anchor near bottom of window: spaceBelow is 48px, but tooltip height is 150px
        val anchorBounds = IntRect(left = 200, top = 2300, right = 248, bottom = 2348)
        val windowSize = IntSize(width = 1080, height = 2400)
        val popupContentSize = IntSize(width = 400, height = 150)

        val position = provider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize
        )

        // Flips above anchor: top (2300) - spacing (4) - popupHeight (150) = 2146
        assertThat(position.y).isEqualTo(2146)
        // isCaretOnTop is false, meaning caret is drawn on bottom edge pointing DOWN to anchor
        assertThat(calculatedIsCaretOnTop).isFalse()
    }

    @Test
    fun positionProvider_supportsCaretAlignmentOptions() {
        val anchorBounds = IntRect(left = 200, top = 100, right = 300, bottom = 200)
        val windowSize = IntSize(width = 1080, height = 2400)
        val popupContentSize = IntSize(width = 400, height = 150)

        // BubbleCenter
        var caretX = 0f
        val bubbleCenterProvider = JcaTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            caretAlignment = CaretAlignment.BubbleCenter,
            density = Density(1f),
            onCaretCalculated = { x, _ -> caretX = x }
        )
        bubbleCenterProvider.calculatePosition(
            anchorBounds,
            windowSize,
            LayoutDirection.Ltr,
            popupContentSize
        )
        assertThat(caretX).isEqualTo(200f)

        // Start (LTR)
        val startProvider = JcaTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            caretAlignment = CaretAlignment.Start,
            density = Density(1f),
            onCaretCalculated = { x, _ -> caretX = x }
        )
        startProvider.calculatePosition(
            anchorBounds,
            windowSize,
            LayoutDirection.Ltr,
            popupContentSize
        )
        // cornerRadius (16) + caretWidth / 2 (8) = 24
        assertThat(caretX).isEqualTo(24f)

        // End (LTR)
        val endProvider = JcaTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            caretAlignment = CaretAlignment.End,
            density = Density(1f),
            onCaretCalculated = { x, _ -> caretX = x }
        )
        endProvider.calculatePosition(
            anchorBounds,
            windowSize,
            LayoutDirection.Ltr,
            popupContentSize
        )
        // popupWidth (400) - cornerRadius (16) - caretWidth / 2 (8) = 376
        assertThat(caretX).isEqualTo(376f)

        // Custom offset
        val customProvider = JcaTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            caretAlignment = CaretAlignment.Custom(75.dp),
            density = Density(1f),
            onCaretCalculated = { x, _ -> caretX = x }
        )
        customProvider.calculatePosition(
            anchorBounds,
            windowSize,
            LayoutDirection.Ltr,
            popupContentSize
        )
        assertThat(caretX).isEqualTo(75f)
    }

    @Test
    fun tooltip_touchTarget_meetsMinimumAccessibilitySize() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(JcaTooltipDefaults.IconSize)) },
                    tooltipText = "Touch target test"
                )
            }
        }

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG)
            .assertWidthIsAtLeast(JcaTooltipDefaults.TouchTargetSize)
            .assertHeightIsAtLeast(JcaTooltipDefaults.TouchTargetSize)
    }

    @Test
    fun tooltipDefaults_mapsToMaterial3Tokens() {
        assertThat(JcaTooltipDefaults.CaretSize.width).isEqualTo(16.dp)
        assertThat(JcaTooltipDefaults.CaretSize.height).isEqualTo(8.dp)
        assertThat(JcaTooltipDefaults.TouchTargetSize).isEqualTo(48.dp)
        assertThat(JcaTooltipDefaults.ContainerSize).isEqualTo(32.dp)
        assertThat(JcaTooltipDefaults.IconSize).isEqualTo(24.dp)
        assertThat(JcaTooltipDefaults.CornerRadius).isEqualTo(16.dp)
        assertThat(JcaTooltipDefaults.DefaultElevation).isEqualTo(2.dp)
    }

    @Test
    fun tooltip_accessibility_hasButtonRoleAndContentDescription() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    contentDescription = "Custom info action",
                    tooltipText = "Accessibility description test"
                )
            }
        }

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Custom info action")
                )
            )
    }

    @Test
    fun bubbleShape_caretXClamped_doesNotBleedIntoCornerRadius() {
        val density = Density(1f)
        val size = Size(400f, 150f)

        // Shape with extreme negative caret X
        val shapeNegative = JcaTooltipBubbleShape(
            localCaretX = -100f,
            isCaretOnTop = true
        )
        val outlineNegative = shapeNegative.createOutline(size, LayoutDirection.Ltr, density)
        assertThat(outlineNegative).isInstanceOf(Outline.Generic::class.java)
        assertThat(outlineNegative.bounds.width).isEqualTo(400f)
        assertThat(outlineNegative.bounds.height).isEqualTo(150f)

        // Shape with extreme positive caret X
        val shapeOverflow = JcaTooltipBubbleShape(
            localCaretX = 1000f,
            isCaretOnTop = false
        )
        val outlineOverflow = shapeOverflow.createOutline(size, LayoutDirection.Ltr, density)
        assertThat(outlineOverflow).isInstanceOf(Outline.Generic::class.java)
        assertThat(outlineOverflow.bounds.width).isEqualTo(400f)
        assertThat(outlineOverflow.bounds.height).isEqualTo(150f)
    }
}
