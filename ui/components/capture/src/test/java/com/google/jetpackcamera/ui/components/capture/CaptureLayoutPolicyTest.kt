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
package com.google.jetpackcamera.ui.components.capture

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CaptureLayoutPolicyTest {

    @Test
    fun shouldOverlayTopBar_threeFour_isFalse() {
        assertThat(shouldOverlayTopBar(AspectRatio.THREE_FOUR)).isFalse()
    }

    @Test
    fun shouldOverlayTopBar_oneOne_isFalse() {
        assertThat(shouldOverlayTopBar(AspectRatio.ONE_ONE)).isFalse()
    }

    @Test
    fun shouldOverlayTopBar_nineSixteen_isTrueByDefault() {
        assertThat(shouldOverlayTopBar(AspectRatio.NINE_SIXTEEN)).isTrue()
    }

    @Test
    fun shouldOverlayTopBar_nineSixteen_respectsFullBleedFlag() {
        assertThat(shouldOverlayTopBar(AspectRatio.NINE_SIXTEEN, isFullBleedEnabled = false))
            .isFalse()
    }

    @Test
    fun shouldOverlayTopBar_nullAspectRatio_isFalse() {
        assertThat(shouldOverlayTopBar(null)).isFalse()
    }
}
