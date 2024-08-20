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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.jetpackcamera.feature.preview.CaptureModeToggleUiState
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.ExpandedQuickSetRatio
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_CAPTURE_MODE_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLASH_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_HDR_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickFlipCamera
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetCaptureMode
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetConcurrentCamera
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetFlash
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetHdr
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetRatio
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSettingsGrid
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LowLightBoost
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.model.forCurrentLens

/**
 * The UI component for quick settings.
 */
@Composable
fun QuickSettingsScreenOverlay(
    previewUiState: PreviewUiState.Ready,
    currentCameraSettings: CameraAppSettings,
    toggleIsOpen: () -> Unit,
    onLensFaceClick: (lensFace: LensFacing) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit,
    onCaptureModeClick: (captureMode: CaptureMode) -> Unit,
    onDynamicRangeClick: (dynamicRange: DynamicRange) -> Unit,
    onImageOutputFormatClick: (imageOutputFormat: ImageOutputFormat) -> Unit,
    onConcurrentCameraModeClick: (concurrentCameraMode: ConcurrentCameraMode) -> Unit,
    onLowLightBoostClick: (lowLightBoost: LowLightBoost) -> Unit,
    modifier: Modifier = Modifier,
    isOpen: Boolean = false
) {
    var shouldShowQuickSetting by remember {
        mutableStateOf(IsExpandedQuickSetting.NONE)
    }

    val backgroundColor =
        animateColorAsState(
            targetValue = Color.Black.copy(alpha = if (isOpen) 0.7f else 0f),
            label = "backgroundColorAnimation"
        )

    val contentAlpha =
        animateFloatAsState(
            targetValue = if (isOpen) 1f else 0f,
            label = "contentAlphaAnimation",
            animationSpec = tween()
        )

    if (isOpen) {
        val onBack = {
            when (shouldShowQuickSetting) {
                IsExpandedQuickSetting.NONE -> toggleIsOpen()
                else -> shouldShowQuickSetting = IsExpandedQuickSetting.NONE
            }
        }
        BackHandler(onBack = onBack)
        Column(
            modifier =
            modifier
                .fillMaxSize()
                .background(color = backgroundColor.value)
                .alpha(alpha = contentAlpha.value)
                .clickable(onClick = onBack),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpandedQuickSettingsUi(
                previewUiState = previewUiState,
                currentCameraSettings = currentCameraSettings,
                shouldShowQuickSetting = shouldShowQuickSetting,
                setVisibleQuickSetting = { enum: IsExpandedQuickSetting ->
                    shouldShowQuickSetting = enum
                },
                onLensFaceClick = onLensFaceClick,
                onFlashModeClick = onFlashModeClick,
                onAspectRatioClick = onAspectRatioClick,
                onCaptureModeClick = onCaptureModeClick,
                onDynamicRangeClick = onDynamicRangeClick,
                onImageOutputFormatClick = onImageOutputFormatClick,
                onConcurrentCameraModeClick = onConcurrentCameraModeClick,
                onLowLightBoostClick = onLowLightBoostClick
            )
        }
    } else {
        shouldShowQuickSetting = IsExpandedQuickSetting.NONE
    }
}

// enum representing which individual quick setting is currently expanded
private enum class IsExpandedQuickSetting {
    NONE,
    ASPECT_RATIO
}

/**
 * The UI component for quick settings when it is expanded.
 */
@Composable
private fun ExpandedQuickSettingsUi(
    previewUiState: PreviewUiState.Ready,
    currentCameraSettings: CameraAppSettings,
    onLensFaceClick: (newLensFace: LensFacing) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit,
    onCaptureModeClick: (captureMode: CaptureMode) -> Unit,
    shouldShowQuickSetting: IsExpandedQuickSetting,
    setVisibleQuickSetting: (IsExpandedQuickSetting) -> Unit,
    onDynamicRangeClick: (dynamicRange: DynamicRange) -> Unit,
    onImageOutputFormatClick: (imageOutputFormat: ImageOutputFormat) -> Unit,
    onConcurrentCameraModeClick: (concurrentCameraMode: ConcurrentCameraMode) -> Unit,
    onLowLightBoostClick: (lowLightBoost: LowLightBoost) -> Unit
) {
    Column(
        modifier =
        Modifier
            .padding(
                horizontal = dimensionResource(
                    id = R.dimen.quick_settings_ui_horizontal_padding
                )
            )
    ) {
        // if no setting is chosen, display the grid of settings
        // to change the order of display just move these lines of code above or below each other
        when (shouldShowQuickSetting) {
            IsExpandedQuickSetting.NONE -> {
                val displayedQuickSettings: List<@Composable () -> Unit> =
                    buildList {
                        add {
                            QuickSetFlash(
                                modifier = Modifier.testTag(QUICK_SETTINGS_FLASH_BUTTON),
                                onClick = { f: FlashMode -> onFlashModeClick(f) },
                                currentFlashMode = currentCameraSettings.flashMode
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
                                    setVisibleQuickSetting(
                                        IsExpandedQuickSetting.ASPECT_RATIO
                                    )
                                },
                                ratio = currentCameraSettings.aspectRatio,
                                currentRatio = currentCameraSettings.aspectRatio
                            )
                        }

                        add {
                            QuickSetCaptureMode(
                                modifier = Modifier.testTag(QUICK_SETTINGS_CAPTURE_MODE_BUTTON),
                                setCaptureMode = { c: CaptureMode -> onCaptureModeClick(c) },
                                currentCaptureMode = currentCameraSettings.captureMode,
                                enabled = currentCameraSettings.concurrentCameraMode ==
                                    ConcurrentCameraMode.OFF
                            )
                        }

                        val cameraConstraints = previewUiState.systemConstraints.forCurrentLens(
                            currentCameraSettings
                        )
                        add {
                            fun CameraConstraints.hdrDynamicRangeSupported(): Boolean =
                                this.supportedDynamicRanges.size > 1

                            fun CameraConstraints.hdrImageFormatSupported(): Boolean =
                                supportedImageFormatsMap[currentCameraSettings.captureMode]
                                    ?.let { it.size > 1 } ?: false

                            // TODO(tm): Move this to PreviewUiState
                            fun shouldEnable(): Boolean = when {
                                currentCameraSettings.concurrentCameraMode !=
                                    ConcurrentCameraMode.OFF -> false
                                else -> (
                                    cameraConstraints?.hdrDynamicRangeSupported() == true &&
                                        previewUiState.previewMode is PreviewMode.StandardMode
                                    ) ||
                                    cameraConstraints?.hdrImageFormatSupported() == true
                            }

                            QuickSetHdr(
                                modifier = Modifier.testTag(QUICK_SETTINGS_HDR_BUTTON),
                                onClick = { d: DynamicRange, i: ImageOutputFormat ->
                                    onDynamicRangeClick(d)
                                    onImageOutputFormatClick(i)
                                },
                                selectedDynamicRange = currentCameraSettings.dynamicRange,
                                selectedImageOutputFormat = currentCameraSettings.imageFormat,
                                hdrDynamicRange = currentCameraSettings.defaultHdrDynamicRange,
                                hdrImageFormat = currentCameraSettings.defaultHdrImageOutputFormat,
                                hdrDynamicRangeSupported =
                                cameraConstraints?.hdrDynamicRangeSupported() ?: false,
                                previewMode = previewUiState.previewMode,
                                enabled = shouldEnable()
                            )
                        }

                        add {
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
                                previewUiState.previewMode !is PreviewMode.ExternalImageCaptureMode
                            )
                        }
                    }
                QuickSettingsGrid(quickSettingsButtons = displayedQuickSettings)
            }
            // if a setting that can be expanded is selected, show it
            IsExpandedQuickSetting.ASPECT_RATIO -> {
                ExpandedQuickSetRatio(
                    setRatio = onAspectRatioClick,
                    currentRatio = currentCameraSettings.aspectRatio
                )
            }
        }
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
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible
            ),
            currentCameraSettings = CameraAppSettings(),
            onLensFaceClick = { },
            onFlashModeClick = { },
            shouldShowQuickSetting = IsExpandedQuickSetting.NONE,
            setVisibleQuickSetting = { },
            onAspectRatioClick = { },
            onCaptureModeClick = { },
            onDynamicRangeClick = { },
            onImageOutputFormatClick = { },
            onConcurrentCameraModeClick = { },
            onLowLightBoostClick = { }
        )
    }
}

@Preview
@Composable
fun ExpandedQuickSettingsUiPreview_WithHdr() {
    MaterialTheme {
        ExpandedQuickSettingsUi(
            previewUiState = PreviewUiState.Ready(
                currentCameraSettings = CameraAppSettings(),
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
                previewMode = PreviewMode.StandardMode {},
                captureModeToggleUiState = CaptureModeToggleUiState.Invisible
            ),
            currentCameraSettings = CameraAppSettings(dynamicRange = DynamicRange.HLG10),
            onLensFaceClick = { },
            onFlashModeClick = { },
            shouldShowQuickSetting = IsExpandedQuickSetting.NONE,
            setVisibleQuickSetting = { },
            onAspectRatioClick = { },
            onCaptureModeClick = { },
            onDynamicRangeClick = { },
            onImageOutputFormatClick = { },
            onConcurrentCameraModeClick = { },
            onLowLightBoostClick = { }
        )
    }
}

private val TYPICAL_SYSTEM_CONSTRAINTS_WITH_HDR =
    TYPICAL_SYSTEM_CONSTRAINTS.copy(
        perLensConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints.entries.associate {
                (lensFacing, constraints) ->
            lensFacing to constraints.copy(
                supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10)
            )
        }
    )
