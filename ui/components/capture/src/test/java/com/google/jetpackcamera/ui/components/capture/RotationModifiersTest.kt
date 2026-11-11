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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.DeviceRotation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w1000dp-h1000dp-mdpi")
class RotationModifiersTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rememberGravityRotationState_usesShortestPathAcrossZeroAnd270Boundary() {
        var deviceRotation by mutableStateOf(DeviceRotation.Natural)
        var currentDegrees = 0f

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDisableAnimations provides true) {
                val state = rememberGravityRotationState(
                    deviceRotation = deviceRotation,
                    windowRotation = DeviceRotation.Natural
                )
                currentDegrees = state.value
            }
        }

        assertThat(currentDegrees).isEqualTo(0f)

        // Tilting 90° CW (Dev 90°, Win 0°) -> target 270°, shortest path from 0° is -90°
        deviceRotation = DeviceRotation.Rotated90
        composeTestRule.waitForIdle()
        assertThat(currentDegrees).isEqualTo(-90f)

        // Returning to Natural (Dev 0°, Win 0°) -> shortest path from -90° back to 0° is 0°
        deviceRotation = DeviceRotation.Natural
        composeTestRule.waitForIdle()
        assertThat(currentDegrees).isEqualTo(0f)

        // Tilting 90° CCW (Dev 270°, Win 0°) -> target 90°, shortest path from 0° is +90°
        deviceRotation = DeviceRotation.Rotated270
        composeTestRule.waitForIdle()
        assertThat(currentDegrees).isEqualTo(90f)
    }

    @Test
    fun rotateLayoutToGravity_swapsOuterDimensionsAt90And270Degrees() {
        var outerSize = IntSize.Zero
        var rotationDegrees by mutableStateOf(0)

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier = Modifier.onSizeChanged { outerSize = it }
                ) {
                    Box(
                        modifier = Modifier
                            .rotateLayoutToGravity(rotationDegrees)
                            .size(width = 200.dp, height = 60.dp)
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        assertThat(outerSize).isEqualTo(IntSize(200, 60))

        rotationDegrees = 90
        composeTestRule.waitForIdle()
        assertThat(outerSize).isEqualTo(IntSize(60, 200))

        rotationDegrees = 180
        composeTestRule.waitForIdle()
        assertThat(outerSize).isEqualTo(IntSize(200, 60))

        rotationDegrees = 270
        composeTestRule.waitForIdle()
        assertThat(outerSize).isEqualTo(IntSize(60, 200))
    }

    @Test
    fun counterRotateToPortrait_swapsConstraintsAndRotatesForLandscape_andIsNoOpForPortrait() {
        val topStripTag = "CounterRotateTopStrip"
        var windowRotation by mutableStateOf(DeviceRotation.Natural)
        var outerSize = IntSize.Zero
        var innerMeasuredSize = IntSize.Zero

        composeTestRule.setContent {
            CompositionLocalProvider(LocalDensity provides Density(1f)) {
                Box(
                    modifier = Modifier
                        .size(width = 740.dp, height = 360.dp)
                        .onSizeChanged { outerSize = it }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .counterRotateToPortrait(windowRotation)
                            .onSizeChanged { innerMeasuredSize = it }
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag(topStripTag)
                        )
                        // Also exercise unbounded width constraint swapping without crashing.
                        Box(
                            modifier = Modifier
                                .wrapContentWidth(unbounded = true)
                                .counterRotateToPortrait(windowRotation)
                                .size(width = 24.dp, height = 12.dp)
                        )
                    }
                }
            }
        }

        // Natural: no constraint swap, placed at (0, 0) with 0° rotation.
        composeTestRule.waitForIdle()
        assertThat(outerSize).isEqualTo(IntSize(740, 360))
        assertThat(innerMeasuredSize).isEqualTo(IntSize(740, 360))
        assertThat(composeTestRule.onNodeWithTag(topStripTag).fetchSemanticsNode().boundsInRoot)
            .isEqualTo(Rect(0f, 0f, 740f, 48f))

        // Rotated180: still portrait (!isLandscape), complete no-op with 0° rotation.
        windowRotation = DeviceRotation.Rotated180
        composeTestRule.waitForIdle()
        assertThat(innerMeasuredSize).isEqualTo(IntSize(740, 360))
        assertThat(composeTestRule.onNodeWithTag(topStripTag).fetchSemanticsNode().boundsInRoot)
            .isEqualTo(Rect(0f, 0f, 740f, 48f))

        // Rotated90: swaps constraints to 360x740 and counter-rotates +90° CW -> top strip on right
        windowRotation = DeviceRotation.Rotated90
        composeTestRule.waitForIdle()
        assertThat(outerSize).isEqualTo(IntSize(740, 360))
        assertThat(innerMeasuredSize).isEqualTo(IntSize(360, 740))
        assertThat(composeTestRule.onNodeWithTag(topStripTag).fetchSemanticsNode().boundsInRoot)
            .isEqualTo(Rect(692f, 0f, 740f, 360f))

        // Rotated270: swaps constraints to 360x740 and counter-rotates -90° CCW -> top on left.
        windowRotation = DeviceRotation.Rotated270
        composeTestRule.waitForIdle()
        assertThat(outerSize).isEqualTo(IntSize(740, 360))
        assertThat(innerMeasuredSize).isEqualTo(IntSize(360, 740))
        assertThat(composeTestRule.onNodeWithTag(topStripTag).fetchSemanticsNode().boundsInRoot)
            .isEqualTo(Rect(0f, 0f, 48f, 360f))
    }

    @Test
    fun toPortraitWindowInsets_remapsAllEdges_acrossAllRotationsInLtrAndRtl() {
        val density = Density(1f)
        val observedDirections = mutableListOf<LayoutDirection>()
        val baseInsets = object : WindowInsets {
            override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int {
                observedDirections.add(layoutDirection)
                return 10
            }

            override fun getTop(density: Density): Int = 20

            override fun getRight(density: Density, layoutDirection: LayoutDirection): Int {
                observedDirections.add(layoutDirection)
                return 30
            }

            override fun getBottom(density: Density): Int = 40
        }

        for (direction in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            observedDirections.clear()
            val natural = baseInsets.toPortraitWindowInsets(DeviceRotation.Natural)
            assertThat(natural.getLeft(density, direction)).isEqualTo(10)
            assertThat(natural.getTop(density)).isEqualTo(20)
            assertThat(natural.getRight(density, direction)).isEqualTo(30)
            assertThat(natural.getBottom(density)).isEqualTo(40)

            val rotated270 = baseInsets.toPortraitWindowInsets(DeviceRotation.Rotated270)
            assertThat(rotated270.getLeft(density, direction)).isEqualTo(40)
            assertThat(rotated270.getTop(density)).isEqualTo(10)
            assertThat(rotated270.getRight(density, direction)).isEqualTo(20)
            assertThat(rotated270.getBottom(density)).isEqualTo(30)

            val rotated90 = baseInsets.toPortraitWindowInsets(DeviceRotation.Rotated90)
            assertThat(rotated90.getLeft(density, direction)).isEqualTo(20)
            assertThat(rotated90.getTop(density)).isEqualTo(30)
            assertThat(rotated90.getRight(density, direction)).isEqualTo(40)
            assertThat(rotated90.getBottom(density)).isEqualTo(10)

            val rotated180 = baseInsets.toPortraitWindowInsets(DeviceRotation.Rotated180)
            assertThat(rotated180.getLeft(density, direction)).isEqualTo(30)
            assertThat(rotated180.getTop(density)).isEqualTo(40)
            assertThat(rotated180.getRight(density, direction)).isEqualTo(10)
            assertThat(rotated180.getBottom(density)).isEqualTo(20)

            // Physical window edges must always query baseInsets with LayoutDirection.Ltr.
            assertThat(observedDirections).isNotEmpty()
            assertThat(observedDirections.all { it == LayoutDirection.Ltr }).isTrue()
        }

        // Verify value equality and hashCode contract on PortraitWindowInsets.
        val first = baseInsets.toPortraitWindowInsets(DeviceRotation.Rotated90)
        val second = baseInsets.toPortraitWindowInsets(DeviceRotation.Rotated90)
        val different = baseInsets.toPortraitWindowInsets(DeviceRotation.Rotated270)
        assertThat(first).isEqualTo(second)
        assertThat(first.hashCode()).isEqualTo(second.hashCode())
        assertThat(first).isNotEqualTo(different)
    }
}
