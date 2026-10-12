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
package com.google.jetpackcamera.ui.debug

import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.model.TestPattern
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Implementation of [DebugController] that interacts with [CameraSystem] and updates
 * [trackedCaptureUiState].
 *
 * @param cameraSystemProvider Provider for the initialized [CameraSystem].
 * @param trackedCaptureUiState The state flow to update with debug information.
 * @param coroutineContext The [CoroutineContext] for launching coroutines.
 */
class DebugControllerImpl(
    private val cameraSystemProvider: suspend () -> CameraSystem,
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    coroutineContext: CoroutineContext = Dispatchers.Main.immediate
) : DebugController {
    private val job = Job(parent = coroutineContext[Job])
    private val scope = CoroutineScope(coroutineContext + job)

    override fun toggleDebugHidingComponents() {
        trackedCaptureUiState.update { old ->
            old.copy(debugHidingComponents = !old.debugHidingComponents)
        }
    }

    override fun toggleDebugOverlay() {
        trackedCaptureUiState.update { old ->
            old.copy(isDebugOverlayOpen = !old.isDebugOverlayOpen)
        }
    }

    override fun setTestPattern(testPattern: TestPattern) {
        scope.launch {
            cameraSystemProvider().setTestPattern(
                newTestPattern = testPattern
            )
        }
    }
}
