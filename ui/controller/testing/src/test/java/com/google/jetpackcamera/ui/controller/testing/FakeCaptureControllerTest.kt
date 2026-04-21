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

import android.content.ContentResolver
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureEvent
import kotlinx.coroutines.channels.Channel
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class FakeCaptureControllerTest {
    @Test
    fun captureEvents_returnsProvidedChannel() {
        val channel = Channel<CaptureEvent>()
        val controller = FakeCaptureController(captureEvents = channel)
        assertThat(controller.captureEvents).isEqualTo(channel)
    }

    @Test
    fun captureImage_invokesAction() {
        var calledResolver: ContentResolver? = null
        val controller = FakeCaptureController(captureImageAction = { calledResolver = it })
        val resolver = ApplicationProvider.getApplicationContext<Context>().contentResolver
        controller.captureImage(resolver)
        assertThat(calledResolver).isEqualTo(resolver)
    }

    @Test
    fun startVideoRecording_invokesAction() {
        var called = false
        val controller = FakeCaptureController(startVideoRecordingAction = { called = true })
        controller.startVideoRecording()
        assertThat(called).isTrue()
    }

    @Test
    fun stopVideoRecording_invokesAction() {
        var called = false
        val controller = FakeCaptureController(stopVideoRecordingAction = { called = true })
        controller.stopVideoRecording()
        assertThat(called).isTrue()
    }

    @Test
    fun setLockedRecording_invokesAction() {
        var calledValue: Boolean? = null
        val controller = FakeCaptureController(setLockedRecordingAction = { calledValue = it })
        controller.setLockedRecording(true)
        assertThat(calledValue).isTrue()
    }

    @Test
    fun setPaused_invokesAction() {
        var calledValue: Boolean? = null
        val controller = FakeCaptureController(setPausedAction = { calledValue = it })
        controller.setPaused(true)
        assertThat(calledValue).isTrue()
    }

    @Test
    fun setAudioEnabled_invokesAction() {
        var calledValue: Boolean? = null
        val controller = FakeCaptureController(setAudioEnabledAction = { calledValue = it })
        controller.setAudioEnabled(true)
        assertThat(calledValue).isTrue()
    }
}
