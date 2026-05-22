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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.ui.uistateadapter.capture.compound.roundVideoRecordingState
import java.util.concurrent.TimeUnit
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CaptureUiStateAdapterTest {

    @Test
    fun roundVideoRecordingState_nanoseconds_noRounding() {
        val state = VideoRecordingState.Active.Recording(0L, 0.0, 1234567890L)
        val rounded = roundVideoRecordingState(state, TimeUnit.NANOSECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1234567890L)
    }

    @Test
    fun roundVideoRecordingState_milliseconds_roundsToMillis() {
        val state = VideoRecordingState.Active.Recording(0L, 0.0, 1234567890L)
        val rounded = roundVideoRecordingState(state, TimeUnit.MILLISECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1234000000L)
    }

    @Test
    fun roundVideoRecordingState_seconds_roundsToSeconds() {
        val state = VideoRecordingState.Active.Recording(0L, 0.0, 1234567890L)
        val rounded = roundVideoRecordingState(state, TimeUnit.SECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1000000000L)
    }
}
