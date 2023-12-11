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

import android.content.ContentValues
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.MainActivityUiState.Loading
import com.google.jetpackcamera.MainActivityUiState.Success
import com.google.jetpackcamera.domain.camera.TakePictureCallback
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.receiver.ImageCaptureReceiver
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val externalContentValues: ContentValues? =
            if (intent.extras == null) {
                null
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.extras!!.getParcelable(
                    MediaStore.EXTRA_OUTPUT,
                    ContentValues::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                intent.extras!!.getParcelable(MediaStore.EXTRA_OUTPUT)
            }
        val shouldFinishAfterCapture =
            if (intent.extras == null) {
                false
            } else {
                intent.extras!!.getBoolean(ImageCaptureReceiver.EXTRA_SHOULD_FINISH_AFTER_CAPTURE)
            }
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
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            JcaApp(
                                onPreviewViewModel = { previewViewModel = it },
                                contentResolver = contentResolver,
                                contentValues = externalContentValues,
                                takePictureCallback = object : TakePictureCallback {
                                    override fun onPictureTaken(savedUri: Uri?) {
                                        if (shouldFinishAfterCapture) {
                                            setResult(RESULT_OK)
                                            finish()
                                        }
                                    }

                                    override fun onError() {
                                        if (shouldFinishAfterCapture) {
                                            setResult(RESULT_CANCELED)
                                            finish()
                                        }
                                    }
                                }
                            )
                        }
                    }
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
