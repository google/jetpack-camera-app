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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [CutoutAwareRow] verifying layout positioning around display cutouts
 * and horizontal animation stability.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CutoutAwareRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noCutout_placesItemsInStandardHorizontalSequence() {
        composeTestRule.setContent {
            CutoutAwareRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalSpacing = 8.dp,
                cutoutClearance = 8.dp,
                cutoutRectsOverride = emptyList()
            ) {
                Box(Modifier.size(24.dp).testTag("item0"))
                Box(Modifier.size(24.dp).testTag("item1"))
                Box(Modifier.size(24.dp).testTag("item2"))
            }
        }

        assertThat(composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot().left)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag("item1").getUnclippedBoundsInRoot().left)
            .isEqualTo(32.dp)
        assertThat(composeTestRule.onNodeWithTag("item2").getUnclippedBoundsInRoot().left)
            .isEqualTo(64.dp)
    }

    @Test
    fun leftCornerCutout_shiftsAllItemsPastCutout() {
        // Cutout occupies [0, 50] with 8dp clearance -> keep-out range [0, 58].
        composeTestRule.setContent {
            CutoutAwareRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalSpacing = 8.dp,
                cutoutClearance = 8.dp,
                cutoutRectsOverride = listOf(IntRect(0, 0, 50, 48))
            ) {
                Box(Modifier.size(24.dp).testTag("item0"))
                Box(Modifier.size(24.dp).testTag("item1"))
            }
        }

        assertThat(composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot().left)
            .isEqualTo(58.dp)
        assertThat(composeTestRule.onNodeWithTag("item1").getUnclippedBoundsInRoot().left)
            .isEqualTo(90.dp)
    }

    @Test
    fun centerCutout_flowsItemsAroundCutout() {
        // Cutout occupies [60, 100] with 8dp clearance -> keep-out range [52, 108].
        // item0 (24dp) sits at [0, 24] (left of cutout).
        // item1 (24dp) would land at [32, 56], overlapping [52, 108], so it jumps to 108dp.
        // item2 (24dp) follows item1 on the right at 140dp.
        composeTestRule.setContent {
            CutoutAwareRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalSpacing = 8.dp,
                cutoutClearance = 8.dp,
                cutoutRectsOverride = listOf(IntRect(60, 0, 100, 48))
            ) {
                Box(Modifier.size(24.dp).testTag("item0"))
                Box(Modifier.size(24.dp).testTag("item1"))
                Box(Modifier.size(24.dp).testTag("item2"))
            }
        }

        assertThat(composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot().left)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag("item1").getUnclippedBoundsInRoot().left)
            .isEqualTo(108.dp)
        assertThat(composeTestRule.onNodeWithTag("item2").getUnclippedBoundsInRoot().left)
            .isEqualTo(140.dp)
    }

    @Test
    fun animatedVisibility_enteringAndExitingItem_neverJumpsAcrossCutoutMidAnimation() {
        // Cutout occupies [44, 90] with 8dp clearance -> keep-out range [36, 98].
        // item0 (24dp) sits at [0, 24], leaving cursor at 32dp.
        // Notice 32dp < 36dp: when item1's animated width is 1..4dp, [32, 32 + width] would fit
        // to the LEFT of 36dp if not for LookaheadScope locking it to its target side (98dp).
        var item1Visible by mutableStateOf(false)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            CutoutAwareRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                horizontalSpacing = 8.dp,
                cutoutClearance = 8.dp,
                cutoutRectsOverride = listOf(IntRect(44, 0, 90, 48))
            ) {
                Box(Modifier.size(24.dp).testTag("item0"))
                AnimatedVisibility(
                    visible = item1Visible,
                    enter = expandHorizontally(tween(200, easing = LinearEasing)),
                    exit = shrinkHorizontally(tween(200, easing = LinearEasing))
                ) {
                    Box(Modifier.size(24.dp).testTag("item1"))
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()

        // Trigger enter animation (0dp -> 24dp).
        item1Visible = true
        composeTestRule.mainClock.advanceTimeByFrame()

        var observedEnteringFrames = 0
        for (step in 1..15) {
            composeTestRule.mainClock.advanceTimeBy(16)
            val nodes = composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag("item1")
            ).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                val unclipped = composeTestRule.onNodeWithTag("item1").getUnclippedBoundsInRoot()
                val clipped = composeTestRule.onNodeWithTag("item1").getBoundsInRoot()
                if (clipped.width > 0.dp) {
                    observedEnteringFrames++
                    assertWithMessage(
                        "Enter step=$step unclipped=$unclipped clipped=$clipped"
                    ).that(clipped.left).isEqualTo(98.dp)
                }
            }
        }
        assertThat(observedEnteringFrames).isGreaterThan(3)

        // Trigger exit animation (24dp -> 0dp).
        item1Visible = false
        composeTestRule.mainClock.advanceTimeByFrame()

        var observedExitingFrames = 0
        for (step in 1..15) {
            composeTestRule.mainClock.advanceTimeBy(16)
            val nodes = composeTestRule.onAllNodes(
                androidx.compose.ui.test.hasTestTag("item1")
            ).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                val bounds = composeTestRule.onNodeWithTag("item1").getBoundsInRoot()
                if (bounds.width > 0.dp) {
                    observedExitingFrames++
                    assertThat(bounds.left).isEqualTo(98.dp)
                }
            }
        }
        assertThat(observedExitingFrames).isGreaterThan(3)
    }

    @Test
    fun cutoutAwareRow_rtlLayoutDirection_shiftsItemsLeftOfRightSideCutout() {
        // Right-side cutout spanning [290, 330] in a 360dp-wide row with 8dp clearance ->
        // physical keep-out interval is [282, 338].
        // In RTL (starting from x = 360 going left):
        // - Item 0 (width 30dp) at the right edge [330, 360] overlaps [282, 338], so it must
        //   jump to the left of the keep-out zone and land at [252, 282].
        // - Item 1 (width 30dp) follows 8dp to the left at [214, 244].
        val rightSideCutout = IntRect(left = 290, top = 0, right = 330, bottom = 48)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CutoutAwareRow(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    horizontalSpacing = 8.dp,
                    cutoutClearance = 8.dp,
                    cutoutRectsOverride = listOf(rightSideCutout)
                ) {
                    Box(modifier = Modifier.size(30.dp, 24.dp).testTag("rtl0"))
                    Box(modifier = Modifier.size(30.dp, 24.dp).testTag("rtl1"))
                }
            }
        }

        val rtl0Bounds = composeTestRule.onNodeWithTag("rtl0").getUnclippedBoundsInRoot()
        val rtl1Bounds = composeTestRule.onNodeWithTag("rtl1").getUnclippedBoundsInRoot()

        assertThat(rtl0Bounds.left).isEqualTo(252.dp)
        assertThat(rtl0Bounds.right).isEqualTo(282.dp)
        assertThat(rtl1Bounds.left).isEqualTo(214.dp)
        assertThat(rtl1Bounds.right).isEqualTo(244.dp)
    }

    @Test
    fun cutoutAwareRow_nullOverride_laysOutChildrenInLtrAndRtl() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var hostView: android.view.View? = null
        composeTestRule.setContent {
            hostView = androidx.compose.ui.platform.LocalView.current
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CutoutAwareRow(
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    horizontalSpacing = 8.dp,
                    cutoutClearance = 8.dp,
                    cutoutRectsOverride = null
                ) {
                    Box(modifier = Modifier.size(30.dp, 24.dp).testTag("live0"))
                    Box(modifier = Modifier.size(30.dp, 24.dp).testTag("live1"))
                }
            }
        }

        val cutout = androidx.core.view.DisplayCutoutCompat(
            android.graphics.Rect(0, 48, 0, 0),
            listOf(android.graphics.Rect(10, 0, 50, 48))
        )
        val insets = androidx.core.view.WindowInsetsCompat.Builder()
            .setDisplayCutout(cutout)
            .build()
        composeTestRule.runOnUiThread {
            androidx.core.view.ViewCompat.dispatchApplyWindowInsets(checkNotNull(hostView), insets)
        }
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithTag("live0").getUnclippedBoundsInRoot().left)
            .isAtLeast(0.dp)

        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithTag("live0").getUnclippedBoundsInRoot().right)
            .isAtMost(360.dp)

        val windowRects = listOf(android.graphics.Rect(20, 4, 60, 44))
        val rowBounds = IntRect(left = 10, top = 4, right = 370, bottom = 52)
        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = windowRects,
                rowBoundsInWindow = rowBounds,
                isRtl = false,
                rtlReferenceWidth = 360
            )
        ).containsExactly(IntRect(left = 10, top = 0, right = 50, bottom = 40))

        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = windowRects,
                rowBoundsInWindow = rowBounds,
                isRtl = true,
                rtlReferenceWidth = 360
            )
        ).containsExactly(IntRect(left = 310, top = 0, right = 350, bottom = 40))
    }
}
