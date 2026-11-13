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

import android.view.View
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [CompactShortLayout] across `COMPACT_SHORT` (`360x360dp`, `470x400dp`),
 * `COMPACT_SHORT_SLIM` (`320x340dp`), RTL policies, and visible multi-window `statusBars` insets,
 * asserting zero clipping, zero vertical overlap in both rails, and in-hierarchy
 * [QuickSettingsOverlayCard] bounds.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h360dp")
class CompactShortLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun boundsOf(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun compactShortLayout_at360x360_fitsAllControlsWithoutClippingOrVerticalOverlap() {
        var observedActiveLayout: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                CompactShortLayout(
                    slots = PlaceholderCaptureSlots.sideRail(
                        isCompactHeight = false,
                        includeImageWell = true,
                        includeElapsedTime = true
                    ),
                    viewfinder = { mod ->
                        observedActiveLayout = LocalActiveCaptureLayout.current
                        PlaceholderViewfinder(modifier = mod)
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        assertThat(observedActiveLayout).isEqualTo("CompactShortLayout")

        val viewfinderBounds = boundsOf(PREVIEW_DISPLAY)
        assertThat(viewfinderBounds.left.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.top.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.right.value).isWithin(TOLERANCE).of(360f)
        assertThat(viewfinderBounds.bottom.value).isWithin(TOLERANCE).of(360f)

        assertNoClippingAndNoOverlapInWindow(
            contextLabel = "COMPACT_SHORT 360x360",
            windowWidth = 360f,
            windowHeight = 360f,
            minTop = 0f
        )

        val startRail = boundsOf(START_SIDE_RAIL_TAG)
        val endRail = boundsOf(END_SIDE_RAIL_TAG)
        val carousel = boundsOf(CAPTURE_MODE_CAROUSEL)
        assertThat(startRail.right.value).isAtMost(carousel.left.value + TOLERANCE)
        assertThat(carousel.right.value).isAtMost(endRail.left.value + TOLERANCE)
    }

    @Config(qualifiers = "w320dp-h340dp")
    @Test
    fun compactShortLayout_at320x340_fitsCompactShortSlimWithoutClippingOrOverlap() {
        composeTestRule.setContent {
            MaterialTheme {
                CompactShortLayout(
                    slots = PlaceholderCaptureSlots.sideRail(
                        isCompactHeight = false,
                        includeImageWell = true,
                        includeElapsedTime = true
                    ),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
                )
            }
        }
        composeTestRule.waitForIdle()

        assertNoClippingAndNoOverlapInWindow(
            contextLabel = "COMPACT_SHORT_SLIM 320x340",
            windowWidth = 320f,
            windowHeight = 340f,
            minTop = 0f
        )
    }

    @Config(qualifiers = "w470dp-h400dp")
    @Test
    fun compactShortLayout_rtlDefaultAndMirroredPolicies_preserveVerticalOrderAndChildDirection() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var rtlPolicy by mutableStateOf(CaptureRtlPolicy(mirrorSideRails = false))
        val observedDirections = mutableMapOf<String, LayoutDirection>()

        val baseSlots = PlaceholderCaptureSlots.sideRail(
            isCompactHeight = true,
            includeImageWell = true,
            includeElapsedTime = true
        )
        val instrumentedSlots = baseSlots.copy(
            topBar = { mod ->
                observedDirections["topBar"] = LocalLayoutDirection.current
                baseSlots.topBar(this, mod)
            },
            zoomControls = { mod ->
                observedDirections["zoomControls"] = LocalLayoutDirection.current
                baseSlots.zoomControls(this, mod)
            },
            captureButton = { mod ->
                observedDirections["captureButton"] = LocalLayoutDirection.current
                baseSlots.captureButton(this, mod)
            },
            captureModeToggle = { mod ->
                observedDirections["captureModeToggle"] = LocalLayoutDirection.current
                baseSlots.captureModeToggle(this, mod)
            },
            captureModeCarousel = { mod ->
                observedDirections["captureModeCarousel"] = LocalLayoutDirection.current
                baseSlots.captureModeCarousel(this, mod)
            }
        )

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalCaptureRtlPolicy provides rtlPolicy
                ) {
                    CompactShortLayout(
                        slots = instrumentedSlots,
                        viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val ltrStartTops = START_RAIL_TAGS.map { boundsOf(it).top.value }
        val ltrEndTops = END_RAIL_TAGS.map { boundsOf(it).top.value }

        observedDirections.clear()
        layoutDirection = LayoutDirection.Rtl
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = false)
        composeTestRule.waitForIdle()

        assertThat(boundsOf(START_SIDE_RAIL_TAG).left.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).right.value).isWithin(TOLERANCE).of(470f)
        observedDirections.forEach { (slotName, dir) ->
            assertWithMessage("Slot $slotName must observe LayoutDirection.Rtl")
                .that(dir)
                .isEqualTo(LayoutDirection.Rtl)
        }
        START_RAIL_TAGS.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrStartTops[i])
        }
        END_RAIL_TAGS.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrEndTops[i])
        }

        observedDirections.clear()
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = true)
        composeTestRule.waitForIdle()

        assertThat(boundsOf(START_SIDE_RAIL_TAG).right.value).isWithin(TOLERANCE).of(470f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).left.value).isWithin(TOLERANCE).of(0f)
        assertNoClippingAndNoOverlapInWindow(
            contextLabel = "COMPACT_SHORT 470x400 RTL Mirrored",
            windowWidth = 470f,
            windowHeight = 400f,
            minTop = 0f
        )
        START_RAIL_TAGS.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrStartTops[i])
        }
        END_RAIL_TAGS.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrEndTops[i])
        }
    }

    @Test
    fun compactShortLayout_withMultiWindowVisibleStatusBars_clearsBarAndFits360x360() {
        lateinit var hostView: View
        composeTestRule.setContent {
            hostView = LocalView.current
            MaterialTheme {
                CompactShortLayout(
                    slots = PlaceholderCaptureSlots.sideRail(
                        isCompactHeight = false,
                        includeImageWell = true,
                        includeElapsedTime = true
                    ),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
                )
            }
        }

        dispatchStatusBarInsets(
            view = hostView,
            statusBarHeightPx = MULTI_WINDOW_STATUS_BAR_PX,
            visible = true
        )
        composeTestRule.waitForIdle()

        assertThat(boundsOf(PREVIEW_DISPLAY).top.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(PREVIEW_DISPLAY).bottom.value).isWithin(TOLERANCE).of(360f)
        assertThat(boundsOf(START_SIDE_RAIL_TAG).top.value).isAtLeast(32f - TOLERANCE)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).top.value).isAtLeast(32f - TOLERANCE)

        assertNoClippingAndNoOverlapInWindow(
            contextLabel = "COMPACT_SHORT 360x360 with 32dp visible statusBar",
            windowWidth = 360f,
            windowHeight = 360f,
            minTop = 32f
        )

        dispatchStatusBarInsets(
            view = hostView,
            statusBarHeightPx = MULTI_WINDOW_STATUS_BAR_PX,
            visible = false
        )
        composeTestRule.waitForIdle()
        assertThat(boundsOf(START_SIDE_RAIL_TAG).top.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).top.value).isWithin(TOLERANCE).of(0f)
    }

    @Test
    fun compactShortLayout_quickSettingsOverlayCard_displaysInHierarchyWithinCondensedBounds() {
        val state = CaptureScaffoldState(isInitiallyExpanded = true)
        composeTestRule.setContent {
            MaterialTheme {
                CompactShortLayout(
                    slots = PlaceholderCaptureSlots.sideRail(tagQuickSettingsOverlay = true),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    state = state
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)
            .assertIsDisplayed()
            .assert(!isPopup())
            .assert(!isDialog())
            .assert(!hasAnyAncestor(isPopup()))
            .assert(!hasAnyAncestor(isDialog()))
        assertThat(
            composeTestRule.onAllNodesWithTag(QUICK_SETTINGS_BOTTOM_SHEET).fetchSemanticsNodes()
        ).hasSize(1)

        val cardBounds = boundsOf(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)
        assertThat(cardBounds.left.value).isAtLeast(8f - TOLERANCE)
        assertThat(cardBounds.right.value).isAtMost(352f + TOLERANCE)
        assertThat(cardBounds.right.value - cardBounds.left.value).isAtMost(320f + TOLERANCE)
        assertThat(cardBounds.top.value).isAtLeast(8f - TOLERANCE)
        assertThat(cardBounds.bottom.value).isAtMost(352f + TOLERANCE)

        // Tapping inside the floating card must not dismiss Quick Settings; tapping the exposed
        // top-end scrim region outside the card dismisses it.
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG).performClick()
        composeTestRule.waitForIdle()
        assertThat(state.isQuickSettingsOpen).isTrue()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).performTouchInput {
            click(Offset(right - 10f, top + 10f))
        }
        composeTestRule.waitForIdle()
        assertThat(state.isQuickSettingsOpen).isFalse()
    }

    private fun assertNoClippingAndNoOverlapInWindow(
        contextLabel: String,
        windowWidth: Float,
        windowHeight: Float,
        minTop: Float
    ) {
        val allTags = START_RAIL_TAGS + END_RAIL_TAGS + HUD_TAGS
        allTags.forEach { tag ->
            val clipped = composeTestRule.onNodeWithTag(tag).getBoundsInRoot()
            val unclipped = composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

            assertWithMessage("$contextLabel: $tag must have positive width")
                .that(unclipped.width)
                .isGreaterThan(0.dp)
            assertWithMessage("$contextLabel: $tag must have positive height")
                .that(unclipped.height)
                .isGreaterThan(0.dp)
            assertWithMessage("$contextLabel: $tag left (${unclipped.left}) must be >= 0dp")
                .that(unclipped.left.value)
                .isAtLeast(0f - TOLERANCE)
            assertWithMessage(
                "$contextLabel: $tag right (${unclipped.right}) must be <= ${windowWidth}dp"
            )
                .that(unclipped.right.value)
                .isAtMost(windowWidth + TOLERANCE)
            assertWithMessage("$contextLabel: $tag top (${unclipped.top}) must be >= ${minTop}dp")
                .that(unclipped.top.value)
                .isAtLeast(minTop - TOLERANCE)
            assertWithMessage(
                "$contextLabel: $tag bottom (${unclipped.bottom}) must be <= ${windowHeight}dp"
            )
                .that(unclipped.bottom.value)
                .isAtMost(windowHeight + TOLERANCE)
            assertWithMessage("$contextLabel: $tag must not be clipped horizontally")
                .that(clipped.width.value)
                .isWithin(TOLERANCE)
                .of(unclipped.width.value)
            assertWithMessage("$contextLabel: $tag must not be clipped vertically")
                .that(clipped.height.value)
                .isWithin(TOLERANCE)
                .of(unclipped.height.value)
        }

        assertStrictVerticalOrder("$contextLabel StartSideRail", START_RAIL_TAGS)
        assertStrictVerticalOrder("$contextLabel EndSideRail", END_RAIL_TAGS)
    }

    private fun assertStrictVerticalOrder(railLabel: String, orderedTags: List<String>) {
        val rects = orderedTags.map { boundsOf(it) }
        for (i in 0 until rects.lastIndex) {
            assertWithMessage(
                "$railLabel: ${orderedTags[i]} bottom (${rects[i].bottom}) must not overlap " +
                    "${orderedTags[i + 1]} top (${rects[i + 1].top})"
            )
                .that(rects[i].bottom.value)
                .isAtMost(rects[i + 1].top.value + TOLERANCE)
        }
    }

    private fun dispatchStatusBarInsets(view: View, statusBarHeightPx: Int, visible: Boolean) {
        val barInsets = Insets.of(0, statusBarHeightPx, 0, 0)
        val windowInsets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.statusBars(),
                if (visible) barInsets else Insets.NONE
            )
            .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars(), barInsets)
            .setVisible(WindowInsetsCompat.Type.statusBars(), visible)
            .build()

        composeTestRule.runOnUiThread {
            ViewCompat.dispatchApplyWindowInsets(view, windowInsets)
        }
    }

    private companion object {
        val START_RAIL_TAGS = listOf(
            VIDEO_QUALITY_TAG,
            ZOOM_BUTTON_ROW_TAG,
            QUICK_SETTINGS_DROP_DOWN
        )
        val END_RAIL_TAGS = listOf(
            FLIP_CAMERA_BUTTON,
            CAPTURE_BUTTON,
            IMAGE_WELL_TAG,
            CAPTURE_MODE_TOGGLE_BUTTON
        )
        val HUD_TAGS = listOf(
            ELAPSED_TIME_TAG,
            CAPTURE_MODE_CAROUSEL
        )
        const val MULTI_WINDOW_STATUS_BAR_PX = 32
        const val TOLERANCE = 0.5f
    }
}
