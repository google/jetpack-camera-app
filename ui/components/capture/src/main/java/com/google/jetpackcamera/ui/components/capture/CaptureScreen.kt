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

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Range
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.tracing.Trace
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.jetpackcamera.core.camera.InitialRecordingSettings
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DebugSettings
import com.google.jetpackcamera.settings.model.DeviceRotation
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LensToZoom
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.ui.components.capture.debug.DebugOverlayComponent
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsScreenOverlay
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

private const val TAG = "PreviewScreen"

/**
 * Screen used for the Preview feature.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun CaptureScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPostCapture: () -> Unit,
    debugSettings: DebugSettings,
    modifier: Modifier = Modifier,
    onRequestWindowColorMode: (Int) -> Unit = {},
    onFirstFrameCaptureCompleted: () -> Unit = {},
    viewModel: CaptureViewModel
) {
    Log.d(TAG, "PreviewScreen")

    val captureUiState: CaptureUiState by viewModel.getCaptureUiState().collectAsState()

    val screenFlashUiState: ScreenFlashUiState
        by viewModel.getScreenFlashUiState().collectAsState()

    val surfaceRequest: SurfaceRequest?
        by viewModel.getSurfaceRequest().collectAsState()

    LifecycleStartEffect(Unit) {
        viewModel.startCamera()
        onStopOrDispose {
            viewModel.stopCamera()
        }
    }

    if (Trace.isEnabled()) {
        LaunchedEffect(onFirstFrameCaptureCompleted) {
            snapshotFlow { captureUiState }
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

    when (val currentUiState = captureUiState) {
        is CaptureUiState.NotReady -> LoadingScreen()
        is CaptureUiState.Ready -> {
            var initialRecordingSettings by remember {
                mutableStateOf<InitialRecordingSettings?>(
                    null
                )
            }

            val context = LocalContext.current
            LaunchedEffect(Unit) {
                debouncedOrientationFlow(context).collect(viewModel::setDisplayRotation)
            }
            val scope = rememberCoroutineScope()
            val zoomState = remember {
                // the initialZoomLevel must be fetched from the settings, not the cameraState.
                // since we want to reset the ZoomState on flip, the zoomstate of the cameraState may not yet be congruent with the settings

                ZoomState(
                    initialZoomLevel = (
                        currentUiState.zoomControlUiState as?
                            ZoomControlUiState.Enabled
                        )
                        ?.initialZoomRatio
                        ?: 1f,
                    onAnimateStateChanged = viewModel::setZoomAnimationState,
                    onChangeZoomLevel = viewModel::changeZoomRatio,
                    zoomRange = (currentUiState.zoomUiState as? ZoomUiState.Enabled)
                        ?.primaryZoomRange
                        ?: Range(1f, 1f)
                )
            }

            LaunchedEffect(
                (currentUiState.flipLensUiState as? FlipLensUiState.Available)
                    ?.selectedLensFacing
            ) {
                zoomState.onChangeLens(
                    newInitialZoomLevel = (
                        currentUiState.zoomControlUiState as?
                            ZoomControlUiState.Enabled
                        )
                        ?.initialZoomRatio
                        ?: 1f,
                    newZoomRange = (currentUiState.zoomUiState as? ZoomUiState.Enabled)
                        ?.primaryZoomRange
                        ?: Range(1f, 1f)
                )
            }
            // todo(kc) handle reset certain values after video recording is complete
            LaunchedEffect(currentUiState.videoRecordingState) {
                with(currentUiState.videoRecordingState) {
                    when (this) {
                        is VideoRecordingState.Starting -> {
                            initialRecordingSettings = this.initialRecordingSettings
                        }

                        is VideoRecordingState.Inactive -> {
                            initialRecordingSettings?.let {
                                val oldPrimaryLensFacing = it.lensFacing
                                val oldZoomRatios = it.zoomRatios
                                val oldAudioEnabled = it.isAudioEnabled
                                Log.d(TAG, "reset pre recording settings")
                                viewModel.setAudioEnabled(oldAudioEnabled)
                                viewModel.setLensFacing(oldPrimaryLensFacing)
                                zoomState.apply {
                                    absoluteZoom(
                                        targetZoomLevel = oldZoomRatios[oldPrimaryLensFacing] ?: 1f,
                                        lensToZoom = LensToZoom.PRIMARY
                                    )
                                    absoluteZoom(
                                        targetZoomLevel = oldZoomRatios[oldPrimaryLensFacing.flip()]
                                            ?: 1f,
                                        lensToZoom = LensToZoom.SECONDARY
                                    )
                                }
                            }
                            initialRecordingSettings = null
                        }

                        is VideoRecordingState.Active -> {}
                    }
                }
            }

            ContentScreen(
                modifier = modifier,
                captureUiState = currentUiState,
                screenFlashUiState = screenFlashUiState,
                surfaceRequest = surfaceRequest,
                onNavigateToSettings = onNavigateToSettings,
                onClearUiScreenBrightness = viewModel::setClearUiScreenBrightness,
                onSetLensFacing = viewModel::setLensFacing,
                onTapToFocus = viewModel::tapToFocus,
                onAbsoluteZoom = { zoomRatio: Float, lensToZoom: LensToZoom ->
                    scope.launch {
                        zoomState.absoluteZoom(
                            zoomRatio,
                            lensToZoom
                        )
                    }
                },
                onScaleZoom = { zoomRatio: Float, lensToZoom: LensToZoom ->
                    scope.launch {
                        zoomState.scaleZoom(
                            zoomRatio,
                            lensToZoom
                        )
                    }
                },
                onAnimateZoom = { zoomRatio: Float, lensToZoom: LensToZoom ->
                    scope.launch {
                        zoomState.animatedZoom(
                            targetZoomLevel = zoomRatio,
                            lensToZoom = lensToZoom
                        )
                    }
                },
                onIncrementZoom = { zoomRatio: Float, lensToZoom: LensToZoom ->
                    scope.launch {
                        zoomState.incrementZoom(
                            zoomRatio,
                            lensToZoom
                        )
                    }
                },

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
                onRequestWindowColorMode = onRequestWindowColorMode,
                onSnackBarResult = viewModel::onSnackBarResult,
                isDebugMode = debugSettings.isDebugModeEnabled,
                onImageWellClick = onNavigateToPostCapture
            )
            val readStoragePermission: PermissionState = rememberPermissionState(
                Manifest.permission.READ_EXTERNAL_STORAGE
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
    captureUiState: CaptureUiState.Ready,
    screenFlashUiState: ScreenFlashUiState,
    surfaceRequest: SurfaceRequest?,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onClearUiScreenBrightness: (Float) -> Unit = {},
    onSetCaptureMode: (CaptureMode) -> Unit = {},
    onSetLensFacing: (newLensFacing: LensFacing) -> Unit = {},
    onTapToFocus: (x: Float, y: Float) -> Unit = { _, _ -> },
    onAbsoluteZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onScaleZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onIncrementZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onAnimateZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onChangeFlash: (FlashMode) -> Unit = {},
    onChangeAspectRatio: (AspectRatio) -> Unit = {},
    onSetStreamConfig: (StreamConfig) -> Unit = {},
    onChangeDynamicRange: (DynamicRange) -> Unit = {},
    onChangeConcurrentCameraMode: (ConcurrentCameraMode) -> Unit = {},
    onChangeImageFormat: (ImageOutputFormat) -> Unit = {},
    onDisabledCaptureMode: (DisableRationale) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onToggleDebugOverlay: () -> Unit = {},
    onSetPause: (Boolean) -> Unit = {},
    onSetAudioEnabled: (Boolean) -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        Boolean,
        (ImageCaptureEvent, Int) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStopVideoRecording: () -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit = {},
    onRequestWindowColorMode: (Int) -> Unit = {},
    onSnackBarResult: (String) -> Unit = {},
    isDebugMode: Boolean = false,
    onImageWellClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) {
        val onFlipCamera = {
            if (captureUiState.flipLensUiState is FlipLensUiState.Available) {
                onSetLensFacing(
                    (
                        captureUiState.flipLensUiState as FlipLensUiState.Available
                        )
                        .selectedLensFacing.flip()
                )
            }
        }

        val isAudioEnabled = remember(captureUiState) {
            captureUiState.audioUiState is AudioUiState.Enabled.On
        }
        val onToggleAudio = remember(isAudioEnabled) {
            {
                onSetAudioEnabled(!isAudioEnabled)
            }
        }
        Box(modifier.fillMaxSize()) {
            // display camera feed. this stays behind everything else
            PreviewDisplay(
                previewDisplayUiState = captureUiState.previewDisplayUiState,
                onFlipCamera = onFlipCamera,
                onTapToFocus = onTapToFocus,
                onScaleZoom = { onScaleZoom(it, LensToZoom.PRIMARY) },
                surfaceRequest = surfaceRequest,
                onRequestWindowColorMode = onRequestWindowColorMode
            )

            QuickSettingsScreenOverlay(
                modifier = Modifier,
                quickSettingsUiState = captureUiState.quickSettingsUiState,
                toggleQuickSettings = onToggleQuickSettings,
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
                captureUiState = captureUiState,
                onNavigateToSettings = onNavigateToSettings,
                onSetCaptureMode = onSetCaptureMode,
                onFlipCamera = onFlipCamera,
                onChangeFlash = onChangeFlash,
                onToggleAudio = onToggleAudio,
                onAnimateZoom = { onAnimateZoom(it, LensToZoom.PRIMARY) },
                onIncrementZoom = { onIncrementZoom(it, LensToZoom.PRIMARY) },
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
                debugUiState = captureUiState.debugUiState,
                onChangeZoomRatio = { f: Float -> onAbsoluteZoom(f, LensToZoom.PRIMARY) }
            )

            val snackBarData = captureUiState.snackBarUiState.snackBarQueue.peek()
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
            captureUiState = FAKE_PREVIEW_UI_STATE_READY,
            screenFlashUiState = ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Standard_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiState = FAKE_PREVIEW_UI_STATE_READY.copy(),
            screenFlashUiState = ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_ImageOnly_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiState = FAKE_PREVIEW_UI_STATE_READY.copy(
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
            ),
            screenFlashUiState = ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_VideoOnly_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiState = FAKE_PREVIEW_UI_STATE_READY.copy(
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY)
            ),
            screenFlashUiState = ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Standard_Recording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiState = FAKE_PREVIEW_UI_STATE_PRESSED_RECORDING,
            screenFlashUiState = ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Locked_Recording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiState = FAKE_PREVIEW_UI_STATE_LOCKED_RECORDING,
            screenFlashUiState = ScreenFlashUiState(),
            surfaceRequest = null
        )
    }
}

private val FAKE_PREVIEW_UI_STATE_READY = CaptureUiState.Ready(
    videoRecordingState = VideoRecordingState.Inactive(),
    externalCaptureMode = ExternalCaptureMode.StandardMode {},
    captureModeToggleUiState = CaptureModeToggleUiState.Unavailable
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

interface CaptureViewModel {
    fun startCamera()
    fun stopCamera()
    fun setLensFacing(newLensFacing: LensFacing)
    fun setFlash(flashMode: FlashMode)
    fun setAspectRatio(aspectRatio: AspectRatio)
    fun setStreamConfig(streamConfig: StreamConfig)
    fun setAudioEnabled(shouldEnableAudio: Boolean)
    fun setPaused(shouldBePaused: Boolean)
    fun tapToFocus(x: Float, y: Float)
    fun changeZoomRatio(zoomRatio: CameraZoomRatio)
    fun setDynamicRange(dynamicRange: DynamicRange)
    fun setCaptureMode(captureMode: CaptureMode)
    fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode)
    fun setImageFormat(imageOutputFormat: ImageOutputFormat)
    fun enqueueDisabledHdrToggleSnackBar(disableRationale: DisableRationale)
    fun toggleQuickSettings()

    fun toggleDebugOverlay()
    fun captureImageWithUri(
        contentResolver: ContentResolver,
        imageCaptureUri: Uri?,
        ignoreUri: Boolean = false,
        onImageCapture: (ImageCaptureEvent, Int) -> Unit
    )
    fun startVideoRecording(
        videoCaptureUri: Uri?,
        shouldUseUri: Boolean,
        onVideoCapture: (VideoCaptureEvent) -> Unit
    )
    fun stopVideoRecording()
    fun setLockedRecording(isLocked: Boolean)
    fun setDisplayRotation(deviceRotation: DeviceRotation)
    fun updateLastCapturedMedia()
    fun onSnackBarResult(cookie: String)
    fun getSurfaceRequest(): StateFlow<SurfaceRequest?>
    fun getCaptureUiState(): StateFlow<CaptureUiState>
    fun getScreenFlashUiState(): StateFlow<ScreenFlashUiState>
    fun setClearUiScreenBrightness(brightness: Float)
}
