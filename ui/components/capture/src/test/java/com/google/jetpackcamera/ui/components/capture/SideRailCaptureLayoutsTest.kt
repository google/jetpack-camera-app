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
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.compose.ui.unit.dp
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
 * Unit tests for [FoldableLayout], [TabletPortraitLayout], [TabletLandscapeLayout], and
 * [CompactPortraitLayout] verifying full-bleed viewfinder geometry, portrait tablet lower thumb
 * zone (`0.70f`), rail-cleared [QuickSettingsOverlayCard] non-overlap, [CaptureRtlPolicy] modes,
 * live multi-window `statusBars` clearance, and [LocalActiveCaptureLayout] reporting.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w852dp-h883dp")
class SideRailCaptureLayoutsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun boundsOf(tag: String): DpRect =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private val hiddenFromA11yAncestor = hasAnyAncestor(
        SemanticsMatcher.keyIsDefined(SemanticsProperties.HideFromAccessibility)
    )

    @Test
    fun foldableLayout_viewfinderIsFlushTopToBottom_providesActiveLayoutName_andOverlaysRails() {
        var observedSolverSolution: CameraLayoutSolution? = null
        var observedActiveLayout: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                FoldableLayout(
                    slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                    viewfinder = { vfModifier ->
                        observedSolverSolution = LocalCameraLayoutSolution.current
                        observedActiveLayout = LocalActiveCaptureLayout.current
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

        assertThat(observedSolverSolution).isNull()
        assertThat(observedActiveLayout).isEqualTo("FoldableLayout")

        val viewfinderBounds = boundsOf(PREVIEW_DISPLAY)
        assertThat(viewfinderBounds.left.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.top.value).isWithin(TOLERANCE).of(0f)
        assertThat(viewfinderBounds.right.value).isWithin(TOLERANCE).of(852f)
        assertThat(viewfinderBounds.bottom.value).isWithin(TOLERANCE).of(883f)

        val squareBounds = boundsOf(SQUARE_PREVIEW_FRAME_TAG)
        val squareCenterY = (squareBounds.top.value + squareBounds.bottom.value) / 2f
        assertThat(squareCenterY).isWithin(TOLERANCE).of(883f / 2f)

        composeTestRule.onNodeWithTag(START_SIDE_RAIL_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(END_SIDE_RAIL_TAG).assertIsDisplayed()
        val startRail = boundsOf(START_SIDE_RAIL_TAG)
        val endRail = boundsOf(END_SIDE_RAIL_TAG)
        assertThat(startRail.left.value).isWithin(TOLERANCE).of(0f)
        assertThat(endRail.right.value).isWithin(TOLERANCE).of(852f)
        assertThat(startRail.right.value).isLessThan(endRail.left.value)

        // Foldable centers zoom and shutter vertically around 50% (441.5dp).
        val zoomBounds = boundsOf(ZOOM_BUTTON_ROW_TAG)
        val zoomCenterY = (zoomBounds.top.value + zoomBounds.bottom.value) / 2f
        assertThat(zoomCenterY).isWithin(2f).of(883f * 0.50f)

        val elapsedBounds = boundsOf(ELAPSED_TIME_TAG)
        val carouselBounds = boundsOf(CAPTURE_MODE_CAROUSEL)
        assertThat((elapsedBounds.left.value + elapsedBounds.right.value) / 2f)
            .isWithin(TOLERANCE).of(852f / 2f)
        assertThat((carouselBounds.left.value + carouselBounds.right.value) / 2f)
            .isWithin(TOLERANCE).of(852f / 2f)
        assertThat(elapsedBounds.right.value).isLessThan(endRail.left.value)
    }

    @Config(qualifiers = "w800dp-h1280dp")
    @Test
    fun tabletPortraitLayout_shiftsZoomAndShutterIntoLowerThumbZoneWithoutCollision() {
        var observedActiveLayout: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                TabletPortraitLayout(
                    slots = PlaceholderCaptureSlots.sideRail(
                        includeImageWell = true,
                        includeElapsedTime = true
                    ),
                    viewfinder = { vfModifier ->
                        observedActiveLayout = LocalActiveCaptureLayout.current
                        PlaceholderViewfinder(modifier = vfModifier)
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        assertThat(observedActiveLayout).isEqualTo("TabletPortraitLayout")

        val zoomBounds = boundsOf(ZOOM_BUTTON_ROW_TAG)
        val qsButtonBounds = boundsOf(QUICK_SETTINGS_DROP_DOWN)
        val flipBounds = boundsOf(FLIP_CAMERA_BUTTON)
        val imageWellBounds = boundsOf(IMAGE_WELL_TAG)
        val modeToggleBounds = boundsOf(CAPTURE_MODE_TOGGLE_BUTTON)

        val zoomCenterY = (zoomBounds.top.value + zoomBounds.bottom.value) / 2f
        val shutterClusterCenterY = (flipBounds.top.value + imageWellBounds.bottom.value) / 2f

        // Both center clusters shift down to the 0.70f thumb zone (~896dp), well below 50% (640dp).
        assertThat(zoomCenterY).isWithin(2f).of(1280f * 0.70f)
        assertThat(shutterClusterCenterY).isGreaterThan(750f)

        // Zero vertical collision with bottom rail buttons (bottomPadding = 48.dp, gap >= 16.dp).
        assertThat(zoomBounds.bottom.value).isAtMost(qsButtonBounds.top.value - 16f + TOLERANCE)
        assertThat(imageWellBounds.bottom.value)
            .isAtMost(modeToggleBounds.top.value - 16f + TOLERANCE)
        assertThat(qsButtonBounds.bottom.value).isWithin(TOLERANCE).of(1280f - 48f)
        assertThat(modeToggleBounds.bottom.value).isWithin(TOLERANCE).of(1280f - 48f)
    }

    @Test
    fun foldableLayout_quickSettingsOverlayCard_clearsBothSideRailsAndHasSingleSheetTag() {
        val state = CaptureScaffoldState(isInitiallyExpanded = false)

        composeTestRule.setContent {
            MaterialTheme {
                FoldableLayout(
                    slots = PlaceholderCaptureSlots.sideRail(tagQuickSettingsOverlay = true),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    state = state
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()

        composeTestRule.runOnIdle { state.showQuickSettings() }
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

        val startRail = boundsOf(START_SIDE_RAIL_TAG)
        val endRail = boundsOf(END_SIDE_RAIL_TAG)
        val cardBounds = boundsOf(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)

        // Card clears both StartSideRail and EndSideRail by >= 12dp and respects 380dp max width.
        assertThat(cardBounds.left.value).isAtLeast(startRail.right.value + 12f - TOLERANCE)
        assertThat(cardBounds.right.value).isAtMost(endRail.left.value - 12f + TOLERANCE)
        assertThat(cardBounds.right.value - cardBounds.left.value).isAtMost(380f + TOLERANCE)
        assertThat(cardBounds.bottom.value).isWithin(TOLERANCE).of(883f - 24f)

        // Background camera chrome is pruned from accessibility while Quick Settings is open.
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assert(hiddenFromA11yAncestor)

        // Scrim tap dismisses CaptureScaffoldState.isQuickSettingsOpen.
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_SCRIM).performClick()
        composeTestRule.waitForIdle()
        assertThat(state.isQuickSettingsOpen).isFalse()
        composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)
            .assertDoesNotExist()
    }

    @Config(qualifiers = "w800dp-h1280dp")
    @Test
    fun tabletPortraitLayout_quickSettingsOverlayCard_clearsBothSideRailsAt800x1280() {
        val state = CaptureScaffoldState(isInitiallyExpanded = true)

        composeTestRule.setContent {
            MaterialTheme {
                TabletPortraitLayout(
                    slots = PlaceholderCaptureSlots.sideRail(tagQuickSettingsOverlay = true),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    state = state
                )
            }
        }
        composeTestRule.waitForIdle()

        val startRail = boundsOf(START_SIDE_RAIL_TAG)
        val endRail = boundsOf(END_SIDE_RAIL_TAG)
        val cardBounds = boundsOf(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)

        assertThat(cardBounds.left.value).isAtLeast(startRail.right.value + 12f - TOLERANCE)
        assertThat(cardBounds.right.value).isAtMost(endRail.left.value - 12f + TOLERANCE)
        assertThat(cardBounds.right.value - cardBounds.left.value).isAtMost(412f + TOLERANCE)
        assertThat(cardBounds.bottom.value).isWithin(TOLERANCE).of(1280f - 48f)
    }

    @Config(qualifiers = "w1280dp-h800dp")
    @Test
    fun tabletLandscapeLayout_rtlPolicies_scopeRailAndCardPlacementAndRestoreLocaleDirection() {
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var rtlPolicy by mutableStateOf(CaptureRtlPolicy(mirrorSideRails = false))
        val state = CaptureScaffoldState(isInitiallyExpanded = false)
        var observedActiveLayout: String? = null

        val observedDirections = mutableMapOf<String, LayoutDirection>()
        val baseSlots = PlaceholderCaptureSlots.sideRail(
            includeImageWell = true,
            includeElapsedTime = true,
            tagQuickSettingsOverlay = true
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
            quickSettingsOverlay = { mod ->
                observedDirections["quickSettingsOverlay"] = LocalLayoutDirection.current
                baseSlots.quickSettingsOverlay(this, mod)
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
                    TabletLandscapeLayout(
                        slots = instrumentedSlots,
                        viewfinder = { mod ->
                            observedActiveLayout = LocalActiveCaptureLayout.current
                            PlaceholderViewfinder(modifier = mod)
                        },
                        state = state
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(observedActiveLayout).isEqualTo("TabletLandscapeLayout")

        val startOrderedTags = listOf(
            VIDEO_QUALITY_TAG,
            ZOOM_BUTTON_ROW_TAG,
            QUICK_SETTINGS_DROP_DOWN
        )
        val endOrderedTags = listOf(
            FLIP_CAMERA_BUTTON,
            CAPTURE_BUTTON,
            IMAGE_WELL_TAG,
            CAPTURE_MODE_TOGGLE_BUTTON
        )

        val ltrStartTops = startOrderedTags.map { boundsOf(it).top.value }
        val ltrEndTops = endOrderedTags.map { boundsOf(it).top.value }
        assertThat(boundsOf(START_SIDE_RAIL_TAG).left.value).isWithin(TOLERANCE).of(0f)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).right.value).isWithin(TOLERANCE).of(1280f)

        // 1. RTL with default CaptureRtlPolicy(false, false): rails stay on physical left/right
        // while QuickSettingsOverlayCard clears StartSideRail on the left and restores RTL inside!
        observedDirections.clear()
        layoutDirection = LayoutDirection.Rtl
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = false, mirrorCompactBottomBar = false)
        state.showQuickSettings()
        composeTestRule.waitForIdle()

        val unmirroredStartRail = boundsOf(START_SIDE_RAIL_TAG)
        val unmirroredEndRail = boundsOf(END_SIDE_RAIL_TAG)
        val unmirroredCard = boundsOf(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)
        assertThat(unmirroredStartRail.left.value).isWithin(TOLERANCE).of(0f)
        assertThat(unmirroredEndRail.right.value).isWithin(TOLERANCE).of(1280f)
        assertThat(unmirroredCard.left.value)
            .isAtLeast(unmirroredStartRail.right.value + 12f - TOLERANCE)
        assertThat(unmirroredCard.right.value).isLessThan(1280f / 2f)

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

        // 2. RTL with mirrored CaptureRtlPolicy(true, true): StartSideRail swaps to physical right
        // and QuickSettingsOverlayCard anchors beside StartSideRail on the right without overlap!
        observedDirections.clear()
        rtlPolicy = CaptureRtlPolicy(mirrorSideRails = true, mirrorCompactBottomBar = true)
        composeTestRule.waitForIdle()

        val mirroredStartRail = boundsOf(START_SIDE_RAIL_TAG)
        val mirroredEndRail = boundsOf(END_SIDE_RAIL_TAG)
        val mirroredCard = boundsOf(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)
        assertThat(mirroredStartRail.right.value).isWithin(TOLERANCE).of(1280f)
        assertThat(mirroredEndRail.left.value).isWithin(TOLERANCE).of(0f)
        assertThat(mirroredCard.right.value)
            .isAtMost(mirroredStartRail.left.value - 12f + TOLERANCE)
        assertThat(mirroredCard.left.value)
            .isAtLeast(mirroredEndRail.right.value + 12f - TOLERANCE)
        assertThat(mirroredCard.left.value).isGreaterThan(1280f / 2f)

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
        var observedActiveLayout: String? = null
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
                        viewfinder = { mod ->
                            observedActiveLayout = LocalActiveCaptureLayout.current
                            PlaceholderViewfinder(modifier = mod)
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        assertThat(observedActiveLayout).isEqualTo("CompactPortraitLayout")
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
    fun foldableLayout_statusBarsInsets_clearsVisibleBarAndZeroPadsWhenHidden() {
        lateinit var hostView: View
        composeTestRule.setContent {
            hostView = LocalView.current
            MaterialTheme {
                FoldableLayout(
                    slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    screenFlashOverlay = { mod ->
                        Box(modifier = mod.fillMaxSize().testTag(SCREEN_FLASH_OVERLAY))
                    }
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
        assertThat(boundsOf(PREVIEW_DISPLAY).bottom.value).isWithin(TOLERANCE).of(883f)
        assertThat(boundsOf(START_SIDE_RAIL_TAG).top.value).isAtLeast(32f - TOLERANCE)
        assertThat(boundsOf(END_SIDE_RAIL_TAG).top.value).isAtLeast(32f - TOLERANCE)
        assertThat(boundsOf(VIDEO_QUALITY_TAG).top.value).isAtLeast(32f - TOLERANCE)
        assertThat(boundsOf(SCREEN_FLASH_OVERLAY).top.value).isWithin(TOLERANCE).of(0f)

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

        assertThat(boundsOf(IMAGE_WELL_TAG).right.value)
            .isLessThan(boundsOf(CAPTURE_BUTTON).left.value)
        assertThat(boundsOf(FLIP_CAMERA_BUTTON).left.value)
            .isGreaterThan(boundsOf(CAPTURE_BUTTON).right.value)
        assertThat(flipDirection).isEqualTo(LayoutDirection.Rtl)

        rtlPolicy = CaptureRtlPolicy(mirrorCompactBottomBar = true)
        composeTestRule.waitForIdle()
        assertThat(boundsOf(FLIP_CAMERA_BUTTON).right.value)
            .isLessThan(boundsOf(CAPTURE_BUTTON).left.value)
        assertThat(boundsOf(IMAGE_WELL_TAG).left.value)
            .isGreaterThan(boundsOf(CAPTURE_BUTTON).right.value)

        dispatchStatusBarInsets(
            view = hostView,
            statusBarHeightPx = MULTI_WINDOW_STATUS_BAR_PX,
            visible = true
        )
        composeTestRule.waitForIdle()
        assertThat(boundsOf(VIDEO_QUALITY_TAG).top.value).isAtLeast(32f - TOLERANCE)
    }

    @Test
    fun foldableLayout_controlsVisibleAndErrorDialog_hideControlsAndPruneBackgroundA11y() {
        var controlsVisible by mutableStateOf(true)
        var showErrorDialog by mutableStateOf(false)
        val state = CaptureScaffoldState(isInitiallyExpanded = true)

        val baseSlots = PlaceholderCaptureSlots.sideRail(tagQuickSettingsOverlay = true)
        val slots = baseSlots.copy(
            errorDialog = { mod ->
                if (showErrorDialog) {
                    Box(modifier = mod.size(200.dp).testTag(CAMERA_ERROR_DIALOG_TAG))
                }
            }
        )

        composeTestRule.setContent {
            MaterialTheme {
                FoldableLayout(
                    slots = slots,
                    viewfinder = { mod -> PlaceholderViewfinder(modifier = mod) },
                    state = state,
                    controlsVisible = controlsVisible,
                    isErrorDialogShowing = showErrorDialog
                )
            }
        }
        composeTestRule.waitForIdle()

        // Error dialog supersedes Quick Settings card and prunes background accessibility.
        composeTestRule.runOnIdle { showErrorDialog = true }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAMERA_ERROR_DIALOG_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithTag(QUICK_SETTINGS_OVERLAY_CARD_PLACEHOLDER_TAG)
            .assertDoesNotExist()
        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assert(hiddenFromA11yAncestor)

        // controlsVisible = false removes side rails while keeping viewfinder visible.
        composeTestRule.runOnIdle {
            showErrorDialog = false
            state.dismissQuickSettings()
            controlsVisible = false
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(CAPTURE_BUTTON).assertDoesNotExist()
        composeTestRule.onNodeWithTag(START_SIDE_RAIL_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(END_SIDE_RAIL_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).assertIsDisplayed()
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
