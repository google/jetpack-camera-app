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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutMode
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutMode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private object TestTokens {
    const val MULTI_WINDOW_STATUS_BAR_PX = 36
    const val BOUNDS_TOLERANCE_DP = 0.5f

    val CaptureButtonSize = 80.dp
    val SecondaryControlSize = 48.dp
    val ZoomControlWidth = 160.dp
    val IndicatorWidth = 120.dp
    val IndicatorHeight = 36.dp
    val ModeToggleWidth = 96.dp
}

/**
 * Robolectric Compose UI tests for [PreviewLayout] adaptive orchestration across device qualifiers,
 * Quick Settings single-tag guarantees, and `movableContentOf` viewfinder preservation across all
 * [CameraLayoutMode] states and hysteresis deadbands.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
class AdaptiveCaptureLayoutTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Composable
    private fun OrchestratedPreviewUnderTest(
        modifier: Modifier = Modifier,
        scaffoldState: BottomSheetScaffoldState = rememberCompactPortraitScaffoldState(),
        slots: CaptureSlots? = null,
        onModeObserved: (CameraLayoutMode) -> Unit = {},
        viewfinderContent: @Composable (Modifier) -> Unit = { vfMod ->
            PlaceholderViewfinder(modifier = vfMod)
        }
    ) {
        MaterialTheme {
            PreviewLayout(
                modifier = modifier,
                scaffoldState = scaffoldState,
                viewfinder = { vfModifier ->
                    onModeObserved(LocalCameraLayoutMode.current)
                    viewfinderContent(vfModifier)
                },
                captureButton = { btnMod ->
                    Box(btnMod.size(TestTokens.CaptureButtonSize).testTag(CAPTURE_BUTTON))
                },
                imageWell = { wellMod ->
                    Box(wellMod.size(TestTokens.SecondaryControlSize).testTag(IMAGE_WELL_TAG))
                },
                flipCameraButton = { flipMod ->
                    Box(flipMod.size(TestTokens.SecondaryControlSize).testTag(FLIP_CAMERA_BUTTON))
                },
                zoomLevelDisplay = { zoomMod ->
                    Box(
                        zoomMod
                            .size(
                                width = TestTokens.ZoomControlWidth,
                                height = TestTokens.SecondaryControlSize
                            )
                            .testTag(ZOOM_BUTTON_ROW_TAG)
                    )
                },
                elapsedTimeDisplay = {},
                quickSettingsButton = { qsMod ->
                    Box(
                        qsMod.size(
                            TestTokens.SecondaryControlSize
                        ).testTag(QUICK_SETTINGS_DROP_DOWN)
                    )
                },
                indicatorRow = { indMod ->
                    Box(
                        indMod
                            .size(
                                width = TestTokens.IndicatorWidth,
                                height = TestTokens.IndicatorHeight
                            )
                            .testTag(VIDEO_QUALITY_TAG)
                    )
                },
                captureModeToggle = { modeMod ->
                    Box(
                        modeMod
                            .size(
                                width = TestTokens.ModeToggleWidth,
                                height = TestTokens.SecondaryControlSize
                            )
                            .testTag(CAPTURE_MODE_TOGGLE_BUTTON)
                    )
                },
                quickSettingsOverlay = { qsOverlayMod ->
                    Box(
                        qsOverlayMod
                            .size(TestTokens.SecondaryControlSize)
                            .testTag(QUICK_SETTINGS_BOTTOM_SHEET)
                    )
                },
                debugOverlay = {},
                debugVisibilityWrapper = { content -> content() },
                screenFlashOverlay = {},
                snackBar = { _, _ -> },
                slots = slots
            )
        }
    }

    private fun assertCompactLayoutTopology(
        observedMode: CameraLayoutMode?,
        expectedMode: CameraLayoutMode = CameraLayoutMode.COMPACT_PORTRAIT
    ) {
        assertThat(observedMode).isEqualTo(expectedMode)
        composeTestRule.onNodeWithTag(COMPACT_CONTROLS_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(START_SIDE_RAIL_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(END_SIDE_RAIL_TAG).assertDoesNotExist()
    }

    private fun assertSideRailTopology(
        observedMode: CameraLayoutMode?,
        expectedMode: CameraLayoutMode
    ) {
        assertThat(observedMode).isEqualTo(expectedMode)
        composeTestRule.onNodeWithTag(START_SIDE_RAIL_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(END_SIDE_RAIL_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(COMPACT_CONTROLS_TAG).assertDoesNotExist()
    }

    @Config(qualifiers = "w360dp-h740dp-normal-long-port-mdpi")
    @Test
    fun standardPhonePortrait_w360dp_h740dp_rendersCompactPortraitWithoutSideRails() {
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_PORTRAIT)
    }

    @Config(qualifiers = "sw360dp-w740dp-h360dp-normal-long-land-mdpi")
    @Test
    fun standardPhoneLandscapeFullScreen_sw360dp_w740dp_h360dp_resolvesToCompactHorizontal() {
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_HORIZONTAL)
    }

    @Config(qualifiers = "sw320dp-w320dp-h640dp-port-mdpi")
    @Test
    fun slimDisplay_sw320dp_w320dp_h640dp_resolvesToCompactSlimUsingCompactLayout() {
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_SLIM)
    }

    @Config(qualifiers = "sw800dp-w480dp-h800dp-port-mdpi")
    @Test
    fun tabletNarrowSplitScreen_sw800dp_w480dp_h800dp_resolvesToCompactPortrait() {
        var observedMode: CameraLayoutMode? = null
        lateinit var hostView: View

        composeTestRule.setContent {
            hostView = LocalView.current
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_PORTRAIT)

        val statusBarInsets = Insets.of(0, TestTokens.MULTI_WINDOW_STATUS_BAR_PX, 0, 0)
        val insets = WindowInsetsCompat.Builder()
            .setInsets(WindowInsetsCompat.Type.statusBars(), statusBarInsets)
            .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars(), statusBarInsets)
            .setVisible(WindowInsetsCompat.Type.statusBars(), true)
            .build()
        composeTestRule.runOnUiThread {
            ViewCompat.dispatchApplyWindowInsets(hostView, insets)
        }
        composeTestRule.waitForIdle()

        val indicatorBounds =
            composeTestRule.onNodeWithTag(VIDEO_QUALITY_TAG).getUnclippedBoundsInRoot()
        assertThat(indicatorBounds.top.value)
            .isAtLeast(
                TestTokens.MULTI_WINDOW_STATUS_BAR_PX.toFloat() - TestTokens.BOUNDS_TOLERANCE_DP
            )
    }

    @Config(qualifiers = "sw673dp-w852dp-h883dp-mdpi")
    @Test
    fun unfoldedFoldableInnerDisplay_portrait_resolvesToTwoSideRail() {
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertSideRailTopology(observedMode, CameraLayoutMode.TWO_SIDE_RAIL)
    }

    @Config(qualifiers = "sw673dp-w883dp-h852dp-land-mdpi")
    @Test
    fun unfoldedFoldableInnerDisplay_landscape_resolvesToTwoSideRail() {
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertSideRailTopology(observedMode, CameraLayoutMode.TWO_SIDE_RAIL)
    }

    @Config(qualifiers = "sw800dp-w1280dp-h800dp-land-mdpi")
    @Test
    fun tabletLandscape_resolvesToTwoSideRail() {
        var observedMode: CameraLayoutMode? = null
        val isQuickSettingsExpanded = mutableStateOf(false)

        composeTestRule.setContent {
            val scaffoldState = if (isQuickSettingsExpanded.value) {
                rememberCompactPortraitScaffoldState(initialValue = SheetValue.Expanded)
            } else {
                rememberCompactPortraitScaffoldState(initialValue = SheetValue.Hidden)
            }
            OrchestratedPreviewUnderTest(
                scaffoldState = scaffoldState,
                onModeObserved = { observedMode = it }
            )
        }
        composeTestRule.waitForIdle()

        assertSideRailTopology(observedMode, CameraLayoutMode.TWO_SIDE_RAIL)
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()

        isQuickSettingsExpanded.value = true
        composeTestRule.waitForIdle()
        assertThat(
            composeTestRule.onAllNodesWithTag(QUICK_SETTINGS_BOTTOM_SHEET).fetchSemanticsNodes()
        ).hasSize(1)
    }

    @Config(qualifiers = "sw800dp-w800dp-h1280dp-port-mdpi")
    @Test
    fun tabletPortrait_resolvesToTwoSideRail() {
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertSideRailTopology(observedMode, CameraLayoutMode.TWO_SIDE_RAIL)
    }

    @Config(qualifiers = "sw600dp-w800dp-h400dp-land-mdpi")
    @Test
    fun horizontalSplitScreen_sw600dp_w800dp_h400dp_resolvesToCompactHorizontal() {
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_HORIZONTAL)
    }

    @Config(qualifiers = "sw360dp-w360dp-h360dp-mdpi")
    @Test
    fun clamshellCoverOrPhoneSplit_sw360dp_w360dp_h360dp_resolvesToCompactShort() {
        var observedMode: CameraLayoutMode? = null
        val isQuickSettingsExpanded = mutableStateOf(false)

        composeTestRule.setContent {
            val scaffoldState = if (isQuickSettingsExpanded.value) {
                rememberCompactPortraitScaffoldState(initialValue = SheetValue.Expanded)
            } else {
                rememberCompactPortraitScaffoldState(initialValue = SheetValue.Hidden)
            }
            OrchestratedPreviewUnderTest(
                scaffoldState = scaffoldState,
                onModeObserved = { observedMode = it }
            )
        }
        composeTestRule.waitForIdle()

        assertSideRailTopology(observedMode, CameraLayoutMode.COMPACT_SHORT)
        composeTestRule.onNodeWithTag(QUICK_SETTINGS_BOTTOM_SHEET).assertDoesNotExist()

        isQuickSettingsExpanded.value = true
        composeTestRule.waitForIdle()
        assertThat(
            composeTestRule.onAllNodesWithTag(QUICK_SETTINGS_BOTTOM_SHEET).fetchSemanticsNodes()
        ).hasSize(1)
    }

    @Config(qualifiers = "sw320dp-w320dp-h340dp-mdpi")
    @Test
    fun tinyCoverOrSplit_sw320dp_w320dp_h340dp_resolvesToCompactShortSlim() {
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            OrchestratedPreviewUnderTest(onModeObserved = { observedMode = it })
        }
        composeTestRule.waitForIdle()

        assertSideRailTopology(observedMode, CameraLayoutMode.COMPACT_SHORT_SLIM)
    }

    @Config(qualifiers = "sw800dp-w1280dp-h1280dp-mdpi")
    @Test
    fun dynamicDividerResize_acrossAllSixModes_preservesViewfinderWithMovableContentOf() {
        val windowSizeState = mutableStateOf(DpSize(400.dp, 800.dp))
        var composeCount = 0
        var disposeCount = 0
        var observedMode: CameraLayoutMode? = null

        composeTestRule.setContent {
            Box(modifier = Modifier.size(windowSizeState.value)) {
                OrchestratedPreviewUnderTest(
                    slots = PlaceholderCaptureSlots.sideRail(includeElapsedTime = true),
                    onModeObserved = { observedMode = it },
                    viewfinderContent = { vfModifier ->
                        DisposableEffect(Unit) {
                            composeCount++
                            onDispose { disposeCount++ }
                        }
                        Box(
                            modifier = vfModifier
                                .fillMaxSize()
                                .testTag(PREVIEW_DISPLAY)
                        )
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Step 1: 400x800dp -> COMPACT_PORTRAIT.
        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        // Step 2: 604x800dp -> inside [593dp, 607dp] width deadband -> stays COMPACT_PORTRAIT.
        windowSizeState.value = DpSize(604.dp, 800.dp)
        composeTestRule.waitForIdle()
        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        // Step 3: 852x883dp -> crosses width deadband -> TWO_SIDE_RAIL.
        windowSizeState.value = DpSize(852.dp, 883.dp)
        composeTestRule.waitForIdle()
        assertSideRailTopology(observedMode, CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        // Step 4: 852x476dp -> inside [473dp, 487dp] height deadband -> stays TWO_SIDE_RAIL.
        windowSizeState.value = DpSize(852.dp, 476.dp)
        composeTestRule.waitForIdle()
        assertSideRailTopology(observedMode, CameraLayoutMode.TWO_SIDE_RAIL)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        // Step 5: 800x400dp -> crosses height deadband -> COMPACT_HORIZONTAL.
        windowSizeState.value = DpSize(800.dp, 400.dp)
        composeTestRule.waitForIdle()
        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_HORIZONTAL)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        // Step 6: 400x400dp -> crosses 480dp width deadband -> COMPACT_SHORT.
        windowSizeState.value = DpSize(400.dp, 400.dp)
        composeTestRule.waitForIdle()
        assertSideRailTopology(observedMode, CameraLayoutMode.COMPACT_SHORT)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        // Step 7: 320x340dp -> crosses 360dp slim deadband -> COMPACT_SHORT_SLIM.
        windowSizeState.value = DpSize(320.dp, 340.dp)
        composeTestRule.waitForIdle()
        assertSideRailTopology(observedMode, CameraLayoutMode.COMPACT_SHORT_SLIM)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        // Step 8: 320x640dp -> crosses 480dp height deadband while slim -> COMPACT_SLIM.
        windowSizeState.value = DpSize(320.dp, 640.dp)
        composeTestRule.waitForIdle()
        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_SLIM)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)

        // Step 9: 400x800dp -> returns to COMPACT_PORTRAIT without ever disposing viewfinder.
        windowSizeState.value = DpSize(400.dp, 800.dp)
        composeTestRule.waitForIdle()
        assertCompactLayoutTopology(observedMode, CameraLayoutMode.COMPACT_PORTRAIT)
        assertThat(composeCount).isEqualTo(1)
        assertThat(disposeCount).isEqualTo(0)
    }
}
