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

import android.util.Range
import android.util.Size
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.feature.preview.ui.ImageWellUiState
import com.google.jetpackcamera.feature.preview.ui.SnackbarData
import com.google.jetpackcamera.feature.preview.ui.ToastMessage
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.SystemConstraints
import com.google.jetpackcamera.settings.model.VideoQuality
import com.google.jetpackcamera.ui.uistate.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.FlashModeUiState
import java.util.LinkedList
import java.util.Queue

/**
 * Defines the current state of the [PreviewScreen].
 */
sealed interface PreviewUiState {
    data object NotReady : PreviewUiState

    data class Ready(
        // "quick" settings
        val currentCameraSettings: CameraAppSettings = CameraAppSettings(),
        val systemConstraints: SystemConstraints = SystemConstraints(),
        val videoRecordingState: VideoRecordingState = VideoRecordingState.Inactive(),
        val quickSettingsIsOpen: Boolean = false,

        // todo: remove after implementing post capture screen
        val toastMessageToShow: ToastMessage? = null,
        val snackBarQueue: Queue<SnackbarData> = LinkedList(),
        val lastBlinkTimeStamp: Long = 0,
        val previewMode: PreviewMode = PreviewMode.StandardMode {},
        val captureModeToggleUiState: CaptureModeUiState = CaptureModeUiState.Unavailable,
        val sessionFirstFrameTimestamp: Long = 0L,
        val currentPhysicalCameraId: String? = null,
        val currentLogicalCameraId: String? = null,
        val debugUiState: DebugUiState = DebugUiState(),
        val stabilizationUiState: StabilizationUiState = StabilizationUiState.Disabled,
        val flashModeUiState: FlashModeUiState = FlashModeUiState.Unavailable,
        val videoQuality: VideoQuality = VideoQuality.UNSPECIFIED,
        val audioUiState: AudioUiState = AudioUiState.Disabled,
        val elapsedTimeUiState: ElapsedTimeUiState = ElapsedTimeUiState.Unavailable,
        val captureButtonUiState: CaptureButtonUiState = CaptureButtonUiState.Unavailable,
        val imageWellUiState: ImageWellUiState = ImageWellUiState.Unavailable,
        val captureModeUiState: CaptureModeUiState = CaptureModeUiState.Unavailable,
        val zoomUiState: ZoomUiState = ZoomUiState.Unavailable,
        val hdrUiState: HdrUiState = HdrUiState.Unavailable
    ) : PreviewUiState
}

data class DebugUiState(
    val cameraPropertiesJSON: String = "",
    val videoResolution: Size? = null,
    val isDebugMode: Boolean = false,
    val isDebugOverlayOpen: Boolean = false
)
val DEFAULT_CAPTURE_BUTTON_STATE = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)

sealed interface CaptureButtonUiState {
    data object Unavailable : CaptureButtonUiState
    sealed interface Enabled : CaptureButtonUiState {
        data class Idle(val captureMode: CaptureMode) : Enabled

        sealed interface Recording : Enabled {
            data object PressedRecording : Recording
            data object LockedRecording : Recording
        }
    }
}

sealed interface ElapsedTimeUiState {
    data object Unavailable : ElapsedTimeUiState
    data class Enabled(val elapsedTimeNanos: Long) : ElapsedTimeUiState
}
sealed interface HdrUiState {
    data object Unavailable : HdrUiState
    data class Available(
        val currentImageOutputFormat: ImageOutputFormat,
        val currentDynamicRange: DynamicRange
    ) : HdrUiState
}
sealed interface ZoomUiState {
    data object Unavailable : ZoomUiState
    data class Enabled(
        val primaryZoomRange: Range<Float>,
        val primaryZoomRatio: Float? = null,
        val primaryLinearZoom: Float? = null
    ) : ZoomUiState
}
sealed interface AudioUiState {
    val amplitude: Double

    sealed interface Enabled : AudioUiState {
        data class On(override val amplitude: Double) : Enabled
        data object Mute : Enabled {
            override val amplitude = 0.0
        }
    }

    // todo give a disabledreason when audio permission is not granted
    data object Disabled : AudioUiState {
        override val amplitude = 0.0
    }
}

sealed interface StabilizationUiState {
    data object Disabled : StabilizationUiState

    sealed interface Enabled : StabilizationUiState {
        val stabilizationMode: StabilizationMode
        val active: Boolean
    }

    data class Specific(
        override val stabilizationMode: StabilizationMode,
        override val active: Boolean = true
    ) : Enabled {
        init {
            require(stabilizationMode != StabilizationMode.AUTO) {
                "Specific StabilizationUiState cannot have AUTO stabilization mode."
            }
        }
    }

    data class Auto(override val stabilizationMode: StabilizationMode) : Enabled {
        override val active = true
    }
}
