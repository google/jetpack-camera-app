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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.google.jetpackcamera.settings.SettingsUiState.Loading
import com.google.jetpackcamera.settings.SettingsUiState.Success


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

        when (settingsUiState) {
            Loading -> Text(text = "loading...")
            is Success -> BigSettings(uiState = settingsUiState as Success, viewModel = viewModel )
        }
    }
}

@Composable
fun BigSettings(uiState: SettingsUiState.Success, viewModel: SettingsViewModel){
    sectionHeader(title = "General")
    defaultCameraFacing(settingsUiState = uiState,
        onClick = viewModel::setDefaultFrontCamera
    )
    defaultCameraFacing(settingsUiState = uiState,
        onClick = viewModel::setDefaultFrontCamera
    )

    // the settings below are just to help visualize the different setting formats.
    // switches are non functional bc they arent linked to a ui state value
    sectionHeader(title = "Another Section")

    basicSettingUI(title = "Foo", description = "fighters that fight foo",
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Face,
                contentDescription = null
            )
        }, onClick = {/* todo*/ })

    popupSettingUI(title = "Boo", description = null,
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
            IconButton(onClick = {navBack()}) {
                Icon(Icons.Filled.ArrowBack, "Accessibility text")
            }
        }
    )
    //Divider()
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
fun defaultCameraFacing(settingsUiState: Success, onClick: () -> Unit) {
    //todo set strign resources
    switchSettingUI(
        title = "Set Default Front Camera",
        description = null,
        leadingIcon = null,
        onClick = { onClick() },
        settingValue = settingsUiState.settings.defaultFrontCamera
    )
}

/*
 * Setting UI sub-Components
 * small and whimsical :)
 */

@Composable
// a setting with no trailing icon (for simple popups)
fun basicSettingUI(
    title: String,
    description: String?,
    leadingIcon: @Composable () -> Unit,
    onClick: () -> Unit
) {
    settingUI(
        title = title, description = description, leadingIcon = leadingIcon, onClick = onClick,
        trailingContent = null
    )
}


// a setting with an arrow trailing icon (for full page popups?)
@Composable
fun popupSettingUI(
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

// a setting with a switch
// the value should correspond to the setting's UI state value. the switch will only change appearance if the UI state has been successfully updated
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

// template used to construct the other settings
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

//todo popup box frame
// todo full screen popup frame

// settingValue is a
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