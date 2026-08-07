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

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
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
            CompositionLocalProvider(
                LocalOverlapTargetBounds provides Rect(0f, 0f, 100f, 100f)
            ) {
                // If the element is at y=50, it overlaps the bottom 50px of the target.
                // 50 x 100 overlap / 100x100 box = 0.5f overlap.
                // However, let's use exact offsets.
                // offset moves it relative to parent.
                // We'll trust onGloballyPositioned will reflect the offset.
                // But Robolectric testing density is 1f.
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
        // 90px intersect / 10000px area = 0.9 overlap -> BLACK_60
        assertEquals(CameraControlBackgroundStyle.BLACK_60, detectedStyle)
    }

    @Test
    fun overlapAware_belowThreshold_providesWhite20() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalOverlapTargetBounds provides Rect(0f, 0f, 100f, 100f)
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

        composeTestRule.waitForIdle()
        // 40px intersect / 10000 px area = .4 overlap -> WHITE_20
        assertEquals(CameraControlBackgroundStyle.WHITE_20, detectedStyle)
    }

    @Test
    fun overlapAware_none_providesWhite20() {
        var detectedStyle: CameraControlBackgroundStyle? = null

        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalOverlapTargetBounds provides Rect(0f, 0f, 100f, 100f)
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

        composeTestRule.waitForIdle()
        assertEquals(CameraControlBackgroundStyle.WHITE_20, detectedStyle)
    }
}
