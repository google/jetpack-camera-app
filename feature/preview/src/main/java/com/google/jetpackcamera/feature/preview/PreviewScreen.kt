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

package com.google.jetpackcamera.feature.preview

import android.util.Log
import androidx.camera.core.Preview.SurfaceProvider
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.google.jetpackcamera.viewfinder.CameraPreview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.feature.quicksettings.QuickSettingsUiModel
import com.google.jetpackcamera.feature.quicksettings.QuickSettingsUi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation

private const val TAG = "ViewFinder"

/**
 * Screen used for the Preview feature.
 */
@Composable
fun PreviewScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    Log.d(TAG, "ViewFinder")

    val previewUiState: PreviewUiState by viewModel.previewUiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    val deferredSurfaceProvider = CompletableDeferred<SurfaceProvider>()
    val onSurfaceProviderReady: (SurfaceProvider) -> Unit = {
        Log.d(TAG, "onSurfaceProviderReady")
        deferredSurfaceProvider.complete(it)
    }

    LaunchedEffect(previewUiState, lifecycleOwner) {
        val surfaceProvider = deferredSurfaceProvider.await()
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.runCamera(surfaceProvider)
            try {
                awaitCancellation()
            } finally {
                viewModel.stopCamera()
            }
        }
    }

    if (previewUiState.cameraState == CameraState.NOT_READY) {
        Text(text = stringResource(R.string.camera_not_ready))
    } else if (previewUiState.cameraState == CameraState.READY) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            CameraPreview(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = { offset ->
                                Log.d(TAG, "onDoubleTap $offset")
                                viewModel.flipCamera()
                            }
                        )
                    },
                onSurfaceProviderReady = onSurfaceProviderReady,
                onRequestBitmapReady = {
                    val bitmap = it.invoke()
                }
            )

            QuickSettingsUi(
                modifier = Modifier.fillMaxSize(),
                quickSettingsUiModels = listOf(
                    QuickSettingsUiModel(
                        drawableResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_cameraswitch_72,
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_cameraswitch_72
                        ),
                        textResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_front_camera_text,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_back_camera_text
                        ),
                        descriptionResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_front_camera_description,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_back_camera_description
                        ),
                        highlightedIndex = 0 /*TODO*/,
                        onClicks = listOf({}, {}) /*TODO*/
                    ),
                    QuickSettingsUiModel(
                        drawableResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_aspect_ratio_72,
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_aspect_ratio_72,
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_aspect_ratio_72,
                        ),
                        textResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_aspect_ratio_3_4,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_aspect_ratio_16_9,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_aspect_ratio_1_1,
                        ),
                        descriptionResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_aspect_ratio_3_4_description,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_aspect_ratio_16_9_description,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_aspect_ratio_1_1_description,
                        ),
                        highlightedIndex = 0 /*TODO*/,
                        onClicks = listOf({}, {}, {}) /*TODO*/
                    ),
                    QuickSettingsUiModel(
                        drawableResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_flash_off_72,
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_flash_auto_72,
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_flash_on_72,
                        ),
                        textResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_flash_off,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_flash_auto,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_flash_on,
                        ),
                        descriptionResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_flash_off_description,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_flash_auto_description,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_flash_on_description,
                        ),
                        highlightedIndex = 0 /*TODO*/,
                        onClicks = listOf({}, {}, {}) /*TODO*/
                    ),
                    QuickSettingsUiModel(
                        drawableResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_timer_off_72,
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_timer_3_72,
                            com.google.jetpackcamera.quicksettings.R.drawable.baseline_timer_10_72,
                        ),
                        textResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_timer_off,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_timer_3,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_timer_10,
                        ),
                        descriptionResIds = listOf(
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_timer_off_description,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_timer_3_description,
                            com.google.jetpackcamera.quicksettings.R.string.quick_settings_timer_10_description,
                        ),
                        highlightedIndex = 0 /*TODO*/,
                        onClicks = listOf({}, {}, {}) /*TODO*/
                    )
                )
            )

            IconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                onClick = onNavigateToSettings,
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_content_description),
                    modifier = Modifier.size(72.dp)
                )
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                CaptureButton(
                    onClick = { viewModel.captureImage() }
                )
            }
        }
    }
}

@Composable
fun CaptureButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = CircleShape,
        modifier = Modifier
            .size(120.dp)
            .padding(18.dp)
            .border(4.dp, Color.White, CircleShape)
    ) {}
}
