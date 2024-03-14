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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.feature.preview.MultipleEventsCutter
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.feature.preview.VideoRecordingState
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSettingsIndicators
import com.google.jetpackcamera.feature.quicksettings.ui.ToggleQuickSettingsButton
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SupportedStabilizationMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    previewUiState: PreviewUiState,
    zoomLevelDisplayState: ZoomLevelDisplayState = remember { ZoomLevelDisplayState() },
    onNavigateToSettings: () -> Unit,
    previewMode: PreviewMode,
    onFlipCamera: () -> Unit = {},
    onChangeFlash: (FlashMode) -> Unit = {},
    onToggleQuickSettings: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        (PreviewViewModel.ImageCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    blinkState: BlinkState
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
        Box(Modifier.fillMaxSize()) {
            if (previewUiState.videoRecordingState == VideoRecordingState.INACTIVE) {
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
                zoomLevel = previewUiState.zoomScale,
                showZoomLevel = zoomLevelDisplayState.showZoomLevel,
                isQuickSettingsOpen = previewUiState.quickSettingsIsOpen,
                currentCameraSettings = previewUiState.currentCameraSettings,
                videoRecordingState = previewUiState.videoRecordingState,
                previewMode = previewMode,
                onFlipCamera = onFlipCamera,
                onCaptureImage = onCaptureImage,
                onCaptureImageWithUri = onCaptureImageWithUri,
                onToggleQuickSettings = onToggleQuickSettings,
                onStartVideoRecording = onStartVideoRecording,
                onStopVideoRecording = onStopVideoRecording,
                blinkState = blinkState
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
                Modifier.padding(12.dp).testTag(SETTINGS_BUTTON),
                onNavigateToSettings
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
                supportedStabilizationMode = currentCameraSettings.supportedStabilizationModes,
                videoStabilization = currentCameraSettings.videoCaptureStabilization,
                previewStabilization = currentCameraSettings.previewStabilization
            )
        }
    }
}

@Composable
private fun ControlsBottom(
    zoomLevel: Float,
    showZoomLevel: Boolean,
    isQuickSettingsOpen: Boolean,
    currentCameraSettings: CameraAppSettings,
    videoRecordingState: VideoRecordingState,
    previewMode: PreviewMode,
    modifier: Modifier = Modifier,
    onFlipCamera: () -> Unit = {},
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        (PreviewViewModel.ImageCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onToggleQuickSettings: () -> Unit = {},
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    blinkState: BlinkState? = null
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (showZoomLevel) {
            ZoomScaleText(zoomLevel)
        }

        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.SpaceEvenly) {
                if (!isQuickSettingsOpen && videoRecordingState == VideoRecordingState.INACTIVE) {
                    FlipCameraButton(
                        modifier = modifier.testTag(FLIP_CAMERA_BUTTON),
                        onClick = onFlipCamera,
                        // enable only when phone has front and rear camera
                        enabledCondition = currentCameraSettings.isBackCameraAvailable &&
                            currentCameraSettings.isFrontCameraAvailable
                    )
                }
            }
            CaptureButton(
                previewMode = previewMode,
                isQuickSettingsOpen = isQuickSettingsOpen,
                videoRecordingState = videoRecordingState,
                onCaptureImage = onCaptureImage,
                onCaptureImageWithUri = onCaptureImageWithUri,
                onToggleQuickSettings = onToggleQuickSettings,
                onStartVideoRecording = onStartVideoRecording,
                onStopVideoRecording = onStopVideoRecording,
                blinkState = blinkState
            )
            Row(Modifier.weight(1f)) {
                /*TODO("Place other components here") */
            }
        }
    }
}

@Composable
private fun CaptureButton(
    previewMode: PreviewMode,
    isQuickSettingsOpen: Boolean,
    videoRecordingState: VideoRecordingState,
    modifier: Modifier = Modifier,
    onCaptureImage: () -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        (PreviewViewModel.ImageCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onToggleQuickSettings: () -> Unit = {},
    onStartVideoRecording: () -> Unit = {},
    onStopVideoRecording: () -> Unit = {},
    blinkState: BlinkState? = null
) {
    val multipleEventsCutter = remember { MultipleEventsCutter() }
    val context = LocalContext.current
    CaptureButton(
        modifier = modifier.testTag(CAPTURE_BUTTON),
        onClick = {
            blinkState?.scope?.launch { blinkState.play() }
            multipleEventsCutter.processEvent {
                when (previewMode) {
                    is PreviewMode.StandardMode -> {
                        onCaptureImage()
                    }

                    is PreviewMode.ExternalImageCaptureMode -> {
                        onCaptureImageWithUri(
                            context.contentResolver,
                            previewMode.imageCaptureUri,
                            previewMode.onImageCapture
                        )
                    }
                }
            }
            if (isQuickSettingsOpen) {
                onToggleQuickSettings()
            }
        },
        onLongPress = {
            onStartVideoRecording()
            if (isQuickSettingsOpen) {
                onToggleQuickSettings()
            }
        },
        onRelease = { onStopVideoRecording() },
        videoRecordingState = videoRecordingState
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
                supportedStabilizationModes = listOf(SupportedStabilizationMode.HIGH_QUALITY),
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
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(),
            videoRecordingState = VideoRecordingState.INACTIVE,
            previewMode = PreviewMode.StandardMode
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_NoZoomLevel() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            zoomLevel = 1.3f,
            showZoomLevel = false,
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(),
            videoRecordingState = VideoRecordingState.INACTIVE,
            previewMode = PreviewMode.StandardMode
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_QuickSettingsOpen() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = true,
            currentCameraSettings = CameraAppSettings(),
            videoRecordingState = VideoRecordingState.INACTIVE,
            previewMode = PreviewMode.StandardMode
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_NoFlippableCamera() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(isBackCameraAvailable = false),
            videoRecordingState = VideoRecordingState.INACTIVE,
            previewMode = PreviewMode.StandardMode
        )
    }
}

@Preview(backgroundColor = 0xFF000000, showBackground = true)
@Composable
private fun Preview_ControlsBottom_Recording() {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        ControlsBottom(
            zoomLevel = 1.3f,
            showZoomLevel = true,
            isQuickSettingsOpen = false,
            currentCameraSettings = CameraAppSettings(),
            videoRecordingState = VideoRecordingState.ACTIVE,
            previewMode = PreviewMode.StandardMode
        )
    }
}
