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
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ChipColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.feature.preview.ui.CaptureButton
import com.google.jetpackcamera.feature.preview.ui.FlipCameraButton
import com.google.jetpackcamera.feature.preview.ui.PreviewDisplay
import com.google.jetpackcamera.feature.preview.ui.SettingsNavButton
import com.google.jetpackcamera.feature.preview.ui.ZoomScaleText
import com.google.jetpackcamera.feature.quicksettings.QuickSettingsScreen
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation

private const val TAG = "PreviewScreen"

/**
 * Screen used for the Preview feature.
 */
@Composable
fun PreviewScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    Log.d(TAG, "PreviewScreen")

    val previewUiState: PreviewUiState by viewModel.previewUiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    val deferredSurfaceProvider = remember { CompletableDeferred<SurfaceProvider>() }

    var zoomScale by remember { mutableStateOf(1f) }

    var zoomScaleShow by remember { mutableStateOf(false) }

    val zoomHandler = Handler(Looper.getMainLooper())

    val transformableState = rememberTransformableState(
        onTransformation = { zoomChange, _, _ ->
            zoomScale = viewModel.setZoomScale(zoomChange)
            zoomScaleShow = true
            zoomHandler.postDelayed({ zoomScaleShow = false }, 3000)
        })

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
        Text(text = stringResource(R.string.camera_not_ready))
    } else if (previewUiState.cameraState == CameraState.READY) {
        // display camera feed. this stays behind everything else
        PreviewDisplay(
            viewModel = viewModel,
            transformableState = transformableState,
            previewUiState = previewUiState,
            deferredSurfaceProvider = deferredSurfaceProvider
        )
        // overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            QuickSettingsScreen(
                isOpen = previewUiState.quickSettingsIsOpen,
                toggleIsOpen = { viewModel.toggleQuickSettings() },
                currentCameraSettings = previewUiState.currentCameraSettings,
                onLensFaceClick = viewModel::flipCamera,
                onFlashModeClick = viewModel::setFlash,
                onAspectRatioClick = {
                    viewModel.setAspectRatio(it)
                },
                //onTimerClick = {}/*TODO*/
            )

            SettingsNavButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                onNavigateToSettings = onNavigateToSettings
            )

                SuggestionChip(
                    onClick = { viewModel.toggleCaptureMode() },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp),
                    label = {
                        Text(
                            stringResource(
                                if (previewUiState.singleStreamCapture) {
                                    R.string.capture_mode_single_stream
                                } else {
                                    R.string.capture_mode_multi_stream
                                }
                            )
                        )
                    }
                )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ZoomScaleText(
                    zoomScale = zoomScale,
                    show = zoomScaleShow
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                ) {
                    FlipCameraButton(
                        onClick = { viewModel.flipCamera() },
                        //enable only when phone has front and rear camera
                        enabledCondition =
                        previewUiState.currentCameraSettings.back_camera_available
                                && previewUiState.currentCameraSettings.front_camera_available
                    )
                    CaptureButton(
                        onClick = { onImageCapture(viewModel) },
                        onLongPress = { onStartRecording(viewModel) },
                        onRelease = { onStopRecording(viewModel) },
                        state = previewUiState.videoRecordingState
                    )
                }

            }
        }
    }
}

fun onStartRecording(viewModel: PreviewViewModel) {
    viewModel.startVideoRecording()
}

fun onStopRecording(viewModel: PreviewViewModel) {
    viewModel.stopVideoRecording()
}

fun onImageCapture(viewModel: PreviewViewModel) {
    viewModel.captureImage()
}
