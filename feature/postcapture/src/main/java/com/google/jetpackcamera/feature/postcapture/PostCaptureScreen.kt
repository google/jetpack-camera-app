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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.feature.postcapture.ui.DeleteCurrentMediaButton
import com.google.jetpackcamera.feature.postcapture.ui.ExitPostCaptureButton
import com.google.jetpackcamera.feature.postcapture.ui.ImageFromBitmap
import com.google.jetpackcamera.feature.postcapture.ui.PostCaptureLayout
import com.google.jetpackcamera.feature.postcapture.ui.SaveCurrentMediaButton
import com.google.jetpackcamera.feature.postcapture.ui.ShareCurrentMediaButton
import com.google.jetpackcamera.feature.postcapture.ui.VideoPlayer
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
        onDeleteMedia = {
            (uiState.mediaDescriptor as? MediaDescriptor.Content)?.let {
                viewModel.deleteMedia(it)
            }
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
    player: ExoPlayer?,
    onSaveMedia: ((Boolean) -> Unit) -> Unit,
    onDeleteMedia: () -> Unit
) {
    val context = LocalContext.current
    PostCaptureLayout(
        mediaSurface = {
            MediaViewer(
                modifier = it,
                media = uiState.media,
                player = player
            )
        },
        exitButton = {
            ExitPostCaptureButton(
                modifier = it,
                onExitPostCapture = onNavigateBack
            )
        },
        saveButton = {
            val saveSuccessString = stringResource(R.string.toast_save_success)
            val saveFailureString = stringResource(R.string.toast_save_failure)
            SaveCurrentMediaButton(modifier = it, onClick = {
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
        },
        shareButton = {
            ShareCurrentMediaButton(modifier = it, onClick = {
                val mediaDescriptor = uiState.mediaDescriptor
                (mediaDescriptor as? MediaDescriptor.Content)?.let { descriptor ->
                    shareMedia(context, descriptor)
                }
            })
        },
        deleteButton = {
            if ((uiState.mediaDescriptor as? MediaDescriptor.Content)?.isCached != true) {
                DeleteCurrentMediaButton(onClick = {
                    onDeleteMedia()
                    onNavigateBack()
                })
            }
        }
    )
}

@Composable
private fun MediaViewer(media: Media, player: ExoPlayer?, modifier: Modifier = Modifier) {
    when (media) {
        is Media.Image -> {
            val bitmap = media.bitmap
            ImageFromBitmap(modifier, bitmap)
        }

        is Media.Video -> {
            player?.let {
                VideoPlayer(modifier = modifier, player = it)
            } ?: @Composable {
                Log.w(TAG, "null player resource for Video Media playback")
                Text(modifier = modifier, text = stringResource(R.string.player_unavailable))
            }
        }

        Media.None -> {
            Text(modifier = modifier, text = stringResource(R.string.no_media_available))
        }

        Media.Error -> {
            Text(modifier = modifier, text = stringResource(R.string.error_loading_media))
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

    // if the uri isn't already managed by a content provider, we will need
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
