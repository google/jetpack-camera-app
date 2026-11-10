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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
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
    fun tooltip_touchTap_togglesVisibility() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipTitle = "Info Title",
                    tooltipText = "Help information"
                )
            }
        }

        composeTestRule.onNode(isPopup()).assertDoesNotExist()

        // Show on first touch tap
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput { click() }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(isPopup()).assertExists()
        composeTestRule.onNode(hasText("Help information") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule.onNode(hasText("Info Title") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()

        // Dismiss on second touch tap
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput { click() }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun tooltip_composableSlotOverload_rendersCustomRichTooltipAndAction() {
        var actionClicked = false
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    contentDescription = "Info button",
                    hasAction = true,
                    tooltip = {
                        RichTooltip(
                            action = {
                                TextButton(onClick = { actionClicked = true }) {
                                    Text("Learn more")
                                }
                            }
                        ) {
                            Text("Styled tooltip body")
                        }
                    }
                )
            }
        }

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput { click() }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasText("Styled tooltip body") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
        composeTestRule.onNode(hasText("Learn more") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
            .performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertThat(actionClicked).isTrue()
    }

    @Test
    fun rememberJcaTooltipCaret_providesCaretPaddingAndBubbleShape_andKeysOnPositioning() {
        var positioning by mutableStateOf(TooltipAnchorPosition.Below)
        var observedPadding: PaddingValues? = null

        composeTestRule.setContent {
            MaterialTheme {
                val caret = rememberJcaTooltipCaret(positioning)
                observedPadding = caret.caretPadding
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    contentDescription = "Caret slot button",
                    positionProvider = caret.positionProvider,
                    tooltip = {
                        RichTooltip(
                            caretShape = null,
                            shape = caret.bubbleShape()
                        ) {
                            Text(
                                text = "Custom caret body",
                                modifier = Modifier.padding(caret.caretPadding)
                            )
                        }
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        assertThat(observedPadding?.calculateTopPadding()).isEqualTo(JcaTooltipDefaults.CaretHeight)
        assertThat(observedPadding?.calculateBottomPadding()).isEqualTo(0.dp)

        // Changing positioning re-keys rememberJcaTooltipCaret and flips initial caret padding edge
        composeTestRule.runOnIdle {
            positioning = TooltipAnchorPosition.Above
        }
        composeTestRule.waitForIdle()

        assertThat(observedPadding?.calculateTopPadding()).isEqualTo(0.dp)
        assertThat(observedPadding?.calculateBottomPadding())
            .isEqualTo(JcaTooltipDefaults.CaretHeight)

        // Opening the custom slot tooltip renders the bubble shape and body cleanly
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput { click() }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasText("Custom caret body") and hasAnyAncestor(isPopup()))
            .assertIsDisplayed()
    }

    @Test
    fun tooltip_onDismissRequest_invokedExactlyOnceWhenClosingAnchor() {
        var dismissCount = 0
        lateinit var tooltipState: TooltipState

        composeTestRule.setContent {
            MaterialTheme {
                tooltipState = rememberTooltipState(isPersistent = true)
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(24.dp)) },
                    tooltipText = "Dismiss count test",
                    state = tooltipState,
                    onDismissRequest = { dismissCount++ }
                )
            }
        }

        // 1. Open and close via touch tap on anchor
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput { click() }
        composeTestRule.waitForIdle()
        assertThat(dismissCount).isEqualTo(0)

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput { click() }
        composeTestRule.waitForIdle()
        assertThat(dismissCount).isEqualTo(1)
        composeTestRule.onNode(isPopup()).assertDoesNotExist()

        // 2. Open and close via semantics performClick()
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
        assertThat(dismissCount).isEqualTo(1)

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performClick()
        composeTestRule.waitForIdle()
        assertThat(dismissCount).isEqualTo(2)
        composeTestRule.onNode(isPopup()).assertDoesNotExist()

        // 3. Simulate real-window touch sequence where Popup outside-touch dismisses on ACTION_DOWN
        // (setting transition.targetState = false) before clickable runs on ACTION_UP:
        // clickable must NOT invoke onDismissRequest a second time.
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput { click() }
        composeTestRule.waitForIdle()
        assertThat(dismissCount).isEqualTo(2)

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput {
            down(center)
        }
        composeTestRule.runOnIdle {
            // Simulate TooltipBox.onDismissRequest having already fired on ACTION_DOWN
            tooltipState.dismiss()
            dismissCount++
        }
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput {
            up()
        }
        composeTestRule.waitForIdle()

        assertThat(dismissCount).isEqualTo(3)
        composeTestRule.onNode(isPopup()).assertDoesNotExist()
    }

    @Test
    fun positionProvider_anchoredToFarLeft_clampsToScreenMarginAndPointsCaretAt48dpAnchor() {
        var calculatedLocalCaretX = 0f
        var calculatedIsCaretOnTop = false

        val provider = JcaTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            spacing = 4.dp,
            screenMargin = 16.dp,
            density = Density(1f),
            onCaretCalculated = { localCaretX, isCaretOnTop ->
                calculatedLocalCaretX = localCaretX
                calculatedIsCaretOnTop = isCaretOnTop
            }
        )

        // 48px touch-target anchor placed at the 16px left screen margin -> center X = 40
        val anchorBounds = IntRect(left = 16, top = 20, right = 64, bottom = 68)
        val windowSize = IntSize(width = 1080, height = 2400)
        val popupContentSize = IntSize(width = 400, height = 150)

        val position = provider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize
        )

        // Popup clamps to screen margin (16px)
        assertThat(position.x).isEqualTo(16)
        // Positioned below anchor: bottom (68) + spacing (4) = 72
        assertThat(position.y).isEqualTo(72)

        // Local caret X is anchor center X (40) - popup X (16) = 24
        assertThat(calculatedLocalCaretX).isEqualTo(24f)
        assertThat(calculatedIsCaretOnTop).isTrue()

        // Global caret X points directly at anchor center X (40)
        val expectedAnchorCenter = (anchorBounds.left + anchorBounds.right) / 2
        assertThat(position.x + calculatedLocalCaretX.toInt()).isEqualTo(expectedAnchorCenter)
    }

    @Test
    fun positionProvider_anchorInsideCornerArc_clampsCaretToMinCornerClearance() {
        var calculatedLocalCaretX = 0f

        val provider = JcaTooltipPositionProvider(
            positioning = TooltipAnchorPosition.Below,
            spacing = 4.dp,
            screenMargin = 16.dp,
            cornerRadius = 16.dp,
            caretWidth = 16.dp,
            density = Density(1f),
            onCaretCalculated = { localCaretX, _ ->
                calculatedLocalCaretX = localCaretX
            }
        )

        // Narrow anchor near left edge: center X (32) - popup X (16) = 16, which is inside
        // minCaretX = cornerRadius (16) + caretWidth / 2 (8) = 24
        val anchorBounds = IntRect(left = 16, top = 20, right = 48, bottom = 52)
        val windowSize = IntSize(width = 1080, height = 2400)
        val popupContentSize = IntSize(width = 400, height = 150)

        provider.calculatePosition(
            anchorBounds = anchorBounds,
            windowSize = windowSize,
            layoutDirection = LayoutDirection.Ltr,
            popupContentSize = popupContentSize
        )

        assertThat(calculatedLocalCaretX).isEqualTo(24f)
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
        // isCaretOnTop is false, meaning caret is drawn on bottom edge pointing down to anchor
        assertThat(calculatedIsCaretOnTop).isFalse()
    }

    @Test
    fun tooltip_accessibility_hasTouchTargetButtonRoleSingleLabelAndPoliteLiveRegion() {
        composeTestRule.setContent {
            MaterialTheme {
                TooltipIconButton(
                    icon = { Box(modifier = Modifier.size(JcaTooltipDefaults.IconSize)) },
                    contentDescription = "Custom info action",
                    tooltipText = "Accessibility description test"
                )
            }
        }

        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG)
            .assertWidthIsAtLeast(JcaTooltipDefaults.TouchTargetSize)
            .assertHeightIsAtLeast(JcaTooltipDefaults.TouchTargetSize)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.ContentDescription,
                    listOf("Custom info action")
                )
            )
            .assert(
                SemanticsMatcher("onClickLabel is null to prevent duplicate TalkBack speech") {
                    it.config.getOrNull(SemanticsActions.OnClick)?.label == null
                }
            )

        // Show tooltip and verify LiveRegionMode.Polite semantics on RichTooltip
        composeTestRule.onNodeWithTag(TOOLTIP_ANCHOR_BUTTON_TAG).performTouchInput { click() }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(TOOLTIP_CONTENT_TAG)
            .assert(
                SemanticsMatcher.expectValue(
                    SemanticsProperties.LiveRegion,
                    LiveRegionMode.Polite
                )
            )
    }

    @Test
    fun bubbleShape_caretXClamped_doesNotBleedIntoCornerRadius() {
        val density = Density(1f)
        val size = Size(400f, 150f)

        // Shape with extreme negative caret X
        val shapeNegative = JcaTooltipBubbleShape(
            localCaretXProvider = { -100f },
            isCaretOnTop = true
        )
        val outlineNegative = shapeNegative.createOutline(size, LayoutDirection.Ltr, density)
        assertThat(outlineNegative).isInstanceOf(Outline.Generic::class.java)
        assertThat(outlineNegative.bounds.width).isEqualTo(400f)
        assertThat(outlineNegative.bounds.height).isEqualTo(150f)

        // Shape with extreme positive caret X
        val shapeOverflow = JcaTooltipBubbleShape(
            localCaretXProvider = { 1000f },
            isCaretOnTop = false
        )
        val outlineOverflow = shapeOverflow.createOutline(size, LayoutDirection.Ltr, density)
        assertThat(outlineOverflow).isInstanceOf(Outline.Generic::class.java)
        assertThat(outlineOverflow.bounds.width).isEqualTo(400f)
        assertThat(outlineOverflow.bounds.height).isEqualTo(150f)
    }

    @Test
    fun bubbleShape_defersCaretXProviderReadUntilCreateOutline() {
        var providerReads = 0
        var dynamicCaretX = 60f
        val shape = JcaTooltipBubbleShape(
            localCaretXProvider = {
                providerReads++
                dynamicCaretX
            },
            isCaretOnTop = true
        )

        // Constructing the shape during composition must not evaluate localCaretXProvider
        assertThat(providerReads).isEqualTo(0)

        val density = Density(1f)
        val size = Size(200f, 100f)
        val outlineAt60 =
            shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Generic
        assertThat(providerReads).isEqualTo(1)

        // Updating dynamicCaretX during layout before draw produces a shifted caret outline
        // from the same JcaTooltipBubbleShape instance without recomposing
        dynamicCaretX = 140f
        val outlineAt140 =
            shape.createOutline(size, LayoutDirection.Ltr, density) as Outline.Generic
        assertThat(providerReads).isEqualTo(2)

        val xorDiff = Path().apply {
            op(outlineAt60.path, outlineAt140.path, PathOperation.Xor)
        }
        assertThat(xorDiff.isEmpty).isFalse()

        // Verify the tip at y = 0 moved from x = 60 to x = 140
        val probeAt140Tip = Path().apply {
            addRect(androidx.compose.ui.geometry.Rect(Offset(139f, 1f), Size(2f, 2f)))
        }
        val intersectAt140 = Path().apply {
            op(outlineAt140.path, probeAt140Tip, PathOperation.Intersect)
        }
        assertThat(intersectAt140.isEmpty).isFalse()
    }
}
