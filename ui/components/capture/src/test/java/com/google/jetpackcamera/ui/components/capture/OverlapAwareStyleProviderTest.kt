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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OverlapAwareStyleProviderTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun overlapAware_aboveThreshold_providesBlack60() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val targetBoundsPx = remember(density) {
                with(density) {
                    Rect(0f, 0f, 100.dp.toPx(), 100.dp.toPx())
                }
            }
            Box(Modifier.size(500.dp)) {
                CompositionLocalProvider(
                    LocalOverlapTargetBounds provides remember(
                        targetBoundsPx
                    ) { mutableStateOf(targetBoundsPx) }
                ) {
                    OverlapAwareStyleProvider(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 10.dp)
                            .size(100.dp)
                    ) {
                        detectedStyle = LocalCameraControlBackgroundStyle.current
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(detectedStyle).isEqualTo(CameraControlBackgroundStyle.BLACK_60)
    }

    @Test
    fun overlapAware_belowThreshold_providesWhite20() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val targetBoundsPx = remember(density) {
                with(density) {
                    Rect(0f, 0f, 100.dp.toPx(), 100.dp.toPx())
                }
            }
            Box(Modifier.size(500.dp)) {
                CompositionLocalProvider(
                    LocalOverlapTargetBounds provides remember(
                        targetBoundsPx
                    ) { mutableStateOf(targetBoundsPx) }
                ) {
                    OverlapAwareStyleProvider(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 60.dp)
                            .size(100.dp)
                    ) {
                        detectedStyle = LocalCameraControlBackgroundStyle.current
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(detectedStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)
    }

    @Test
    fun overlapAware_none_providesWhite20() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            val targetBoundsPx = remember(density) {
                with(density) {
                    Rect(0f, 0f, 100.dp.toPx(), 100.dp.toPx())
                }
            }
            Box(Modifier.size(500.dp)) {
                CompositionLocalProvider(
                    LocalOverlapTargetBounds provides remember(
                        targetBoundsPx
                    ) { mutableStateOf(targetBoundsPx) }
                ) {
                    OverlapAwareStyleProvider(
                        modifier = Modifier
                            .offset(x = 0.dp, y = 110.dp)
                            .size(100.dp)
                    ) {
                        detectedStyle = LocalCameraControlBackgroundStyle.current
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(detectedStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)
    }

    @Test
    fun overlapAware_noProvider_defaultsSafelyToWhite20() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            // We consciously DO NOT wrap this in a CompositionLocalProvider
            // to test the new fallback mechanism.
            Box(Modifier.size(500.dp)) {
                OverlapAwareStyleProvider(
                    modifier = Modifier
                        .offset(x = 0.dp, y = 10.dp)
                        .size(100.dp)
                ) {
                    detectedStyle = LocalCameraControlBackgroundStyle.current
                }
            }
        }
        composeTestRule.waitForIdle()

        // Because the missing provider falls back to Rect.Zero,
        // the 0f intersection correctly results in WHITE_20.
        assertThat(detectedStyle).isEqualTo(CameraControlBackgroundStyle.WHITE_20)
    }
}
