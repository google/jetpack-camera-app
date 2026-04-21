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

import androidx.camera.viewfinder.core.ImplementationMode
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.core.camera.PreviewSurfaceRequest
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for the [PreviewDisplay] composable.
 */
@RunWith(AndroidJUnit4::class)
class PreviewDisplayTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Verifies that the [PreviewDisplay] composable correctly renders when provided with a
     * [PreviewSurfaceRequest.Viewfinder] and fulfills the surface request.
     */
    @Test
    fun previewDisplay_withViewfinderRequest_rendersWithoutError() {
        val viewfinderRequest = ViewfinderSurfaceRequest(
            1920,
            1080,
            ImplementationMode.EXTERNAL
        )
        val surfaceRequest = PreviewSurfaceRequest.Viewfinder(viewfinderRequest)

        composeTestRule.setContent {
            PreviewDisplay(
                previewDisplayUiState = PreviewDisplayUiState(
                    aspectRatioUiState = AspectRatioUiState.Available(
                        availableAspectRatios = listOf(
                            SingleSelectableUiState.SelectableUi(
                                AspectRatio.THREE_FOUR
                            )
                        ),
                        selectedAspectRatio = AspectRatio.THREE_FOUR
                    ),
                    surfaceRequest = surfaceRequest
                ),
                onTapToFocus = { _, _ -> },
                onFlipCamera = { },
                onScaleZoom = { },
                onRequestWindowColorMode = { },
                focusMeteringUiState = FocusMeteringUiState.Unspecified,
                surfaceRequest = surfaceRequest
            )
        }

        composeTestRule.waitForIdle()
    }
}
