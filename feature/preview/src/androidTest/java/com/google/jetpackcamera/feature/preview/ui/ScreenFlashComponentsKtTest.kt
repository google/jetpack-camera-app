/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
import com.google.jetpackcamera.feature.preview.ScreenFlashUiState
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScreenFlashComponentsKtTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val screenFlashUiState: MutableState<ScreenFlashUiState> =
        mutableStateOf(ScreenFlashUiState.NotApplied())

    @Before
    fun setUp() {
        composeTestRule.setContent {
            ScreenFlashScreen(
                screenFlashUiState = screenFlashUiState.value,
                onInitialBrightnessCalculated = {}
            )
        }
    }

    @Test
    fun screenFlashOverlay_doesNotExistByDefault() = runTest {
        composeTestRule.awaitIdle()
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay")).assertDoesNotExist()
    }

    @Test
    fun screenFlashOverlay_existsAfterStateIsEnabled() = runTest {
        screenFlashUiState.value = ScreenFlashUiState.Applied(onComplete = {})

        composeTestRule.awaitIdle()
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay")).assertExists()
    }

    @Test
    fun screenFlashOverlay_doesNotExistWhenDisabledAfterEnabled() = runTest {
        screenFlashUiState.value = ScreenFlashUiState.Applied(onComplete = {})
        screenFlashUiState.value = ScreenFlashUiState.NotApplied()

        composeTestRule.awaitIdle()
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay")).assertDoesNotExist()
    }

    @Test
    fun screenFlashOverlay_sizeFillsMaxSize() = runTest {
        screenFlashUiState.value = ScreenFlashUiState.Applied(onComplete = {})

        composeTestRule.awaitIdle()
        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay"))
            .assertWidthIsAtLeast(rootBounds.width)
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay"))
            .assertHeightIsAtLeast(rootBounds.height)
    }

    @Test
    fun screenFlashOverlay_fullWhiteWhenEnabled() = runTest {
        screenFlashUiState.value = ScreenFlashUiState.Applied(onComplete = {})

        composeTestRule.awaitIdle()
        val overlayScreenShot =
            composeTestRule.onNode(hasTestTag("ScreenFlashOverlay")).captureToImage()

        // check a few pixels near center instead of whole image to save time
        val overlayPixels = IntArray(4)
        overlayScreenShot.readPixels(
            overlayPixels,
            overlayScreenShot.width / 2,
            overlayScreenShot.height / 2,
            2,
            2
        )
        overlayPixels.forEach {
            assertEquals(Color.White.toArgb(), it)
        }
    }
}
