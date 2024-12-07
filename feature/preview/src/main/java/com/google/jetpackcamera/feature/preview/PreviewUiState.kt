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
import com.google.jetpackcamera.feature.preview.ui.ToastMessage
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.SystemConstraints

/**
 * Defines the current state of the [PreviewScreen].
 */
sealed interface PreviewUiState {
    data object NotReady : PreviewUiState

    data class Ready(
        // "quick" settings
        val currentCameraSettings: CameraAppSettings = CameraAppSettings(),
        val systemConstraints: SystemConstraints = SystemConstraints(),
        val zoomScale: Float = 1f,
        val videoRecordingState: VideoRecordingState = VideoRecordingState.Inactive(),
        val quickSettingsIsOpen: Boolean = false,
        val audioMuted: Boolean = false,

        // todo: remove after implementing post capture screen
        val toastMessageToShow: ToastMessage? = null,
        val snackBarToShow: SnackbarData? = null,
        val lastBlinkTimeStamp: Long = 0,
        val previewMode: PreviewMode = PreviewMode.StandardMode {},
        val captureModeToggleUiState: CaptureModeToggleUiState = CaptureModeToggleUiState.Invisible,
        val sessionFirstFrameTimestamp: Long = 0L,
        val currentPhysicalCameraId: String? = null,
        val currentLogicalCameraId: String? = null,
        val debugUiState: DebugUiState = DebugUiState(),
        val stabilizationUiState: StabilizationUiState = StabilizationUiState.Disabled,
        val flashModeUiState: FlashModeUiState = FlashModeUiState.Unavailable
    ) : PreviewUiState
}

// todo(kc): add ElapsedTimeUiState class

data class DebugUiState(
    val cameraPropertiesJSON: String = "",
    val isDebugMode: Boolean = false,
    val isDebugOverlayOpen: Boolean = false
)

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

    data class Auto(
        override val stabilizationMode: StabilizationMode
    ) : Enabled {
        override val active = true
    }
}

sealed class FlashModeUiState {
    data object Unavailable : FlashModeUiState()

    data class Available(
        val selectedFlashMode: FlashMode,
        val availableFlashModes: List<FlashMode>
    ) : FlashModeUiState() {
        init {
            check(selectedFlashMode in availableFlashModes) {
                "Selected flash mode of $selectedFlashMode not in available modes: " +
                    "$availableFlashModes"
            }
        }
    }

    companion object {
        private val ORDERED_UI_SUPPORTED_FLASH_MODES = listOf(
            FlashMode.OFF,
            FlashMode.ON,
            FlashMode.AUTO,
            FlashMode.LOW_LIGHT_BOOST
        )

        /**
         * Creates a FlashModeUiState from a selected flash mode and a set of supported flash modes
         * that may not include flash modes supported by the UI.
         */
        fun createFrom(
            selectedFlashMode: FlashMode,
            supportedFlashModes: Set<FlashMode>
        ): FlashModeUiState {
            // Ensure we at least support one flash mode
            check(supportedFlashModes.isNotEmpty()) {
                "No flash modes supported. Should at least support OFF."
            }

            // Convert available flash modes to list we support in the UI in our desired order
            val availableModes = ORDERED_UI_SUPPORTED_FLASH_MODES.filter {
                it in supportedFlashModes
            }

            return if (availableModes.isEmpty() || availableModes == listOf(FlashMode.OFF)) {
                // If we only support OFF, then return "Unavailable".
                Unavailable
            } else {
                Available(
                    selectedFlashMode = selectedFlashMode,
                    availableFlashModes = availableModes
                )
            }
        }
    }
}
