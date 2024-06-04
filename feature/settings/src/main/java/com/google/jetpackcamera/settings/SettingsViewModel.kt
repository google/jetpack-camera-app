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
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.Stabilization
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val TAG = "SettingsViewModel"

/**
 * [ViewModel] for [SettingsScreen].
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    constraintsRepository: ConstraintsRepository
) : ViewModel() {

    val settingsUiState: StateFlow<SettingsUiState> =
        combine(
            settingsRepository.defaultCameraAppSettings,
            constraintsRepository.systemConstraints.filterNotNull()
        ) { updatedSettings, constraints ->
            SettingsUiState.Enabled(
                cameraAppSettings = updatedSettings,
                systemConstraints = constraints
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Disabled
        )

    fun setDefaultLensFacing(lensFacing: LensFacing) {
        viewModelScope.launch {
            settingsRepository.updateDefaultLensFacing(lensFacing)
            Log.d(TAG, "set camera default facing: $lensFacing")
        }
    }

    fun setDarkMode(darkMode: DarkMode) {
        viewModelScope.launch {
            settingsRepository.updateDarkModeStatus(darkMode)
            Log.d(TAG, "set dark mode theme: $darkMode")
        }
    }

    fun setFlashMode(flashMode: FlashMode) {
        viewModelScope.launch {
            settingsRepository.updateFlashModeStatus(flashMode)
            Log.d(TAG, "set flash mode: $flashMode")
        }
    }

    fun setTargetFrameRate(targetFrameRate: Int) {
        viewModelScope.launch {
            settingsRepository.updateTargetFrameRate(targetFrameRate)
            Log.d(TAG, "set target frame rate: $targetFrameRate")
        }
    }

    fun setAspectRatio(aspectRatio: AspectRatio) {
        viewModelScope.launch {
            settingsRepository.updateAspectRatio(aspectRatio)
            Log.d(TAG, "set aspect ratio: $aspectRatio")
        }
    }

    fun setCaptureMode(captureMode: CaptureMode) {
        viewModelScope.launch {
            settingsRepository.updateCaptureMode(captureMode)
            Log.d(TAG, "set default capture mode: $captureMode")
        }
    }

    fun setPreviewStabilization(stabilization: Stabilization) {
        viewModelScope.launch {
            settingsRepository.updatePreviewStabilization(stabilization)
            Log.d(TAG, "set preview stabilization: $stabilization")
        }
    }

    fun setVideoStabilization(stabilization: Stabilization) {
        viewModelScope.launch {
            settingsRepository.updateVideoStabilization(stabilization)
            Log.d(TAG, "set video stabilization: $stabilization")
        }
    }
}
