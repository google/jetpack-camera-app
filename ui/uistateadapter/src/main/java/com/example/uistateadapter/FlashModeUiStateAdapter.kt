/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.example.uistateadapter

import com.google.jetpackcamera.core.camera.CameraUseCase
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LowLightBoostState
import com.google.jetpackcamera.settings.model.forCurrentLens
import com.google.jetpackcamera.ui.uistate.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.UiSingleSelectableState
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ViewModelScoped
class FlashModeUiStateAdapter @Inject constructor(
    private val cameraUseCase: CameraUseCase,
    private val constraintsRepository: ConstraintsRepository,
    private val externalScope: CoroutineScope,
) : UiStateAdapter<FlashModeUiState> {

    private val _uiStates = MutableStateFlow<FlashModeUiState>(FlashModeUiState.Unavailable)
    private val uiStates = _uiStates.asStateFlow()

    init {
        externalScope.launch {
            combine(
                cameraUseCase.getCurrentSettings().filterNotNull(),
                constraintsRepository.systemConstraints.filterNotNull(),
            ) { cameraAppSettings, systemConstraints ->
                val selectedFlashMode = cameraAppSettings.flashMode
                val supportedFlashModes = systemConstraints.forCurrentLens(cameraAppSettings)
                    ?.supportedFlashModes
                    ?: setOf(FlashMode.OFF)

                _uiStates.update {
                    FlashModeUiState.createFrom(
                        selectedFlashMode = selectedFlashMode,
                        supportedFlashModes = supportedFlashModes
                    )
                }
            }.collect { }
        }
    }


    override fun getUiStates(): StateFlow<FlashModeUiState> = uiStates

    fun update(
        flashMode: FlashMode,
        supportedFlashModes: Set<FlashMode>?,
        lowLightBoostState: LowLightBoostState
    ) {
        _uiStates.update { oldUiState ->
            when (oldUiState) {
                is FlashModeUiState.Available -> {
                    val oldFlashModesSet = oldUiState.availableFlashModes
                        .filterIsInstance<UiSingleSelectableState.Selectable<FlashMode>>()
                        .map { it.value }.toSet()

                    if (oldFlashModesSet != supportedFlashModes) {
                        // Supported flash modes have changed, generate a new FlashModeUiState
                        FlashModeUiState.createFrom(
                            selectedFlashMode = flashMode,
                            supportedFlashModes = supportedFlashModes ?: setOf(FlashMode.OFF)
                        )
                    } else if (oldUiState.selectedFlashMode != flashMode) {
                        oldUiState.copy(selectedFlashMode = flashMode)
                    } else {
                        if (flashMode == FlashMode.LOW_LIGHT_BOOST) {
                            oldUiState.copy(
                                isActive = lowLightBoostState == LowLightBoostState.ACTIVE
                            )
                        } else {
                            oldUiState
                        }
                    }
                }

                is FlashModeUiState.Unavailable -> {
                    FlashModeUiState.createFrom(
                        selectedFlashMode = flashMode,
                        supportedFlashModes = supportedFlashModes ?: setOf(FlashMode.OFF)
                    )
                }
            }
        }
    }
}