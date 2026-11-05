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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoScreenTorchFrameTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        composeTestRule.setContent {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            ) {
                VideoScreenTorchFrame(Modifier.fillMaxSize())
            }
        }
    }

    @Test
    fun videoScreenTorchFrame_sizeFillsMaxSize() = runTest {
        composeTestRule.awaitIdle()
        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()
        composeTestRule.onNode(hasTestTag(VIDEO_SCREEN_TORCH_OVERLAY))
            .assertWidthIsAtLeast(rootBounds.width)
        composeTestRule.onNode(hasTestTag(VIDEO_SCREEN_TORCH_OVERLAY))
            .assertHeightIsAtLeast(rootBounds.height)
    }

    @Test
    fun videoScreenTorchFrame_borderIsWhiteAndCenterIsNotWhite() = runTest {
        composeTestRule.awaitIdle()
        val frameImage =
            composeTestRule.onNode(hasTestTag(VIDEO_SCREEN_TORCH_OVERLAY)).captureToImage()

        val borderPixel = IntArray(1)
        frameImage.readPixels(
            borderPixel,
            frameImage.width / 2,
            1,
            1,
            1
        )
        assertThat(borderPixel[0]).isEqualTo(Color.White.toArgb())

        val centerPixel = IntArray(1)
        frameImage.readPixels(
            centerPixel,
            frameImage.width / 2,
            frameImage.height / 2,
            1,
            1
        )
        assertThat(centerPixel[0]).isNotEqualTo(Color.White.toArgb())
    }
}
