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
     * Generates a unique filename for an image file.
     */
    fun generateImageFilename(): String

    /**
     * Generates a unique filename for a video file.
     */
    fun generateVideoFilename(): String

    /**
     * Provides the relative output path for an image file, used for MediaStore.
     */
    val relativeImageOutputPath: String

    /**
     * Provides the relative output path for a video file, used for MediaStore on API 29+.
     */
    val relativeVideoOutputPath: String

    /**
     * Provides the absolute output path for a video file, used for MediaStore on API 28 and below.
     */
    val absoluteVideoOutputPath: String
}
