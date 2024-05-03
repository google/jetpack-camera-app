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

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.jetpackcamera.permissions.ui.PermissionTemplate
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState

/**
 * Permission prompts screen.
 * Camera permission will always prompt when disabled, and the app cannot be used otherwise
 * if optional settings have not yet been declined by the user, then they will be prompted as well
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier,
    viewModel: PermissionsViewModel,
    openAppSettings: () -> Unit
) {
    if (!viewModel.visiblePermissionDialogQueue.isEmpty()) {
        val permissionEnum = viewModel.visiblePermissionDialogQueue.first()
        val currentPermissionState =
            rememberPermissionState(permission = permissionEnum.getPermission())

        val permissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { permissionGranted ->
                if (permissionGranted) {
                    // remove from list
                    viewModel.dismissPermission()
                } else if (permissionEnum.isOptional()) {
                    viewModel.dismissPermission()
                }
            }
        )

        PermissionTemplate(
            modifier = modifier,
            permissionEnum = permissionEnum,
            permissionState = currentPermissionState,
            onSkipPermission = when (permissionEnum) {
                // todo: a prettier navigation to app settings.
                PermissionEnum.CAMERA -> null
                // todo: skip permission button functionality. currently need to go through the prompt to skip
                else -> null // permissionsViewModel::dismissPermission
            },
            onRequestPermission = { permissionLauncher.launch(permissionEnum.getPermission()) },
            onOpenAppSettings = openAppSettings
        )
    }
}
