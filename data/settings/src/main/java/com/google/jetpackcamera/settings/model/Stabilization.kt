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

import com.google.jetpackcamera.settings.PreviewStabilization as PreviewStabilizationProto
import com.google.jetpackcamera.settings.VideoStabilization as VideoStabilizationProto

enum class Stabilization {
    UNDEFINED,
    OFF,
    ON;

    companion object {
        /** returns the Stabilization enum equivalent of a provided [PreviewStabilizationProto]. */
        fun fromProto(stabilizationProto: PreviewStabilizationProto): Stabilization {
            return when (stabilizationProto) {
                PreviewStabilizationProto.PREVIEW_STABILIZATION_UNDEFINED -> UNDEFINED
                PreviewStabilizationProto.PREVIEW_STABILIZATION_OFF -> OFF
                PreviewStabilizationProto.PREVIEW_STABILIZATION_ON -> ON
                else -> UNDEFINED
            }
        }

        /** returns the Stabilization enum equivalent of a provided [VideoStabilizationProto]. */

        fun fromProto(stabilizationProto: VideoStabilizationProto): Stabilization {
            return when (stabilizationProto) {
                VideoStabilizationProto.VIDEO_STABILIZATION_UNDEFINED -> UNDEFINED
                VideoStabilizationProto.VIDEO_STABILIZATION_OFF -> OFF
                VideoStabilizationProto.VIDEO_STABILIZATION_ON -> ON
                else -> UNDEFINED
            }
        }
    }
}
