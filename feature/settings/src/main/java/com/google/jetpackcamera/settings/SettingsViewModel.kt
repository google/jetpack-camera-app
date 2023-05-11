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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
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

    // stateflow from repository... can't emit values to this
    private val repositoryState =
        settingsRepository.settings.map { settings ->
            RepositoryState.Success(
                settings = DefaultSettings(
                    defaultFrontCamera = settings.default_front_camera,
                    darkModeStatus = settings.dark_mode_status
                )
            )
        }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = RepositoryState.Loading
            )


    val settingsUiState: MutableStateFlow<SettingsUiState> =
        when (repositoryState.value) {
            RepositoryState.Loading -> MutableStateFlow(
                SettingsUiState(
                    repositoryStatus = false,
                    DefaultSettings()
                )
            )

            is RepositoryState.Success -> MutableStateFlow(
                SettingsUiState(
                    repositoryStatus = true,
                    settings = (repositoryState.value as RepositoryState.Success).settings.copy()
                )
            )
        }
// suspend function

    fun setDefaultFrontCamera() {
        // true means default is front
        viewModelScope.launch {
            // update ui
            settingsUiState.emit(
                // update the ui
                settingsUiState.value.copy(
                    settings = settingsUiState.value.settings.copy(defaultFrontCamera = true),
                    disabled = true
                )
            )
            Log.d(
                TAG, "set ui default facing: " +
                        settingsUiState.value.settings.defaultFrontCamera
            )
            Log.d(
                TAG, "set camera default facing: " +
                        (repositoryState.value as RepositoryState.Success).settings.defaultFrontCamera
            )
            // todo run through camerausecase

            // after updating datastore, both should reset. disabled should be false,
            settingsRepository.updateDefaultFrontCamera()
            syncUiStateToRepository()
        }
    }


    fun setDarkMode(darkModeStatus: DarkModeStatus) {
        viewModelScope.launch {
            settingsRepository.updateDarkModeStatus(darkModeStatus)
            syncUiStateToRepository()
        }
    }

    private suspend fun syncUiStateToRepository() {
        // update UI
        settingsUiState.emit(
            settingsUiState.value.copy(
                settings = (repositoryState.value as RepositoryState.Success).settings.copy(),
                disabled = false
            )
        )
    }
}

// class to show what settings the user can set to default
data class DefaultSettings(
    var defaultFrontCamera: Boolean = false,
    val darkModeStatus: DarkModeStatus = DarkModeStatus.SYSTEM
)
