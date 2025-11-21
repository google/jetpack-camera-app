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

class JcaFilePathGenerator() : FilePathGenerator {
    private fun createTimestamp() = Date().time

    override fun generateImageFilename(): String {
        val timestamp = createTimestamp()
        return "JCA-photo-$timestamp.jpg"
    }

    override fun generateVideoFilename(): String {
        val timestamp = createTimestamp()
        return "JCA-recording-$timestamp.mp4"
    }

    override val relativeImageOutputPath: String
        get() = Environment.DIRECTORY_DCIM + File.separator + "Camera"

    override val relativeVideoOutputPath: String
        get() = Environment.DIRECTORY_DCIM + File.separator + "Camera"

    override val absoluteVideoOutputPath: String
        get() = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_MOVIES
        ).path
}
