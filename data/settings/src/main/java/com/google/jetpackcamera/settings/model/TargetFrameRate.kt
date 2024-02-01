/*
 * Copyright (C) 2023 The Android Open Source Project
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

import android.util.Range
import com.google.jetpackcamera.settings.TargetFrameRate as TargetFrameRateProto

enum class TargetFrameRate(val range: Range<Int>) {
    TARGET_FPS_NONE(Range(0, 0)),
    TARGET_FPS_15(Range(15, 15)),
    TARGET_FPS_30(Range(30, 30)),
    TARGET_FPS_60(Range(60, 60));

    // converts our kotlin enum to a proto enum
    fun toProto(): TargetFrameRateProto = when (this) {
        TARGET_FPS_NONE -> TargetFrameRateProto.TARGET_FRAME_RATE_NONE
        TARGET_FPS_15 -> TargetFrameRateProto.TARGET_FRAME_RATE_15
        TARGET_FPS_30 -> TargetFrameRateProto.TARGET_FRAME_RATE_30
        TARGET_FPS_60 -> TargetFrameRateProto.TARGET_FRAME_RATE_60
    }

    companion object {
        // converts proto enum to our kotlin enum
        fun fromProto(targetFrameRate: TargetFrameRateProto): TargetFrameRate {
            return when (targetFrameRate) {
                TargetFrameRateProto.TARGET_FRAME_RATE_NONE -> TARGET_FPS_NONE
                TargetFrameRateProto.TARGET_FRAME_RATE_15 -> TARGET_FPS_15
                TargetFrameRateProto.TARGET_FRAME_RATE_30 -> TARGET_FPS_30
                TargetFrameRateProto.TARGET_FRAME_RATE_60 -> TARGET_FPS_60
                else -> TARGET_FPS_NONE
            }
        }
    }
}
