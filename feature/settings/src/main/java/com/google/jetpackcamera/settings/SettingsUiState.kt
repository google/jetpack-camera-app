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

import com.google.jetpackcamera.settings.model.Settings

private const val TAG = "SettingsUiState"

/**
 * Defines the current state of the [SettingsScreen].
 */
data class SettingsUiState(
    val repositoryStatus: Boolean,
    val settings: Settings,
    var disabled: Boolean = false
)


sealed interface RepositoryState {
    object Loading : RepositoryState
    data class Success(val settings: Settings) : RepositoryState
}

