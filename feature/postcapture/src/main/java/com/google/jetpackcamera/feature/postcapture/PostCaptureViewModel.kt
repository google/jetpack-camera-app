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
import android.net.Uri
import androidx.core.net.toFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
        val mediaDescriptor = uiState.value.mediaDescriptor
        if ((mediaDescriptor as? MediaDescriptor.Content)?.isCached == true) {
            deleteCachedMedia(mediaDescriptor)
        }

        viewModelScope.launch {
            mediaRepository.setCurrentMedia(MediaDescriptor.None)
        }
    }

    val player = ExoPlayer.Builder(context).build()

    val uiState: StateFlow<PostCaptureUiState> = _uiState

    fun deleteCachedMedia(mediaDescriptor: MediaDescriptor.Content) {
        mediaDescriptor.uri.toFile().delete()
    }

    fun deleteMedia(contentResolver: ContentResolver) {
        val mediaDescriptor = uiState.value.mediaDescriptor
        if (mediaDescriptor is MediaDescriptor.Content) {
            if (mediaDescriptor.isCached) {
                deleteCachedMedia(mediaDescriptor)
            } else {
                contentResolver.delete(mediaDescriptor.uri, null, null)
            }

            _uiState.update { it.copy(mediaDescriptor = MediaDescriptor.None, media = Media.None) }
        }
    }

    fun copyMedia(contentResolver: ContentResolver, sourceUri: Uri, targetUri: Uri) {
        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            } ?: throw IOException("Could not open output stream for $targetUri")
        } ?: throw IOException("Could not open input stream for $sourceUri")
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
}

data class PostCaptureUiState(
    val mediaDescriptor: MediaDescriptor?,
    val media: Media
)
