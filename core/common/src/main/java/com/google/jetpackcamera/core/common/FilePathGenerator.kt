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
package com.google.jetpackcamera.core.common

/**
 * Data layer interface for providing file names and directories for captured media.
 */
interface FilePathGenerator {

    /**
     * Provides the relative output path for an image file, used for MediaStore.
     *
     * (i.e [android.os.Environment.DIRECTORY_DCIM], [android.os.Environment.DIRECTORY_PICTURES])
     */
    val relativeImageOutputPath: String

    /**
     * Provides the relative output path for a video file, used for MediaStore on API 29+.
     *
     * (i.e [android.os.Environment.DIRECTORY_DCIM], [android.os.Environment.DIRECTORY_MOVIES])
     */
    val relativeVideoOutputPath: String

    /**
     * Provides the absolute output path for a video file, used for MediaStore on API 28 and below.
     */
    val absoluteVideoOutputPath: String

    /**
     * Generates a unique filename for an image file.
     *
     * @param suffixText additional text to append to the end of the generated filename, before the
     * file extension.
     * @param fileExtension the extension to be appended at the end of the generated filename.
     */
    fun generateImageFilename(suffixText: String? = null, fileExtension: String? = ".jpg"): String

    /**
     * Generates a unique filename for a video file.
     *
     * @param suffixText additional text to append to the end of the generated filename, before the
     * file extension.
     * @param fileExtension the extension to be appended at the end of the generated filename
     */
    fun generateVideoFilename(suffixText: String? = null, fileExtension: String? = ".mp4"): String

    companion object {
        /**
         * constructs a filename based on the provided values
         *
         * @param prefix the first portion of the filename's text
         * @param timestamp a unique string to prevent subsequently created files from overwriting
         * each other. i.e. a timestamp
         * @param suffixText additional text to append to the end of the generated filename, before
         * the file extension.
         * @param fileExtension the extension to be appended at the end of the generated filename
         * (i.e. `.mp4` or `.jpg`)
         */
        fun constructFilename(
            prefix: String,
            timestamp: String,
            suffixText: String?,
            fileExtension: String?
        ): String {
            return buildString {
                append("$prefix-$timestamp")
                suffixText?.let { append("-$it") }
                fileExtension?.let { append(it) }
            }
        }
    }
}
