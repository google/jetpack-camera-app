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

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.modifiers.resizeWithContentScale
import androidx.media3.ui.compose.state.rememberPresentationState
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_DELETE
import com.google.jetpackcamera.feature.postcapture.ui.BUTTON_POST_CAPTURE_SHARE
import com.google.jetpackcamera.feature.postcapture.ui.CancelPostCaptureButton
import com.google.jetpackcamera.feature.postcapture.ui.SaveCurrentMediaButton
import com.google.jetpackcamera.feature.postcapture.ui.VIEWER_POST_CAPTURE_IMAGE
import com.google.jetpackcamera.feature.postcapture.ui.VIEWER_POST_CAPTURE_VIDEO
import java.io.File
import java.io.FileNotFoundException

private const val TAG = "PostCaptureScreen"

@OptIn(UnstableApi::class)
@Composable
fun PostCaptureScreen(
    onNavigateBack: () -> Unit,
    viewModel: PostCaptureViewModel = hiltViewModel()
) {
    Log.d(TAG, "PostCaptureScreen")

    val uiState: PostCaptureUiState by viewModel.uiState.collectAsState()
    PostCaptureComponent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        player = viewModel.player,
        playVideo = viewModel::playVideo,
        onDeleteMedia = {
            (uiState.mediaDescriptor as? MediaDescriptor.Content)
                ?.let { viewModel.deleteMedia(it) }
        },
        onSaveMedia = { block ->
            viewModel.saveCurrentMedia { block(it) }
        }
    )
}

@OptIn(UnstableApi::class)
@Composable
fun PostCaptureComponent(
    uiState: PostCaptureUiState,
    onNavigateBack: () -> Unit,
    player: ExoPlayer,
    playVideo: () -> Unit,
    onSaveMedia: ((Boolean) -> Unit) -> Unit,
    onDeleteMedia: () -> Unit
) {
    val context = LocalContext.current
    Box(modifier = Modifier.fillMaxSize()) {
        when (val currentMedia = uiState.media) {
            is Media.Image -> {
                val bitmap = currentMedia.bitmap
                Canvas(
                    modifier = Modifier
                        .testTag(VIEWER_POST_CAPTURE_IMAGE)
                        .fillMaxSize()
                ) {
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
                val presentationState = rememberPresentationState(player)
                PlayerSurface(
                    player = player,
                    modifier = Modifier
                        .testTag(VIEWER_POST_CAPTURE_VIDEO)
                        .resizeWithContentScale(
                            ContentScale.Fit,
                            presentationState.videoSizeDp
                        )
                )
                playVideo()
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

        CancelPostCaptureButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .safeContentPadding(),
            onExitPostCapture = onNavigateBack
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            val saveSuccessString = stringResource(R.string.toast_save_success)
            val saveFailureString = stringResource(R.string.toast_save_failure)
            SaveCurrentMediaButton(onClick = {
                // FIXME(kc): set up proper save events
                onSaveMedia { isSaved ->
                    if (isSaved) {
                        Toast.makeText(context, saveSuccessString, Toast.LENGTH_SHORT)
                            .show()
                    } else {
                        Toast.makeText(context, saveFailureString, Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            })
            // Delete Image Button visible for saved media
            if ((uiState.mediaDescriptor as? MediaDescriptor.Content)?.isCached != true) {
                IconButton(
                    onClick = {
                        onDeleteMedia()
                        onNavigateBack()
                    },
                    modifier = Modifier
                        .size(56.dp)
                        .shadow(10.dp, CircleShape)
                        .testTag(BUTTON_POST_CAPTURE_DELETE),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(
                            R.string.button_delete_media_description
                        ),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Share Media Button
            IconButton(
                onClick = {
                    val mediaDescriptor = uiState.mediaDescriptor
                    (mediaDescriptor as? MediaDescriptor.Content)?.let {
                        shareMedia(context, it)
                    }
                },
                modifier = Modifier
                    .size(56.dp)
                    .shadow(10.dp, CircleShape)
                    .testTag(BUTTON_POST_CAPTURE_SHARE),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = stringResource(R.string.button_share_media_description),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

/**
 * Starts an intent to share media.
 *
 * @param context the context of the calling component.
 * @param mediaDescriptor the [MediaDescriptor] of the media to be shared.
 */
private fun shareMedia(context: Context, mediaDescriptor: MediaDescriptor.Content) {
    // todo(kc): support sharing multiple media
    val uri = mediaDescriptor.uri
    val mimeType: String = when (mediaDescriptor) {
        is MediaDescriptor.Content.Image -> "image/jpeg"
        is MediaDescriptor.Content.Video -> "video/mp4"
    }

    val contentUri: Uri =
        if (uri.scheme == ContentResolver.SCHEME_CONTENT) uri else getShareableUri(context, uri)

    val intent = Intent(Intent.ACTION_SEND).apply {
        type = mimeType
        putExtra(Intent.EXTRA_STREAM, contentUri)
    }
    intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)

    // todo(kc): prevent "edit image" from appearing in the ShareSheet.
    context.startActivity(Intent.createChooser(intent, "Share Media"))
}

/**
 * Creates a content Uri for a given file Uri.
 *
 * @param context the context of the calling component.
 * @param uri the Uri of the file.
 *
 * @return a content Uri to be used for sharing.
 */
private fun getShareableUri(context: Context, uri: Uri): Uri {
    val authority = "${context.packageName}.fileprovider"
    val file =
        uri.path
            ?.let { File(it) }
            ?: throw FileNotFoundException("path does not exist")

    return FileProvider.getUriForFile(context, authority, file)
}
