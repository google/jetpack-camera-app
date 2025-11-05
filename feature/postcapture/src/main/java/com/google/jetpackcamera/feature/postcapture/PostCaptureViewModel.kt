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
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.Date
import javax.inject.Inject

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
     * returns true if successfully saved, false if not
     */
    fun saveCurrentMedia(contentResolver: ContentResolver): Boolean =
        (uiState.value.mediaDescriptor as? MediaDescriptor.Content)
            ?.let {
                saveMediaDescriptorToMediaStore(contentResolver, it)
            } != null

    fun copyUriToUri(contentResolver: ContentResolver, sourceUri: Uri, targetUri: Uri) {
        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            contentResolver.openOutputStream(targetUri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            } ?: throw IOException("Could not open output stream for $targetUri")
        } ?: throw IOException("Could not open input stream for $sourceUri")
    }
}

private fun saveMediaDescriptorToMediaStore(
    contentResolver: ContentResolver,
    mediaDescriptor: MediaDescriptor.Content
): Uri? {
    val mimeType =
        if (mediaDescriptor is MediaDescriptor.Content.Video) "video/mp4" else "image/jpeg"
    val filename = createFilename(mediaDescriptor)
    return copyToMediaStore(contentResolver, mediaDescriptor.uri, filename, mimeType)
}
// todo(kc) support saving to target location
// saves cached media to default location
/**
 * Copies content from a source URI to a destination URI.
 *
 * @param context Context used to access the ContentResolver.
 * @param sourceUri The URI to read data from.
 * @param destinationUri The URI to write data to.
 * @return True if the copy was successful, false otherwise.
 */
private fun copyToMediaStore(
    contentResolver: ContentResolver,
    sourceUri: Uri,
    outputFilename: String,
    mimeType: String
): Uri? {
    // "video/mp4" else "image/jpeg"
    var destinationUri: Uri?

    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, outputFilename)
        put(MediaStore.MediaColumns.MIME_TYPE, mimeType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                Environment.DIRECTORY_DCIM + File.separator + "Camera"
            )
            // Mark as "pending" so the file isn't visible until we're done writing
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
    }
    try {
        destinationUri = contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )
        if (destinationUri == null) {
            throw IOException("Failed to create new MediaStore entry")
        }

        // Copy the data from source to destination
        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                inputStream.copyTo(outputStream)
            } ?: throw IOException("Failed to open output stream for $destinationUri")
        } ?: throw IOException("Failed to open input stream for $sourceUri")

        // 4. (API 29+) Publish the file by marking it as "not pending"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            contentResolver.update(destinationUri, contentValues, null, null)
        }

        Log.d(TAG, "File saved to MediaStore: $destinationUri")
        return destinationUri
    } catch (e: Exception) {
        Log.e(TAG, "Error saving to MediaStore: ${e.message}", e)

        return null
    }
}

fun createFilename(mediaDescriptor: MediaDescriptor.Content): String = when (mediaDescriptor) {
    is MediaDescriptor.Content.Image -> {
        "JCA-photo-${Date()}.mp4"
    }

    is MediaDescriptor.Content.Video -> {
        "JCA-recording-${Date()}.mp4"
    }
}

data class PostCaptureUiState(
    val mediaDescriptor: MediaDescriptor?,
    val media: Media
)
