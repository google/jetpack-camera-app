/*
 * Copyright (C) 2023 The Android Open Source Project
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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.width
import com.google.jetpackcamera.domain.camera.CameraUseCase.ScreenFlashEvent
import com.google.jetpackcamera.domain.camera.test.FakeCameraUseCase
import com.google.jetpackcamera.feature.preview.ScreenFlashViewModel
import com.google.jetpackcamera.feature.preview.rules.MainDispatcherRule
import com.google.jetpackcamera.feature.preview.workaround.captureToImage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ScreenFlashComponentsKtTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule(testDispatcher)

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var cameraUseCase: FakeCameraUseCase
    private lateinit var screenFlashViewModel: ScreenFlashViewModel

    @Before
    fun setUp() {
        cameraUseCase = FakeCameraUseCase(testScope)
        screenFlashViewModel = ScreenFlashViewModel(cameraUseCase = cameraUseCase)

        composeTestRule.setContent {
            ScreenFlash(viewModel = screenFlashViewModel)
        }
    }

    @Test
    fun screenFlashOverlay_disabledByDefault() = runTest {
        advanceUntilIdle()
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay")).assertDoesNotExist()
    }

    @Test
    fun screenFlashOverlay_enabledAfterApplyUiEvent() = runTest {
        cameraUseCase.emitScreenFlashEvent(ScreenFlashEvent(ScreenFlashEvent.Type.APPLY_UI) {})

        advanceUntilIdle()
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay")).assertExists()
    }

    @Test
    fun screenFlashOverlay_disabledAfterClearUiEvent() = runTest {
        cameraUseCase.emitScreenFlashEvent(ScreenFlashEvent(ScreenFlashEvent.Type.APPLY_UI) {})
        cameraUseCase.emitScreenFlashEvent(ScreenFlashEvent(ScreenFlashEvent.Type.CLEAR_UI) {})

        advanceUntilIdle()
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay")).assertDoesNotExist()
    }

    @Test
    fun screenFlashOverlay_sizeFillsMaxSize() = runTest {
        cameraUseCase.emitScreenFlashEvent(ScreenFlashEvent(ScreenFlashEvent.Type.APPLY_UI) {})

        advanceUntilIdle()
        val rootBounds = composeTestRule.onRoot().getBoundsInRoot()
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay"))
            .assertWidthIsAtLeast(rootBounds.width)
        composeTestRule.onNode(hasTestTag("ScreenFlashOverlay"))
            .assertHeightIsAtLeast(rootBounds.height)
    }

    @Test
    fun screenFlashOverlay_fullWhiteWhenApplied() = runTest {
        cameraUseCase.emitScreenFlashEvent(ScreenFlashEvent(ScreenFlashEvent.Type.APPLY_UI) {})

        advanceUntilIdle()
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
            assertEquals(Color.White.value.toInt(), it)
        }
    }
}
