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
package com.google.jetpackcamera.feature.preview

import android.util.Range
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LensToZoom
import com.google.jetpackcamera.ui.components.capture.LocalDisableAnimations
import com.google.jetpackcamera.ui.components.capture.PREVIEW_DISPLAY
import com.google.jetpackcamera.ui.controller.testing.FakeZoomController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests that pinch-to-zoom in [ContentScreen] uses the zoom range reported by the camera for the
 * selected lens, including when the zoom state is temporarily unavailable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w411dp-h891dp")
class ContentScreenZoomTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val primaryZoomRatios = mutableListOf<Float>()
    private val zoomController = FakeZoomController(
        setZoomRatioAction = { zoomRatio ->
            if (zoomRatio.changeType.lensToZoom == LensToZoom.PRIMARY) {
                primaryZoomRatios += zoomRatio.changeType.value
            }
        }
    )

    private var captureUiState: CaptureUiState.Ready by mutableStateOf(
        LOADING_CAPTURE_UI_STATE_SKELETON
    )

    @Test
    fun pinchZoom_afterLoadingSkeletonReplacedOnSameLens_isNotClampedToOne() {
        setContent()

        // The camera finishes loading on the same (back) lens shown by the loading skeleton.
        updateCaptureUiState(
            readyState(lensFacing = LensFacing.BACK, zoomRange = Range(1f, 10f))
        )
        pinchOut()

        assertThat(primaryZoomRatios.last()).isGreaterThan(1f)
    }

    @Test
    fun pinchZoom_afterLensFlipThroughUnavailableZoom_usesNewLensZoomRange() {
        captureUiState = readyState(lensFacing = LensFacing.BACK, zoomRange = Range(1f, 10f))
        setContent()

        // The lens flip reports the new lens before its zoom range is available.
        updateCaptureUiState(
            readyState(lensFacing = LensFacing.FRONT, zoomRange = null)
        )
        updateCaptureUiState(
            readyState(lensFacing = LensFacing.FRONT, zoomRange = Range(1f, FRONT_MAX_ZOOM))
        )
        pinchOut()

        // The pinch exceeds the front lens's range, so the zoom is clamped to its maximum.
        assertThat(primaryZoomRatios.last()).isEqualTo(FRONT_MAX_ZOOM)
    }

    private fun setContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                ContentScreen(
                    captureUiStateProvider = { captureUiState },
                    surfaceRequest = null,
                    zoomController = zoomController
                )
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun updateCaptureUiState(newState: CaptureUiState.Ready) {
        composeTestRule.runOnIdle { captureUiState = newState }
        composeTestRule.waitForIdle()
    }

    /** Pinches outward on the viewfinder, spreading the fingers to 8x their starting distance. */
    private fun pinchOut() {
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).performTouchInput {
            pinch(
                start0 = center - Offset(PINCH_START_HALF_SPAN_PX, 0f),
                end0 = center - Offset(PINCH_END_HALF_SPAN_PX, 0f),
                start1 = center + Offset(PINCH_START_HALF_SPAN_PX, 0f),
                end1 = center + Offset(PINCH_END_HALF_SPAN_PX, 0f)
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun readyState(lensFacing: LensFacing, zoomRange: Range<Float>?) =
        LOADING_CAPTURE_UI_STATE_SKELETON.copy(
            flipLensUiState = FlipLensUiState.Available(
                selectedLensFacing = lensFacing,
                availableLensFacings = listOf(
                    SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                    SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
                )
            ),
            zoomUiState = zoomRange?.let { ZoomUiState.Enabled(primaryZoomRange = it) }
                ?: ZoomUiState.Unavailable,
            zoomControlUiState = zoomRange?.let {
                ZoomControlUiState.Enabled(
                    zoomLevels = listOf(1f),
                    primaryLensFacing = lensFacing,
                    initialZoomRatio = 1f
                )
            } ?: ZoomControlUiState.Unavailable
        )

    private companion object {
        const val FRONT_MAX_ZOOM = 2f
        const val PINCH_START_HALF_SPAN_PX = 25f
        const val PINCH_END_HALF_SPAN_PX = 200f
    }
}
