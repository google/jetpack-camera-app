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
package com.google.jetpackcamera.ui.uistate.capture

/**
 * How the capture button shows the progress of a capture in progress.
 */
sealed interface CaptureProgressUiState {
    /** The progress is unknown. The progress indicator spins continuously. */
    data object Indeterminate : CaptureProgressUiState

    /**
     * The capture has completed [fraction] of its work. A [fraction] of `1` means the capture is
     * complete: the progress indicator fills, then hides.
     *
     * @param fraction The completed fraction of the capture, from 0 to 1.
     */
    data class Determinate(val fraction: Float) : CaptureProgressUiState {
        init {
            require(fraction in 0f..1f) { "fraction must be between 0 and 1, was $fraction" }
        }
    }

    /**
     * The capture is expected to take [durationMillis] from [startedAtUptimeMillis]. The progress
     * indicator fills linearly over that time, then stays full until the capture ends.
     *
     * @param startedAtUptimeMillis The [android.os.SystemClock.uptimeMillis] time at which the
     * capture started.
     * @param durationMillis The expected duration of the capture.
     */
    data class Estimated(
        val startedAtUptimeMillis: Long,
        val durationMillis: Long
    ) : CaptureProgressUiState {
        init {
            require(durationMillis > 0) { "durationMillis must be positive, was $durationMillis" }
        }
    }

    companion object
}
