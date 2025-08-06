/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.permissions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import com.google.jetpackcamera.permissions.navigation.getRequestablePermissions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

/**
 * A [ViewModel] for [PermissionsScreen]]
 */
@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel()
class PermissionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    // Initialize required permissions from savedStateHandle. Assume all permissions are not yet
    // granted.
    private var permissionQueue = MutableStateFlow(savedStateHandle.getRequestablePermissions())
    val permissionsUiState: StateFlow<PermissionsUiState> =
        permissionQueue.map { permissionQueue ->
            permissionQueue.toUiState()
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = permissionQueue.value.toUiState()
        )
    private fun List<PermissionEnum>.toUiState(): PermissionsUiState = if (isEmpty()) {
        PermissionsUiState.AllPermissionsGranted
    } else {
        PermissionsUiState.PermissionsNeeded(first())
    }

    fun updatePermissionStates(multiplePermissionsState: MultiplePermissionsState) {
        permissionQueue.update {
            getRequestablePermissions(multiplePermissionsState)
        }
    }
}

/**
 *
 * Provides a set of [PermissionEnum] representing the permissions that can still be requested.
 * Permissions that can be requested are:
 * - mandatory permissions that have not been granted
 * - optional permissions that have not yet been denied by the user
 */
@OptIn(ExperimentalPermissionsApi::class)
fun getRequestablePermissions(permissionStates: MultiplePermissionsState): List<PermissionEnum> =
    buildList {
        permissionStates.permissions.forEach { permissionState ->
            val permission = PermissionEnum.fromString(permissionState.permission)
            if (!permissionState.status.isGranted) {
                if (!permission.isOptional() || !permissionState.status.shouldShowRationale) {
                    add(permission)
                }
            }
        }
    }
