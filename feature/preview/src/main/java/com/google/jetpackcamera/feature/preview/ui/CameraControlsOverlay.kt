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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.google.jetpackcamera.feature.preview.CaptureModeToggleUiState
import com.google.jetpackcamera.feature.preview.MultipleEventsCutter
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSettingsIndicators
import com.google.jetpackcamera.feature.preview.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import kotlinx.coroutines.delay

class ZoomLevelDisplayState(showInitially: Boolean = false) {
    private var _showZoomLevel = mutableStateOf(showInitially)
    val showZoomLevel: Boolean get() = _showZoomLevel.value

    suspend fun showZoomLevel() {
        _showZoomLevel.value = true
        delay(3000)
        _showZoomLevel.value = false
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
    onMuteAudio: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        Boolean,
        (PreviewViewModel.ImageCaptureEvent) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (PreviewViewModel.VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStopVideoRecording: () -> Unit = {}
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
        Box(modifier.fillMaxSize()) {
            if (previewUiState.videoRecordingState is VideoRecordingState.Inactive) {
                ControlsTop(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    isQuickSettingsOpen = previewUiState.quickSettingsIsOpen,
                    currentCameraSettings = previewUiState.currentCameraSettings,
                    onNavigateToSettings = onNavigateToSettings,
                    onChangeFlash = onChangeFlash,
                    onToggleQuickSettings = onToggleQuickSettings
                )
            }

            ControlsBottom(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                previewUiState = previewUiState,
                zoomLevel = previewUiState.zoomScale,
                physicalCameraId = previewUiState.currentPhysicalCameraId,
                logicalCameraId = previewUiState.currentLogicalCameraId,
                showZoomLevel = zoomLevelDisplayState.showZoomLevel,
                isQuickSettingsOpen = previewUiState.quickSettingsIsOpen,
                currentCameraSettings = previewUiState.currentCameraSettings,
                systemConstraints = previewUiState.systemConstraints,
                videoRecordingState = previewUiState.videoRecordingState,
                onFlipCamera = onFlipCamera,
                onCaptureImage = onCaptureImage,
                onCaptureImageWithUri = onCaptureImageWithUri,
                onToggleQuickSettings = onToggleQuickSettings,
                onToggleAudioMuted = onMuteAudio,
                onTogglePause = onTogglePause,
                onChangeImageFormat = onChangeImageFormat,
                onToggleWhenDisabled = onToggleWhenDisabled,
                onStartVideoRecording = onStartVideoRecording,
                onStopVideoRecording = onStopVideoRecording
            )
        }
    }
}

@Composable
private fun ControlsTop(
    isQuickSettingsOpen: Boolean,
    currentCameraSettings: CameraAppSettings,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {}
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            // button to open default settings page
            SettingsNavButton(
                modifier = Modifier
                    .padding(12.dp)
                    .testTag(SETTINGS_BUTTON),
                onNavigateToSettings = onNavigateToSettings
            )
            if (!isQuickSettingsOpen) {
                QuickSettingsIndicators(
                    currentFlashMode = currentCameraSettings.flashMode,
                    onFlashModeClick = onChangeFlash
                )
            }
        }

        // quick settings button
        ToggleQuickSettingsButton(onToggleQuickSettings, isQuickSettingsOpen)

        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StabilizationIcon(
                videoStabilization = currentCameraSettings.videoCaptureStabilization,
                previewStabilization = currentCameraSettings.previewStabilization
            )
            LowLightBoostIcon(
                lowLightBoost = currentCameraSettings.lowLightBoost
            )
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
    currentCameraSettings: CameraAppSettings,
    systemConstraints: SystemConstraints,
    videoRecordingState: VideoRecordingState,
    onFlipCamera: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        Boolean,
        (PreviewViewModel.ImageCaptureEvent) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onToggleQuickSettings: () -> Unit = {},
    onToggleAudioMuted: () -> Unit = {},
    onTogglePause: () -> Unit = {},
    onChangeImageFormat: (ImageOutputFormat) -> Unit = {},
    onToggleWhenDisabled: (CaptureModeToggleUiState.DisabledReason) -> Unit = {},
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (PreviewViewModel.VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStopVideoRecording: () -> Unit = {}
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontSize = 20.sp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (showZoomLevel) {
                    ZoomScaleText(zoomLevel)
                }
                if (previewUiState.isDebugMode) {
                    CurrentCameraIdText(physicalCameraId, logicalCameraId)
                }
                ElapsedTimeText(
                    modifier = Modifier.testTag(ELAPSED_TIME_TAG),
                    videoRecordingState = videoRecordingState,
                    elapsedNs = when (previewUiState.videoRecordingState) {
                        is VideoRecordingState.Active ->
                            previewUiState.videoRecordingState.elapsedTimeNanos

                        is VideoRecordingState.Inactive ->
                            previewUiState.videoRecordingState.finalElapsedTimeNanos
                    }
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
                if (!isQuickSettingsOpen && videoRecordingState is VideoRecordingState.Inactive) {
                    FlipCameraButton(
                        modifier = Modifier.testTag(FLIP_CAMERA_BUTTON),
                        onClick = onFlipCamera,
                        // enable only when phone has front and rear camera
                        enabledCondition = systemConstraints.availableLenses.size > 1
                    )
                } else if (!isQuickSettingsOpen &&
                    videoRecordingState is VideoRecordingState.Active
                ) {
                    PauseResumeToggleButton(
                        onTogglePause = onTogglePause,
                        size = 75,
                        currentRecordingState = videoRecordingState
                    )
                }
            }
            CaptureButton(
                previewUiState = previewUiState,
                isQuickSettingsOpen = isQuickSettingsOpen,
                videoRecordingState = videoRecordingState,
                onCaptureImage = onCaptureImage,
                onCaptureImageWithUri = onCaptureImageWithUri,
                onToggleQuickSettings = onToggleQuickSettings,
                onStartVideoRecording = onStartVideoRecording,
                onStopVideoRecording = onStopVideoRecording
            )
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (videoRecordingState is VideoRecordingState.Active) {
                    AmplitudeVisualizer(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        onToggleMute = onToggleAudioMuted,
                        size = 75,
                        audioAmplitude = videoRecordingState.audioAmplitude
                    )
                } else {
                    if (!isQuickSettingsOpen &&
                        previewUiState.captureModeToggleUiState is CaptureModeToggleUiState.Visible
                    ) {
                        CaptureModeToggleButton(
                            uiState = previewUiState.captureModeToggleUiState,
                            onChangeImageFormat = onChangeImageFormat,
                            onToggleWhenDisabled = onToggleWhenDisabled,
                            modifier = Modifier.testTag(CAPTURE_MODE_TOGGLE_BUTTON)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CaptureButton(
    previewUiState: PreviewUiState.Ready,
    isQuickSettingsOpen: Boolean,
    videoRecordingState: VideoRecordingState,
    modifier: Modifier = Modifier,
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        Boolean,
        (PreviewViewModel.ImageCaptureEvent) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onToggleQuickSettings: () -> Unit = {},
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (PreviewViewModel.VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStopVideoRecording: () -> Unit = {}
) {
    val multipleEventsCutter = remember { MultipleEventsCutter() }
    val context = LocalContext.current

    CaptureButton(
        modifier = modifier.testTag(CAPTURE_BUTTON),
        onClick = {
            multipleEventsCutter.processEvent {
                when (previewUiState.previewMode) {
                    is PreviewMode.StandardMode -> {
                        onCaptureImageWithUri(
                            context.contentResolver,
                            null,
                            true,
                            previewUiState.previewMode.onImageCapture
                        )
                    }

                    is PreviewMode.ExternalImageCaptureMode -> {
                        onCaptureImageWithUri(
                            context.contentResolver,
                            previewUiState.previewMode.imageCaptureUri,
                            false,
                            previewUiState.previewMode.onImageCapture
                        )
                    }

                    else -> {
                        onCaptureImageWithUri(
                            context.contentResolver,
                            null,
                            false
                        ) {}
                    }
                }
            }
            if (isQuickSettingsOpen) {
                onToggleQuickSettings()
            }
        },
        onLongPress = {
            when (previewUiState.previewMode) {
                is PreviewMode.StandardMode -> {
                    onStartVideoRecording(null, false) {}
                }

                is PreviewMode.ExternalVideoCaptureMode -> {
                    onStartVideoRecording(
                        previewUiState.previewMode.videoCaptureUri,
                        true,
                        previewUiState.previewMode.onVideoCapture
                    )
                }

                else -> {
                    onStartVideoRecording(null, false) {}
                }
            }
            if (isQuickSettingsOpen) {
                onToggleQuickSettings()
            }
        },
        onRelease = {
            onStopVideoRecording()
        },
        videoRecordingState = videoRecordingState
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
            isQuickSettingsOpen = true,
            currentCameraSettings = CameraAppSettings()
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_QuickSettingsClosed() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings()
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_FlashModeOn() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(flashMode = FlashMode.ON)
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_FlashModeAuto() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(flashMode = FlashMode.AUTO)
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsTop_WithStabilization() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsTop(
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(
                videoCaptureStabilization = Stabilization.ON,
                previewStabilization = Stabilization.ON
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
                currentCameraSettings = CameraAppSettings(),
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Inactive()
            ),
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(),
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
                currentCameraSettings = CameraAppSettings(),
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Inactive()
            ),
            zoomLevel = 1.3f,
            showZoomLevel = false,
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(),
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
                currentCameraSettings = CameraAppSettings(),
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Inactive()
            ),
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = true,
            currentCameraSettings = CameraAppSettings(),
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
                currentCameraSettings = CameraAppSettings(),
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Inactive()
            ),
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(),
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
                currentCameraSettings = CameraAppSettings(),
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                videoRecordingState = VideoRecordingState.Active.Recording(0L, .9, 1_000_000_000)

            ),
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(),
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            videoRecordingState = VideoRecordingState.Active.Recording(0L, .9, 1_000_000_000)
        )
    }
}
