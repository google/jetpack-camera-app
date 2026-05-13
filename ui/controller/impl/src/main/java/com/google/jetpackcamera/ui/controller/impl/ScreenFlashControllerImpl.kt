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
import com.google.jetpackcamera.ui.controller.ScreenFlashController
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Implementation of [ScreenFlashController] that handles screen flash actions.
 *
 * @param cameraSystem The [CameraSystem] for accessing camera events.
 * @param trackedCaptureUiState The [MutableStateFlow] of [TrackedCaptureUiState] to update the flash state.
 * @param coroutineContext The [CoroutineContext] for launching coroutines.
 */
class ScreenFlashControllerImpl(
    private val cameraSystem: CameraSystem,
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    coroutineContext: CoroutineContext
) : ScreenFlashController {
    private val job = Job(parent = coroutineContext[Job.Key])
    private val scope = CoroutineScope(coroutineContext + job)

    init {
        scope.launch {
            for (event in cameraSystem.getScreenFlashEvents()) {
                trackedCaptureUiState.update { old ->
                    val oldFlashState = old.screenFlashUiState
                    old.copy(
                        screenFlashUiState = when (event.type) {
                            CameraSystem.ScreenFlashEvent.Type.APPLY_UI ->
                                oldFlashState.copy(
                                    enabled = true,
                                    onChangeComplete = event.onComplete
                                )

                            CameraSystem.ScreenFlashEvent.Type.CLEAR_UI ->
                                oldFlashState.copy(
                                    enabled = false,
                                    onChangeComplete = {
                                        event.onComplete()
                                        // reset ui state on CLEAR_UI event completion
                                        scope.launch {
                                            trackedCaptureUiState.update { o ->
                                                o.copy(screenFlashUiState = ScreenFlashUiState())
                                            }
                                        }
                                    }
                                )
                        }
                    )
                }
            }
        }
    }

    override fun setClearUiScreenBrightness(brightness: Float) {
        trackedCaptureUiState.update { old ->
            old.copy(
                screenFlashUiState = old.screenFlashUiState.copy(
                    screenBrightnessToRestore = brightness
                )
            )
        }
    }

    /**
     * Initiates the cancellation of this controller's scope and returns its Job.
     * To wait for cancellation to complete, call .join() on the returned Job.
     */
    fun cancelScope(): Job {
        scope.cancel()
        return scope.coroutineContext.job
    }
}
