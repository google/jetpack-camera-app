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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.jetpackcamera.model.DEFAULT_HDR_DYNAMIC_RANGE
import com.google.jetpackcamera.model.DEFAULT_HDR_IMAGE_OUTPUT
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashModeIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.HdrIndicator
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val BOX_DYNAMIC_ITEM_0_TAG = "box_dynamic_item_0"
private const val BOX_DYNAMIC_ITEM_1_TAG = "box_dynamic_item_1"
private val DYNAMIC_ITEM_TAGS = listOf(BOX_DYNAMIC_ITEM_0_TAG, BOX_DYNAMIC_ITEM_1_TAG)
private const val PILL_PUBLIC_VERTICAL_TAG = "pill_public_vertical"
private const val PILL_PUBLIC_DELEGATED_VERTICAL_TAG = "pill_public_delegated_vertical"

/**
 * Tests for [CutoutAwareColumn] and [VerticalIndicatorPill] verifying vertical cutout avoidance,
 * LTR/RTL width and coordinate symmetry, pill segment grouping/splitting, vertical animation
 * stability, traversal-group semantics, and immediate semantics pruning on exiting indicators.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CutoutAwareColumnTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun noCutout_placesItemsInStandardVerticalSequence_ltrAndRtl() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CutoutAwareColumn(
                    modifier = Modifier
                        .width(48.dp)
                        .fillMaxHeight(),
                    verticalSpacing = 8.dp,
                    cutoutClearance = 8.dp,
                    cutoutRectsOverride = emptyList()
                ) {
                    Box(Modifier.size(24.dp).testTag("item0"))
                    Box(Modifier.size(24.dp).testTag("item1"))
                    Box(Modifier.size(24.dp).testTag("item2"))
                }
            }
        }

        assertThat(composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot().top)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag("item1").getUnclippedBoundsInRoot().top)
            .isEqualTo(32.dp)
        assertThat(composeTestRule.onNodeWithTag("item2").getUnclippedBoundsInRoot().top)
            .isEqualTo(64.dp)

        // Vertical order remains Top -> Bottom in RTL (y is never inverted).
        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot().top)
            .isEqualTo(0.dp)
        assertThat(composeTestRule.onNodeWithTag("item1").getUnclippedBoundsInRoot().top)
            .isEqualTo(32.dp)
        assertThat(composeTestRule.onNodeWithTag("item2").getUnclippedBoundsInRoot().top)
            .isEqualTo(64.dp)
    }

    @Test
    fun topCornerCutout_shiftsAllItemsDownPastCutout() {
        // Cutout occupies y=[0, 50] with 8dp clearance -> vertical keep-out range [0, 58].
        composeTestRule.setContent {
            CutoutAwareColumn(
                modifier = Modifier
                    .width(48.dp)
                    .fillMaxHeight(),
                verticalSpacing = 8.dp,
                cutoutClearance = 8.dp,
                cutoutRectsOverride = listOf(IntRect(0, 0, 48, 50))
            ) {
                Box(Modifier.size(24.dp).testTag("item0"))
                Box(Modifier.size(24.dp).testTag("item1"))
            }
        }

        assertThat(composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot().top)
            .isEqualTo(58.dp)
        assertThat(composeTestRule.onNodeWithTag("item1").getUnclippedBoundsInRoot().top)
            .isEqualTo(90.dp)
    }

    @Test
    fun startAlignedColumn_rtlLayoutDirection_reflectsHorizontalPillAndCutoutWithoutInvertingY() {
        // In a 60dp-wide column with Alignment.Start and pillMinWidth = 40dp:
        // - In LTR, start-aligned pill sits at x=[0, 40].
        // - In RTL, start-aligned pill reflects to physical x=[20, 60].
        // A cutout at physical local x=[20, 60], y=[60, 100] overlaps the column in both
        // directions (keep-out y=[52, 108]), splitting items across y=[0..36] and y=[108..144].
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var pillSegments: List<IntRect> = emptyList()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CutoutAwareColumn(
                    modifier = Modifier.width(60.dp),
                    verticalSpacing = 0.dp,
                    cutoutClearance = 8.dp,
                    pillVerticalPadding = 6.dp,
                    pillMinWidth = 40.dp,
                    horizontalAlignment = Alignment.Start,
                    cutoutRectsOverride = listOf(
                        IntRect(left = 20, top = 60, right = 60, bottom = 100)
                    ),
                    onPillSegmentsPlaced = { pillSegments = it }
                ) {
                    Box(Modifier.size(width = 40.dp, height = 24.dp).testTag("item0"))
                    Box(Modifier.size(width = 40.dp, height = 24.dp).testTag("item1"))
                }
            }
        }

        assertThat(pillSegments).containsExactly(
            IntRect(left = 0, top = 0, right = 40, bottom = 36),
            IntRect(left = 0, top = 108, right = 40, bottom = 144)
        ).inOrder()

        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()

        assertThat(pillSegments).containsExactly(
            IntRect(left = 20, top = 0, right = 60, bottom = 36),
            IntRect(left = 20, top = 108, right = 60, bottom = 144)
        ).inOrder()
        val item0Rtl = composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot()
        val item1Rtl = composeTestRule.onNodeWithTag("item1").getUnclippedBoundsInRoot()
        assertThat(item0Rtl.left).isEqualTo(20.dp)
        assertThat(item0Rtl.top).isEqualTo(6.dp)
        assertThat(item1Rtl.left).isEqualTo(20.dp)
        assertThat(item1Rtl.top).isEqualTo(114.dp)
    }

    @Test
    fun cutoutAwareColumn_nullOverride_laysOutChildrenInLtrAndRtl() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var hostView: android.view.View? = null
        composeTestRule.setContent {
            hostView = LocalView.current
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CutoutAwareColumn(
                    modifier = Modifier.width(48.dp).testTag("col"),
                    verticalSpacing = 8.dp,
                    cutoutClearance = 8.dp,
                    cutoutRectsOverride = null
                ) {
                    Box(modifier = Modifier.size(24.dp, 30.dp).testTag("live0"))
                    Box(modifier = Modifier.size(24.dp, 30.dp).testTag("live1"))
                }
            }
        }

        val cutout = DisplayCutoutCompat(
            android.graphics.Rect(48, 0, 0, 0),
            listOf(android.graphics.Rect(0, 10, 48, 50))
        )
        val insets = WindowInsetsCompat.Builder()
            .setDisplayCutout(cutout)
            .build()
        composeTestRule.runOnUiThread {
            ViewCompat.dispatchApplyWindowInsets(checkNotNull(hostView), insets)
        }
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithTag("live0").getUnclippedBoundsInRoot().top)
            .isAtLeast(0.dp)
        assertThat(composeTestRule.onNodeWithTag("col").getUnclippedBoundsInRoot().width)
            .isEqualTo(48.dp)

        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithTag("live0").getUnclippedBoundsInRoot().top)
            .isAtLeast(0.dp)
        assertThat(composeTestRule.onNodeWithTag("col").getUnclippedBoundsInRoot().width)
            .isEqualTo(48.dp)
    }

    @Test
    fun cutoutAwareColumn_shrinkingChildCount_evictsStaleLockedMinY() {
        var itemCount by mutableIntStateOf(2)
        var cutouts by mutableStateOf(listOf(IntRect(0, 44, 48, 90)))

        composeTestRule.setContent {
            CutoutAwareColumn(
                modifier = Modifier.width(48.dp),
                verticalSpacing = 8.dp,
                cutoutClearance = 8.dp,
                cutoutRectsOverride = cutouts
            ) {
                for (i in 0 until itemCount) {
                    Box(Modifier.size(24.dp).testTag(DYNAMIC_ITEM_TAGS[i]))
                }
            }
        }

        // With cutout at [44, 90] + 8dp clearance -> [36, 98], second item is locked at 98dp.
        assertThat(
            composeTestRule.onNodeWithTag(BOX_DYNAMIC_ITEM_1_TAG).getUnclippedBoundsInRoot().top
        ).isEqualTo(98.dp)

        // Remove second item from composition so lockedMinY prunes indices >= 1, then clear
        // cutouts.
        itemCount = 1
        composeTestRule.waitForIdle()
        cutouts = emptyList()
        itemCount = 2
        composeTestRule.waitForIdle()

        assertThat(
            composeTestRule.onNodeWithTag(BOX_DYNAMIC_ITEM_1_TAG).getUnclippedBoundsInRoot().top
        ).isEqualTo(32.dp)
    }

    @Test
    fun verticalIndicatorPill_noActiveIndicators_drawsNoPillSegments() {
        var pillSegments: List<IntRect> = listOf(IntRect(0, 0, 10, 10))
        composeTestRule.setContent {
            VerticalIndicatorPill(
                cutoutRectsOverride = emptyList(),
                onPillSegmentsPlaced = { pillSegments = it }
            ) {
                FlashModeIndicator(
                    flashModeUiState = FlashModeUiState.Available(
                        selectedFlashMode = FlashMode.OFF,
                        availableFlashModes = listOf(
                            SingleSelectableUiState.SelectableUi(FlashMode.OFF)
                        ),
                        isLowLightBoostActive = false
                    ),
                    modifier = Modifier.testTag("flash")
                )
                HdrIndicator(
                    hdrUiState = HdrUiState.Unavailable,
                    modifier = Modifier.testTag("hdr")
                )
                VideoQualityIcon(
                    videoQuality = VideoQuality.UNSPECIFIED,
                    modifier = Modifier.testTag("vq")
                )
                StabilizationIcon(
                    stabilizationUiState = StabilizationUiState.Specific(
                        stabilizationMode = StabilizationMode.ON,
                        active = false
                    ),
                    modifier = Modifier.testTag("stab")
                )
            }
        }

        assertThat(pillSegments).isEmpty()
        composeTestRule.onNodeWithTag("flash").assertDoesNotExist()
        composeTestRule.onNodeWithTag("hdr").assertDoesNotExist()
        composeTestRule.onNodeWithTag("vq").assertDoesNotExist()
        composeTestRule.onNodeWithTag("stab").assertDoesNotExist()
    }

    @Test
    fun verticalIndicatorPill_oneActiveIndicator_renders40x48Pill() {
        var pillSegments: List<IntRect> = emptyList()
        composeTestRule.setContent {
            VerticalIndicatorPill(
                cutoutRectsOverride = emptyList(),
                onPillSegmentsPlaced = { pillSegments = it }
            ) {
                FlashModeIndicator(
                    flashModeUiState = activeFlashUiState(),
                    modifier = Modifier.testTag("flash")
                )
                HdrIndicator(
                    hdrUiState = HdrUiState.Unavailable,
                    modifier = Modifier.testTag("hdr")
                )
            }
        }

        // Centered 40dp pill inside 48dp slot -> left = 4, right = 44; height = 6 + 36 + 6 = 48.
        assertThat(pillSegments).containsExactly(
            IntRect(left = 4, top = 0, right = 44, bottom = 48)
        )
        val flashBounds = composeTestRule.onNodeWithTag("flash").getUnclippedBoundsInRoot()
        assertThat(flashBounds.left).isEqualTo(4.dp)
        assertThat(flashBounds.right).isEqualTo(44.dp)
        assertThat(flashBounds.top).isEqualTo(6.dp)
        assertThat(flashBounds.bottom).isEqualTo(42.dp)
    }

    @Test
    fun verticalIndicatorPill_twoActiveIndicators_sharesSingle40x84Pill() {
        var pillSegments: List<IntRect> = emptyList()
        composeTestRule.setContent {
            VerticalIndicatorPill(
                cutoutRectsOverride = emptyList(),
                onPillSegmentsPlaced = { pillSegments = it }
            ) {
                FlashModeIndicator(
                    flashModeUiState = activeFlashUiState(),
                    modifier = Modifier.testTag("flash")
                )
                HdrIndicator(
                    hdrUiState = activeHdrUiState(),
                    modifier = Modifier.testTag("hdr")
                )
            }
        }

        // Height = 6 + 36 + 36 + 6 = 84; width = 40 centered at [4, 44].
        assertThat(pillSegments).containsExactly(
            IntRect(left = 4, top = 0, right = 44, bottom = 84)
        )
        val flashBounds = composeTestRule.onNodeWithTag("flash").getUnclippedBoundsInRoot()
        val hdrBounds = composeTestRule.onNodeWithTag("hdr").getUnclippedBoundsInRoot()
        assertThat(flashBounds.top).isEqualTo(6.dp)
        assertThat(flashBounds.bottom).isEqualTo(42.dp)
        assertThat(hdrBounds.top).isEqualTo(42.dp)
        assertThat(hdrBounds.bottom).isEqualTo(78.dp)
    }

    @Test
    fun verticalIndicatorPill_widerParent_preserves48dpWidthAndOrderInLtrAndRtl() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var pillSegments: List<IntRect> = emptyList()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                Box(modifier = Modifier.widthIn(max = 400.dp)) {
                    VerticalIndicatorPill(
                        modifier = Modifier.testTag("vertical_pill"),
                        cutoutRectsOverride = emptyList(),
                        onPillSegmentsPlaced = { pillSegments = it }
                    ) {
                        FlashModeIndicator(
                            flashModeUiState = activeFlashUiState(),
                            modifier = Modifier.testTag("flash")
                        )
                        HdrIndicator(
                            hdrUiState = activeHdrUiState(),
                            modifier = Modifier.testTag("hdr")
                        )
                        VideoQualityIcon(
                            videoQuality = VideoQuality.UHD,
                            modifier = Modifier.testTag("vq")
                        )
                        StabilizationIcon(
                            stabilizationUiState = activeStabilizationUiState(),
                            modifier = Modifier.testTag("stab")
                        )
                    }
                }
            }
        }

        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            layoutDirection = direction
            composeTestRule.waitForIdle()

            val containerBounds =
                composeTestRule.onNodeWithTag("vertical_pill").getUnclippedBoundsInRoot()
            assertWithMessage("Container width in $direction")
                .that(containerBounds.width)
                .isEqualTo(48.dp)
            assertWithMessage("Pill segments in $direction")
                .that(pillSegments)
                .containsExactly(IntRect(left = 4, top = 0, right = 44, bottom = 156))

            // Verify strict Top -> Bottom stacking: Flash -> HDR -> VideoQuality -> Stabilization.
            assertThat(composeTestRule.onNodeWithTag("flash").getUnclippedBoundsInRoot().top)
                .isEqualTo(6.dp)
            assertThat(composeTestRule.onNodeWithTag("hdr").getUnclippedBoundsInRoot().top)
                .isEqualTo(42.dp)
            assertThat(composeTestRule.onNodeWithTag("vq").getUnclippedBoundsInRoot().top)
                .isEqualTo(78.dp)
            assertThat(composeTestRule.onNodeWithTag("stab").getUnclippedBoundsInRoot().top)
                .isEqualTo(114.dp)
        }
    }

    @Test
    fun verticalIndicatorPill_cutoutBetweenIndicators_splitsIntoTwoSeparatePillsInLtrAndRtl() {
        // Cutout occupies y=[60, 100] with 8dp clearance -> vertical keep-out range [52, 108].
        // First indicator pill y=[0, 48] fits above the cutout.
        // Second and third indicators shift below to y=[108, 192] as a second 40x84dp pill.
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var pillSegments: List<IntRect> = emptyList()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                VerticalIndicatorPill(
                    cutoutRectsOverride = listOf(
                        IntRect(left = 0, top = 60, right = 48, bottom = 100)
                    ),
                    onPillSegmentsPlaced = { pillSegments = it }
                ) {
                    FlashModeIndicator(
                        flashModeUiState = activeFlashUiState(),
                        modifier = Modifier.testTag("flash")
                    )
                    VideoQualityIcon(
                        videoQuality = VideoQuality.FHD,
                        modifier = Modifier.testTag("vq")
                    )
                    StabilizationIcon(
                        stabilizationUiState = activeStabilizationUiState(),
                        modifier = Modifier.testTag("stab")
                    )
                }
            }
        }

        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            layoutDirection = direction
            composeTestRule.waitForIdle()

            assertWithMessage("Split pill segments in $direction")
                .that(pillSegments)
                .containsExactly(
                    IntRect(left = 4, top = 0, right = 44, bottom = 48),
                    IntRect(left = 4, top = 108, right = 44, bottom = 192)
                )
                .inOrder()
            assertThat(composeTestRule.onNodeWithTag("flash").getUnclippedBoundsInRoot().top)
                .isEqualTo(6.dp)
            assertThat(composeTestRule.onNodeWithTag("vq").getUnclippedBoundsInRoot().top)
                .isEqualTo(114.dp)
            assertThat(composeTestRule.onNodeWithTag("stab").getUnclippedBoundsInRoot().top)
                .isEqualTo(150.dp)
        }
    }

    @Test
    fun verticalIndicatorPill_horizontallyNonOverlappingCutouts_areIgnoredInLtrAndRtl() {
        // Cutouts outside the 48dp horizontal slot ([80, 140] to the right and [-100, -20] to the
        // left) do not intersect the column horizontally and must not shift or split the pill.
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var pillSegments: List<IntRect> = emptyList()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                VerticalIndicatorPill(
                    cutoutRectsOverride = listOf(
                        IntRect(left = 80, top = 10, right = 140, bottom = 70),
                        IntRect(left = -100, top = 10, right = -20, bottom = 70)
                    ),
                    onPillSegmentsPlaced = { pillSegments = it }
                ) {
                    FlashModeIndicator(
                        flashModeUiState = activeFlashUiState(),
                        modifier = Modifier.testTag("flash")
                    )
                    HdrIndicator(
                        hdrUiState = activeHdrUiState(),
                        modifier = Modifier.testTag("hdr")
                    )
                }
            }
        }

        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            layoutDirection = direction
            composeTestRule.waitForIdle()

            assertWithMessage("Unshifted pill in $direction")
                .that(pillSegments)
                .containsExactly(IntRect(left = 4, top = 0, right = 44, bottom = 84))
            assertThat(composeTestRule.onNodeWithTag("flash").getUnclippedBoundsInRoot().top)
                .isEqualTo(6.dp)
            assertThat(composeTestRule.onNodeWithTag("hdr").getUnclippedBoundsInRoot().top)
                .isEqualTo(42.dp)
        }
    }

    @Test
    fun indicatorPillIcon_expandAndShrinkVertically_neverTeleportsAcrossCutoutMidAnimation() {
        // Cutout occupies y=[62, 100] with 8dp clearance -> vertical keep-out range [54, 108].
        // Flash (index 0) occupies pill y=[0, 48] (icon y=[6, 42]).
        // Notice 42dp + small animated height + 6dp bottom padding <= 54dp on early/late frames,
        // so without LookaheadScope + maxIntrinsicHeight + lockedMinY, HDR (index 1) would start
        // or end at y=42dp above the cutout instead of staying anchored at y=114dp below [54, 108].
        var isHdrVisible by mutableStateOf(false)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            VerticalIndicatorPill(
                cutoutRectsOverride = listOf(IntRect(left = 0, top = 62, right = 48, bottom = 100))
            ) {
                FlashModeIndicator(
                    flashModeUiState = activeFlashUiState(),
                    modifier = Modifier.testTag("flash")
                )
                HdrIndicator(
                    hdrUiState = if (isHdrVisible) activeHdrUiState() else HdrUiState.Unavailable,
                    modifier = Modifier.testTag("hdr")
                )
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()

        // Trigger vertical enter animation (height 0dp -> 36dp).
        isHdrVisible = true
        composeTestRule.mainClock.advanceTimeByFrame()

        var observedEnteringFrames = 0
        var observedIntermediateEnterHeight = false
        for (step in 1..30) {
            composeTestRule.mainClock.advanceTimeBy(16)
            val nodes = composeTestRule.onAllNodes(hasTestTag("hdr")).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                val clipped = composeTestRule.onNodeWithTag("hdr").getBoundsInRoot()
                if (clipped.height > 0.dp) {
                    observedEnteringFrames++
                    if (clipped.height < 36.dp) {
                        observedIntermediateEnterHeight = true
                        // Width is already full 40dp during vertical expansion.
                        assertThat(clipped.width).isEqualTo(40.dp)
                    }
                    assertWithMessage("Enter step=$step clipped=$clipped")
                        .that(clipped.top)
                        .isEqualTo(114.dp)
                }
            }
        }
        assertThat(observedEnteringFrames).isGreaterThan(3)
        assertThat(observedIntermediateEnterHeight).isTrue()

        // Allow enter transition to settle completely.
        composeTestRule.mainClock.advanceTimeBy(500)

        // Trigger vertical exit animation (height 36dp -> 0dp).
        isHdrVisible = false
        composeTestRule.mainClock.advanceTimeByFrame()

        var observedExitingFrames = 0
        var observedIntermediateExitHeight = false
        for (step in 1..30) {
            composeTestRule.mainClock.advanceTimeBy(16)
            val nodes = composeTestRule.onAllNodes(hasTestTag("hdr")).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                val clipped = composeTestRule.onNodeWithTag("hdr").getBoundsInRoot()
                if (clipped.height > 0.dp) {
                    observedExitingFrames++
                    if (clipped.height < 36.dp) {
                        observedIntermediateExitHeight = true
                        assertThat(clipped.width).isEqualTo(40.dp)
                    }
                    assertWithMessage("Exit step=$step clipped=$clipped")
                        .that(clipped.top)
                        .isEqualTo(114.dp)
                }
            }
        }
        assertThat(observedExitingFrames).isGreaterThan(3)
        assertThat(observedIntermediateExitHeight).isTrue()
    }

    @Test
    fun indicatorPillIcon_disableAnimationsTrue_snapsImmediatelyInVerticalPill() {
        var isHdrVisible by mutableStateOf(false)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                VerticalIndicatorPill(cutoutRectsOverride = emptyList()) {
                    FlashModeIndicator(
                        flashModeUiState = activeFlashUiState(),
                        modifier = Modifier.testTag("flash")
                    )
                    HdrIndicator(
                        hdrUiState = if (isHdrVisible) {
                            activeHdrUiState()
                        } else {
                            HdrUiState.Unavailable
                        },
                        modifier = Modifier.testTag("hdr")
                    )
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithTag("hdr").assertDoesNotExist()

        // First frame schedules recomposition; second frame materializes the child snapped directly
        // to its full 40x36dp target bounds with zero intermediate animation heights.
        isHdrVisible = true
        repeat(2) {
            composeTestRule.mainClock.advanceTimeByFrame()
            val nodes = composeTestRule.onAllNodes(hasTestTag("hdr")).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                val bounds = composeTestRule.onNodeWithTag("hdr").getBoundsInRoot()
                assertThat(bounds.width).isEqualTo(40.dp)
                assertThat(bounds.height).isEqualTo(36.dp)
            }
        }
        val enteredBounds = composeTestRule.onNodeWithTag("hdr").getBoundsInRoot()
        assertThat(enteredBounds.width).isEqualTo(40.dp)
        assertThat(enteredBounds.height).isEqualTo(36.dp)

        // Exiting with animations disabled removes the child immediately without shrinking frames.
        isHdrVisible = false
        repeat(2) {
            composeTestRule.mainClock.advanceTimeByFrame()
            val nodes = composeTestRule.onAllNodes(hasTestTag("hdr")).fetchSemanticsNodes()
            if (nodes.isNotEmpty()) {
                val bounds = composeTestRule.onNodeWithTag("hdr").getBoundsInRoot()
                assertThat(bounds.height).isEqualTo(36.dp)
            }
        }
        composeTestRule.onNodeWithTag("hdr").assertDoesNotExist()
    }

    @Test
    fun absoluteAlignedColumn_ltrAndRtl_pinsPillAndItemsToPhysicalEdges() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var alignment by mutableStateOf<Alignment.Horizontal>(AbsoluteAlignment.Left)
        var pillSegments: List<IntRect> = emptyList()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                CutoutAwareColumn(
                    modifier = Modifier.width(60.dp),
                    verticalSpacing = 0.dp,
                    pillVerticalPadding = 6.dp,
                    pillMinWidth = 40.dp,
                    horizontalAlignment = alignment,
                    cutoutRectsOverride = emptyList(),
                    onPillSegmentsPlaced = { pillSegments = it }
                ) {
                    Box(Modifier.size(width = 40.dp, height = 24.dp).testTag("item0"))
                }
            }
        }

        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            alignment = AbsoluteAlignment.Left
            layoutDirection = direction
            composeTestRule.waitForIdle()

            assertWithMessage("AbsoluteAlignment.Left pill in $direction")
                .that(pillSegments)
                .containsExactly(IntRect(left = 0, top = 0, right = 40, bottom = 36))
            assertThat(composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot().left)
                .isEqualTo(0.dp)

            alignment = AbsoluteAlignment.Right
            composeTestRule.waitForIdle()

            assertWithMessage("AbsoluteAlignment.Right pill in $direction")
                .that(pillSegments)
                .containsExactly(IntRect(left = 20, top = 0, right = 60, bottom = 36))
            assertThat(composeTestRule.onNodeWithTag("item0").getUnclippedBoundsInRoot().left)
                .isEqualTo(20.dp)
        }
    }

    @Test
    fun statusContainers_exposeIsTraversalGroupSemantics() {
        composeTestRule.setContent {
            Column {
                CutoutAwareColumn(
                    modifier = Modifier.testTag("cutout_column"),
                    cutoutRectsOverride = emptyList()
                ) {
                    Box(Modifier.size(24.dp))
                }
                VerticalIndicatorPill(
                    modifier = Modifier.testTag("vertical_pill"),
                    cutoutRectsOverride = emptyList()
                ) {
                    FlashModeIndicator(flashModeUiState = activeFlashUiState())
                }
                HorizontalIndicatorPill(
                    modifier = Modifier.testTag("horizontal_pill_direct"),
                    cutoutRectsOverride = emptyList()
                ) {
                    FlashModeIndicator(flashModeUiState = activeFlashUiState())
                }
                IndicatorPill(
                    modifier = Modifier.testTag("horizontal_pill"),
                    cutoutRectsOverride = emptyList()
                ) {
                    FlashModeIndicator(flashModeUiState = activeFlashUiState())
                }
                IndicatorPill(
                    modifier = Modifier.testTag("delegated_vertical_pill"),
                    isVertical = true,
                    cutoutRectsOverride = emptyList()
                ) {
                    FlashModeIndicator(flashModeUiState = activeFlashUiState())
                }
                VerticalIndicatorPill(
                    modifier = Modifier.testTag(PILL_PUBLIC_VERTICAL_TAG)
                ) {
                    FlashModeIndicator(flashModeUiState = activeFlashUiState())
                }
                IndicatorPill(
                    modifier = Modifier.testTag(PILL_PUBLIC_DELEGATED_VERTICAL_TAG),
                    isVertical = true
                ) {
                    FlashModeIndicator(flashModeUiState = activeFlashUiState())
                }
            }
        }

        val isTraversalGroupMatcher =
            SemanticsMatcher.expectValue(SemanticsProperties.IsTraversalGroup, true)
        composeTestRule.onNodeWithTag("cutout_column").assert(isTraversalGroupMatcher)
        composeTestRule.onNodeWithTag("vertical_pill").assert(isTraversalGroupMatcher)
        composeTestRule.onNodeWithTag("horizontal_pill_direct").assert(isTraversalGroupMatcher)
        composeTestRule.onNodeWithTag("horizontal_pill").assert(isTraversalGroupMatcher)
        composeTestRule.onNodeWithTag("delegated_vertical_pill").assert(isTraversalGroupMatcher)
        composeTestRule.onNodeWithTag(PILL_PUBLIC_VERTICAL_TAG).assert(isTraversalGroupMatcher)
        composeTestRule.onNodeWithTag(PILL_PUBLIC_DELEGATED_VERTICAL_TAG)
            .assert(isTraversalGroupMatcher)
    }

    @Test
    fun indicatorPill_isVerticalFlag_delegatesToVerticalAndHorizontalLayouts() {
        var isVertical by mutableStateOf(false)
        var pillSegments: List<IntRect> = emptyList()

        composeTestRule.setContent {
            IndicatorPill(
                isVertical = isVertical,
                cutoutRectsOverride = emptyList(),
                onPillSegmentsPlaced = { pillSegments = it }
            ) {
                FlashModeIndicator(
                    flashModeUiState = activeFlashUiState(),
                    modifier = Modifier.testTag("flash")
                )
                HdrIndicator(
                    hdrUiState = activeHdrUiState(),
                    modifier = Modifier.testTag("hdr")
                )
            }
        }

        // Horizontal (isVertical = false): 84x40dp pill, icons side-by-side at x=[6..42], [42..78].
        assertThat(pillSegments).containsExactly(
            IntRect(left = 0, top = 0, right = 84, bottom = 40)
        )
        assertThat(composeTestRule.onNodeWithTag("flash").getUnclippedBoundsInRoot().left)
            .isEqualTo(6.dp)
        assertThat(composeTestRule.onNodeWithTag("hdr").getUnclippedBoundsInRoot().left)
            .isEqualTo(42.dp)

        // Vertical (isVertical = true): 40x84dp pill centered at x=[4..44],
        // icons stacked at y=[6..42], [42..78].
        isVertical = true
        composeTestRule.waitForIdle()

        assertThat(pillSegments).containsExactly(
            IntRect(left = 4, top = 0, right = 44, bottom = 84)
        )
        assertThat(composeTestRule.onNodeWithTag("flash").getUnclippedBoundsInRoot().top)
            .isEqualTo(6.dp)
        assertThat(composeTestRule.onNodeWithTag("hdr").getUnclippedBoundsInRoot().top)
            .isEqualTo(42.dp)
    }

    @Test
    fun verticalIndicatorPill_allIndicatorsInactive_emitsZeroChildAndImageSemanticsNodes() {
        composeTestRule.setContent {
            VerticalIndicatorPill(
                modifier = Modifier.testTag("vertical_pill"),
                cutoutRectsOverride = emptyList()
            ) {
                FlashModeIndicator(flashModeUiState = inactiveFlashUiState())
                HdrIndicator(hdrUiState = HdrUiState.Unavailable)
                VideoQualityIcon(videoQuality = VideoQuality.UNSPECIFIED)
                StabilizationIcon(
                    stabilizationUiState = StabilizationUiState.Specific(
                        stabilizationMode = StabilizationMode.ON,
                        active = false
                    )
                )
            }
        }

        assertThat(composeTestRule.onNodeWithTag("vertical_pill").fetchSemanticsNode().children)
            .isEmpty()
        composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image),
            useUnmergedTree = true
        ).assertCountEquals(0)
        composeTestRule.onAllNodes(
            SemanticsMatcher.keyIsDefined(SemanticsProperties.ContentDescription),
            useUnmergedTree = true
        ).assertCountEquals(0)
    }

    @Test
    fun indicatorPillIcon_exitingWithAnimationsEnabled_immediatelyClearsSemanticsMidShrink() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val flashOnDescription = context.getString(R.string.quick_settings_flash_on_description)
        val hdrDescription =
            context.getString(R.string.quick_settings_dynamic_range_hdr_description)
        var isActive by mutableStateOf(true)
        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides false) {
                Column {
                    VerticalIndicatorPill(cutoutRectsOverride = emptyList()) {
                        FlashModeIndicator(
                            flashModeUiState = if (isActive) {
                                activeFlashUiState()
                            } else {
                                inactiveFlashUiState()
                            },
                            modifier = Modifier.testTag("flash")
                        )
                    }
                    IndicatorPill(cutoutRectsOverride = emptyList()) {
                        HdrIndicator(
                            hdrUiState = if (isActive) {
                                activeHdrUiState()
                            } else {
                                HdrUiState.Unavailable
                            },
                            modifier = Modifier.testTag("hdr")
                        )
                    }
                }
            }
        }

        composeTestRule.mainClock.advanceTimeByFrame()
        composeTestRule.onNodeWithContentDescription(flashOnDescription).assertExists()
        composeTestRule.onNodeWithContentDescription(hdrDescription).assertExists()

        // Transition both indicators to inactive while shrink + fadeOut animations are enabled.
        isActive = false
        Snapshot.sendApplyNotifications()
        composeTestRule.mainClock.advanceTimeByFrame()

        // Semantics must be pruned immediately while the exiting wrappers are still composed.
        composeTestRule.onNodeWithTag("flash").assertExists()
        composeTestRule.onNodeWithTag("hdr").assertExists()
        composeTestRule.onNodeWithContentDescription(flashOnDescription).assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription(hdrDescription).assertDoesNotExist()
        composeTestRule.onAllNodes(
            SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image)
        ).assertCountEquals(0)

        // Verify the icons are still mid-shrink visually while remaining absent from semantics.
        var observedMidVerticalShrink = false
        var observedMidHorizontalShrink = false
        for (step in 1..20) {
            val flashNodes = composeTestRule.onAllNodes(hasTestTag("flash")).fetchSemanticsNodes()
            if (flashNodes.isNotEmpty()) {
                val flashHeight = composeTestRule.onNodeWithTag("flash").getBoundsInRoot().height
                if (flashHeight > 0.dp && flashHeight < 36.dp) {
                    observedMidVerticalShrink = true
                }
            }
            val hdrNodes = composeTestRule.onAllNodes(hasTestTag("hdr")).fetchSemanticsNodes()
            if (hdrNodes.isNotEmpty()) {
                val hdrWidth = composeTestRule.onNodeWithTag("hdr").getBoundsInRoot().width
                if (hdrWidth > 0.dp && hdrWidth < 36.dp) {
                    observedMidHorizontalShrink = true
                }
            }
            composeTestRule.onNodeWithContentDescription(flashOnDescription).assertDoesNotExist()
            composeTestRule.onNodeWithContentDescription(hdrDescription).assertDoesNotExist()
            composeTestRule.onAllNodes(
                SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Image)
            ).assertCountEquals(0)
            composeTestRule.mainClock.advanceTimeBy(16)
        }
        assertThat(observedMidVerticalShrink).isTrue()
        assertThat(observedMidHorizontalShrink).isTrue()
    }

    @Test
    fun resolveLocalCutoutRects_multipleWindowCutouts_transformsToLocalCoordinates() {
        val windowCutoutRects = listOf(
            android.graphics.Rect(0, 0, 48, 36),
            android.graphics.Rect(20, 100, 52, 140),
            android.graphics.Rect(300, 200, 360, 240)
        )
        val anchor = WindowAnchor(left = 12, top = 40, width = 48)

        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = windowCutoutRects,
                windowAnchor = anchor,
                isRtl = false,
                rtlReferenceWidth = 48
            )
        ).containsExactly(
            IntRect(left = -12, top = -40, right = 36, bottom = -4),
            IntRect(left = 8, top = 60, right = 40, bottom = 100),
            IntRect(left = 288, top = 160, right = 348, bottom = 200)
        ).inOrder()

        // In RTL with a resolved windowAnchor, horizontal coordinates mirror around anchor.width
        // while vertical coordinates stay top-to-bottom.
        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = windowCutoutRects,
                windowAnchor = anchor,
                isRtl = true,
                rtlReferenceWidth = 60
            )
        ).containsExactly(
            IntRect(left = 12, top = -40, right = 60, bottom = -4),
            IntRect(left = 8, top = 60, right = 40, bottom = 100),
            IntRect(left = -300, top = 160, right = -240, bottom = 200)
        ).inOrder()
    }

    @Test
    fun resolveLocalCutoutRects_nullAnchorAndOverride_handlesFallbackAndPrecedence() {
        val windowCutoutRects = listOf(
            android.graphics.Rect(4, 10, 44, 50),
            android.graphics.Rect(10, 120, 38, 160)
        )

        // When windowAnchor is null, offsets default to (0, 0) and RTL mirrors around
        // rtlReferenceWidth.
        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = windowCutoutRects,
                windowAnchor = null,
                isRtl = false,
                rtlReferenceWidth = 48
            )
        ).containsExactly(
            IntRect(left = 4, top = 10, right = 44, bottom = 50),
            IntRect(left = 10, top = 120, right = 38, bottom = 160)
        ).inOrder()

        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = windowCutoutRects,
                windowAnchor = null,
                isRtl = true,
                rtlReferenceWidth = 60
            )
        ).containsExactly(
            IntRect(left = 16, top = 10, right = 56, bottom = 50),
            IntRect(left = 22, top = 120, right = 50, bottom = 160)
        ).inOrder()

        // When cutoutRectsOverride is provided, windowCutoutRects and windowAnchor are bypassed.
        val overrideRects = listOf(
            IntRect(left = 0, top = 20, right = 24, bottom = 60),
            IntRect(left = 12, top = 110, right = 48, bottom = 150)
        )
        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = overrideRects,
                windowCutoutRects = windowCutoutRects,
                windowAnchor = WindowAnchor(left = 100, top = 100, width = 48),
                isRtl = true,
                rtlReferenceWidth = 60
            )
        ).containsExactly(
            IntRect(left = 36, top = 20, right = 60, bottom = 60),
            IntRect(left = 12, top = 110, right = 48, bottom = 150)
        ).inOrder()
    }

    @Test
    fun multipleCutouts_clampsKeepOutRangesAndSplitsIndicatorsAcrossThreePillSegments() {
        // Cutouts in local coordinates with 8dp clearance:
        // - [-60, -20] -> bottom + clearance = -12 <= 0 (completely above column, ignored)
        // - [-20, 12]  -> [-28, 20] clamped to [0, 20], shifting first pill to y=20
        // - [80, 116]  -> [72, 124], splitting Flash (pill [20, 68]) from HDR + VideoQuality
        //                 (pill [124, 208])
        // - [220, 256] -> [212, 264], splitting Stabilization into a third pill [264, 312]
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var pillSegments: List<IntRect> = emptyList()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                VerticalIndicatorPill(
                    cutoutRectsOverride = listOf(
                        IntRect(left = 0, top = 220, right = 48, bottom = 256),
                        IntRect(left = 0, top = -60, right = 48, bottom = -20),
                        IntRect(left = 0, top = 80, right = 48, bottom = 116),
                        IntRect(left = 0, top = -20, right = 48, bottom = 12)
                    ),
                    onPillSegmentsPlaced = { pillSegments = it }
                ) {
                    FlashModeIndicator(
                        flashModeUiState = activeFlashUiState(),
                        modifier = Modifier.testTag("flash")
                    )
                    HdrIndicator(
                        hdrUiState = activeHdrUiState(),
                        modifier = Modifier.testTag("hdr")
                    )
                    VideoQualityIcon(
                        videoQuality = VideoQuality.FHD,
                        modifier = Modifier.testTag("vq")
                    )
                    StabilizationIcon(
                        stabilizationUiState = activeStabilizationUiState(),
                        modifier = Modifier.testTag("stab")
                    )
                }
            }
        }

        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            layoutDirection = direction
            composeTestRule.waitForIdle()

            assertWithMessage("Multi-cutout pill segments in $direction")
                .that(pillSegments)
                .containsExactly(
                    IntRect(left = 4, top = 20, right = 44, bottom = 68),
                    IntRect(left = 4, top = 124, right = 44, bottom = 208),
                    IntRect(left = 4, top = 264, right = 44, bottom = 312)
                )
                .inOrder()
            assertThat(composeTestRule.onNodeWithTag("flash").getUnclippedBoundsInRoot().top)
                .isEqualTo(26.dp)
            assertThat(composeTestRule.onNodeWithTag("hdr").getUnclippedBoundsInRoot().top)
                .isEqualTo(130.dp)
            assertThat(composeTestRule.onNodeWithTag("vq").getUnclippedBoundsInRoot().top)
                .isEqualTo(166.dp)
            assertThat(composeTestRule.onNodeWithTag("stab").getUnclippedBoundsInRoot().top)
                .isEqualTo(270.dp)
        }
    }

    @Test
    fun cutoutAwareColumn_shortPillSegmentAndBoundedConstraints_clampsDimensionsCleanly() {
        var pillSegments: List<IntRect> = emptyList()
        composeTestRule.setContent {
            CutoutAwareColumn(
                modifier = Modifier
                    .size(width = 36.dp, height = 14.dp)
                    .testTag("bounded_col"),
                verticalSpacing = 0.dp,
                pillVerticalPadding = 4.dp,
                pillMinWidth = 40.dp,
                pillBackgroundColor = Color.Black,
                cutoutRectsOverride = emptyList(),
                onPillSegmentsPlaced = { pillSegments = it }
            ) {
                Box(Modifier.size(width = 24.dp, height = 8.dp).testTag("short_item"))
            }
        }

        // Pill width clamps to bounded columnWidth (36dp); segment height is 4 + 8 + 4 = 16dp
        // (< 36dp width, exercising min(width, height) / 2f corner radius clamping), while
        // layout height clamps to bounded maxHeight (14dp).
        assertThat(pillSegments).containsExactly(
            IntRect(left = 0, top = 0, right = 36, bottom = 16)
        )
        val colBounds = composeTestRule.onNodeWithTag("bounded_col").getUnclippedBoundsInRoot()
        assertThat(colBounds.width).isEqualTo(36.dp)
        assertThat(colBounds.height).isEqualTo(14.dp)
    }

    private fun inactiveFlashUiState() = FlashModeUiState.Available(
        selectedFlashMode = FlashMode.OFF,
        availableFlashModes = listOf(SingleSelectableUiState.SelectableUi(FlashMode.OFF)),
        isLowLightBoostActive = false
    )

    private fun activeFlashUiState() = FlashModeUiState.Available(
        selectedFlashMode = FlashMode.ON,
        availableFlashModes = listOf(SingleSelectableUiState.SelectableUi(FlashMode.ON)),
        isLowLightBoostActive = false
    )

    private fun activeHdrUiState() = HdrUiState.Available(
        selectedDynamicRange = DEFAULT_HDR_DYNAMIC_RANGE,
        selectedImageFormat = DEFAULT_HDR_IMAGE_OUTPUT,
        isSupported = true
    )

    private fun activeStabilizationUiState() = StabilizationUiState.Specific(
        stabilizationMode = StabilizationMode.ON,
        active = true
    )
}
