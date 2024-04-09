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
import androidx.lifecycle.ViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalPermissionsApi::class)
class PermissionsViewModel(
    val permissionEnums: Set<PermissionEnum>,
) : ViewModel() {

    val visiblePermissionDialogQueue = mutableStateListOf<PermissionEnum>()

    init {
        visiblePermissionDialogQueue.addAll(permissionEnums)
    }

    fun dismissPermission() {
        visiblePermissionDialogQueue.removeFirst()
    }

    fun addPermissions(permissionEnums: Iterable<PermissionEnum>) {
        visiblePermissionDialogQueue.addAll(permissionEnums)
    }
}

