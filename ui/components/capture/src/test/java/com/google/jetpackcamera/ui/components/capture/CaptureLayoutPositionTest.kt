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
import com.google.common.truth.Truth.assertThat
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

    private companion object {
        const val CAPTURE_BUTTON_TAG = "test_capture_button"
        const val ZOOM_BAR_TAG = "test_zoom_bar"
        val CAPTURE_BUTTON_SIZE = 80.dp
        val ZOOM_BAR_HEIGHT = 48.dp
        val CAROUSEL_HEIGHT = 32.dp
        val TALL_SCREEN_HEIGHT = 800.dp
        val SHORT_SCREEN_HEIGHT = 500.dp

        /**
         * Dynamic linear spacing, measured down from the top of the window:
         * In this test environment without insets (360x800dp), the 3:4 viewfinder ends at
         * 48dp (top bar) + 480dp (3:4 height) = 528dp.
         * The linear fraction distributes available space below the 3:4 preview so the top of
         * the 86dp shutter row aligns to the viewfinder boundary (528dp), placing the centered
         * 80dp capture button at 530dp.
         */
        const val EXPECTED_TALL_BUTTON_TOP = 530f

        /**
         * Compressed spacing: 12 + 64 + 12 + 32 + 12 + 86 puts the top of the shutter row at
         * 500 - 218 = 282dp, plus the same 3dp of centring.
         */
        const val EXPECTED_SHORT_BUTTON_TOP = 285f

        const val TOLERANCE = 0.5f
    }
}
