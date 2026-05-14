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

import android.Manifest
import android.os.Build
import android.util.Log
import android.util.Range
import androidx.camera.core.SurfaceRequest
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
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
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.ImageCaptureEvent
import com.google.jetpackcamera.model.LensToZoom
import com.google.jetpackcamera.model.VideoCaptureEvent
import com.google.jetpackcamera.ui.components.capture.AmplitudeToggleButton
import com.google.jetpackcamera.ui.components.capture.CAPTURE_MODE_TOGGLE_BUTTON
import com.google.jetpackcamera.ui.components.capture.CaptureButton
import com.google.jetpackcamera.ui.components.capture.CaptureModeToggleButton
import com.google.jetpackcamera.ui.components.capture.ELAPSED_TIME_TAG
import com.google.jetpackcamera.ui.components.capture.ElapsedTimeText
import com.google.jetpackcamera.ui.components.capture.FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.FlipCameraButton
import com.google.jetpackcamera.ui.components.capture.ImageWell
import com.google.jetpackcamera.ui.components.capture.PauseResumeToggleButton
import com.google.jetpackcamera.ui.components.capture.PreviewDisplay
import com.google.jetpackcamera.ui.components.capture.PreviewLayout
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.ScreenFlashScreen
import com.google.jetpackcamera.ui.components.capture.StabilizationIcon
import com.google.jetpackcamera.ui.components.capture.TestableSnackbar
import com.google.jetpackcamera.ui.components.capture.VIDEO_QUALITY_TAG
import com.google.jetpackcamera.ui.components.capture.VideoQualityIcon
import com.google.jetpackcamera.ui.components.capture.ZoomButtonRow
import com.google.jetpackcamera.ui.components.capture.ZoomStateManager
import com.google.jetpackcamera.ui.components.capture.debouncedOrientationFlow
import com.google.jetpackcamera.ui.components.capture.debug.DebugOverlay
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsBottomSheet
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashModeIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.HdrIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.ui.controller.CameraController
import com.google.jetpackcamera.ui.controller.CaptureController
import com.google.jetpackcamera.ui.controller.ImageWellController
import com.google.jetpackcamera.ui.controller.ScreenFlashController
import com.google.jetpackcamera.ui.controller.SnackBarController
import com.google.jetpackcamera.ui.controller.debug.DebugController
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
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
    onCaptureEvent: (CaptureEvent) -> Unit,
    modifier: Modifier = Modifier,
    onRequestWindowColorMode: (Int) -> Unit = {},
    onFirstFrameCaptureCompleted: () -> Unit = {},
    viewModel: PreviewViewModel = hiltViewModel()
) {
    Log.d(TAG, "PreviewScreen")

    val captureUiState: CaptureUiState by viewModel.captureUiState.collectAsState()
    val debugUiState: DebugUiState by viewModel.debugUiState.collectAsState()
    val snackBarUiState: SnackBarUiState by viewModel.snackBarUiState.collectAsState()

    val screenFlashUiState =
        (captureUiState as? CaptureUiState.Ready)?.screenFlashUiState ?: ScreenFlashUiState()

    val surfaceRequest: SurfaceRequest?
        by viewModel.surfaceRequest.collectAsState()

    LifecycleStartEffect(Unit) {
        viewModel.cameraController.startCamera()
        onStopOrDispose {
            viewModel.cameraController.stopCamera()
        }
    }

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
                debouncedOrientationFlow(context).collect(
                    viewModel.cameraController::setDisplayRotation
                )
            }
            val scope = rememberCoroutineScope()
            val zoomStateManager = remember {
                // the initialZoomLevel must be fetched from the settings, not the cameraState.
                // since we want to reset the ZoomState on flip, the zoomstate of the cameraState
                // may not yet be congruent with the settings

                ZoomStateManager(
                    initialZoomLevel = (
                        currentUiState.zoomControlUiState as?
                            ZoomControlUiState.Enabled
                        )
                        ?.initialZoomRatio
                        ?: 1f,
                    zoomRange = (currentUiState.zoomUiState as? ZoomUiState.Enabled)
                        ?.primaryZoomRange
                        ?: Range(1f, 1f),
                    zoomController = viewModel.zoomController
                )
            }

            LaunchedEffect(
                (currentUiState.flipLensUiState as? FlipLensUiState.Available)
                    ?.selectedLensFacing
            ) {
                zoomStateManager.onChangeLens(
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
                                viewModel.captureController.setAudioEnabled(oldAudioEnabled)
                                viewModel.quickSettingsController.setLensFacing(
                                    oldPrimaryLensFacing
                                )
                                zoomStateManager.apply {
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

                onAbsoluteZoom = { zoomRatio: Float, lensToZoom: LensToZoom ->
                    scope.launch {
                        zoomStateManager.absoluteZoom(
                            zoomRatio,
                            lensToZoom
                        )
                    }
                },
                onScaleZoom = { zoomRatio: Float, lensToZoom: LensToZoom ->
                    scope.launch {
                        zoomStateManager.scaleZoom(
                            zoomRatio,
                            lensToZoom
                        )
                    }
                },
                onAnimateZoom = { zoomRatio: Float, lensToZoom: LensToZoom ->
                    scope.launch {
                        zoomStateManager.animatedZoom(
                            targetZoomLevel = zoomRatio,
                            lensToZoom = lensToZoom
                        )
                    }
                },
                onIncrementZoom = { zoomRatio: Float, lensToZoom: LensToZoom ->
                    scope.launch {
                        zoomStateManager.incrementZoom(
                            zoomRatio,
                            lensToZoom
                        )
                    }
                },
                onRequestWindowColorMode = onRequestWindowColorMode,
                onNavigatePostCapture = onNavigateToPostCapture,
                debugUiState = debugUiState,
                snackBarUiState = snackBarUiState,
                debugController = viewModel.debugController,
                snackBarController = viewModel.snackBarController,
                quickSettingsController = viewModel.quickSettingsController,
                captureController = viewModel.captureController,
                imageWellController = viewModel.imageWellController,
                cameraController = viewModel.cameraController,
                screenFlashController = viewModel.screenFlashController
            )
            val readStoragePermission: PermissionState = rememberPermissionState(
                Manifest.permission.READ_EXTERNAL_STORAGE
            )

            LaunchedEffect(readStoragePermission.status) {
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
                    readStoragePermission.status.isGranted
                ) {
                    viewModel.imageWellController.updateLastCapturedMedia()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentScreen(
    captureUiState: CaptureUiState.Ready,
    screenFlashUiState: ScreenFlashUiState,
    surfaceRequest: SurfaceRequest?,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onAbsoluteZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onScaleZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onIncrementZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onAnimateZoom: (Float, LensToZoom) -> Unit = { _, _ -> },
    onRequestWindowColorMode: (Int) -> Unit = {},
    onNavigatePostCapture: () -> Unit = {},
    debugUiState: DebugUiState = DebugUiState.Disabled,
    snackBarUiState: SnackBarUiState = SnackBarUiState.Disabled,
    debugController: DebugController? = null,
    quickSettingsController: QuickSettingsController? = null,
    snackBarController: SnackBarController? = null,
    captureController: CaptureController? = null,
    imageWellController: ImageWellController? = null,
    cameraController: CameraController? = null,
    screenFlashController: ScreenFlashController? = null
) {
    val onFlipCamera = remember(captureUiState.flipLensUiState, quickSettingsController) {
        {
            if (captureUiState.flipLensUiState is FlipLensUiState.Available) {
                quickSettingsController?.setLensFacing(
                    (captureUiState.flipLensUiState as FlipLensUiState.Available).selectedLensFacing.flip()
                )
            }
        }
    }

    val isAudioEnabled = remember(captureUiState) {
        captureUiState.audioUiState is AudioUiState.Enabled.On
    }
    val onToggleAudio: () -> Unit = remember(isAudioEnabled) {
        {
            captureController?.setAudioEnabled(!isAudioEnabled)
        }
    }

    // Hoist state for lambdas to avoid recomposition
    val currentHdrUiState = rememberUpdatedState(captureUiState.hdrUiState)
    val currentFlashModeUiState = rememberUpdatedState(captureUiState.flashModeUiState)
    val currentVideoQuality = rememberUpdatedState(captureUiState.videoQuality)
    val currentStabilizationUiState = rememberUpdatedState(captureUiState.stabilizationUiState)
    val currentPreviewDisplayUiState = rememberUpdatedState(captureUiState.previewDisplayUiState)
    val currentFocusMeteringUiState = rememberUpdatedState(captureUiState.focusMeteringUiState)
    val currentFlipLensUiState = rememberUpdatedState(captureUiState.flipLensUiState)
    val currentZoomControlUiState = rememberUpdatedState(captureUiState.zoomControlUiState)
    val currentVideoRecordingState = rememberUpdatedState(captureUiState.videoRecordingState)
    val currentElapsedTimeUiState = rememberUpdatedState(captureUiState.elapsedTimeUiState)
    val currentCaptureButtonUiState = rememberUpdatedState(captureUiState.captureButtonUiState)
    val currentQuickSettingsUiState = rememberUpdatedState(captureUiState.quickSettingsUiState)
    val currentAudioUiState = rememberUpdatedState(captureUiState.audioUiState)
    val currentCaptureModeToggleUiState = rememberUpdatedState(captureUiState.captureModeToggleUiState)
    val currentSnackBarUiState = rememberUpdatedState(snackBarUiState)
    val currentExternalCaptureMode = rememberUpdatedState(captureUiState.externalCaptureMode)
    val currentImageWellUiState = rememberUpdatedState(captureUiState.imageWellUiState)
    val currentDebugUiState = rememberUpdatedState(debugUiState)
    val currentScreenFlashUiState = rememberUpdatedState(screenFlashUiState)

    val hdrIndicator = remember {
        @Composable { _: Modifier -> HdrIndicator(modifier = Modifier, hdrUiState = currentHdrUiState.value) }
    }
    val flashModeIndicator = remember {
        @Composable { _: Modifier -> FlashModeIndicator(modifier = Modifier, flashModeUiState = currentFlashModeUiState.value) }
    }
    val videoQualityIndicator = remember {
        @Composable { _: Modifier -> VideoQualityIcon(currentVideoQuality.value, Modifier.testTag(VIDEO_QUALITY_TAG)) }
    }
    val stabilizationIndicator = remember {
        @Composable { modifier: Modifier -> StabilizationIcon(modifier = modifier, stabilizationUiState = currentStabilizationUiState.value) }
    }

    val onTapToFocusLambda = cameraController?.let { it::tapToFocus } ?: remember { { _: Float, _: Float -> } }
    val onScaleZoomLambda = remember(onScaleZoom) { { zoomRatio: Float -> onScaleZoom(zoomRatio, LensToZoom.PRIMARY) } }

    val viewfinderLambda = remember(onFlipCamera, onTapToFocusLambda, onScaleZoomLambda, surfaceRequest, onRequestWindowColorMode) {
        @Composable { _: Modifier ->
            PreviewDisplay(
                previewDisplayUiState = currentPreviewDisplayUiState.value,
                onFlipCamera = onFlipCamera,
                onTapToFocus = onTapToFocusLambda,
                onScaleZoom = onScaleZoomLambda,
                surfaceRequest = surfaceRequest,
                onRequestWindowColorMode = onRequestWindowColorMode,
                focusMeteringUiState = currentFocusMeteringUiState.value
            )
        }
    }

    val flipCameraButtonLambda = remember(onFlipCamera) {
        @Composable { _: Modifier ->
            FlipCameraButton(
                modifier = Modifier.testTag(FLIP_CAMERA_BUTTON),
                onClick = onFlipCamera,
                flipLensUiState = currentFlipLensUiState.value,
                enabledCondition = when (val flipLensUiState = currentFlipLensUiState.value) {
                    is FlipLensUiState.Available -> flipLensUiState.availableLensFacings.size > 1
                    FlipLensUiState.Unavailable -> false
                }
            )
        }
    }

    val onChangeZoomLambda = remember(onAnimateZoom) { { targetZoom: Float -> onAnimateZoom(targetZoom, LensToZoom.PRIMARY) } }

    val zoomLevelDisplayLambda = remember(onChangeZoomLambda) {
        @Composable { _: Modifier ->
            Column(modifier = Modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                ZoomButtonRow(
                    zoomControlUiState = currentZoomControlUiState.value,
                    onChangeZoom = onChangeZoomLambda
                )
            }
        }
    }

    val elapsedTimeDisplayLambda = remember {
        @Composable { _: Modifier ->
            AnimatedVisibility(
                visible = (currentVideoRecordingState.value is VideoRecordingState.Active),
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(delayMillis = 1_500))
            ) {
                ElapsedTimeText(
                    modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                    elapsedTimeUiState = currentElapsedTimeUiState.value
                )
            }
        }
    }

    val captureButtonLambda = remember(onIncrementZoom, quickSettingsController, captureController) {
        @Composable { _: Modifier ->
            fun runCaptureAction(action: () -> Unit) {
                if ((currentQuickSettingsUiState.value as? QuickSettingsUiState.Available)
                        ?.quickSettingsIsOpen == true
                ) {
                    quickSettingsController?.toggleQuickSettings()
                }
                action()
            }
            CaptureButton(
                captureButtonUiState = currentCaptureButtonUiState.value,
                isQuickSettingsOpen = (
                    currentQuickSettingsUiState.value as?
                        QuickSettingsUiState.Available
                    )?.quickSettingsIsOpen ?: false,
                onCaptureImage = {
                    runCaptureAction {
                        captureController?.captureImage(it)
                    }
                },
                onIncrementZoom = { targetZoom ->
                    onIncrementZoom(targetZoom, LensToZoom.PRIMARY)
                },
                onStartVideoRecording = {
                    runCaptureAction {
                        captureController?.startVideoRecording()
                    }
                },
                onStopVideoRecording = { captureController?.stopVideoRecording() },
                onLockVideoRecording = { isLocked ->
                    captureController?.setLockedRecording(
                        isLocked
                    )
                }
            )
        }
    }

    val quickSettingsButtonLambda = remember(quickSettingsController) {
        @Composable { _: Modifier ->
            AnimatedVisibility(
                visible = (currentVideoRecordingState.value !is VideoRecordingState.Active),
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(delayMillis = 1_500))
            ) {
                quickSettingsController?.let { quickSettingsController ->
                    ToggleQuickSettingsButton(
                        modifier = Modifier,
                        isOpen = (
                            currentQuickSettingsUiState.value
                                as? QuickSettingsUiState.Available
                            )?.quickSettingsIsOpen == true,
                        quickSettingsController = quickSettingsController
                    )
                }
            }
        }
    }

    val audioToggleButtonLambda = remember(onToggleAudio) {
        @Composable { modifier: Modifier ->
            AmplitudeToggleButton(
                modifier = modifier,
                onToggleAudio = onToggleAudio,
                audioUiState = currentAudioUiState.value
            )
        }
    }

    val captureModeToggleLambda = remember(quickSettingsController, snackBarController) {
        @Composable { modifier: Modifier ->
            if (currentCaptureModeToggleUiState.value is CaptureModeToggleUiState.Available) {
                CaptureModeToggleButton(
                    uiState = currentCaptureModeToggleUiState.value
                        as CaptureModeToggleUiState.Available,
                    quickSettingsController = quickSettingsController,
                    snackBarController = snackBarController,
                    modifier = modifier.testTag(CAPTURE_MODE_TOGGLE_BUTTON)
                )
            }
        }
    }

    val quickSettingsOverlayLambda = remember(quickSettingsController, onNavigateToSettings) {
        @Composable { modifier: Modifier ->
            quickSettingsController?.let { quickSettingsController ->
                QuickSettingsBottomSheet(
                    modifier = modifier,
                    quickSettingsUiState = currentQuickSettingsUiState.value,
                    onNavigateToSettings = {
                        quickSettingsController.toggleQuickSettings()
                        onNavigateToSettings()
                    },
                    quickSettingsController = quickSettingsController
                )
            }
        }
    }

    val debugOverlayLambda = remember(debugController, onAbsoluteZoom) {
        @Composable { modifier: Modifier, extraControls: Array<@Composable () -> Unit>? ->
            (currentDebugUiState.value as? DebugUiState.Enabled)?.let { debugUiState ->
                debugController?.let { debugController ->
                    DebugOverlay(
                        modifier = modifier,
                        debugUiState = debugUiState,
                        onChangeZoomRatio = { f: Float -> onAbsoluteZoom(f, LensToZoom.PRIMARY) },
                        extraControls = extraControls.orEmpty(),
                        debugController = debugController
                    )
                }
            }
        }
    }

    val debugVisibilityWrapperLambda = remember {
        @Composable { content: @Composable () -> Unit ->
            val uiState = currentDebugUiState.value
            if (uiState !is DebugUiState.Enabled || !uiState.debugHidingComponents) {
                content()
            }
        }
    }

    val screenFlashOverlayLambda = remember(screenFlashController) {
        @Composable { _: Modifier ->
            ScreenFlashScreen(
                screenFlashUiState = currentScreenFlashUiState.value,
                onInitialBrightnessCalculated = {
                    screenFlashController?.setClearUiScreenBrightness(
                        it
                    )
                }
            )
        }
    }

    val snackBarLambda = remember(snackBarController) {
        @Composable { modifier: Modifier, snackbarHostState: SnackbarHostState ->
            if (currentSnackBarUiState.value is SnackBarUiState.Enabled) {
                val snackBarData = (currentSnackBarUiState.value as SnackBarUiState.Enabled).snackBarQueue.peek()
                if (snackBarData != null) {
                    snackBarController?.let { snackBarController ->
                        TestableSnackbar(
                            modifier = modifier.testTag(snackBarData.testTag),
                            snackbarToShow = snackBarData,
                            snackbarHostState = snackbarHostState,
                            snackBarController = snackBarController
                        )
                    }
                }
            }
        }
    }

    val pauseToggleButtonLambda = remember(captureController) {
        @Composable { _: Modifier ->
            PauseResumeToggleButton(
                onSetPause = captureController?.let { it::setPaused } ?: { _ -> },
                currentRecordingState = currentVideoRecordingState.value
            )
        }
    }

    val imageWellLambda = remember(imageWellController, onNavigatePostCapture) {
        @Composable { modifier: Modifier ->
            if (currentExternalCaptureMode.value == ExternalCaptureMode.Standard) {
                (currentImageWellUiState.value as? ImageWellUiState.Content)?.let { contentState ->
                    ImageWell(
                        modifier = modifier,
                        imageWellUiState = contentState,
                        onClick = {
                            imageWellController?.imageWellToRepository(contentState.mediaDescriptor)
                            onNavigatePostCapture()
                        }
                    )
                }
            }
        }
    }

    LayoutWrapper(
        modifier = modifier,
        hdrIndicator = hdrIndicator,
        flashModeIndicator = flashModeIndicator,
        videoQualityIndicator = videoQualityIndicator,
        stabilizationIndicator = stabilizationIndicator,
        viewfinder = viewfinderLambda,
        captureButton = captureButtonLambda,
        flipCameraButton = flipCameraButtonLambda,
        zoomLevelDisplay = zoomLevelDisplayLambda,
        elapsedTimeDisplay = elapsedTimeDisplayLambda,
        quickSettingsButton = quickSettingsButtonLambda,
        audioToggleButton = audioToggleButtonLambda,
        captureModeToggle = captureModeToggleLambda,
        quickSettingsOverlay = quickSettingsOverlayLambda,
        debugOverlay = debugOverlayLambda,
        debugVisibilityWrapper = debugVisibilityWrapperLambda,
        screenFlashOverlay = screenFlashOverlayLambda,
        snackBar = snackBarLambda,
        pauseToggleButton = pauseToggleButtonLambda,
        imageWell = imageWellLambda
    )
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

@Composable
private fun LayoutWrapper(
    modifier: Modifier = Modifier,
    viewfinder: @Composable (modifier: Modifier) -> Unit,
    captureButton: @Composable (modifier: Modifier) -> Unit,
    flipCameraButton: @Composable (modifier: Modifier) -> Unit,
    zoomLevelDisplay: @Composable (modifier: Modifier) -> Unit,
    elapsedTimeDisplay: @Composable (modifier: Modifier) -> Unit,
    quickSettingsButton: @Composable (modifier: Modifier) -> Unit,
    flashModeIndicator: @Composable (modifier: Modifier) -> Unit,
    hdrIndicator: @Composable (modifier: Modifier) -> Unit,
    videoQualityIndicator: @Composable (modifier: Modifier) -> Unit,
    stabilizationIndicator: @Composable (modifier: Modifier) -> Unit,
    pauseToggleButton: @Composable (modifier: Modifier) -> Unit,
    audioToggleButton: @Composable (modifier: Modifier) -> Unit,
    captureModeToggle: @Composable (modifier: Modifier) -> Unit,
    imageWell: @Composable (modifier: Modifier) -> Unit,
    quickSettingsOverlay: @Composable (modifier: Modifier) -> Unit,
    debugOverlay: @Composable (
        modifier: Modifier,
        extraButtons: Array<@Composable () -> Unit>?
    ) -> Unit,
    debugVisibilityWrapper: (@Composable (@Composable () -> Unit) -> Unit),
    screenFlashOverlay: @Composable (modifier: Modifier) -> Unit,
    snackBar: @Composable (modifier: Modifier, snackbarHostState: SnackbarHostState) -> Unit
) {
    PreviewLayout(
        modifier = modifier,
        viewfinder = viewfinder,
        captureButton = captureButton,
        imageWell = imageWell,
        flipCameraButton = flipCameraButton,
        zoomLevelDisplay = zoomLevelDisplay,
        elapsedTimeDisplay = elapsedTimeDisplay,
        quickSettingsButton = quickSettingsButton,
        captureModeToggle = captureModeToggle,
        quickSettingsOverlay = quickSettingsOverlay,
        indicatorRow = { modifier ->
            Row(
                modifier = modifier
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                flashModeIndicator(Modifier)
                hdrIndicator(Modifier)
                videoQualityIndicator(Modifier)
                stabilizationIndicator(Modifier)
            }
        },
        debugOverlay = { modifier ->
            debugOverlay(
                modifier,
                arrayOf(
                    { audioToggleButton(Modifier) },
                    { pauseToggleButton(Modifier) }
                )
            )
        },
        debugVisibilityWrapper = debugVisibilityWrapper,
        screenFlashOverlay = screenFlashOverlay,
        snackBar = snackBar
    )
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
    externalCaptureMode = ExternalCaptureMode.Standard,
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
