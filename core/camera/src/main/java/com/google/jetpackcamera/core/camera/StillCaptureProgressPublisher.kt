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

import android.os.SystemClock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/**
 * Publishes the progress of a single still capture to [CameraState.stillCaptureProgress].
 *
 * Create one publisher per capture, forward the capture's callbacks to [onCaptureStarted] and
 * [onCaptureProcessProgressed], and call [clear] when the capture ends. The publisher only clears
 * or updates progress that it published itself, so a capture that ends after the next capture has
 * started does not clear the next capture's progress.
 *
 * @param cameraState The camera state to publish to.
 * @param captureLatencyMillis Returns the camera's current estimate of how long capturing frames
 * takes, or `null` if the camera does not report one. Read when the capture starts.
 * @param uptimeMillis Returns the current [SystemClock.uptimeMillis] time.
 */
internal class StillCaptureProgressPublisher(
    private val cameraState: MutableStateFlow<CameraState>,
    private val captureLatencyMillis: () -> Long?,
    private val uptimeMillis: () -> Long = SystemClock::uptimeMillis
) {
    @Volatile
    private var published: StillCaptureProgress? = null

    /** Publishes that the camera started capturing frames for this capture. */
    fun onCaptureStarted() {
        publish(
            StillCaptureProgress(
                startedAtUptimeMillis = uptimeMillis(),
                captureLatencyMillis = captureLatencyMillis()?.takeIf { it > 0 }
            )
        )
    }

    /** Publishes the processing progress of this capture, from 0 to 100. */
    fun onCaptureProcessProgressed(percent: Int) {
        val base = published ?: StillCaptureProgress(startedAtUptimeMillis = uptimeMillis())
        publish(base.copy(processProgressPercent = percent.coerceIn(0, 100)))
    }

    /** Clears the progress of this capture, if it is still the published progress. */
    fun clear() {
        val previous = published ?: return
        published = null
        cameraState.update { old ->
            if (old.stillCaptureProgress == previous) {
                old.copy(stillCaptureProgress = null)
            } else {
                old
            }
        }
    }

    private fun publish(progress: StillCaptureProgress) {
        val previous = published
        published = progress
        cameraState.update { old ->
            // Another capture has replaced this capture's progress; leave it in place.
            if (previous != null && old.stillCaptureProgress != previous) {
                old
            } else {
                old.copy(stillCaptureProgress = progress)
            }
        }
    }
}
