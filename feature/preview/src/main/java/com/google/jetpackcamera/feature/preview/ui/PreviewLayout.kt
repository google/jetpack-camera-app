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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState

//layouts are only concerned with placement. nothing else. no state handling
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun PreviewLayout(
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    viewfinder: @Composable (modifier: Modifier) -> Unit,
    captureButton: @Composable (modifier: Modifier) -> Unit,
    flipCameraButton: @Composable (modifier: Modifier) -> Unit,
    zoomLevelDisplay: @Composable (modifier: Modifier) -> Unit,
    settingsButton: @Composable (modifier: Modifier) -> Unit,
    quickSettingsButton: @Composable (modifier: Modifier) -> Unit,
    imageWellButton: @Composable (modifier: Modifier) -> Unit,
    flashModeButton: @Composable (modifier: Modifier) -> Unit,
    audioToggleButton: @Composable (modifier: Modifier) -> Unit,
    captureModeToggle: @Composable (modifier: Modifier) -> Unit,
    quickSettingsOverlay: @Composable (modifier: Modifier) -> Unit,
    debugOverlay: @Composable (modifier: Modifier) -> Unit,
    screenFlashOverlay: @Composable (modifier: Modifier) -> Unit,
    snackBar: @Composable (modifier: Modifier) -> Unit,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            viewfinder(Modifier)
            VerticalMaterialControls(
                captureButton = captureButton,
                flipCameraButton = flipCameraButton,
                toggleSettingsButton = settingsButton,
                toggleCaptureModeSwitch = captureModeToggle
            )
            // controls overlay
                snackBar(Modifier)

            quickSettingsOverlay(Modifier)
            screenFlashOverlay(Modifier)
        }
    }
}

@Composable
private fun VerticalMaterialControls(
    modifier: Modifier = Modifier,
    captureButton: @Composable (Modifier) -> Unit,
    flipCameraButton: @Composable (Modifier) -> Unit,
    toggleSettingsButton: @Composable (Modifier) -> Unit,
    toggleCaptureModeSwitch: @Composable (Modifier) -> Unit,
    ) {
    Column(modifier = modifier.fillMaxSize()) {
        Spacer(Modifier.weight(2F))

        // todo zoom controls

        Column(Modifier.weight(1f), verticalArrangement = Arrangement.SpaceBetween) {
            //capture button row

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Spacer(Modifier.weight(1f))
                captureButton(Modifier)
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    flipCameraButton(Modifier)
                }
            }

            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    toggleSettingsButton(Modifier)
                }
               // toggleCaptureModeSwitch(Modifier)
                Spacer(Modifier.weight(1f))

            }
            // bottom controls row
        }
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

@Composable
private fun NewMaterialLayout() {

}

//todo controlstop should only be visible in debug mode
@Composable
fun ControlsTop(
    settingsButton: @Composable (modifier: Modifier) -> Unit,
    quickSettingsDropDownButton: @Composable (modifier: Modifier) -> Unit,
    flashModePreviewButton: @Composable (modifier: Modifier) -> Unit,
    stabilizationIndicator: @Composable (modifier: Modifier) -> Unit,
    videoQualityIndicator: @Composable (modifier: Modifier) -> Unit,
    debugOverlayToggle: @Composable ((modifier: Modifier) -> Unit)? = null,
) {

}

@Composable
fun ControlsBottom(
    debugZoomRatioText: @Composable ((modifier: Modifier) -> Unit)?,
    debugCameraIdText: @Composable ((modifier: Modifier) -> Unit)?,
    elapsedTimeText: @Composable (modifier: Modifier) -> Unit,
    captureModeHdrToggle: @Composable (modifier: Modifier) -> Unit,
    flipCameraButton: @Composable (modifier: Modifier) -> Unit,
    pauseResumeToggle: @Composable (modifier: Modifier) -> Unit,
    captureButton: @Composable (modifier: Modifier) -> Unit,
) {

}