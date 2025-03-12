/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.feature.postcapture

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.jetpackcamera.data.media.Media
import android.util.Log
import com.google.jetpackcamera.data.media.MediaDescriptor

private const val TAG = "PostCaptureScreen"

@Composable
fun PostCaptureScreen(
    viewModel: PostCaptureViewModel = hiltViewModel()
) {
    Log.d(TAG, "PostCaptureScreen")

    val uiState: PostCaptureUiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.getLastCapture()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when(val media = uiState.media) {
            is Media.Image -> {
                val bitmap = media.bitmap
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawIntoCanvas { canvas ->
                        val scale = maxOf(
                            size.width / bitmap.width,
                            size.height / bitmap.height
                        )
                        val imageSize = Size(bitmap.width * scale, bitmap.height * scale)
                        canvas.nativeCanvas.drawBitmap(
                            bitmap,
                            null,
                            android.graphics.RectF(
                                0f,
                                0f,
                                imageSize.width,
                                imageSize.height
                            ),
                            null
                        )
                    }
                }
            }
            is Media.Video -> {
                Text(text = "Video support pending",
                        modifier = Modifier.align(Alignment.Center))
            }
            Media.None -> {
                Text(text = "No Media Captured", modifier = Modifier.align(Alignment.Center))
            }
            Media.Error -> {
                Text(text = "Error loading media", modifier = Modifier.align(Alignment.Center))
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Delete Image Button
            IconButton(
                onClick = { viewModel.deleteMedia(context.contentResolver) },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, CircleShape),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // Share Image Button
            IconButton(
                onClick = {
                    val mediaDescriptor = uiState.mediaDescriptor

                    if (mediaDescriptor is MediaDescriptor.Image) {
                        shareImage(context, mediaDescriptor.uri)
                    }

                    if(mediaDescriptor is MediaDescriptor.Video) {
                        shareImage(context, mediaDescriptor.uri)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, CircleShape),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = "Share",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Starts an intent to share an image
 *
 * @param context The application context
 * @param imagePath The path to the image to share
 */
private fun shareImage(context: Context, uri: Uri) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/jpeg"
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share Image"))
}
