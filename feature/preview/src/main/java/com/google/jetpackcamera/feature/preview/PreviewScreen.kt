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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
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
    viewModel: PreviewViewModel = hiltViewModel(),
    previewMode: PreviewMode
) {
    Log.d(TAG, "PreviewScreen")

    val previewUiState: PreviewUiState by viewModel.previewUiState.collectAsState()

    val screenFlashUiState: ScreenFlash.ScreenFlashUiState
        by viewModel.screenFlash.screenFlashUiState.collectAsState()

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
        Box(
            modifier = Modifier.semantics {
                testTagsAsResourceId = true
            }
        ) {
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

            QuickSettingsScreenOverlay(
                modifier = Modifier,
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
            // relative-grid style overlay on top of preview display
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
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
                                        onFlashModeClick = viewModel::setFlash
                                    )
                                }
                            }
                            // quick settings button
                            ToggleQuickSettingsButton(
                                toggleDropDown = { viewModel.toggleQuickSettings() },
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
                                    onClick = { viewModel.toggleCaptureMode() },
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
                    ZoomScaleText(zoomScale = zoomScale)
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
                                        onClick = { viewModel.flipCamera() },
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
                                        viewModel.captureImage()
                                    }

                                    is PreviewMode.ExternalImageCaptureMode -> {
                                        viewModel.captureImage(
                                            context.contentResolver,
                                            previewMode.imageCaptureUri,
                                            previewMode.onImageCapture
                                        )
                                    }
                                }
                            }
                            if (previewUiState.quickSettingsIsOpen) {
                                viewModel.toggleQuickSettings()
                            }
                        },
                        onLongPress = {
                            viewModel.startVideoRecording()
                            if (previewUiState.quickSettingsIsOpen) {
                                viewModel.toggleQuickSettings()
                            }
                        },
                        onRelease = { viewModel.stopVideoRecording() },
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
            // displays toast when there is a message to show
            if (previewUiState.toastMessageToShow != null) {
                ShowTestableToast(
                    modifier = Modifier
                        .testTag(previewUiState.toastMessageToShow!!.testTag),
                    toastMessage = previewUiState.toastMessageToShow!!,
                    onToastShown = viewModel::onToastShown
                )
            }

            // Screen flash overlay that stays on top of everything but invisible normally. This should
            // not be enabled based on whether screen flash is enabled because a previous image capture
            // may still be running after flash mode change and clear actions (e.g. brightness restore)
            // may need to be handled later. Compose smart recomposition should be able to optimize this
            // if the relevant states are no longer changing.
            ScreenFlashScreen(
                screenFlashUiState = screenFlashUiState,
                onInitialBrightnessCalculated = viewModel.screenFlash::setClearUiScreenBrightness
            )
        }
    }
}



