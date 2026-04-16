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

/**
 * Represents the state of the camera.
 *
 * @param videoRecordingState The current state of video recording.
 * @param zoomRatios A map of [LensFacing] to the current zoom ratio. This value is updated from
 * the camera's [TotalCaptureResult] or [ZoomState].
 * @param linearZoomScales A map of [LensFacing] to the current linear zoom scale.
 * @param sessionFirstFrameTimestamp The timestamp of the first frame of the current camera session.
 * @param isTorchEnabled Whether the torch is currently enabled.
 * @param isCameraRunning Whether the camera is currently running.
 * @param stabilizationMode The current video stabilization mode. This value is updated from the
 * camera's [TotalCaptureResult].
 * @param lowLightBoostState The current state of the low light boost feature.
 * @param debugInfo Information for debugging purposes.
 * @param videoQualityInfo Information about the current video quality.
 * @param focusState The current focus state of the camera.
 */
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
    val videoQualityInfo: VideoQualityInfo = VideoQualityInfo(VideoQuality.UNSPECIFIED, 0, 0),
    val focusState: FocusState = FocusState.Unspecified
)

/**
 * Contains debugging information about the camera.
 *
 * @param logicalCameraId The ID of the logical camera. This value is derived from the session's
 * [CameraDevice.getId()](https://developer.android.com/reference/android/hardware/camera2/CameraDevice#getId()).
 * @param physicalCameraId The ID of the physical camera. This value is derived from the
 * [TotalCaptureResult] of the current session.
 */
data class DebugInfo(val logicalCameraId: String?, val physicalCameraId: String?)

/**
 * Represents the UI state of an autofocus event.
 */
sealed interface FocusState {
    /**
     * The camera's focus is in an unspecified state.
     */
    data object Unspecified : FocusState

    /**
     * The camera's focus has been explicitly set.
     *
     * @param x The x-coordinate of the focus point.
     * @param y The y-coordinate of the focus point.
     * @param status The status of the focus operation.
     */
    data class Specified(
        val x: Float,
        val y: Float,
        val status: Status
    ) : FocusState

    /**
     * Represents the status of a focus operation.
     */
    enum class Status {
        /**
         * The focus operation is in progress.
         */
        RUNNING,

        /**
         * The focus operation completed successfully.
         */
        SUCCESS,

        /**
         * The focus operation failed.
         */
        FAILURE,

        /**
         * The focus operation was cancelled.
         */
        CANCELLED
    }
}

/**
 * Contains information about the video quality.
 *
 * @param quality The selected video quality.
 * @param width The width of the video in pixels.
 * @param height The height of the video in pixels.
 */
data class VideoQualityInfo(val quality: VideoQuality, val width: Int, val height: Int)

/**
 * Represents the state of video recording.
 */
sealed interface VideoRecordingState {

    /**
     * Indicates that a [PendingRecording][androidx.camera.video.PendingRecording] is about to start.
     * This state may be used as a signal to start processes just before the recording actually starts.
     *
     * @param initialRecordingSettings The initial settings for the recording.
     */
    data class Starting(val initialRecordingSettings: InitialRecordingSettings? = null) :
        VideoRecordingState

    /**
     * Camera is not currently recording a video.
     *
     * @param finalElapsedTimeNanos The final elapsed time of the recording in nanoseconds. This is
     * not an error but the actual duration of the video.
     */
    data class Inactive(val finalElapsedTimeNanos: Long = 0) : VideoRecordingState

    /**
     * Camera is currently active; paused, stopping, or recording a video.
     */
    sealed interface Active : VideoRecordingState {
        /**
         * The maximum duration of the recording in milliseconds.
         */
        val maxDurationMillis: Long
        /**
         * The current amplitude of the audio being recorded.
         */
        val audioAmplitude: Double
        /**
         * The elapsed time of the recording in nanoseconds.
         */
        val elapsedTimeNanos: Long

        /**
         * The camera is currently recording.
         */
        data class Recording(
            override val maxDurationMillis: Long,
            override val audioAmplitude: Double,
            override val elapsedTimeNanos: Long
        ) : Active

        /**
         * The camera is currently paused.
         */
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
    /**
     * The video was recorded successfully.
     *
     * @param savedUri The [Uri] of the saved video.
     */
    data class OnVideoRecorded(val savedUri: Uri) : OnVideoRecordEvent

    /**
     * An error occurred while recording the video.
     *
     * @param error The [Throwable] that caused the error.
     */
    data class OnVideoRecordError(val error: Throwable) : OnVideoRecordEvent
}
