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

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

@RunWith(Parameterized::class)
class AspectRatioTest(
    private val aspectRatio: AspectRatio,
    private val expectedFloat: Float,
    private val expectedLandscapeFloat: Float
) {
    companion object {
        @JvmStatic
        @Parameters(name = "{0}")
        fun data(): Collection<Array<Any>> = listOf(
            arrayOf(AspectRatio.THREE_FOUR, 3f / 4f, 4f / 3f),
            arrayOf(AspectRatio.NINE_SIXTEEN, 9f / 16f, 16f / 9f),
            arrayOf(AspectRatio.ONE_ONE, 1f / 1f, 1f / 1f)
        )
    }

    @Test
    fun toFloat_returnsCorrectFloat() {
        val floatValue = aspectRatio.toFloat()
        assertThat(floatValue).isEqualTo(expectedFloat)
    }

    @Test
    fun toLandscapeFloat_returnsCorrectFloat() {
        val landscapeFloatValue = aspectRatio.toLandscapeFloat()
        assertThat(landscapeFloatValue).isEqualTo(expectedLandscapeFloat)
    }
}
