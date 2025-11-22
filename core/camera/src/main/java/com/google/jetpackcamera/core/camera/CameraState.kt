/*
 * Copyright (C) 2025 The Android Open Source Project
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

import android.net.Uri
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostState
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality

data class CameraState(
    val videoRecordingState: VideoRecordingState = VideoRecordingState.Inactive(),
    val zoomRatios: Map<LensFacing, Float> = mapOf(),
    val linearZoomScales: Map<LensFacing, Float> = mapOf(),
    val sessionFirstFrameTimestamp: Long = 0L,
    val isTorchEnabled: Boolean = false,
    val isCameraRunning: Boolean = false,
    val stabilizationMode: StabilizationMode = StabilizationMode.OFF,
    val lowLightBoostState: LowLightBoostState = LowLightBoostState.Inactive,
    val debugInfo: DebugInfo = DebugInfo(null, null),
    val videoQualityInfo: VideoQualityInfo = VideoQualityInfo(VideoQuality.UNSPECIFIED, 0, 0)
)

data class DebugInfo(val logicalCameraId: String?, val physicalCameraId: String?)

data class VideoQualityInfo(val quality: VideoQuality, val width: Int, val height: Int)

sealed interface VideoRecordingState {

    /**
     * Indicates that a [PendingRecording][androidx.camera.video.PendingRecording] is about to start.
     * This state may be used as a signal to start processes just before the recording actually starts.
     */
    data class Starting(val initialRecordingSettings: InitialRecordingSettings? = null) :
        VideoRecordingState

    /**
     * Camera is not currently recording a video
     */
    data class Inactive(val finalElapsedTimeNanos: Long = 0) : VideoRecordingState

    /**
     * Camera is currently active; paused, stopping, or recording a video
     */
    sealed interface Active : VideoRecordingState {
        val maxDurationMillis: Long
        val audioAmplitude: Double
        val elapsedTimeNanos: Long

        data class Recording(
            override val maxDurationMillis: Long,
            override val audioAmplitude: Double,
            override val elapsedTimeNanos: Long
        ) : Active

        data class Paused(
            override val maxDurationMillis: Long,
            override val audioAmplitude: Double,
            override val elapsedTimeNanos: Long
        ) : Active
    }
}

/**
 * Represents the events for video recording.
 */
sealed interface OnVideoRecordEvent {
    data class OnVideoRecorded(val savedUri: Uri) : OnVideoRecordEvent

    data class OnVideoRecordError(val error: Throwable) : OnVideoRecordEvent
}
