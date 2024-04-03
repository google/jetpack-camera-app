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

import com.google.jetpackcamera.domain.camera.CameraUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private const val TAG = "ScreenFlash"

/**
 * Contains the UI state maintaining logic for screen flash feature.
 */
// TODO: Add this to ViewModelScoped so that it can be injected automatically. However, the current
//  ViewModel and Hilt APIs probably don't support injecting the viewModelScope.
class ScreenFlash(
    cameraUseCase: CameraUseCase,
    scope: CoroutineScope
) {
    private var screenBrightnessToRestore: Float? = null

    val screenFlashUiState: StateFlow<ScreenFlashUiState> =
        cameraUseCase.getScreenFlashEvents().map { event ->
            when (event.type) {
                CameraUseCase.ScreenFlashEvent.Type.APPLY_UI ->
                    ScreenFlashUiState.Applied(onComplete = event.onComplete)
                CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI -> {
                    ScreenFlashUiState.NotApplied(
                        screenBrightnessToRestore = screenBrightnessToRestore
                    )
                }
            }
        }.stateIn(
            scope = scope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ScreenFlashUiState.NotApplied()
        )

    /**
     * Set the screen brightness to restore to after a screen flash has been applied.
     */
    fun setClearUiScreenBrightness(brightness: Float) {
        screenBrightnessToRestore = brightness
    }
}

sealed interface ScreenFlashUiState {
    data class Applied(val onComplete: () -> Unit) : ScreenFlashUiState
    data class NotApplied(val screenBrightnessToRestore: Float? = null) : ScreenFlashUiState
}
