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
    /**
     * A [StateFlow] of the current [MediaDescriptor] being viewed or processed.
     */
    val currentMedia: StateFlow<MediaDescriptor>

    /**
     * Sets the current media item to be viewed or processed.
     *
     * @param pendingMedia The [MediaDescriptor] to set as current.
     */
    suspend fun setCurrentMedia(pendingMedia: MediaDescriptor)

    /**
     * Retrieves the most recently captured media item.
     *
     * @return The [MediaDescriptor] of the last captured media.
     */
    suspend fun getLastCapturedMedia(): MediaDescriptor

    /**
     * Deletes a media item from storage.
     *
     * @param mediaDescriptor The [MediaDescriptor.Content] of the media to delete.
     * @return `true` if the deletion was successful, `false` otherwise.
     */
    suspend fun deleteMedia(mediaDescriptor: MediaDescriptor.Content): Boolean

    /**
     * Copies the content of a media item to a specified URI.
     *
     * @param mediaDescriptor The [MediaDescriptor.Content] of the media to copy.
     * @param destinationUri The [Uri] to copy the media to.
     */
    suspend fun copyToUri(mediaDescriptor: MediaDescriptor.Content, destinationUri: Uri)

    /**
     * Saves a media item to the device's MediaStore.
     *
     * @param mediaDescriptor The [MediaDescriptor.Content] of the media to save.
     * @param outputFilename An optional filename for the saved media.
     * @return The [Uri] of the saved media, or `null` if the save operation failed.
     */
    suspend fun saveToMediaStore(
        mediaDescriptor: MediaDescriptor.Content,
        outputFilename: String?
    ): Uri?

    /**
     * Loads the full [Media] item from a [MediaDescriptor].
     *
     * @param mediaDescriptor The descriptor of the media to load.
     * @return The loaded [Media] item.
     */
    suspend fun load(mediaDescriptor: MediaDescriptor): Media
}

/**
 * Descriptors used for [Media].
 *
 * Media descriptors contain a reference to a [Media] item that's not yet loaded.
 */
sealed interface MediaDescriptor {
    /**
     * Represents the absence of a media item.
     */
    data object None : MediaDescriptor

    /**
     * A sealed interface representing content that can be described by a [MediaDescriptor].
     *
     * @property uri The [Uri] of the media content.
     * @property thumbnail An optional [Bitmap] thumbnail for the media.
     * @property isCached `true` if the media is stored in the app's cache, `false` otherwise.
     */
    sealed interface Content : MediaDescriptor {
        val uri: Uri
        val thumbnail: Bitmap?
        val isCached: Boolean

        /**
         * A [MediaDescriptor] for an image.
         */
        class Image(
            override val uri: Uri,
            override val thumbnail: Bitmap?,
            override val isCached: Boolean = false
        ) : Content

        /**
         * A [MediaDescriptor] for a video.
         */
        class Video(
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
    /**
     * Represents the absence of a media item.
     */
    data object None : Media

    /**
     * Represents an error state when loading media.
     */
    data object Error : Media

    /**
     * A loaded image media item.
     *
     * @property bitmap The full-size [Bitmap] of the image.
     */
    class Image(val bitmap: Bitmap) : Media

    /**
     * A loaded video media item.
     *
     * @property uri The [Uri] of the video file.
     */
    class Video(val uri: Uri) : Media
}
