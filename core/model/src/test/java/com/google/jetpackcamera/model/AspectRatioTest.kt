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

class AspectRatioTest {
    @Test
    fun toFloat_returnsCorrectFloat() {
        val aspectRatio = AspectRatio.THREE_FOUR
        val floatValue = aspectRatio.toFloat()
        assertThat(floatValue).isEqualTo(3f / 4f)
    }

    @Test
    fun toLandscapeFloat_returnsCorrectFloat() {
        val aspectRatio = AspectRatio.THREE_FOUR
        val landscapeFloatValue = aspectRatio.toLandscapeFloat()
        assertThat(landscapeFloatValue).isEqualTo(4f / 3f)
    }
}
