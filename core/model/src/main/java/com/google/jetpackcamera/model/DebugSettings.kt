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

import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.TestPattern

/**
 * Data class for defining settings used in debug flows within the app.
 *
 * @param isDebugModeEnabled Controls whether the debug mode UI is shown.
 *                         Acceptable values are `true` or `false`.
 * @param singleLensMode Configures the camera to only use a single lens on the device,
 *                       making it appear as if no other lenses are present.
 *                       The provided [LensFacing] determines which lens will be used.
 *                       If `null`, single lens mode is disabled.
 */
data class DebugSettings(
    val isDebugModeEnabled: Boolean = false,
    val singleLensMode: LensFacing? = null,
    val testPattern: TestPattern = TestPattern.Off
) {
    companion object {
        /**
         * Parses the string into a [DebugSettings] instance.
         */
        fun parseFromString(value: String): DebugSettings {
            val parts = value.split(";")
            var isDebugModeEnabled = false
            var singleLensMode: LensFacing? = null
            var testPattern: TestPattern = TestPattern.Off

            for (part in parts) {
                val kv = part.split(":")
                if (kv.size == 2) {
                    when (kv[0]) {
                        "debug" -> isDebugModeEnabled = kv[1].toBoolean()
                        "lens" -> singleLensMode = if (kv[1].isEmpty()) null else LensFacing.valueOf(kv[1])
                        "pattern" -> {
                            testPattern = when (kv[1]) {
                                "Off" -> TestPattern.Off
                                "ColorBars" -> TestPattern.ColorBars
                                "ColorBarsFadeToGray" -> TestPattern.ColorBarsFadeToGray
                                "PN9" -> TestPattern.PN9
                                "Custom1" -> TestPattern.Custom1
                                else -> {
                                    if (kv[1].startsWith("SolidColor")) {
                                        val channels = kv[1].removePrefix("SolidColor(").removeSuffix(")").split(",")
                                        TestPattern.SolidColor(
                                            red = channels[0].toUInt(),
                                            greenEven = channels[1].toUInt(),
                                            greenOdd = channels[2].toUInt(),
                                            blue = channels[3].toUInt()
                                        )
                                    } else {
                                        TestPattern.Off
                                    }
                                }
                            }
                        }
                    }
                }
            }
            return DebugSettings(isDebugModeEnabled, singleLensMode, testPattern)
        }

        /**
         * Encodes the [DebugSettings] data class to a string.
         */
        fun DebugSettings.encodeAsString(): String {
            val lensStr = singleLensMode?.name ?: ""
            val patternStr = when (val pattern = testPattern) {
                is TestPattern.SolidColor -> "SolidColor(${pattern.red},${pattern.greenEven},${pattern.greenOdd},${pattern.blue})"
                else -> pattern.toString()
            }
            return "debug:$isDebugModeEnabled;lens:$lensStr;pattern:$patternStr"
        }
    }
}
