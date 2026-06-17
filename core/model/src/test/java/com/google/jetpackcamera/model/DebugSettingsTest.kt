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
package com.google.jetpackcamera.model

import com.google.jetpackcamera.model.DebugSettings.Companion.encodeAsString
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugSettingsTest {

    @Test
    fun roundTripSerialization_defaultSettings() {
        val original = DebugSettings()
        val encoded = original.encodeAsString()
        val parsed = DebugSettings.parseFromString(encoded)
        assertEquals(original, parsed)
    }

    @Test
    fun roundTripSerialization_allFieldsEnabled() {
        val original = DebugSettings(
            isDebugModeEnabled = true,
            singleLensMode = LensFacing.FRONT,
            testPattern = TestPattern.ColorBars
        )
        val encoded = original.encodeAsString()
        val parsed = DebugSettings.parseFromString(encoded)
        assertEquals(original, parsed)
    }

    @Test
    fun roundTripSerialization_solidColorPattern() {
        val original = DebugSettings(
            isDebugModeEnabled = true,
            singleLensMode = LensFacing.BACK,
            testPattern = TestPattern.SolidColor(10u, 20u, 30u, 40u)
        )
        val encoded = original.encodeAsString()
        val parsed = DebugSettings.parseFromString(encoded)
        assertEquals(original, parsed)
    }

    @Test
    fun parseFromString_malformedString_returnsDefaultOrSafeValues() {
        // Empty string should fall back to defaults safely
        val parsedEmpty = DebugSettings.parseFromString("")
        assertEquals(DebugSettings(false, null, TestPattern.Off), parsedEmpty)

        // Invalid pattern name should fall back to TestPattern.Off
        val parsedInvalidPattern = DebugSettings.parseFromString(
            "debug:true;lens:BACK;pattern:InvalidPattern"
        )
        assertEquals(DebugSettings(true, LensFacing.BACK, TestPattern.Off), parsedInvalidPattern)

        // Malformed SolidColor (missing channel) should fall back to TestPattern.Off
        val parsedInvalidSolidColor = DebugSettings.parseFromString(
            "debug:true;lens:BACK;pattern:SolidColor(1,2,3)"
        )
        assertEquals(DebugSettings(true, LensFacing.BACK, TestPattern.Off), parsedInvalidSolidColor)
    }
}
