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
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.model.ZoomStrategy
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeZoomControllerTest {
    @Test
    fun setZoomRatio_invokesAction() {
        var calledValue: CameraZoomRatio? = null
        val controller = FakeZoomController(setZoomRatioAction = { calledValue = it })
        val ratio = CameraZoomRatio(ZoomStrategy.Absolute(1f))
        controller.setZoomRatio(ratio)
        assertThat(calledValue).isEqualTo(ratio)
    }

    @Test
    fun setZoomAnimationState_invokesAction() {
        var calledValue: Float? = null
        val controller = FakeZoomController(setZoomAnimationStateAction = { calledValue = it })
        controller.setZoomAnimationState(2.5f)
        assertThat(calledValue).isEqualTo(2.5f)
    }
}
