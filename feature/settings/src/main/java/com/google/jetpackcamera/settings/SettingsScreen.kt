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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Screen used for the Settings feature.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val settingsUiState by viewModel.settingsUiState.collectAsState()
    Column {
        pageHeader(settingsUiState.text)
        sectionHeader(title = "general")
        basicSettingUI(title = "Some setting", description = "some cool description")
        basicSettingUI(title = "Some setting2", description = "some cool description")
        basicSettingUI(title = "Some setting3", description = "some cool description")
        basicSettingUI(title = "Some setting4", description = "some cool description")
    }
}

@Composable
fun pageHeader(title:String) {
    Text(
        text = title,
        modifier = Modifier
    )
    Divider()
}

@Composable
fun sectionHeader(title: String) {
    Text(
        text = title,
        modifier = Modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun basicSettingUI(title: String, description:String) {
    Box(modifier = Modifier) {
        Column {
            ListItem(
                headlineText = {Text(title)} ,
                  supportingText = {Text(description)},
                  leadingContent = {
                      Icon(Icons.Filled.Settings, contentDescription = "something" )
                  },
                trailingContent = {
                    Icon(
                        Icons.Filled.KeyboardArrowRight,
                        contentDescription = "something"
                    )
                }
        )
        }
        Divider()
    }
}