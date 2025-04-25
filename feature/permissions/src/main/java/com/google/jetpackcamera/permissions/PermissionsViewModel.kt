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

import android.Manifest
import android.os.Build
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.shouldShowRationale
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlin.collections.removeFirst as ktRemoveFirst // alias must be used now. see https://issuetracker.google.com/348683480
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * A [ViewModel] for [PermissionsScreen]]
 */
@OptIn(ExperimentalPermissionsApi::class)
@HiltViewModel(assistedFactory = PermissionsViewModel.Factory::class)
class PermissionsViewModel @AssistedInject constructor(
    @Assisted permissionStates: MultiplePermissionsState
) : ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(runtimeArg: MultiplePermissionsState): PermissionsViewModel
    }

    private var permissionQueue = mutableListOf<PermissionEnum>()

    init {
        permissionQueue.addAll(getRequestablePermissions(permissionStates))
    }

    private val _permissionsUiState: MutableStateFlow<PermissionsUiState> =
        MutableStateFlow(getCurrentPermission())
    val permissionsUiState: StateFlow<PermissionsUiState> = _permissionsUiState.asStateFlow()

    private fun getCurrentPermission(): PermissionsUiState {
        return if (permissionQueue.isEmpty()) {
            PermissionsUiState.AllPermissionsGranted
        } else {
            PermissionsUiState.PermissionsNeeded(permissionQueue.first())
        }
    }

    fun dismissPermission() {
        if (permissionQueue.isNotEmpty()) {
            permissionQueue.ktRemoveFirst()
        }
        _permissionsUiState.update {
            (getCurrentPermission())
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
fun getRequestablePermissions(permissionStates: MultiplePermissionsState): Set<PermissionEnum> {
    val unGrantedPermissions = buildSet {
        permissionStates.permissions.forEach { permissionState ->
            when (permissionState.permission) {
                // camera is always required
                Manifest.permission.CAMERA -> {
                    if (!permissionState.status.isGranted) {
                        add(PermissionEnum.CAMERA)
                    }
                }

                // optional permissions
                Manifest.permission.RECORD_AUDIO -> {
                    if (!permissionState.status.shouldShowRationale &&
                        !permissionState.status.isGranted
                    ) {
                        add(PermissionEnum.RECORD_AUDIO)
                    }
                }

                Manifest.permission.READ_EXTERNAL_STORAGE -> {
                    if (!permissionState.status.shouldShowRationale &&
                        !permissionState.status.isGranted &&
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                    ) {
                        add(PermissionEnum.READ_STORAGE)
                    }
                }

                Manifest.permission.WRITE_EXTERNAL_STORAGE -> {
                    if (!permissionState.status.shouldShowRationale &&
                        !permissionState.status.isGranted &&
                        Build.VERSION.SDK_INT <= Build.VERSION_CODES.P
                    ) {
                        add(PermissionEnum.WRITE_STORAGE)
                    }
                }
            }
        }
    }
    return unGrantedPermissions
}
