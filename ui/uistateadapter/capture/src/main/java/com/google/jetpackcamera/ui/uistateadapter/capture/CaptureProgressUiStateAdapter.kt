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

import com.google.jetpackcamera.core.camera.StillCaptureProgress
import com.google.jetpackcamera.ui.uistate.capture.CaptureProgressUiState

/**
 * Creates a [CaptureProgressUiState] from the progress that the camera reports for a capture.
 *
 * - Processing progress, once reported, is shown as [CaptureProgressUiState.Determinate].
 * - Otherwise, a capture latency estimate is shown as [CaptureProgressUiState.Estimated].
 * - Otherwise, including before the capture has started, the progress is
 *   [CaptureProgressUiState.Indeterminate].
 *
 * @param stillCaptureProgress The progress of the capture, or `null` if it has not started.
 */
fun CaptureProgressUiState.Companion.from(
    stillCaptureProgress: StillCaptureProgress?
): CaptureProgressUiState {
    if (stillCaptureProgress == null) return CaptureProgressUiState.Indeterminate
    val percent = stillCaptureProgress.processProgressPercent
    val latencyMillis = stillCaptureProgress.captureLatencyMillis
    return when {
        percent != null -> CaptureProgressUiState.Determinate(percent.coerceIn(0, 100) / 100f)
        latencyMillis != null && latencyMillis > 0 -> CaptureProgressUiState.Estimated(
            startedAtUptimeMillis = stillCaptureProgress.startedAtUptimeMillis,
            durationMillis = latencyMillis
        )
        else -> CaptureProgressUiState.Indeterminate
    }
}
