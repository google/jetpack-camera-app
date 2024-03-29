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

import android.content.Intent
import android.hardware.Camera
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.VisibleForTesting
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.ui.JcaMainApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activity for the JetpackCameraApp.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @VisibleForTesting
    var previewViewModel: PreviewViewModel? = null

    @OptIn(ExperimentalComposeUiApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val previewMode = remember { getPreviewMode() }
            JcaMainApp(
                modifier = Modifier.semantics { testTagsAsResourceId = true },
                onPreviewViewModel = { previewViewModel = it },
                previewMode = previewMode
            )
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


