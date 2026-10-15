/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.jetpackcamera.permissions.data.PermissionsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

@OptIn(ExperimentalPermissionsApi::class)
class FakeMultiplePermissionsState(
    override val permissions: List<PermissionState> = emptyList(),
    override val allPermissionsGranted: Boolean = false,
    override val revokedPermissions: List<PermissionState> = emptyList(),
    override val shouldShowRationale: Boolean = false,
    private val onLaunch: () -> Unit = {}
) : MultiplePermissionsState {
    override fun launchMultiplePermissionRequest() {
        onLaunch()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
class FakePermissionState(
    override val permission: String,
    status: PermissionStatus
) : PermissionState {
    override var status: PermissionStatus by mutableStateOf(status)
    override fun launchPermissionRequest() {}
}

class FakePermissionsRepository : PermissionsRepository {
    override val requestedPermissions = MutableStateFlow<Set<String>>(emptySet())

    override suspend fun markPermissionRequested(permissionName: String) {
        requestedPermissions.update { it + permissionName }
    }
}
