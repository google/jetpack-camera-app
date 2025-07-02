/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview

import com.google.jetpackcamera.core.camera.CameraUseCase
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

private const val TAG = "ScreenFlash"

/**
 * Contains the UI state maintaining logic for screen flash feature.
 */
// TODO: Add this to ViewModelScoped so that it can be injected automatically. However, the current
//  ViewModel and Hilt APIs probably don't support injecting the viewModelScope.
class ScreenFlash(
    private val cameraUseCase: CameraUseCase,
    private val scope: CoroutineScope
) {

    private val _screenFlashUiState: MutableStateFlow<ScreenFlashUiState> =
        MutableStateFlow(ScreenFlashUiState())
    val screenFlashUiState: StateFlow<ScreenFlashUiState> = _screenFlashUiState

    init {
        scope.launch {
            for (event in cameraUseCase.getScreenFlashEvents()) {
                _screenFlashUiState.emit(
                    when (event.type) {
                        CameraUseCase.ScreenFlashEvent.Type.APPLY_UI ->
                            screenFlashUiState.value.copy(
                                enabled = true,
                                onChangeComplete = event.onComplete
                            )

                        CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI ->
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

    /**
     * Sets the screenBrightness value to the value right before APPLY_UI event for the next
     * CLEAR_UI event, will be set to unknown (null) again after CLEAR_UI event is completed.
     */
    fun setClearUiScreenBrightness(brightness: Float) {
        scope.launch {
            _screenFlashUiState.emit(
                screenFlashUiState.value.copy(screenBrightnessToRestore = brightness)
            )
        }
    }
}
