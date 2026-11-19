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

import android.view.Surface
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
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
    fun resolveWindowRotation_portraitNaturalDisplay_mapsDirectlyFromSurfaceRotation() {
        assertThat(
            resolveWindowRotation(
                surfaceRotation = Surface.ROTATION_0,
                displayWidth = 1080,
                displayHeight = 2400
            )
        ).isEqualTo(DeviceRotation.Natural)

        assertThat(
            resolveWindowRotation(
                surfaceRotation = Surface.ROTATION_90,
                displayWidth = 2400,
                displayHeight = 1080
            )
        ).isEqualTo(DeviceRotation.Rotated270)

        assertThat(
            resolveWindowRotation(
                surfaceRotation = Surface.ROTATION_180,
                displayWidth = 1080,
                displayHeight = 2400
            )
        ).isEqualTo(DeviceRotation.Rotated180)

        assertThat(
            resolveWindowRotation(
                surfaceRotation = Surface.ROTATION_270,
                displayWidth = 2400,
                displayHeight = 1080
            )
        ).isEqualTo(DeviceRotation.Rotated90)
    }

    @Test
    fun resolveWindowRotation_landscapeNaturalDisplay_mapsToPortraitRelativeRotation() {
        assertThat(
            resolveWindowRotation(
                surfaceRotation = Surface.ROTATION_0,
                displayWidth = 2560,
                displayHeight = 1600
            )
        ).isEqualTo(DeviceRotation.Rotated270)

        assertThat(
            resolveWindowRotation(
                surfaceRotation = Surface.ROTATION_90,
                displayWidth = 1600,
                displayHeight = 2560
            )
        ).isEqualTo(DeviceRotation.Natural)

        assertThat(
            resolveWindowRotation(
                surfaceRotation = Surface.ROTATION_180,
                displayWidth = 2560,
                displayHeight = 1600
            )
        ).isEqualTo(DeviceRotation.Rotated90)

        assertThat(
            resolveWindowRotation(
                surfaceRotation = Surface.ROTATION_270,
                displayWidth = 1600,
                displayHeight = 2560
            )
        ).isEqualTo(DeviceRotation.Rotated180)
    }

    @Test
    fun undoWindowCounterRotation_restoresWindowDimensionsInLandscape() {
        val counterRotationState = WindowCounterRotationState()

        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .size(width = 200.dp, height = 100.dp)
                    .counterRotateToPortrait(
                        windowRotation = DeviceRotation.Rotated270,
                        counterRotationState = counterRotationState
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .undoWindowCounterRotation(counterRotationState)
                        .testTag("Viewfinder")
                )
            }
        }

        val bounds = composeTestRule.onNodeWithTag("Viewfinder").getUnclippedBoundsInRoot()
        assertThat(DpSize(bounds.width, bounds.height)).isEqualTo(DpSize(200.dp, 100.dp))
    }
}
