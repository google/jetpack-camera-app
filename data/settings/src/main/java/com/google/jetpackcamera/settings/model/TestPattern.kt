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
package com.google.jetpackcamera.settings.model

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest

/**
 * Represents a test pattern to replace sensor pixel data.
 *
 * This sealed interface defines various test patterns that can be displayed by the camera.
 * The behavior of these patterns is generally aligned with the definitions in
 * [CaptureRequest.SENSOR_TEST_PATTERN_MODE].
 *
 * This interface can be used to provide recognizable test patterns for debugging and testing
 * camera output. Note that the test pattern is applied before any other ISP operations, so
 * the appearance of final camera output may differ from the description of each test pattern.
 */
sealed interface TestPattern {
    /**
     * No test pattern is applied; the sensor's normal output is used.
     * @see CaptureRequest.SENSOR_TEST_PATTERN_MODE_OFF
     */
    object Off : TestPattern

    /**
     * Displays color bars.
     * @see CameraMetadata.SENSOR_TEST_PATTERN_MODE_COLOR_BARS
     */
    object ColorBars : TestPattern

    /**
     * Displays color bars that fade to gray.
     * @see CameraMetadata.SENSOR_TEST_PATTERN_MODE_COLOR_BARS_FADE_TO_GRAY
     */
    object ColorBarsFadeToGray : TestPattern

    /**
     * Displays a PN9 sequence.
     * @see CameraMetadata.SENSOR_TEST_PATTERN_MODE_PN9
     */
    object PN9 : TestPattern

    /**
     * Displays a custom test pattern. The specifics of this pattern are
     * implementation-dependent.
     * @see CameraMetadata.SENSOR_TEST_PATTERN_MODE_CUSTOM1
     */
    object Custom1 : TestPattern

    /**
     * Displays a solid color as the test pattern.
     * The color is defined by the red, green (even/odd lines), and blue channel values.
     * This pattern is similar in concept to `SENSOR_TEST_PATTERN_MODE_SOLID_COLOR`
     * but provides more granular control over the color components.
     * @see CameraMetadata.SENSOR_TEST_PATTERN_MODE_SOLID_COLOR
     *
     * @param red The value for the red channel.
     * @param greenEven The value for the green channel on even-numbered rows.
     * @param greenOdd The value for the green channel on odd-numbered rows.
     * @param blue The value for the blue channel.
     */
    data class SolidColor(
        val red: UInt,
        val greenEven: UInt,
        val greenOdd: UInt,
        val blue: UInt
    ) : TestPattern {
        companion object {
            /** A solid red test pattern. */
            val RED = SolidColor(
                red = UInt.MAX_VALUE,
                greenEven = UInt.MIN_VALUE,
                greenOdd = UInt.MIN_VALUE,
                blue = UInt.MIN_VALUE
            )

            /** A solid green test pattern. */
            val GREEN = SolidColor(
                red = UInt.MIN_VALUE,
                greenEven = UInt.MAX_VALUE,
                greenOdd = UInt.MAX_VALUE,
                blue = UInt.MIN_VALUE
            )

            /** A solid blue test pattern. */
            val BLUE = SolidColor(
                red = UInt.MIN_VALUE,
                greenEven = UInt.MIN_VALUE,
                greenOdd = UInt.MIN_VALUE,
                blue = UInt.MAX_VALUE
            )

            /** A solid white test pattern (100% intensity). */
            val WHITE = SolidColor(
                red = UInt.MAX_VALUE,
                greenEven = UInt.MAX_VALUE,
                greenOdd = UInt.MAX_VALUE,
                blue = UInt.MAX_VALUE
            )

            /** A solid black test pattern (0% intensity). */
            val BLACK = SolidColor(
                red = UInt.MIN_VALUE,
                greenEven = UInt.MIN_VALUE,
                greenOdd = UInt.MIN_VALUE,
                blue = UInt.MIN_VALUE
            )

            /** A solid gray test pattern (50% intensity). */
            val GRAY = SolidColor(
                red = UInt.MAX_VALUE / 2u,
                greenEven = UInt.MAX_VALUE / 2u,
                greenOdd = UInt.MAX_VALUE / 2u,
                blue = UInt.MAX_VALUE / 2u
            )

            /** A solid dark gray test pattern (25% intensity). */
            val DARK_GRAY = SolidColor(
                red = UInt.MAX_VALUE / 4u,
                greenEven = UInt.MAX_VALUE / 4u,
                greenOdd = UInt.MAX_VALUE / 4u,
                blue = UInt.MAX_VALUE / 4u
            )
        }
    }
}
