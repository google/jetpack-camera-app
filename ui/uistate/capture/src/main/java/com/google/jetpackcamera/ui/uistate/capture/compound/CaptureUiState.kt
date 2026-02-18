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
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState

/**
 * Defines the current UI state for the camera capture screen.
 * This state object encapsulates various sub-states related to camera controls,
 * settings, and display elements, providing a comprehensive view of the UI.
 */
sealed interface CaptureUiState {
    /**
     * The capture UI is not ready and should display a loading state or be disabled.
     * This is the initial state before the camera is fully initialized.
     */
    data object NotReady : CaptureUiState

    /**
     * The capture UI is ready for user interaction.
     *
     * @property videoRecordingState The current state of video recording.
     * @property quickSettingsUiState The UI state for the quick settings panel.
     * @property aspectRatioUiState The UI state for the aspect ratio setting.
     * @property flipLensUiState The UI state for the flip lens (front/back camera) button.
     * @property snackBarUiState The UI state for the snack bar, used for showing messages to the user.
     * @property previewDisplayUiState The UI state for the preview display.
     * @property lastBlinkTimeStamp The timestamp of the last image capture blink animation.
     * @property externalCaptureMode The external capture mode used by the intent that launched the camera. Default is [ExternalCaptureMode.Standard].
     * @property captureModeToggleUiState The UI state for the photo/video toggle.
     * @property sessionFirstFrameTimestamp The timestamp of the first frame of the current camera session.
     * @property debugUiState The UI state for the debug overlay.
     * @property stabilizationUiState The UI state for the video stabilization setting.
     * @property flashModeUiState The UI state for the flash mode setting.
     * @property videoQuality The currently selected video quality.
     * @property audioUiState The UI state for audio recording.
     * @property elapsedTimeUiState The UI state for the elapsed time display during video recording.
     * @property captureButtonUiState The UI state for the capture button.
     * @property imageWellUiState The UI state for the image well, which shows the last captured media.
     * @property zoomUiState The UI state for the zoom level display.
     * @property zoomControlUiState The UI state for the zoom control buttons.
     * @property hdrUiState The UI state for the HDR setting.
     * @property focusMeteringUiState The UI state for focus and metering.
     */
    data class Ready(
        val videoRecordingState: VideoRecordingState = VideoRecordingState.Inactive(),
        val quickSettingsUiState: QuickSettingsUiState = QuickSettingsUiState.Unavailable,
        val aspectRatioUiState: AspectRatioUiState = AspectRatioUiState.Unavailable,
        val flipLensUiState: FlipLensUiState = FlipLensUiState.Unavailable,
        val previewDisplayUiState: PreviewDisplayUiState =
            PreviewDisplayUiState(aspectRatioUiState = AspectRatioUiState.Unavailable),
        val lastBlinkTimeStamp: Long = 0,
        val externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard,
        val captureModeToggleUiState: CaptureModeToggleUiState =
            CaptureModeToggleUiState.Unavailable,
        val sessionFirstFrameTimestamp: Long = 0L,
        val stabilizationUiState: StabilizationUiState = StabilizationUiState.Disabled,
        val flashModeUiState: FlashModeUiState = FlashModeUiState.Unavailable,
        val videoQuality: VideoQuality = VideoQuality.UNSPECIFIED,
        val audioUiState: AudioUiState = AudioUiState.Disabled,
        val elapsedTimeUiState: ElapsedTimeUiState = ElapsedTimeUiState.Unavailable,
        val captureButtonUiState: CaptureButtonUiState = CaptureButtonUiState.Unavailable,
        val imageWellUiState: ImageWellUiState = ImageWellUiState.Unavailable,
        val zoomUiState: ZoomUiState = ZoomUiState.Unavailable,
        val zoomControlUiState: ZoomControlUiState = ZoomControlUiState.Unavailable,
        val hdrUiState: HdrUiState = HdrUiState.Unavailable,
        val focusMeteringUiState: FocusMeteringUiState = FocusMeteringUiState.Unspecified
    ) : CaptureUiState

    companion object
}
