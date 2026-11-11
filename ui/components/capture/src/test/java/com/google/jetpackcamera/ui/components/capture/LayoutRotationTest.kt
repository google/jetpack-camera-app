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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutSolution
import com.google.jetpackcamera.ui.components.capture.layout.CameraRowIds
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private object LayoutRotationTestTokens {
    const val INDICATOR_TAG = "RotationTestIndicatorRow"

    val CaptureButtonSize = 80.dp
    val ControlSize = 48.dp
    val ZoomBarWidth = 160.dp
    val IndicatorWidth = 120.dp
    val IndicatorHeight = 36.dp
    val ModeToggleWidth = 96.dp
}

/**
 * Robolectric Compose UI tests verifying layout-level counter-rotation scoping across full-screen
 * compact handhelds, portrait & horizontal split-screen panes (`550x500dp` and `800x400dp`),
 * 7-mode side-rail postures (`FOLDABLE`, `TABLET_PORTRAIT`, `TABLET_LANDSCAPE`, `COMPACT_SHORT`),
 * and [CutoutAwareRow] cutout remapping.
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
            effectiveWindowRotation: DeviceRotation,
            gravityRotationDegrees: Float,
            solution: CameraLayoutSolution?,
            viewfinderSize: IntSize
        ) -> Unit = { _, _, _, _, _ -> }
    ) {
        MaterialTheme {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                CaptureScaffoldLayout(
                    modifier = modifier,
                    deviceRotation = deviceRotation,
                    windowRotation = windowRotation,
                    slots = slots,
                    viewfinder = { vfModifier ->
                        val mode = LocalCameraLayoutMode.current
                        val effectiveWinRot = LocalWindowRotation.current
                        val gravityDeg = LocalGravityRotationState.current.value
                        val solution = LocalCameraLayoutSolution.current
                        Box(
                            modifier = vfModifier
                                .fillMaxSize()
                                .onSizeChanged { vfSize ->
                                    onViewfinderObserved(
                                        mode,
                                        effectiveWinRot,
                                        gravityDeg,
                                        solution,
                                        vfSize
                                    )
                                }
                                .testTag(PREVIEW_DISPLAY)
                        )
                    },
                    captureButton = { mod ->
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
    fun compactPortraitLayout_fullScreenLandscapePhone_counterRotatesToPortrait() {
        var windowRotation by mutableStateOf(DeviceRotation.Rotated90)
        var observedMode: CameraLayoutMode? = null
        var observedEffectiveWinRot: DeviceRotation? = null
        var observedGravityDeg: Float? = null
        var observedSolution: CameraLayoutSolution? = null
        var observedViewfinderSize = IntSize.Zero

        composeTestRule.setContent {
            RotationLayoutUnderTest(
                deviceRotation = windowRotation,
                windowRotation = windowRotation,
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
        assertThat(observedEffectiveWinRot).isEqualTo(DeviceRotation.Rotated90)
        assertThat(observedSolution).isNotNull()
        assertThat(observedViewfinderSize).isEqualTo(IntSize(360, 740))
        assertThat(((observedGravityDeg!! % 360f) + 360f) % 360f).isEqualTo(270f)

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
        assertThat(observedEffectiveWinRot).isEqualTo(DeviceRotation.Rotated270)
        assertThat(observedSolution).isNotNull()
        assertThat(observedViewfinderSize).isEqualTo(IntSize(360, 740))
        assertThat(((observedGravityDeg!! % 360f) + 360f) % 360f).isEqualTo(90f)

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
    fun compactHorizontalPanes_800x400And550x500_rotateCompactLayout90DegreesToFillSpace() {
        var paneSize by mutableStateOf(DpSize(800.dp, 400.dp))
        var observedMode: CameraLayoutMode? = null
        var observedEffectiveWinRot: DeviceRotation? = null
        var observedGravityDeg: Float? = null
        var observedViewfinderSize = IntSize.Zero

        composeTestRule.setContent {
            RotationLayoutUnderTest(
                modifier = Modifier.size(paneSize),
                deviceRotation = DeviceRotation.Natural,
                windowRotation = DeviceRotation.Natural,
                onViewfinderObserved = { mode, effWinRot, gravDeg, _, vfSize ->
                    observedMode = mode
                    observedEffectiveWinRot = effWinRot
                    observedGravityDeg = gravDeg
                    observedViewfinderSize = vfSize
                }
            )
        }
        composeTestRule.waitForIdle()

        assertThat(observedMode).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(observedEffectiveWinRot).isEqualTo(DeviceRotation.Rotated270)
        assertThat(observedViewfinderSize).isEqualTo(IntSize(400, 800))
        assertThat(((observedGravityDeg!! % 360f) + 360f) % 360f).isEqualTo(90f)

        // 550x500dp (W > H with H >= 480dp) also resolves to COMPACT and counter-rotates cleanly!
        paneSize = DpSize(550.dp, 500.dp)
        composeTestRule.waitForIdle()
        assertThat(observedMode).isEqualTo(CameraLayoutMode.COMPACT)
        assertThat(observedEffectiveWinRot).isEqualTo(DeviceRotation.Rotated270)
        assertThat(((observedGravityDeg!! % 360f) + 360f) % 360f).isEqualTo(90f)
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
