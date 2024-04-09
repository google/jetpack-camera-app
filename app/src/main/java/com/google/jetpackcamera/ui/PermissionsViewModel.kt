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

package com.google.jetpackcamera.ui

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState

@OptIn(ExperimentalPermissionsApi::class)
class PermissionsViewModel(
    val shouldShowRequestPermissionRationale: (String) -> Boolean,
    val openAppSettings: () -> Unit,
    val permissionEnums: SnapshotStateList<PermissionEnum>,
) : ViewModel() {

    val visiblePermissionDialogQueue = mutableStateListOf<PermissionEnum>()
    //todo skip permission
    fun dismissPermission() {
        visiblePermissionDialogQueue.removeFirst()
    }

    fun getPermissionToShow(): PermissionEnum {
        return permissionEnums.first()
    }

    fun onPermissionResult(permission: PermissionEnum, isGranted:Boolean, isRequired: Boolean){

    }

    fun onRequestPermission(permissionState: PermissionState) {
        if (shouldShowRequestPermissionRationale(permissionState.permission)) {
            //todo make it a prettier flow vs just redirecting to app settings
            openAppSettings()
        } else {
            permissionState.launchPermissionRequest()
        }

    }
}

