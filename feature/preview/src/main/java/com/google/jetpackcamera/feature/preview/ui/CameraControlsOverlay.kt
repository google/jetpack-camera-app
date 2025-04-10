/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview.ui

import android.content.ContentResolver
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.google.jetpackcamera.feature.preview.CaptureButtonUiState
import com.google.jetpackcamera.feature.preview.CaptureModeUiState
import com.google.jetpackcamera.feature.preview.DEFAULT_CAPTURE_BUTTON_STATE
import com.google.jetpackcamera.feature.preview.DisabledReason
import com.google.jetpackcamera.feature.preview.ElapsedTimeUiState
import com.google.jetpackcamera.feature.preview.FlashModeUiState
import com.google.jetpackcamera.feature.preview.MultipleEventsCutter
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.SingleSelectableState
import com.google.jetpackcamera.feature.preview.StabilizationUiState
import com.google.jetpackcamera.feature.preview.ZoomUiState
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSettingsIndicators
import com.google.jetpackcamera.feature.preview.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.feature.preview.ui.debug.DebugOverlayToggleButton
import com.google.jetpackcamera.settings.model.CameraZoomRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.model.VideoQuality
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
    previewUiState: PreviewUiState.Ready,
    modifier: Modifier = Modifier,
    zoomLevelDisplayState: ZoomLevelDisplayState = remember { ZoomLevelDisplayState() },
    onNavigateToSettings: () -> Unit = {},
    onSetCaptureMode: (CaptureMode) -> Unit = {},
    onFlipCamera: () -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onChangeImageFormat: (ImageOutputFormat) -> Unit = {},
    onDisabledCaptureMode: (DisabledReason) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onToggleDebugOverlay: () -> Unit = {},
    onToggleAudio: () -> Unit = {},
    onSetPause: (Boolean) -> Unit = {},
    onSetZoom: (CameraZoomRatio) -> Unit = {},
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
    onImageWellClick: () -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit
) {
    // Show the current zoom level for a short period of time, only when the level changes.
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(previewUiState.zoomUiState) {
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
            if (previewUiState.videoRecordingState is VideoRecordingState.Inactive) {
                ControlsTop(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    isQuickSettingsOpen = previewUiState.quickSettingsIsOpen,
                    isDebugMode = previewUiState.debugUiState.isDebugMode,
                    onNavigateToSettings = onNavigateToSettings,
                    onChangeFlash = onChangeFlash,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onToggleDebugOverlay = onToggleDebugOverlay,
                    stabilizationUiState = previewUiState.stabilizationUiState,
                    videoQuality = previewUiState.videoQuality,
                    flashModeUiState = previewUiState.flashModeUiState
                )
            }

            ControlsBottom(
                modifier = Modifier
                    // padding to avoid snackbar
                    .padding(bottom = 60.dp)
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                previewUiState = previewUiState,
                zoomUiState = previewUiState.zoomUiState,
                physicalCameraId = previewUiState.currentPhysicalCameraId,
                logicalCameraId = previewUiState.currentLogicalCameraId,
                showZoomLevel = zoomLevelDisplayState.showZoomLevel,
                isQuickSettingsOpen = previewUiState.quickSettingsIsOpen,
                systemConstraints = previewUiState.systemConstraints,
                videoRecordingState = previewUiState.videoRecordingState,
                onSetCaptureMode = onSetCaptureMode,
                onFlipCamera = onFlipCamera,
                onSetZoom = onSetZoom,
                onCaptureImageWithUri = onCaptureImageWithUri,
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
    isDebugMode: Boolean = false,
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
        if (isDebugMode) {
            DebugOverlayToggleButton(toggleIsOpen = onToggleDebugOverlay)
        }
    }
}

@Composable
private fun ControlsBottom(
    modifier: Modifier = Modifier,
    previewUiState: PreviewUiState.Ready,
    physicalCameraId: String? = null,
    logicalCameraId: String? = null,
    zoomUiState: ZoomUiState,
    showZoomLevel: Boolean,
    isQuickSettingsOpen: Boolean,
    systemConstraints: SystemConstraints,
    videoRecordingState: VideoRecordingState,
    onFlipCamera: () -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        Boolean,
        (PreviewViewModel.ImageCaptureEvent, Int) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onToggleQuickSettings: () -> Unit = {},
    onToggleAudio: () -> Unit = {},
    onSetPause: (Boolean) -> Unit = {},
    onSetCaptureMode: (CaptureMode) -> Unit = {},
    onDisabledCaptureMode: (DisabledReason) -> Unit = {},
    onSetZoom: (CameraZoomRatio) -> Unit = {},
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (PreviewViewModel.VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
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
                    visible = (showZoomLevel && zoomUiState is ZoomUiState.Enabled),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    ZoomRatioText(zoomUiState as ZoomUiState.Enabled)
                }
                val animationDuration = remember{ mutableStateOf<Int?>(null) }
                Switch(checked = animationDuration.value != null, onCheckedChange = {if (it) animationDuration.value = 500 else animationDuration.value = null})
                Row() {
                    (zoomUiState as? ZoomUiState.Enabled)?.let {
                        ZoomButton(
                            targetZoom = it.primaryZoomRange.lower,
                            zoomUiState = it,
                            onZoomChanged = onSetZoom,
                            animationDurationMillis = animationDuration.value
                        )
                        ZoomButton(
                            targetZoom = 1f,
                            zoomUiState = it,
                            onZoomChanged = onSetZoom,
                            animationDurationMillis = animationDuration.value
                        )
                        if (it.primaryZoomRange.contains(2f))
                            ZoomButton(
                                targetZoom = 2f,
                                zoomUiState = it,
                                onZoomChanged = onSetZoom,
                                animationDurationMillis = animationDuration.value
                            )
                        if (it.primaryZoomRange.contains(8f))
                            ZoomButton(
                                targetZoom = 8f,
                                zoomUiState = it,
                                onZoomChanged = onSetZoom,
                                animationDurationMillis = animationDuration.value
                            )
                        ZoomButton(
                            targetZoom = it.primaryZoomRange.upper,
                            zoomUiState = it,
                            onZoomChanged = onSetZoom,
                            animationDurationMillis = animationDuration.value
                        )
                    }
                }
                if (previewUiState.debugUiState.isDebugMode) {
                    CurrentCameraIdText(physicalCameraId, logicalCameraId)
                }
                if (previewUiState.elapsedTimeUiState is ElapsedTimeUiState.Enabled) {
                    AnimatedVisibility(
                        visible = (
                                previewUiState.videoRecordingState is
                                        VideoRecordingState.Active
                                ),
                        enter = fadeIn(),
                        exit = fadeOut(animationSpec = tween(delayMillis = 1_500))
                    ) {
                        ElapsedTimeText(
                            modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                            elapsedTimeUiState = previewUiState.elapsedTimeUiState
                        )
                    }
                }
            }
        }

        Column {
            if (!isQuickSettingsOpen &&
                previewUiState.captureModeToggleUiState
                        is CaptureModeUiState.Enabled
            ) {
                // TODO(yasith): Align to end of ImageWell based on alignment lines
                Box(
                    Modifier.align(Alignment.End).padding(end = 12.dp)
                ) {
                    CaptureModeToggleButton(
                        uiState = previewUiState.captureModeToggleUiState,
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
                                lensFacing = previewUiState.currentCameraSettings.cameraLensFacing,
                                // enable only when phone has front and rear camera
                                enabledCondition = systemConstraints.availableLenses.size > 1
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
                    captureButtonUiState = previewUiState.captureButtonUiState,
                    previewMode = previewUiState.previewMode,
                    isQuickSettingsOpen = isQuickSettingsOpen,
                    onCaptureImageWithUri = onCaptureImageWithUri,
                    onSetZoom = onSetZoom,
                    onToggleQuickSettings = onToggleQuickSettings,
                    onStartVideoRecording = onStartVideoRecording,
                    onStopVideoRecording = onStopVideoRecording,
                    onLockVideoRecording = onLockVideoRecording
                )

                Box(
                    modifier = Modifier.weight(1f).size(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (videoRecordingState is VideoRecordingState.Active) {
                        AmplitudeVisualizer(
                            modifier = Modifier.fillMaxSize(),
                            onToggleAudio = onToggleAudio,
                            audioUiState = previewUiState.audioUiState
                        )
                    } else if (!isQuickSettingsOpen &&
                        previewUiState.previewMode is PreviewMode.StandardMode
                    ) {
                        ImageWell(
                            imageWellUiState = previewUiState.imageWellUiState,
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
    previewMode: PreviewMode,
    onToggleQuickSettings: () -> Unit = {},
    onSetZoom: (CameraZoomRatio) -> Unit = {},
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
    onLockVideoRecording: (Boolean) -> Unit = {}
) {
    val multipleEventsCutter = remember { MultipleEventsCutter() }
    val context = LocalContext.current

    CaptureButton(
        modifier = modifier.testTag(CAPTURE_BUTTON),
        onSetZoom = onSetZoom,
        onImageCapture = {
            if (captureButtonUiState is CaptureButtonUiState.Enabled) {
                multipleEventsCutter.processEvent {
                    when (previewMode) {
                        is PreviewMode.StandardMode -> {
                            onCaptureImageWithUri(
                                context.contentResolver,
                                null,
                                true
                            ) { event: PreviewViewModel.ImageCaptureEvent, _: Int ->
                                previewMode.onImageCapture(event)
                            }
                        }

                        is PreviewMode.ExternalImageCaptureMode -> {
                            onCaptureImageWithUri(
                                context.contentResolver,
                                previewMode.imageCaptureUri,
                                false
                            ) { event: PreviewViewModel.ImageCaptureEvent, _: Int ->
                                previewMode.onImageCapture(event)
                            }
                        }

                        is PreviewMode.ExternalMultipleImageCaptureMode -> {
                            val ignoreUri =
                                previewMode.imageCaptureUris.isNullOrEmpty()
                            onCaptureImageWithUri(
                                context.contentResolver,
                                null,
                                previewMode.imageCaptureUris.isNullOrEmpty() ||
                                        ignoreUri,
                                previewMode.onImageCapture
                            )
                        }

                        else -> {
                            onCaptureImageWithUri(
                                context.contentResolver,
                                null,
                                false
                            ) { _: PreviewViewModel.ImageCaptureEvent, _: Int -> }
                        }
                    }
                }
            }
            if (isQuickSettingsOpen) {
                onToggleQuickSettings()
            }
        },
        onStartRecording = {
            if (captureButtonUiState is CaptureButtonUiState.Enabled) {
                when (previewMode) {
                    is PreviewMode.StandardMode -> {
                        onStartVideoRecording(null, false) {}
                    }

                    is PreviewMode.ExternalVideoCaptureMode -> {
                        onStartVideoRecording(
                            previewMode.videoCaptureUri,
                            true,
                            previewMode.onVideoCapture
                        )
                    }

                    else -> {
                        onStartVideoRecording(null, false) {}
                    }
                }
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
    uiState: CaptureModeUiState.Enabled,
    onChangeCaptureMode: (CaptureMode) -> Unit,
    onToggleWhenDisabled: (DisabledReason) -> Unit,
    modifier: Modifier = Modifier
) {
    // Captures hdr image (left) when output format is UltraHdr, else captures hdr video (right).
    val initialState =
        when (uiState.currentSelection) {
            CaptureMode.IMAGE_ONLY -> ToggleState.Left
            CaptureMode.VIDEO_ONLY -> ToggleState.Right
            CaptureMode.STANDARD -> TODO("toggle should not be visible for STANDARD mode")
        }
    val enabled =
        uiState.videoOnlyCaptureState == SingleSelectableState.Selectable &&
                uiState.imageOnlyCaptureState == SingleSelectableState.Selectable
    ToggleButton(
        leftIcon = if (uiState.currentSelection ==
            CaptureMode.IMAGE_ONLY
        ) {
            rememberVectorPainter(image = Icons.Filled.CameraAlt)
        } else {
            rememberVectorPainter(image = Icons.Outlined.CameraAlt)
        },
        rightIcon = if (uiState.currentSelection ==
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
            val disabledReason: DisabledReason? =
                (uiState.videoOnlyCaptureState as? SingleSelectableState.Disabled)?.disabledReason
                    ?: (uiState.imageOnlyCaptureState as? SingleSelectableState.Disabled)
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
                availableFlashModes = listOf(FlashMode.OFF, FlashMode.ON),
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
                availableFlashModes = listOf(FlashMode.OFF, FlashMode.ON, FlashMode.AUTO),
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
            previewUiState = PreviewUiState.Ready(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            videoRecordingState = VideoRecordingState.Inactive(),
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
            previewUiState = PreviewUiState.Ready(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomUiState = ZoomUiState.Enabled(
                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            ),
            showZoomLevel = false,
            isQuickSettingsOpen = false,
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            videoRecordingState = VideoRecordingState.Inactive()
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_QuickSettingsOpen() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            previewUiState = PreviewUiState.Ready(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomUiState = ZoomUiState.Enabled(
                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            ),
            showZoomLevel = true,
            isQuickSettingsOpen = true,
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            videoRecordingState = VideoRecordingState.Inactive()
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_NoFlippableCamera() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            previewUiState = PreviewUiState.Ready(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomUiState = ZoomUiState.Enabled(
                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            ),
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
                availableLenses = listOf(LensFacing.FRONT),
                perLensConstraints = mapOf(
                    LensFacing.FRONT to
                            TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[LensFacing.FRONT]!!
                )
            ),
            videoRecordingState = VideoRecordingState.Inactive()
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_Recording() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            previewUiState = PreviewUiState.Ready(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Active.Recording(0L, .9, 1_000_000_000),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomUiState = ZoomUiState.Enabled(
                primaryZoomRange = Range(1.0f, 10.0f),
                primaryZoomRatio = 1.0f
            ),
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            videoRecordingState = VideoRecordingState.Active.Recording(0L, .9, 1_000_000_000)
        )
    }
}
