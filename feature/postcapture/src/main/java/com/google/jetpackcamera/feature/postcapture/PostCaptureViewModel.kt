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
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "PostCaptureViewModel"

@HiltViewModel
class PostCaptureViewModel @Inject constructor(
    private val mediaRepository: MediaRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        PostCaptureUiState(
            mediaDescriptor = MediaDescriptor.None,
            media = Media.None
        )
    )
    val uiState: StateFlow<PostCaptureUiState> = _uiState

    val player = ExoPlayer.Builder(context).build()

    init {
        viewModelScope.launch {
            mediaRepository.currentMedia.filterNotNull().collectLatest { mediaDescriptor ->
                val mediaDescriptor = mediaDescriptor
                val media = mediaRepository.load(mediaDescriptor)
                _uiState.update {
                    it.copy(mediaDescriptor = mediaDescriptor, media = media)
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        val mediaDescriptor = uiState.value.mediaDescriptor

        if ((mediaDescriptor as? MediaDescriptor.Content)?.isCached == true) {
            viewModelScope.launch {
                mediaRepository.deleteMedia(context.contentResolver, mediaDescriptor)
                mediaRepository.setCurrentMedia(MediaDescriptor.None)
            }
        }
    }

    fun playVideo() {
        val media = uiState.value.media
        if (media is Media.Video) {
            val mediaItem = MediaItem.fromUri(media.uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE)
            player.play()
        }
    }

    /**
     * Saves the current media to the MediaStore.
     *
     * @param onMediaSaved a callback to be invoked with the result of the save operation.
     */
    inline fun saveCurrentMedia(crossinline onMediaSaved: (Boolean) -> Unit) {
        (uiState.value.mediaDescriptor as? MediaDescriptor.Content)?.let {
            viewModelScope.launch {
                // FIXME(kc): set up proper save events
                onMediaSaved(saveMedia(it))
            }
        }
    }

    /**
     * returns true if successfully saved, false if not
     */
    suspend fun saveMedia(mediaDescriptor: MediaDescriptor.Content): Boolean =
        mediaRepository.saveToMediaStore(
            context.contentResolver,
            mediaDescriptor,
            createFilename(mediaDescriptor)
        ) != null

    /**
     * Deletes the given media.
     *
     * @param mediaDescriptor the [MediaDescriptor] of the media to be deleted.
     */
    fun deleteMedia(mediaDescriptor: MediaDescriptor.Content) {
        viewModelScope.launch {
            mediaRepository.deleteMedia(context.contentResolver, mediaDescriptor)
            _uiState.update { it.copy(mediaDescriptor = MediaDescriptor.None, media = Media.None) }
        }
    }
}

/**
 * Creates a filename for the media descriptor.
 *
 * @param mediaDescriptor the [MediaDescriptor] to create a filename for.
 *
 * @return a filename for the media descriptor.
 */
private fun createFilename(mediaDescriptor: MediaDescriptor.Content): String {
    val timeStamp =
        java.text.SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", java.util.Locale.US).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                format(java.time.LocalDateTime.now())
            } else {
                format(Date())
            }
        }

    return when (mediaDescriptor) {
        is MediaDescriptor.Content.Image -> {
            "JCA-photo-$timeStamp.jpg"
        }

        is MediaDescriptor.Content.Video -> {
            "JCA-recording-$timeStamp.mp4"
        }
    }
}

data class PostCaptureUiState(
    val mediaDescriptor: MediaDescriptor?,
    val media: Media
)
