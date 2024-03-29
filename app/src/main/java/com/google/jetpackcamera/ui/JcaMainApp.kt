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

package com.google.jetpackcamera.ui

import android.content.res.Configuration.UI_MODE_NIGHT_NO
import android.content.res.Configuration.UI_MODE_NIGHT_YES
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.jetpackcamera.R
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.ui.theme.JetpackCameraTheme

@Composable
fun JcaMainApp(
    onPreviewViewModel: (PreviewViewModel) -> Unit,
    previewMode: PreviewMode,
    modifier: Modifier = Modifier,
    viewModel: JcaMainAppViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    JcaMainApp(uiState, modifier, onPreviewViewModel, previewMode)
}

@Composable
private fun JcaMainApp(
    uiState: JcaMainAppUiState,
    modifier: Modifier = Modifier,
    onPreviewViewModel: (PreviewViewModel) -> Unit = {},
    previewMode: PreviewMode = PreviewMode.StandardMode {}
) {
    when (uiState) {
        JcaMainAppUiState.Loading -> {
            LoadingScreen(modifier)
        }

        is JcaMainAppUiState.Success -> {
            // TODO(kimblebee@): add app setting to enable/disable dynamic color
            JetpackCameraTheme(
                darkTheme = when (uiState.cameraAppSettings.darkMode) {
                    DarkMode.DARK -> true
                    DarkMode.LIGHT -> false
                    DarkMode.SYSTEM -> isSystemInDarkTheme()
                },
                dynamicColor = false
            ) {
                Surface(modifier.fillMaxSize()) {
                    JcaApp(
                        onPreviewViewModel = onPreviewViewModel,
                        previewMode = previewMode
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingScreen(modifier: Modifier = Modifier) {
    JetpackCameraTheme(dynamicColor = false) {
        Surface(modifier.fillMaxSize()) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(modifier = Modifier.size(50.dp))
                Text(
                    text = stringResource(R.string.jca_loading),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Preview(name = "Loading - Dark Mode", showBackground = true, uiMode = UI_MODE_NIGHT_YES)
@Preview(name = "Loading - Light Mode", showBackground = true, uiMode = UI_MODE_NIGHT_NO)
@Composable
private fun LoadingPreview() {
    LoadingScreen()
}