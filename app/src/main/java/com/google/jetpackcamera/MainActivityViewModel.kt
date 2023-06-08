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

package com.google.jetpackcamera

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.Settings
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import com.google.jetpackcamera.MainActivityUiState.Success
import com.google.jetpackcamera.MainActivityUiState.Loading
import dagger.hilt.android.lifecycle.HiltViewModel

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel(){
    val uiState: StateFlow<MainActivityUiState> = settingsRepository.settings.map {
        Success(it)
    }.stateIn(
        scope = viewModelScope,
        initialValue = Loading,
        started = SharingStarted.WhileSubscribed(5_000)
    )
}
sealed interface MainActivityUiState {
    object Loading : MainActivityUiState
    data class Success(val settings: Settings) : MainActivityUiState
}

