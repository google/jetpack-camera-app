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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.COMMAND_PLAY_PAUSE
import androidx.media3.common.Player.COMMAND_SET_MEDIA_ITEM
import androidx.media3.exoplayer.ExoPlayer
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class PostCaptureViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    val playerListener = object : Player.Listener {
        /**
         * This callback is the single source of truth for
         * what the UI is allowed to do.
         */
        override fun onAvailableCommandsChanged(commands: Player.Commands) {
            super.onAvailableCommandsChanged(commands)
            playerState.update {
                it.copy(
                    canPlayPause = commands.contains(COMMAND_PLAY_PAUSE),
                    canSetMediaItem = commands.contains(COMMAND_SET_MEDIA_ITEM)
                )
            }
        }
    }
    init {
        getLastCapture()
        initPlayer()
    }

    private val _uiState = MutableStateFlow(
        PostCaptureUiState(
            mediaDescriptor = MediaDescriptor.None,
            media = Media.None
        )
    )
    private val playerState = MutableStateFlow(
        PlayerState()
    )

    var player: ExoPlayer? = null // ExoPlayer.Builder(context).build()

    val uiState: StateFlow<PostCaptureUiState> = _uiState

    fun getLastCapture() {
        viewModelScope.launch {
            val mediaDescriptor = mediaRepository.getLastCapturedMedia()
            val media = mediaRepository.load(mediaDescriptor)

            _uiState.update { it.copy(mediaDescriptor = mediaDescriptor, media = media) }
        }
    }

    fun deleteMedia(contentResolver: ContentResolver) {
        when (val mediaDescriptor = uiState.value.mediaDescriptor) {
            is MediaDescriptor.Image -> contentResolver.delete(mediaDescriptor.uri, null, null)
            is MediaDescriptor.Video -> contentResolver.delete(mediaDescriptor.uri, null, null)
            MediaDescriptor.None -> {}
        }
        _uiState.update { it.copy(mediaDescriptor = MediaDescriptor.None, media = Media.None) }
    }

    // exoplayer controls
    fun initPlayer() {
        player?.release()
        player = ExoPlayer.Builder(context).build()
        player?.addListener(playerListener)
    }

    fun releasePlayer() {
        player?.release()
        player = null
    }

    fun loadVideo() {
        val media = uiState.value.media
        if (media is Media.Video) {
            player?.stop()
            val mediaItem = MediaItem.fromUri(media.uri)
            player!!.setMediaItem(mediaItem)
            player!!.prepare()
        }
    }

    fun playPostCaptureVideo()  {
        player?.let {
            loadVideo()
            it.repeatMode = ExoPlayer.REPEAT_MODE_ONE
            playVideo()
        }
    }

    fun playVideo() {
        player?.let { exoPlayer ->
            exoPlayer.prepare()
            if (playerState.value.canPlayPause) {
                exoPlayer.play()
            }
        }
    }

    fun pauseVideo() {
        player?.let {
            if (playerState.value.canPlayPause) {
                it.pause()
            }
        }
    }
}

data class PostCaptureUiState(
    val mediaDescriptor: MediaDescriptor,
    val media: Media
)

data class PlayerState(
    val canPlayPause: Boolean = false,
    val canSetMediaItem: Boolean = false
)
