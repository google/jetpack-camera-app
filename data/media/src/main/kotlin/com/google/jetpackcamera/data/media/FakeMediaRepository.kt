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

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A fake implementation of [MediaRepository] for use in tests.
 */
class FakeMediaRepository : MediaRepository {
    private val _currentMedia = MutableStateFlow<MediaDescriptor>(MediaDescriptor.None)

    override val currentMedia = _currentMedia.asStateFlow()

    /**
     * A handler for the [load] function.
     *
     * Tests can provide a custom implementation to simulate different loading scenarios.
     */
    var loadHandler: (MediaDescriptor) -> Media = { Media.None }
    /**
     * A handler for the [saveToMediaStore] function.
     *
     * Tests can provide a custom implementation to simulate different save scenarios.
     */
    var saveToMediaStoreHandler: (MediaDescriptor.Content) -> Uri? = { mediaDescriptor ->
        when (mediaDescriptor) {
            is MediaDescriptor.Content.Image -> "img.jpg".toUri()
            is MediaDescriptor.Content.Video -> "video.mp4".toUri()
        }
    }
    /**
     * A handler for the [deleteMedia] function.
     *
     * Tests can provide a custom implementation to simulate different delete scenarios.
     */
    var deleteMediaHandler: (MediaDescriptor.Content) -> Boolean = { true }

    override suspend fun setCurrentMedia(pendingMedia: MediaDescriptor) {
        _currentMedia.update { pendingMedia }
    }

    override suspend fun getLastCapturedMedia(): MediaDescriptor {
        return MediaDescriptor.None
    }

    override suspend fun load(mediaDescriptor: MediaDescriptor): Media {
        return loadHandler(mediaDescriptor)
    }

    override suspend fun deleteMedia(mediaDescriptor: MediaDescriptor.Content): Boolean {
        val result = deleteMediaHandler(mediaDescriptor)
        if (result && mediaDescriptor == currentMedia.value) {
            _currentMedia.update { MediaDescriptor.None }
        }
        return result
    }

    override suspend fun saveToMediaStore(
        mediaDescriptor: MediaDescriptor.Content,
        outputFilename: String?
    ): Uri? {
        return saveToMediaStoreHandler(mediaDescriptor)
    }

    override suspend fun copyToUri(mediaDescriptor: MediaDescriptor.Content, destinationUri: Uri) {
    }
}
