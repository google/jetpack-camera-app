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
package com.google.jetpackcamera.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.settings.R
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SupportedStabilizationMode

/**
 * MAJOR SETTING UI COMPONENTS
 * these are ready to be popped into the ui
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageHeader(modifier: Modifier = Modifier, title: String, navBack: () -> Unit) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(title)
        },
        navigationIcon = {
            IconButton(onClick = { navBack() }) {
                Icon(Icons.Filled.ArrowBack, stringResource(id = R.string.nav_back_accessibility))
            }
        }
    )
}

@Composable
fun SectionHeader(modifier: Modifier = Modifier, title: String) {
    Text(
        modifier = modifier
            .padding(start = 20.dp, top = 10.dp),
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 18.sp
    )
}

@Composable
fun DefaultCameraFacing(
    modifier: Modifier = Modifier,
    cameraAppSettings: CameraAppSettings,
    onClick: () -> Unit
) {
    SwitchSettingUI(
        modifier = modifier,
        title = stringResource(id = R.string.default_facing_camera_title),
        description = null,
        leadingIcon = null,
        onClick = { onClick() },
        settingValue = cameraAppSettings.isFrontCameraFacing,
        enabled = cameraAppSettings.isBackCameraAvailable &&
                cameraAppSettings.isFrontCameraAvailable
    )
}

@Composable
fun DarkModeSetting(
    modifier: Modifier = Modifier,
    currentDarkMode: DarkMode,
    setDarkMode: (DarkMode) -> Unit
) {
    BasicPopupSetting(
        modifier = modifier,
        title = stringResource(id = R.string.dark_mode_title),
        leadingIcon = null,
        description = when (currentDarkMode) {
            DarkMode.SYSTEM -> stringResource(id = R.string.dark_mode_description_system)
            DarkMode.DARK -> stringResource(id = R.string.dark_mode_description_dark)
            DarkMode.LIGHT -> stringResource(id = R.string.dark_mode_description_light)
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    text = stringResource(id = R.string.dark_mode_selector_dark),
                    selected = currentDarkMode == DarkMode.DARK,
                    onClick = { setDarkMode(DarkMode.DARK) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.dark_mode_selector_light),
                    selected = currentDarkMode == DarkMode.LIGHT,
                    onClick = { setDarkMode(DarkMode.LIGHT) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.dark_mode_selector_system),
                    selected = currentDarkMode == DarkMode.SYSTEM,
                    onClick = { setDarkMode(DarkMode.SYSTEM) }
                )
            }
        }
    )
}

@Composable
fun FlashModeSetting(
    modifier: Modifier = Modifier,
    currentFlashMode: FlashMode,
    setFlashMode: (FlashMode) -> Unit
) {
    BasicPopupSetting(
        modifier = modifier,
        title = stringResource(id = R.string.flash_mode_title),
        leadingIcon = null,
        description = when (currentFlashMode) {
            FlashMode.AUTO -> stringResource(id = R.string.flash_mode_description_auto)
            FlashMode.ON -> stringResource(id = R.string.flash_mode_description_on)
            FlashMode.OFF -> stringResource(id = R.string.flash_mode_description_off)
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    text = stringResource(id = R.string.flash_mode_selector_auto),
                    selected = currentFlashMode == FlashMode.AUTO,
                    onClick = { setFlashMode(FlashMode.AUTO) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.flash_mode_selector_on),
                    selected = currentFlashMode == FlashMode.ON,
                    onClick = { setFlashMode(FlashMode.ON) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.flash_mode_selector_off),
                    selected = currentFlashMode == FlashMode.OFF,
                    onClick = { setFlashMode(FlashMode.OFF) }
                )
            }
        }
    )
}

@Composable
fun AspectRatioSetting(currentAspectRatio: AspectRatio, setAspectRatio: (AspectRatio) -> Unit) {
    BasicPopupSetting(
        title = stringResource(id = R.string.aspect_ratio_title),
        leadingIcon = null,
        description = when (currentAspectRatio) {
            AspectRatio.NINE_SIXTEEN -> stringResource(id = R.string.aspect_ratio_description_9_16)
            AspectRatio.THREE_FOUR -> stringResource(id = R.string.aspect_ratio_description_3_4)
            AspectRatio.ONE_ONE -> stringResource(id = R.string.aspect_ratio_description_1_1)
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    text = stringResource(id = R.string.aspect_ratio_selector_9_16),
                    selected = currentAspectRatio == AspectRatio.NINE_SIXTEEN,
                    onClick = { setAspectRatio(AspectRatio.NINE_SIXTEEN) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.aspect_ratio_selector_3_4),
                    selected = currentAspectRatio == AspectRatio.THREE_FOUR,
                    onClick = { setAspectRatio(AspectRatio.THREE_FOUR) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.aspect_ratio_selector_1_1),
                    selected = currentAspectRatio == AspectRatio.ONE_ONE,
                    onClick = { setAspectRatio(AspectRatio.ONE_ONE) }
                )
            }
        }
    )
}

@Composable
fun CaptureModeSetting(currentCaptureMode: CaptureMode, setCaptureMode: (CaptureMode) -> Unit) {
    BasicPopupSetting(
        title = stringResource(R.string.capture_mode_title),
        leadingIcon = null,
        description = when (currentCaptureMode) {
            CaptureMode.MULTI_STREAM -> stringResource(
                id = R.string.capture_mode_description_multi_stream
            )

            CaptureMode.SINGLE_STREAM -> stringResource(
                id = R.string.capture_mode_description_single_stream
            )
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    text = stringResource(id = R.string.capture_mode_selector_multi_stream),
                    selected = currentCaptureMode == CaptureMode.MULTI_STREAM,
                    onClick = { setCaptureMode(CaptureMode.MULTI_STREAM) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.capture_mode_description_single_stream),
                    selected = currentCaptureMode == CaptureMode.SINGLE_STREAM,
                    onClick = { setCaptureMode(CaptureMode.SINGLE_STREAM) }
                )
            }
        }
    )
}

/**
 * Returns the description text depending on the preview/video stabilization configuration.
 * On - preview is on and video is NOT off.
 * High Quality - preview is unspecified and video is ON.
 * Off - Every other configuration.
 */
private fun getStabilizationStringRes(
    previewStabilization: Stabilization,
    videoStabilization: Stabilization
): Int {
    return if (previewStabilization == Stabilization.ON &&
        videoStabilization != Stabilization.OFF
    ) {
        R.string.stabilization_description_on
    } else if (previewStabilization == Stabilization.UNDEFINED &&
        videoStabilization == Stabilization.ON
    ) {
        R.string.stabilization_description_high_quality
    } else {
        R.string.stabilization_description_off
    }
}

/**
 * A Setting to set preview and video stabilization.
 *
 * ON - Both preview and video are stabilized.
 * HIGH_QUALITY - Video will be stabilized, preview might be stabilized, depending on the device.
 * OFF - Preview and video stabilization is disabled.
 *
 * @param supportedStabilizationMode the enabled condition for this setting.
 */
@Composable
fun VideoStabilizeSetting(
    currentPreviewStabilization: Stabilization,
    currentVideoStabilization: Stabilization,
    supportedStabilizationMode: SupportedStabilizationMode,
    setVideoStabilization: (Stabilization) -> Unit,
    setPreviewStabilization: (Stabilization) -> Unit
) {
    BasicPopupSetting(
        title = stringResource(R.string.video_stabilization_title),
        leadingIcon = null,
        enabled = supportedStabilizationMode != SupportedStabilizationMode.UNSUPPORTED,
        description = if (supportedStabilizationMode == SupportedStabilizationMode.UNSUPPORTED) {
            stringResource(id = R.string.stabilization_description_unsupported)

        } else {
            stringResource(
                id = getStabilizationStringRes(
                    previewStabilization = currentPreviewStabilization,
                    videoStabilization = currentVideoStabilization
                )
            )
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                Text(
                    text = stringResource(id = R.string.stabilization_popup_disclaimer),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Spacer(modifier = Modifier.height(10.dp))

                // on selector
                SingleChoiceSelector(
                    text = stringResource(id = R.string.stabilization_selector_on),
                    secondaryText = stringResource(id = R.string.stabilization_selector_on_info),
                    enabled = supportedStabilizationMode == SupportedStabilizationMode.FULL,
                    selected = (currentPreviewStabilization == Stabilization.ON) &&
                            (currentVideoStabilization != Stabilization.OFF),
                    onClick = {
                        setVideoStabilization(Stabilization.ON)
                        setPreviewStabilization(Stabilization.ON)
                    }
                )

                // high quality selector
                SingleChoiceSelector(
                    text = stringResource(id = R.string.stabilization_selector_high_quality),
                    secondaryText = stringResource(
                        id = R.string.stabilization_selector_high_quality_info
                    ),

                    selected = (currentPreviewStabilization == Stabilization.UNDEFINED) &&
                            (currentVideoStabilization == Stabilization.ON),
                    onClick = {
                        setVideoStabilization(Stabilization.ON)
                        setPreviewStabilization(Stabilization.UNDEFINED)
                    }
                )

                // off selector
                SingleChoiceSelector(
                    text = stringResource(id = R.string.stabilization_selector_off),
                    selected = (currentPreviewStabilization != Stabilization.ON) &&
                            (currentVideoStabilization != Stabilization.ON),
                    onClick = {
                        setVideoStabilization(Stabilization.OFF)
                        setPreviewStabilization(Stabilization.OFF)
                    }
                )
            }
        }
    )
}

/*
 * Setting UI sub-Components
 * small and whimsical :)
 * don't use these directly, use them to build the ready-to-use setting components
 */

/** a composable for creating a simple popup setting **/

@Composable
fun BasicPopupSetting(
    modifier: Modifier = Modifier,
    title: String,
    description: String?,
    enabled: Boolean = true,
    leadingIcon: @Composable (() -> Unit)?,
    popupContents: @Composable () -> Unit
) {
    val popupStatus = remember { mutableStateOf(false) }
    SettingUI(
        modifier = modifier.clickable(enabled = enabled) { popupStatus.value = true },
        title = title,
        enabled = enabled,
        description = description,
        leadingIcon = leadingIcon,
        trailingContent = null
    )
    if (popupStatus.value) {
        AlertDialog(
            onDismissRequest = { popupStatus.value = false },
            confirmButton = {
                Text(
                    text = "Close",
                    modifier = Modifier.clickable { popupStatus.value = false }
                )
            },
            title = { Text(text = title) },
            text = popupContents
        )
    }
}

/**
 * A composable for creating a setting with a Switch.
 *
 * <p> the value should correspond to the setting's UI state value. the switch will only change
 * appearance if the UI state has been successfully updated
 */
@Composable
fun SwitchSettingUI(
    modifier: Modifier = Modifier,
    title: String,
    description: String?,
    leadingIcon: @Composable (() -> Unit)?,
    onClick: () -> Unit,
    settingValue: Boolean,
    enabled: Boolean
) {
    SettingUI(
        modifier = modifier.toggleable(
            enabled = enabled,
            role = Role.Switch,
            value = settingValue,
            onValueChange = { _ -> onClick() }
        ),
        enabled = enabled,
        title = title,
        description = description,
        leadingIcon = leadingIcon,
        trailingContent = {
            Switch(
                enabled = enabled,
                checked = settingValue,
                onCheckedChange = {
                    onClick()
                }
            )
        }
    )
}

/**
 * A composable used as a template used to construct other settings components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingUI(
    modifier: Modifier = Modifier,
    title: String,
    enabled: Boolean = true,
    description: String? = null,
    leadingIcon: @Composable (() -> Unit)?,
    trailingContent: @Composable (() -> Unit)?
) {
    ListItem(
        modifier = modifier,
        headlineText = {
            when (enabled) {
                true -> Text(title)
                false -> {
                    Text(text = title, color = LocalContentColor.current.copy(alpha = .7f) )
                }
            }
        },
        supportingText = {
            if (description != null) {
                when (enabled) {
                    true -> Text(description)
                    false -> Text(
                        text = description,
                        color = LocalContentColor.current.copy(alpha = .7f)
                    )
                }
            }
        },
        leadingContent = leadingIcon,
        trailingContent = trailingContent
    )
}

/**
 * A component for a single-choice selector for a multiple choice list
 */
@Composable
fun SingleChoiceSelector(
    modifier: Modifier = Modifier,
    text: String,
    secondaryText: String? = null,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
                enabled = enabled
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingUI(
            title = text,
            description = secondaryText,
            enabled = enabled,
            leadingIcon = {
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled
                )
            },
            trailingContent = null
        )
    }
}
