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

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Range
import androidx.camera.core.SurfaceRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.tracing.Trace
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.jetpackcamera.core.camera.InitialRecordingSettings
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.feature.preview.quicksettings.QuickSettingsBottomSheet
import com.google.jetpackcamera.feature.preview.quicksettings.QuickSettingsScreenOverlay
import com.google.jetpackcamera.feature.preview.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.feature.preview.ui.AmplitudeVisualizer
import com.google.jetpackcamera.feature.preview.ui.CaptureButton
import com.google.jetpackcamera.feature.preview.ui.CaptureModeToggleButton
import com.google.jetpackcamera.feature.preview.ui.FlipCameraButton
import com.google.jetpackcamera.feature.preview.ui.ImageWell
import com.google.jetpackcamera.feature.preview.ui.PreviewDisplay
import com.google.jetpackcamera.feature.preview.ui.PreviewLayout
import com.google.jetpackcamera.feature.preview.ui.ScreenFlashScreen
import com.google.jetpackcamera.feature.preview.ui.SettingsNavButton
import com.google.jetpackcamera.feature.preview.ui.TestableSnackbar
import com.google.jetpackcamera.feature.preview.ui.ZoomLevelDisplayState
import com.google.jetpackcamera.feature.preview.ui.ZoomRatioText
import com.google.jetpackcamera.feature.preview.ui.debouncedOrientationFlow
import com.google.jetpackcamera.feature.preview.ui.debug.DebugOverlayComponent
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DebugSettings
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LensToZoom
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.ui.components.capture.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.PreviewMode
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

private const val TAG = "PreviewScreen"

/**
 * Screen used for the Preview feature.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PreviewScreen(
    onNavigateToSettings: () -> Unit,
    onNavigateToPostCapture: () -> Unit,
    externalCaptureMode: ExternalCaptureMode,
    debugSettings: DebugSettings,
    modifier: Modifier = Modifier,
    onRequestWindowColorMode: (Int) -> Unit = {},
    onFirstFrameCaptureCompleted: () -> Unit = {},
    viewModel: PreviewViewModel = hiltViewModel<PreviewViewModel, PreviewViewModel.Factory>
    { factory -> factory.create(externalCaptureMode, debugSettings) }
) {
    Log.d(TAG, "PreviewScreen")

    val captureUiState: CaptureUiState by viewModel.captureUiState.collectAsState()

    val screenFlashUiState: ScreenFlashUiState
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
                onClearUiScreenBrightness = viewModel.screenFlash::setClearUiScreenBrightness,
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

@OptIn(ExperimentalMaterial3Api::class)
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
        (PreviewViewModel.ImageCaptureEvent, Int) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (PreviewViewModel.VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStopVideoRecording: () -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit = {},
    onRequestWindowColorMode: (Int) -> Unit = {},
    onSnackBarResult: (String) -> Unit = {},
    isDebugMode: Boolean = false,
    onImageWellClick: () -> Unit = {}
) {
    val snackbarHostState = remember { SnackbarHostState() }

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

    PreviewLayout(
        modifier = Modifier,
        snackbarHostState = snackbarHostState,
        viewfinder = {
            PreviewDisplay(
                previewDisplayUiState = captureUiState.previewDisplayUiState,
                onFlipCamera = onFlipCamera,
                onTapToFocus = onTapToFocus,
                onScaleZoom = { onScaleZoom(it, LensToZoom.PRIMARY) },
                surfaceRequest = surfaceRequest,
                onRequestWindowColorMode = onRequestWindowColorMode
            )
        },
        captureButton = {
            CaptureButton(
                captureButtonUiState = captureUiState.captureButtonUiState,
                externalCaptureMode = captureUiState.externalCaptureMode,
                isQuickSettingsOpen = (captureUiState.quickSettingsUiState as? QuickSettingsUiState.Available)?.quickSettingsIsOpen
                    ?: false,
                onCaptureImageWithUri = onCaptureImageWithUri,
                onIncrementZoom = { targetZoom ->
                    onIncrementZoom(targetZoom, LensToZoom.PRIMARY)
                },
                onToggleQuickSettings = onToggleQuickSettings,
                onStartVideoRecording = onStartVideoRecording,
                onStopVideoRecording = onStopVideoRecording,
                onLockVideoRecording = onLockVideoRecording
            )
        },
        flipCameraButton = {
            FlipCameraButton(
                modifier = Modifier.testTag(FLIP_CAMERA_BUTTON),
                onClick = onFlipCamera,
                flipLensUiState = captureUiState.flipLensUiState,
                // enable only when phone has front and rear camera
                enabledCondition = when (val flipLensUiState = captureUiState.flipLensUiState) {
                    is FlipLensUiState.Available -> flipLensUiState.availableLensFacings.size > 1
                    FlipLensUiState.Unavailable -> false
                }
            )
        },
        zoomLevelDisplay = {
            val zoomLevelDisplayState = remember { ZoomLevelDisplayState(isDebugMode) }
            var firstRun by remember { mutableStateOf(true) }
            LaunchedEffect(captureUiState.zoomUiState) {
                if (firstRun) {
                    firstRun = false
                } else {
                    zoomLevelDisplayState.showZoomLevel()
                }
            }

            AnimatedVisibility(
                visible = (zoomLevelDisplayState.showZoomLevel && captureUiState.zoomUiState is ZoomUiState.Enabled),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                ZoomRatioText(
                    modifier = it,
                    zoomUiState = captureUiState.zoomUiState as ZoomUiState.Enabled
                )
            }
        },
        settingsButton = {
            SettingsNavButton(
                modifier = it
                    .padding(12.dp)
                    .testTag(SETTINGS_BUTTON),
                onNavigateToSettings = onNavigateToSettings
            )
        },
        quickSettingsButton = {
            ToggleQuickSettingsButton(
                modifier = it,
                toggleDropDown = onToggleQuickSettings,
                isOpen = (
                        captureUiState.quickSettingsUiState
                                as QuickSettingsUiState.Available
                        ).quickSettingsIsOpen
            )
        },
        imageWellButton = {
            if (!(captureUiState.quickSettingsUiState as QuickSettingsUiState.Available)
                    .quickSettingsIsOpen &&
                captureUiState.externalCaptureMode is ExternalCaptureMode.StandardMode
            ) {
                ImageWell(
                    modifier = it,
                    imageWellUiState = captureUiState.imageWellUiState,
                    onClick = onImageWellClick
                )
            }
        },
        flashModeButton = {},
        audioToggleButton = {
            AmplitudeVisualizer(
                modifier = it,
                onToggleAudio = onToggleAudio,
                audioUiState = captureUiState.audioUiState
            )
        },
        captureModeToggle = {
            if (captureUiState.captureModeToggleUiState is CaptureModeToggleUiState.Available) {
                CaptureModeToggleButton(
                    uiState = captureUiState.captureModeToggleUiState
                            as CaptureModeToggleUiState.Available,
                    onChangeCaptureMode = onSetCaptureMode,
                    onToggleWhenDisabled = onDisabledCaptureMode,
                    modifier = it.testTag(CAPTURE_MODE_TOGGLE_BUTTON)
                )
            }
        },
        quickSettingsOverlay = {
            QuickSettingsBottomSheet(
                modifier = it,
                quickSettingsUiState = captureUiState.quickSettingsUiState,
                toggleQuickSettings = onToggleQuickSettings,
                onLensFaceClick = onSetLensFacing,
                onFlashModeClick = onChangeFlash,
                onAspectRatioClick = onChangeAspectRatio,
                onStreamConfigClick = onSetStreamConfig,
                onDynamicRangeClick = onChangeDynamicRange,
                onImageOutputFormatClick = onChangeImageFormat,
                onConcurrentCameraModeClick = onChangeConcurrentCameraMode,
                onCaptureModeClick = onSetCaptureMode,
            )
        },
        debugOverlay = {
            DebugOverlayComponent(
                toggleIsOpen = onToggleDebugOverlay,
                debugUiState = captureUiState.debugUiState,
                onChangeZoomRatio = { f: Float -> onAbsoluteZoom(f, LensToZoom.PRIMARY) }
            )
        },
        screenFlashOverlay = {
            // Screen flash overlay that stays on top of everything but invisible normally. This should
            // not be enabled based on whether screen flash is enabled because a previous image capture
            // may still be running after flash mode change and clear actions (e.g. brightness restore)
            // may need to be handled later. Compose smart recomposition should be able to optimize this
            // if the relevant states are no longer changing.
            ScreenFlashScreen(
                screenFlashUiState = screenFlashUiState,
                onInitialBrightnessCalculated = onClearUiScreenBrightness
            )
        },
        snackBar = {
            val snackBarData = captureUiState.snackBarUiState.snackBarQueue.peek()
            if (snackBarData != null) {
                TestableSnackbar(
                    modifier = Modifier.testTag(snackBarData.testTag),
                    snackbarToShow = snackBarData,
                    snackbarHostState = snackbarHostState,
                    onSnackbarResult = onSnackBarResult
                )
            }
        },
    )

    /*Box(modifier.fillMaxSize()) {
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
    }*/
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
