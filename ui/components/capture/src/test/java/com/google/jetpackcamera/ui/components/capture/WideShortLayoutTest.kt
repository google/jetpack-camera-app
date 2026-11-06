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
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
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
 * Unit tests for [WideShortLayout] across `WIDE_SHORT` (`800x400dp`), `COMPACT_SHORT`
 * (`360x360dp`), RTL policies, and visible multi-window `statusBars` insets, asserting zero
 * clipping and zero vertical overlap in both rails.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w800dp-h400dp")
class WideShortLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun boundsOf(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun wideShortLayout_at800x400_fitsAllControlsWithoutClippingOrVerticalOverlap() {
        composeTestRule.setContent {
            MaterialTheme {
                WideShortLayout(
                    slots = PlaceholderCaptureSlots.sideRail(
                        isCompactHeight = false,
                        includeImageWell = true,
                        includePauseSlot = true,
                        includeElapsedTime = true
                    ),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    isRecording = true
                )
            }
        }
        composeTestRule.waitForIdle()

        val viewfinderBounds = boundsOf(PREVIEW_DISPLAY)
        assertThat(viewfinderBounds.left.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.top.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.right.value).isWithin(TOLERANCE).of(800f)
        assertThat(viewfinderBounds.bottom.value).isWithin(TOLERANCE).of(400f)

        assertNoClippingAndNoOverlapInWindow(
            contextLabel = "WIDE_SHORT 800x400",
            windowWidth = 800f,
            windowHeight = 400f,
            minTop = 0f
        )
    }

    @Config(qualifiers = "w360dp-h360dp")
    @Test
    fun wideShortLayout_at360x360_fitsAllControlsWithoutClippingOrVerticalOverlap() {
        composeTestRule.setContent {
            MaterialTheme {
                WideShortLayout(
                    slots = PlaceholderCaptureSlots.sideRail(
                        isCompactHeight = false,
                        includeImageWell = true,
                        includePauseSlot = true,
                        includeElapsedTime = true
                    ),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    isRecording = true
                )
            }
        }
        composeTestRule.waitForIdle()

        val viewfinderBounds = boundsOf(PREVIEW_DISPLAY)
        assertThat(viewfinderBounds.top.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.bottom.value).isWithin(TOLERANCE).of(360f)

        assertNoClippingAndNoOverlapInWindow(
            contextLabel = "COMPACT_SHORT 360x360",
            windowWidth = 360f,
            windowHeight = 360f,
            minTop = 0f
        )

        // StartSideRail, center HUD, and EndSideRail must not horizontally collide even at 360dp.
        val startRail = boundsOf(START_SIDE_RAIL_TAG)
        val endRail = boundsOf(END_SIDE_RAIL_TAG)
        val carousel = boundsOf(CAPTURE_MODE_CAROUSEL)
        assertThat(startRail.right.value).isAtMost(carousel.left.value + TOLERANCE)
        assertThat(carousel.right.value).isAtMost(endRail.left.value + TOLERANCE)
    }

    @Test
    fun wideShortLayout_rtlDefaultAndMirroredPolicies_preserveVerticalOrderAndChildDirection() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var rtlPolicy by mutableStateOf(CaptureRtlPolicy(mirrorSideRails = false))
        val observedDirections = mutableMapOf<String, LayoutDirection>()

        val baseSlots = PlaceholderCaptureSlots.sideRail(
            isCompactHeight = true,
            includeImageWell = true,
            includePauseSlot = true,
            includeElapsedTime = true
        )
        val instrumentedSlots = baseSlots.copy(
            indicatorColumn = { mod ->
                observedDirections["indicatorColumn"] = LocalLayoutDirection.current
                baseSlots.indicatorColumn(this, mod)
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
                    WideShortLayout(
                        slots = instrumentedSlots,
                        viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                        isRecording = true
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val ltrStartTops = START_RAIL_TAGS.map { boundsOf(it).top.value }
        val ltrEndTops = END_RAIL_TAGS.map { boundsOf(it).top.value }

        // Default RTL policy: rails stay on physical left/right while child slots observe RTL.
        observedDirections.clear()
        layoutDirection = LayoutDirection.Rtl
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = false)
        composeTestRule.waitForIdle()

        assertThat(boundsOf(START_SIDE_RAIL_TAG).left.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).right.value).isWithin(TOLERANCE).of(800f)
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

        // Mirrored RTL policy: rails swap horizontally while vertical bounds stay identical.
        observedDirections.clear()
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = true)
        composeTestRule.waitForIdle()

        assertThat(boundsOf(START_SIDE_RAIL_TAG).right.value).isWithin(TOLERANCE).of(800f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).left.value).isWithin(TOLERANCE).of(0f)
        assertNoClippingAndNoOverlapInWindow(
            contextLabel = "WIDE_SHORT 800x400 RTL Mirrored",
            windowWidth = 800f,
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

    @Config(qualifiers = "w360dp-h360dp")
    @Test
    fun wideShortLayout_withMultiWindowVisibleStatusBars_clearsBarAndFits360x360WithoutOverlap() {
        lateinit var hostView: View
        composeTestRule.setContent {
            hostView = LocalView.current
            MaterialTheme {
                WideShortLayout(
                    slots = PlaceholderCaptureSlots.sideRail(
                        isCompactHeight = false,
                        includeImageWell = true,
                        includePauseSlot = true,
                        includeElapsedTime = true
                    ),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    isRecording = true
                )
            }
        }

        dispatchStatusBarInsets(
            view = hostView,
            statusBarHeightPx = MULTI_WINDOW_STATUS_BAR_PX,
            visible = true
        )
        composeTestRule.waitForIdle()

        // Viewfinder stays flush [0..360] while rails and all child slots fit in [32..360] with
        // zero clipping and zero vertical overlap!
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

        // Hiding the status bar collapses rail top padding back to 0dp.
        dispatchStatusBarInsets(
            view = hostView,
            statusBarHeightPx = MULTI_WINDOW_STATUS_BAR_PX,
            visible = false
        )
        composeTestRule.waitForIdle()
        assertThat(boundsOf(START_SIDE_RAIL_TAG).top.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).top.value).isWithin(TOLERANCE).of(0f)
    }

    @Config(qualifiers = "w360dp-h360dp")
    @Test
    fun wideShortLayout_quickSettingsFloatingCard_displaysInHierarchyWithinCondensedBounds() {
        var isQuickSettingsOpen by mutableStateOf(true)
        composeTestRule.setContent {
            MaterialTheme {
                WideShortLayout(
                    slots = PlaceholderCaptureSlots.default(),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    isQuickSettingsOpen = isQuickSettingsOpen,
                    onDismissQuickSettings = { isQuickSettingsOpen = false }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)
            .assertIsDisplayed()
            .assert(!isPopup())
            .assert(!isDialog())
            .assert(!hasAnyAncestor(isPopup()))
            .assert(!hasAnyAncestor(isDialog()))

        val cardBounds = boundsOf(QUICK_SETTINGS_BOTTOM_SHEET)
        assertThat(cardBounds.left.value).isAtLeast(0f - TOLERANCE)
        assertThat(cardBounds.right.value).isAtMost(360f + TOLERANCE)
        assertThat(cardBounds.top.value).isAtLeast(0f - TOLERANCE)
        assertThat(cardBounds.bottom.value).isAtMost(360f + TOLERANCE)

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).performClick()
        composeTestRule.waitForIdle()
        assertThat(isQuickSettingsOpen).isFalse()
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
            AUDIO_INPUT_TOGGLE,
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
