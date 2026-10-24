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

class MirrorModeTest {

    @Test
    fun off_appliesToNoLens() {
        assertThat(MirrorMode.OFF.appliesTo(LensFacing.BACK)).isFalse()
        assertThat(MirrorMode.OFF.appliesTo(LensFacing.FRONT)).isFalse()
    }

    @Test
    fun on_appliesToAllLenses() {
        assertThat(MirrorMode.ON.appliesTo(LensFacing.BACK)).isTrue()
        assertThat(MirrorMode.ON.appliesTo(LensFacing.FRONT)).isTrue()
    }

    @Test
    fun onFrontOnly_appliesToFrontLensOnly() {
        assertThat(MirrorMode.ON_FRONT_ONLY.appliesTo(LensFacing.BACK)).isFalse()
        assertThat(MirrorMode.ON_FRONT_ONLY.appliesTo(LensFacing.FRONT)).isTrue()
    }
}
