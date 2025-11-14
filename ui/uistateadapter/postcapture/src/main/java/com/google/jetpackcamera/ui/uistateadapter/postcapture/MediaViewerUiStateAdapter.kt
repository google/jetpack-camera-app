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
package com.google.jetpackcamera.ui.uistateadapter.postcapture

import androidx.media3.common.Player
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.postcapture.MediaViewerUiState

fun MediaViewerUiState.Companion.from(
    mediaDescriptor: MediaDescriptor,
    media: Media,
    player: Player?,
    playerState: Boolean
): MediaViewerUiState {
    return when (media) {
        Media.Error, Media.None -> MediaViewerUiState.Loading
        is Media.Image -> MediaViewerUiState.Content.Image(media.bitmap)
        is Media.Video -> {
            val thumbnail =
                (mediaDescriptor as? MediaDescriptor.Content.Video)?.thumbnail
            if (playerState) {
                player?.let {
                    MediaViewerUiState.Content.Video.Ready(
                        it,
                        thumbnail
                    )
                } ?: MediaViewerUiState.Content.Video.Loading(thumbnail)
            } else {
                MediaViewerUiState.Content.Video.Loading(thumbnail)
            }
        }
    }
}
