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
package com.google.jetpackcamera.model

import android.util.Rational
import com.google.jetpackcamera.model.proto.AspectRatio as AspectRatioProto

enum class AspectRatio(val ratio: Rational) {
    THREE_FOUR(Rational(3, 4)),
    NINE_SIXTEEN(Rational(9, 16)),
    ONE_ONE(Rational(1, 1));

    val landscapeRatio: Rational by lazy {
        Rational(ratio.denominator, ratio.numerator)
    }

    companion object {

        /** returns the AspectRatio enum equivalent of a provided AspectRatioProto */
        fun fromProto(aspectRatioProto: AspectRatioProto): AspectRatio {
            return when (aspectRatioProto) {
                AspectRatioProto.ASPECT_RATIO_NINE_SIXTEEN -> NINE_SIXTEEN
                AspectRatioProto.ASPECT_RATIO_ONE_ONE -> ONE_ONE

                // defaults to 3:4 aspect ratio
                AspectRatioProto.ASPECT_RATIO_THREE_FOUR,
                AspectRatioProto.ASPECT_RATIO_UNDEFINED,
                AspectRatioProto.UNRECOGNIZED
                -> THREE_FOUR
            }
        }
    }
}
