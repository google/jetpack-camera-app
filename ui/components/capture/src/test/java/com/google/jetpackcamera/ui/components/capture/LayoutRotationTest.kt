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

import android.graphics.Rect as AndroidRect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolution
import com.google.jetpackcamera.ui.components.capture.layout.CameraRowIds
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private object LayoutRotationTestTokens {
    const val INDICATOR_TAG = "RotationTestIndicatorRow"
    const val VIEWFINDER_TOP_MARKER_TAG = "RotationTestViewfinderTopMarker"

    val CaptureButtonSize = 80.dp
    val ControlSize = 48.dp
    val ZoomBarWidth = 160.dp
    val IndicatorWidth = 120.dp
    val IndicatorHeight = 36.dp
    val ModeToggleWidth = 96.dp
}

/**
 * Robolectric Compose UI tests verifying layout-level counter-rotation scoping across full-screen
 * compact handhelds, portrait & horizontal split-screen panes (`852x420dp`, `800x400dp`,
 * `550x500dp`), un-rotated viewfinder hosting, 7-mode side-rail postures (`FOLDABLE`,
 * `TABLET_PORTRAIT`, `TABLET_LANDSCAPE`, `COMPACT_SHORT`), and [CutoutAwareRow] cutout remapping.
 */
@RunWith(RobolectricTestRunner::class)
class LayoutRotationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun RotationLayoutUnderTest(
        deviceRotation: DeviceRotation,
        windowRotation: DeviceRotation,
        modifier: Modifier = Modifier,
        slots: CaptureSlots? = null,
        onViewfinderObserved: (
            mode: CameraLayoutMode,
            vfWindowRotation: DeviceRotation,
            vfGravityDegrees: Float,
            solution: CameraLayoutSolution?,
            viewfinderSize: IntSize
        ) -> Unit = { _, _, _, _, _ -> },
        onChromeObserved: (
            chromeWindowRotation: DeviceRotation,
            chromeGravityDegrees: Float,
            solution: CameraLayoutSolution?
        ) -> Unit = { _, _, _ -> }
    ) {
        MaterialTheme {
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalDisableAnimations provides true
            ) {
                CaptureScaffoldLayout(
                    modifier = modifier,
                    deviceRotation = deviceRotation,
                    windowRotation = windowRotation,
                    slots = slots,
                    viewfinder = { vfModifier ->
                        val mode = LocalCameraLayoutMode.current
                        val vfWinRot = LocalWindowRotation.current
                        val vfGravityDeg = LocalGravityRotationState.current.value
                        val solution = LocalCameraLayoutSolution.current
                        Box(
                            modifier = vfModifier
                                .fillMaxSize()
                                .onSizeChanged { vfSize ->
                                    onViewfinderObserved(
                                        mode,
                                        vfWinRot,
                                        vfGravityDeg,
                                        solution,
                                        vfSize
                                    )
                                }
                                .testTag(PREVIEW_DISPLAY)
                        ) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .size(width = 24.dp, height = 6.dp)
                                    .testTag(LayoutRotationTestTokens.VIEWFINDER_TOP_MARKER_TAG)
                            )
                        }
                    },
                    captureButton = { mod ->
                        onChromeObserved(
                            LocalWindowRotation.current,
                            LocalGravityRotationState.current.value,
                            LocalCameraLayoutSolution.current
                        )
                        Box(
                            mod
                                .size(LayoutRotationTestTokens.CaptureButtonSize)
                                .testTag(CAPTURE_BUTTON)
                        )
                    },
                    imageWell = { mod ->
                        Box(
                            mod
                                .size(LayoutRotationTestTokens.ControlSize)
                                .testTag(IMAGE_WELL_TAG)
                        )
                    },
                    flipCameraButton = { mod ->
                        Box(
                            mod
                                .size(LayoutRotationTestTokens.ControlSize)
                                .testTag(FLIP_CAMERA_BUTTON)
                        )
                    },
                    zoomLevelDisplay = { mod ->
                        Box(
                            mod
                                .size(
                                    width = LayoutRotationTestTokens.ZoomBarWidth,
                                    height = LayoutRotationTestTokens.ControlSize
                                )
                                .testTag(ZOOM_BUTTON_ROW_TAG)
                        )
                    },
                    elapsedTimeDisplay = {},
                    quickSettingsButton = { mod ->
                        Box(
                            mod
                                .size(LayoutRotationTestTokens.ControlSize)
                                .testTag(QUICK_SETTINGS_DROP_DOWN)
                        )
                    },
                    indicatorRow = { mod ->
                        Box(
                            mod
                                .size(
                                    width = LayoutRotationTestTokens.IndicatorWidth,
                                    height = LayoutRotationTestTokens.IndicatorHeight
                                )
                                .testTag(LayoutRotationTestTokens.INDICATOR_TAG)
                        )
                    },
                    captureModeToggle = { mod ->
                        Box(
                            mod
                                .size(
                                    width = LayoutRotationTestTokens.ModeToggleWidth,
                                    height = LayoutRotationTestTokens.ControlSize
                                )
                                .testTag(CAPTURE_MODE_TOGGLE_BUTTON)
                        )
                    }
                )
            }
        }
    }

    @Config(qualifiers = "sw360dp-w740dp-h360dp-normal-long-land-mdpi")
    @Test
    fun compactPortraitLayout_landscapePhone_counterRotatesChromeWithUprightViewfinder() {
        var windowRotation by mutableStateOf(DeviceRotation.Rotated90)
        var observedMode: CameraLayoutMode? = null
        var observedVfWinRot: DeviceRotation? = null
        var observedChromeWinRot: DeviceRotation? = null
        var observedChromeGravityDeg: Float? = null
        var observedSolution: CameraLayoutSolution? = null
        var observedViewfinderSize = IntSize.Zero

        composeTestRule.setContent {
            RotationLayoutUnderTest(
                deviceRotation = windowRotation,
                windowRotation = windowRotation,
                onViewfinderObserved = { mode, vfWinRot, _, solution, vfSize ->
                    observedMode = mode
                    observedVfWinRot = vfWinRot
                    observedSolution = solution
                    observedViewfinderSize = vfSize
                },
                onChromeObserved = { chromeWinRot, chromeGravDeg, _ ->
                    observedChromeWinRot = chromeWinRot
                    observedChromeGravityDeg = chromeGravDeg
                }
            )
        }
        composeTestRule.waitForIdle()

        assertThat(observedMode).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(observedVfWinRot).isEqualTo(DeviceRotation.Rotated90)
        assertThat(observedChromeWinRot).isEqualTo(DeviceRotation.Rotated90)
        assertThat(observedSolution).isNotNull()
        assertThat(observedViewfinderSize).isEqualTo(IntSize(740, 360))
        assertThat(((observedChromeGravityDeg!! % 360f) + 360f) % 360f).isEqualTo(270f)

        // Viewfinder top marker remains at physical top of the 740x360dp window (0° rotation).
        val topMarker90 = composeTestRule
            .onNodeWithTag(LayoutRotationTestTokens.VIEWFINDER_TOP_MARKER_TAG)
            .getBoundsInRoot()
        assertThat(topMarker90.top.value).isEqualTo(0f)

        val captureBand90 = observedSolution!!.rowBands.getValue(CameraRowIds.CAPTURE_ROW)
        val expectedCaptureCenterX90 =
            740f - (captureBand90.top.value + captureBand90.bottom.value) / 2f
        val indicatorRotated90 = composeTestRule
            .onNodeWithTag(LayoutRotationTestTokens.INDICATOR_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val captureRotated90 = composeTestRule
            .onNodeWithTag(CAPTURE_BUTTON)
            .fetchSemanticsNode()
            .boundsInRoot
        assertThat(indicatorRotated90.left).isGreaterThan(680f)
        assertThat(captureRotated90.center.x).isWithin(1f).of(expectedCaptureCenterX90)
        assertThat(captureRotated90.center.y).isWithin(1f).of(180f)

        windowRotation = DeviceRotation.Rotated270
        composeTestRule.waitForIdle()

        assertThat(observedMode).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(observedVfWinRot).isEqualTo(DeviceRotation.Rotated270)
        assertThat(observedChromeWinRot).isEqualTo(DeviceRotation.Rotated270)
        assertThat(observedSolution).isNotNull()
        assertThat(observedViewfinderSize).isEqualTo(IntSize(740, 360))
        assertThat(((observedChromeGravityDeg!! % 360f) + 360f) % 360f).isEqualTo(90f)

        val captureBand270 = observedSolution!!.rowBands.getValue(CameraRowIds.CAPTURE_ROW)
        val expectedCaptureCenterX270 =
            (captureBand270.top.value + captureBand270.bottom.value) / 2f
        val indicatorRotated270 = composeTestRule
            .onNodeWithTag(LayoutRotationTestTokens.INDICATOR_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val captureRotated270 = composeTestRule
            .onNodeWithTag(CAPTURE_BUTTON)
            .fetchSemanticsNode()
            .boundsInRoot
        assertThat(indicatorRotated270.right).isLessThan(60f)
        assertThat(captureRotated270.center.x).isWithin(1f).of(expectedCaptureCenterX270)
        assertThat(captureRotated270.center.y).isWithin(1f).of(180f)
    }

    @Config(qualifiers = "sw360dp-w800dp-h800dp-mdpi")
    @Test
    fun compactPortraitLayout_portraitSplitScreenOnRotatedPhone_doesNotCounterRotate() {
        var observedMode: CameraLayoutMode? = null
        var observedEffectiveWinRot: DeviceRotation? = null
        var observedGravityDeg: Float? = null
        var observedSolution: CameraLayoutSolution? = null
        var observedViewfinderSize = IntSize.Zero

        composeTestRule.setContent {
            RotationLayoutUnderTest(
                modifier = Modifier.size(width = 380.dp, height = 760.dp),
                deviceRotation = DeviceRotation.Rotated90,
                windowRotation = DeviceRotation.Rotated90,
                onViewfinderObserved = { mode, effWinRot, gravDeg, solution, vfSize ->
                    observedMode = mode
                    observedEffectiveWinRot = effWinRot
                    observedGravityDeg = gravDeg
                    observedSolution = solution
                    observedViewfinderSize = vfSize
                }
            )
        }
        composeTestRule.waitForIdle()

        assertThat(observedMode).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(observedEffectiveWinRot).isEqualTo(DeviceRotation.Natural)
        assertThat(observedGravityDeg).isEqualTo(0f)
        assertThat(observedSolution).isNotNull()
        assertThat(observedViewfinderSize).isEqualTo(IntSize(380, 760))
    }

    @Config(qualifiers = "sw800dp-w1400dp-h1000dp-mdpi")
    @Test
    fun compactHorizontalSplitScreen_keepsViewfinderUnrotatedAcrossSizesAndHostRotations() {
        var paneSize by mutableStateOf(DpSize(852.dp, 420.dp))
        var hostWindowRotation by mutableStateOf(DeviceRotation.Natural)
        var observedMode: CameraLayoutMode? = null
        var observedVfWinRot: DeviceRotation? = null
        var observedVfGravityDeg: Float? = null
        var observedChromeWinRot: DeviceRotation? = null
        var observedChromeGravityDeg: Float? = null
        var observedViewfinderSize = IntSize.Zero

        composeTestRule.setContent {
            RotationLayoutUnderTest(
                modifier = Modifier.size(paneSize),
                deviceRotation = hostWindowRotation,
                windowRotation = hostWindowRotation,
                onViewfinderObserved = { mode, vfWinRot, vfGravDeg, _, vfSize ->
                    observedMode = mode
                    observedVfWinRot = vfWinRot
                    observedVfGravityDeg = vfGravDeg
                    observedViewfinderSize = vfSize
                },
                onChromeObserved = { chromeWinRot, chromeGravDeg, _ ->
                    observedChromeWinRot = chromeWinRot
                    observedChromeGravityDeg = chromeGravDeg
                }
            )
        }

        val paneSizes = listOf(
            DpSize(852.dp, 420.dp) to IntSize(852, 420),
            DpSize(800.dp, 400.dp) to IntSize(800, 400),
            DpSize(550.dp, 500.dp) to IntSize(502, 500)
        )
        val hostRotations = listOf(
            DeviceRotation.Natural,
            DeviceRotation.Rotated90,
            DeviceRotation.Rotated270
        )

        for ((size, expectedVfSize) in paneSizes) {
            for (winRot in hostRotations) {
                paneSize = size
                hostWindowRotation = winRot
                composeTestRule.waitForIdle()

                val label = "pane=$size hostWinRot=$winRot"
                assertWithMessage(label).that(observedMode).isEqualTo(CameraLayoutMode.COMPACT)
                // Viewfinder stays in un-rotated host window frame (0° upright, LocalWindowRotation
                // == windowRotation), while UI chrome counter-rotates into portrait bands.
                assertWithMessage(label).that(observedVfWinRot).isEqualTo(winRot)
                assertWithMessage(label).that(observedVfGravityDeg).isEqualTo(0f)
                assertWithMessage(label).that(observedViewfinderSize).isEqualTo(expectedVfSize)
                assertWithMessage(label).that(observedChromeWinRot?.isLandscape).isTrue()
                assertWithMessage(label)
                    .that(((observedChromeGravityDeg!! % 360f) + 360f) % 360f)
                    .isIn(listOf(90f, 270f))

                val topMarker = composeTestRule
                    .onNodeWithTag(LayoutRotationTestTokens.VIEWFINDER_TOP_MARKER_TAG)
                    .getBoundsInRoot()
                assertWithMessage("$label viewfinder top marker upright at y=0")
                    .that(topMarker.top.value)
                    .isEqualTo(0f)
                assertWithMessage("$label viewfinder top marker horizontal")
                    .that(topMarker.width.value)
                    .isGreaterThan(topMarker.height.value)
            }
        }
    }

    @Config(qualifiers = "sw800dp-w1400dp-h1000dp-mdpi")
    @Test
    fun previewDisplay_inHorizontalCompactSplitScreen_alignsSolvedBandHorizontallyWithChrome() {
        var hostRotation by mutableStateOf(DeviceRotation.Rotated270)
        var observedSolution: CameraLayoutSolution? = null

        composeTestRule.setContent {
            MaterialTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(1f),
                    LocalDisableAnimations provides true
                ) {
                    CaptureScaffoldLayout(
                        modifier = Modifier.size(width = 800.dp, height = 400.dp),
                        deviceRotation = hostRotation,
                        windowRotation = hostRotation,
                        viewfinder = { vfModifier ->
                            observedSolution = LocalCameraLayoutSolution.current
                            PreviewDisplay(
                                modifier = vfModifier,
                                previewDisplayUiState = PreviewDisplayUiState(
                                    lastBlinkTimeStamp = 0L,
                                    aspectRatioUiState = AspectRatioUiState.Available(
                                        selectedAspectRatio = AspectRatio.THREE_FOUR,
                                        availableAspectRatios = listOf(
                                            SingleSelectableUiState.SelectableUi(
                                                AspectRatio.THREE_FOUR
                                            )
                                        )
                                    )
                                ),
                                onTapToFocus = { _, _ -> },
                                onFlipCamera = {},
                                onScaleZoom = {},
                                onRequestWindowColorMode = {},
                                surfaceRequest = null,
                                focusMeteringUiState = FocusMeteringUiState.Unspecified,
                                useLandscapeAspectRatio = true
                            )
                        },
                        indicatorRow = { mod ->
                            Box(
                                mod
                                    .size(120.dp, 36.dp)
                                    .testTag(LayoutRotationTestTokens.INDICATOR_TAG)
                            )
                        },
                        captureButton = { mod ->
                            Box(mod.size(80.dp).testTag(CAPTURE_BUTTON))
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        val band270 = checkNotNull(observedSolution).viewfinderBandFor(3f / 4f)!!
        val vfBounds270 = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getBoundsInRoot()
        // Rotated270 (-90° CCW chrome): top bar on left [0, 48], 4:3 frame [48, 48 + 533.33] x 400.
        assertThat(vfBounds270.left.value).isWithin(1f).of(band270.top.value)
        assertThat(vfBounds270.width.value).isWithin(1f).of(band270.height.value)
        assertThat(vfBounds270.height.value).isWithin(1f).of(400f)

        // Rotated90 (+90° CW chrome): top bar on right [752, 800], 4:3 frame flush against x=752.
        hostRotation = DeviceRotation.Rotated90
        composeTestRule.waitForIdle()

        val band90 = checkNotNull(observedSolution).viewfinderBandFor(3f / 4f)!!
        val vfBounds90 = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getBoundsInRoot()
        assertThat(vfBounds90.right.value).isWithin(1f).of(800f - band90.top.value)
        assertThat(vfBounds90.width.value).isWithin(1f).of(band90.height.value)
        assertThat(vfBounds90.height.value).isWithin(1f).of(400f)
    }

    @Config(qualifiers = "sw800dp-w1400dp-h1400dp-mdpi")
    @Test
    fun foldableTabletAndCompactShortLayouts_neverCounterRotate() {
        var containerSize by mutableStateOf(DpSize(1280.dp, 800.dp))
        var windowRotation by mutableStateOf(DeviceRotation.Rotated90)
        var observedMode: CameraLayoutMode? = null
        var observedViewfinderSize = IntSize.Zero

        composeTestRule.setContent {
            RotationLayoutUnderTest(
                modifier = Modifier.size(containerSize),
                deviceRotation = windowRotation,
                windowRotation = windowRotation,
                slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                onViewfinderObserved = { mode, _, _, _, vfSize ->
                    observedMode = mode
                    observedViewfinderSize = vfSize
                }
            )
        }

        val testPostures = listOf(
            Triple(DpSize(1280.dp, 800.dp), CameraLayoutMode.TABLET_LANDSCAPE, IntSize(1280, 800)),
            Triple(DpSize(800.dp, 1280.dp), CameraLayoutMode.TABLET_PORTRAIT, IntSize(800, 1280)),
            Triple(DpSize(883.dp, 852.dp), CameraLayoutMode.FOLDABLE, IntSize(883, 852)),
            Triple(DpSize(440.dp, 412.dp), CameraLayoutMode.COMPACT_SHORT, IntSize(440, 412))
        )

        for ((size, expectedMode, expectedVfSize) in testPostures) {
            for (rotation in listOf(DeviceRotation.Rotated90, DeviceRotation.Rotated270)) {
                containerSize = size
                windowRotation = rotation
                composeTestRule.waitForIdle()

                assertThat(observedMode).isEqualTo(expectedMode)
                assertThat(observedViewfinderSize).isEqualTo(expectedVfSize)

                composeTestRule.onNodeWithTag(START_SIDE_RAIL_TAG).assertIsDisplayed()
                composeTestRule.onNodeWithTag(END_SIDE_RAIL_TAG).assertIsDisplayed()

                val startRailBounds = composeTestRule
                    .onNodeWithTag(START_SIDE_RAIL_TAG)
                    .fetchSemanticsNode()
                    .boundsInRoot
                val endRailBounds = composeTestRule
                    .onNodeWithTag(END_SIDE_RAIL_TAG)
                    .fetchSemanticsNode()
                    .boundsInRoot

                assertThat(startRailBounds.left).isLessThan(endRailBounds.left)
                assertThat(startRailBounds.height).isGreaterThan(startRailBounds.width)
                assertThat(endRailBounds.height).isGreaterThan(endRailBounds.width)
            }
        }
    }

    @Test
    fun cutoutAwareRow_remapsCutoutsUnderRotated90AndRotated270() {
        val rotated90RowBounds = IntRect(left = 692, top = 0, right = 740, bottom = 360)
        val rotated90WindowCutout = listOf(AndroidRect(692, 20, 740, 60))

        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = rotated90WindowCutout,
                rowBoundsInWindow = rotated90RowBounds,
                isRtl = false,
                rtlReferenceWidth = 360,
                windowRotation = DeviceRotation.Rotated90,
                fallbackViewWidth = 740,
                fallbackViewHeight = 360
            )
        ).containsExactly(IntRect(left = 20, top = 0, right = 60, bottom = 48))

        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = rotated90WindowCutout,
                rowBoundsInWindow = rotated90RowBounds,
                isRtl = true,
                rtlReferenceWidth = 360,
                windowRotation = DeviceRotation.Rotated90,
                fallbackViewWidth = 740,
                fallbackViewHeight = 360
            )
        ).containsExactly(IntRect(left = 300, top = 0, right = 340, bottom = 48))

        val rotated270RowBounds = IntRect(left = 0, top = 0, right = 48, bottom = 360)
        val rotated270WindowCutout = listOf(AndroidRect(0, 300, 48, 340))

        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = rotated270WindowCutout,
                rowBoundsInWindow = rotated270RowBounds,
                isRtl = false,
                rtlReferenceWidth = 360,
                windowRotation = DeviceRotation.Rotated270,
                fallbackViewWidth = 740,
                fallbackViewHeight = 360
            )
        ).containsExactly(IntRect(left = 20, top = 0, right = 60, bottom = 48))

        assertThat(
            resolveLocalCutoutRects(
                cutoutRectsOverride = null,
                windowCutoutRects = rotated270WindowCutout,
                rowBoundsInWindow = rotated270RowBounds,
                isRtl = true,
                rtlReferenceWidth = 360,
                windowRotation = DeviceRotation.Rotated270,
                fallbackViewWidth = 740,
                fallbackViewHeight = 360
            )
        ).containsExactly(IntRect(left = 300, top = 0, right = 340, bottom = 48))

        var placedSegments = emptyList<IntRect>()
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDensity provides Density(1f),
                LocalWindowRotation provides DeviceRotation.Rotated90
            ) {
                CutoutAwareRow(
                    pillHorizontalPadding = 8.dp,
                    pillMinHeight = 32.dp,
                    cutoutRectsOverride = listOf(IntRect(20, 0, 60, 48)),
                    onPillSegmentsPlaced = { placedSegments = it }
                ) {
                    Box(Modifier.size(24.dp))
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(placedSegments).hasSize(1)
        assertThat(placedSegments.single().left).isAtLeast(68)
    }
}
