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

package com.google.jetpackcamera.feature.quicksettings

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.SettingsViewModel
import com.google.jetpackcamera.settings.model.QuickSettings
import com.google.jetpackcamera.settings.model.getDefaultQuickSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

//@HiltViewModel
//class QuickSettingsViewModel @Inject constructor(
//    private val settingsViewModel: SettingsViewModel
//) : ViewModel() {
//    private val _quickSettingsUiState: MutableStateFlow<QuickSettingsUiState> = MutableStateFlow(
//        QuickSettingsUiState(
//            getDefaultQuickSettings(),
//        )
//    )
//    val quickSettingsUiState: StateFlow<QuickSettingsUiState> = _quickSettingsUiState
//
//    init {
//        viewModelScope.launch {
//            _quickSettingsUiState.emit(
//                quickSettingsUiState.value.copy(
//                    quickSettings = QuickSettings(
//                        default_front_camera =
//                        settingsViewModel.settingsUiState.value.settings.default_front_camera
//                    ),
//                )
//            )
//        }
//    }
//
//    fun setDefaultToFrontCamera() {
//        settingsViewModel.setDefaultToFrontCamera()
//    }
//
//}