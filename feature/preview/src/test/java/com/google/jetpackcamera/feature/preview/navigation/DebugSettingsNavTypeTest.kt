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
package com.google.jetpackcamera.feature.preview.navigation

import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.TestPattern
import org.junit.Assert.assertEquals
import org.junit.Test

class DebugSettingsNavTypeTest {

    @Test
    fun serializeAndParse_roundTrip_default() {
        val original = DebugSettings()
        val serialized = DebugSettingsNavType.serializeAsValue(original)
        val parsed = DebugSettingsNavType.parseValue(serialized)
        assertEquals(original, parsed)
    }

    @Test
    fun serializeAndParse_roundTrip_complex() {
        val original = DebugSettings(
            isDebugModeEnabled = true,
            singleLensMode = LensFacing.FRONT,
            testPattern = TestPattern.SolidColor(255u, 128u, 64u, 32u)
        )
        val serialized = DebugSettingsNavType.serializeAsValue(original)
        val parsed = DebugSettingsNavType.parseValue(serialized)
        assertEquals(original, parsed)
    }

    @Test
    fun serialize_isUrlSafe() {
        // URL-unsafe characters like ';', ':', '(', ')', ',' should be encoded
        val original = DebugSettings(
            isDebugModeEnabled = true,
            singleLensMode = LensFacing.BACK,
            testPattern = TestPattern.SolidColor(1u, 2u, 3u, 4u)
        )
        val serialized = DebugSettingsNavType.serializeAsValue(original)

        // None of these special characters should be present in the raw serialized string
        val unsafeChars = listOf(";", ":", "(", ")", ",")
        for (char in unsafeChars) {
            assert(!serialized.contains(char)) {
                "Serialized string '$serialized' contains unsafe URL character '$char'"
            }
        }
    }
}
