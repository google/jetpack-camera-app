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

import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.tracing.Trace
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.feature.preview.quicksettings.QuickSettingsScreenOverlay
import com.google.jetpackcamera.feature.preview.ui.CameraControlsOverlay
import com.google.jetpackcamera.feature.preview.ui.PreviewDisplay
import com.google.jetpackcamera.feature.preview.ui.ScreenFlashScreen
import com.google.jetpackcamera.feature.preview.ui.TestableSnackbar
import com.google.jetpackcamera.feature.preview.ui.TestableToast
import com.google.jetpackcamera.feature.preview.ui.ZoomLevelDisplayState
import com.google.jetpackcamera.feature.preview.ui.debouncedOrientationFlow
import com.google.jetpackcamera.feature.preview.ui.debug.DebugOverlayComponent
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraZoomRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import kotlinx.coroutines.flow.transformWhile

private const val TAG = "PreviewScreen"

/**
 * Screen used for the Preview feature.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PreviewScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPostCapture: () -> Unit,
    previewMode: PreviewMode,
    isDebugMode: Boolean,
    modifier: Modifier = Modifier,
    onRequestWindowColorMode: (Int) -> Unit = {},
    onFirstFrameCaptureCompleted: () -> Unit = {},
    viewModel: PreviewViewModel = hiltViewModel<PreviewViewModel, PreviewViewModel.Factory>
        { factory -> factory.create(previewMode, isDebugMode) }
) {
    Log.d(TAG, "PreviewScreen")

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

    if (Trace.isEnabled()) {
        LaunchedEffect(onFirstFrameCaptureCompleted) {
            snapshotFlow { previewUiState }
                .transformWhile {
                    var continueCollecting = true
                    (it as? PreviewUiState.Ready)?.let { ready ->
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

    when (val currentUiState = previewUiState) {
        is PreviewUiState.NotReady -> LoadingScreen()
        is PreviewUiState.Ready -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                debouncedOrientationFlow(context).collect(viewModel::setDisplayRotation)
            }

            ContentScreen(
                modifier = modifier,
                previewUiState = currentUiState,
                screenFlashUiState = screenFlashUiState,
                surfaceRequest = surfaceRequest,
                onNavigateToSettings = onNavigateToSettings,
                onClearUiScreenBrightness = viewModel.screenFlash::setClearUiScreenBrightness,
                onSetLensFacing = viewModel::setLensFacing,
                onTapToFocus = viewModel::tapToFocus,
                onChangeZoomRatio = viewModel::changeZoomRatio,
                onSetCaptureMode = viewModel::setCaptureMode,
                onChangeFlash = viewModel::setFlash,
                onChangeAspectRatio = viewModel::setAspectRatio,
                onSetStreamConfig = viewModel::setStreamConfig,
                onChangeDynamicRange = viewModel::setDynamicRange,
                onChangeConcurrentCameraMode = viewModel::setConcurrentCameraMode,
                onChangeImageFormat = viewModel::setImageFormat,
                onDisabledCaptureMode = viewModel::enqueueDisabledHdrToggleSnackBar,
                onToggleQuickSettings = viewModel::toggleQuickSettings,
                onToggleDebugOverlay = viewModel::toggleDebugOverlay,
                onSetPause = viewModel::setPaused,
                onSetAudioEnabled = viewModel::setAudioEnabled,
                onCaptureImageWithUri = viewModel::captureImageWithUri,
                onStartVideoRecording = viewModel::startVideoRecording,
                onStopVideoRecording = viewModel::stopVideoRecording,
                onLockVideoRecording = viewModel::setLockedRecording,
                onToastShown = viewModel::onToastShown,
                onRequestWindowColorMode = onRequestWindowColorMode,
                onSnackBarResult = viewModel::onSnackBarResult,
                isDebugMode = isDebugMode,
                onImageWellClick = onNavigateToPostCapture
            )
            val readStoragePermission: PermissionState = rememberPermissionState(
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )

            LaunchedEffect(readStoragePermission.status) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
                    readStoragePermission.status.isGranted
                ) {
                    viewModel.updateLastCapturedMedia()
                }
            }
        }
    }
}

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
private fun ContentScreen(
    previewUiState: PreviewUiState.Ready,
    screenFlashUiState: ScreenFlash.ScreenFlashUiState,
    surfaceRequest: SurfaceRequest?,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onClearUiScreenBrightness: (Float) -> Unit = {},
    onSetCaptureMode: (CaptureMode) -> Unit = {},
    onSetLensFacing: (newLensFacing: LensFacing) -> Unit = {},
    onTapToFocus: (x: Float, y: Float) -> Unit = { _, _ -> },
    onChangeZoomRatio: (CameraZoomRatio) -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onChangeAspectRatio: (AspectRatio) -> Unit = {},
    onSetStreamConfig: (StreamConfig) -> Unit = {},
    onChangeDynamicRange: (DynamicRange) -> Unit = {},
    onChangeConcurrentCameraMode: (ConcurrentCameraMode) -> Unit = {},
    onChangeImageFormat: (ImageOutputFormat) -> Unit = {},
    onDisabledCaptureMode: (DisabledReason) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onToggleDebugOverlay: () -> Unit = {},
    onSetPause: (Boolean) -> Unit = {},
    onSetAudioEnabled: (Boolean) -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        Boolean,
        (PreviewViewModel.ImageCaptureEvent, Int) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (PreviewViewModel.VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStopVideoRecording: () -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit = {},
    onToastShown: () -> Unit = {},
    onRequestWindowColorMode: (Int) -> Unit = {},
    onSnackBarResult: (String) -> Unit = {},
    isDebugMode: Boolean = false,
    onImageWellClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        val lensFacing by rememberUpdatedState(
            previewUiState.currentCameraSettings.cameraLensFacing
        )

        val onFlipCamera = { onSetLensFacing(lensFacing.flip()) }

        val isAudioEnabled = remember(previewUiState) {
            previewUiState.currentCameraSettings.audioEnabled
        }
        val onToggleAudio = remember(isAudioEnabled) {
            {
                onSetAudioEnabled(!isAudioEnabled)
            }
        }

        Box(modifier.fillMaxSize()) {
            // display camera feed. this stays behind everything else
            PreviewDisplay(
                previewUiState = previewUiState,
                onFlipCamera = onFlipCamera,
                onTapToFocus = onTapToFocus,
                onZoomRatioChange = onChangeZoomRatio,
                aspectRatio = previewUiState.currentCameraSettings.aspectRatio,
                surfaceRequest = surfaceRequest,
                onRequestWindowColorMode = onRequestWindowColorMode
            )

            QuickSettingsScreenOverlay(
                modifier = Modifier,
                previewUiState = previewUiState,
                isOpen = previewUiState.quickSettingsIsOpen,
                toggleQuickSettings = onToggleQuickSettings,
                currentCameraSettings = previewUiState.currentCameraSettings,
                onLensFaceClick = onSetLensFacing,
                onFlashModeClick = onChangeFlash,
                onAspectRatioClick = onChangeAspectRatio,
                onStreamConfigClick = onSetStreamConfig,
                onDynamicRangeClick = onChangeDynamicRange,
                onImageOutputFormatClick = onChangeImageFormat,
                onConcurrentCameraModeClick = onChangeConcurrentCameraMode,
                onCaptureModeClick = onSetCaptureMode
            )
            // relative-grid style overlay on top of preview display
            CameraControlsOverlay(
                previewUiState = previewUiState,
                onNavigateToSettings = onNavigateToSettings,
                onSetCaptureMode = onSetCaptureMode,
                onFlipCamera = onFlipCamera,
                onChangeFlash = onChangeFlash,
                onToggleAudio = onToggleAudio,
                onSetZoom = onChangeZoomRatio,
                onToggleQuickSettings = onToggleQuickSettings,
                onToggleDebugOverlay = onToggleDebugOverlay,
                onChangeImageFormat = onChangeImageFormat,
                onDisabledCaptureMode = onDisabledCaptureMode,
                onSetPause = onSetPause,
                onCaptureImageWithUri = onCaptureImageWithUri,
                onStartVideoRecording = onStartVideoRecording,
                onStopVideoRecording = onStopVideoRecording,
                zoomLevelDisplayState = remember { ZoomLevelDisplayState(isDebugMode) },
                onImageWellClick = onImageWellClick,
                onLockVideoRecording = onLockVideoRecording
            )

            DebugOverlayComponent(
                toggleIsOpen = onToggleDebugOverlay,
                previewUiState = previewUiState,
                onChangeZoomRatio = onChangeZoomRatio
            )

            // displays toast when there is a message to show
            if (previewUiState.toastMessageToShow != null) {
                TestableToast(
                    modifier = Modifier.testTag(previewUiState.toastMessageToShow.testTag),
                    toastMessage = previewUiState.toastMessageToShow,
                    onToastShown = onToastShown
                )
            }

            val snackBarData = previewUiState.snackBarQueue.peek()
            if (snackBarData != null) {
                TestableSnackbar(
                    modifier = Modifier.testTag(snackBarData.testTag),
                    snackbarToShow = snackBarData,
                    snackbarHostState = snackbarHostState,
                    onSnackbarResult = onSnackBarResult
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
    }
}

@Composable
private fun LoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
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
            previewUiState = FAKE_PREVIEW_UI_STATE_READY,
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Standard_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            previewUiState = FAKE_PREVIEW_UI_STATE_READY.copy(),
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_ImageOnly_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            previewUiState = FAKE_PREVIEW_UI_STATE_READY.copy(
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
            ),
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_VideoOnly_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            previewUiState = FAKE_PREVIEW_UI_STATE_READY.copy(
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY)
            ),
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Standard_Recording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            previewUiState = FAKE_PREVIEW_UI_STATE_PRESSED_RECORDING,
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Locked_Recording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            previewUiState = FAKE_PREVIEW_UI_STATE_LOCKED_RECORDING,
            screenFlashUiState = ScreenFlash.ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

private val FAKE_PREVIEW_UI_STATE_READY = PreviewUiState.Ready(
    currentCameraSettings = DEFAULT_CAMERA_APP_SETTINGS,
    videoRecordingState = VideoRecordingState.Inactive(),
    systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
    previewMode = PreviewMode.StandardMode {},
    captureModeToggleUiState = CaptureModeUiState.Unavailable
)

private val FAKE_PREVIEW_UI_STATE_PRESSED_RECORDING = FAKE_PREVIEW_UI_STATE_READY.copy(
    videoRecordingState = VideoRecordingState.Active.Recording(0, 0.0, 0),
    captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording,
    audioUiState = AudioUiState.Enabled.On(1.0)
)

private val FAKE_PREVIEW_UI_STATE_LOCKED_RECORDING = FAKE_PREVIEW_UI_STATE_READY.copy(
    videoRecordingState = VideoRecordingState.Active.Recording(0, 0.0, 0),
    captureButtonUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording,
    audioUiState = AudioUiState.Enabled.On(1.0)
)
