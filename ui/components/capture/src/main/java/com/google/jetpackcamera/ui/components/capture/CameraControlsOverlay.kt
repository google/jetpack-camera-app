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

import android.content.ContentResolver
import android.util.Range
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.VideoQuality
import com.google.jetpackcamera.ui.components.capture.debug.DebugOverlayToggleButton
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSettingsIndicators
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState.Unavailable.findSelectableStateFor
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState.Unavailable.isCaptureModeSelectable
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import kotlinx.coroutines.delay

class ZoomLevelDisplayState(private val alwaysDisplay: Boolean = false) {
    private var _showZoomLevel = mutableStateOf(alwaysDisplay)
    val showZoomLevel: Boolean get() = _showZoomLevel.value

    suspend fun showZoomLevel() {
        if (!alwaysDisplay) {
            _showZoomLevel.value = true
            delay(3000)
            _showZoomLevel.value = false
        }
    }
}

@Composable
fun CameraControlsOverlay(
    captureUiState: CaptureUiState.Ready,
    modifier: Modifier = Modifier,
    zoomLevelDisplayState: ZoomLevelDisplayState = remember { ZoomLevelDisplayState() },
    onNavigateToSettings: () -> Unit = {},
    onSetCaptureMode: (CaptureMode) -> Unit = {},
    onFlipCamera: () -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onChangeImageFormat: (ImageOutputFormat) -> Unit = {},
    onDisabledCaptureMode: (DisableRationale) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onToggleDebugOverlay: () -> Unit = {},
    onToggleAudio: () -> Unit = {},
    onSetPause: (Boolean) -> Unit = {},
    onAnimateZoom: (Float) -> Unit = {},
    onIncrementZoom: (Float) -> Unit = {},
    onCaptureImage: (ContentResolver) -> Unit = {},
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    onImageWellClick: () -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit
) {
    // Show the current zoom level for a short period of time, only when the level changes.
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(captureUiState.zoomUiState) {
        if (firstRun) {
            firstRun = false
        } else {
            zoomLevelDisplayState.showZoomLevel()
        }
    }

    CompositionLocalProvider(LocalContentColor provides Color.White) {
        Box(
            modifier
                .safeDrawingPadding()
                .fillMaxSize()
        ) {
            if (captureUiState.videoRecordingState is VideoRecordingState.Inactive) {
                val showDebugButton = captureUiState.debugUiState is DebugUiState.Enabled &&
                    captureUiState.debugUiState !is DebugUiState.Open
                ControlsTop(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    isQuickSettingsOpen =
                    (
                        captureUiState.quickSettingsUiState
                            as QuickSettingsUiState.Available
                        ).quickSettingsIsOpen,
                    showDebugButton = showDebugButton,
                    onNavigateToSettings = onNavigateToSettings,
                    onChangeFlash = onChangeFlash,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onToggleDebugOverlay = onToggleDebugOverlay,
                    stabilizationUiState = captureUiState.stabilizationUiState,
                    videoQuality = captureUiState.videoQuality,
                    flashModeUiState = captureUiState.flashModeUiState
                )
            }

            ControlsBottom(
                modifier = Modifier
                    // padding to avoid snackbar
                    .padding(bottom = 60.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                captureUiState = captureUiState,
                zoomControlUiState = captureUiState.zoomControlUiState,
                flipLensUiState = captureUiState.flipLensUiState,
                zoomUiState = captureUiState.zoomUiState,
                showZoomLevel = zoomLevelDisplayState.showZoomLevel,
                isQuickSettingsOpen =
                (
                    captureUiState.quickSettingsUiState
                        as QuickSettingsUiState.Available
                    ).quickSettingsIsOpen,
                videoRecordingState = captureUiState.videoRecordingState,
                onSetCaptureMode = onSetCaptureMode,
                onFlipCamera = onFlipCamera,
                onAnimateZoom = onAnimateZoom,
                onIncrementZoom = onIncrementZoom,
                onCaptureImage = onCaptureImage,
                onToggleQuickSettings = onToggleQuickSettings,
                onToggleAudio = onToggleAudio,
                onSetPause = onSetPause,
                onDisabledCaptureMode = onDisabledCaptureMode,
                onStartVideoRecording = onStartVideoRecording,
                onStopVideoRecording = onStopVideoRecording,
                onImageWellClick = onImageWellClick,
                onLockVideoRecording = onLockVideoRecording
            )
        }
    }
}

@Composable
private fun ControlsTop(
    isQuickSettingsOpen: Boolean,
    modifier: Modifier = Modifier,
    showDebugButton: Boolean = false,
    onNavigateToSettings: () -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onToggleDebugOverlay: () -> Unit = {},
    stabilizationUiState: StabilizationUiState = StabilizationUiState.Disabled,
    videoQuality: VideoQuality = VideoQuality.UNSPECIFIED,
    flashModeUiState: FlashModeUiState = FlashModeUiState.Unavailable
) {
    Column(modifier) {
        Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                // button to open default settings page
                SettingsNavButton(
                    modifier = Modifier
                        .padding(12.dp)
                        .testTag(SETTINGS_BUTTON),
                    onNavigateToSettings = onNavigateToSettings
                )
                AnimatedVisibility(
                    visible = !isQuickSettingsOpen,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    QuickSettingsIndicators(
                        flashModeUiState = flashModeUiState,
                        onFlashModeClick = onChangeFlash
                    )
                }
            }

            // quick settings button
            ToggleQuickSettingsButton(
                toggleDropDown = onToggleQuickSettings,
                isOpen = isQuickSettingsOpen
            )

            Row(
                Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                var visibleStabilizationUiState: StabilizationUiState by remember {
                    mutableStateOf(StabilizationUiState.Disabled)
                }
                if (stabilizationUiState is StabilizationUiState.Enabled) {
                    // Only save StabilizationUiState.Set so exit transition can happen properly
                    visibleStabilizationUiState = stabilizationUiState
                }
                AnimatedVisibility(
                    visible = stabilizationUiState is StabilizationUiState.Enabled,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    (visibleStabilizationUiState as? StabilizationUiState.Enabled)?.let {
                        StabilizationIcon(stabilizationUiState = it)
                    }
                }
                VideoQualityIcon(videoQuality, Modifier.testTag(VIDEO_QUALITY_TAG))
            }
        }
        AnimatedVisibility(
            showDebugButton,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            DebugOverlayToggleButton(toggleIsOpen = onToggleDebugOverlay)
        }
    }
}

@Composable
private fun ControlsBottom(
    modifier: Modifier = Modifier,
    captureUiState: CaptureUiState.Ready,
    flipLensUiState: FlipLensUiState,
    zoomUiState: ZoomUiState,
    zoomControlUiState: ZoomControlUiState,
    showZoomLevel: Boolean,
    isQuickSettingsOpen: Boolean,
    videoRecordingState: VideoRecordingState,
    onFlipCamera: () -> Unit = {},
    onCaptureImage: (ContentResolver) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onToggleAudio: () -> Unit = {},
    onSetPause: (Boolean) -> Unit = {},
    onSetCaptureMode: (CaptureMode) -> Unit = {},
    onDisabledCaptureMode: (DisableRationale) -> Unit = {},
    onAnimateZoom: (Float) -> Unit = {},
    onIncrementZoom: (Float) -> Unit = {},
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    onImageWellClick: () -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit = {}
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 20.sp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                AnimatedVisibility(
                    visible = (
                        captureUiState.debugUiState is DebugUiState.Enabled && showZoomLevel &&
                            zoomUiState is ZoomUiState.Enabled
                        ),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ZoomRatioText(zoomUiState as ZoomUiState.Enabled)
                }
                val debugUiState = captureUiState.debugUiState
                if (debugUiState is DebugUiState.Enabled) {
                    CurrentCameraIdText(
                        debugUiState.currentPhysicalCameraId,
                        debugUiState.currentLogicalCameraId
                    )
                }
                if (zoomControlUiState is ZoomControlUiState.Enabled &&
                    zoomUiState is ZoomUiState.Enabled
                ) {
                    ZoomButtonRow(
                        zoomControlUiState = zoomControlUiState,
                        onChangeZoom = { targetZoom ->
                            onAnimateZoom(targetZoom)
                        }
                    )
                }
                if (captureUiState.elapsedTimeUiState is ElapsedTimeUiState.Enabled) {
                    AnimatedVisibility(
                        visible = (
                            captureUiState.videoRecordingState is
                                VideoRecordingState.Active
                            ),
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(delayMillis = 1_500))
                    ) {
                        ElapsedTimeText(
                            modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                            elapsedTimeUiState = captureUiState.elapsedTimeUiState
                                as ElapsedTimeUiState.Enabled
                        )
                    }
                }
            }
        }

        Column {
            if (!isQuickSettingsOpen &&
                captureUiState.captureModeToggleUiState
                    is CaptureModeToggleUiState.Available
            ) {
                // TODO(yasith): Align to end of ImageWell based on alignment lines
                Box(
                    Modifier
                        .align(Alignment.End)
                        .padding(end = 12.dp)
                ) {
                    CaptureModeToggleButton(
                        uiState = captureUiState.captureModeToggleUiState
                            as CaptureModeToggleUiState.Available,
                        onChangeCaptureMode = onSetCaptureMode,
                        onToggleWhenDisabled = onDisabledCaptureMode,
                        modifier = Modifier.testTag(CAPTURE_MODE_TOGGLE_BUTTON)
                    )
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Row that holds flip camera, capture button, and audio
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    // animation fades in/out this component based on quick settings
                    AnimatedVisibility(
                        visible = !isQuickSettingsOpen,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        if (videoRecordingState is VideoRecordingState.Inactive) {
                            FlipCameraButton(
                                modifier = Modifier.testTag(FLIP_CAMERA_BUTTON),
                                onClick = onFlipCamera,
                                flipLensUiState = flipLensUiState,
                                // enable only when phone has front and rear camera
                                enabledCondition =
                                flipLensUiState is FlipLensUiState.Available &&
                                    flipLensUiState.availableLensFacings.size > 1
                            )
                        } else if (videoRecordingState is VideoRecordingState.Active
                        ) {
                            PauseResumeToggleButton(
                                onSetPause = onSetPause,
                                currentRecordingState = videoRecordingState
                            )
                        }
                    }
                }
                CaptureButton(
                    captureButtonUiState = captureUiState.captureButtonUiState,
                    isQuickSettingsOpen = isQuickSettingsOpen,
                    onCaptureImage = onCaptureImage,
                    onIncrementZoom = { targetZoom ->
                        onIncrementZoom(targetZoom)
                    },
                    onToggleQuickSettings = onToggleQuickSettings,
                    onStartVideoRecording = onStartVideoRecording,
                    onStopVideoRecording = onStopVideoRecording,
                    onLockVideoRecording = onLockVideoRecording
                )

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoRecordingState is VideoRecordingState.Active) {
                        AmplitudeVisualizer(
                            modifier = Modifier.fillMaxSize(),
                            onToggleAudio = onToggleAudio,
                            audioUiState = captureUiState.audioUiState
                        )
                    } else if (!isQuickSettingsOpen &&
                        captureUiState.externalCaptureMode == ExternalCaptureMode.Standard
                    ) {
                        ImageWell(
                            imageWellUiState = captureUiState.imageWellUiState,
                            onClick = onImageWellClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    captureButtonUiState: CaptureButtonUiState,
    isQuickSettingsOpen: Boolean,
    onToggleQuickSettings: () -> Unit = {},
    onIncrementZoom: (Float) -> Unit = {},
    onCaptureImage: (ContentResolver) -> Unit = {},
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit = {}
) {
    val multipleEventsCutter = remember { MultipleEventsCutter() }
    val context = LocalContext.current

    CaptureButton(
        modifier = modifier.testTag(CAPTURE_BUTTON),
        onIncrementZoom = onIncrementZoom,
        onImageCapture = {
            if (captureButtonUiState is CaptureButtonUiState.Enabled) {
                multipleEventsCutter.processEvent {
                    onCaptureImage(context.contentResolver)
                }
            }
            if (isQuickSettingsOpen) {
                onToggleQuickSettings()
            }
        },
        onStartRecording = {
            if (captureButtonUiState is CaptureButtonUiState.Enabled) {
                onStartVideoRecording()
                if (isQuickSettingsOpen) {
                    onToggleQuickSettings()
                }
            }
        },
        onStopRecording = {
            onStopVideoRecording()
        },
        captureButtonUiState = captureButtonUiState,
        onLockVideoRecording = onLockVideoRecording
    )
}

@Composable
private fun CaptureModeToggleButton(
    uiState: CaptureModeToggleUiState.Available,
    onChangeCaptureMode: (CaptureMode) -> Unit,
    onToggleWhenDisabled: (DisableRationale) -> Unit,
    modifier: Modifier = Modifier
) {
    // Captures hdr image (left) when output format is UltraHdr, else captures hdr video (right).
    val initialState =
        when (uiState.selectedCaptureMode) {
            CaptureMode.IMAGE_ONLY -> ToggleState.Left
            CaptureMode.VIDEO_ONLY -> ToggleState.Right
            CaptureMode.STANDARD -> TODO("toggle should not be visible for STANDARD mode")
        }
    val enabled =
        uiState.isCaptureModeSelectable(CaptureMode.VIDEO_ONLY) &&
            uiState.isCaptureModeSelectable(CaptureMode.IMAGE_ONLY)
    ToggleButton(
        leftIcon = if (uiState.selectedCaptureMode ==
            CaptureMode.IMAGE_ONLY
        ) {
            rememberVectorPainter(image = Icons.Filled.CameraAlt)
        } else {
            rememberVectorPainter(image = Icons.Outlined.CameraAlt)
        },
        rightIcon = if (uiState.selectedCaptureMode ==
            CaptureMode.VIDEO_ONLY
        ) {
            rememberVectorPainter(image = Icons.Filled.Videocam)
        } else {
            rememberVectorPainter(image = Icons.Outlined.Videocam)
        },
        initialState = initialState,
        onToggleStateChanged = {
            val captureMode = when (it) {
                ToggleState.Left -> CaptureMode.IMAGE_ONLY
                ToggleState.Right -> CaptureMode.VIDEO_ONLY
            }
            onChangeCaptureMode(captureMode)
        },
        onToggleWhenDisabled = {
            val disabledReason: DisableRationale? =
                (
                    uiState.findSelectableStateFor(CaptureMode.VIDEO_ONLY) as?
                        SingleSelectableUiState.Disabled<CaptureMode>
                    )?.disabledReason
                    ?: (
                        uiState.findSelectableStateFor(CaptureMode.IMAGE_ONLY)
                            as? SingleSelectableUiState.Disabled<CaptureMode>
                        )
                        ?.disabledReason
            disabledReason?.let { onToggleWhenDisabled(it) }
        },
        // toggle only enabled when both capture modes are available
        enabled = enabled,
        leftIconDescription =
        if (enabled) {
            stringResource(id = R.string.capture_mode_image_capture_content_description)
        } else {
            stringResource(
                id = R.string.capture_mode_image_capture_content_description_disabled
            )
        },
        rightIconDescription =
        if (enabled) {
            stringResource(id = R.string.capture_mode_video_recording_content_description)
        } else {
            stringResource(
                id = R.string.capture_mode_video_recording_content_description_disabled
            )
        },
        modifier = modifier
    )
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_QuickSettingsOpen() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = true
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_QuickSettingsClosed() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_FlashModeOn() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false,
            flashModeUiState = FlashModeUiState.Available(
                selectedFlashMode = FlashMode.ON,
                availableFlashModes = listOf(
                    SingleSelectableUiState.SelectableUi(FlashMode.OFF),
                    SingleSelectableUiState.SelectableUi(FlashMode.ON)
                ),
                isActive = false
            )
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_FlashModeAuto() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false,
            flashModeUiState = FlashModeUiState.Available(
                selectedFlashMode = FlashMode.AUTO,
                availableFlashModes = listOf(
                    SingleSelectableUiState.SelectableUi(FlashMode.OFF),
                    SingleSelectableUiState.SelectableUi(FlashMode.ON),
                    SingleSelectableUiState.SelectableUi(FlashMode.AUTO)
                ),
                isActive = false
            )
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_WithStabilization() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false,
            stabilizationUiState = StabilizationUiState.Specific(
                stabilizationMode = StabilizationMode.ON
            )
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_WithStabilizationAuto() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false,
            stabilizationUiState = StabilizationUiState.Auto(
                stabilizationMode = StabilizationMode.OPTICAL
            )
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            captureUiState = CaptureUiState.Ready(
                externalCaptureMode = ExternalCaptureMode.Standard,
                captureModeToggleUiState = CaptureModeToggleUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
            ),
            flipLensUiState = FlipLensUiState.Available(
                LensFacing.FRONT,
                listOf(
                    SingleSelectableUiState.SelectableUi(LensFacing.FRONT),
                    SingleSelectableUiState.SelectableUi(LensFacing.BACK)
                )
            ),
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            videoRecordingState = VideoRecordingState.Inactive(),
            zoomControlUiState = ZoomControlUiState.Enabled(
                listOf(1f, 2f, 5f),
                primaryLensFacing = LensFacing.FRONT,
                primaryZoomRatio = 1f
            ),
            zoomUiState = ZoomUiState.Enabled(
                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            )
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_NoZoomLevel() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            captureUiState = CaptureUiState.Ready(
                externalCaptureMode = ExternalCaptureMode.Standard,
                captureModeToggleUiState = CaptureModeToggleUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
            ),
            zoomControlUiState = ZoomControlUiState.Enabled(
                listOf(1f, 2f, 5f),
                primaryLensFacing = LensFacing.FRONT,

                primaryZoomRatio = 1f
            ),
            zoomUiState = ZoomUiState.Enabled(
                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            ),
            flipLensUiState = FlipLensUiState.Available(
                LensFacing.FRONT,
                listOf(
                    SingleSelectableUiState.SelectableUi(LensFacing.FRONT),
                    SingleSelectableUiState.SelectableUi(LensFacing.BACK)
                )
            ),
            showZoomLevel = false,
            isQuickSettingsOpen = false,
            videoRecordingState = VideoRecordingState.Inactive()
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_QuickSettingsOpen() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            captureUiState = CaptureUiState.Ready(
                externalCaptureMode = ExternalCaptureMode.Standard,
                captureModeToggleUiState = CaptureModeToggleUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
            ),
            zoomControlUiState = ZoomControlUiState.Enabled(
                listOf(1f, 2f, 5f),
                primaryLensFacing = LensFacing.FRONT,

                primaryZoomRatio = 1f
            ),

            zoomUiState = ZoomUiState.Enabled(
                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            ),
            flipLensUiState = FlipLensUiState.Available(
                LensFacing.FRONT,
                listOf(
                    SingleSelectableUiState.SelectableUi(LensFacing.FRONT),
                    SingleSelectableUiState.SelectableUi(LensFacing.BACK)
                )
            ),
            showZoomLevel = true,
            isQuickSettingsOpen = true,
            videoRecordingState = VideoRecordingState.Inactive()
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_NoFlippableCamera() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            captureUiState = CaptureUiState.Ready(
                externalCaptureMode = ExternalCaptureMode.Standard,
                captureModeToggleUiState = CaptureModeToggleUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
            ),
            flipLensUiState = FlipLensUiState.Available(
                LensFacing.FRONT,
                listOf(
                    SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
                )
            ),
            zoomControlUiState = ZoomControlUiState.Enabled(
                listOf(1f, 2f, 5f),
                primaryLensFacing = LensFacing.FRONT,

                primaryZoomRatio = 1f
            ),

            zoomUiState = ZoomUiState.Enabled(

                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            ),
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            videoRecordingState = VideoRecordingState.Inactive()
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_Recording() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            captureUiState = CaptureUiState.Ready(
                externalCaptureMode = ExternalCaptureMode.Standard,
                captureModeToggleUiState = CaptureModeToggleUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Active.Recording(0L, .9, 1_000_000_000),
                captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
            ),
            flipLensUiState = FlipLensUiState.Available(
                LensFacing.FRONT,
                listOf(
                    SingleSelectableUiState.SelectableUi(LensFacing.FRONT),
                    SingleSelectableUiState.SelectableUi(LensFacing.BACK)
                )
            ),
            zoomControlUiState = ZoomControlUiState.Enabled(
                listOf(1f, 2f, 5f),
                primaryLensFacing = LensFacing.FRONT,
                primaryZoomRatio = 1f
            ),

            zoomUiState = ZoomUiState.Enabled(
                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            ),
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            videoRecordingState = VideoRecordingState.Active.Recording(0L, .9, 1_000_000_000)
        )
    }
}
