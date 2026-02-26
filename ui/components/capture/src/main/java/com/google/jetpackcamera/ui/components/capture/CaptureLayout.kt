/*
 * Copyright (C) 2025 The Android Open Source Project
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/**
 * The base layout for the camera capture screen.
 *
 * @param modifier the modifier for this component
 * @param viewfinder the viewfinder composable
 * @param captureButton the capture button composable
 * @param imageWell the image well composable
 * @param flipCameraButton the flip camera button composable
 * @param zoomLevelDisplay the zoom level display composable
 * @param elapsedTimeDisplay the elapsed time display composable
 * @param quickSettingsButton the quick settings button composable
 * @param indicatorRow the indicator row composable
 * @param captureModeToggle the capture mode toggle composable
 * @param quickSettingsOverlay the quick settings overlay composable
 * @param debugOverlay the debug overlay composable
 * @param debugVisibilityWrapper A wrapper that conditionally hides its contents based on debug settings
 * @param screenFlashOverlay the screen flash overlay composable
 * @param snackBar the snack bar composable for showing messages
 */
@Composable
fun CaptureLayout(
    modifier: Modifier = Modifier,
    viewfinder: @Composable (Modifier) -> Unit,
    captureButton: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    zoomLevelDisplay: @Composable (Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (Modifier) -> Unit,
    quickSettingsButton: @Composable (Modifier) -> Unit,
    indicatorRow: @Composable (Modifier) -> Unit,
    captureModeToggle: @Composable (Modifier) -> Unit,
    quickSettingsOverlay: @Composable (Modifier) -> Unit,
    debugOverlay: @Composable (Modifier) -> Unit,
    debugVisibilityWrapper: (@Composable (@Composable () -> Unit) -> Unit),
    screenFlashOverlay: @Composable (Modifier) -> Unit,
    snackBar: @Composable (Modifier, snackbarHostState: SnackbarHostState) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(modifier = modifier.background(Color.Black)) {
            Column {
                indicatorRow(Modifier.statusBarsPadding())
                viewfinder(Modifier)
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .safeDrawingPadding()
            ) {
                debugVisibilityWrapper {
                    VerticalMaterialControls(
                        captureButton = captureButton,
                        imageWell = imageWell,
                        flipCameraButton = flipCameraButton,
                        quickSettingsToggleButton = quickSettingsButton,
                        captureModeToggleSwitch = captureModeToggle,
                        bottomSheetQuickSettings = quickSettingsOverlay,
                        zoomControls = zoomLevelDisplay,
                        elapsedTimeDisplay = elapsedTimeDisplay
                    )
                }
                // controls overlay
                snackBar(Modifier, snackbarHostState)
                screenFlashOverlay(Modifier)
            }
            debugOverlay(Modifier)
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun VerticalMaterialControls(
    modifier: Modifier = Modifier,
    captureButton: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (Modifier) -> Unit,
    imageWell: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    quickSettingsToggleButton: @Composable (Modifier) -> Unit,
    bottomSheetQuickSettings: @Composable (Modifier) -> Unit,
    captureModeToggleSwitch: @Composable (Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (Modifier) -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                // elapsed time
                elapsedTimeDisplay(Modifier)

                // zoom controls component
                zoomControls(Modifier)

                // capture button row
                CaptureButtonRow(
                    modifier = Modifier.padding(24.dp),
                    captureButton = { captureButton(Modifier) },
                    leftItem = { imageWell(Modifier) },
                    rightItem = { flipCameraButton(Modifier) }
                )

                // bottom controls row
                BottomControls(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    centerItem = { captureModeToggleSwitch(Modifier) },
                    leftItem = { quickSettingsToggleButton(Modifier) },
                    rightItem = { }
                )
            }
        }
        bottomSheetQuickSettings(Modifier)
    }
}

@Composable
private fun CaptureButtonRow(
    captureButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leftItem: @Composable () -> Unit = {},
    rightItem: @Composable () -> Unit = {}
) {
    Row(
        modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left controls (imageWell)
        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            leftItem()
        }

        // Capture Button at Center
        captureButton()

        // Right controls (flipCameraButton)
        Box(
            modifier = Modifier
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            rightItem()
        }
    }
}

@Composable
private fun BottomControls(
    centerItem: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    leftItem: @Composable () -> Unit = {},
    rightItem: @Composable () -> Unit = {}
) {
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Row that holds toggle buttons for quick settings and capture mode
        // quick settings toggle switch item to the left
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart
        ) {
            leftItem()
        }

        // capture mode toggle switch center
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center
        ) {
            centerItem()
        }

        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            rightItem()
        }
    }
}

@Preview
@Composable
private fun CaptureLayoutPreview() {
    CaptureLayout(
        modifier = Modifier.background(Color.Black),
        viewfinder = { modifier ->
            Box(
                modifier = modifier
                    .fillMaxWidth()
                    .height(600.dp)
                    .background(Color.DarkGray),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "viewfinder", textAlign = TextAlign.Center, color = Color.White)
            }
        },
        captureButton = { modifier ->
            Box(
                modifier = modifier
                    .size(80.dp)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "capture button", textAlign = TextAlign.Center)
            }
        },
        flipCameraButton = { modifier ->
            Box(
                modifier = modifier
                    .size(48.dp)
                    .background(Color.Cyan),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "flip camera ", textAlign = TextAlign.Center)
            }
        },
        imageWell = { modifier ->
            Box(
                modifier = modifier
                    .size(48.dp)
                    .background(Color.Green),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "image well", textAlign = TextAlign.Center)
            }
        },
        zoomLevelDisplay = { modifier ->
            Box(
                modifier = modifier
                    .height(48.dp)
                    .fillMaxWidth(.5f)
                    .background(Color.Magenta),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "zoom controls", textAlign = TextAlign.Center)
            }
        },
        elapsedTimeDisplay = { modifier ->
            Box(
                modifier = modifier
                    .height(24.dp)
                    .fillMaxWidth(0.25f)
                    .background(Color.Red),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "elapsed time", textAlign = TextAlign.Center)
            }
        },
        quickSettingsButton = { modifier ->
            Box(
                modifier = modifier
                    .size(48.dp)
                    .background(Color.Yellow),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "quick setting toggle", textAlign = TextAlign.Center)
            }
        },
        indicatorRow = { modifier ->
            Box(
                modifier = modifier
                    .height(48.dp)
                    .fillMaxWidth()
                    .background(Color.Green),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "indicators row")
            }
        },
        captureModeToggle = { modifier ->
            Box(
                modifier = modifier
                    .height(48.dp)
                    .fillMaxWidth(0.5f)
                    .background(Color.Blue),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "capture toggle", textAlign = TextAlign.Center, color = Color.Yellow)
            }
        },
        quickSettingsOverlay = {
            // No-op for preview
        },
        debugOverlay = {
            // No-op for preview
        },
        screenFlashOverlay = {
            // No-op for preview
        },
        snackBar = { _, _ ->
            // No-op for preview
        },
        debugVisibilityWrapper = { content -> content() }
    )
}
