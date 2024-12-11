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

import androidx.camera.video.Quality


enum class VideoQuality {
    DEFAULT,
    LOWEST,
    HIGHEST,
    SD,
    HD,
    FHD,
    UHD;

    companion object {
        fun toQuality(videoQuality: VideoQuality): Quality? {
            return when (videoQuality) {
                LOWEST -> Quality.LOWEST
                HIGHEST -> Quality.HIGHEST
                SD -> Quality.SD
                HD -> Quality.HD
                FHD -> Quality.FHD
                UHD -> Quality.UHD
                else -> null
            }
        }

        fun fromQuality(quality: Quality): VideoQuality {
            return when (quality) {
                Quality.LOWEST -> LOWEST
                Quality.HIGHEST -> HIGHEST
                Quality.SD -> SD
                Quality.HD -> HD
                Quality.FHD -> FHD
                Quality.UHD -> UHD
                else -> DEFAULT
            }
        }

        /** returns the VideoQuality enum equivalent of a provided VideoQualityProto */
        fun fromProto(aspectRatioProto: com.google.jetpackcamera.settings.VideoQuality): VideoQuality {
            return when (aspectRatioProto) {
                com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_LOWEST -> LOWEST
                com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_HIGHEST -> HIGHEST
                com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_SD -> SD
                com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_HD -> HD
                com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_FHD -> FHD
                com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_UHD -> UHD
                com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_DEFAULT,
                com.google.jetpackcamera.settings.VideoQuality.UNRECOGNIZED,
                    -> DEFAULT
            }
        }

        fun VideoQuality.toProto(): com.google.jetpackcamera.settings.VideoQuality {
            return when (this) {
                DEFAULT -> com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_DEFAULT
                LOWEST -> com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_LOWEST
                HIGHEST -> com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_HIGHEST
                SD -> com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_SD
                HD -> com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_HD
                FHD -> com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_FHD
                UHD -> com.google.jetpackcamera.settings.VideoQuality.VIDEO_QUALITY_UHD
            }
        }
    }
}