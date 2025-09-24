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
package com.google.jetpackcamera.ui.uistate.capture.compound

import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.SnackBarUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState

/**
 * Defines the current UI state for the camera capture screen.
 * This state object encapsulates various sub-states related to camera controls,
 * settings, and display elements, providing a comprehensive view of the UI.
 */
sealed interface CaptureUiState {
    data object NotReady : CaptureUiState

    data class Ready(
        val videoRecordingState: VideoRecordingState = VideoRecordingState.Inactive(),
        val quickSettingsUiState: QuickSettingsUiState = QuickSettingsUiState.Unavailable,
        val aspectRatioUiState: AspectRatioUiState = AspectRatioUiState.Unavailable,
        val flipLensUiState: FlipLensUiState = FlipLensUiState.Unavailable,
        val snackBarUiState: SnackBarUiState = SnackBarUiState(),
        val previewDisplayUiState: PreviewDisplayUiState =
            PreviewDisplayUiState(aspectRatioUiState = AspectRatioUiState.Unavailable),
        val lastBlinkTimeStamp: Long = 0,
        val externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard,
        val captureModeToggleUiState: CaptureModeToggleUiState =
            CaptureModeToggleUiState.Unavailable,
        val sessionFirstFrameTimestamp: Long = 0L,
        val debugUiState: DebugUiState = DebugUiState.Disabled,
        val stabilizationUiState: StabilizationUiState = StabilizationUiState.Disabled,
        val flashModeUiState: FlashModeUiState = FlashModeUiState.Unavailable,
        val videoQuality: VideoQuality = VideoQuality.UNSPECIFIED,
        val audioUiState: AudioUiState = AudioUiState.Disabled,
        val elapsedTimeUiState: ElapsedTimeUiState = ElapsedTimeUiState.Unavailable,
        val captureButtonUiState: CaptureButtonUiState = CaptureButtonUiState.Unavailable,
        val imageWellUiState: ImageWellUiState = ImageWellUiState.Unavailable,
        val zoomUiState: ZoomUiState = ZoomUiState.Unavailable,
        val zoomControlUiState: ZoomControlUiState = ZoomControlUiState.Unavailable
    ) : CaptureUiState
}
