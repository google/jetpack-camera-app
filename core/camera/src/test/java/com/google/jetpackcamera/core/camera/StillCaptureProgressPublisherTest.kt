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
package com.google.jetpackcamera.core.camera

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StillCaptureProgressPublisherTest {

    private val cameraState = MutableStateFlow(CameraState())
    private var now = START_UPTIME_MILLIS
    private var latencyEstimate: Long? = CAPTURE_LATENCY_MILLIS

    private fun newPublisher() = StillCaptureProgressPublisher(
        cameraState = cameraState,
        captureLatencyMillis = { latencyEstimate },
        uptimeMillis = { now }
    )

    @Test
    fun onCaptureStarted_publishesStartTimeAndLatencyEstimate() {
        val publisher = newPublisher()

        publisher.onCaptureStarted()

        assertThat(cameraState.value.stillCaptureProgress).isEqualTo(
            StillCaptureProgress(
                startedAtUptimeMillis = START_UPTIME_MILLIS,
                captureLatencyMillis = CAPTURE_LATENCY_MILLIS
            )
        )
    }

    @Test
    fun onCaptureStarted_withoutLatencyEstimate_publishesNullLatency() {
        latencyEstimate = null
        val publisher = newPublisher()

        publisher.onCaptureStarted()

        assertThat(cameraState.value.stillCaptureProgress?.captureLatencyMillis).isNull()
    }

    @Test
    fun onCaptureStarted_withZeroLatencyEstimate_publishesNullLatency() {
        latencyEstimate = 0L
        val publisher = newPublisher()

        publisher.onCaptureStarted()

        assertThat(cameraState.value.stillCaptureProgress?.captureLatencyMillis).isNull()
    }

    @Test
    fun onCaptureProcessProgressed_keepsStartTimeAndAddsProgress() {
        val publisher = newPublisher()
        publisher.onCaptureStarted()
        now += 500L

        publisher.onCaptureProcessProgressed(40)

        assertThat(cameraState.value.stillCaptureProgress).isEqualTo(
            StillCaptureProgress(
                startedAtUptimeMillis = START_UPTIME_MILLIS,
                captureLatencyMillis = CAPTURE_LATENCY_MILLIS,
                processProgressPercent = 40
            )
        )
    }

    @Test
    fun onCaptureProcessProgressed_beforeStarted_usesCurrentTimeAsStart() {
        val publisher = newPublisher()

        publisher.onCaptureProcessProgressed(10)

        assertThat(cameraState.value.stillCaptureProgress).isEqualTo(
            StillCaptureProgress(
                startedAtUptimeMillis = START_UPTIME_MILLIS,
                processProgressPercent = 10
            )
        )
    }

    @Test
    fun onCaptureProcessProgressed_clampsPercentToValidRange() {
        val publisher = newPublisher()
        publisher.onCaptureStarted()

        publisher.onCaptureProcessProgressed(150)

        assertThat(cameraState.value.stillCaptureProgress?.processProgressPercent).isEqualTo(100)
    }

    @Test
    fun onCaptureStarted_afterProcessProgress_keepsProgress() {
        val publisher = newPublisher()
        publisher.onCaptureProcessProgressed(20)
        now += 700L

        publisher.onCaptureStarted()

        assertThat(cameraState.value.stillCaptureProgress).isEqualTo(
            StillCaptureProgress(
                startedAtUptimeMillis = START_UPTIME_MILLIS + 700L,
                captureLatencyMillis = CAPTURE_LATENCY_MILLIS,
                processProgressPercent = 20
            )
        )
    }

    @Test
    fun onCaptureProcessProgressed_lowerThanPublished_isIgnored() {
        val publisher = newPublisher()
        publisher.onCaptureStarted()
        publisher.onCaptureProcessProgressed(60)

        publisher.onCaptureProcessProgressed(30)

        assertThat(cameraState.value.stillCaptureProgress?.processProgressPercent).isEqualTo(60)
    }

    @Test
    fun clear_removesPublishedProgress() {
        val publisher = newPublisher()
        publisher.onCaptureStarted()
        publisher.onCaptureProcessProgressed(100)

        publisher.clear()

        assertThat(cameraState.value.stillCaptureProgress).isNull()
    }

    @Test
    fun clear_afterNextCaptureStarted_keepsNextCaptureProgress() {
        val first = newPublisher()
        val second = newPublisher()
        first.onCaptureStarted()
        now += 1_000L
        second.onCaptureStarted()

        first.clear()

        assertThat(cameraState.value.stillCaptureProgress?.startedAtUptimeMillis)
            .isEqualTo(START_UPTIME_MILLIS + 1_000L)
    }

    @Test
    fun onCaptureProcessProgressed_afterNextCaptureStarted_doesNotReplaceNextCaptureProgress() {
        val first = newPublisher()
        val second = newPublisher()
        first.onCaptureStarted()
        now += 1_000L
        second.onCaptureStarted()

        first.onCaptureProcessProgressed(50)

        assertThat(cameraState.value.stillCaptureProgress?.processProgressPercent).isNull()
    }

    @Test
    fun clear_withoutPublishedProgress_leavesStateUnchanged() {
        val state = CameraState(inFlightCapturesCount = 1)
        cameraState.value = state

        newPublisher().clear()

        assertThat(cameraState.value).isSameInstanceAs(state)
    }

    private companion object {
        const val START_UPTIME_MILLIS = 1_000L
        const val CAPTURE_LATENCY_MILLIS = 3_000L
    }
}
