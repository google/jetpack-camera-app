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
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.DeviceRotation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
}
