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
import com.google.jetpackcamera.settings.model.DarkModeStatus
import com.google.jetpackcamera.settings.model.getDefaultSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
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

    val settingsUiState: MutableStateFlow<SettingsUiState> =
        MutableStateFlow(
            SettingsUiState(
                getDefaultSettings(),
                disabled = true
            )
        )

    init {
        // updates our viewmodel as soon as datastore is updated
        viewModelScope.launch {
            settingsRepository.settings.collect { updatedSettings ->
                settingsUiState.emit(
                    settingsUiState.value.copy(
                        settings = updatedSettings,
                        disabled = false
                    )
                )
            }
        }
    }

    fun setDefaultToFrontCamera() {
        // true means default is front
        viewModelScope.launch {
            settingsRepository.updateDefaultFrontCamera()
            Log.d(
                TAG,
                "set camera default facing: " + settingsRepository.getSettings().default_front_camera
            )
        }
    }


    fun setDarkMode(darkModeStatus: DarkModeStatus) {
        viewModelScope.launch {
            settingsRepository.updateDarkModeStatus(darkModeStatus)
            Log.d(
                TAG, "set dark mode theme: " + settingsRepository.getSettings().dark_mode_status
            )
        }
    }
}

