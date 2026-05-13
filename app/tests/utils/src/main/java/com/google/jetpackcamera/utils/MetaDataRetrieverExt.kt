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
package com.google.jetpackcamera.utils

import android.media.MediaMetadataRetriever
import android.media.MediaMetadataRetriever.METADATA_KEY_DURATION
import android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO
import android.media.MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO
import android.media.MediaMetadataRetriever.METADATA_KEY_MIMETYPE
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT
import android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH
import android.util.Rational
import android.util.Size

inline fun <R> MediaMetadataRetriever.useAndRelease(
    crossinline block: (MediaMetadataRetriever) -> R
): R? {
    try {
        return block(this)
    } finally {
        release()
    }
}

fun MediaMetadataRetriever.hasAudio(): Boolean = extractMetadata(METADATA_KEY_HAS_AUDIO) == "yes"

fun MediaMetadataRetriever.hasVideo(): Boolean = extractMetadata(METADATA_KEY_HAS_VIDEO) == "yes"

fun MediaMetadataRetriever.getDurationMs(): Long =
    checkNotNull(extractMetadata(METADATA_KEY_DURATION)?.toLong()) {
        "duration unavailable"
    }

fun MediaMetadataRetriever.getWidth(): Int =
    checkNotNull(extractMetadata(METADATA_KEY_VIDEO_WIDTH)?.toInt()) {
        "width unavailable"
    }

fun MediaMetadataRetriever.getHeight(): Int =
    checkNotNull(extractMetadata(METADATA_KEY_VIDEO_HEIGHT)?.toInt()) {
        "height information unavailable"
    }

fun MediaMetadataRetriever.getResolution(): Size = Size(getWidth(), getHeight())

fun MediaMetadataRetriever.getAspectRatio(): Rational = Rational(getWidth(), getHeight())

fun MediaMetadataRetriever.getMimeType(): String = extractMetadata(METADATA_KEY_MIMETYPE)!!
