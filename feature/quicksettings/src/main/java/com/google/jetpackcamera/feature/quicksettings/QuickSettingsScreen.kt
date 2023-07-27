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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.res.dimensionResource
import com.google.jetpackcamera.feature.quicksettings.ui.DemoMultiple
import com.google.jetpackcamera.feature.quicksettings.ui.DemoSetSwitch
import com.google.jetpackcamera.feature.quicksettings.ui.DropDownIcon
import com.google.jetpackcamera.feature.quicksettings.ui.ExpandedDemoMultiple
import com.google.jetpackcamera.feature.quicksettings.ui.ExpandedQuickSetRatio
import com.google.jetpackcamera.feature.quicksettings.ui.QuickFlipCamera
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSetFlash
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSetRatio
import com.google.jetpackcamera.feature.quicksettings.ui.QuickSettingsGrid
import com.google.jetpackcamera.quicksettings.R
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DemoMultipleStatus
import com.google.jetpackcamera.settings.model.FlashModeStatus

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
    onFlashModeClick: (flashMode: FlashModeStatus) -> Unit,
    onDemoSwitchClick: (Boolean) -> Unit,
    onDemoMultipleClick: (DemoMultipleStatus) -> Unit
) {
    var shouldShowQuickSetting by remember {
        mutableStateOf(IsExpandedQuickSetting.NONE)
    }

    val backgroundColor = animateColorAsState(
        targetValue = Color.Black.copy(alpha = if (isOpen) 0.7f else 0f),
        label = "backgroundColorAnimation"
    )

    val contentAlpha = animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f, label = "contentAlphaAnimation",
        animationSpec = tween()
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = backgroundColor.value),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        DropDownIcon(toggleIsOpen, isOpen = isOpen)

        if (isOpen) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(alpha = contentAlpha.value)
                    .clickable {
                        // if a setting is expanded, close it. if no other settings are expanded, then close out of the popup
                        if (shouldShowQuickSetting == IsExpandedQuickSetting.NONE) toggleIsOpen() else shouldShowQuickSetting =
                            IsExpandedQuickSetting.NONE
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
                    onDemoSwitchClick = onDemoSwitchClick,
                    onDemoMultipleClick = onDemoMultipleClick,
                    //onAspectRatioClick = onAspectRatioClick,
                    //onTimerClick = onTimerClick,
                )
            }
        }
    }
}

// enum representing which individual quick setting is currently expanded
private enum class IsExpandedQuickSetting {
    NONE,
    ASPECT_RATIO,
    DEMO,
}

/**
 * The UI component for quick settings when it is expanded.
 */
@Composable
private fun ExpandedQuickSettingsUi(
    currentCameraSettings: CameraAppSettings,
    onLensFaceClick: (lensFacingFront: Boolean) -> Unit,
    onFlashModeClick: (flashMode: FlashModeStatus) -> Unit,
    onDemoSwitchClick: (Boolean) -> Unit,
    onDemoMultipleClick: (DemoMultipleStatus) -> Unit,
    shouldShowQuickSetting: IsExpandedQuickSetting,
    setVisibleQuickSetting: (IsExpandedQuickSetting) -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimensionResource(id = R.dimen.quick_settings_ui_horizontal_padding))
    ) {
        // if no setting is chosen, display the grid of settings
        // to change the order of display just move these lines of code above or below each other
        if (shouldShowQuickSetting == IsExpandedQuickSetting.NONE) {
            val displayedQuickSettings: Array<@Composable () -> Unit> = arrayOf(

                {
                    QuickSetFlash(
                        onClick = { f: FlashModeStatus -> onFlashModeClick(f) },
                        currentFlashMode = currentCameraSettings.flash_mode_status
                    )
                },
                {
                    QuickFlipCamera(
                        flipCamera = { b: Boolean -> onLensFaceClick(b) },
                        currentFacingFront = currentCameraSettings.default_front_camera
                    )
                },
                {
                    DemoSetSwitch(onClick = onDemoSwitchClick, currentDemoSwitchValue = currentCameraSettings.demo_switch)
                },
                {
                    DemoMultiple(onClick = { setVisibleQuickSetting(IsExpandedQuickSetting.DEMO) }, assignedValue = currentCameraSettings.demo_multiple,
                        currentValue = currentCameraSettings.demo_multiple,
                    )
                }
                //TODO: Implement Set Ratio
                /*
                {
                    QuickSetRatio(
                        onClick = { setVisibleQuickSetting(IsExpandedQuickSetting.ASPECT_RATIO) },
                        ratio = 1,
                        currentRatio = 1
                    )
                },
                 */
            )
            QuickSettingsGrid(quickSettingsButtons = displayedQuickSettings)

        }
        // if a setting that can be expanded is selected, show it
        else {
            if (shouldShowQuickSetting == IsExpandedQuickSetting.ASPECT_RATIO) {
                ExpandedQuickSetRatio(setRatio = {}, currentRatio = 1)
            }
            else if (shouldShowQuickSetting == IsExpandedQuickSetting.DEMO) {
                ExpandedDemoMultiple(onClick = onDemoMultipleClick, currentDemoMultipleStatus = currentCameraSettings.demo_multiple)
            }
        }
        Spacer(modifier = Modifier.height(dimensionResource(id = R.dimen.quick_settings_spacer_height)))
    }
}
