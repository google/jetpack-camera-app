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
import com.google.jetpackcamera.permissions.data.PermissionsRepository
import com.google.jetpackcamera.permissions.navigation.getRequestablePermissions
import com.google.jetpackcamera.settings.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * A [ViewModel] for [PermissionsScreen]]
 */
@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel()
class PermissionsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val settingsRepository: SettingsRepository,
    private val permissionsRepository: PermissionsRepository
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

    private var lastLocationGranted: Boolean? = null
    private val dismissedPermissions = mutableSetOf<PermissionEnum>()
    private var requestedPermissions = emptySet<String>()
    private var latestPermissionsState: MultiplePermissionsState? = null

    init {
        viewModelScope.launch {
            permissionsRepository.requestedPermissions.collect { requested ->
                requestedPermissions = requested
                latestPermissionsState?.let { state ->
                    recomputeQueue(state)
                }
            }
        }
    }

    /**
     * Advances past [permission].
     *
     * The permission is removed from the current queue. An optional permission is also excluded
     * for the rest of this session and recorded as requested in [PermissionsRepository] so it is
     * not shown again. A mandatory permission is only removed from the queue, so it is requested
     * again if it is not granted.
     *
     * @param permission The permission to advance past.
     */
    internal fun dismissPermission(permission: PermissionEnum) {
        if (permission.isOptional()) {
            dismissedPermissions.add(permission)
            viewModelScope.launch {
                permissionsRepository.markPermissionRequested(permission.name)
            }
        }
        permissionQueue.update { queue ->
            queue.filter { it != permission }
        }
    }

    fun updatePermissionStates(multiplePermissionsState: MultiplePermissionsState) {
        latestPermissionsState = multiplePermissionsState
        val isLocationGranted = multiplePermissionsState.permissions.any {
            it.permission in PermissionEnum.LOCATION.getPermissions() && it.status.isGranted
        }

        if (lastLocationGranted == false && isLocationGranted) {
            viewModelScope.launch {
                settingsRepository.updateLocationEnabled(true)
            }
        }
        lastLocationGranted = isLocationGranted

        recomputeQueue(multiplePermissionsState)
    }

    private fun recomputeQueue(multiplePermissionsState: MultiplePermissionsState) {
        permissionQueue.update {
            getRequestablePermissions(
                multiplePermissionsState,
                requestedPermissions
            ).filter { it !in dismissedPermissions }
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
internal fun getRequestablePermissions(
    permissionStates: MultiplePermissionsState,
    requestedPermissions: Set<String> = emptySet()
): List<PermissionEnum> = permissionStates.permissions
    .map { PermissionEnum.fromString(it.permission) }
    .distinct()
    .filter { permission ->
        val componentStates = permissionStates.permissions.filter {
            it.permission in permission.getPermissions()
        }
        val isAnyGranted = componentStates.any { it.status.isGranted }
        when {
            isAnyGranted -> false
            !permission.isOptional() -> true
            else -> {
                val wasPreviouslyHandled =
                    permission.name in requestedPermissions ||
                        componentStates.any { it.status.shouldShowRationale }
                !wasPreviouslyHandled
            }
        }
    }
