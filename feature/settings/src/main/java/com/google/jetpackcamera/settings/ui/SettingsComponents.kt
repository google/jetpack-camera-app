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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.settings.R
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DarkModeStatus
import com.google.jetpackcamera.settings.model.FlashModeStatus


/**
 * MAJOR SETTING UI COMPONENTS
 * these are ready to be popped into the ui
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageHeader(title: String, navBack: () -> Unit) {
    TopAppBar(
        modifier = Modifier,
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
fun SectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .padding(start = 20.dp, top = 10.dp),
        fontSize = 18.sp
    )
}

@Composable
fun DefaultCameraFacing(cameraAppSettings: CameraAppSettings, onClick: () -> Unit) {
    SwitchSettingUI(
        title = stringResource(id = R.string.default_facing_camera_title),
        description = null,
        leadingIcon = null,
        onClick = { onClick() },
        settingValue = cameraAppSettings.default_front_camera,
        enabled = cameraAppSettings.back_camera_available && cameraAppSettings.front_camera_available
    )
}

@Composable
fun DarkModeSetting(currentDarkModeStatus: DarkModeStatus, setDarkMode: (DarkModeStatus) -> Unit) {
    BasicPopupSetting(
        title = stringResource(id = R.string.dark_mode_title),
        leadingIcon = null,
        description = when (currentDarkModeStatus) {
            DarkModeStatus.SYSTEM -> stringResource(id = R.string.dark_mode_status_system)
            DarkModeStatus.DARK -> stringResource(id = R.string.dark_mode_status_dark)
            DarkModeStatus.LIGHT -> stringResource(id = R.string.dark_mode_status_light)
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(text = stringResource(id = R.string.dark_mode_selector_dark),
                    selected = currentDarkModeStatus == DarkModeStatus.DARK,
                    onClick = { setDarkMode(DarkModeStatus.DARK) }
                )
                SingleChoiceSelector(text = stringResource(id = R.string.dark_mode_selector_light),
                    selected = currentDarkModeStatus == DarkModeStatus.LIGHT,
                    onClick = { setDarkMode(DarkModeStatus.LIGHT) }
                )
                SingleChoiceSelector(text = stringResource(id = R.string.dark_mode_selector_system),
                    selected = currentDarkModeStatus == DarkModeStatus.SYSTEM,
                    onClick = { setDarkMode(DarkModeStatus.SYSTEM) }
                )
            }
        }
    )
}

@Composable
fun FlashModeSetting(currentFlashMode: FlashModeStatus, setFlashMode: (FlashModeStatus) -> Unit) {
    BasicPopupSetting(
        title = stringResource(id = R.string.flash_mode_title),
        leadingIcon = null,
        description = when (currentFlashMode) {
            FlashModeStatus.AUTO -> stringResource(id = R.string.flash_mode_status_auto)
            FlashModeStatus.ON -> stringResource(id = R.string.flash_mode_status_on)
            FlashModeStatus.OFF -> stringResource(id = R.string.flash_mode_status_off)
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(text = stringResource(id = R.string.flash_mode_selector_auto),
                    selected = currentFlashMode == FlashModeStatus.AUTO,
                    onClick = { setFlashMode(FlashModeStatus.AUTO) }
                )
                SingleChoiceSelector(text = stringResource(id = R.string.flash_mode_selector_on),
                    selected = currentFlashMode == FlashModeStatus.ON,
                    onClick = { setFlashMode(FlashModeStatus.ON) }
                )
                SingleChoiceSelector(text = stringResource(id = R.string.flash_mode_selector_off),
                    selected = currentFlashMode == FlashModeStatus.OFF,
                    onClick = { setFlashMode(FlashModeStatus.OFF) }
                )
            }
        }
    )
}

/**
 * Setting UI sub-Components
 * small and whimsical :)
 * don't use these directly, use them to build the ready-to-use setting components
 */

/** a composable for creating a simple popup setting **/

@Composable
fun BasicPopupSetting(
    title: String,
    description: String?,
    leadingIcon: @Composable (() -> Unit)?,
    popupContents: @Composable () -> Unit
) {
    val popupStatus = remember { mutableStateOf(false) }
    SettingUI(
        title = title,
        description = description,
        leadingIcon = leadingIcon,
        onClick = { popupStatus.value = true },
        trailingContent = null
    )
    if (popupStatus.value) {
        AlertDialog(
            onDismissRequest = { popupStatus.value = false },
            confirmButton = {
                Text(
                    text = "Close",
                    modifier = Modifier.clickable { popupStatus.value = false })
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
    title: String,
    description: String?,
    leadingIcon: @Composable (() -> Unit)?,
    onClick: () -> Unit,
    settingValue: Boolean,
    enabled: Boolean
) {
    SettingUI(
        enabled = enabled,
        title = title,
        description = description,
        leadingIcon = leadingIcon,
        onClick = onClick,
        trailingContent = {
            SettingSwitch(settingValue, onClick, enabled)
        }
    )
}

/**
 * A composable used as a template used to construct other settings components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingUI(
    title: String,
    description: String? = null,
    leadingIcon: @Composable (() -> Unit)?,
    trailingContent: @Composable (() -> Unit)?,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Box(modifier = Modifier) {
        ListItem(
            modifier = Modifier.clickable(enabled = enabled) { onClick() },
            headlineText = { Text(title) },
            supportingText = {
                when (description) {
                    null -> {}
                    else -> {
                        Text(description)
                    }
                }
            },
            leadingContent = leadingIcon,
            trailingContent = trailingContent
        )
    }
}

/**
 * A component for a switch
 */
@Composable
fun SettingSwitch(settingValue: Boolean, onClick: () -> Unit, enabled: Boolean = true) {
    Switch(
        modifier = Modifier,
        enabled = enabled,
        checked = settingValue,
        onCheckedChange = {
            onClick()
        }
    )
}

/**
 * A component for a single-choice selector for a multiple choice list
 */
@Composable
fun SingleChoiceSelector(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = null,
            enabled = enabled
        )
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
