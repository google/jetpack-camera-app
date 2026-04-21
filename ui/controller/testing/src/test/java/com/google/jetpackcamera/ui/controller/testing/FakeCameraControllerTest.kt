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
import com.google.jetpackcamera.model.DeviceRotation
import org.junit.Test

class FakeCameraControllerTest {
    @Test
    fun startCamera_invokesAction() {
        var called = false
        val controller = FakeCameraController(startCameraAction = { called = true })
        controller.startCamera()
        assertThat(called).isTrue()
    }

    @Test
    fun stopCamera_invokesAction() {
        var called = false
        val controller = FakeCameraController(stopCameraAction = { called = true })
        controller.stopCamera()
        assertThat(called).isTrue()
    }

    @Test
    fun tapToFocus_invokesAction() {
        var calledCoords: Pair<Float, Float>? = null
        val controller = FakeCameraController(tapToFocusAction = { x, y -> calledCoords = x to y })
        controller.tapToFocus(1f, 2f)
        assertThat(calledCoords).isEqualTo(1f to 2f)
    }

    @Test
    fun setDisplayRotation_invokesAction() {
        var calledRotation: DeviceRotation? = null
        val controller = FakeCameraController(
            setDisplayRotationAction = { calledRotation = it }
        )
        controller.setDisplayRotation(DeviceRotation.Rotated90)
        assertThat(calledRotation).isEqualTo(DeviceRotation.Rotated90)
    }
}
