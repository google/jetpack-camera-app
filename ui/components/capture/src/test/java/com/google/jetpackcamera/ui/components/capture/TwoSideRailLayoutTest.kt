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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isPopup
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolution
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [TwoSideRailLayout] and [CompactPortraitLayout] verifying full-bleed viewfinder
 * geometry, [CaptureRtlPolicy] hand-placement vs. mirrored modes, live multi-window `statusBars`
 * clearance, and in-hierarchy Quick Settings card behavior.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w852dp-h883dp")
class TwoSideRailLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun boundsOf(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    @Test
    fun twoSideRailLayout_viewfinderIsFlushTopToBottom_andSideRailsOverlayEdges() {
        var observedSolverSolution: CameraLayoutSolution? = null

        composeTestRule.setContent {
            MaterialTheme {
                TwoSideRailLayout(
                    slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                    viewfinder = { vfModifier ->
                        observedSolverSolution = LocalCameraLayoutSolution.current
                        Box(
                            modifier = vfModifier.testTag(PREVIEW_DISPLAY),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .aspectRatio(1f)
                                    .testTag(SQUARE_PREVIEW_FRAME_TAG)
                            )
                        }
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Rail mode must not provide a portrait CameraLayoutSolver solution.
        assertThat(observedSolverSolution).isNull()

        val viewfinderBounds = boundsOf(PREVIEW_DISPLAY)
        assertThat(viewfinderBounds.left.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.top.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.right.value).isWithin(TOLERANCE).of(852f)
        assertThat(viewfinderBounds.bottom.value).isWithin(TOLERANCE).of(883f)

        // A 1:1 preview frame centers naturally inside the full-bleed background container.
        val squareBounds = boundsOf(SQUARE_PREVIEW_FRAME_TAG)
        val squareCenterY = (squareBounds.top.value + squareBounds.bottom.value) / 2f
        assertThat(squareCenterY).isWithin(TOLERANCE).of(883f / 2f)

        // Start and End rails overlay the horizontal edges cleanly.
        composeTestRule.onNodeWithTag(START_SIDE_RAIL_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(END_SIDE_RAIL_TAG).assertIsDisplayed()
        val startRail = boundsOf(START_SIDE_RAIL_TAG)
        val endRail = boundsOf(END_SIDE_RAIL_TAG)
        assertThat(startRail.left.value).isWithin(TOLERANCE).of(0f)
        assertThat(endRail.right.value).isWithin(TOLERANCE).of(852f)
        assertThat(startRail.right.value).isLessThan(endRail.left.value)

        // Elapsed time and capture mode carousel sit at the top/bottom center HUD rather than in
        // EndSideRail.
        val elapsedBounds = boundsOf(ELAPSED_TIME_TAG)
        val carouselBounds = boundsOf(CAPTURE_MODE_CAROUSEL)
        assertThat((elapsedBounds.left.value + elapsedBounds.right.value) / 2f)
            .isWithin(TOLERANCE).of(852f / 2f)
        assertThat((carouselBounds.left.value + carouselBounds.right.value) / 2f)
            .isWithin(TOLERANCE).of(852f / 2f)
        assertThat(elapsedBounds.right.value).isLessThan(endRail.left.value)
    }

    @Config(qualifiers = "w1280dp-h800dp")
    @Test
    fun twoSideRailLayout_rtlPolicies_scopeRailPlacementAndRestoreLocaleDirectionInSlots() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var rtlPolicy by mutableStateOf(CaptureRtlPolicy(mirrorSideRails = false))

        val observedDirections = mutableMapOf<String, LayoutDirection>()
        val baseSlots = PlaceholderCaptureSlots.sideRail(
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
            quickSettingsButton = { mod ->
                observedDirections["quickSettingsButton"] = LocalLayoutDirection.current
                baseSlots.quickSettingsButton(this, mod)
            },
            captureButton = { mod ->
                observedDirections["captureButton"] = LocalLayoutDirection.current
                baseSlots.captureButton(this, mod)
            },
            flipCameraButton = { mod ->
                observedDirections["flipCameraButton"] = LocalLayoutDirection.current
                baseSlots.flipCameraButton(this, mod)
            },
            imageWell = { mod ->
                observedDirections["imageWell"] = LocalLayoutDirection.current
                baseSlots.imageWell(this, mod)
            },
            captureModeToggle = { mod ->
                observedDirections["captureModeToggle"] = LocalLayoutDirection.current
                baseSlots.captureModeToggle(this, mod)
            },
            captureModeCarousel = { mod ->
                observedDirections["captureModeCarousel"] = LocalLayoutDirection.current
                baseSlots.captureModeCarousel(this, mod)
            },
            elapsedTimeDisplay = { mod ->
                observedDirections["elapsedTimeDisplay"] = LocalLayoutDirection.current
                baseSlots.elapsedTimeDisplay(this, mod)
            },
            snackBar = { mod, host ->
                observedDirections["snackBar"] = LocalLayoutDirection.current
                baseSlots.snackBar(mod, host)
            },
            errorDialog = { mod ->
                observedDirections["errorDialog"] = LocalLayoutDirection.current
                baseSlots.errorDialog(mod)
            }
        )

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalCaptureRtlPolicy provides rtlPolicy
                ) {
                    TwoSideRailLayout(
                        slots = instrumentedSlots,
                        viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                        isRecording = true
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val startOrderedTags = listOf(
            VIDEO_QUALITY_TAG,
            ZOOM_BUTTON_ROW_TAG,
            QUICK_SETTINGS_DROP_DOWN
        )
        val endOrderedTags = listOf(
            FLIP_CAMERA_BUTTON,
            CAPTURE_BUTTON,
            IMAGE_WELL_TAG,
            AUDIO_INPUT_TOGGLE,
            CAPTURE_MODE_TOGGLE_BUTTON
        )

        val ltrStartTops = startOrderedTags.map { boundsOf(it).top.value }
        val ltrEndTops = endOrderedTags.map { boundsOf(it).top.value }
        assertThat(boundsOf(START_SIDE_RAIL_TAG).left.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).right.value).isWithin(TOLERANCE).of(1280f)

        // 1. RTL with default CaptureRtlPolicy(false, false): rails preserve physical grip sides,
        // while every inner slot receives LayoutDirection.Rtl!
        observedDirections.clear()
        layoutDirection = LayoutDirection.Rtl
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = false, mirrorCompactBottomBar = false)
        composeTestRule.waitForIdle()

        assertThat(boundsOf(START_SIDE_RAIL_TAG).left.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).right.value).isWithin(TOLERANCE).of(1280f)
        observedDirections.forEach { (slotName, dir) ->
            assertWithMessage("Slot $slotName must receive LayoutDirection.Rtl")
                .that(dir)
                .isEqualTo(LayoutDirection.Rtl)
        }
        startOrderedTags.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrStartTops[i])
        }
        endOrderedTags.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrEndTops[i])
        }

        // 2. RTL with mirrored CaptureRtlPolicy(true, true): StartSideRail and EndSideRail swap
        // horizontal sides while vertical Top -> Bottom order remains invariant!
        observedDirections.clear()
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = true, mirrorCompactBottomBar = true)
        composeTestRule.waitForIdle()

        assertThat(boundsOf(START_SIDE_RAIL_TAG).right.value).isWithin(TOLERANCE).of(1280f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).left.value).isWithin(TOLERANCE).of(0f)
        observedDirections.forEach { (slotName, dir) ->
            assertWithMessage("Slot $slotName must receive LayoutDirection.Rtl when mirrored")
                .that(dir)
                .isEqualTo(LayoutDirection.Rtl)
        }
        startOrderedTags.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrStartTops[i])
        }
        endOrderedTags.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrEndTops[i])
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = "w412dp-h915dp")
    @Test
    fun compactPortraitLayout_rtlPolicies_scopeBottomBarAndRestoreLocaleDirectionInSlots() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var rtlPolicy by mutableStateOf(CaptureRtlPolicy(mirrorCompactBottomBar = false))
        val observedDirections = mutableMapOf<String, LayoutDirection>()

        val baseSlots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true)
        val instrumentedSlots = baseSlots.copy(
            topBar = { mod ->
                observedDirections["topBar"] = LocalLayoutDirection.current
                baseSlots.topBar(this, mod)
            },
            zoomControls = { mod ->
                observedDirections["zoomControls"] = LocalLayoutDirection.current
                baseSlots.zoomControls(this, mod)
            },
            imageWell = { mod ->
                observedDirections["imageWell"] = LocalLayoutDirection.current
                baseSlots.imageWell(this, mod)
            },
            captureButton = { mod ->
                observedDirections["captureButton"] = LocalLayoutDirection.current
                baseSlots.captureButton(this, mod)
            },
            flipCameraButton = { mod ->
                observedDirections["flipCameraButton"] = LocalLayoutDirection.current
                baseSlots.flipCameraButton(this, mod)
            },
            captureModeCarousel = { mod ->
                observedDirections["captureModeCarousel"] = LocalLayoutDirection.current
                baseSlots.captureModeCarousel(this, mod)
            },
            quickSettingsButton = { mod ->
                observedDirections["quickSettingsButton"] = LocalLayoutDirection.current
                baseSlots.quickSettingsButton(this, mod)
            },
            captureModeToggle = { mod ->
                observedDirections["captureModeToggle"] = LocalLayoutDirection.current
                baseSlots.captureModeToggle(this, mod)
            }
        )

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides layoutDirection,
                    LocalCaptureRtlPolicy provides rtlPolicy
                ) {
                    CompactPortraitLayout(
                        slots = instrumentedSlots,
                        viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(COMPACT_CONTROLS_TAG).assertIsDisplayed()
        val verticalOrderTags = listOf(
            VIDEO_QUALITY_TAG,
            ELAPSED_TIME_TAG,
            ZOOM_BUTTON_ROW_TAG,
            CAPTURE_BUTTON,
            CAPTURE_MODE_CAROUSEL,
            CAPTURE_MODE_TOGGLE_BUTTON
        )
        val ltrTops = verticalOrderTags.map { boundsOf(it).top.value }

        // Default RTL policy: bottom rows stay unmirrored (ImageWell left, Flip right, Quick
        // Settings left) while every child slot receives LayoutDirection.Rtl!
        observedDirections.clear()
        layoutDirection = LayoutDirection.Rtl
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = false, mirrorCompactBottomBar = false)
        composeTestRule.waitForIdle()

        assertThat(boundsOf(IMAGE_WELL_TAG).right.value)
            .isLessThan(boundsOf(CAPTURE_BUTTON).left.value)
        assertThat(boundsOf(FLIP_CAMERA_BUTTON).left.value)
            .isGreaterThan(boundsOf(CAPTURE_BUTTON).right.value)
        val defaultQsCenter =
            (
                boundsOf(QUICK_SETTINGS_DROP_DOWN).left.value +
                    boundsOf(QUICK_SETTINGS_DROP_DOWN).right.value
                ) / 2f
        assertThat(defaultQsCenter).isLessThan(412f / 2f)

        observedDirections.forEach { (slotName, dir) ->
            assertWithMessage("Compact slot $slotName must receive LayoutDirection.Rtl")
                .that(dir)
                .isEqualTo(LayoutDirection.Rtl)
        }
        verticalOrderTags.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrTops[i])
        }

        // Mirrored RTL policy: CaptureRow and BottomToolbar reflect horizontally while vertical
        // Top -> Bottom order remains invariant!
        observedDirections.clear()
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = true, mirrorCompactBottomBar = true)
        composeTestRule.waitForIdle()

        assertThat(boundsOf(FLIP_CAMERA_BUTTON).right.value)
            .isLessThan(boundsOf(CAPTURE_BUTTON).left.value)
        assertThat(boundsOf(IMAGE_WELL_TAG).left.value)
            .isGreaterThan(boundsOf(CAPTURE_BUTTON).right.value)
        val mirroredQsCenter =
            (
                boundsOf(QUICK_SETTINGS_DROP_DOWN).left.value +
                    boundsOf(QUICK_SETTINGS_DROP_DOWN).right.value
                ) / 2f
        assertThat(mirroredQsCenter).isGreaterThan(412f / 2f)

        observedDirections.forEach { (slotName, dir) ->
            assertWithMessage("Mirrored compact slot $slotName must receive LayoutDirection.Rtl")
                .that(dir)
                .isEqualTo(LayoutDirection.Rtl)
        }
        verticalOrderTags.forEachIndexed { i, tag ->
            assertThat(boundsOf(tag).top.value).isWithin(TOLERANCE).of(ltrTops[i])
        }
    }

    @Test
    fun twoSideRailLayout_statusBarsInsets_clearsVisibleBarAndZeroPadsWhenHidden() {
        lateinit var hostView: View
        composeTestRule.setContent {
            hostView = LocalView.current
            MaterialTheme {
                TwoSideRailLayout(
                    slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    screenFlashOverlay = { mod ->
                        Box(modifier = mod.fillMaxSize().testTag(SCREEN_FLASH_OVERLAY))
                    }
                )
            }
        }

        // 1. Multi-window visible status bar (32px == 32dp at Robolectric default mdpi):
        // rails and indicators clear >= 32dp while viewfinder and screen flash stay full-bleed.
        dispatchStatusBarInsets(
            view = hostView,
            statusBarHeightPx = MULTI_WINDOW_STATUS_BAR_PX,
            visible = true
        )
        composeTestRule.waitForIdle()

        assertThat(boundsOf(PREVIEW_DISPLAY).top.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(PREVIEW_DISPLAY).bottom.value).isWithin(TOLERANCE).of(883f)
        assertThat(boundsOf(START_SIDE_RAIL_TAG).top.value).isAtLeast(32f - TOLERANCE)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).top.value).isAtLeast(32f - TOLERANCE)
        assertThat(boundsOf(VIDEO_QUALITY_TAG).top.value).isAtLeast(32f - TOLERANCE)
        assertThat(boundsOf(SCREEN_FLASH_OVERLAY).top.value).isWithin(TOLERANCE).of(0f)

        // 2. Immersive hidden status bar (0px live, 32px ignoring visibility):
        // live WindowInsets.systemBars must collapse rail top offset to 0dp!
        dispatchStatusBarInsets(
            hostView,
            statusBarHeightPx = MULTI_WINDOW_STATUS_BAR_PX,
            visible = false
        )
        composeTestRule.waitForIdle()

        assertThat(boundsOf(START_SIDE_RAIL_TAG).top.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).top.value).isWithin(TOLERANCE).of(0f)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = "w412dp-h915dp")
    @Test
    fun compactPortraitLayout_visibleMultiWindowStatusBar_clearsIndicatorsBelowStatusBar() {
        lateinit var hostView: View
        composeTestRule.setContent {
            hostView = LocalView.current
            MaterialTheme {
                CompactPortraitLayout(
                    slots = PlaceholderCaptureSlots.sideRail(),
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

        assertThat(boundsOf(VIDEO_QUALITY_TAG).top.value).isAtLeast(32f - TOLERANCE)
    }

    @Config(qualifiers = "w1280dp-h800dp")
    @Test
    fun twoSideRailLayout_quickSettingsFloatingCard_isInHierarchyAndDismissesOnScrimClick() {
        var isQuickSettingsOpen by mutableStateOf(false)
        var overlayDirection: LayoutDirection? = null
        var rtlPolicy by mutableStateOf(CaptureRtlPolicy(mirrorSideRails = false))

        val baseSlots = PlaceholderCaptureSlots.default()
        val instrumentedSlots = baseSlots.copy(
            quickSettingsOverlay = { mod ->
                overlayDirection = LocalLayoutDirection.current
                baseSlots.quickSettingsOverlay(this, mod)
            }
        )

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalCaptureRtlPolicy provides rtlPolicy
                ) {
                    TwoSideRailLayout(
                        slots = instrumentedSlots,
                        viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                        isQuickSettingsOpen = isQuickSettingsOpen,
                        onDismissQuickSettings = { isQuickSettingsOpen = false }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsDisplayed()

        // Open Quick Settings floating card.
        isQuickSettingsOpen = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET)
            .assertIsDisplayed()
            .assert(!isPopup())
            .assert(!isDialog())
            .assert(!hasAnyAncestor(isPopup()))
            .assert(!hasAnyAncestor(isDialog()))
        assertThat(overlayDirection).isEqualTo(LayoutDirection.Rtl)

        // Default RTL policy anchors the floating card to BottomLeft matching StartSideRail.
        val unmirroredCardBounds = boundsOf(QUICK_SETTINGS_BOTTOM_SHEET)
        assertThat(unmirroredCardBounds.right.value).isLessThan(1280f / 2f)

        // Mirrored RTL policy anchors the floating card to BottomStart (physical right in RTL).
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = true)
        composeTestRule.waitForIdle()
        val mirroredCardBounds = boundsOf(QUICK_SETTINGS_BOTTOM_SHEET)
        assertThat(mirroredCardBounds.left.value).isGreaterThan(1280f / 2f)

        // Background camera chrome semantics are suppressed while modal card is open.
        assertThat(
            composeTestRule.onAllNodesWithTag(CAPTURE_BUTTON).fetchSemanticsNodes()
        ).isEmpty()

        // Clicking the scrim dismisses Quick Settings and restores background semantics.
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM)
            .assertIsDisplayed()
            .performClick()
        composeTestRule.waitForIdle()

        assertThat(isQuickSettingsOpen).isFalse()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertIsDisplayed()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = "w360dp-h500dp")
    @Test
    fun compactPortraitLayout_shortSplitScreenFallback_appliesRtlPoliciesAndClearsStatusBar() {
        var rtlPolicy by mutableStateOf(CaptureRtlPolicy(mirrorCompactBottomBar = false))
        var flipDirection: LayoutDirection? = null
        lateinit var hostView: View

        val baseSlots = PlaceholderCaptureSlots.sideRail()
        val instrumentedSlots = baseSlots.copy(
            flipCameraButton = { mod ->
                flipDirection = LocalLayoutDirection.current
                baseSlots.flipCameraButton(this, mod)
            }
        )

        composeTestRule.setContent {
            hostView = LocalView.current
            MaterialTheme {
                CompositionLocalProvider(
                    LocalLayoutDirection provides LayoutDirection.Rtl,
                    LocalCaptureRtlPolicy provides rtlPolicy
                ) {
                    CompactPortraitLayout(
                        slots = instrumentedSlots,
                        viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        // Default policy in VerticalMaterialControls fallback keeps ImageWell left and Flip right.
        assertThat(boundsOf(IMAGE_WELL_TAG).right.value)
            .isLessThan(boundsOf(CAPTURE_BUTTON).left.value)
        assertThat(boundsOf(FLIP_CAMERA_BUTTON).left.value)
            .isGreaterThan(boundsOf(CAPTURE_BUTTON).right.value)
        assertThat(flipDirection).isEqualTo(LayoutDirection.Rtl)

        // Mirrored policy reflects horizontal placement in VerticalMaterialControls fallback.
        rtlPolicy = CaptureRtlPolicy(mirrorCompactBottomBar = true)
        composeTestRule.waitForIdle()
        assertThat(boundsOf(FLIP_CAMERA_BUTTON).right.value)
            .isLessThan(boundsOf(CAPTURE_BUTTON).left.value)
        assertThat(boundsOf(IMAGE_WELL_TAG).left.value)
            .isGreaterThan(boundsOf(CAPTURE_BUTTON).right.value)

        // Visible multi-window status bar clears indicatorRow in non-handheld fallback too.
        dispatchStatusBarInsets(
            view = hostView,
            statusBarHeightPx = MULTI_WINDOW_STATUS_BAR_PX,
            visible = true
        )
        composeTestRule.waitForIdle()
        assertThat(boundsOf(VIDEO_QUALITY_TAG).top.value).isAtLeast(32f - TOLERANCE)
    }

    @Test
    fun twoSideRailLayout_tagQuickSettingsCardFalse_avoidsDuplicateSheetTagsAndErrorSuppressesBg() {
        var isQuickSettingsOpen by mutableStateOf(true)
        var isErrorDialogShowing by mutableStateOf(false)

        val slotsWithInnerSheetTag = PlaceholderCaptureSlots.sideRail(
            tagQuickSettingsOverlay = true,
            includeErrorDialog = true
        )

        composeTestRule.setContent {
            MaterialTheme {
                TwoSideRailLayout(
                    slots = slotsWithInnerSheetTag,
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    isQuickSettingsOpen = isQuickSettingsOpen,
                    isErrorDialogShowing = isErrorDialogShowing,
                    tagQuickSettingsCard = false,
                    onDismissQuickSettings = { isQuickSettingsOpen = false }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Exactly one QUICK_SETTINGS_BOTTOM_SHEET node exists (owned by inner overlay).
        assertThat(
            composeTestRule.onAllNodesWithTag(QUICK_SETTINGS_BOTTOM_SHEET).fetchSemanticsNodes()
        ).hasSize(1)

        // Close Quick Settings and show error dialog: background controls are suppressed while
        // CAMERA_ERROR_DIALOG_TAG is displayed.
        isQuickSettingsOpen = false
        isErrorDialogShowing = true
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TAG).assertIsDisplayed()
        assertThat(
            composeTestRule.onAllNodesWithTag(CAPTURE_BUTTON).fetchSemanticsNodes()
        ).isEmpty()
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
        const val SQUARE_PREVIEW_FRAME_TAG = "square_preview_frame"
        const val MULTI_WINDOW_STATUS_BAR_PX = 32
        const val TOLERANCE = 0.5f
    }
}
