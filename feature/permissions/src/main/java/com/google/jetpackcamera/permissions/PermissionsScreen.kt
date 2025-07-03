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
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.jetpackcamera.permissions.ui.PermissionTemplate

private const val TAG = "PermissionsScreen"

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    shouldRequestAudioPermission: Boolean,
    shouldRequestReadWriteStoragePermission: Boolean,
    onAllPermissionsGranted: () -> Unit,
    openAppSettings: () -> Unit
) {
    val permissionStates = rememberMultiplePermissionsState(
        permissions =
        buildList {
            add(Manifest.permission.CAMERA)
            if (shouldRequestAudioPermission) {
                add(Manifest.permission.RECORD_AUDIO)
            }

            // sometimes, when the write storage permission is granted, it will automatically grant the read storage permission
            if (shouldRequestReadWriteStoragePermission) {
                add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
    )
    PermissionsScreen(
        permissionStates = permissionStates,
        onAllPermissionsGranted = onAllPermissionsGranted,
        openAppSettings = openAppSettings
    )
}

/**
 * Permission prompts screen.
 * Camera permission will always prompt when disabled, and the app cannot be used otherwise
 * if optional settings have not yet been declined by the user, then they will be prompted as well
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionsScreen(
    modifier: Modifier = Modifier,
    onAllPermissionsGranted: () -> Unit,
    openAppSettings: () -> Unit,
    permissionStates: MultiplePermissionsState,
    viewModel: PermissionsViewModel = hiltViewModel<
        PermissionsViewModel,
        PermissionsViewModel.Factory
        > { factory -> factory.create(permissionStates) }
) {
    Log.d(TAG, "PermissionsScreen")
    val permissionsUiState: PermissionsUiState by viewModel.permissionsUiState.collectAsState()
    LaunchedEffect(permissionsUiState) {
        if (permissionsUiState is PermissionsUiState.AllPermissionsGranted) {
            onAllPermissionsGranted()
        }
    }

    if (permissionsUiState is PermissionsUiState.PermissionsNeeded) {
        val permissionEnum =
            (permissionsUiState as PermissionsUiState.PermissionsNeeded).currentPermission

        PermissionTemplate(
            modifier = modifier,
            permissionEnum = permissionEnum,
            onDismissPermission = { viewModel.dismissPermission() },
            onSkipPermission = when (permissionEnum) {
                PermissionEnum.CAMERA -> null
                // todo: skip permission button functionality. currently need to go through the
                // prompt to skip
                else -> null // permissionsViewModel::dismissPermission
            },
            onOpenAppSettings = openAppSettings
        )
    }
}
