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
import android.util.Log
import androidx.annotation.OptIn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor

private const val TAG = "PostCaptureScreen"

@OptIn(UnstableApi::class)
@Composable
fun PostCaptureScreen(viewModel: PostCaptureViewModel = hiltViewModel()) {
    Log.d(TAG, "PostCaptureScreen")

    val uiState: PostCaptureUiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
        when (val media = uiState.media) {
            is Media.Image -> {
                val bitmap = media.bitmap
                ImageFromBitmap(Modifier.fillMaxSize(), bitmap)
            }

            is Media.Video -> {
                VideoPlayer(
                    modifier = Modifier,
                    player = viewModel.player
                )
                LaunchedEffect(media.uri) {
                    viewModel.startPostCapturePlayback()
                }
            }

            Media.None -> {
                Text(
                    text = stringResource(R.string.no_media_available),
                    modifier = Modifier.align(Alignment.Center)
                )
            }

            Media.Error -> {
                Text(
                    text = stringResource(R.string.error_loading_media),
                    modifier = Modifier.align(Alignment.Center)
                )
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

            // Share Media Button
            IconButton(
                onClick = {
                    val mediaDescriptor = uiState.mediaDescriptor

                    if (mediaDescriptor is MediaDescriptor.Image) {
                        shareImage(context, mediaDescriptor.uri, "image/jpeg")
                    }

                    if (mediaDescriptor is MediaDescriptor.Video) {
                        shareImage(context, mediaDescriptor.uri, "video/mp4")
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
 * Starts an intent to share media
 */
private fun shareImage(context: Context, uri: Uri, mimeType: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, uri)
    }
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Share Media"))
}
