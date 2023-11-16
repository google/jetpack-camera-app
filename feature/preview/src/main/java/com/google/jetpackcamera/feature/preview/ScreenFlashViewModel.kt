/*
 * Copyright (C) 2023 The Android Open Source Project
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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetpackcamera.domain.camera.CameraUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ScreenFlashViewModel"

/**
 * [ViewModel] for [ScreenFlash].
 */
@HiltViewModel
class ScreenFlashViewModel @Inject constructor(
    private val cameraUseCase: CameraUseCase,
) : ViewModel() {
    data class ScreenFlashUiState(
        val enabled: Boolean = false,
        val onChangeComplete: () -> Unit = {},
        val screenBrightnessToRestore: Float? = null, // restored during CLEAR_UI event
    )

    private val _screenFlashUiState: MutableStateFlow<ScreenFlashUiState> =
        MutableStateFlow(ScreenFlashUiState())
    val screenFlashUiState: StateFlow<ScreenFlashUiState> = _screenFlashUiState

    init {
        viewModelScope.launch {
            cameraUseCase.getScreenFlashEvents().collect { event ->
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
                                    viewModelScope.launch {
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
     * CLEAR_UI event, will be set to unknown (null) again after CLEAR_UI event is completed
     */
    fun setClearUiScreenBrightness(brightness: Float) {
        viewModelScope.launch {
            _screenFlashUiState.emit(
                screenFlashUiState.value.copy(screenBrightnessToRestore = brightness)
            )
        }
    }
}
