/*
 * Copyright (C) 2024 The Android Open Source Project
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

/**
 * Represents events that control video capture operations.
 */
sealed interface VideoCaptureControlEvent {

    /**
     * Starts video recording.
     *
     * @param onVideoRecord Callback to handle video recording events.
     */
    class StartRecordingEvent(
        val videoCaptureUri: Uri?,
        val shouldUseUri: Boolean,
        val maxVideoDuration: Long,
        val onRestoreSettings: () -> Unit,
        val onVideoRecord: (CameraUseCase.OnVideoRecordEvent) -> Unit
    ) : VideoCaptureControlEvent

    /**
     * Pauses a video recording.
     */
    data object PauseRecordingEvent : VideoCaptureControlEvent

    /**
     * Resumes a paused video recording.
     */
    data object ResumeRecordingEvent : VideoCaptureControlEvent

    /**
     * Stops video recording.
     */
    data object StopRecordingEvent : VideoCaptureControlEvent
}
