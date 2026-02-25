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
package com.google.jetpackcamera.ui.components.capture.quicksettings.controller

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

class QuickSettingsControllerImpl(
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    private val viewModelScope: CoroutineScope,
    private val cameraSystem: CameraSystem,
    private val externalCaptureMode: ExternalCaptureMode
) : QuickSettingsController {
    override fun toggleQuickSettings() {
        trackedCaptureUiState.update { old ->
            old.copy(isQuickSettingsOpen = !old.isQuickSettingsOpen)
        }
    }

    override fun setFocusedSetting(focusedQuickSetting: FocusedQuickSetting) {
        trackedCaptureUiState.update { old ->
            old.copy(focusedQuickSetting = focusedQuickSetting)
        }
    }

    override fun setLensFacing(lensFace: LensFacing) {
        viewModelScope.launch {
            // apply to cameraSystem
            cameraSystem.setLensFacing(lensFace)
        }
    }

    override fun setFlash(flashMode: FlashMode) {
        viewModelScope.launch {
            // apply to cameraSystem
            cameraSystem.setFlashMode(flashMode)
        }
    }

    override fun setAspectRatio(aspectRatio: AspectRatio) {
        viewModelScope.launch {
            cameraSystem.setAspectRatio(aspectRatio)
        }
    }

    override fun setStreamConfig(streamConfig: StreamConfig) {
        viewModelScope.launch {
            cameraSystem.setStreamConfig(streamConfig)
        }
    }

    override fun setDynamicRange(dynamicRange: DynamicRange) {
        if (externalCaptureMode != ExternalCaptureMode.ImageCapture &&
            externalCaptureMode != ExternalCaptureMode.MultipleImageCapture
        ) {
            viewModelScope.launch {
                cameraSystem.setDynamicRange(dynamicRange)
            }
        }
    }

    override fun setImageFormat(imageOutputFormat: ImageOutputFormat) {
        if (externalCaptureMode != ExternalCaptureMode.VideoCapture) {
            viewModelScope.launch {
                cameraSystem.setImageFormat(imageOutputFormat)
            }
        }
    }

    override fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        viewModelScope.launch {
            cameraSystem.setConcurrentCameraMode(concurrentCameraMode)
        }
    }

    override fun setCaptureMode(captureMode: CaptureMode) {
        viewModelScope.launch {
            cameraSystem.setCaptureMode(captureMode)
        }
    }
}
