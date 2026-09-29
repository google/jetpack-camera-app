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
package com.google.jetpackcamera.ui.controller.impl

import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.model.CameraZoomRatio
import com.google.jetpackcamera.ui.controller.ZoomController
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Implementation of [ZoomController] that updates the camera's zoom and tracked UI state.
 *
 * @param cameraSystemProvider Provider for the initialized [CameraSystem].
 * @param trackedCaptureUiState State for tracking zoom changes.
 * @param coroutineContext The [CoroutineContext] for launching coroutines.
 */
class ZoomControllerImpl(
    private val cameraSystemProvider: suspend () -> CameraSystem,
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    coroutineContext: CoroutineContext = Dispatchers.Main.immediate
) : ZoomController {
    private val job = Job(parent = coroutineContext[Job])
    private val scope = CoroutineScope(coroutineContext + job)

    override fun setZoomRatio(zoomRatio: CameraZoomRatio) {
        scope.launch {
            cameraSystemProvider().changeZoomRatio(
                newZoomState = zoomRatio
            )
        }
    }

    override fun setZoomAnimationState(targetValue: Float?) {
        trackedCaptureUiState.update { old ->
            old.copy(zoomAnimationTarget = targetValue)
        }
    }
}
