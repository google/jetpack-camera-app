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
import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DefaultFilePathGeneratorTest {

    @Test
    fun defaultProperties_returnExpectedDefaults() {
        val generator = DefaultFilePathGenerator()
        val expectedRelativePath = "${Environment.DIRECTORY_DCIM}${File.separator}Camera"

        assertThat(generator.prefix).isEqualTo("JCA")
        assertThat(generator.baseRelativePath).isEqualTo(expectedRelativePath)
        assertThat(generator.relativeImageOutputPath).isEqualTo(expectedRelativePath)
        assertThat(generator.relativeVideoOutputPath).isEqualTo(expectedRelativePath)
        @Suppress("DEPRECATION")
        assertThat(generator.absoluteVideoOutputPath)
            .isEqualTo(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)?.path
                    ?: ""
            )
    }

    @Test
    fun customProperties_overridePrefixAndRelativePaths() {
        val customPath = "${Environment.DIRECTORY_PICTURES}${File.separator}CustomSubdir"
        val generator = DefaultFilePathGenerator(
            prefix = "CustomApp",
            relativeOutputPath = customPath
        )

        assertThat(generator.prefix).isEqualTo("CustomApp")
        assertThat(generator.baseRelativePath).isEqualTo(customPath)
        assertThat(generator.relativeImageOutputPath).isEqualTo(customPath)
        assertThat(generator.relativeVideoOutputPath).isEqualTo(customPath)
    }

    @Test
    fun generateImageFilename_withDefaults_formatsWithJcaPrefixAndJpgExtension() {
        val generator = DefaultFilePathGenerator()

        val filename = generator.generateImageFilename()

        assertThat(filename).matches("JCA-photo-\\d+\\.jpg")
    }

    @Test
    fun generateImageFilename_withCustomPrefixSuffixAndExtension_formatsCorrectly() {
        val generator = DefaultFilePathGenerator(prefix = "CustomApp")

        val filename = generator.generateImageFilename(
            suffixText = "burst1",
            fileExtension = ".dng"
        )

        assertThat(filename).matches("CustomApp-photo-burst1-\\d+\\.dng")
    }

    @Test
    fun generateImageFilename_withNullSuffixAndNullExtension_omitsOptionalParts() {
        val generator = DefaultFilePathGenerator(prefix = "CustomApp")

        val filename = generator.generateImageFilename(
            suffixText = null,
            fileExtension = null
        )

        assertThat(filename).matches("CustomApp-photo-\\d+")
        assertThat(filename).doesNotContain("null")
    }

    @Test
    fun generateVideoFilename_withDefaults_formatsWithJcaPrefixAndMp4Extension() {
        val generator = DefaultFilePathGenerator()

        val filename = generator.generateVideoFilename()

        assertThat(filename).matches("JCA-recording-\\d+\\.mp4")
    }

    @Test
    fun generateVideoFilename_withCustomPrefixSuffixAndExtension_formatsCorrectly() {
        val generator = DefaultFilePathGenerator(prefix = "CustomApp")

        val filename = generator.generateVideoFilename(
            suffixText = "clip2",
            fileExtension = ".3gp"
        )

        assertThat(filename).matches("CustomApp-recording-clip2-\\d+\\.3gp")
    }

    @Test
    fun generateFilenames_withEmptyPrefix_omitsLeadingHyphen() {
        val generator = DefaultFilePathGenerator(prefix = "")

        val imageFilename = generator.generateImageFilename()
        val videoFilename = generator.generateVideoFilename()

        assertThat(imageFilename).matches("photo-\\d+\\.jpg")
        assertThat(videoFilename).matches("recording-\\d+\\.mp4")
    }
}
