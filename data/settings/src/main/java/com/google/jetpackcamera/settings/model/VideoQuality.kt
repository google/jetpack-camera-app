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
package com.google.jetpackcamera.settings.model

import com.google.jetpackcamera.settings.VideoQuality as VideoQualityProto

enum class VideoQuality {
    AUTO,
    LOWEST,
    HIGHEST,
    SD,
    HD,
    FHD,
    UHD;

    companion object {
        /** returns the VideoQuality enum equivalent of a provided VideoQualityProto */
        fun fromProto(videoQualityProto: VideoQualityProto): VideoQuality {
            return when (videoQualityProto) {
                VideoQualityProto.VIDEO_QUALITY_LOWEST -> LOWEST
                VideoQualityProto.VIDEO_QUALITY_HIGHEST -> HIGHEST
                VideoQualityProto.VIDEO_QUALITY_SD -> SD
                VideoQualityProto.VIDEO_QUALITY_HD -> HD
                VideoQualityProto.VIDEO_QUALITY_FHD -> FHD
                VideoQualityProto.VIDEO_QUALITY_UHD -> UHD
                VideoQualityProto.VIDEO_QUALITY_AUTO,
                VideoQualityProto.UNRECOGNIZED
                -> AUTO
            }
        }

        fun VideoQuality.toProto(): com.google.jetpackcamera.settings.VideoQuality {
            return when (this) {
                AUTO -> VideoQualityProto.VIDEO_QUALITY_AUTO
                LOWEST -> VideoQualityProto.VIDEO_QUALITY_LOWEST
                HIGHEST -> VideoQualityProto.VIDEO_QUALITY_HIGHEST
                SD -> VideoQualityProto.VIDEO_QUALITY_SD
                HD -> VideoQualityProto.VIDEO_QUALITY_HD
                FHD -> VideoQualityProto.VIDEO_QUALITY_FHD
                UHD -> VideoQualityProto.VIDEO_QUALITY_UHD
            }
        }
    }
}