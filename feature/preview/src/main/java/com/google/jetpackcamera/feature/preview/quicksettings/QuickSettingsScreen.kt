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
package com.google.jetpackcamera.feature.preview.quicksettings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.tooling.preview.Preview
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.ui.ExpandedQuickSetRatio
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_CAPTURE_MODE_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLASH_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_HDR_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_LOW_LIGHT_BOOST_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickFlipCamera
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetCaptureMode
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetFlash
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetHdr
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetLowLightBoost
import com.google.jetpackcamera.feature.preview.quicksettings.ui.QuickSetRatio
import com.google.jetpackcamera.feature.preview.ui.rotatedLayout
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LowLightBoost
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.model.forCurrentLens

/**
 * The UI component for quick settings.
 */
@Composable
fun QuickSettingsScreenOverlay(
    previewUiState: PreviewUiState.Ready,
    currentCameraSettings: CameraAppSettings,
    systemConstraints: SystemConstraints,
    toggleIsOpen: () -> Unit,
    onLensFaceClick: (lensFace: LensFacing) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit,
    onCaptureModeClick: (captureMode: CaptureMode) -> Unit,
    onDynamicRangeClick: (dynamicRange: DynamicRange) -> Unit,
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

        // Only needed to initialize
        val currentDisplayRotation = LocalView.current.display.rotation
        var newSurfaceRotation by remember { mutableIntStateOf(currentDisplayRotation) }
        var prevSurfaceRotation by remember { mutableIntStateOf(currentDisplayRotation) }
        key(LocalConfiguration.current) {
            prevSurfaceRotation = newSurfaceRotation
            newSurfaceRotation = LocalView.current.display.rotation
        }

        AnimatedContent(
            targetState = prevSurfaceRotation,
            label = "QuickSettingOrientation",
            modifier = Modifier.background(color = backgroundColor.value)
        ) { prevRotation ->
            Column(
                modifier =
                modifier
                    .fillMaxSize()
                    .alpha(alpha = contentAlpha.value)
                    .clickable(onClick = onBack)
                    .rotatedLayout(baseRotation = prevRotation),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ExpandedQuickSettingsUi(
                    previewUiState = previewUiState,
                    currentCameraSettings = currentCameraSettings,
                    systemConstraints = systemConstraints,
                    shouldShowQuickSetting = shouldShowQuickSetting,
                    setVisibleQuickSetting = { enum: IsExpandedQuickSetting ->
                        shouldShowQuickSetting = enum
                    },
                    onLensFaceClick = onLensFaceClick,
                    onFlashModeClick = onFlashModeClick,
                    onAspectRatioClick = onAspectRatioClick,
                    onCaptureModeClick = onCaptureModeClick,
                    onDynamicRangeClick = onDynamicRangeClick,
                    onLowLightBoostClick = onLowLightBoostClick
                )
            }
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
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandedQuickSettingsUi(
    previewUiState: PreviewUiState.Ready,
    currentCameraSettings: CameraAppSettings,
    systemConstraints: SystemConstraints,
    onLensFaceClick: (newLensFace: LensFacing) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit,
    onCaptureModeClick: (captureMode: CaptureMode) -> Unit,
    shouldShowQuickSetting: IsExpandedQuickSetting,
    setVisibleQuickSetting: (IsExpandedQuickSetting) -> Unit,
    onDynamicRangeClick: (dynamicRange: DynamicRange) -> Unit,
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
                FlowRow(
                    maxItemsInEachRow = 3,
                    horizontalArrangement = Arrangement.Center
                ) {
                    val itemModifier = Modifier.width(
                        dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size) +
                            dimensionResource(id = R.dimen.quick_settings_ui_item_padding) * 2
                    )
                    QuickSetFlash(
                        modifier = itemModifier.testTag(QUICK_SETTINGS_FLASH_BUTTON),
                        onClick = { f: FlashMode -> onFlashModeClick(f) },
                        currentFlashMode = currentCameraSettings.flashMode
                    )
                    QuickFlipCamera(
                        modifier = itemModifier.testTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON),
                        setLensFacing = { l: LensFacing -> onLensFaceClick(l) },
                        currentLensFacing = currentCameraSettings.cameraLensFacing
                    )
                    QuickSetRatio(
                        modifier = itemModifier.testTag(QUICK_SETTINGS_RATIO_BUTTON),
                        onClick = {
                            setVisibleQuickSetting(
                                IsExpandedQuickSetting.ASPECT_RATIO
                            )
                        },
                        ratio = currentCameraSettings.aspectRatio,
                        currentRatio = currentCameraSettings.aspectRatio
                    )
                    QuickSetCaptureMode(
                        modifier = itemModifier.testTag(QUICK_SETTINGS_CAPTURE_MODE_BUTTON),
                        setCaptureMode = { c: CaptureMode -> onCaptureModeClick(c) },
                        currentCaptureMode = currentCameraSettings.captureMode
                    )
                    QuickSetHdr(
                        modifier = itemModifier.testTag(QUICK_SETTINGS_HDR_BUTTON),
                        onClick = { d: DynamicRange -> onDynamicRangeClick(d) },
                        selectedDynamicRange = currentCameraSettings.dynamicRange,
                        hdrDynamicRange = currentCameraSettings.defaultHdrDynamicRange,
                        enabled = previewUiState.previewMode !is
                            PreviewMode.ExternalImageCaptureMode &&
                            systemConstraints.forCurrentLens(currentCameraSettings)?.let {
                                it.supportedDynamicRanges.size > 1
                            } ?: false
                    )
                    QuickSetLowLightBoost(
                        modifier = itemModifier.testTag(QUICK_SETTINGS_LOW_LIGHT_BOOST_BUTTON),
                        onClick = {
                                l: LowLightBoost ->
                            onLowLightBoostClick(l)
                        },
                        selectedLowLightBoost = currentCameraSettings.lowLightBoost
                    )
                }
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
                previewMode = PreviewMode.StandardMode {}
            ),
            currentCameraSettings = CameraAppSettings(),
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            onLensFaceClick = { },
            onFlashModeClick = { },
            shouldShowQuickSetting = IsExpandedQuickSetting.NONE,
            setVisibleQuickSetting = { },
            onAspectRatioClick = { },
            onCaptureModeClick = { },
            onDynamicRangeClick = { },
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
                previewMode = PreviewMode.StandardMode {}
            ),
            currentCameraSettings = CameraAppSettings(dynamicRange = DynamicRange.HLG10),
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS_WITH_HDR,
            onLensFaceClick = { },
            onFlashModeClick = { },
            shouldShowQuickSetting = IsExpandedQuickSetting.NONE,
            setVisibleQuickSetting = { },
            onAspectRatioClick = { },
            onCaptureModeClick = { },
            onDynamicRangeClick = { },
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
