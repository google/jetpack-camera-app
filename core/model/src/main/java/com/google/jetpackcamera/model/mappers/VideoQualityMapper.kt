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

package com.google.jetpackcamera.model.mappers

import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.VideoQuality.FHD
import com.google.jetpackcamera.model.VideoQuality.HD
import com.google.jetpackcamera.model.VideoQuality.SD
import com.google.jetpackcamera.model.VideoQuality.UHD
import com.google.jetpackcamera.model.VideoQuality.UNSPECIFIED
import com.google.jetpackcamera.model.proto.VideoQuality as VideoQualityProto


/** returns the VideoQuality enum equivalent of a provided VideoQualityProto */
fun VideoQualityProto.fromProto(): VideoQuality {
    return when (this) {
        VideoQualityProto.VIDEO_QUALITY_SD -> SD
        VideoQualityProto.VIDEO_QUALITY_HD -> HD
        VideoQualityProto.VIDEO_QUALITY_FHD -> FHD
        VideoQualityProto.VIDEO_QUALITY_UHD -> UHD
        VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED,
        VideoQualityProto.UNRECOGNIZED
            -> UNSPECIFIED
    }
}

fun VideoQuality.toProto(): VideoQualityProto {
    return when (this) {
        UNSPECIFIED -> VideoQualityProto.VIDEO_QUALITY_UNSPECIFIED
        SD -> VideoQualityProto.VIDEO_QUALITY_SD
        HD -> VideoQualityProto.VIDEO_QUALITY_HD
        FHD -> VideoQualityProto.VIDEO_QUALITY_FHD
        UHD -> VideoQualityProto.VIDEO_QUALITY_UHD

    }
}