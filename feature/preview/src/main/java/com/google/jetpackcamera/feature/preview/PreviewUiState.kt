/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview

import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.feature.preview.ui.SnackbarData
import com.google.jetpackcamera.settings.model.VideoQuality
import com.google.jetpackcamera.ui.uistate.viewfinder.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.AudioUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.DebugUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.ZoomUiState
import com.google.jetpackcamera.ui.uistate.viewfinder.compound.QuickSettingsUiState
import java.util.LinkedList
import java.util.Queue

/**
 * Defines the current state of the [PreviewScreen].
 */
sealed interface PreviewUiState {
    data object NotReady : PreviewUiState

    data class Ready(
        val videoRecordingState: VideoRecordingState = VideoRecordingState.Inactive(),
        val quickSettingsUiState: QuickSettingsUiState = QuickSettingsUiState.Unavailable,
        val aspectRatioUiState: AspectRatioUiState = AspectRatioUiState.Unavailable,
        val flipLensUiState: FlipLensUiState = FlipLensUiState.Unavailable,
        val snackBarQueue: Queue<SnackbarData> = LinkedList(),
        val lastBlinkTimeStamp: Long = 0,
        val previewMode: PreviewMode = PreviewMode.StandardMode {},
        val captureModeToggleUiState: CaptureModeUiState = CaptureModeUiState.Unavailable,
        val sessionFirstFrameTimestamp: Long = 0L,
        val debugUiState: DebugUiState = DebugUiState(),
        val stabilizationUiState: StabilizationUiState = StabilizationUiState.Disabled,
        val flashModeUiState: FlashModeUiState = FlashModeUiState.Unavailable,
        val videoQuality: VideoQuality = VideoQuality.UNSPECIFIED,
        val audioUiState: AudioUiState = AudioUiState.Disabled,
        val elapsedTimeUiState: ElapsedTimeUiState = ElapsedTimeUiState.Unavailable,
        val captureButtonUiState: CaptureButtonUiState = CaptureButtonUiState.Unavailable,
        val imageWellUiState: ImageWellUiState = ImageWellUiState.Unavailable,
        val zoomUiState: ZoomUiState = ZoomUiState.Unavailable
    ) : PreviewUiState
}
