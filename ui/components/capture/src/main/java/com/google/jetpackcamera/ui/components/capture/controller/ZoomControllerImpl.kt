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

import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class ZoomControllerImpl(
    private val cameraSystem: CameraSystem,
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>
) : ZoomController {
    override fun setZoomRatio(zoomRatio: CameraZoomRatio) {
        cameraSystem.changeZoomRatio(
            newZoomState = zoomRatio
        )
    }

    override fun setZoomAnimationState(targetValue: Float?) {
        trackedCaptureUiState.update { old ->
            old.copy(zoomAnimationTarget = targetValue)
        }
    }
}
