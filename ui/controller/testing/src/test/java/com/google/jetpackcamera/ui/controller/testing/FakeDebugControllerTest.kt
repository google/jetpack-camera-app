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
package com.google.jetpackcamera.ui.controller.testing

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.TestPattern
import org.junit.Test

class FakeDebugControllerTest {
    @Test
    fun toggleDebugHidingComponents_invokesAction() {
        var called = false
        val controller = FakeDebugController(toggleDebugHidingComponentsAction = { called = true })
        controller.toggleDebugHidingComponents()
        assertThat(called).isTrue()
    }

    @Test
    fun toggleDebugOverlay_invokesAction() {
        var called = false
        val controller = FakeDebugController(toggleDebugOverlayAction = { called = true })
        controller.toggleDebugOverlay()
        assertThat(called).isTrue()
    }

    @Test
    fun setTestPattern_invokesAction() {
        var calledPattern: TestPattern? = null
        val controller = FakeDebugController(setTestPatternAction = { calledPattern = it })
        controller.setTestPattern(TestPattern.ColorBars)
        assertThat(calledPattern).isEqualTo(TestPattern.ColorBars)
    }
}
