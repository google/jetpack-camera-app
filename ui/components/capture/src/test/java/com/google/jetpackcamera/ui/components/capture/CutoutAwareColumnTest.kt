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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.view.DisplayCutoutCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
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

/**
 * Tests for [CutoutAwareColumn] and [VerticalIndicatorPill] verifying vertical cutout avoidance,
 * LTR/RTL width and coordinate symmetry, pill segment grouping/splitting, and vertical animation
 * stability.
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
                    Box(Modifier.size(24.dp).testTag("dyn$i"))
                }
            }
        }

        // With cutout at [44, 90] + 8dp clearance -> [36, 98], dyn1 is locked at 98dp.
        assertThat(composeTestRule.onNodeWithTag("dyn1").getUnclippedBoundsInRoot().top)
            .isEqualTo(98.dp)

        // Remove dyn1 from composition so lockedMinY prunes indices >= 1, then clear cutouts.
        itemCount = 1
        composeTestRule.waitForIdle()
        cutouts = emptyList()
        itemCount = 2
        composeTestRule.waitForIdle()

        assertThat(composeTestRule.onNodeWithTag("dyn1").getUnclippedBoundsInRoot().top)
            .isEqualTo(32.dp)
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
