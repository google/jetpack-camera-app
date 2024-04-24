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
package com.google.jetpackcamera

import android.app.Activity
import android.content.Intent
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.background
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.MainActivityUiState.Loading
import com.google.jetpackcamera.MainActivityUiState.Success
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.ui.JcaApp
import com.google.jetpackcamera.ui.theme.JetpackCameraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Activity for the JetpackCameraApp.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    @VisibleForTesting
    var previewViewModel: PreviewViewModel? = null

    @RequiresApi(Build.VERSION_CODES.M)
    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var uiState: MainActivityUiState by mutableStateOf(Loading)

        lifecycleScope.launch {
            lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState
                    .onEach {
                        uiState = it
                    }
                    .collect()
            }
        }
        setContent {
            when (uiState) {
                Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(50.dp))
                        Text(text = stringResource(R.string.jca_loading), color = Color.White)
                    }
                }

                is Success -> {
                    // TODO(kimblebee@): add app setting to enable/disable dynamic color
                    JetpackCameraTheme(
                        darkTheme = isInDarkMode(uiState = uiState),
                        dynamicColor = false
                    ) {
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                                .semantics {
                                    testTagsAsResourceId = true
                                },
                            color = MaterialTheme.colorScheme.background
                        ) {
                            JcaApp(
                                onPreviewViewModel = { previewViewModel = it },
                                previewMode = getPreviewMode(),
                                openAppSettings = ::openAppSettings
                            )
                        }
                    }
                }
            }
        }
    }

    private fun getPreviewMode(): PreviewMode {
        if (intent == null || MediaStore.ACTION_IMAGE_CAPTURE != intent.action) {
            return PreviewMode.StandardMode { event ->
                if (event is PreviewViewModel.ImageCaptureEvent.ImageSaved) {
                    val intent = Intent(Camera.ACTION_NEW_PICTURE)
                    intent.setData(event.savedUri)
                    sendBroadcast(intent)
                }
            }
        } else {
            var uri = if (intent.extras == null ||
                !intent.extras!!.containsKey(MediaStore.EXTRA_OUTPUT)
            ) {
                null
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.extras!!.getParcelable(
                    MediaStore.EXTRA_OUTPUT,
                    Uri::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.extras!!.getParcelable(MediaStore.EXTRA_OUTPUT)
            }
            if (uri == null && intent.clipData != null && intent.clipData!!.itemCount != 0) {
                uri = intent.clipData!!.getItemAt(0).uri
            }
            return PreviewMode.ExternalImageCaptureMode(uri) { event ->
                if (event is PreviewViewModel.ImageCaptureEvent.ImageSaved) {
                    setResult(RESULT_OK)
                    finish()
                }
            }
        }
    }
}

/**
 * Determines whether the Theme should be in dark, light, or follow system theme
 */
@Composable
private fun isInDarkMode(uiState: MainActivityUiState): Boolean = when (uiState) {
    Loading -> isSystemInDarkTheme()
    is Success -> when (uiState.cameraAppSettings.darkMode) {
        DarkMode.DARK -> true
        DarkMode.LIGHT -> false
        DarkMode.SYSTEM -> isSystemInDarkTheme()
    }
}

/**
 * Open the app settings when necessary to enable permissions
 */
fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
