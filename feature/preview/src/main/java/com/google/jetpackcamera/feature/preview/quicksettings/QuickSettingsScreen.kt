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
package com.google.jetpackcamera.feature.preview.quicksettings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.feature.preview.CaptureModeUiState
import com.google.jetpackcamera.feature.preview.DEFAULT_CAPTURE_BUTTON_STATE
import com.google.jetpackcamera.feature.preview.FlashModeUiState
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE
import com.google.jetpackcamera.feature.preview.quicksettings.ui.FocusedQuickSetCaptureMode
import com.google.jetpackcamera.feature.preview.quicksettings.ui.FocusedQuickSetRatio
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_BACKGROUND_FOCUSED
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_BACKGROUND_MAIN
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLASH_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_HDR_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_STREAM_CONFIG_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickFlipCamera
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetCaptureMode
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetConcurrentCamera
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetFlash
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetHdr
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetRatio
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetStreamConfig
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSettingsGrid
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DEFAULT_HDR_DYNAMIC_RANGE
import com.google.jetpackcamera.settings.model.DEFAULT_HDR_IMAGE_OUTPUT
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StreamConfig
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.model.forCurrentLens

/**
 * The UI component for quick settings.
 */
@Composable
fun QuickSettingsScreenOverlay(
    modifier: Modifier = Modifier,
    previewUiState: PreviewUiState.Ready,
    currentCameraSettings: CameraAppSettings,
    toggleQuickSettings: () -> Unit,
    onLensFaceClick: (lensFace: LensFacing) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit,
    onStreamConfigClick: (streamConfig: StreamConfig) -> Unit,
    onDynamicRangeClick: (dynamicRange: DynamicRange) -> Unit,
    onImageOutputFormatClick: (imageOutputFormat: ImageOutputFormat) -> Unit,
    onConcurrentCameraModeClick: (concurrentCameraMode: ConcurrentCameraMode) -> Unit,
    onCaptureModeClick: (CaptureMode) -> Unit,
    isOpen: Boolean = false
) {
    var focusedQuickSetting by remember {
        mutableStateOf(FocusedQuickSetting.NONE)
    }

    AnimatedVisibility(
        visible = isOpen,
        enter = slideInVertically(initialOffsetY = { -it / 8 }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it / 16 }) + fadeOut()
    ) {
        val onBack = {
            when (focusedQuickSetting) {
                FocusedQuickSetting.NONE -> toggleQuickSettings()
                else -> focusedQuickSetting = FocusedQuickSetting.NONE
            }
        }
        // close out of focused quick setting
        if (!isOpen) {
            focusedQuickSetting = FocusedQuickSetting.NONE
        }

        BackHandler(onBack = onBack)
        Column(
            modifier =
            modifier
                .testTag(
                    when (focusedQuickSetting) {
                        FocusedQuickSetting.NONE -> QUICK_SETTINGS_BACKGROUND_MAIN
                        else -> QUICK_SETTINGS_BACKGROUND_FOCUSED
                    }
                )
                .fillMaxSize()
                .background(color = Color.Black.copy(alpha = 0.7f))
                .clickable(
                    onClick = onBack,
                    indication = null,
                    interactionSource = null
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpandedQuickSettingsUi(
                previewUiState = previewUiState,
                currentCameraSettings = currentCameraSettings,
                focusedQuickSetting = focusedQuickSetting,
                setFocusedQuickSetting = { enum: FocusedQuickSetting ->
                    focusedQuickSetting = enum
                },
                onLensFaceClick = onLensFaceClick,
                onFlashModeClick = onFlashModeClick,
                onAspectRatioClick = onAspectRatioClick,
                onStreamConfigClick = onStreamConfigClick,
                onDynamicRangeClick = onDynamicRangeClick,
                onImageOutputFormatClick = onImageOutputFormatClick,
                onConcurrentCameraModeClick = onConcurrentCameraModeClick,
                onCaptureModeClick = onCaptureModeClick
            )
        }
    }
}

// enum representing which individual quick setting is currently focused
private enum class FocusedQuickSetting {
    NONE,
    ASPECT_RATIO,
    CAPTURE_MODE
}

// todo: Add UI states for Quick Settings buttons

/**
 * The UI component for quick settings when it is focused.
 */
@Composable
private fun ExpandedQuickSettingsUi(
    modifier: Modifier = Modifier,
    previewUiState: PreviewUiState.Ready,
    currentCameraSettings: CameraAppSettings,
    onLensFaceClick: (newLensFace: LensFacing) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit,
    onStreamConfigClick: (streamConfig: StreamConfig) -> Unit,
    focusedQuickSetting: FocusedQuickSetting,
    setFocusedQuickSetting: (FocusedQuickSetting) -> Unit,
    onDynamicRangeClick: (dynamicRange: DynamicRange) -> Unit,
    onImageOutputFormatClick: (imageOutputFormat: ImageOutputFormat) -> Unit,
    onConcurrentCameraModeClick: (concurrentCameraMode: ConcurrentCameraMode) -> Unit,
    onCaptureModeClick: (CaptureMode) -> Unit
) {
    Column(
        modifier =
        modifier
            .padding(
                horizontal = dimensionResource(
                    id = R.dimen.quick_settings_ui_horizontal_padding
                )
            )
    ) {
        // if no setting is chosen, display the grid of settings
        // to change the order of display just move these lines of code above or below each other
        AnimatedVisibility(visible = focusedQuickSetting == FocusedQuickSetting.NONE) {
            val displayedQuickSettings: List<@Composable () -> Unit> =
                buildList {
                    add {
                        QuickSetFlash(
                            modifier = Modifier.testTag(QUICK_SETTINGS_FLASH_BUTTON),
                            onClick = { f: FlashMode -> onFlashModeClick(f) },
                            flashModeUiState = previewUiState.flashModeUiState
                        )
                    }

                    add {
                        QuickFlipCamera(
                            modifier = Modifier.testTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON),
                            setLensFacing = { l: LensFacing -> onLensFaceClick(l) },
                            currentLensFacing = currentCameraSettings.cameraLensFacing
                        )
                    }

                    add {
                        QuickSetRatio(
                            modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_BUTTON),
                            onClick = {
                                setFocusedQuickSetting(
                                    FocusedQuickSetting.ASPECT_RATIO
                                )
                            },
                            ratio = currentCameraSettings.aspectRatio,
                            currentRatio = currentCameraSettings.aspectRatio
                        )
                    }

                    add {
                        QuickSetStreamConfig(
                            modifier = Modifier.testTag(
                                QUICK_SETTINGS_STREAM_CONFIG_BUTTON
                            ),
                            setStreamConfig = { c: StreamConfig -> onStreamConfigClick(c) },
                            currentStreamConfig = currentCameraSettings.streamConfig,
                            enabled = !(
                                currentCameraSettings.concurrentCameraMode ==
                                    ConcurrentCameraMode.DUAL ||
                                    currentCameraSettings.imageFormat ==
                                    ImageOutputFormat.JPEG_ULTRA_HDR
                                )
                        )
                    }

                    val cameraConstraints = previewUiState.systemConstraints.forCurrentLens(
                        currentCameraSettings
                    )
                    add {
                        fun CameraConstraints.hdrDynamicRangeSupported(): Boolean =
                            this.supportedDynamicRanges.size > 1

                        fun CameraConstraints.hdrImageFormatSupported(): Boolean =
                            supportedImageFormatsMap[currentCameraSettings.streamConfig]
                                ?.let { it.size > 1 } == true

                        // TODO(tm): Move this to PreviewUiState
                        fun shouldEnable(): Boolean = when {
                            currentCameraSettings.concurrentCameraMode !=
                                ConcurrentCameraMode.OFF -> false

                            else -> (
                                cameraConstraints?.hdrDynamicRangeSupported() == true ||
                                    cameraConstraints?.hdrImageFormatSupported() == true
                                )
                        }

                        QuickSetHdr(
                            modifier = Modifier.testTag(QUICK_SETTINGS_HDR_BUTTON),
                            onClick = { d: DynamicRange, i: ImageOutputFormat ->
                                onDynamicRangeClick(d)
                                onImageOutputFormatClick(i)
                            },
                            hdrUiState = previewUiState.hdrUiState
                        )
                    }

                    add {
                        // todo(): use a UiState for this
                        QuickSetConcurrentCamera(
                            modifier =
                            Modifier.testTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON),
                            setConcurrentCameraMode = { c: ConcurrentCameraMode ->
                                onConcurrentCameraModeClick(c)
                            },
                            currentConcurrentCameraMode =
                            currentCameraSettings.concurrentCameraMode,
                            enabled =
                            previewUiState.systemConstraints.concurrentCamerasSupported &&
                                previewUiState.previewMode
                                    !is PreviewMode.ExternalImageCaptureMode &&
                                (
                                    (
                                        previewUiState.captureModeUiState as?
                                            CaptureModeUiState.Enabled
                                        )
                                        ?.currentSelection !=
                                        CaptureMode.IMAGE_ONLY
                                    ) ==
                                true &&
                                (
                                    currentCameraSettings.dynamicRange !=
                                        DEFAULT_HDR_DYNAMIC_RANGE &&
                                        currentCameraSettings.imageFormat !=
                                        DEFAULT_HDR_IMAGE_OUTPUT
                                    )
                        )
                    }

                    add {
                        val context = LocalContext.current
                        QuickSetCaptureMode(
                            modifier = Modifier
                                .testTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                                .semantics {
                                    previewUiState.captureModeUiState.stateDescription()?.let {
                                        stateDescription = context.getString(it)
                                    }
                                },
                            onClick = {
                                setFocusedQuickSetting(
                                    FocusedQuickSetting.CAPTURE_MODE
                                )
                            },
                            captureModeUiState = previewUiState.captureModeUiState,
                            assignedCaptureMode = null
                        )
                    }
                }
            QuickSettingsGrid(quickSettingsButtons = displayedQuickSettings)
        }
        // if a setting that can be focused is selected, show it
        AnimatedVisibility(visible = focusedQuickSetting == FocusedQuickSetting.ASPECT_RATIO) {
            FocusedQuickSetRatio(
                setRatio = onAspectRatioClick,
                currentRatio = currentCameraSettings.aspectRatio
            )
        }

        AnimatedVisibility(visible = (focusedQuickSetting == FocusedQuickSetting.CAPTURE_MODE)) {
            FocusedQuickSetCaptureMode(
                onSetCaptureMode = onCaptureModeClick,
                captureModeUiState = previewUiState.captureModeUiState
            )
        }
    }
}

private fun CaptureModeUiState.stateDescription() = (this as? CaptureModeUiState.Enabled)?.let {
    when (currentSelection) {
        CaptureMode.STANDARD -> R.string.quick_settings_description_capture_mode_standard
        CaptureMode.VIDEO_ONLY -> R.string.quick_settings_description_capture_mode_video_only
        CaptureMode.IMAGE_ONLY -> R.string.quick_settings_description_capture_mode_image_only
    }
}

@Preview
@Composable
fun ExpandedQuickSettingsUiPreview() {
    MaterialTheme {
        ExpandedQuickSettingsUi(
            previewUiState = PreviewUiState.Ready(
                currentCameraSettings = CameraAppSettings(),
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                videoRecordingState = VideoRecordingState.Inactive(),
                // captureModeToggleUiState = CaptureModeToggleUiState.Invisible,
                flashModeUiState = FlashModeUiState.Available(
                    selectedFlashMode = FlashMode.OFF,
                    availableFlashModes = listOf(FlashMode.OFF, FlashMode.ON),
                    isActive = false
                ),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            currentCameraSettings = CameraAppSettings(),
            onLensFaceClick = { },
            onFlashModeClick = { },
            focusedQuickSetting = FocusedQuickSetting.NONE,
            setFocusedQuickSetting = { },
            onAspectRatioClick = { },
            onStreamConfigClick = { },
            onDynamicRangeClick = { },
            onImageOutputFormatClick = { },
            onConcurrentCameraModeClick = { },
            onCaptureModeClick = { }
        )
    }
}

@Preview
@Composable
fun ExpandedQuickSettingsUiPreview_WithHdr() {
    MaterialTheme {
        ExpandedQuickSettingsUi(
            previewUiState = PreviewUiState.Ready(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeUiState.Unavailable,
                videoRecordingState = VideoRecordingState.Inactive(),
                captureButtonUiState = DEFAULT_CAPTURE_BUTTON_STATE
            ),
            currentCameraSettings = CameraAppSettings(dynamicRange = DynamicRange.HLG10),
            onLensFaceClick = { },
            onFlashModeClick = { },
            focusedQuickSetting = FocusedQuickSetting.NONE,
            setFocusedQuickSetting = { },
            onAspectRatioClick = { },
            onStreamConfigClick = { },
            onDynamicRangeClick = { },
            onImageOutputFormatClick = { },
            onConcurrentCameraModeClick = { },
            onCaptureModeClick = { }
        )
    }
}

private val TYPICAL_SYSTEM_CONSTRAINTS_WITH_HDR =
    TYPICAL_SYSTEM_CONSTRAINTS.copy(
        perLensConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints.entries
            .associate { (lensFacing, constraints) ->
                lensFacing to constraints.copy(
                    supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10)
                )
            }
    )
