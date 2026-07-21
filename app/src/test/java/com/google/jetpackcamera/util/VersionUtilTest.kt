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
package com.google.jetpackcamera.util

import org.junit.Assert.assertEquals
import org.junit.Test

class VersionUtilTest {

    @Test
    fun getDetailedLabels_allPropertiesPresent_returnsAll() {
        val details = VersionUtil.getDetailedLabels(
            buildOrigin = "Gradle",
            gitSha = "abcdef123123",
            changelist = "123456",
            soongBuildId = "eng.build.123"
        )

        assertEquals(4, details.size)
        assertEquals("Build Origin" to "Gradle", details[0])
        assertEquals("Git SHA" to "abcdef123123", details[1])
        assertEquals("Changelist" to "123456", details[2])
        assertEquals("Soong Build ID" to "eng.build.123", details[3])
    }

    @Test
    fun getDetailedLabels_missingProperties_returnsOnlyPresent() {
        // Gradle build where we only have Origin and Git SHA
        val details = VersionUtil.getDetailedLabels(
            buildOrigin = "Gradle",
            gitSha = "abcdef123123",
            changelist = "",
            soongBuildId = ""
        )

        assertEquals(2, details.size)
        assertEquals("Build Origin" to "Gradle", details[0])
        assertEquals("Git SHA" to "abcdef123123", details[1])
    }

    @Test
    fun getDetailedLabels_emptyStrings_dropFromList() {
        val details = VersionUtil.getDetailedLabels(
            buildOrigin = "",
            gitSha = "",
            changelist = "",
            soongBuildId = ""
        )
        assertEquals(0, details.size)
    }
}
