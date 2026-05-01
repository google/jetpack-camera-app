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
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FakeQuickSettingsControllerTest {
    @Test
    fun toggleQuickSettings_invokesAction() {
        var called = false
        val controller = FakeQuickSettingsController(toggleQuickSettingsAction = { called = true })
        controller.toggleQuickSettings()
        assertThat(called).isTrue()
    }

    @Test
    fun setFocusedSetting_invokesAction() {
        var calledValue: FocusedQuickSetting? = null
        val controller = FakeQuickSettingsController(setFocusedSettingAction = { calledValue = it })
        controller.setFocusedSetting(FocusedQuickSetting.ASPECT_RATIO)
        assertThat(calledValue).isEqualTo(FocusedQuickSetting.ASPECT_RATIO)
    }

    @Test
    fun setLensFacing_invokesAction() {
        var calledValue: LensFacing? = null
        val controller = FakeQuickSettingsController(setLensFacingAction = { calledValue = it })
        controller.setLensFacing(LensFacing.FRONT)
        assertThat(calledValue).isEqualTo(LensFacing.FRONT)
    }

    @Test
    fun setFlash_invokesAction() {
        var calledValue: FlashMode? = null
        val controller = FakeQuickSettingsController(setFlashAction = { calledValue = it })
        controller.setFlash(FlashMode.ON)
        assertThat(calledValue).isEqualTo(FlashMode.ON)
    }

    @Test
    fun setAspectRatio_invokesAction() {
        var calledValue: AspectRatio? = null
        val controller = FakeQuickSettingsController(setAspectRatioAction = { calledValue = it })
        controller.setAspectRatio(AspectRatio.THREE_FOUR)
        assertThat(calledValue).isEqualTo(AspectRatio.THREE_FOUR)
    }

    @Test
    fun setStreamConfig_invokesAction() {
        var calledValue: StreamConfig? = null
        val controller = FakeQuickSettingsController(setStreamConfigAction = { calledValue = it })
        controller.setStreamConfig(StreamConfig.SINGLE_STREAM)
        assertThat(calledValue).isEqualTo(StreamConfig.SINGLE_STREAM)
    }

    @Test
    fun setDynamicRange_invokesAction() {
        var calledValue: DynamicRange? = null
        val controller = FakeQuickSettingsController(setDynamicRangeAction = { calledValue = it })
        controller.setDynamicRange(DynamicRange.HLG10)
        assertThat(calledValue).isEqualTo(DynamicRange.HLG10)
    }

    @Test
    fun setImageFormat_invokesAction() {
        var calledValue: ImageOutputFormat? = null
        val controller = FakeQuickSettingsController(setImageFormatAction = { calledValue = it })
        controller.setImageFormat(ImageOutputFormat.JPEG)
        assertThat(calledValue).isEqualTo(ImageOutputFormat.JPEG)
    }

    @Test
    fun setConcurrentCameraMode_invokesAction() {
        var calledValue: ConcurrentCameraMode? = null
        val controller = FakeQuickSettingsController(
            setConcurrentCameraModeAction = { calledValue = it }
        )
        controller.setConcurrentCameraMode(ConcurrentCameraMode.DUAL)
        assertThat(calledValue).isEqualTo(ConcurrentCameraMode.DUAL)
    }

    @Test
    fun setCaptureMode_invokesAction() {
        var calledValue: CaptureMode? = null
        val controller = FakeQuickSettingsController(setCaptureModeAction = { calledValue = it })
        controller.setCaptureMode(CaptureMode.STANDARD)
        assertThat(calledValue).isEqualTo(CaptureMode.STANDARD)
    }
}
