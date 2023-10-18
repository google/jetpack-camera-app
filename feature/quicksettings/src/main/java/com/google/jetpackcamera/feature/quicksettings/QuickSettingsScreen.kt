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
package com.google.jetpackcamera.feature.quicksettings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.google.jetpackcamera.feature.quicksettings.ui.DropDownIcon
import com.google.jetpackcamera.feature.quicksettings.ui.ExpandedQuickSetRatio
import com.google.jetpackcamera.feature.quicksettings.ui.QuickFlipCamera
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSetFlash
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSetRatio
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSettingsGrid
import com.google.jetpackcamera.quicksettings.R
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashMode

/**
 * The UI component for quick settings.
 */
@Composable
fun QuickSettingsScreen(
    modifier: Modifier = Modifier,
    currentCameraSettings: CameraAppSettings,
    isOpen: Boolean = false,
    toggleIsOpen: () -> Unit,
    onLensFaceClick: (lensFace: Boolean) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit
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
        Column(
            modifier =
            Modifier
                .fillMaxSize()
                .background(color = backgroundColor.value)
                .alpha(alpha = contentAlpha.value)
                .clickable {
                    // if a setting is expanded, click on the background to close it.
                    // if no other settings are expanded, then close the popup
                    when (shouldShowQuickSetting) {
                        IsExpandedQuickSetting.NONE -> toggleIsOpen()
                        else -> shouldShowQuickSetting = IsExpandedQuickSetting.NONE
                    }
                },
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ExpandedQuickSettingsUi(
                currentCameraSettings = currentCameraSettings,
                shouldShowQuickSetting = shouldShowQuickSetting,
                setVisibleQuickSetting = { enum: IsExpandedQuickSetting ->
                    shouldShowQuickSetting = enum
                },
                onLensFaceClick = onLensFaceClick,
                onFlashModeClick = onFlashModeClick,
                onAspectRatioClick = onAspectRatioClick
            )
        }
    } else {
        shouldShowQuickSetting = IsExpandedQuickSetting.NONE
    }
    DropDownIcon(
        modifier = modifier,
        toggleDropDown = toggleIsOpen,
        isOpen = isOpen
    )
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
    currentCameraSettings: CameraAppSettings,
    onLensFaceClick: (lensFacingFront: Boolean) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    shouldShowQuickSetting: IsExpandedQuickSetting,
    setVisibleQuickSetting: (IsExpandedQuickSetting) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit
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
                val displayedQuickSettings: Array<@Composable () -> Unit> =
                    arrayOf(
                        {
                            QuickSetFlash(
                                modifier = Modifier.testTag("QuickSetFlash"),
                                onClick = { f: FlashMode -> onFlashModeClick(f) },
                                currentFlashMode = currentCameraSettings.flashMode
                            )
                        },
                        {
                            QuickFlipCamera(
                                flipCamera = { b: Boolean -> onLensFaceClick(b) },
                                currentFacingFront = currentCameraSettings.isFrontCameraFacing
                            )
                        },
                        {
                            QuickSetRatio(
                                onClick = {
                                    setVisibleQuickSetting(
                                        IsExpandedQuickSetting.ASPECT_RATIO
                                    )
                                },
                                ratio = currentCameraSettings.aspectRatio,
                                currentRatio = currentCameraSettings.aspectRatio
                            )
                        }
                    )
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
