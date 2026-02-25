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

import android.os.SystemClock
import android.util.Log
import androidx.lifecycle.viewModelScope
import androidx.tracing.Trace
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.common.traceFirstFramePreview
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.transformWhile
import kotlinx.coroutines.launch

private const val TAG = "CameraControllerImpl"

/**
 * Implementation of [CameraController] that manages the camera lifecycle.
 *
 * @param initializationDeferred A [Deferred] that completes when the camera system is initialized.
 * @param captureUiState The [StateFlow] of the capture UI state.
 * @param viewModelScope The [CoroutineScope] for launching coroutines.
 * @param cameraSystem The [CameraSystem] to interact with.
 */
class CameraControllerImpl(
    private val initializationDeferred: Deferred<Unit>,
    private val captureUiState: StateFlow<CaptureUiState>,
    private val viewModelScope: CoroutineScope,
    private val cameraSystem: CameraSystem
) : CameraController {
    private var runningCameraJob: Job? = null

    override fun startCamera() {
        Log.d(TAG, "startCamera")
        stopCamera()
        runningCameraJob = viewModelScope.launch {
            if (Trace.isEnabled()) {
                launch(start = CoroutineStart.UNDISPATCHED) {
                    val startTraceTimestamp: Long = SystemClock.elapsedRealtimeNanos()
                    traceFirstFramePreview(cookie = 1) {
                        captureUiState.transformWhile {
                            var continueCollecting = true
                            (it as? CaptureUiState.Ready)?.let { uiState ->
                                if (uiState.sessionFirstFrameTimestamp > startTraceTimestamp) {
                                    emit(Unit)
                                    continueCollecting = false
                                }
                            }
                            continueCollecting
                        }.collect {}
                    }
                }
            }
            // Ensure CameraSystem is initialized before starting camera
            initializationDeferred.await()
            // TODO(yasith): Handle Exceptions from binding use cases
            cameraSystem.runCamera()
        }
    }

    override fun stopCamera() {
        Log.d(TAG, "stopCamera")
        runningCameraJob?.apply {
            if (isActive) {
                cancel()
            }
        }
    }
}
