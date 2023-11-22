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

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.camera.core.Preview.SurfaceProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.feature.preview.ui.CaptureButton
import com.google.jetpackcamera.feature.preview.ui.CaptureSuccessMessage
import com.google.jetpackcamera.feature.preview.ui.FlipCameraButton
import com.google.jetpackcamera.feature.preview.ui.PreviewDisplay
import com.google.jetpackcamera.feature.preview.ui.ScreenFlash
import com.google.jetpackcamera.feature.preview.ui.SettingsNavButton
import com.google.jetpackcamera.feature.preview.ui.TestingButton
import com.google.jetpackcamera.feature.preview.ui.ZoomScaleText
import com.google.jetpackcamera.feature.quicksettings.QuickSettingsScreen
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSettingsIndicators
import com.google.jetpackcamera.settings.model.CaptureMode
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation

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
    viewModel: PreviewViewModel = hiltViewModel()
) {
    Log.d(TAG, "PreviewScreen")

    val previewUiState: PreviewUiState by viewModel.previewUiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    val deferredSurfaceProvider = remember { CompletableDeferred<SurfaceProvider>() }

    val zoomScale by remember { mutableFloatStateOf(1f) }

    var zoomScaleShow by remember { mutableStateOf(false) }

    val zoomHandler = Handler(Looper.getMainLooper())

    onPreviewViewModel(viewModel)

    LaunchedEffect(lifecycleOwner) {
        val surfaceProvider = deferredSurfaceProvider.await()
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.runCamera(surfaceProvider)
            try {
                awaitCancellation()
            } finally {
                viewModel.stopCamera()
            }
        }
    }
    if (previewUiState.cameraState == CameraState.NOT_READY) {
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
    } else if (previewUiState.cameraState == CameraState.READY) {
        // display camera feed. this stays behind everything else
        PreviewDisplay(
            onFlipCamera = viewModel::flipCamera,
            onTapToFocus = viewModel::tapToFocus,
            onZoomChange = { zoomChange: Float ->
                viewModel.setZoomScale(zoomChange)
                zoomScaleShow = true
                zoomHandler.postDelayed({ zoomScaleShow = false }, ZOOM_SCALE_SHOW_TIMEOUT_MS)
            },
            aspectRatio = previewUiState.currentCameraSettings.aspectRatio,
            deferredSurfaceProvider = deferredSurfaceProvider
        )
        // overlay
        Box(
            modifier = Modifier
                .semantics {
                    testTagsAsResourceId = true
                }
                .fillMaxSize()
        ) {
            // hide settings, quickSettings, and quick capture mode button
            when (previewUiState.videoRecordingState) {
                VideoRecordingState.ACTIVE -> {}
                VideoRecordingState.INACTIVE -> {
                    QuickSettingsScreen(
                        modifier = Modifier
                            .align(Alignment.TopCenter),
                        isOpen = previewUiState.quickSettingsIsOpen,
                        toggleIsOpen = { viewModel.toggleQuickSettings() },
                        currentCameraSettings = previewUiState.currentCameraSettings,
                        onLensFaceClick = viewModel::flipCamera,
                        onFlashModeClick = viewModel::setFlash,
                        onAspectRatioClick = {
                            viewModel.setAspectRatio(it)
                        }
                        // onTimerClick = {}/*TODO*/
                    )

                    Row(
                        modifier = Modifier
                            .align(Alignment.TopStart),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SettingsNavButton(
                            modifier = Modifier
                                .padding(12.dp),
                            onNavigateToSettings = onNavigateToSettings
                        )

                        QuickSettingsIndicators(
                            currentCameraSettings = previewUiState.currentCameraSettings,
                            onFlashModeClick = viewModel::setFlash
                        )
                    }

                    TestingButton(
                        modifier = Modifier
                            .testTag("ToggleCaptureMode")
                            .align(Alignment.TopEnd)
                            .padding(12.dp),
                        onClick = { viewModel.toggleCaptureMode() },
                        text = stringResource(
                            when (previewUiState.currentCameraSettings.captureMode) {
                                CaptureMode.SINGLE_STREAM -> R.string.capture_mode_single_stream
                                CaptureMode.MULTI_STREAM -> R.string.capture_mode_multi_stream
                            }
                        )
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                if (zoomScaleShow) {
                    ZoomScaleText(zoomScale = zoomScale)
                }
                CaptureSuccessMessage(previewUiState.captureIsSuccessful) {
                    viewModel.onCaptureSuccessMessageShown()
                }
                Row(
                    modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    when (previewUiState.videoRecordingState) {
                        VideoRecordingState.ACTIVE -> {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .weight(1f)
                            )
                        }

                        VideoRecordingState.INACTIVE -> {
                            FlipCameraButton(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { viewModel.flipCamera() },
                                // enable only when phone has front and rear camera
                                enabledCondition =
                                previewUiState.currentCameraSettings.isBackCameraAvailable &&
                                    previewUiState.currentCameraSettings.isFrontCameraAvailable
                            )
                        }
                    }
                    val multipleEventsCutter = remember { MultipleEventsCutter() }
                    /*todo: close quick settings on start record/image capture*/
                    CaptureButton(
                        onClick = {
                            multipleEventsCutter.processEvent { viewModel.captureImage() }
                        },
                        onLongPress = { viewModel.startVideoRecording() },
                        onRelease = { viewModel.stopVideoRecording() },
                        videoRecordingState = previewUiState.videoRecordingState
                    )
                    /* spacer is a placeholder to maintain the proportionate location of this row of
                     UI elements. if you want to  add another element, replace it with ONE element.
                     If you want to add multiple components, use a container (Box, Row, Column, etc.)
                     */
                    Spacer(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                    )
                }
            }
        }

        // Screen flash overlay that stays on top of everything but invisible normally
        ScreenFlash()
    }
}
