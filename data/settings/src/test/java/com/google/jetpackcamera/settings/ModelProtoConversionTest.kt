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
package com.google.jetpackcamera.settings

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.DebugSettings.Companion.encodeAsByteArray
import com.google.jetpackcamera.model.DebugSettings.Companion.encodeAsString
import com.google.jetpackcamera.model.DebugSettings.Companion.toProto
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.model.TestPattern.Companion.toProto
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class ModelProtoConversionTest {

    // --- TestPattern ---
    @Test
    fun testPattern_convertsToCorrectProto() {
        val patterns = listOf(
            TestPattern.Off,
            TestPattern.ColorBars,
            TestPattern.ColorBarsFadeToGray,
            TestPattern.PN9,
            TestPattern.Custom1,
            TestPattern.SolidColor(1u, 2u, 3u, 4u)
        )
        patterns.forEach { pattern ->
            val proto = pattern.toProto()
            val domain = TestPattern.fromProto(proto)
            assertThat(domain).isEqualTo(pattern)
        }
    }

    // --- DebugSettings ---
    @Test
    fun debugSettings_convertsToCorrectProto() {
        val settings = listOf(
            DebugSettings(),
            DebugSettings(isDebugModeEnabled = true),
            DebugSettings(singleLensMode = LensFacing.BACK),
            DebugSettings(singleLensMode = LensFacing.FRONT),
            DebugSettings(testPattern = TestPattern.ColorBars),
            DebugSettings(
                isDebugModeEnabled = true,
                singleLensMode = LensFacing.FRONT,
                testPattern = TestPattern.SolidColor(255u, 0u, 0u, 255u)
            )
        )
        settings.forEach { setting ->
            val proto = setting.toProto()
            val domain = DebugSettings.fromProto(proto)
            assertThat(domain).isEqualTo(setting)
        }
    }

    @Test
    fun debugSettings_encodesAndDecodesCorrectly() {
        val setting = DebugSettings(
            isDebugModeEnabled = true,
            singleLensMode = LensFacing.BACK,
            testPattern = TestPattern.ColorBars
        )

        // Byte Array Serialization
        val bytes = setting.encodeAsByteArray()
        val decodedFromBytes = DebugSettings.parseFromByteArray(bytes)
        assertThat(decodedFromBytes).isEqualTo(setting)

        // Base64 String Serialization (used in Navigation Routes)
        val string = setting.encodeAsString()
        val decodedFromString = DebugSettings.parseFromString(string)
        assertThat(decodedFromString).isEqualTo(setting)
    }
}
