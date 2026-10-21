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
package com.google.jetpackcamera.data.media

import android.graphics.Bitmap
import android.net.Uri
import kotlinx.coroutines.flow.StateFlow

/**
 * Data layer for Media.
 */
interface MediaRepository {
    val currentMedia: StateFlow<MediaDescriptor>

    /**
     * A [StateFlow] that emits the most recently captured media (image or video) from the MediaStore.
     *
     * The flow will emit a new [MediaDescriptor] whenever a new media item is saved to the
     * MediaStore. The initial value is the most recent media available at the time of collection.
     */
    val lastCapturedMedia: StateFlow<MediaDescriptor>

    /**
     * Requests that [lastCapturedMedia] re-query the MediaStore for the most recent media.
     *
     * [lastCapturedMedia] already updates on MediaStore change notifications. This is only
     * needed when a previous query may have produced a stale result for reasons the MediaStore
     * does not notify about, such as a storage permission being granted after the repository was
     * created.
     */
    fun refreshLastCapturedMedia()

    fun setCurrentMedia(pendingMedia: MediaDescriptor)

    /**
     * Deletes the [currentMedia] from the cache if it is a cached [MediaDescriptor.Content] and
     * resets [currentMedia] to [lastCapturedMedia].
     *
     * Does nothing if [currentMedia] is not cached.
     */
    fun clearCurrentCachedMedia()

    suspend fun deleteMedia(mediaDescriptor: MediaDescriptor.Content): Boolean

    suspend fun copyToUri(mediaDescriptor: MediaDescriptor.Content, destinationUri: Uri)
    suspend fun saveToMediaStore(
        mediaDescriptor: MediaDescriptor.Content,
        outputFilename: String?
    ): Uri?
    suspend fun load(mediaDescriptor: MediaDescriptor): Media
}

/**
 * Descriptors used for [Media].
 *
 * Media descriptors contain a reference to a [Media] item that's not yet loaded.
 */
sealed interface MediaDescriptor {
    data object None : MediaDescriptor

    sealed interface Content : MediaDescriptor {
        val uri: Uri
        val thumbnail: Bitmap?
        val isCached: Boolean

        data class Image(
            override val uri: Uri,
            override val thumbnail: Bitmap?,
            override val isCached: Boolean = false
        ) : Content

        data class Video(
            override val uri: Uri,
            override val thumbnail: Bitmap?,
            override val isCached: Boolean = false
        ) : Content
    }
}

/**
 * Media items that are supported by [MediaRepository].
 *
 * [Image] will have the bitmap data loaded.
 * [Video] is still a reference to the video file, will switch to a loaded version later on.
 *
 * TODO(yasith): Load the video data to the Video object.
 */
sealed interface Media {
    data object None : Media
    data object Error : Media
    class Image(val bitmap: Bitmap) : Media
    class Video(val uri: Uri) : Media
}
