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

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// these layouts are only concerned with placement. nothing else. no state handling
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PreviewLayout(
    modifier: Modifier = Modifier,
    viewfinder: @Composable (modifier: Modifier) -> Unit,
    captureButton: @Composable (modifier: Modifier) -> Unit,
    flipCameraButton: @Composable (modifier: Modifier) -> Unit,
    zoomLevelDisplay: @Composable (modifier: Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (modifier: Modifier) -> Unit,
    quickSettingsButton: @Composable (modifier: Modifier) -> Unit,
    flashModeIndicator: @Composable (modifier: Modifier) -> Unit,
    hdrIndicator: @Composable (modifier: Modifier) -> Unit,
    videoQualityIndicator: @Composable (modifier: Modifier) -> Unit,
    stabilizationIndicator: @Composable (modifier: Modifier) -> Unit,
    pauseToggleButton: @Composable (modifier: Modifier) -> Unit,
    audioToggleButton: @Composable (modifier: Modifier) -> Unit,
    captureModeToggle: @Composable (modifier: Modifier) -> Unit,
    imageWell: @Composable (modifier: Modifier) -> Unit,
    quickSettingsOverlay: @Composable (modifier: Modifier) -> Unit,
    debugOverlay: @Composable (
        modifier: Modifier,
        extraButtons: Array<@Composable () -> Unit>?
    ) -> Unit,
    screenFlashOverlay: @Composable (modifier: Modifier) -> Unit,
    snackBar: @Composable (modifier: Modifier, snackbarHostState: SnackbarHostState) -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column {
            Row(
                modifier = Modifier
                    .background(Color.Black)
                    .safeContentPadding()
                    .fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                flashModeIndicator(Modifier)
                hdrIndicator(Modifier)
                videoQualityIndicator(Modifier)
                stabilizationIndicator(Modifier)
            }
            viewfinder(Modifier)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .safeDrawingPadding()

        ) {
            VerticalMaterialControls(
                captureButton = captureButton,
                flipCameraButton = flipCameraButton,
                quickSettingsToggleButton = quickSettingsButton,
                captureModeToggleSwitch = captureModeToggle,
                bottomSheetQuickSettings = quickSettingsOverlay,
                zoomControls = zoomLevelDisplay
            )
            // controls overlay
            snackBar(Modifier, snackbarHostState)
            screenFlashOverlay(Modifier)
        }
        debugOverlay(
            Modifier,
            arrayOf(
                { imageWell(Modifier) },
                { audioToggleButton(Modifier) },
                { pauseToggleButton(Modifier) },
                { elapsedTimeDisplay(Modifier) }
            )
        )
    }
}

@Composable
private fun VerticalMaterialControls(
    modifier: Modifier = Modifier,
    captureButton: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    quickSettingsToggleButton: @Composable (Modifier) -> Unit,
    bottomSheetQuickSettings: @Composable (Modifier) -> Unit,
    captureModeToggleSwitch: @Composable (Modifier) -> Unit
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                // zoom controls row
                zoomControls(Modifier)

                // capture button row
                Column {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Row that holds flip camera, capture button, and audio
                        Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                            // animation fades in/out this component based on quick settings
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(120.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                //  item to be placed left of capture button goes here
                            }
                        }
                        captureButton(Modifier)

                        // right capturebutton item
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            flipCameraButton(Modifier)
                        }
                    }
                }

                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        // todo(kc): tune padding
                        .padding(bottom = 50.dp)
                )

                // bottom controls row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 64.dp)
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Row that holds toggle buttons for quick settings and capture mode
                    // quick settings toggle switch item to the left
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        quickSettingsToggleButton(Modifier)
                    }

                    // capture mode toggle switch center
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        captureModeToggleSwitch(Modifier)
                    }

                    // right toggle switch item to the right
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {}
                }
            }
        }
        bottomSheetQuickSettings(Modifier)
    }
}
