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
import com.google.jetpackcamera.model.CameraError
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostState
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality

/**
 * Default maximum number of image captures allowed in flight concurrently in a standard
 * single-camera session, matching CameraX's internal `CaptureNode.MAX_IMAGES` capacity.
 */
const val DEFAULT_MAX_IN_FLIGHT_CAPTURES = 4

/**
 * Represents the state of the camera.
 *
 * @param videoRecordingState The current state of video recording.
 * @param zoomRatios A map of [LensFacing] to the current zoom ratio. This value is updated from
 * the camera's [android.hardware.camera2.TotalCaptureResult] or [androidx.camera.core.ZoomState].
 * @param linearZoomScales A map of [LensFacing] to the current linear zoom scale.
 * @param sessionFirstFrameTimestamp The timestamp of the first frame of the current camera session.
 * @param isTorchEnabled Whether the torch is currently enabled.
 * @param isCameraRunning Whether the camera is currently running.
 * @param inFlightCapturesCount The number of still image captures currently in progress.
 * @param maxInFlightCaptures The maximum number of concurrent still image captures supported by
 * the active camera session.
 * @param stillCaptureProgress The progress of the still image capture that the camera is taking,
 * or `null` if no capture has started capturing frames.
 * @param stabilizationMode The current video stabilization mode. This value is updated from the
 * camera's [android.hardware.camera2.TotalCaptureResult].
 * @param lowLightBoostState The current state of the low light boost feature.
 * @param captureSubModeState The current runtime state of the active capture sub-mode.
 * @param debugInfo Information for debugging purposes.
 * @param videoQualityInfo Information about the current video quality.
 * @param focusState The current focus state of the camera.
 * @param cameraError The current camera error, if any.
 * @param isPreviewLandscape Whether the camera preview output is landscape after applying target
 * rotation, or `null` before a camera is bound.
 */
data class CameraState(
    val videoRecordingState: VideoRecordingState = VideoRecordingState.Inactive(),
    val zoomRatios: Map<LensFacing, Float> = mapOf(),
    val linearZoomScales: Map<LensFacing, Float> = mapOf(),
    val sessionFirstFrameTimestamp: Long = 0L,
    val isTorchEnabled: Boolean = false,
    val isCameraRunning: Boolean = false,
    val inFlightCapturesCount: Int = 0,
    val maxInFlightCaptures: Int = DEFAULT_MAX_IN_FLIGHT_CAPTURES,
    val stillCaptureProgress: StillCaptureProgress? = null,
    val stabilizationMode: StabilizationMode = StabilizationMode.OFF,
    val lowLightBoostState: LowLightBoostState = LowLightBoostState.Inactive,
    val captureSubModeState: CaptureSubModeState = CaptureSubModeState.Idle,
    val debugInfo: DebugInfo = DebugInfo(null, null),
    val videoQualityInfo: VideoQualityInfo = VideoQualityInfo(VideoQuality.UNSPECIFIED, 0, 0),
    val focusState: FocusState = FocusState.Unspecified,
    val cameraError: CameraError? = null,
    val isPreviewLandscape: Boolean? = null
) {
    /**
     * Whether the active camera session has reached its maximum concurrent in-flight image
     * capture capacity.
     */
    val isCaptureAtCapacity: Boolean
        get() = inFlightCapturesCount >= maxInFlightCaptures

    /**
     * Whether the camera is running and has capacity to accept another image capture request.
     */
    val canCaptureImage: Boolean
        get() = isCameraRunning && !isCaptureAtCapacity
}

/**
 * The progress of a still image capture, as reported by the camera.
 *
 * @param startedAtUptimeMillis The [android.os.SystemClock.uptimeMillis] time at which the camera
 * started capturing frames for the image.
 * @param captureLatencyMillis The camera's estimate of the time from [startedAtUptimeMillis] until
 * it has captured all frames for the image, or `null` if the camera does not report an estimate.
 * The image can still be processing after this time.
 * @param processProgressPercent The processing progress reported by the camera, from 0 to 100, or
 * `null` if the camera has not reported any.
 */
data class StillCaptureProgress(
    val startedAtUptimeMillis: Long,
    val captureLatencyMillis: Long? = null,
    val processProgressPercent: Int? = null
)

/**
 * Represents the runtime state of a capture sub-mode during a camera session.
 */
sealed interface CaptureSubModeState {
    /**
     * No capture sub-mode is active, or the active sub-mode is running without errors.
     */
    data object Idle : CaptureSubModeState

    /**
     * An error occurred while resolving or binding [subModeId], causing the session to fall back
     * to the base camera.
     *
     * @param subModeId The [CaptureSubModeId] that failed.
     * @param error The [Throwable] that caused the failure.
     */
    data class Error(val subModeId: CaptureSubModeId, val error: Throwable?) : CaptureSubModeState
}

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
 * Represents the state of the audio stream during video recording.
 */
sealed interface AudioStreamState {
    /**
     * The audio stream is actively recording and providing amplitude data.
     */
    data class Active(val amplitude: Double) : AudioStreamState

    /** The audio stream was not enabled or is disabled. */
    data object Disabled : AudioStreamState

    /** The audio stream was silenced by the system (e.g. privacy toggle). */
    data object Silenced : AudioStreamState

    /** The audio stream is muted (e.g. by the app/user). */
    data object Muted : AudioStreamState

    /** An error occurred in the audio stream. */
    data object Error : AudioStreamState

    /** The audio stream state is unknown. */
    data object Unknown : AudioStreamState
}

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
         * The state of the audio stream being recorded.
         */
        val audioStreamState: AudioStreamState

        /**
         * The elapsed time of the recording in nanoseconds.
         */
        val elapsedTimeNanos: Long

        /**
         * The camera is currently recording.
         */
        data class Recording(
            override val maxDurationMillis: Long,
            override val audioStreamState: AudioStreamState,
            override val elapsedTimeNanos: Long
        ) : Active

        /**
         * The camera is currently paused.
         */
        data class Paused(
            override val maxDurationMillis: Long,
            override val audioStreamState: AudioStreamState,
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
