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
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Implementation of [ScreenFlashController] that handles screen flash actions.
 *
 * @param cameraSystem The [CameraSystem] for accessing camera events.
 * @param coroutineContext The [CoroutineContext] for launching coroutines.
 */
class ScreenFlashControllerImpl(
    private val cameraSystem: CameraSystem,
    coroutineContext: CoroutineContext
) : ScreenFlashController {
    private val job = Job(parent = coroutineContext[Job.Key])
    private val scope = CoroutineScope(coroutineContext + job)

    private val _screenFlashUiState: MutableStateFlow<ScreenFlashUiState> =
        MutableStateFlow(ScreenFlashUiState())
    override val screenFlashUiState: StateFlow<ScreenFlashUiState> = _screenFlashUiState

    init {
        scope.launch {
            for (event in cameraSystem.getScreenFlashEvents()) {
                _screenFlashUiState.emit(
                    when (event.type) {
                        CameraSystem.ScreenFlashEvent.Type.APPLY_UI ->
                            screenFlashUiState.value.copy(
                                enabled = true,
                                onChangeComplete = event.onComplete
                            )

                        CameraSystem.ScreenFlashEvent.Type.CLEAR_UI ->
                            screenFlashUiState.value.copy(
                                enabled = false,
                                onChangeComplete = {
                                    event.onComplete()
                                    // reset ui state on CLEAR_UI event completion
                                    scope.launch {
                                        _screenFlashUiState.emit(
                                            ScreenFlashUiState()
                                        )
                                    }
                                }
                            )
                    }
                )
            }
        }
    }

    override fun setClearUiScreenBrightness(brightness: Float) {
        scope.launch {
            _screenFlashUiState.emit(
                screenFlashUiState.value.copy(screenBrightnessToRestore = brightness)
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
