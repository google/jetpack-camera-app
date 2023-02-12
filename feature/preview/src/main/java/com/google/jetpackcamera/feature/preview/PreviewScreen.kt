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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.jetpackcamera.feature.preview.camera.CameraPreview

private const val TAG = "ViewFinder"

/**
 * Screen used for the Preview feature.
 */
@Composable
fun PreviewScreen(
    viewModel: PreviewViewModel = viewModel()
) {
    Log.d(TAG, "ViewFinder")

    val previewUiState: PreviewUiState by viewModel.previewUiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    val onSurfaceProviderReady: (SurfaceProvider) -> Unit = {
        Log.d(TAG, "onSurfaceProviderReady")
        viewModel.startPreview(lifecycleOwner, it)
    }

    if (previewUiState.cameraState == CameraState.NOT_READY) {
        Text(text = "Camera Not Ready")
    } else if (previewUiState.cameraState == CameraState.READY) {
        Box() {
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
        }
    }
}