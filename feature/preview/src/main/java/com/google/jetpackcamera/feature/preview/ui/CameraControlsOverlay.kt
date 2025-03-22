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
import com.google.jetpackcamera.feature.preview.CaptureButtonUiState
import com.google.jetpackcamera.feature.preview.CaptureModeToggleUiState
import com.google.jetpackcamera.feature.preview.DEFAULT_CAPTURE_BUTTON_STATE
import com.google.jetpackcamera.feature.preview.ElapsedTimeUiState
import com.google.jetpackcamera.feature.preview.FlashModeUiState
import com.google.jetpackcamera.feature.preview.MultipleEventsCutter
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.StabilizationUiState
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSettingsIndicators
import com.google.jetpackcamera.feature.preview.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.feature.preview.ui.debug.DebugOverlayToggleButton
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
    onFlipCamera: () -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onChangeImageFormat: (ImageOutputFormat) -> Unit = {},
    onToggleWhenDisabled: (CaptureModeToggleUiState.DisabledReason) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onToggleDebugOverlay: () -> Unit = {},
    onToggleAudio: () -> Unit = {},
    onSetPause: (Boolean) -> Unit = {},
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
    onImageWellClick: (uri: Uri?) -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit
) {
    // Show the current zoom level for a short period of time, only when the level changes.
    var firstRun by remember { mutableStateOf(true) }
    LaunchedEffect(previewUiState.zoomScale) {
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
                zoomLevel = previewUiState.zoomScale,
                physicalCameraId = previewUiState.currentPhysicalCameraId,
                logicalCameraId = previewUiState.currentLogicalCameraId,
                showZoomLevel = zoomLevelDisplayState.showZoomLevel,
                isQuickSettingsOpen = previewUiState.quickSettingsIsOpen,
                systemConstraints = previewUiState.systemConstraints,
                videoRecordingState = previewUiState.videoRecordingState,
                onFlipCamera = onFlipCamera,
                onCaptureImageWithUri = onCaptureImageWithUri,
                onToggleQuickSettings = onToggleQuickSettings,
                onToggleAudio = onToggleAudio,
                onSetPause = onSetPause,
                onChangeImageFormat = onChangeImageFormat,
                onToggleWhenDisabled = onToggleWhenDisabled,
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
    zoomLevel: Float,
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
    onChangeImageFormat: (ImageOutputFormat) -> Unit = {},
    onToggleWhenDisabled: (CaptureModeToggleUiState.DisabledReason) -> Unit = {},
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (PreviewViewModel.VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStopVideoRecording: () -> Unit = {},
    onImageWellClick: (uri: Uri?) -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 20.sp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showZoomLevel) {
                    ZoomScaleText(zoomLevel)
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
                    is CaptureModeToggleUiState.Visible
            ) {
                // TODO(yasith): Align to end of ImageWell based on alignment lines
                Box(
                    Modifier.align(Alignment.End).padding(end = 12.dp)
                ) {
                    CaptureModeToggleButton(
                        uiState = previewUiState.captureModeToggleUiState,
                        onChangeImageFormat = onChangeImageFormat,
                        onToggleWhenDisabled = onToggleWhenDisabled,
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
                    onToggleQuickSettings = onToggleQuickSettings,
                    onStartVideoRecording = onStartVideoRecording,
                    onStopVideoRecording = onStopVideoRecording,
                    onLockVideoRecording = onLockVideoRecording
                )
                Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                    if (videoRecordingState is VideoRecordingState.Active) {
                        AmplitudeVisualizer(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            onToggleAudio = onToggleAudio,
                            audioUiState = previewUiState.audioUiState
                        )
                    } else {
                        Column {
                            if (!isQuickSettingsOpen &&
                                previewUiState.previewMode is PreviewMode.StandardMode
                            ) {
                                ImageWell(
                                    modifier = Modifier.weight(1f),
                                    imageWellUiState = previewUiState.imageWellUiState,
                                    onClick = onImageWellClick
                                )
                            }
                        }
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
        onStopRecording = { onStopVideoRecording() },
        captureButtonUiState = captureButtonUiState,
        onLockVideoRecording = onLockVideoRecording
    )
}

@Composable
private fun CaptureModeToggleButton(
    uiState: CaptureModeToggleUiState.Visible,
    onChangeImageFormat: (ImageOutputFormat) -> Unit,
    onToggleWhenDisabled: (CaptureModeToggleUiState.DisabledReason) -> Unit,
    modifier: Modifier = Modifier
) {
    // Captures hdr image (left) when output format is UltraHdr, else captures hdr video (right).
    val initialState =
        when (uiState.currentMode) {
            CaptureModeToggleUiState.ToggleMode.CAPTURE_TOGGLE_IMAGE -> ToggleState.Left
            CaptureModeToggleUiState.ToggleMode.CAPTURE_TOGGLE_VIDEO -> ToggleState.Right
        }
    ToggleButton(
        leftIcon = if (uiState.currentMode ==
            CaptureModeToggleUiState.ToggleMode.CAPTURE_TOGGLE_IMAGE
        ) {
            rememberVectorPainter(image = Icons.Filled.CameraAlt)
        } else {
            rememberVectorPainter(image = Icons.Outlined.CameraAlt)
        },
        rightIcon = if (uiState.currentMode ==
            CaptureModeToggleUiState.ToggleMode.CAPTURE_TOGGLE_VIDEO
        ) {
            rememberVectorPainter(image = Icons.Filled.Videocam)
        } else {
            rememberVectorPainter(image = Icons.Outlined.Videocam)
        },
        initialState = initialState,
        onToggleStateChanged = {
            val imageFormat = when (it) {
                ToggleState.Left -> ImageOutputFormat.JPEG_ULTRA_HDR
                ToggleState.Right -> ImageOutputFormat.JPEG
            }
            onChangeImageFormat(imageFormat)
        },
        onToggleWhenDisabled = {
            check(uiState is CaptureModeToggleUiState.Disabled)
            onToggleWhenDisabled(uiState.disabledReason)
        },
        enabled = uiState is CaptureModeToggleUiState.Enabled,
        leftIconDescription =
        stringResource(id = R.string.capture_mode_image_capture_content_description),
        rightIconDescription =
        stringResource(id = R.string.capture_mode_video_recording_content_description),
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
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            videoRecordingState = VideoRecordingState.Inactive()
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
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomLevel = 1.3f,
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
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomLevel = 1.3f,
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
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomLevel = 1.3f,
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
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Active.Recording(0L, .9, 1_000_000_000),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            videoRecordingState = VideoRecordingState.Active.Recording(0L, .9, 1_000_000_000)
        )
    }
}
