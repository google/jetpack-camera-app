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
import com.google.jetpackcamera.settings.SettingsUiState
import com.google.jetpackcamera.settings.model.DarkModeStatus
import com.google.jetpackcamera.settings.model.Settings


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
fun DefaultCameraFacing(settings: Settings, onClick: () -> Unit) {
    SwitchSettingUI(
        title = stringResource(id = R.string.default_facing_camera_title),
        description = null,
        leadingIcon = null,
        onClick = { onClick() },
        settingValue = settings.default_front_camera
    )
}

@Composable
fun DarkModeSetting(uiState: SettingsUiState, setDarkMode: (DarkModeStatus) -> Unit) {
    BasicPopupSetting(
        title = stringResource(id = R.string.dark_mode_title),
        leadingIcon = null,
        description = when (uiState.settings.dark_mode_status) {
            DarkModeStatus.SYSTEM -> stringResource(id = R.string.dark_mode_status_system)
            DarkModeStatus.DARK -> stringResource(id = R.string.dark_mode_status_dark)
            DarkModeStatus.LIGHT -> stringResource(id = R.string.dark_mode_status_light)
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                ChoiceRow(text = stringResource(id = R.string.dark_mode_selector_dark),
                    selected = uiState.settings.dark_mode_status == DarkModeStatus.DARK,
                    onClick = { setDarkMode(DarkModeStatus.DARK) }
                )
                ChoiceRow(text = stringResource(id = R.string.dark_mode_selector_light),
                    selected = uiState.settings.dark_mode_status == DarkModeStatus.LIGHT,
                    onClick = { setDarkMode(DarkModeStatus.LIGHT) }
                )
                ChoiceRow(text = stringResource(id = R.string.dark_mode_selector_system),
                    selected = uiState.settings.dark_mode_status == DarkModeStatus.SYSTEM,
                    onClick = { setDarkMode(DarkModeStatus.SYSTEM) }
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
    settingValue: Boolean
) {
    SettingUI(
        title = title,
        description = description,
        leadingIcon = leadingIcon,
        onClick = onClick,
        trailingContent = {
            SettingSwitch(settingValue, onClick)
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
    onClick: () -> Unit
) {
    Box(modifier = Modifier) {
        ListItem(
            modifier = Modifier.clickable { onClick() },
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
fun SettingSwitch(settingValue: Boolean, onClick: () -> Unit) {
    Switch(
        modifier = Modifier,
        enabled = true,
        checked = settingValue,
        onCheckedChange = {
            onClick()
        }
    )
}

/**
 * A component for a single-choice selector
 */
@Composable
fun ChoiceRow(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
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
        )
        Spacer(Modifier.width(8.dp))
        Text(text)
    }
}
