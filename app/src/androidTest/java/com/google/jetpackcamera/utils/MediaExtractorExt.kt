/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.media.MediaExtractor
import android.media.MediaFormat
import java.nio.ByteBuffer

private const val MICROS_PER_SECOND = 1_000_000L
private const val BITS_PER_BYTE = 8L

/**
 * Returns the average bitrate, in bits per second, of the first video track in the media file at
 * [path].
 *
 * The bitrate is computed from the total size of the encoded video samples divided by the video
 * track duration. Container metadata is not used because [MediaFormat.KEY_BIT_RATE] is often absent
 * from MP4 video tracks and [android.media.MediaMetadataRetriever.METADATA_KEY_BITRATE] includes
 * the audio track.
 */
fun getVideoTrackBitrate(path: String): Long {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(path)
        val trackIndex = checkNotNull(
            (0 until extractor.trackCount).firstOrNull { index ->
                extractor.getTrackFormat(index).getString(MediaFormat.KEY_MIME)
                    ?.startsWith("video/") == true
            }
        ) { "No video track found in $path" }
        val format = extractor.getTrackFormat(trackIndex)
        extractor.selectTrack(trackIndex)

        val buffer = ByteBuffer.allocate(format.getMaxSampleSize())
        var totalBytes = 0L
        var firstSampleTimeUs = -1L
        var lastSampleTimeUs = -1L
        while (true) {
            val sampleSize = extractor.readSampleData(buffer, 0)
            if (sampleSize < 0) break
            totalBytes += sampleSize
            if (firstSampleTimeUs < 0) firstSampleTimeUs = extractor.sampleTime
            lastSampleTimeUs = extractor.sampleTime
            extractor.advance()
        }

        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
            format.getLong(MediaFormat.KEY_DURATION)
        } else {
            lastSampleTimeUs - firstSampleTimeUs
        }
        check(durationUs > 0) { "Invalid video track duration ($durationUs us) in $path" }
        return totalBytes * BITS_PER_BYTE * MICROS_PER_SECOND / durationUs
    } finally {
        extractor.release()
    }
}

/**
 * Returns a buffer size large enough to hold any encoded sample of this video track.
 *
 * Uses [MediaFormat.KEY_MAX_INPUT_SIZE] when present, otherwise the size of an uncompressed
 * YUV 4:2:0 frame, which bounds the size of any encoded frame.
 */
private fun MediaFormat.getMaxSampleSize(): Int {
    val maxInputSize = if (containsKey(MediaFormat.KEY_MAX_INPUT_SIZE)) {
        getInteger(MediaFormat.KEY_MAX_INPUT_SIZE)
    } else {
        0
    }
    val rawFrameSize =
        getInteger(MediaFormat.KEY_WIDTH) * getInteger(MediaFormat.KEY_HEIGHT) * 3 / 2
    return maxOf(maxInputSize, rawFrameSize)
}
