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

/**
 * Data layer for Media.
 */
interface MediaRepository {
    suspend fun getLastCapturedMedia(): MediaDescriptor
    suspend fun load(mediaDescriptor: MediaDescriptor): Media
}

/**
 * Descriptors used for [Media].
 */
sealed class MediaDescriptor {
    data object None : MediaDescriptor()
    class Image(val uri: Uri, val thumbnail: Bitmap?) : MediaDescriptor()
    class Video(val uri: Uri, val thumbnail: Bitmap?) : MediaDescriptor()
}

/**
 * Media items that are supported by [MediaRepository].
 */
sealed class Media {
    data object None : Media()
    class Image(val bitmap: Bitmap?) : Media()
    class Video(val uri: Uri) : Media()
}
