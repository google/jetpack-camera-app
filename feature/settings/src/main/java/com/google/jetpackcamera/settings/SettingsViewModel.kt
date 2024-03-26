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
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.Stabilization
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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
        // updates our view model as soon as datastore is updated
        viewModelScope.launch {
            settingsRepository.cameraAppSettings.collect { updatedSettings ->
                _settingsUiState.emit(
                    settingsUiState.value.copy(
                        cameraAppSettings = updatedSettings,
                        disabled = false
                    )
                )

                Log.d(
                    TAG,
                    "updated setting ${settingsRepository.getCameraAppSettings().captureMode}"
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

    fun setDefaultLensFacing(lensFacing: LensFacing) {
        viewModelScope.launch {
            settingsRepository.updateDefaultLensFacing(lensFacing)
            Log.d(
                TAG,
                "set camera default facing: " +
                    "${settingsRepository.getCameraAppSettings().cameraLensFacing}"
            )
        }
    }

    fun setDarkMode(darkMode: DarkMode) {
        viewModelScope.launch {
            settingsRepository.updateDarkModeStatus(darkMode)
            Log.d(
                TAG,
                "set dark mode theme: ${settingsRepository.getCameraAppSettings().darkMode}"
            )
        }
    }

    fun setFlashMode(flashMode: FlashMode) {
        viewModelScope.launch {
            settingsRepository.updateFlashModeStatus(flashMode)
        }
    }

    fun setTargetFrameRate(targetFrameRate: Int) {
        viewModelScope.launch {
            settingsRepository.updateTargetFrameRate(targetFrameRate)
        }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        viewModelScope.launch {
            settingsRepository.updateAspectRatio(aspectRatio)
            Log.d(
                TAG,
                "set aspect ratio: " +
                    "${settingsRepository.getCameraAppSettings().aspectRatio}"
            )
        }
    }

    fun setCaptureMode(captureMode: CaptureMode) {
        viewModelScope.launch {
            settingsRepository.updateCaptureMode(captureMode)

            Log.d(
                TAG,
                "set default capture mode: " +
                    "${settingsRepository.getCameraAppSettings().captureMode}"
            )
        }
    }

    fun setPreviewStabilization(stabilization: Stabilization) {
        viewModelScope.launch {
            settingsRepository.updatePreviewStabilization(stabilization)

            Log.d(
                TAG,
                "set preview stabilization: " +
                    "${settingsRepository.getCameraAppSettings().previewStabilization}"
            )
        }
    }

    fun setVideoStabilization(stabilization: Stabilization) {
        viewModelScope.launch {
            settingsRepository.updateVideoStabilization(stabilization)

            Log.d(
                TAG,
                "set video stabilization: " +
                    "${settingsRepository.getCameraAppSettings().previewStabilization}"
            )
        }
    }
}
