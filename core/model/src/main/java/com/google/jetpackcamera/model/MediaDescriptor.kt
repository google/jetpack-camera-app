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
package com.google.jetpackcamera.model

import android.graphics.Bitmap
import android.net.Uri

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
