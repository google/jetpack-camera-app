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

import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.DynamicRange.HLG10
import com.google.jetpackcamera.model.DynamicRange.SDR
import com.google.jetpackcamera.model.proto.DynamicRange as DynamicRangeProto


/** returns the DynamicRangeType enum equivalent of a provided DynamicRangeTypeProto */
fun DynamicRangeProto.toDomain(): DynamicRange {
    return when (this) {
        DynamicRangeProto.DYNAMIC_RANGE_HLG10 -> HLG10

        // Treat unrecognized and unspecified as SDR as a fallback
        DynamicRangeProto.DYNAMIC_RANGE_SDR,
        DynamicRangeProto.DYNAMIC_RANGE_UNSPECIFIED,
        DynamicRangeProto.UNRECOGNIZED -> SDR
    }
}

fun DynamicRange.toProto(): DynamicRangeProto {
    return when (this) {
        SDR -> DynamicRangeProto.DYNAMIC_RANGE_SDR
        HLG10 -> DynamicRangeProto.DYNAMIC_RANGE_HLG10

    }
}