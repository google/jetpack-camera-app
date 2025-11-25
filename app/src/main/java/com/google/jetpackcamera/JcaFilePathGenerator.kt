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
package com.google.jetpackcamera

import android.os.Environment
import com.google.jetpackcamera.core.common.FilePathGenerator
import java.io.File
import java.util.Date

class JcaFilePathGenerator : FilePathGenerator {
    private companion object {
        private val RELATIVE_OUTPUT_PATH: String =
            "${Environment.DIRECTORY_DCIM}${File.separator}Camera"
    }
    override val relativeImageOutputPath: String = RELATIVE_OUTPUT_PATH

    override val relativeVideoOutputPath: String = RELATIVE_OUTPUT_PATH

    override val absoluteVideoOutputPath: String
        get() = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES
        )?.path ?: ""

    private fun createTimestamp() = Date().time

    /**
     * constructs a string based on the provided values in the following pattern:
     * "$prefix-$suffixText-$timestamp-$FileExtension"
     *
     * @param prefix the first portion of the filename's text
     * @param timestamp a unique string to prevent subsequently created files from overwriting
     * each other. i.e. a timestamp
     * @param suffixText additional text to append to the end of the generated filename, before
     * the file extension.
     * @param fileExtension the extension to be appended at the end of the generated filename
     * (i.e. `.mp4` or `.jpg`)
     */
    private fun constructFilename(
        prefix: String,
        timestamp: String,
        suffixText: String?,
        fileExtension: String?
    ): String {
        return buildString {
            append(prefix)
            suffixText?.let { append("-$it") }
            append("-$timestamp")
            fileExtension?.let { append(it) }
        }
    }

    override fun generateImageFilename(suffixText: String?, fileExtension: String?): String {
        return constructFilename(
            "JCA-photo",
            createTimestamp().toString(),
            suffixText,
            fileExtension
        )
    }

    override fun generateVideoFilename(suffixText: String?, fileExtension: String?): String {
        return constructFilename(
            "JCA-recording",
            createTimestamp().toString(),
            suffixText,
            fileExtension
        )
    }
}
