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

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.Rect as AndroidRect
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.CameraInternal
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.click
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pinch
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import java.lang.reflect.Proxy
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit and Robolectric Compose UI tests for [calculateViewfinderSize] and [PreviewDisplay].
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1400dp-h1400dp")
class PreviewDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val createdSurfaceRequests = mutableListOf<SurfaceRequest>()

    @SuppressLint("RestrictedApi")
    @After
    fun tearDown() {
        createdSurfaceRequests.forEach { request ->
            request.willNotProvideSurface()
            request.deferrableSurface.close()
        }
        createdSurfaceRequests.clear()
    }

    @Test
    fun calculateViewfinderSize_phonePortrait_usesPortraitAspectRatios() {
        val threeFour = calculateViewfinderSize(
            maxWidth = 412.dp,
            maxHeight = 915.dp,
            aspectRatio = AspectRatio.THREE_FOUR,
            useLandscapeAspectRatio = false
        )
        assertThat(threeFour.width.value).isWithin(0.01f).of(412f)
        assertThat(threeFour.height.value).isWithin(0.01f).of(412f * 4f / 3f)

        val nineSixteen = calculateViewfinderSize(
            maxWidth = 412.dp,
            maxHeight = 915.dp,
            aspectRatio = AspectRatio.NINE_SIXTEEN,
            useLandscapeAspectRatio = false
        )
        assertThat(nineSixteen.width.value).isWithin(0.01f).of(412f)
        assertThat(nineSixteen.height.value).isWithin(0.01f).of(412f * 16f / 9f)

        val oneOne = calculateViewfinderSize(
            maxWidth = 412.dp,
            maxHeight = 915.dp,
            aspectRatio = AspectRatio.ONE_ONE,
            useLandscapeAspectRatio = false
        )
        assertThat(oneOne).isEqualTo(DpSize(412.dp, 412.dp))
    }

    @Test
    fun calculateViewfinderSize_phoneRotated_withoutLandscapeFlag_keepsPortraitRatio() {
        val rotatedThreeFour = calculateViewfinderSize(
            maxWidth = 915.dp,
            maxHeight = 412.dp,
            aspectRatio = AspectRatio.THREE_FOUR,
            useLandscapeAspectRatio = false
        )
        assertThat(rotatedThreeFour.width.value).isWithin(0.01f).of(309f)
        assertThat(rotatedThreeFour.height.value).isWithin(0.01f).of(412f)
    }

    @Test
    fun calculateViewfinderSize_tabletHorizontal_withLandscapeFlag_usesLandscapeRatios() {
        val fourThree = calculateViewfinderSize(
            maxWidth = 1280.dp,
            maxHeight = 800.dp,
            aspectRatio = AspectRatio.THREE_FOUR,
            useLandscapeAspectRatio = true
        )
        assertThat(fourThree.width.value).isWithin(0.01f).of(800f * 4f / 3f)
        assertThat(fourThree.height.value).isWithin(0.01f).of(800f)

        val sixteenNine = calculateViewfinderSize(
            maxWidth = 1280.dp,
            maxHeight = 800.dp,
            aspectRatio = AspectRatio.NINE_SIXTEEN,
            useLandscapeAspectRatio = true
        )
        assertThat(sixteenNine.width.value).isWithin(0.01f).of(1280f)
        assertThat(sixteenNine.height.value).isWithin(0.01f).of(720f)

        val oneOne = calculateViewfinderSize(
            maxWidth = 1280.dp,
            maxHeight = 800.dp,
            aspectRatio = AspectRatio.ONE_ONE,
            useLandscapeAspectRatio = true
        )
        assertThat(oneOne).isEqualTo(DpSize(800.dp, 800.dp))
    }

    @Test
    fun calculateViewfinderSize_tabletVertical_withLandscapeFlag_keepsPortraitRatios() {
        val threeFour = calculateViewfinderSize(
            maxWidth = 800.dp,
            maxHeight = 1280.dp,
            aspectRatio = AspectRatio.THREE_FOUR,
            useLandscapeAspectRatio = true
        )
        assertThat(threeFour.width.value).isWithin(0.01f).of(800f)
        assertThat(threeFour.height.value).isWithin(0.01f).of(800f * 4f / 3f)

        val nineSixteen = calculateViewfinderSize(
            maxWidth = 800.dp,
            maxHeight = 1280.dp,
            aspectRatio = AspectRatio.NINE_SIXTEEN,
            useLandscapeAspectRatio = true
        )
        assertThat(nineSixteen.width.value).isWithin(0.01f).of(720f)
        assertThat(nineSixteen.height.value).isWithin(0.01f).of(1280f)
    }

    @Test
    fun calculateViewfinderSize_foldable_handlesNearSquareBounds() {
        val portraitFoldableThreeFour = calculateViewfinderSize(
            maxWidth = 840.dp,
            maxHeight = 900.dp,
            aspectRatio = AspectRatio.THREE_FOUR,
            useLandscapeAspectRatio = true
        )
        assertThat(portraitFoldableThreeFour.width.value).isWithin(0.01f).of(675f)
        assertThat(portraitFoldableThreeFour.height.value).isWithin(0.01f).of(900f)

        val portraitFoldableOneOne = calculateViewfinderSize(
            maxWidth = 840.dp,
            maxHeight = 900.dp,
            aspectRatio = AspectRatio.ONE_ONE,
            useLandscapeAspectRatio = true
        )
        assertThat(portraitFoldableOneOne).isEqualTo(DpSize(840.dp, 840.dp))

        val landscapeFoldableFourThree = calculateViewfinderSize(
            maxWidth = 900.dp,
            maxHeight = 840.dp,
            aspectRatio = AspectRatio.THREE_FOUR,
            useLandscapeAspectRatio = true
        )
        assertThat(landscapeFoldableFourThree.width.value).isWithin(0.01f).of(900f)
        assertThat(landscapeFoldableFourThree.height.value).isWithin(0.01f).of(675f)
    }

    @Test
    fun calculateViewfinderSize_zeroOrNegativeBounds_returnsZero() {
        assertThat(
            calculateViewfinderSize(
                maxWidth = 0.dp,
                maxHeight = 800.dp,
                aspectRatio = AspectRatio.THREE_FOUR,
                useLandscapeAspectRatio = true
            )
        ).isEqualTo(DpSize.Zero)

        assertThat(
            calculateViewfinderSize(
                maxWidth = 412.dp,
                maxHeight = 0.dp,
                aspectRatio = AspectRatio.THREE_FOUR,
                useLandscapeAspectRatio = false
            )
        ).isEqualTo(DpSize.Zero)

        assertThat(
            calculateViewfinderSize(
                maxWidth = (-1).dp,
                maxHeight = 800.dp,
                aspectRatio = AspectRatio.ONE_ONE,
                useLandscapeAspectRatio = false
            )
        ).isEqualTo(DpSize.Zero)
    }

    @Test
    fun previewDisplay_topCenteredDefault_positionsViewfinderAndUpdatesOverlapTargetBounds() {
        val targetBoundsState = mutableStateOf(Rect.Zero)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalOverlapTargetBounds provides targetBoundsState,
                LocalDisableAnimations provides true
            ) {
                Box(modifier = Modifier.size(width = 300.dp, height = 500.dp)) {
                    PreviewDisplay(
                        previewDisplayUiState = createUiState(AspectRatio.THREE_FOUR),
                        onTapToFocus = { _, _ -> },
                        onFlipCamera = {},
                        onScaleZoom = {},
                        onRequestWindowColorMode = {},
                        surfaceRequest = null,
                        focusMeteringUiState = FocusMeteringUiState.Unspecified
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val bounds = composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(bounds.left).isEqualTo(0.dp)
        assertThat(bounds.top).isEqualTo(0.dp)
        assertThat(bounds.width).isEqualTo(300.dp)
        assertThat(bounds.height).isEqualTo(400.dp)

        assertThat(targetBoundsState.value).isEqualTo(Rect(0f, 0f, 300f, 400f))
    }

    @Test
    fun previewDisplay_centeredLandscapeFraming_updatesBoundsAndOverlapTargetOnRatioChange() {
        val targetBoundsState = mutableStateOf(Rect.Zero)
        var selectedRatio by mutableStateOf(AspectRatio.THREE_FOUR)

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalOverlapTargetBounds provides targetBoundsState,
                LocalDisableAnimations provides true
            ) {
                Box(modifier = Modifier.size(width = 1280.dp, height = 800.dp)) {
                    PreviewDisplay(
                        previewDisplayUiState = createUiState(selectedRatio),
                        onTapToFocus = { _, _ -> },
                        onFlipCamera = {},
                        onScaleZoom = {},
                        onRequestWindowColorMode = {},
                        surfaceRequest = null,
                        focusMeteringUiState = FocusMeteringUiState.Unspecified,
                        contentAlignment = Alignment.Center,
                        useLandscapeAspectRatio = true
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        val pillarboxBounds =
            composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(pillarboxBounds.left.value).isWithin(1f).of(106.5f)
        assertThat(pillarboxBounds.top).isEqualTo(0.dp)
        assertThat(pillarboxBounds.width.value).isWithin(1f).of(1067f)
        assertThat(pillarboxBounds.height).isEqualTo(800.dp)
        assertThat(targetBoundsState.value.left).isWithin(1f).of(106.5f)
        assertThat(targetBoundsState.value.top).isEqualTo(0f)
        assertThat(targetBoundsState.value.width).isWithin(1f).of(1067f)
        assertThat(targetBoundsState.value.height).isEqualTo(800f)

        selectedRatio = AspectRatio.NINE_SIXTEEN
        composeTestRule.waitForIdle()

        val letterboxBounds =
            composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).getUnclippedBoundsInRoot()
        assertThat(letterboxBounds.left).isEqualTo(0.dp)
        assertThat(letterboxBounds.top).isEqualTo(40.dp)
        assertThat(letterboxBounds.width).isEqualTo(1280.dp)
        assertThat(letterboxBounds.height).isEqualTo(720.dp)
        assertThat(targetBoundsState.value).isEqualTo(Rect(0f, 40f, 1280f, 760f))
    }

    @SuppressLint("RestrictedApi")
    @Test
    fun previewDisplay_ltrAndRtl_produceIdenticalTapToFocusCoordinatesAndIndicatorBounds() {
        val surfaceRequest = createFakeSurfaceRequest(width = 600, height = 800)
        var layoutDirection by mutableStateOf(LayoutDirection.Ltr)
        var focusMeteringUiState by
            mutableStateOf<FocusMeteringUiState>(FocusMeteringUiState.Unspecified)
        val recordedTaps = mutableListOf<Offset>()
        var flipCount = 0

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalLayoutDirection provides layoutDirection,
                LocalDisableAnimations provides true
            ) {
                Box(modifier = Modifier.size(width = 300.dp, height = 400.dp)) {
                    PreviewDisplay(
                        previewDisplayUiState = createUiState(AspectRatio.THREE_FOUR),
                        onTapToFocus = { x, y -> recordedTaps.add(Offset(x, y)) },
                        onFlipCamera = { flipCount++ },
                        onScaleZoom = {},
                        onRequestWindowColorMode = {},
                        surfaceRequest = surfaceRequest,
                        focusMeteringUiState = focusMeteringUiState
                    )
                }
            }
        }

        composeTestRule.waitForIdle()

        // Tap at an asymmetric physical point (left quarter of the 300x400 viewfinder) in LTR.
        val physicalTapOffset = Offset(75f, 120f)
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).performTouchInput {
            click(physicalTapOffset)
        }
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()

        assertThat(recordedTaps).hasSize(1)
        val ltrTap = recordedTaps.single()
        assertThat(ltrTap.x).isLessThan(300f)

        focusMeteringUiState = FocusMeteringUiState.Specified(
            surfaceCoordinates = ltrTap,
            status = FocusMeteringUiState.Status.RUNNING
        )
        composeTestRule.waitForIdle()

        val ltrIndicatorBounds =
            composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).getUnclippedBoundsInRoot()
        assertThat(ltrIndicatorBounds.left.value).isLessThan(150f)

        // Switch ambient layout direction to RTL and repeat the identical physical tap.
        recordedTaps.clear()
        focusMeteringUiState = FocusMeteringUiState.Unspecified
        layoutDirection = LayoutDirection.Rtl
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).performTouchInput {
            click(physicalTapOffset)
        }
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()

        assertThat(recordedTaps).hasSize(1)
        val rtlTap = recordedTaps.single()
        assertThat(rtlTap.x).isWithin(0.01f).of(ltrTap.x)
        assertThat(rtlTap.y).isWithin(0.01f).of(ltrTap.y)

        focusMeteringUiState = FocusMeteringUiState.Specified(
            surfaceCoordinates = rtlTap,
            status = FocusMeteringUiState.Status.RUNNING
        )
        composeTestRule.waitForIdle()

        val rtlIndicatorBounds =
            composeTestRule.onNodeWithTag(FOCUS_METERING_INDICATOR_TAG).getUnclippedBoundsInRoot()
        assertThat(rtlIndicatorBounds).isEqualTo(ltrIndicatorBounds)

        // Verify double-tap flip callback still dispatches through rememberUpdatedState.
        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).performTouchInput {
            doubleClick(physicalTapOffset)
        }
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()
        assertThat(flipCount).isEqualTo(1)
    }

    @SuppressLint("RestrictedApi")
    @Test
    fun previewDisplay_updatedCallbacksAndPinchZoom_dispatchLatestHandlers() {
        val surfaceRequest = createFakeSurfaceRequest(width = 600, height = 800)
        val zoomFactors = mutableListOf<Float>()
        var activeGeneration by mutableStateOf(1)
        val flippedGenerations = mutableListOf<Int>()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                Box(modifier = Modifier.size(width = 300.dp, height = 400.dp)) {
                    val capturedGeneration = activeGeneration
                    PreviewDisplay(
                        previewDisplayUiState = createUiState(AspectRatio.THREE_FOUR),
                        onTapToFocus = { _, _ -> },
                        onFlipCamera = { flippedGenerations.add(capturedGeneration) },
                        onScaleZoom = { zoom -> zoomFactors.add(zoom) },
                        onRequestWindowColorMode = {},
                        surfaceRequest = surfaceRequest,
                        focusMeteringUiState = FocusMeteringUiState.Unspecified
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        activeGeneration = 2
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).performTouchInput {
            doubleClick(Offset(150f, 200f))
        }
        composeTestRule.mainClock.advanceTimeBy(500L)
        composeTestRule.waitForIdle()
        assertThat(flippedGenerations).containsExactly(2)

        composeTestRule.onNodeWithTag(PREVIEW_DISPLAY).performTouchInput {
            pinch(
                start0 = Offset(100f, 200f),
                end0 = Offset(50f, 200f),
                start1 = Offset(200f, 200f),
                end1 = Offset(250f, 200f)
            )
        }
        composeTestRule.waitForIdle()
        assertThat(zoomFactors).isNotEmpty()
        assertThat(zoomFactors.any { it > 1f }).isTrue()
    }

    private fun createUiState(aspectRatio: AspectRatio): PreviewDisplayUiState =
        PreviewDisplayUiState(
            aspectRatioUiState = AspectRatioUiState.Available(
                selectedAspectRatio = aspectRatio,
                availableAspectRatios = listOf(
                    SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                    SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
                    SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
                )
            )
        )

    @SuppressLint("RestrictedApi")
    private fun createFakeSurfaceRequest(width: Int, height: Int): SurfaceRequest {
        val fakeCamera = Proxy.newProxyInstance(
            CameraInternal::class.java.classLoader,
            arrayOf(CameraInternal::class.java)
        ) { proxy, method, args ->
            when (method.name) {
                "equals" -> proxy === args?.firstOrNull()
                "hashCode" -> System.identityHashCode(proxy)
                "toString" -> "FakeCameraInternal"
                "getHasTransform" -> true
                "isFrontFacing" -> false
                "isRemoved" -> false
                else -> null
            }
        } as CameraInternal

        return SurfaceRequest(Size(width, height), fakeCamera) {}.also { request ->
            request.updateTransformationInfo(
                SurfaceRequest.TransformationInfo.of(
                    AndroidRect(0, 0, width, height),
                    0,
                    Surface.ROTATION_0,
                    true,
                    Matrix(),
                    false
                )
            )
            createdSurfaceRequests.add(request)
        }
    }
}
