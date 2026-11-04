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
package com.google.jetpackcamera.core.common

import android.os.Environment
import java.io.File
import java.util.Date

/**
 * Default implementation of [FilePathGenerator] that stores captured media in a configurable
 * MediaStore relative directory (defaulting to `DCIM/Camera`) and prefixes generated filenames
 * with [prefix].
 *
 * Note that [relativeOutputPath] configures the MediaStore relative paths ([baseRelativePath],
 * [relativeImageOutputPath], and [relativeVideoOutputPath]) used on API 29+, whereas
 * [absoluteVideoOutputPath] (used only for legacy video capture on API 28 and below) always points
 * to the system's public [Environment.DIRECTORY_MOVIES] directory.
 *
 * @property prefix prefix prepended to generated image and video filenames. When empty, no leading
 * prefix or separator is added before `photo` or `recording`.
 * @param relativeOutputPath relative directory path used for [baseRelativePath],
 * [relativeImageOutputPath], and [relativeVideoOutputPath].
 */
class DefaultFilePathGenerator(
    override val prefix: String = DEFAULT_PREFIX,
    private val relativeOutputPath: String = DEFAULT_RELATIVE_OUTPUT_PATH
) : FilePathGenerator {
    companion object {
        const val DEFAULT_PREFIX: String = "JCA"
        val DEFAULT_RELATIVE_OUTPUT_PATH: String =
            "${Environment.DIRECTORY_DCIM}${File.separator}Camera"
    }

    override val baseRelativePath: String = relativeOutputPath

    override val relativeImageOutputPath: String = relativeOutputPath

    override val relativeVideoOutputPath: String = relativeOutputPath

    // Environment.getExternalStoragePublicDirectory is deprecated in API 29+,
    // but is still required on API <= 28 where MediaStore.MediaColumns.RELATIVE_PATH is unavailable
    // and legacy video output requires an absolute filesystem path for MediaStore.MediaColumns.DATA.
    @Suppress("DEPRECATION")
    override val absoluteVideoOutputPath: String
        get() = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES
        )?.path ?: ""

    private fun createTimestamp() = Date().time

    /**
     * Constructs a filename string in the pattern:
     * `[<prefix>-]<baseName>[-<suffixText>]-<timestamp>[<fileExtension>]`
     */
    private fun constructFilename(
        baseName: String,
        timestamp: String,
        suffixText: String?,
        fileExtension: String?
    ): String {
        return buildString {
            if (prefix.isNotEmpty()) {
                append(prefix)
                append('-')
            }
            append(baseName)
            suffixText?.let { append("-$it") }
            append("-$timestamp")
            fileExtension?.let { append(it) }
        }
    }

    override fun generateImageFilename(suffixText: String?, fileExtension: String?): String {
        return constructFilename(
            baseName = "photo",
            timestamp = createTimestamp().toString(),
            suffixText = suffixText,
            fileExtension = fileExtension
        )
    }

    override fun generateVideoFilename(suffixText: String?, fileExtension: String?): String {
        return constructFilename(
            baseName = "recording",
            timestamp = createTimestamp().toString(),
            suffixText = suffixText,
            fileExtension = fileExtension
        )
    }
}
