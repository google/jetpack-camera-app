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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutDefaults
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolver
import com.google.jetpackcamera.ui.components.capture.layout.CameraWindow
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleQuickSettingsButton
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Verifies the vertical placement guarantees of the capture screen control stack.
 *
 * The middle slot between the shutter row and the lower controls row is reserved at a fixed
 * height, so populating it must never move the capture button. Each test pins the window size
 * with Robolectric qualifiers so that the regular and the compressed spacing are both covered.
 */
@RunWith(RobolectricTestRunner::class)
class CaptureLayoutPositionTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    private fun TestCaptureLayout(captureModeCarousel: @Composable (Modifier) -> Unit = {}) {
        PreviewLayout(
            viewfinder = {},
            captureButton = {
                Box(
                    Modifier
                        .testTag(CAPTURE_BUTTON_TAG)
                        .size(CAPTURE_BUTTON_SIZE)
                )
            },
            imageWell = {},
            flipCameraButton = {},
            zoomLevelDisplay = {
                Box(
                    Modifier
                        .testTag(ZOOM_BAR_TAG)
                        .fillMaxWidth()
                        .height(ZOOM_BAR_HEIGHT)
                )
            },
            elapsedTimeDisplay = {},
            quickSettingsButton = {},
            indicatorRow = {},
            captureModeToggle = {},
            captureModeCarousel = captureModeCarousel,
            quickSettingsOverlay = {},
            debugOverlay = {},
            debugVisibilityWrapper = { it() },
            screenFlashOverlay = {},
            snackBar = { _, _ -> }
        )
    }

    private fun topOf(tag: String) =
        composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot().top.value

    private fun rootHeight() = composeTestRule.onRoot().getUnclippedBoundsInRoot().height.value

    /**
     * The capture button must sit at exactly the same height whether or not the reserved middle
     * slot has content. This is the regression guard for the slot reservation.
     *
     * The window is tall enough to use the regular (uncompressed) spacing.
     */
    @Config(qualifiers = "w360dp-h800dp")
    @Test
    fun captureButtonPosition_isUnchanged_whenMiddleSlotIsPopulated() {
        var slotPopulated by mutableStateOf(false)

        composeTestRule.setContent {
            TestCaptureLayout(
                captureModeCarousel = { carouselModifier ->
                    if (slotPopulated) {
                        Box(
                            carouselModifier
                                .fillMaxWidth()
                                .height(CAROUSEL_HEIGHT)
                        )
                    }
                }
            )
        }
        composeTestRule.waitForIdle()

        // Guards against the window silently being a different size than the qualifier asks for,
        // which would otherwise let this test pass while exercising the compressed spacing.
        assertThat(rootHeight()).isWithin(TOLERANCE).of(TALL_SCREEN_HEIGHT.value)

        val emptySlotTop = topOf(CAPTURE_BUTTON_TAG)
        assertThat(emptySlotTop).isWithin(TOLERANCE).of(EXPECTED_TALL_BUTTON_TOP)

        slotPopulated = true
        composeTestRule.waitForIdle()

        val populatedSlotTop = topOf(CAPTURE_BUTTON_TAG)

        assertThat(populatedSlotTop).isEqualTo(emptySlotTop)
    }

    /**
     * On a short window the gaps compress while the fixed element heights stay intact, which
     * pulls the top of the stack down and keeps the zoom bar on screen.
     *
     * The stack is bottom anchored, so any overflow clips at the top, starting with the zoom bar.
     * The regular stack is 340dp tall (48 + 32 + 86 + 24 + 32 + 24 + 64, plus 30dp of bottom
     * padding) and the compressed stack is 282dp tall (48 + 16 + 86 + 12 + 32 + 12 + 64, plus
     * 12dp of bottom padding); compression therefore buys 58dp of headroom but clipping still
     * begins once less than 282dp of height is available.
     */
    @Config(qualifiers = "w360dp-h500dp")
    @Test
    fun controlStack_onShortScreen_compressesAndKeepsZoomBarOnScreen() {
        composeTestRule.setContent { TestCaptureLayout() }
        composeTestRule.waitForIdle()

        assertThat(rootHeight()).isWithin(TOLERANCE).of(SHORT_SCREEN_HEIGHT.value)
        // The compressed gaps place the capture button lower than the regular spacing would.
        assertThat(topOf(CAPTURE_BUTTON_TAG)).isWithin(TOLERANCE).of(EXPECTED_SHORT_BUTTON_TOP)
        // The top of the stack must not be pushed off the top of the window.
        assertThat(topOf(ZOOM_BAR_TAG)).isAtLeast(0f)
    }

    /**
     * Switching between 3:4, 1:1, and 9:16 viewfinders must place each viewfinder at the solver's
     * target band while keeping the capture button completely stationary.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = "w412dp-h915dp")
    @Test
    fun viewfinderAndControlStack_matchSolverAndRemainStationaryAcrossAspectRatios() {
        var aspectRatio by mutableStateOf(3f / 4f)

        composeTestRule.setContent {
            PreviewLayout(
                viewfinder = { vfModifier ->
                    Box(
                        vfModifier
                            .testTag(VIEWFINDER_TAG)
                            .fillMaxWidth()
                            .height(412.dp / aspectRatio)
                    )
                },
                captureButton = {
                    Box(
                        Modifier
                            .testTag(CAPTURE_BUTTON_TAG)
                            .size(CAPTURE_BUTTON_SIZE)
                    )
                },
                imageWell = {},
                flipCameraButton = {},
                zoomLevelDisplay = {
                    Box(
                        Modifier
                            .testTag(ZOOM_BAR_TAG)
                            .fillMaxWidth()
                            .height(ZOOM_BAR_HEIGHT)
                    )
                },
                elapsedTimeDisplay = {},
                quickSettingsButton = {},
                indicatorRow = {},
                captureModeToggle = {},
                quickSettingsOverlay = {},
                debugOverlay = {},
                debugVisibilityWrapper = { it() },
                screenFlashOverlay = {},
                snackBar = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        val buttonTop34 = topOf(CAPTURE_BUTTON_TAG)
        val vfTop34 = topOf(VIEWFINDER_TAG)

        aspectRatio = 1f
        composeTestRule.waitForIdle()
        val buttonTop11 = topOf(CAPTURE_BUTTON_TAG)
        val vfTop11 = topOf(VIEWFINDER_TAG)

        aspectRatio = 9f / 16f
        composeTestRule.waitForIdle()
        val buttonTop916 = topOf(CAPTURE_BUTTON_TAG)
        val vfTop916 = topOf(VIEWFINDER_TAG)

        // Control stack must not move by a single pixel across aspect ratios.
        assertThat(buttonTop11).isEqualTo(buttonTop34)
        assertThat(buttonTop916).isEqualTo(buttonTop34)

        // 1:1 viewfinder is centered within the 3:4 frame area rather than pinned to the top bar.
        assertThat(vfTop11).isGreaterThan(vfTop34)
        assertThat(vfTop916).isAtLeast(0f)
    }

    /**
     * `topStartContent` stays anchored directly below the top bar across aspect ratios, even when
     * the viewfinder shifts down below it.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = "w412dp-h915dp")
    @Test
    fun topStartContent_staysFixedBelowTopBar_acrossAspectRatios() {
        var aspectRatio by mutableStateOf(3f / 4f)

        composeTestRule.setContent {
            PreviewLayout(
                viewfinder = { vfModifier ->
                    Box(
                        vfModifier
                            .testTag(VIEWFINDER_TAG)
                            .fillMaxWidth()
                            .height(412.dp / aspectRatio)
                    )
                },
                topStartContent = { topStartModifier ->
                    Box(
                        topStartModifier
                            .testTag(TOP_START_TAG)
                            .size(48.dp)
                    )
                },
                captureButton = {
                    Box(
                        Modifier
                            .testTag(CAPTURE_BUTTON_TAG)
                            .size(CAPTURE_BUTTON_SIZE)
                    )
                },
                imageWell = {},
                flipCameraButton = {},
                zoomLevelDisplay = {},
                elapsedTimeDisplay = {},
                quickSettingsButton = {},
                indicatorRow = {},
                captureModeToggle = {},
                quickSettingsOverlay = {},
                debugOverlay = {},
                debugVisibilityWrapper = { it() },
                screenFlashOverlay = {},
                snackBar = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        val topStart34 = topOf(TOP_START_TAG)
        assertThat(topStart34).isWithin(TOLERANCE).of(48f)
        assertThat(topOf(VIEWFINDER_TAG)).isWithin(TOLERANCE).of(48f)

        // Switching to 1:1 moves the viewfinder down, but keeps topStartContent stationary.
        aspectRatio = 1f
        composeTestRule.waitForIdle()
        val topStart11 = topOf(TOP_START_TAG)
        assertThat(topStart11).isEqualTo(topStart34)
        assertThat(topOf(VIEWFINDER_TAG)).isGreaterThan(topStart11 + 48f)
    }

    /**
     * Content taller than the mode switcher band, such as a carousel with a larger touch target,
     * is centered on the band without moving any row, and is limited to the band plus the smaller
     * adjacent gap on each side so it never overlaps the neighboring rows.
     *
     * On 360x800dp the solver places the capture row at 532..618dp, the mode switcher band at
     * 642..674dp (center 658dp) and the bottom toolbar at 698..762dp, leaving 24dp gaps.
     */
    @Config(qualifiers = "w360dp-h800dp")
    @Test
    fun modeSwitcherContent_tallerThanBand_isCenteredAndBoundedByAdjacentGaps() {
        var carouselHeight by mutableStateOf(0.dp)

        composeTestRule.setContent {
            TestCaptureLayout(
                captureModeCarousel = { carouselModifier ->
                    if (carouselHeight > 0.dp) {
                        Box(
                            carouselModifier
                                .testTag(CAROUSEL_TAG)
                                .fillMaxWidth()
                                .height(carouselHeight)
                        )
                    }
                }
            )
        }
        composeTestRule.waitForIdle()
        val emptySlotButtonTop = topOf(CAPTURE_BUTTON_TAG)

        carouselHeight = TALL_CAROUSEL_HEIGHT
        composeTestRule.waitForIdle()
        val carousel = composeTestRule.onNodeWithTag(CAROUSEL_TAG).getUnclippedBoundsInRoot()
        assertThat(carousel.height.value).isWithin(TOLERANCE).of(TALL_CAROUSEL_HEIGHT.value)
        assertThat((carousel.top.value + carousel.bottom.value) / 2f)
            .isWithin(TOLERANCE).of(MODE_SWITCHER_BAND_CENTER)
        assertThat(topOf(CAPTURE_BUTTON_TAG)).isEqualTo(emptySlotButtonTop)

        // Oversized content is clamped to 32dp + 2 * 24dp and still stays clear of both rows.
        carouselHeight = 120.dp
        composeTestRule.waitForIdle()
        val clamped = composeTestRule.onNodeWithTag(CAROUSEL_TAG).getUnclippedBoundsInRoot()
        assertThat(clamped.height.value).isWithin(TOLERANCE).of(80f)
        assertThat(clamped.top.value).isAtLeast(CAPTURE_ROW_BOTTOM - TOLERANCE)
        assertThat(clamped.bottom.value).isAtMost(BOTTOM_TOOLBAR_TOP + TOLERANCE)
        assertThat(topOf(CAPTURE_BUTTON_TAG)).isEqualTo(emptySlotButtonTop)
    }

    /**
     * In the compressed fallback layout, a carousel taller than the reserved middle slot keeps its
     * full height and does not move the capture button, because the slot shares the adjacent
     * gaps.
     */
    @Config(qualifiers = "w360dp-h500dp")
    @Test
    fun middleSlotContent_tallerThanSlot_keepsHeightInCompressedFallbackLayout() {
        composeTestRule.setContent {
            TestCaptureLayout(
                captureModeCarousel = { carouselModifier ->
                    Box(
                        carouselModifier
                            .testTag(CAROUSEL_TAG)
                            .fillMaxWidth()
                            .height(TALL_CAROUSEL_HEIGHT)
                    )
                }
            )
        }
        composeTestRule.waitForIdle()

        assertThat(topOf(CAPTURE_BUTTON_TAG)).isWithin(TOLERANCE).of(EXPECTED_SHORT_BUTTON_TOP)
        val carousel = composeTestRule.onNodeWithTag(CAROUSEL_TAG).getUnclippedBoundsInRoot()
        assertThat(carousel.height.value).isWithin(TOLERANCE).of(TALL_CAROUSEL_HEIGHT.value)
    }

    /**
     * The adaptive layout reports whether immersive mode is required and withdraws the request
     * when it leaves the composition, so a stale request can never outlive the layout.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = "w360dp-h800dp")
    @Test
    fun onRequiresImmersiveChange_reportsSolverValue_andFalseOnDispose() {
        val reported = mutableListOf<Boolean>()
        var showLayout by mutableStateOf(true)

        composeTestRule.setContent {
            if (showLayout) {
                PreviewLayout(
                    viewfinder = {},
                    captureButton = {},
                    imageWell = {},
                    flipCameraButton = {},
                    zoomLevelDisplay = {},
                    elapsedTimeDisplay = {},
                    quickSettingsButton = {},
                    indicatorRow = {},
                    captureModeToggle = {},
                    quickSettingsOverlay = {},
                    debugOverlay = {},
                    debugVisibilityWrapper = { it() },
                    screenFlashOverlay = {},
                    snackBar = { _, _ -> },
                    onRequiresImmersiveChange = { reported += it }
                )
            }
        }
        composeTestRule.waitForIdle()
        // A 360x800dp window with no navigation bar never requires immersive mode.
        assertThat(reported).containsExactly(false)

        reported.clear()
        showLayout = false
        composeTestRule.waitForIdle()
        assertThat(reported).containsExactly(false)
    }

    /**
     * Verifies that when the adaptive solver compacts the bottom toolbar (e.g. on a 460x920dp
     * window with 3-button navigation where `toolbarHeight = 46.dp`), [ToggleSwitch] and
     * [ToggleQuickSettingsButton] scale down uniformly in both width and height.
     */
    @Test
    fun bottomToolbarControls_scaleProportionally_whenToolbarIsCompacted() {
        val compactBandWindow = CameraWindow(
            width = 460.dp,
            height = 920.dp,
            topInset = 42.dp,
            navInset = 48.dp,
            gestureInset = 48.dp
        )
        val compactSolution = CameraLayoutSolver.solve(
            spec = CameraLayoutDefaults.spec(),
            window = compactBandWindow
        )
        assertThat(compactSolution.toolbarHeight.value).isWithin(TOLERANCE).of(46f)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalCameraLayoutSolution provides compactSolution) {
                Box {
                    ToggleSwitch(
                        checked = false,
                        onCheckedChange = {},
                        modifier = Modifier.testTag(TOGGLE_SWITCH_TAG)
                    )
                    ToggleQuickSettingsButton(
                        isOpen = false,
                        onClick = {}
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val expectedScale = 46f / 64f
        val switchBounds =
            composeTestRule.onNodeWithTag(TOGGLE_SWITCH_TAG).getUnclippedBoundsInRoot()
        assertThat(switchBounds.width.value).isWithin(TOLERANCE).of(116f * expectedScale)
        assertThat(switchBounds.height.value).isWithin(TOLERANCE).of(64f * expectedScale)

        val quickSettingsBounds =
            composeTestRule.onNodeWithTag(QUICK_SETTINGS_DROP_DOWN).getUnclippedBoundsInRoot()
        assertThat(quickSettingsBounds.width.value).isWithin(TOLERANCE).of(48f * expectedScale)
        assertThat(quickSettingsBounds.height.value).isWithin(TOLERANCE).of(56f * expectedScale)
    }

    /**
     * On a short 16:9 portrait handheld (360x640dp), the adaptive solver places the control stack
     * at captureRow = 366..452dp (centered 80dp button at 369dp) while keeping the zoom bar on
     * screen, and keeps `topStartContent` below the top bar even when a full-bleed 9:16 viewfinder
     * starts at y = 0.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Config(qualifiers = "w360dp-h640dp")
    @Test
    fun controlStack_onShort16By9Phone_matchesGoldenSolverPlacement() {
        composeTestRule.setContent {
            PreviewLayout(
                viewfinder = { vfModifier ->
                    Box(
                        vfModifier
                            .testTag(VIEWFINDER_TAG)
                            .fillMaxWidth()
                            .height(640.dp)
                    )
                },
                topStartContent = { topStartModifier ->
                    Box(
                        topStartModifier
                            .testTag(TOP_START_TAG)
                            .size(48.dp)
                    )
                },
                captureButton = {
                    Box(
                        Modifier
                            .testTag(CAPTURE_BUTTON_TAG)
                            .size(CAPTURE_BUTTON_SIZE)
                    )
                },
                imageWell = {},
                flipCameraButton = {},
                zoomLevelDisplay = {
                    Box(
                        Modifier
                            .testTag(ZOOM_BAR_TAG)
                            .fillMaxWidth()
                            .height(ZOOM_BAR_HEIGHT)
                    )
                },
                elapsedTimeDisplay = {},
                quickSettingsButton = {},
                indicatorRow = {},
                captureModeToggle = {},
                quickSettingsOverlay = {},
                debugOverlay = {},
                debugVisibilityWrapper = { it() },
                screenFlashOverlay = {},
                snackBar = { _, _ -> }
            )
        }
        composeTestRule.waitForIdle()

        assertThat(rootHeight()).isWithin(TOLERANCE).of(640f)
        assertThat(topOf(CAPTURE_BUTTON_TAG)).isWithin(TOLERANCE).of(EXPECTED_16_9_BUTTON_TOP)
        assertThat(topOf(ZOOM_BAR_TAG)).isAtLeast(0f)
        assertThat(topOf(VIEWFINDER_TAG)).isWithin(TOLERANCE).of(0f)
        assertThat(topOf(TOP_START_TAG)).isWithin(TOLERANCE).of(48f)
    }

    private companion object {
        const val CAPTURE_BUTTON_TAG = "test_capture_button"
        const val ZOOM_BAR_TAG = "test_zoom_bar"
        const val VIEWFINDER_TAG = "test_viewfinder"
        const val TOP_START_TAG = "test_top_start"
        const val TOGGLE_SWITCH_TAG = "test_toggle_switch"
        const val CAROUSEL_TAG = "test_carousel"
        val CAPTURE_BUTTON_SIZE = 80.dp
        val ZOOM_BAR_HEIGHT = 48.dp
        val CAROUSEL_HEIGHT = 32.dp

        /** A carousel whose touch target is taller than the 32dp mode switcher band. */
        val TALL_CAROUSEL_HEIGHT = 50.dp
        val TALL_SCREEN_HEIGHT = 800.dp
        val SHORT_SCREEN_HEIGHT = 500.dp

        /**
         * Solver spacing on 360x800dp:
         * 800 - 38 (resolved bottom padding) - 64 (toolbar) - 24 - 32 (mode switcher) - 24 - 86
         * (capture) places the top of the 86dp shutter row at 532dp, and the centered 80dp capture
         * button at 535dp.
         */
        const val EXPECTED_TALL_BUTTON_TOP = 535f

        /** Bottom of the 86dp shutter row on 360x800dp (532 + 86). */
        const val CAPTURE_ROW_BOTTOM = 618f

        /** Center of the 32dp mode switcher band at 642..674dp on 360x800dp. */
        const val MODE_SWITCHER_BAND_CENTER = 658f

        /** Top of the 64dp bottom toolbar at 698..762dp on 360x800dp. */
        const val BOTTOM_TOOLBAR_TOP = 698f

        /**
         * Solver placement on 360x640dp (with zero navigation bar inset in Robolectric):
         * 640 - 44 (resolved bottom padding, 4dp below the 3:4 frame at 48..528dp) - 64 (toolbar)
         * - 24 - 32 (mode switcher) - 24 - 86 (capture) places the shutter row at 366..452dp, and
         * the centered 80dp capture button at 369dp.
         */
        const val EXPECTED_16_9_BUTTON_TOP = 369f

        /**
         * Compressed spacing on 360x500dp (wider than 16:9 split-screen fallback):
         * 12 + 64 + 12 + 32 + 12 + 86 puts the top of the shutter row at 500 - 218 = 282dp, plus
         * 3dp of centering = 285dp.
         */
        const val EXPECTED_SHORT_BUTTON_TOP = 285f

        const val TOLERANCE = 0.5f
    }
}
