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

import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.DebugUiState
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.FocusMeteringUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomControlUiState
import com.google.jetpackcamera.ui.uistate.capture.ZoomUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.from
import com.google.jetpackcamera.ui.uistateadapter.capture.updateFrom
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

fun CaptureUiState.Companion.update(
    captureUiState: MutableStateFlow<CaptureUiState>,
    cameraAppSettings: CameraAppSettings,
    systemConstraints: CameraSystemConstraints,
    cameraState: CameraState,
    externalCaptureMode: ExternalCaptureMode,
    debugSettings: DebugSettings,
    cameraPropertiesJSON: String,
    isQuickSettingsOpen: Boolean,
    focusedQuickSetting: FocusedQuickSetting,
    isDebugOverlayOpen: Boolean,
    isRecordingLocked: Boolean,
    zoomAnimationTarget: Float?,
    debugHidingComponents: Boolean,
    recentCapturedMedia: MediaDescriptor
) {
    var flashModeUiState: FlashModeUiState
    var focusMeteringUiState: FocusMeteringUiState

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
    captureUiState.update { old ->
        when (old) {
            is CaptureUiState.NotReady -> {
                flashModeUiState = FlashModeUiState.from(
                    cameraAppSettings,
                    systemConstraints
                )
                focusMeteringUiState = FocusMeteringUiState.from(cameraState)
                // This is the first PreviewUiState.Ready. Create the initial
                // PreviewUiState.Ready from defaults and initialize it below.
                CaptureUiState.Ready()
            }

            is CaptureUiState.Ready -> {
                flashModeUiState = old.flashModeUiState.updateFrom(
                    cameraAppSettings = cameraAppSettings,
                    systemConstraints = systemConstraints,
                    cameraState = cameraState
                )

                focusMeteringUiState = old.focusMeteringUiState.updateFrom(cameraState)
                // We have a previous `PreviewUiState.Ready`, return it here and
                // update it below.
                old
            }
        }.copy(
            // Update or initialize PreviewUiState.Ready
            externalCaptureMode = externalCaptureMode,
            videoRecordingState = cameraState.videoRecordingState,
            flipLensUiState = flipLensUiState,
            aspectRatioUiState = aspectRatioUiState,
            previewDisplayUiState = PreviewDisplayUiState(0, aspectRatioUiState),
            quickSettingsUiState = QuickSettingsUiState.from(
                captureModeUiState,
                flashModeUiState,
                flipLensUiState,
                cameraAppSettings,
                systemConstraints,
                aspectRatioUiState,
                hdrUiState,
                isQuickSettingsOpen,
                focusedQuickSetting,
                externalCaptureMode
            ),
            sessionFirstFrameTimestamp = cameraState.sessionFirstFrameTimestamp,
            debugUiState = DebugUiState.from(
                systemConstraints,
                cameraAppSettings,
                cameraState,
                isDebugOverlayOpen,
                debugHidingComponents,
                debugSettings,
                cameraPropertiesJSON
            ),
            stabilizationUiState = StabilizationUiState.from(
                cameraAppSettings,
                cameraState
            ),
            flashModeUiState = flashModeUiState,
            videoQuality = cameraState.videoQualityInfo.quality,
            audioUiState = AudioUiState.from(
                cameraAppSettings,
                cameraState
            ),
            elapsedTimeUiState = ElapsedTimeUiState.from(cameraState),
            captureButtonUiState = CaptureButtonUiState.from(
                cameraAppSettings,
                cameraState,
                isRecordingLocked
            ),
            zoomUiState = ZoomUiState.from(
                systemConstraints,
                cameraAppSettings.cameraLensFacing,
                cameraState
            ),
            zoomControlUiState = ZoomControlUiState.from(
                zoomAnimationTarget,
                systemConstraints,
                cameraAppSettings,
                cameraState
            ),
            captureModeToggleUiState = CaptureModeToggleUiState.from(
                systemConstraints,
                cameraAppSettings,
                cameraState,
                externalCaptureMode
            ),
            hdrUiState = hdrUiState,
            focusMeteringUiState = focusMeteringUiState,
            imageWellUiState = ImageWellUiState.from(
                recentCapturedMedia,
                cameraState.videoRecordingState
            )

        )
    }
}
