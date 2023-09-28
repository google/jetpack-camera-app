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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.jetpackcamera.settings.ui.DarkModeSetting
import com.google.jetpackcamera.settings.ui.DefaultCameraFacing
import com.google.jetpackcamera.settings.ui.FlashModeSetting
import com.google.jetpackcamera.settings.ui.SectionHeader
import com.google.jetpackcamera.settings.ui.SettingsPageHeader


private const val TAG = "SettingsScreen"

/**
 * Screen used for the Settings feature.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateToPreview: () -> Unit
) {
    val settingsUiState by viewModel.settingsUiState.collectAsState()

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        SettingsPageHeader(
            title = stringResource(id = R.string.settings_title),
            navBack = onNavigateToPreview
        )
        SettingsList(uiState = settingsUiState, viewModel = viewModel)
    }
}

@Composable
fun SettingsList(uiState: SettingsUiState, viewModel: SettingsViewModel) {
        SectionHeader(title = stringResource(id = R.string.section_title_camera_settings))

        DefaultCameraFacing(
            cameraAppSettings = uiState.cameraAppSettings,
            onClick = viewModel::setDefaultToFrontCamera
        )

        FlashModeSetting(
            currentFlashMode = uiState.cameraAppSettings.flashMode,
            setFlashMode = viewModel::setFlashMode
        )

        SectionHeader(title = stringResource(id = R.string.section_title_app_settings))

        DarkModeSetting(
            currentDarkModeStatus = uiState.cameraAppSettings.darkMode,
            setDarkMode = viewModel::setDarkMode
        )
}