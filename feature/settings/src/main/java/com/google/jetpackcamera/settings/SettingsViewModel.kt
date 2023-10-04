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

package com.google.jetpackcamera.settings

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.DarkModeStatus
import com.google.jetpackcamera.settings.model.FlashModeStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SettingsViewModel"


/**
 * [ViewModel] for [SettingsScreen].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _settingsUiState: MutableStateFlow<SettingsUiState> =
        MutableStateFlow(
            SettingsUiState(
                DEFAULT_CAMERA_APP_SETTINGS,
                disabled = true
            )
        )
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState

    init {
        // updates our viewmodel as soon as datastore is updated
        viewModelScope.launch {
            settingsRepository.cameraAppSettings.collect { updatedSettings ->
                _settingsUiState.emit(
                    settingsUiState.value.copy(
                        cameraAppSettings = updatedSettings,
                        disabled = false
                    )
                )
            }
        }
        viewModelScope.launch {
            _settingsUiState.emit(
                settingsUiState.value.copy(
                    disabled = false
                )
            )
        }
    }

    fun setDefaultToFrontCamera() {
        // true means default is front
        viewModelScope.launch {
            settingsRepository.updateDefaultToFrontCamera()
            Log.d(
                TAG,
                "set camera default facing: " + settingsRepository.getCameraAppSettings().isFrontCameraFacing
            )
        }
    }


    fun setDarkMode(darkModeStatus: DarkModeStatus) {
        viewModelScope.launch {
            settingsRepository.updateDarkModeStatus(darkModeStatus)
            Log.d(
                TAG, "set dark mode theme: " + settingsRepository.getCameraAppSettings().darkMode
            )
        }
    }

    fun setFlashMode(flashModeStatus: FlashModeStatus) {
        viewModelScope.launch {
            settingsRepository.updateFlashModeStatus(flashModeStatus)
        }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        viewModelScope.launch {
            settingsRepository.updateAspectRatio(aspectRatio)
            Log.d(TAG, "set aspect ratio ${settingsRepository.getCameraAppSettings().aspectRatio}")
        }
    }
}

