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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntRect
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
}
