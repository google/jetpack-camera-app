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
import com.google.jetpackcamera.model.proto.AspectRatio as AspectRatioProto

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.AspectRatio.NINE_SIXTEEN
import com.google.jetpackcamera.model.AspectRatio.ONE_ONE
import com.google.jetpackcamera.model.AspectRatio.THREE_FOUR


    /** returns the AspectRatio enum equivalent of a provided AspectRatioProto */
    fun AspectRatioProto.toDomain(): AspectRatio {
        return when (this) {
            AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN -> NINE_SIXTEEN
            AspectRatioProto.ASPECT_RATIO_ONE_ONE -> ONE_ONE

            // defaults to 3:4 aspect ratio
            AspectRatioProto.ASPECT_RATIO_THREE_FOUR,
            AspectRatioProto.ASPECT_RATIO_UNDEFINED,
            AspectRatioProto.UNRECOGNIZED
                -> THREE_FOUR
        }
    }
