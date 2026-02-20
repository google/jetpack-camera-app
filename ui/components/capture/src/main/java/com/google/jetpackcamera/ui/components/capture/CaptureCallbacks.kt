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
package com.google.jetpackcamera.ui.components.capture

import android.util.Log
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.ui.uistate.capture.ImageWellUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * This file contains the data class [CaptureCallbacks] and helper functions to create it.
 * [CaptureCallbacks] is used to handle UI events on the capture screen.
 */

private const val TAG = "CaptureCallbacks"

/**
 * Data class holding callbacks for capture-related UI events.
 *
 * @param setDisplayRotation Sets the display rotation for the camera.
 * @param tapToFocus Initiates a tap-to-focus action at the given coordinates.
 * @param changeZoomRatio Changes the camera's zoom ratio.
 * @param setZoomAnimationState Sets the target for the zoom animation.
 * @param setAudioEnabled Toggles audio recording.
 * @param setLockedRecording Locks or unlocks the recording.
 * @param updateLastCapturedMedia Updates the UI with the most recently captured media.
 * @param imageWellToRepository Posts the media from the image well to the media repository.
 * @param setPaused Pauses or resumes video recording.
 */
data class CaptureCallbacks(
    val setDisplayRotation: (DeviceRotation) -> Unit,
    val tapToFocus: (Float, Float) -> Unit,
    val changeZoomRatio: (CameraZoomRatio) -> Unit,
    val setZoomAnimationState: (Float?) -> Unit,
    val setAudioEnabled: (Boolean) -> Unit,
    val setLockedRecording: (Boolean) -> Unit,
    val updateLastCapturedMedia: () -> Unit,
    val imageWellToRepository: () -> Unit,
    val setPaused: (Boolean) -> Unit
)

/**
 * Creates a [CaptureCallbacks] instance with implementations that interact with the camera system
 * and update the UI state.
 *
 * @param viewModelScope The [CoroutineScope] for launching coroutines.
 * @param cameraSystem The [CameraSystem] to interact with the camera hardware.
 * @param trackedCaptureUiState The mutable state flow for the tracked capture UI state.
 * @param mediaRepository The [MediaRepository] for accessing media.
 * @param captureUiState The state flow for the overall capture UI state.
 * @return An instance of [CaptureCallbacks].
 */
fun getCaptureCallbacks(
    viewModelScope: CoroutineScope,
    cameraSystem: CameraSystem,
    trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    mediaRepository: MediaRepository,
    captureUiState: StateFlow<CaptureUiState>
): CaptureCallbacks {
    return CaptureCallbacks(
        setDisplayRotation = { deviceRotation ->
            viewModelScope.launch {
                cameraSystem.setDeviceRotation(deviceRotation)
            }
        },
        tapToFocus = { x, y ->
            Log.d(TAG, "tapToFocus")
            viewModelScope.launch {
                cameraSystem.tapToFocus(x, y)
            }
        },
        changeZoomRatio = { newZoomState ->
            cameraSystem.changeZoomRatio(
                newZoomState = newZoomState
            )
        },
        setZoomAnimationState = { targetValue ->
            trackedCaptureUiState.update { old ->
                old.copy(zoomAnimationTarget = targetValue)
            }
        },
        setAudioEnabled = { shouldEnableAudio ->
            viewModelScope.launch {
                cameraSystem.setAudioEnabled(shouldEnableAudio)
            }

            Log.d(
                TAG,
                "Toggle Audio: $shouldEnableAudio"
            )
        },
        setLockedRecording = { isLocked ->
            trackedCaptureUiState.update { old ->
                old.copy(isRecordingLocked = isLocked)
            }
        },
        updateLastCapturedMedia = {
            viewModelScope.launch {
                trackedCaptureUiState.update { old ->
                    old.copy(recentCapturedMedia = mediaRepository.getLastCapturedMedia())
                }
            }
        },
        imageWellToRepository = {
            (captureUiState.value as? CaptureUiState.Ready)
                ?.let { it.imageWellUiState as? ImageWellUiState.LastCapture }
                ?.let {
                    postCurrentMediaToMediaRepository(
                        viewModelScope,
                        mediaRepository,
                        it.mediaDescriptor
                    )
                }
        },
        setPaused = { shouldBePaused ->
            viewModelScope.launch {
                if (shouldBePaused) {
                    cameraSystem.pauseVideoRecording()
                } else {
                    cameraSystem.resumeVideoRecording()
                }
            }
        }
    )
}

/**
 * Posts the given [MediaDescriptor] to the [MediaRepository] as the current media.
 *
 * @param viewModelScope The [CoroutineScope] for launching the coroutine.
 * @param mediaRepository The repository to update.
 * @param mediaDescriptor The media to set as current.
 */
fun postCurrentMediaToMediaRepository(
    viewModelScope: CoroutineScope,
    mediaRepository: MediaRepository,
    mediaDescriptor: MediaDescriptor
) {
    viewModelScope.launch {
        mediaRepository.setCurrentMedia(mediaDescriptor)
    }
}
