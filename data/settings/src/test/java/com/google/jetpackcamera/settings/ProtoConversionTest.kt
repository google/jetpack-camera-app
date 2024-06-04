/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.settings

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.settings.DynamicRange as DynamicRangeProto
import com.google.jetpackcamera.settings.ImageOutputFormat as ImageOutputFormatProto
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.DynamicRange.Companion.toProto
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.ImageOutputFormat.Companion.toProto
import org.junit.Test

class ProtoConversionTest {
    @Test
    fun dynamicRange_convertsToCorrectProto() {
        val correctConversions = { dynamicRange: DynamicRange ->
            when (dynamicRange) {
                DynamicRange.SDR -> DynamicRangeProto.DYNAMIC_RANGE_SDR
                DynamicRange.HLG10 -> DynamicRangeProto.DYNAMIC_RANGE_HLG10
                else -> TODO(
                    "Test does not yet contain correct conversion for dynamic range " +
                        "type: ${dynamicRange.name}"
                )
            }
        }

        enumValues<DynamicRange>().forEach {
            assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun dynamicRangeProto_convertsToCorrectDynamicRange() {
        val correctConversions = { dynamicRangeProto: DynamicRangeProto ->
            when (dynamicRangeProto) {
                DynamicRangeProto.DYNAMIC_RANGE_SDR,
                DynamicRangeProto.UNRECOGNIZED,
                DynamicRangeProto.DYNAMIC_RANGE_UNSPECIFIED
                -> DynamicRange.SDR

                DynamicRangeProto.DYNAMIC_RANGE_HLG10 -> DynamicRange.HLG10
                else -> TODO(
                    "Test does not yet contain correct conversion for dynamic range " +
                        "proto type: ${dynamicRangeProto.name}"
                )
            }
        }

        enumValues<DynamicRangeProto>().forEach {
            assertThat(correctConversions(it)).isEqualTo(DynamicRange.fromProto(it))
        }
    }

    @Test
    fun imageOutputFormat_convertsToCorrectProto() {
        val correctConversions = { imageOutputFormat: ImageOutputFormat ->
            when (imageOutputFormat) {
                ImageOutputFormat.JPEG -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG
                ImageOutputFormat.JPEG_ULTRA_HDR
                -> ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
                else -> TODO(
                    "Test does not yet contain correct conversion for image output format " +
                        "type: ${imageOutputFormat.name}"
                )
            }
        }

        enumValues<ImageOutputFormat>().forEach {
            assertThat(correctConversions(it)).isEqualTo(it.toProto())
        }
    }

    @Test
    fun imageOutputFormatProto_convertsToCorrectImageOutputFormat() {
        val correctConversions = { imageOutputFormatProto: ImageOutputFormatProto ->
            when (imageOutputFormatProto) {
                ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG,
                ImageOutputFormatProto.UNRECOGNIZED
                -> ImageOutputFormat.JPEG
                ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
                -> ImageOutputFormat.JPEG_ULTRA_HDR
                else -> TODO(
                    "Test does not yet contain correct conversion for image output format " +
                        "proto type: ${imageOutputFormatProto.name}"
                )
            }
        }

        enumValues<ImageOutputFormatProto>().forEach {
            assertThat(correctConversions(it)).isEqualTo(ImageOutputFormat.fromProto(it))
        }
    }
}
