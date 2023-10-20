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
package com.google.jetpackcamera.ui

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.jetpackcamera.feature.preview.PreviewScreen
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.settings.SettingsScreen
import com.google.jetpackcamera.ui.Routes.PREVIEW_ROUTE
import com.google.jetpackcamera.ui.Routes.SETTINGS_ROUTE

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun JcaApp(
    onPreviewViewModel: (PreviewViewModel) -> Unit
    /*TODO(b/306236646): remove after still capture*/
) {
    val permissionState =
        rememberPermissionState(permission = Manifest.permission.CAMERA)

    if (permissionState.status.isGranted) {
        JetpackCameraNavHost(onPreviewViewModel)
    } else {
        CameraPermission(
            modifier = Modifier.fillMaxSize(),
            cameraPermissionState = permissionState
        )
    }
}

@Composable
private fun JetpackCameraNavHost(
    onPreviewViewModel: (PreviewViewModel) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = PREVIEW_ROUTE) {
        composable(PREVIEW_ROUTE) {
            PreviewScreen(
                onPreviewViewModel = onPreviewViewModel,
                onNavigateToSettings = { navController.navigate(SETTINGS_ROUTE) }
            )
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                onNavigateToPreview = { navController.navigate(PREVIEW_ROUTE) }
            )
        }
    }
}
