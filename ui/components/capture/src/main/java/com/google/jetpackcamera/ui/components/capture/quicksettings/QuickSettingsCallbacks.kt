/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.google.jetpackcamera.ui.components.capture.quicksettings

import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This file contains the data class [QuickSettingsCallbacks] and a helper function to create it.
 * [QuickSettingsCallbacks] is used to handle UI events on the quick settings screen.
 */

/**
 * Data class holding callbacks for quick settings UI events.
 *
 * @param toggleQuickSettings Toggles the quick settings panel.
 * @param setFocusedSetting Sets the currently focused quick setting.
 * @param setLensFacing Toggles the lens facing (front or back).
 * @param setFlash Toggles the flash mode.
 * @param setAspectRatio Sets the aspect ratio.
 * @param setStreamConfig Sets the stream configuration.
 * @param setDynamicRange Sets the dynamic range.
 * @param setImageFormat Sets the image format.
 * @param setConcurrentCameraMode Sets the concurrent camera mode.
 * @param setCaptureMode Sets the capture mode.
 */
data class QuickSettingsCallbacks(
    val toggleQuickSettings: () -> Unit,
    val setFocusedSetting: (FocusedQuickSetting) -> Unit,
    val setLensFacing: (lensFace: LensFacing) -> Unit,
    val setFlash: (flashMode: FlashMode) -> Unit,
    val setAspectRatio: (aspectRation: AspectRatio) -> Unit,
    val setStreamConfig: (streamConfig: StreamConfig) -> Unit,
    val setDynamicRange: (dynamicRange: DynamicRange) -> Unit,
    val setImageFormat: (imageOutputFormat: ImageOutputFormat) -> Unit,
    val setConcurrentCameraMode: (concurrentCameraMode: ConcurrentCameraMode) -> Unit,
    val setCaptureMode: (CaptureMode) -> Unit
)

/**
 * Creates a [QuickSettingsCallbacks] instance with implementations that interact with the camera
 * system and update the UI state.
 *
 * @param trackedCaptureUiState The mutable state flow for the tracked capture UI state.
 * @param viewModelScope The [CoroutineScope] for launching coroutines.
 * @param cameraSystem The [CameraSystem] to interact with the camera hardware.
 * @param externalCaptureMode The external capture mode.
 * @return An instance of [QuickSettingsCallbacks].
 */
fun getQuickSettingsCallbacks(
    trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    viewModelScope: CoroutineScope,
    cameraSystem: CameraSystem,
    externalCaptureMode: ExternalCaptureMode
): QuickSettingsCallbacks {
    return QuickSettingsCallbacks(
        toggleQuickSettings = {
            trackedCaptureUiState.update { old ->
                old.copy(isQuickSettingsOpen = !old.isQuickSettingsOpen)
            }
        },
        setFocusedSetting = { focusedQuickSetting ->
            trackedCaptureUiState.update { old ->
                old.copy(focusedQuickSetting = focusedQuickSetting)
            }
        },
        setLensFacing = { newLensFacing ->
            viewModelScope.launch {
                // apply to cameraSystem
                cameraSystem.setLensFacing(newLensFacing)
            }
        },
        setFlash = { flashMode ->
            viewModelScope.launch {
                // apply to cameraSystem
                cameraSystem.setFlashMode(flashMode)
            }
        },
        setAspectRatio = { aspectRatio ->
            viewModelScope.launch {
                cameraSystem.setAspectRatio(aspectRatio)
            }
        },
        setStreamConfig = { streamConfig ->
            viewModelScope.launch {
                cameraSystem.setStreamConfig(streamConfig)
            }
        },
        setDynamicRange = { dynamicRange ->
            if (externalCaptureMode != ExternalCaptureMode.ImageCapture &&
                externalCaptureMode != ExternalCaptureMode.MultipleImageCapture
            ) {
                viewModelScope.launch {
                    cameraSystem.setDynamicRange(dynamicRange)
                }
            }
        },
        setImageFormat = { imageFormat ->
            if (externalCaptureMode != ExternalCaptureMode.VideoCapture) {
                viewModelScope.launch {
                    cameraSystem.setImageFormat(imageFormat)
                }
            }
        },
        setConcurrentCameraMode = { concurrentCameraMode ->
            viewModelScope.launch {
                cameraSystem.setConcurrentCameraMode(concurrentCameraMode)
            }
        },
        setCaptureMode = { captureMode ->
            viewModelScope.launch {
                cameraSystem.setCaptureMode(captureMode)
            }
        }
    )
}
