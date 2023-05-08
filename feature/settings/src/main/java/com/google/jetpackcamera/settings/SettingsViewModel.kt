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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
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
    private val _settingsUiState =
        settingsRepository.settings.map {
            settings ->
            SettingsUiState.Success(
                settings = DefaultSettings(
                    defaultFrontCamera = settings.default_front_camera,
                    darkModeStatus = settings.dark_mode_status
                )
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = SettingsUiState.Loading
            )
    val settingsUiState: StateFlow<SettingsUiState> = _settingsUiState


    fun setDefaultFrontCamera(){
        // true means default is front
        //todo run through camerausecase
        viewModelScope.launch {
            settingsRepository.updateDefaultFrontCamera()
        }
        Log.d(TAG, "set camera default facing: " +
                (_settingsUiState.value as SettingsUiState.Success).settings.defaultFrontCamera)
    }

    fun setDarkMode(darkModeStatus: DarkModeStatus){
        viewModelScope.launch {
            settingsRepository.updateDarkModeStatus(darkModeStatus)
        }
    }
}

// class to show what settings the user can set to default
data class DefaultSettings(
    val defaultFrontCamera: Boolean,
    val darkModeStatus: DarkModeStatus
)
