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
import android.content.pm.ActivityInfo
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
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
import androidx.core.content.IntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.tracing.Trace
import com.google.jetpackcamera.MainActivityUiState.Loading
import com.google.jetpackcamera.MainActivityUiState.Success
import com.google.jetpackcamera.core.common.traceFirstFrameMainActivity
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.ui.JcaApp
import com.google.jetpackcamera.ui.theme.JetpackCameraTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

private const val TAG = "MainActivity"
private const val KEY_DEBUG_MODE = "KEY_DEBUG_MODE"

/**
 * Activity for the JetpackCameraApp.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

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

        var firstFrameComplete: CompletableDeferred<Unit>? = null
        if (Trace.isEnabled()) {
            firstFrameComplete = CompletableDeferred()
            // start trace between app starting and the earliest possible completed capture
            lifecycleScope.launch {
                traceFirstFrameMainActivity(cookie = 0) {
                    firstFrameComplete.await()
                }
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
                                previewMode = getPreviewMode(),
                                isDebugMode = isDebugMode(),
                                openAppSettings = ::openAppSettings,
                                onRequestWindowColorMode = { colorMode ->
                                    // Window color mode APIs require API level 26+
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        Log.d(
                                            TAG,
                                            "Setting window color mode to:" +
                                                " ${colorMode.toColorModeString()}"
                                        )
                                        window?.colorMode = colorMode
                                    }
                                },
                                onFirstFrameCaptureCompleted = {
                                    firstFrameComplete?.complete(Unit)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun isDebugMode(): Boolean {
        return intent != null && intent.hasExtra(KEY_DEBUG_MODE) &&
                intent.getBooleanExtra(KEY_DEBUG_MODE, false)
    }

    private fun getStandardMode(): PreviewMode.StandardMode {
        return PreviewMode.StandardMode { event ->
            if (event is PreviewViewModel.ImageCaptureEvent.ImageSaved) {
                val intent = Intent(Camera.ACTION_NEW_PICTURE)
                intent.setData(event.savedUri)
                sendBroadcast(intent)
            }
        }
    }

    private fun getExternalCaptureUri(): Uri? {
        return IntentCompat.getParcelableExtra(
            intent,
            MediaStore.EXTRA_OUTPUT,
            Uri::class.java
        ) ?: intent?.clipData?.getItemAt(0)?.uri
    }

    private fun getPreviewMode(): PreviewMode {
        return intent?.action?.let { action ->
            when (action) {
                MediaStore.ACTION_IMAGE_CAPTURE ->
                    PreviewMode.ExternalImageCaptureMode(getExternalCaptureUri()) { event ->
                        Log.d(TAG, "onImageCapture, event: $event")
                        if (event is PreviewViewModel.ImageCaptureEvent.ImageSaved) {
                            val resultIntent = Intent()
                            resultIntent.putExtra(MediaStore.EXTRA_OUTPUT, event.savedUri)
                            setResult(RESULT_OK, resultIntent)
                            Log.d(TAG, "onImageCapture, finish()")
                            finish()
                        }
                    }

                MediaStore.ACTION_VIDEO_CAPTURE ->
                    PreviewMode.ExternalVideoCaptureMode(getExternalCaptureUri()) { event ->
                        Log.d(TAG, "onVideoCapture, event: $event")
                        if (event is PreviewViewModel.VideoCaptureEvent.VideoSaved) {
                            val resultIntent = Intent()
                            resultIntent.putExtra(MediaStore.EXTRA_OUTPUT, event.savedUri)
                            setResult(RESULT_OK, resultIntent)
                            Log.d(TAG, "onVideoCapture, finish()")
                            finish()
                        }
                    }

                else -> {
                    Log.w(TAG, "Ignoring external intent with unknown action.")
                    getStandardMode()
                }
            }
        } ?: getStandardMode()
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

@RequiresApi(Build.VERSION_CODES.O)
private fun Int.toColorModeString(): String {
    return when (this) {
        ActivityInfo.COLOR_MODE_DEFAULT -> "COLOR_MODE_DEFAULT"
        ActivityInfo.COLOR_MODE_HDR -> "COLOR_MODE_HDR"
        ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT -> "COLOR_MODE_WIDE_COLOR_GAMUT"
        else -> "<Unknown>"
    }
}

/**
 * Open the app settings when necessary. I.e. to enable permissions that have been denied by a user
 */
private fun Activity.openAppSettings() {
    Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null)
    ).also(::startActivity)
}
