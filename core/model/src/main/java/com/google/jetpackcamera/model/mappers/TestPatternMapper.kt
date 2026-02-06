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

import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.TestPattern.ColorBars
import com.google.jetpackcamera.model.TestPattern.ColorBarsFadeToGray
import com.google.jetpackcamera.model.TestPattern.Custom1
import com.google.jetpackcamera.model.TestPattern.Off
import com.google.jetpackcamera.model.TestPattern.PN9
import com.google.jetpackcamera.model.TestPattern.SolidColor

import com.google.jetpackcamera.model.proto.TestPattern as ProtoTestPattern
import com.google.jetpackcamera.model.proto.TestPattern.PatternCase
import com.google.jetpackcamera.model.proto.testPattern as protoTestPattern
import com.google.jetpackcamera.model.proto.testPatternColorBars
import com.google.jetpackcamera.model.proto.testPatternColorBarsFadeToGray
import com.google.jetpackcamera.model.proto.testPatternCustom1
import com.google.jetpackcamera.model.proto.testPatternOff
import com.google.jetpackcamera.model.proto.testPatternPN9
import com.google.jetpackcamera.model.proto.testPatternSolidColor




/**
 * Converts a [TestPattern] sealed interface instance to its Protocol Buffer representation
 * ([ProtoTestPattern]).
 */
fun TestPattern.toProto(): ProtoTestPattern {
    return protoTestPattern {
        when (val pattern = this@toProto) {
            is Off -> off = testPatternOff {}
            is ColorBars -> colorBars = testPatternColorBars {}
            is ColorBarsFadeToGray ->
                colorBarsFadeToGray = testPatternColorBarsFadeToGray {}

            is PN9 -> pn9 = testPatternPN9 {}
            is Custom1 -> custom1 = testPatternCustom1 {}
            is SolidColor -> solidColor = testPatternSolidColor {
                red = pattern.red.toInt()
                greenEven = pattern.greenEven.toInt()
                greenOdd = pattern.greenOdd.toInt()
                blue = pattern.blue.toInt()
            }
        }
    }
}

/**
 * Converts a [ProtoTestPattern] Protocol Buffer message to its Kotlin [TestPattern] sealed
 * interface representation.
 */
fun ProtoTestPattern.fromProto(): TestPattern {
    return when (this.patternCase) {
        PatternCase.OFF,
        PatternCase.PATTERN_NOT_SET -> {
            // Default to Off if the oneof is not set
            Off
        }

        PatternCase.COLOR_BARS -> ColorBars
        PatternCase.COLOR_BARS_FADE_TO_GRAY -> ColorBarsFadeToGray
        PatternCase.PN9 -> PN9
        PatternCase.CUSTOM1 -> Custom1
        PatternCase.SOLID_COLOR -> {
            val protoSolidColor = this.solidColor
            SolidColor(
                red = protoSolidColor.red.toUInt(),
                greenEven = protoSolidColor.greenEven.toUInt(),
                greenOdd = protoSolidColor.greenOdd.toUInt(),
                blue = protoSolidColor.blue.toUInt()
            )
        }
    }
}
