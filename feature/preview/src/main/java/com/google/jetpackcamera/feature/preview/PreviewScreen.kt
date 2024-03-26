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
package com.google.jetpackcamera.feature.preview

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import android.view.Display
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import com.google.jetpackcamera.feature.preview.ui.BlinkState
import com.google.jetpackcamera.feature.preview.ui.CameraControlsOverlay
import com.google.jetpackcamera.feature.preview.ui.PreviewDisplay
import com.google.jetpackcamera.feature.preview.ui.ScreenFlashScreen
import com.google.jetpackcamera.feature.preview.ui.TestableToast
import com.google.jetpackcamera.feature.quicksettings.QuickSettingsScreenOverlay
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing

private const val TAG = "PreviewScreen"

/**
 * Screen used for the Preview feature.
 */
@Composable
fun PreviewScreen(
    onPreviewViewModel: (PreviewViewModel) -> Unit,
    onNavigateToSettings: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel(),
    previewMode: PreviewMode
) {
    Log.d(TAG, "PreviewScreen")
    onPreviewViewModel(viewModel)

    val previewUiState: PreviewUiState by viewModel.previewUiState.collectAsState()

    val screenFlashUiState: ScreenFlash.ScreenFlashUiState
        by viewModel.screenFlash.screenFlashUiState.collectAsState()

    val surfaceRequest: SurfaceRequest?
        by viewModel.surfaceRequest.collectAsState()

    LifecycleStartEffect(Unit) {
        viewModel.startCamera()
        onStopOrDispose {
            viewModel.stopCamera()
        }
    }

    when (previewUiState.cameraState) {
        CameraState.NOT_READY -> LoadingScreen()
        CameraState.READY -> ContentScreen(
            previewUiState = previewUiState,
            previewMode = previewMode,
            screenFlashUiState = screenFlashUiState,
            surfaceRequest = surfaceRequest,
            onNavigateToSettings = onNavigateToSettings,
            onClearUiScreenBrightness = viewModel.screenFlash::setClearUiScreenBrightness,
            onSetLensFacing = viewModel::setLensFacing,
            onTapToFocus = viewModel::tapToFocus,
            onChangeZoomScale = viewModel::setZoomScale,
            onChangeFlash = viewModel::setFlash,
            onChangeAspectRatio = viewModel::setAspectRatio,
            onChangeCaptureMode = viewModel::setCaptureMode,
            onToggleQuickSettings = viewModel::toggleQuickSettings,
            onCaptureImage = viewModel::captureImage,
            onCaptureImageWithUri = viewModel::captureImageWithUri,
            onStartVideoRecording = viewModel::startVideoRecording,
            onStopVideoRecording = viewModel::stopVideoRecording,
            onToastShown = viewModel::onToastShown
        )
    }
}

@Composable
private fun ContentScreen(
    previewUiState: PreviewUiState,
    previewMode: PreviewMode,
    screenFlashUiState: ScreenFlash.ScreenFlashUiState,
    surfaceRequest: SurfaceRequest?,
    onNavigateToSettings: () -> Unit = {},
    onClearUiScreenBrightness: (Float) -> Unit = {},
    onSetLensFacing: (newLensFacing: LensFacing) -> Unit = {},
    onTapToFocus: (Display, Int, Int, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onChangeZoomScale: (Float) -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onChangeAspectRatio: (AspectRatio) -> Unit = {},
    onChangeCaptureMode: (CaptureMode) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        (PreviewViewModel.ImageCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    onToastShown: () -> Unit = {}
) {
    val lensFacing = remember(previewUiState) {
        previewUiState.currentCameraSettings.cameraLensFacing
    }

    val onFlipCamera = remember(lensFacing) {
        {
            onSetLensFacing(lensFacing.flip())
        }
    }

    val scope = rememberCoroutineScope()
    val blinkState = remember { BlinkState(coroutineScope = scope) }
    // display camera feed. this stays behind everything else
    PreviewDisplay(
        onFlipCamera = onFlipCamera,
        onTapToFocus = onTapToFocus,
        onZoomChange = onChangeZoomScale,
        aspectRatio = previewUiState.currentCameraSettings.aspectRatio,
        surfaceRequest = surfaceRequest,
        blinkState = blinkState
    )

    QuickSettingsScreenOverlay(
        modifier = Modifier,
        isOpen = previewUiState.quickSettingsIsOpen,
        toggleIsOpen = onToggleQuickSettings,
        currentCameraSettings = previewUiState.currentCameraSettings,
        onLensFaceClick = onSetLensFacing,
        onFlashModeClick = onChangeFlash,
        onAspectRatioClick = onChangeAspectRatio,
        onCaptureModeClick = onChangeCaptureMode
        // onTimerClick = {}/*TODO*/
    )
    // relative-grid style overlay on top of preview display
    CameraControlsOverlay(
        previewUiState = previewUiState,
        onNavigateToSettings = onNavigateToSettings,
        previewMode = previewMode,
        onFlipCamera = onFlipCamera,
        onChangeFlash = onChangeFlash,
        onToggleQuickSettings = onToggleQuickSettings,
        onCaptureImage = onCaptureImage,
        onCaptureImageWithUri = onCaptureImageWithUri,
        onStartVideoRecording = onStartVideoRecording,
        onStopVideoRecording = onStopVideoRecording,
        blinkState = blinkState
    )

    // displays toast when there is a message to show
    if (previewUiState.toastMessageToShow != null) {
        TestableToast(
            modifier = Modifier.testTag(previewUiState.toastMessageToShow.testTag),
            toastMessage = previewUiState.toastMessageToShow,
            onToastShown = onToastShown
        )
    }

    // Screen flash overlay that stays on top of everything but invisible normally. This should
    // not be enabled based on whether screen flash is enabled because a previous image capture
    // may still be running after flash mode change and clear actions (e.g. brightness restore)
    // may need to be handled later. Compose smart recomposition should be able to optimize this
    // if the relevant states are no longer changing.
    ScreenFlashScreen(
        screenFlashUiState = screenFlashUiState,
        onInitialBrightnessCalculated = onClearUiScreenBrightness
    )
}

@Composable
private fun LoadingScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(modifier = Modifier.size(50.dp))
        Text(text = stringResource(R.string.camera_not_ready), color = Color.White)
    }
}

@Preview
@Composable
private fun ContentScreenPreview() {
    MaterialTheme {
        ContentScreen(
            previewUiState = PreviewUiState(),
            previewMode = PreviewMode.StandardMode,
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_WhileRecording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            previewUiState = PreviewUiState(
                videoRecordingState = VideoRecordingState.ACTIVE
            ),
            previewMode = PreviewMode.StandardMode,
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}
