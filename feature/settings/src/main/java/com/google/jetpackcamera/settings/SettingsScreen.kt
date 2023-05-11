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

package com.google.jetpackcamera.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.jetpackcamera.settings.model.DarkModeStatus
import com.google.jetpackcamera.settings.model.Settings


private const val TAG = "SettingsScreen"

/**
 * Screen used for the Settings feature.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    navController: NavController
) {
    val settingsUiState by viewModel.settingsUiState.collectAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        //todo string resource
        settingsPageHeader("Settings", navBack = { navController.popBackStack() })


        when (settingsUiState.repositoryStatus) {
            false -> Text(text = "loading...") //todo loading
            true -> SettingsList(uiState = settingsUiState, viewModel = viewModel)
        }
    }
}

@Composable

fun SettingsList(uiState: SettingsUiState, viewModel: SettingsViewModel) {
    sectionHeader(title = "General")
    defaultCameraFacing(
        settings = uiState.settings,
        onClick = viewModel::setDefaultFrontCamera
    )

    DarkModeSetting(uiState = uiState, setDarkMode = viewModel::setDarkMode)

    sampleSettings()


}

/**
 * MAJOR SETTING UI COMPONENTS
 * these are ready to be popped into the ui
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingsPageHeader(title: String, navBack: () -> Unit) {
    //todo navigation
    TopAppBar(
        modifier = Modifier,
        title = {
            Text(title)
        },
        navigationIcon = {
            //todo navigate back
            IconButton(onClick = { navBack() }) {
                Icon(Icons.Filled.ArrowBack, "Accessibility text")
            }
        }
    )
}

@Composable
fun sectionHeader(title: String) {
    //todo styling
    Text(
        text = title,
        modifier = Modifier
            .padding(start = 20.dp, top = 10.dp),
        fontSize = 18.sp
    )
}

@Composable
fun defaultCameraFacing(settings: Settings, onClick: () -> Unit) {
    //todo set strign resources
    switchSettingUI(
        title = "Set Default Front Camera",
        description = null,
        leadingIcon = null,
        onClick = { onClick() },
        settingValue = settings.default_front_camera
    )
}

@Composable

fun DarkModeSetting(uiState: SettingsUiState, setDarkMode: (DarkModeStatus) -> Unit) {
    //todo set string resources
    basicPopupSetting(
        title = stringResource(id = R.string.dark_mode_title),
        leadingIcon = null,
        description = when (uiState.settings.dark_mode_status) {
            DarkModeStatus.SYSTEM -> stringResource(id = R.string.dark_mode_status_system)
            DarkModeStatus.DARK -> stringResource(id = R.string.dark_mode_status_dark)
            DarkModeStatus.LIGHT -> stringResource(id = R.string.dark_mode_status_light)
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                choiceRow(text = "dark",
                    selected = uiState.settings.dark_mode_status == DarkModeStatus.DARK,
                    onClick = { setDarkMode(DarkModeStatus.DARK) }
                )
                choiceRow(text = "light",
                    selected = uiState.settings.dark_mode_status == DarkModeStatus.LIGHT,
                    onClick = { setDarkMode(DarkModeStatus.LIGHT) }
                )
                choiceRow(text = "system",
                    selected = uiState.settings.dark_mode_status == DarkModeStatus.SYSTEM,
                    onClick = { setDarkMode(DarkModeStatus.SYSTEM) }
                )
            }
        }
    )
}

@Composable
fun sampleSettings() {
    // the settings below are just to help visualize the different setting formats.
    // switches are non functional bc they arent linked to a ui state value
    sectionHeader(title = "Another Section")

    basicPopupSetting(title = "Foo", description = "fighters that fight foo",
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Face,
                contentDescription = null
            )
        },
        popupContents = {}
    )

    fullPopupSetting(title = "Boo", description = null,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Favorite,
                contentDescription = null
            )
        }, onClick = {/* todo*/ })

    switchSettingUI(
        title = "Goo",
        description = null,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Place,
                contentDescription = null
            )
        },
        onClick = { /*TODO*/ },
        settingValue = false
    )
    switchSettingUI(
        title = "Too",
        description = "or did you mean two",
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Create,
                contentDescription = null
            )
        },
        onClick = { /*TODO*/ },
        settingValue = true
    )
}


/*
 * Setting UI sub-Components
 * small and whimsical :)
 */

/** a composable for creating a simple popup setting **/

@Composable
fun basicPopupSetting(
    title: String,
    description: String?,
    leadingIcon: @Composable (() -> Unit)?,
    popupContents: @Composable () -> Unit
) {
    val popupStatus = remember { mutableStateOf(false) }
    settingUI(
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


//todo
// a setting with an arrow trailing icon (for full page popups?)
@Composable
fun fullPopupSetting(
    title: String,
    description: String?,
    leadingIcon: @Composable() () -> Unit,
    onClick: () -> Unit
) {
    settingUI(title = title,
        description = description,
        leadingIcon = leadingIcon,
        onClick = onClick,
        trailingContent = {
            Icon(
                Icons.Filled.KeyboardArrowRight,
                //todo
                contentDescription = "accessibilityText"
            )
        }
    )
}

/** A composable for creating a setting with a Switch.
 *
 * <p> the value should correspond to the setting's UI state value. the switch will only change
 * appearance if the UI state has been successfully updated
 */
@Composable
fun switchSettingUI(
    title: String,
    description: String?,
    leadingIcon: @Composable (() -> Unit)?,
    onClick: () -> Unit,
    settingValue: Boolean
) {
    settingUI(
        title = title,
        description = description,
        leadingIcon = leadingIcon,
        onClick = onClick,
        trailingContent = {
            settingSwitch(settingValue, onClick)
        }
    )
}

/** A composable used as a template used to construct the other settings */

// style this one to style every setting
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun settingUI(
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
                    null -> null
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
// todo full screen popup frame


@Composable
fun settingSwitch(settingValue: Boolean, onClick: () -> Unit) {
    Switch(
        modifier = Modifier,
        enabled = true,
        checked = settingValue,
        onCheckedChange = {
            onClick()
        }
    )
}

@Composable
fun choiceRow(
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
