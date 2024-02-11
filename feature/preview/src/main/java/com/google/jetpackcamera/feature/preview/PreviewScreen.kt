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
import androidx.camera.core.Preview.SurfaceProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import com.google.jetpackcamera.feature.preview.ui.CAPTURE_BUTTON
import com.google.jetpackcamera.feature.preview.ui.CaptureButton
import com.google.jetpackcamera.feature.preview.ui.FlipCameraButton
import com.google.jetpackcamera.feature.preview.ui.PreviewDisplay
import com.google.jetpackcamera.feature.preview.ui.ScreenFlashScreen
import com.google.jetpackcamera.feature.preview.ui.SettingsNavButton
import com.google.jetpackcamera.feature.preview.ui.ShowTestableToast
import com.google.jetpackcamera.feature.preview.ui.StabilizationIcon
import com.google.jetpackcamera.feature.preview.ui.TestingButton
import com.google.jetpackcamera.feature.preview.ui.ZoomScaleText
import com.google.jetpackcamera.feature.quicksettings.QuickSettingsScreenOverlay
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSettingsIndicators
import com.google.jetpackcamera.feature.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

private const val TAG = "PreviewScreen"
private const val ZOOM_SCALE_SHOW_TIMEOUT_MS = 3000L

/**
 * Screen used for the Preview feature.
 */
@OptIn(ExperimentalComposeUiApi::class)
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

    val deferredSurfaceProvider = remember { CompletableDeferred<SurfaceProvider>() }
    var surfaceProvider by remember { mutableStateOf<SurfaceProvider?>(null) }

    LaunchedEffect(LocalLifecycleOwner.current) {
        surfaceProvider = deferredSurfaceProvider.await()
    }

    LifecycleStartEffect(surfaceProvider) {
        surfaceProvider?.let { viewModel.runCamera(it) }
        onStopOrDispose {
            viewModel.stopCamera()
        }
    }

    when (previewUiState.cameraState) {
        CameraState.NOT_READY -> LoadingScreen()
        CameraState.READY -> ContentScreen(
            previewUiState = previewUiState,
            onNavigateToSettings = onNavigateToSettings,
            previewMode = previewMode,
            screenFlashUiState = screenFlashUiState,
            onClearUiScreenBrightness = viewModel.screenFlash::setClearUiScreenBrightness,
            onFlipCamera = viewModel::flipCamera,
            onTapToFocus = viewModel::tapToFocus,
            onChangeZoomScale = viewModel::setZoomScale,
            onChangeFlash = viewModel::setFlash,
            onChangeAspectRatio = viewModel::setAspectRatio,
            onToggleQuickSettings = viewModel::toggleQuickSettings,
            onCaptureImage = viewModel::captureImage,
            onCaptureImageWithUri = viewModel::captureImageWithUri,
            onStartVideoRecording = viewModel::startVideoRecording,
            onStopVideoRecording = viewModel::stopVideoRecording,
            onToggleCaptureMode = viewModel::toggleCaptureMode,
            onToastShown = viewModel::onToastShown,
            onSurfaceProviderCreated = { deferredSurfaceProvider.complete(it) }
        )
    }
}

@Composable
private fun ContentScreen(
    previewUiState: PreviewUiState,
    onNavigateToSettings: () -> Unit,
    previewMode: PreviewMode,
    screenFlashUiState: ScreenFlash.ScreenFlashUiState,
    onClearUiScreenBrightness: (Float) -> Unit,
    onSurfaceProviderCreated: (SurfaceProvider) -> Unit,
    onFlipCamera: () -> Unit = {},
    onTapToFocus: (Display, Int, Int, Float, Float) -> Unit = { _, _, _, _, _ -> },
    onChangeZoomScale: (Float) -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onChangeAspectRatio: (AspectRatio) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (ContentResolver, Uri?, (PreviewViewModel.ImageCaptureEvent) -> Unit) -> Unit = { _, _, _ -> },
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    onToggleCaptureMode: () -> Unit = {},
    onToastShown: () -> Unit = {},
) {
    // display camera feed. this stays behind everything else
    PreviewDisplay(
        onFlipCamera = onFlipCamera,
        onTapToFocus = onTapToFocus,
        onZoomChange = onChangeZoomScale,
        aspectRatio = previewUiState.currentCameraSettings.aspectRatio,
        onSurfaceProviderCreated = onSurfaceProviderCreated
    )

    QuickSettingsScreenOverlay(
        modifier = Modifier,
        isOpen = previewUiState.quickSettingsIsOpen,
        toggleIsOpen = onToggleQuickSettings,
        currentCameraSettings = previewUiState.currentCameraSettings,
        onLensFaceClick = { onFlipCamera() },
        onFlashModeClick = onChangeFlash,
        onAspectRatioClick = onChangeAspectRatio
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
        onToggleCaptureMode = onToggleCaptureMode
    )

    // displays toast when there is a message to show
    if (previewUiState.toastMessageToShow != null) {
        ShowTestableToast(
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

@Composable
private fun CameraControlsOverlay(
    previewUiState: PreviewUiState,
    onNavigateToSettings: () -> Unit,
    previewMode: PreviewMode,
    onFlipCamera: () -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (ContentResolver, Uri?, (PreviewViewModel.ImageCaptureEvent) -> Unit) -> Unit = { _, _, _ -> },
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    onToggleCaptureMode: () -> Unit = {},
) {
    // Show the current zoom level for a short period of time, only when the level changes.
    var firstRun by remember { mutableStateOf(true) }
    var zoomScaleShow by remember { mutableStateOf(false) }
    LaunchedEffect(previewUiState.zoomScale) {
        if (firstRun) firstRun = false
        else {
            zoomScaleShow = true
            delay(ZOOM_SCALE_SHOW_TIMEOUT_MS)
            zoomScaleShow = false
        }
    }

    Column(Modifier.fillMaxSize()) {
        // hide settings, quickSettings, and quick capture mode button
        when (previewUiState.videoRecordingState) {
            VideoRecordingState.ACTIVE -> {}
            VideoRecordingState.INACTIVE -> {
                // 3-segmented row to keep quick settings button centered
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    // row to left of quick settings button
                    Row(
                        modifier = Modifier
                            .weight(1f),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // button to open default settings page
                        SettingsNavButton(
                            modifier = Modifier
                                .padding(12.dp),
                            onNavigateToSettings = onNavigateToSettings
                        )
                        if (!previewUiState.quickSettingsIsOpen) {
                            QuickSettingsIndicators(
                                currentCameraSettings = previewUiState
                                    .currentCameraSettings,
                                onFlashModeClick = onChangeFlash
                            )
                        }
                    }
                    // quick settings button
                    ToggleQuickSettingsButton(
                        toggleDropDown = onToggleQuickSettings,
                        isOpen = previewUiState.quickSettingsIsOpen
                    )

                    // Row to right of quick settings
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TestingButton(
                            modifier = Modifier
                                .testTag("ToggleCaptureMode"),
                            onClick = onToggleCaptureMode,
                            text = stringResource(
                                when (previewUiState.currentCameraSettings.captureMode) {
                                    CaptureMode.SINGLE_STREAM ->
                                        R.string.capture_mode_single_stream

                                    CaptureMode.MULTI_STREAM ->
                                        R.string.capture_mode_multi_stream
                                }
                            )
                        )
                        StabilizationIcon(
                            supportedStabilizationMode = previewUiState
                                .currentCameraSettings.supportedStabilizationModes,
                            videoStabilization = previewUiState
                                .currentCameraSettings.videoCaptureStabilization,
                            previewStabilization = previewUiState
                                .currentCameraSettings.previewStabilization
                        )
                    }
                }
            }
        }

        // this component places a gap in the center of the column that will push out the top
        // and bottom edges. This will also allow the addition of vertical button bars on the
        // sides of the screen
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {}

        if (zoomScaleShow) {
            ZoomScaleText(previewUiState.zoomScale)
        }

        // 3-segmented row to keep capture button centered
        Row(
            modifier =
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            when (previewUiState.videoRecordingState) {
                // hide first segment while recording in progress
                VideoRecordingState.ACTIVE -> {
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    )
                }
                // show first segment when not recording
                VideoRecordingState.INACTIVE -> {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!previewUiState.quickSettingsIsOpen) {
                            FlipCameraButton(
                                onClick = onFlipCamera,
                                // enable only when phone has front and rear camera
                                enabledCondition =
                                previewUiState
                                    .currentCameraSettings
                                    .isBackCameraAvailable &&
                                        previewUiState
                                            .currentCameraSettings
                                            .isFrontCameraAvailable
                            )
                        }
                    }
                }
            }
            val multipleEventsCutter = remember { MultipleEventsCutter() }
            val context = LocalContext.current
            CaptureButton(
                modifier = Modifier
                    .testTag(CAPTURE_BUTTON),
                onClick = {
                    multipleEventsCutter.processEvent {
                        when (previewMode) {
                            is PreviewMode.StandardMode -> {
                                onCaptureImage()
                            }

                            is PreviewMode.ExternalImageCaptureMode -> {
                                onCaptureImageWithUri(
                                    context.contentResolver,
                                    previewMode.imageCaptureUri,
                                    previewMode.onImageCapture
                                )
                            }
                        }
                    }
                    if (previewUiState.quickSettingsIsOpen) {
                        onToggleQuickSettings()
                    }
                },
                onLongPress = {
                    onStartVideoRecording()
                    if (previewUiState.quickSettingsIsOpen) {
                        onToggleQuickSettings()
                    }
                },
                onRelease = { onStopVideoRecording() },
                videoRecordingState = previewUiState.videoRecordingState
            )
            // You can replace this row so long as the weight of the component is 1f to
            // ensure the capture button remains centered.
            Row(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(1f)
            ) {
                /*TODO("Place other components here") */
            }
        }
    }
}

@Preview
@Composable
private fun ContentScreenPreview() {
    MaterialTheme {
        ContentScreen(
            previewUiState = PreviewUiState(),
            onNavigateToSettings = {},
            previewMode = PreviewMode.StandardMode,
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            onClearUiScreenBrightness = {},
            onSurfaceProviderCreated = {}
        )
    }
}

