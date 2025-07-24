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

package com.google.jetpackcamera.feature.preview.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

// these layouts are only concerned with placement. nothing else. no state handling
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PreviewLayout(
    modifier: Modifier = Modifier,
    // snackbarHostState: SnackbarHostState,
    viewfinder: @Composable (modifier: Modifier) -> Unit,
    captureButton: @Composable (modifier: Modifier) -> Unit,
    flipCameraButton: @Composable (modifier: Modifier) -> Unit,
    zoomLevelDisplay: @Composable (modifier: Modifier) -> Unit,
    settingsButton: @Composable (modifier: Modifier) -> Unit,
    quickSettingsButton: @Composable (modifier: Modifier) -> Unit,
    imageWellButton: @Composable (modifier: Modifier) -> Unit,
    flashModeButton: @Composable (modifier: Modifier) -> Unit,
    pauseToggleButton: @Composable (modifier: Modifier) -> Unit,
    audioToggleButton: @Composable (modifier: Modifier) -> Unit,
    captureModeToggle: @Composable (modifier: Modifier) -> Unit,
    quickSettingsOverlay: @Composable (modifier: Modifier) -> Unit,
    debugOverlay: @Composable (modifier: Modifier) -> Unit,
    screenFlashOverlay: @Composable (modifier: Modifier) -> Unit,
    snackBar: @Composable (modifier: Modifier, snackbarHostState: SnackbarHostState) -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            viewfinder(Modifier)
            VerticalMaterialControls(
                captureButton = captureButton,
                flipCameraButton = flipCameraButton,
                toggleQuickSettings = quickSettingsButton,
                toggleCaptureModeSwitch = captureModeToggle,
                bottomSheetQuickSettings = quickSettingsOverlay,
                zoomControls = zoomLevelDisplay,
            )

            debugOverlay(Modifier)
            // controls overlay
            snackBar(Modifier, snackbarHostState)
            screenFlashOverlay(Modifier)
        }
    }
}

@Composable
private fun VerticalMaterialControls(
    modifier: Modifier = Modifier,
    captureButton: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    toggleQuickSettings: @Composable (Modifier) -> Unit,
    bottomSheetQuickSettings: @Composable (Modifier) -> Unit,
    toggleCaptureModeSwitch: @Composable (Modifier) -> Unit,
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
                                //todo leftCaptureButton item
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

                // bottom controls row
                Row(
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Max)
                        .padding(vertical = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Row that holds flip camera, capture button, and audio
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {

                        // left toggle switch item
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            toggleQuickSettings(Modifier)
                        }
                    }

                    // capture mode toggle switch
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        toggleCaptureModeSwitch(Modifier)
                    }

                    // right toggle switch item
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {}
                }
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        //todo(kc): tune padding
                        .padding(bottom = 50.dp)
                )
            }
        }
        bottomSheetQuickSettings(Modifier)
    }


}

@Composable
private fun VerticalPreviewLayout(
    modifier: Modifier = Modifier,
    viewFinder: @Composable (Modifier) -> Unit,
    zoomControls: @Composable (modifier: Modifier) -> Unit,
    captureButton: @Composable (modifier: Modifier) -> Unit,
    flipCameraButton: @Composable (modifier: Modifier) -> Unit,

    ) {
    //viewfinder should fill from top to bottom? with rounded corners
    Box(
        modifier = modifier
            .safeDrawingPadding()
            .fillMaxSize()
    ) {
        viewFinder(Modifier)

        Column() {

        }
    }
}