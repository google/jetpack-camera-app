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
package com.google.jetpackcamera.ui.components.capture.controller

import android.util.Log
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.ui.components.capture.controller.Utils.postCurrentMediaToMediaRepository
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CaptureScreenControllerImpl"

class CaptureScreenControllerImpl(
    private val viewModelScope: CoroutineScope,
    private val cameraSystem: CameraSystem,
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    private val mediaRepository: MediaRepository,
    private val captureUiState: StateFlow<CaptureUiState>
) : CaptureScreenController {
    override fun setDisplayRotation(deviceRotation: DeviceRotation) {
        viewModelScope.launch {
            cameraSystem.setDeviceRotation(deviceRotation)
        }
    }

    override fun tapToFocus(x: Float, y: Float) {
        Log.d(TAG, "tapToFocus")
        viewModelScope.launch {
            cameraSystem.tapToFocus(x, y)
        }
    }

    override fun setAudioEnabled(shouldEnableAudio: Boolean) {
        viewModelScope.launch {
            cameraSystem.setAudioEnabled(shouldEnableAudio)
        }

        Log.d(
            TAG,
            "Toggle Audio: $shouldEnableAudio"
        )
    }

    override fun updateLastCapturedMedia() {
        viewModelScope.launch {
            trackedCaptureUiState.update { old ->
                old.copy(recentCapturedMedia = mediaRepository.getLastCapturedMedia())
            }
        }
    }

    override fun imageWellToRepository() {
        (captureUiState.value as? CaptureUiState.Ready)
            ?.let { it.imageWellUiState as? ImageWellUiState.LastCapture }
            ?.let {
                postCurrentMediaToMediaRepository(
                    viewModelScope,
                    mediaRepository,
                    it.mediaDescriptor
                )
            }
    }

    override fun setPaused(shouldBePaused: Boolean) {
        viewModelScope.launch {
            if (shouldBePaused) {
                cameraSystem.pauseVideoRecording()
            } else {
                cameraSystem.resumeVideoRecording()
            }
        }
    }
}
