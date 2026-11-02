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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
class PreviewDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    @Config(qualifiers = "w360dp-h800dp")
    fun previewDisplay_portraitWindow_usesPortraitAspectRatio() {
        var aspectRatio by mutableStateOf(AspectRatio.THREE_FOUR)
        setPreviewContent { aspectRatio }

        var bounds = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(bounds.width.value).isWithin(0.5f).of(360f)
        assertThat(bounds.height.value).isWithin(0.5f).of(480f)

        composeTestRule.runOnIdle { aspectRatio = AspectRatio.NINE_SIXTEEN }
        composeTestRule.waitForIdle()
        bounds = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(bounds.width.value).isWithin(0.5f).of(360f)
        assertThat(bounds.height.value).isWithin(0.5f).of(640f)

        composeTestRule.runOnIdle { aspectRatio = AspectRatio.ONE_ONE }
        composeTestRule.waitForIdle()
        bounds = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(bounds.width.value).isWithin(0.5f).of(360f)
        assertThat(bounds.height.value).isWithin(0.5f).of(360f)
    }

    @Test
    @Config(qualifiers = "w800dp-h360dp-land")
    fun previewDisplay_landscapeWindow_usesLandscapeAspectRatio() {
        var aspectRatio by mutableStateOf(AspectRatio.THREE_FOUR)
        setPreviewContent { aspectRatio }

        var bounds = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(bounds.width.value).isWithin(0.5f).of(480f)
        assertThat(bounds.height.value).isWithin(0.5f).of(360f)

        composeTestRule.runOnIdle { aspectRatio = AspectRatio.NINE_SIXTEEN }
        composeTestRule.waitForIdle()
        bounds = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(bounds.width.value).isWithin(0.5f).of(640f)
        assertThat(bounds.height.value).isWithin(0.5f).of(360f)

        composeTestRule.runOnIdle { aspectRatio = AspectRatio.ONE_ONE }
        composeTestRule.waitForIdle()
        bounds = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(bounds.width.value).isWithin(0.5f).of(360f)
        assertThat(bounds.height.value).isWithin(0.5f).of(360f)
    }

    private fun setPreviewContent(selectedAspectRatioProvider: () -> AspectRatio) {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalDisableAnimations provides true,
                LocalOverlapTargetBounds provides remember { mutableStateOf(Rect.Zero) }
            ) {
                val selected = selectedAspectRatioProvider()
                PreviewDisplay(
                    previewDisplayUiState = PreviewDisplayUiState(
                        aspectRatioUiState = AspectRatioUiState.Available(
                            selectedAspectRatio = selected,
                            availableAspectRatios = listOf(
                                SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                                SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
                                SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
                            )
                        )
                    ),
                    onTapToFocus = { _, _ -> },
                    onFlipCamera = {},
                    onScaleZoom = {},
                    onRequestWindowColorMode = {},
                    surfaceRequest = null,
                    focusMeteringUiState = FocusMeteringUiState.Unspecified
                )
            }
        }
        composeTestRule.waitForIdle()
    }
}
