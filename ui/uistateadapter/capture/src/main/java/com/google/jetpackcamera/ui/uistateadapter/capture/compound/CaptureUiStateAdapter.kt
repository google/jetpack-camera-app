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
package com.google.jetpackcamera.ui.uistateadapter.capture.compound

import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.from
import com.google.jetpackcamera.ui.uistateadapter.capture.updateFrom
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull

/**
 * Creates a [Flow] of [CaptureUiState] by combining the latest values from various sources.
 *
 * This function acts as a central adapter to transform low-level camera and UI states into a
 * comprehensive [CaptureUiState] that the UI can directly observe and react to.
 *
 * @param cameraSystem The [CameraSystem] providing real-time camera state and settings.
 * @param constraintsRepository The [ConstraintsRepository] for accessing system-wide constraints.
 * @param trackedCaptureUiState A [MutableStateFlow] representing the user-interacted UI state that
 * needs to be tracked across recompositions (e.g., whether quick settings is open).
 * @param externalCaptureMode The [ExternalCaptureMode] influencing UI behavior based on how the
 * camera is launched (e.g., from an external intent).
 * @param timePrecision The precision to use for rounding the elapsed time of video recording.
 *
 * @return A [Flow] that emits a new [CaptureUiState] whenever any of its underlying
 * data sources change.
 */
fun captureUiState(
    cameraSystem: CameraSystem,
    constraintsRepository: ConstraintsRepository,
    trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    externalCaptureMode: ExternalCaptureMode,
    timePrecision: TimeUnit = TimeUnit.SECONDS
): Flow<CaptureUiState> {
    var flashModeUiState: FlashModeUiState? = null
    var focusMeteringUiState: FocusMeteringUiState? = null

    return combine(
        cameraSystem.getCurrentSettings().filterNotNull(),
        constraintsRepository.systemConstraints.filterNotNull(),
        cameraSystem.getCurrentCameraState(),
        trackedCaptureUiState
    ) { cameraAppSettings, systemConstraints, cameraState, trackedUiState ->
        val videoRecordingState = cameraState.videoRecordingState
        val roundedVideoRecordingState =
            roundVideoRecordingState(videoRecordingState, timePrecision)
        val roundedCameraState = cameraState.copy(videoRecordingState = roundedVideoRecordingState)

        val captureModeUiState = CaptureModeUiState.from(
            systemConstraints,
            cameraAppSettings,
            externalCaptureMode
        )
        val flipLensUiState = FlipLensUiState.from(
            cameraAppSettings,
            systemConstraints
        )
        val aspectRatioUiState = AspectRatioUiState.from(cameraAppSettings)
        val hdrUiState = HdrUiState.from(
            cameraAppSettings,
            systemConstraints,
            externalCaptureMode
        )

        flashModeUiState = flashModeUiState.let {
            it?.updateFrom(
                cameraAppSettings = cameraAppSettings,
                systemConstraints = systemConstraints,
                cameraState = roundedCameraState
            )
                ?: FlashModeUiState.from(cameraAppSettings, systemConstraints)
        }
        focusMeteringUiState = focusMeteringUiState.let {
            it?.updateFrom(
                cameraState = roundedCameraState
            )
                ?: FocusMeteringUiState.from(roundedCameraState)
        }
        CaptureUiState.Ready(
            externalCaptureMode = externalCaptureMode,
            videoRecordingState = roundedVideoRecordingState,
            flipLensUiState = flipLensUiState,
            aspectRatioUiState = aspectRatioUiState,
            previewDisplayUiState = PreviewDisplayUiState(
                trackedUiState.lastBlinkTimeStamp,
                aspectRatioUiState
            ),
            // TODO: add updateFrom() for all ui states to prevent re-updating if
            // values are the same
            quickSettingsUiState = QuickSettingsUiState.from(
                captureModeUiState,
                flashModeUiState,
                flipLensUiState,
                cameraAppSettings,
                systemConstraints,
                aspectRatioUiState,
                hdrUiState,
                trackedUiState.isQuickSettingsOpen,
                trackedUiState.focusedQuickSetting,
                externalCaptureMode
            ),
            sessionFirstFrameTimestamp = roundedCameraState.sessionFirstFrameTimestamp,
            stabilizationUiState = StabilizationUiState.from(
                cameraAppSettings,
                roundedCameraState
            ),
            flashModeUiState = flashModeUiState,
            videoQuality = roundedCameraState.videoQualityInfo.quality,
            audioUiState = AudioUiState.from(
                cameraAppSettings,
                roundedCameraState
            ),
            elapsedTimeUiState = ElapsedTimeUiState.from(roundedCameraState),
            captureButtonUiState = CaptureButtonUiState.from(
                cameraAppSettings,
                roundedCameraState,
                trackedUiState.isRecordingLocked
            ),
            zoomUiState = ZoomUiState.from(
                systemConstraints,
                cameraAppSettings.cameraLensFacing,
                roundedCameraState
            ),
            zoomControlUiState = ZoomControlUiState.from(
                trackedUiState.zoomAnimationTarget,
                systemConstraints,
                cameraAppSettings,
                roundedCameraState
            ),
            captureModeToggleUiState = CaptureModeToggleUiState.from(
                systemConstraints,
                cameraAppSettings,
                roundedCameraState,
                externalCaptureMode
            ),
            hdrUiState = hdrUiState,
            focusMeteringUiState = focusMeteringUiState,
            imageWellUiState = ImageWellUiState.from(
                trackedUiState.recentCapturedMedia,
                roundedVideoRecordingState
            ),
            screenFlashUiState = ScreenFlashUiState.from(trackedUiState)
        )
    }
}

/**
 * Rounds the elapsed time of a [VideoRecordingState] to the given [timePrecision] to reduce UI recomposition frequency.
 */
internal fun roundVideoRecordingState(
    videoRecordingState: VideoRecordingState,
    timePrecision: TimeUnit
): VideoRecordingState {
    if (videoRecordingState !is VideoRecordingState.Active) return videoRecordingState

    val stepNanos = timePrecision.toNanos(1)
    val roundedNanos = (videoRecordingState.elapsedTimeNanos / stepNanos) * stepNanos

    return when (videoRecordingState) {
        is VideoRecordingState.Active.Recording -> videoRecordingState.copy(
            elapsedTimeNanos = roundedNanos
        )
        is VideoRecordingState.Active.Paused -> videoRecordingState.copy(
            elapsedTimeNanos = roundedNanos
        )
    }
}
