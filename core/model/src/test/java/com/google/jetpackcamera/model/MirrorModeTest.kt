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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class MirrorModeTest(
    private val mirrorMode: MirrorMode,
    private val lensFacing: LensFacing,
    private val expectedMirrored: Boolean
) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0} with {1} lens mirrors: {2}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(MirrorMode.OFF, LensFacing.BACK, false),
            arrayOf(MirrorMode.OFF, LensFacing.FRONT, false),
            arrayOf(MirrorMode.ON, LensFacing.BACK, true),
            arrayOf(MirrorMode.ON, LensFacing.FRONT, true),
            arrayOf(MirrorMode.ON_FRONT_ONLY, LensFacing.BACK, false),
            arrayOf(MirrorMode.ON_FRONT_ONLY, LensFacing.FRONT, true)
        )
    }

    @Test
    fun appliesTo_returnsWhetherCapturesAreMirrored() {
        assertThat(mirrorMode.appliesTo(lensFacing)).isEqualTo(expectedMirrored)
    }
}
