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
import com.google.jetpackcamera.core.camera.StillCaptureProgress
import com.google.jetpackcamera.ui.uistate.capture.CaptureProgressUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureProgressUiStateAdapterTest {

    @Test
    fun from_withoutProgress_isIndeterminate() {
        assertThat(CaptureProgressUiState.from(null))
            .isEqualTo(CaptureProgressUiState.Indeterminate)
    }

    @Test
    fun from_startedWithoutEstimate_isIndeterminate() {
        val progress = StillCaptureProgress(startedAtUptimeMillis = 100L)

        assertThat(CaptureProgressUiState.from(progress))
            .isEqualTo(CaptureProgressUiState.Indeterminate)
    }

    @Test
    fun from_startedWithEstimate_isEstimated() {
        val progress = StillCaptureProgress(
            startedAtUptimeMillis = 100L,
            captureLatencyMillis = 4_000L
        )

        assertThat(CaptureProgressUiState.from(progress)).isEqualTo(
            CaptureProgressUiState.Estimated(startedAtUptimeMillis = 100L, durationMillis = 4_000L)
        )
    }

    @Test
    fun from_withNonPositiveEstimate_isIndeterminate() {
        val progress = StillCaptureProgress(startedAtUptimeMillis = 100L, captureLatencyMillis = 0L)

        assertThat(CaptureProgressUiState.from(progress))
            .isEqualTo(CaptureProgressUiState.Indeterminate)
    }

    @Test
    fun from_withProcessProgress_isDeterminateAndReplacesEstimate() {
        val progress = StillCaptureProgress(
            startedAtUptimeMillis = 100L,
            captureLatencyMillis = 4_000L,
            processProgressPercent = 40
        )

        assertThat(CaptureProgressUiState.from(progress))
            .isEqualTo(CaptureProgressUiState.Determinate(0.4f))
    }

    @Test
    fun from_withCompleteProcessProgress_isDeterminateComplete() {
        val progress = StillCaptureProgress(
            startedAtUptimeMillis = 100L,
            processProgressPercent = 100
        )

        assertThat(CaptureProgressUiState.from(progress))
            .isEqualTo(CaptureProgressUiState.Determinate(1f))
    }
}
