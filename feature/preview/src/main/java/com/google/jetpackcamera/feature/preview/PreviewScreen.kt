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
package com.google.jetpackcamera.feature.preview

import android.util.Log
import androidx.camera.core.SurfaceRequest
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tracing.Trace
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.ImageCaptureEvent
import com.google.jetpackcamera.model.LensToZoom
import com.google.jetpackcamera.model.VideoCaptureEvent
import com.google.jetpackcamera.ui.components.capture.AmplitudeToggleButton
import com.google.jetpackcamera.ui.components.capture.CameraSessionEffect
import com.google.jetpackcamera.ui.components.capture.CaptureScaffold
import com.google.jetpackcamera.ui.components.capture.CaptureScaffoldDefaults
import com.google.jetpackcamera.ui.components.capture.CaptureScaffoldLayout
import com.google.jetpackcamera.ui.components.capture.LOADING_CAPTURE_UI_STATE_SKELETON
import com.google.jetpackcamera.ui.components.capture.PauseResumeToggleButton
import com.google.jetpackcamera.ui.debug.DebugOverlay
import com.google.jetpackcamera.ui.debug.DebugUiState
import com.google.jetpackcamera.ui.uistate.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.coroutines.flow.transformWhile

private const val TAG = "PreviewScreen"

/**
 * Screen used for the Preview feature.
 *
 * @param onRequiresImmersiveChange invoked when the capture layout starts or stops requiring the
 * navigation bar to be hidden. See [CaptureScaffoldLayout].
 */
@Composable
fun PreviewScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPostCapture: () -> Unit,
    onCaptureEvent: (CaptureEvent) -> Unit,
    modifier: Modifier = Modifier,
    onRequestWindowColorMode: (Int) -> Unit = {},
    onFirstFrameCaptureCompleted: () -> Unit = {},
    onCloseCamera: () -> Unit = {},
    onRequiresImmersiveChange: (Boolean) -> Unit = {},
    viewModel: PreviewViewModel = hiltViewModel()
) {
    Log.d(TAG, "PreviewScreen")

    val rawUiState = viewModel.captureUiState.collectAsState()
    val debugUiState: DebugUiState by viewModel.debugUiState.collectAsState()
    val snackBarUiState: SnackBarUiState by viewModel.snackBarUiState.collectAsState()

    val surfaceRequest: SurfaceRequest?
        by viewModel.surfaceRequest.collectAsState()

    CameraSessionEffect(
        cameraController = viewModel.cameraController,
        enabled = true,
        onSessionStarted = viewModel::startLocationUpdates,
        onSessionStopped = viewModel::stopLocationUpdates
    )

    val currentOnCaptureEvent by rememberUpdatedState(onCaptureEvent)
    LaunchedEffect(Unit) {
        for (event in viewModel.captureEvents) {
            currentOnCaptureEvent(event)
            if (event is ImageCaptureEvent.SingleImageCached ||
                event is VideoCaptureEvent.VideoCached
            ) {
                onNavigateToPostCapture()
            }
        }
    }

    if (Trace.isEnabled()) {
        LaunchedEffect(onFirstFrameCaptureCompleted) {
            snapshotFlow { rawUiState.value }
                .transformWhile {
                    var continueCollecting = true
                    (it as? CaptureUiState.Ready)?.let { ready ->
                        if (ready.sessionFirstFrameTimestamp > 0) {
                            emit(Unit)
                            continueCollecting = false
                        }
                    }
                    continueCollecting
                }.collect {
                    onFirstFrameCaptureCompleted()
                }
        }
    }

    val readyStateProvider: () -> CaptureUiState.Ready = remember {
        {
            (rawUiState.value as? CaptureUiState.Ready) ?: LOADING_CAPTURE_UI_STATE_SKELETON
        }
    }

    val debugController = viewModel.debugController
    val currentDebugUiState = debugUiState
    val isDebugHidingComponents =
        (currentDebugUiState as? DebugUiState.Enabled)?.debugHidingComponents == true

    val externalCaptureModeState = remember {
        derivedStateOf { readyStateProvider().externalCaptureMode }
    }
    val imageWellState = remember {
        derivedStateOf { readyStateProvider().imageWellUiState }
    }
    val audioState = remember {
        derivedStateOf { readyStateProvider().audioUiState }
    }
    val videoRecordingState = remember {
        derivedStateOf { readyStateProvider().videoRecordingState }
    }

    CaptureScaffold(
        captureUiState = readyStateProvider,
        surfaceRequest = surfaceRequest,
        modifier = modifier,
        onNavigateToSettings = onNavigateToSettings,
        onRequestWindowColorMode = onRequestWindowColorMode,
        onRequiresImmersiveChange = onRequiresImmersiveChange,
        controlsVisible = !isDebugHidingComponents,
        snackBarUiState = snackBarUiState,
        cameraController = viewModel.cameraController,
        captureController = viewModel.captureController,
        quickSettingsController = viewModel.quickSettingsController,
        zoomController = viewModel.zoomController,
        snackBarController = viewModel.snackBarController,
        screenFlashController = viewModel.screenFlashController,
        imageWell = { imageWellModifier ->
            if (externalCaptureModeState.value == ExternalCaptureMode.Standard) {
                CaptureScaffoldDefaults.ImageWell(
                    uiState = imageWellState.value,
                    onClick = onNavigateToPostCapture,
                    modifier = imageWellModifier
                )
            }
        },
        errorDialog = { error, onDismiss ->
            CaptureScaffoldDefaults.ErrorDialog(
                error = error,
                onDismiss = {
                    onDismiss()
                    onCloseCamera()
                }
            )
        },
        overlay = { slotModifier ->
            if (currentDebugUiState is DebugUiState.Enabled) {
                DebugOverlay(
                    modifier = slotModifier,
                    onChangeZoomRatio = { ratio ->
                        setAbsoluteZoom(ratio, LensToZoom.PRIMARY)
                    },
                    debugUiState = currentDebugUiState,
                    {
                        val currentAudioState = audioState.value
                        AmplitudeToggleButton(
                            audioUiState = currentAudioState,
                            onToggleAudio = {
                                viewModel.captureController.setAudioEnabled(
                                    currentAudioState !is AudioUiState.Enabled.On
                                )
                            }
                        )
                    },
                    {
                        PauseResumeToggleButton(
                            onSetPause = viewModel.captureController::setPaused,
                            currentRecordingStateProvider = { videoRecordingState.value }
                        )
                    },
                    debugController = debugController
                )
            }
        }
    )
}
