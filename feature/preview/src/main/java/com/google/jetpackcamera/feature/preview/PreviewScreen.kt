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
import androidx.compose.runtime.derivedStateOf
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsBottomSheet
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashModeIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.HdrIndicator
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.ui.controller.CameraController
import com.google.jetpackcamera.ui.controller.CaptureController
import com.google.jetpackcamera.ui.controller.ImageWellController
import com.google.jetpackcamera.ui.controller.ScreenFlashController
import com.google.jetpackcamera.ui.controller.SnackBarController
import com.google.jetpackcamera.ui.controller.ZoomController
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.debug.DebugController
import com.google.jetpackcamera.ui.debug.DebugOverlay
import com.google.jetpackcamera.ui.debug.DebugUiState
import com.google.jetpackcamera.ui.uistate.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
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

    val rawUiState = viewModel.captureUiState.collectAsState()
    val debugUiState: DebugUiState by viewModel.debugUiState.collectAsState()
    val snackBarUiState: SnackBarUiState by viewModel.snackBarUiState.collectAsState()

    val isReady by remember { derivedStateOf { rawUiState.value is CaptureUiState.Ready } }

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

    if (!isReady) {
        LoadingScreen()
    } else {
        val readyStateProvider: () -> CaptureUiState.Ready = remember {
            {
                requireNotNull(rawUiState.value as? CaptureUiState.Ready) {
                    "Deferred read invoked when state was not Ready. " +
                        "Current state: ${rawUiState.value}"
                }
            }
        }

        val context = LocalContext.current
        LaunchedEffect(Unit) {
            debouncedOrientationFlow(context).collect(
                viewModel.cameraController::setDisplayRotation
            )
        }

        ContentScreen(
            modifier = modifier,
            captureUiStateProvider = readyStateProvider,
            surfaceRequest = surfaceRequest,
            onNavigateToSettings = onNavigateToSettings,
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
            screenFlashController = viewModel.screenFlashController,
            zoomController = viewModel.zoomController
        )
        val readStoragePermission: PermissionState = rememberPermissionState(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )

        LaunchedEffect(readStoragePermission.status) {
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P ||
                readStoragePermission.status.isGranted
            ) {
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContentScreen(
    captureUiStateProvider: () -> CaptureUiState.Ready,
    surfaceRequest: SurfaceRequest?,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
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
    screenFlashController: ScreenFlashController? = null,
    zoomController: ZoomController? = null
) {
    val currentCaptureUiStateProvider by rememberUpdatedState(captureUiStateProvider)
    val flipLensState =
        remember { derivedStateOf { currentCaptureUiStateProvider().flipLensUiState } }
    val zoomControlState = remember {
        derivedStateOf { currentCaptureUiStateProvider().zoomControlUiState }
    }
    val zoomUiState = remember { derivedStateOf { currentCaptureUiStateProvider().zoomUiState } }
    val videoRecordingState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().videoRecordingState
        }
    }
    val isVideoRecordingActive = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().videoRecordingState is VideoRecordingState.Active
        }
    }

    val scope = rememberCoroutineScope()
    val zoomStateManager = remember(zoomController) {
        val safeZoomController = zoomController ?: object : ZoomController {
            override fun setZoomRatio(zoomRatio: com.google.jetpackcamera.model.CameraZoomRatio) {}
            override fun setZoomAnimationState(targetValue: Float?) {}
        }
        ZoomStateManager(
            initialZoomLevel =
            (zoomControlState.value as? ZoomControlUiState.Enabled)?.initialZoomRatio ?: 1f,
            zoomRange = (zoomUiState.value as? ZoomUiState.Enabled)
                ?.primaryZoomRange ?: Range(1f, 1f),
            zoomController = safeZoomController
        )
    }

    LaunchedEffect((flipLensState.value as? FlipLensUiState.Available)?.selectedLensFacing) {
        zoomStateManager.onChangeLens(
            newInitialZoomLevel =
            (zoomControlState.value as? ZoomControlUiState.Enabled)?.initialZoomRatio ?: 1f,
            newZoomRange = (zoomUiState.value as? ZoomUiState.Enabled)
                ?.primaryZoomRange ?: Range(1f, 1f)
        )
    }

    var initialRecordingSettings by remember { mutableStateOf<InitialRecordingSettings?>(null) }
    LaunchedEffect(videoRecordingState.value) {
        with(videoRecordingState.value) {
            when (this) {
                is VideoRecordingState.Starting -> {
                    initialRecordingSettings = this.initialRecordingSettings
                }
                is VideoRecordingState.Inactive -> {
                    initialRecordingSettings?.let {
                        val oldPrimaryLensFacing = it.lensFacing
                        val oldZoomRatios = it.zoomRatios
                        val oldAudioEnabled = it.isAudioEnabled
                        captureController?.setAudioEnabled(oldAudioEnabled)
                        quickSettingsController?.setLensFacing(oldPrimaryLensFacing)
                        zoomStateManager.apply {
                            absoluteZoom(
                                targetZoomLevel = oldZoomRatios[oldPrimaryLensFacing] ?: 1f,
                                lensToZoom = LensToZoom.PRIMARY
                            )
                            absoluteZoom(
                                targetZoomLevel = oldZoomRatios[oldPrimaryLensFacing.flip()] ?: 1f,
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

    val onFlipCamera = remember {
        {
            val state = flipLensState.value
            if (state is FlipLensUiState.Available) {
                quickSettingsController?.setLensFacing(
                    state.selectedLensFacing.flip()
                )
            }
        }
    }

    val audioState = remember { derivedStateOf { currentCaptureUiStateProvider().audioUiState } }
    val isAudioEnabled by remember {
        derivedStateOf { audioState.value is AudioUiState.Enabled.On }
    }
    val onToggleAudio: () -> Unit = remember(isAudioEnabled) {
        {
            captureController?.setAudioEnabled(!isAudioEnabled)
        }
    }

    // Slot lambdas are wrapped in remember blocks to isolate recompositions.
    val hdrState = remember { derivedStateOf { currentCaptureUiStateProvider().hdrUiState } }
    val hdrIndicatorLambda = remember(hdrState) {
        @Composable { modifier: Modifier ->
            HdrIndicator(modifier = modifier, hdrUiState = hdrState.value)
        }
    }
    val flashModeState =
        remember { derivedStateOf { currentCaptureUiStateProvider().flashModeUiState } }
    val flashModeIndicatorLambda = remember {
        @Composable { modifier: Modifier ->
            FlashModeIndicator(
                modifier = modifier,
                flashModeUiStateProvider = { flashModeState.value }
            )
        }
    }
    val videoQualityState =
        remember { derivedStateOf { currentCaptureUiStateProvider().videoQuality } }
    val videoQualityIndicatorLambda = remember(videoQualityState) {
        @Composable { modifier: Modifier ->
            VideoQualityIcon(
                videoQualityState.value,
                modifier.testTag(VIDEO_QUALITY_TAG)
            )
        }
    }
    val stabilizationState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().stabilizationUiState
        }
    }
    val stabilizationIndicatorLambda = remember(stabilizationState) {
        @Composable { modifier: Modifier ->
            StabilizationIcon(
                modifier = modifier,
                stabilizationUiState = stabilizationState.value
            )
        }
    }

    val onTapToFocusLambda = cameraController?.let { it::tapToFocus }
        ?: remember { { _: Float, _: Float -> } }
    val onScaleZoomLambda = remember {
        { zoomRatio: Float ->
            scope.launch { zoomStateManager.scaleZoom(zoomRatio, LensToZoom.PRIMARY) }
        }
    }

    val previewDisplayState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().previewDisplayUiState
        }
    }
    val focusMeteringState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().focusMeteringUiState
        }
    }
    val viewfinderLambda = remember(
        previewDisplayState,
        focusMeteringState,
        onFlipCamera,
        onTapToFocusLambda,
        onScaleZoomLambda,
        surfaceRequest,
        onRequestWindowColorMode
    ) {
        @Composable { modifier: Modifier ->
            PreviewDisplay(
                previewDisplayUiState = previewDisplayState.value,
                onFlipCamera = onFlipCamera,
                onTapToFocus = onTapToFocusLambda,
                onScaleZoom = { zoomRatio -> onScaleZoomLambda(zoomRatio) },
                surfaceRequest = surfaceRequest,
                onRequestWindowColorMode = onRequestWindowColorMode,
                focusMeteringUiState = focusMeteringState.value
            )
        }
    }

    val captureButtonState = remember {
        derivedStateOf { currentCaptureUiStateProvider().captureButtonUiState }
    }
    val quickSettingsState = remember {
        derivedStateOf { currentCaptureUiStateProvider().quickSettingsUiState }
    }
    val captureButtonLambda = remember(
        captureButtonState,
        quickSettingsState,
        quickSettingsController,
        captureController
    ) {
        @Composable { modifier: Modifier ->
            val quickSettingsUiState = quickSettingsState.value
            fun runCaptureAction(action: () -> Unit) {
                if ((quickSettingsUiState as? QuickSettingsUiState.Available)
                        ?.quickSettingsIsOpen == true
                ) {
                    quickSettingsController?.toggleQuickSettings()
                }
                action()
            }
            CaptureButton(
                captureButtonUiState = captureButtonState.value,
                isQuickSettingsOpen = (quickSettingsUiState as? QuickSettingsUiState.Available)
                    ?.quickSettingsIsOpen ?: false,
                onCaptureImage = {
                    runCaptureAction {
                        captureController?.captureImage(it)
                    }
                },
                onIncrementZoom = { targetZoom ->
                    scope.launch { zoomStateManager.incrementZoom(targetZoom, LensToZoom.PRIMARY) }
                },
                onStartVideoRecording = {
                    runCaptureAction {
                        captureController?.startVideoRecording()
                    }
                },
                onStopVideoRecording = { captureController?.stopVideoRecording() },
                onLockVideoRecording = { isLocked ->
                    captureController?.setLockedRecording(isLocked)
                }
            )
        }
    }

    val flipCameraButtonLambda = remember(flipLensState, onFlipCamera) {
        @Composable { modifier: Modifier ->
            FlipCameraButton(
                modifier = modifier.testTag(FLIP_CAMERA_BUTTON),
                onClick = onFlipCamera,
                flipLensUiState = flipLensState.value,
                enabledCondition = when (val uiState = flipLensState.value) {
                    is FlipLensUiState.Available -> uiState.availableLensFacings.size > 1
                    FlipLensUiState.Unavailable -> false
                }
            )
        }
    }

    val zoomLevelDisplayLambda = remember(zoomControlState) {
        @Composable { modifier: Modifier ->
            Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
                ZoomButtonRow(
                    zoomControlUiState = zoomControlState.value,
                    onChangeZoom = { targetZoom ->
                        scope.launch {
                            zoomStateManager.animatedZoom(
                                targetZoomLevel = targetZoom,
                                lensToZoom = LensToZoom.PRIMARY
                            )
                        }
                    }
                )
            }
        }
    }

    val audioToggleButtonLambda = remember(audioState, onToggleAudio) {
        @Composable { modifier: Modifier ->
            AmplitudeToggleButton(
                modifier = modifier,
                onToggleAudio = onToggleAudio,
                audioUiState = audioState.value
            )
        }
    }

    val elapsedTimeDisplayLambda = remember(videoRecordingState) {
        @Composable { modifier: Modifier ->
            val isVisible = videoRecordingState.value is VideoRecordingState.Active
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(delayMillis = 1_500))
            ) {
                val elapsedTimeModifier = remember(modifier) { modifier.testTag(ELAPSED_TIME_TAG) }
                ElapsedTimeText(
                    modifier = elapsedTimeModifier,
                    elapsedTimeUiStateProvider = {
                        currentCaptureUiStateProvider().elapsedTimeUiState
                    }
                )
            }
        }
    }

    val captureModeToggleState = remember {
        derivedStateOf { currentCaptureUiStateProvider().captureModeToggleUiState }
    }
    val captureModeToggleLambda = remember(
        captureModeToggleState,
        quickSettingsController,
        snackBarController
    ) {
        @Composable { modifier: Modifier ->
            val captureModeToggleUiState = captureModeToggleState.value
            if (captureModeToggleUiState is CaptureModeToggleUiState.Available) {
                CaptureModeToggleButton(
                    uiState = captureModeToggleUiState,
                    quickSettingsController = quickSettingsController,
                    snackBarController = snackBarController,
                    modifier = modifier.testTag(CAPTURE_MODE_TOGGLE_BUTTON)
                )
            }
        }
    }

    val quickSettingsButtonLambda = remember(
        isVideoRecordingActive,
        quickSettingsState,
        quickSettingsController
    ) {
        @Composable { modifier: Modifier ->
            val isQuickSettingsVisible = !isVideoRecordingActive.value
            AnimatedVisibility(
                visible = isQuickSettingsVisible,
                enter = fadeIn(),
                exit = fadeOut(animationSpec = tween(delayMillis = 1_500))
            ) {
                quickSettingsController?.let { quickSettingsController ->
                    ToggleQuickSettingsButton(
                        modifier = modifier,
                        isOpen = (quickSettingsState.value as? QuickSettingsUiState.Available)
                            ?.quickSettingsIsOpen == true,
                        quickSettingsController = quickSettingsController
                    )
                }
            }
        }
    }

    val quickSettingsOverlayLambda = remember(
        quickSettingsState,
        quickSettingsController,
        onNavigateToSettings
    ) {
        @Composable { modifier: Modifier ->
            quickSettingsController?.let { quickSettingsController ->
                QuickSettingsBottomSheet(
                    modifier = modifier,
                    quickSettingsUiState = quickSettingsState.value,
                    onNavigateToSettings = {
                        quickSettingsController.toggleQuickSettings()
                        onNavigateToSettings()
                    },
                    quickSettingsController = quickSettingsController
                )
            }
            Unit
        }
    }

    val debugOverlayLambda = remember(debugController, debugUiState) {
        @Composable { modifier: Modifier, extraControls: Array<@Composable () -> Unit>? ->
            if (debugUiState is DebugUiState.Enabled) {
                debugController?.let { debugController ->
                    DebugOverlay(
                        modifier = modifier,
                        debugUiState = debugUiState,
                        onChangeZoomRatio = { f: Float ->
                            scope.launch { zoomStateManager.absoluteZoom(f, LensToZoom.PRIMARY) }
                        },
                        extraControls = extraControls.orEmpty(),
                        debugController = debugController
                    )
                }
            }
            Unit
        }
    }

    val debugVisibilityWrapperLambda = remember(debugUiState) {
        @Composable { content: @Composable () -> Unit ->
            if (debugUiState !is DebugUiState.Enabled || !debugUiState.debugHidingComponents) {
                content()
            }
            Unit
        }
    }

    val screenFlashState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().screenFlashUiState
        }
    }
    val screenFlashOverlayLambda = remember(screenFlashState, screenFlashController) {
        @Composable { modifier: Modifier ->
            ScreenFlashScreen(
                screenFlashUiState = screenFlashState.value,
                onInitialBrightnessCalculated = {
                    screenFlashController?.setClearUiScreenBrightness(it)
                }
            )
        }
    }

    val snackBarLambda = remember(snackBarController, snackBarUiState) {
        @Composable { modifier: Modifier, snackbarHostState: SnackbarHostState ->
            val snackBarUiState = snackBarUiState
            if (snackBarUiState is SnackBarUiState.Enabled) {
                val snackBarData = snackBarUiState.snackBarQueue.peek()
                if (snackBarData != null) {
                    snackBarController?.let { snackBarController ->
                        TestableSnackbar(
                            modifier = modifier,
                            snackbarToShow = snackBarData,
                            snackbarHostState = snackbarHostState,
                            snackBarController = snackBarController
                        )
                    }
                }
            }
            Unit
        }
    }

    val pauseToggleButtonLambda = remember(videoRecordingState, captureController) {
        @Composable { modifier: Modifier ->
            PauseResumeToggleButton(
                modifier = modifier,
                onSetPause = captureController?.let { it::setPaused } ?: { _ -> },
                currentRecordingStateProvider = { videoRecordingState.value }
            )
        }
    }

    val externalCaptureModeState = remember {
        derivedStateOf {
            currentCaptureUiStateProvider().externalCaptureMode
        }
    }
    val imageWellState =
        remember { derivedStateOf { currentCaptureUiStateProvider().imageWellUiState } }
    val imageWellLambda = remember(
        externalCaptureModeState,
        imageWellState,
        imageWellController,
        onNavigatePostCapture
    ) {
        @Composable { modifier: Modifier ->
            if (externalCaptureModeState.value == ExternalCaptureMode.Standard) {
                (imageWellState.value as? ImageWellUiState.Content)?.let { contentState ->
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
            Unit
        }
    }

    LayoutWrapper(
        modifier = modifier,
        hdrIndicator = hdrIndicatorLambda,
        flashModeIndicator = flashModeIndicatorLambda,
        videoQualityIndicator = videoQualityIndicatorLambda,
        stabilizationIndicator = stabilizationIndicatorLambda,

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
            captureUiStateProvider = { FAKE_PREVIEW_UI_STATE_READY },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Standard_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiStateProvider = { FAKE_PREVIEW_UI_STATE_READY.copy() },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_ImageOnly_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiStateProvider = {
                FAKE_PREVIEW_UI_STATE_READY.copy(
                    captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
                )
            },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_VideoOnly_Idle() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiStateProvider = {
                FAKE_PREVIEW_UI_STATE_READY.copy(
                    captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY)
                )
            },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Standard_Recording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiStateProvider = { FAKE_PREVIEW_UI_STATE_PRESSED_RECORDING },
            surfaceRequest = null
        )
    }
}

@Preview
@Composable
private fun ContentScreen_Locked_Recording() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ContentScreen(
            captureUiStateProvider = { FAKE_PREVIEW_UI_STATE_LOCKED_RECORDING },
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
