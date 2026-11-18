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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val STATUS_BAR_HEIGHT_PX = 100
private const val NAVIGATION_BAR_HEIGHT_PX = 60
private const val CONTROL_TAG = "VisibilityDependentControl"

/**
 * Verifies that the capture layout does not move when the status bar is hidden.
 *
 * The capture surfaces hide the status bar, and the regular inset APIs collapse to zero while a bar
 * is hidden, so any layout that uses them shifts on every hide/show. These tests dispatch synthetic
 * insets with the status bar marked visible and then hidden, and assert the controls stay put.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class CaptureLayoutInsetsTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Guards the other test in this class from passing vacuously: a control that uses the regular,
     * visibility-dependent inset API *must* move when the status bar is hidden. If this fails, the
     * test environment is not delivering insets into Compose and the assertions below prove
     * nothing.
     */
    @Test
    fun visibilityDependentPadding_moves_whenStatusBarHidden() {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            Box(Modifier.fillMaxSize()) {
                Box(
                    Modifier
                        .statusBarsPadding()
                        .size(48.dp)
                        .testTag(CONTROL_TAG)
                )
            }
        }

        val withStatusBar = boundsOf(CONTROL_TAG, view, statusBarVisible = true)
        val withoutStatusBar = boundsOf(CONTROL_TAG, view, statusBarVisible = false)

        assertThat(withoutStatusBar.top).isLessThan(withStatusBar.top)
    }

    /**
     * The capture controls are bottom-anchored, so this exercises both bars: the bottom inset is
     * what actually positions them.
     */
    @Test
    fun captureButton_doesNotMove_whenSystemBarsHidden() {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            TestPreviewLayout()
        }

        val withBars = boundsOf(
            CAPTURE_BUTTON,
            view,
            statusBarVisible = true,
            navigationBarVisible = true
        )
        val withoutBars = boundsOf(
            CAPTURE_BUTTON,
            view,
            statusBarVisible = false,
            navigationBarVisible = false
        )

        assertThat(withoutBars).isEqualTo(withBars)
    }

    @Test
    fun indicatorRow_doesNotMove_whenStatusBarHidden() {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            TestPreviewLayout()
        }

        val withStatusBar = boundsOf(INDICATOR_ROW_TAG, view, statusBarVisible = true)
        val withoutStatusBar = boundsOf(INDICATOR_ROW_TAG, view, statusBarVisible = false)

        assertThat(withoutStatusBar).isEqualTo(withStatusBar)
    }

    @Test
    fun indicatorRow_occupiesStatusBarSpace_whenNoDisplayCutout() {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            TestPreviewLayout()
        }

        // Even though statusBarsIgnoringVisibility is 100dp (STATUS_BAR_HEIGHT_PX = 100 at 1x
        // density), when displayCutout is 0dp the top bar height is minTouchTarget (48dp), so the
        // 40dp indicatorRow is vertically centered in [0dp, 48dp] -> [4dp, 44dp].
        val bounds = boundsOf(INDICATOR_ROW_TAG, view, statusBarVisible = false)
        assertThat(bounds.top).isEqualTo(4.dp)
        assertThat(bounds.bottom).isEqualTo(44.dp)
    }

    /** The screen flash must light the whole window, not just the area inside the system bars. */
    @Test
    fun screenFlashOverlay_coversSystemBarAreas() {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            TestPreviewLayout(
                screenFlashOverlay = {
                    Box(it.fillMaxSize().testTag(SCREEN_FLASH_OVERLAY_TAG))
                }
            )
        }

        val overlayBounds = boundsOf(SCREEN_FLASH_OVERLAY_TAG, view, statusBarVisible = false)
        val rootBounds = composeTestRule.onRoot().getUnclippedBoundsInRoot()

        assertThat(overlayBounds).isEqualTo(rootBounds)
    }

    @Test
    fun topBarLeadingContent_touchesIndicatorRowWithZeroGap() {
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            TestPreviewLayout(
                topBarLeadingContent = {
                    Box(it.size(48.dp).testTag(LEADING_BUTTON_TAG))
                }
            )
        }

        val leadingBounds = boundsOf(LEADING_BUTTON_TAG, view, statusBarVisible = false)
        val indicatorBounds = boundsOf(INDICATOR_ROW_TAG, view, statusBarVisible = false)

        assertThat(leadingBounds.left).isEqualTo(16.dp)
        assertThat(leadingBounds.right).isEqualTo(64.dp)
        assertThat(indicatorBounds.left).isEqualTo(leadingBounds.right)
    }

    @Test
    fun topBarLeadingContent_shiftsPastLeftCornerCutoutAlongsideIndicatorRow() {
        // Left-corner cutout occupying local [0, 40] with 8dp clearance -> keep-out [0, 48].
        composeTestRule.setContent {
            TopBarLeadingAndIndicatorsRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                topBarLeadingContent = {
                    Box(it.size(48.dp).testTag(LEADING_BUTTON_TAG))
                },
                indicatorRow = {
                    Box(it.size(40.dp).testTag(INDICATOR_ROW_TAG))
                },
                cutoutRectsOverride = listOf(IntRect(0, 0, 40, 48))
            )
        }
        composeTestRule.waitForIdle()

        val leadingBounds = composeTestRule.onNodeWithTag(LEADING_BUTTON_TAG)
            .getUnclippedBoundsInRoot()
        val indicatorBounds = composeTestRule.onNodeWithTag(INDICATOR_ROW_TAG)
            .getUnclippedBoundsInRoot()

        assertThat(leadingBounds.left).isEqualTo(48.dp)
        assertThat(leadingBounds.right).isEqualTo(96.dp)
        assertThat(indicatorBounds.left).isEqualTo(leadingBounds.right)
    }

    @Test
    fun topBarLeadingContent_receivesPreviewOverlaySlotScopeOverlapState() {
        var leadingOverViewfinder: Boolean? = null
        composeTestRule.setContent {
            CaptureScaffoldLayout(
                viewfinder = { vfModifier ->
                    val overlapTargetBounds = LocalOverlapTargetBounds.current
                    Box(
                        vfModifier
                            .fillMaxSize()
                            .onGloballyPositioned { coordinates ->
                                overlapTargetBounds.value = coordinates.boundsInWindow()
                            }
                    )
                },
                topBarLeadingContent = { leadingModifier ->
                    leadingOverViewfinder = isOverViewfinder
                    Box(leadingModifier.size(48.dp).testTag(LEADING_BUTTON_TAG))
                },
                indicatorRow = {
                    Box(it.size(40.dp).testTag(INDICATOR_ROW_TAG))
                }
            )
        }
        composeTestRule.waitForIdle()

        assertThat(leadingOverViewfinder).isTrue()
    }

    private fun boundsOf(
        tag: String,
        view: View,
        statusBarVisible: Boolean,
        navigationBarVisible: Boolean = true
    ): DpRect {
        dispatchInsets(view, statusBarVisible, navigationBarVisible)
        composeTestRule.waitForIdle()
        return composeTestRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
    }

    /**
     * Dispatches system bar insets, mimicking the real platform behavior where a hidden bar reports
     * zero for its regular insets but keeps reporting its size for the "ignoring visibility" query.
     */
    private fun dispatchInsets(
        view: View,
        statusBarVisible: Boolean,
        navigationBarVisible: Boolean
    ) {
        val statusBarInsets = Insets.of(0, STATUS_BAR_HEIGHT_PX, 0, 0)
        val navigationBarInsets = Insets.of(0, 0, 0, NAVIGATION_BAR_HEIGHT_PX)
        val insets = WindowInsetsCompat.Builder()
            .setInsets(
                WindowInsetsCompat.Type.statusBars(),
                if (statusBarVisible) statusBarInsets else Insets.NONE
            )
            .setInsetsIgnoringVisibility(WindowInsetsCompat.Type.statusBars(), statusBarInsets)
            .setVisible(WindowInsetsCompat.Type.statusBars(), statusBarVisible)
            .setInsets(
                WindowInsetsCompat.Type.navigationBars(),
                if (navigationBarVisible) navigationBarInsets else Insets.NONE
            )
            .setInsetsIgnoringVisibility(
                WindowInsetsCompat.Type.navigationBars(),
                navigationBarInsets
            )
            .setVisible(WindowInsetsCompat.Type.navigationBars(), navigationBarVisible)
            .build()

        composeTestRule.runOnUiThread {
            ViewCompat.dispatchApplyWindowInsets(view, insets)
        }
    }
}

private const val INDICATOR_ROW_TAG = "TestIndicatorRow"
private const val LEADING_BUTTON_TAG = "TestTopBarLeadingButton"
private const val SCREEN_FLASH_OVERLAY_TAG = "TestScreenFlashOverlay"

@Composable
private fun TestPreviewLayout(
    topBarLeadingContent: (@Composable PreviewOverlaySlotScope.(Modifier) -> Unit)? = null,
    screenFlashOverlay: @Composable (Modifier) -> Unit = {
        Box(it.size(0.dp))
    }
) {
    CaptureScaffoldLayout(
        viewfinder = { Box(it.fillMaxSize()) },
        captureButton = { Box(it.size(80.dp).testTag(CAPTURE_BUTTON)) },
        imageWell = { Box(it.size(40.dp)) },
        flipCameraButton = { Box(it.size(40.dp)) },
        zoomLevelDisplay = { Box(it.size(40.dp)) },
        elapsedTimeDisplay = { Box(it.size(40.dp)) },
        quickSettingsButton = { Box(it.size(40.dp)) },
        indicatorRow = { Box(it.size(40.dp).testTag(INDICATOR_ROW_TAG)) },
        topBarLeadingContent = topBarLeadingContent,
        captureModeToggle = { Box(it.size(40.dp)) },
        quickSettingsOverlay = { Box(it.size(40.dp)) },
        screenFlashOverlay = screenFlashOverlay
    )
}
