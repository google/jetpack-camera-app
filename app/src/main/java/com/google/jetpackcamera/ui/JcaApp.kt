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
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.Manifest.permission.READ_MEDIA_IMAGES
import android.Manifest.permission.READ_MEDIA_VIDEO
import android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
import android.os.Build
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.jetpackcamera.BuildConfig
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.PreviewScreen
import com.google.jetpackcamera.feature.preview.PreviewViewModel
import com.google.jetpackcamera.settings.SettingsScreen
import com.google.jetpackcamera.settings.VersionInfoHolder
import com.google.jetpackcamera.ui.Routes.PREVIEW_ROUTE
import com.google.jetpackcamera.ui.Routes.SETTINGS_ROUTE

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun JcaApp(
    previewMode: PreviewMode,
    onPreviewViewModel: (PreviewViewModel) -> Unit,
    onRequestWindowColorMode: (Int) -> Unit
    /*TODO(b/306236646): remove after still capture*/
) {
    val cameraPermissionState =
        rememberMultiplePermissionsState(permissions = listOf(Manifest.permission.CAMERA))
    val storagePermissionState: MultiplePermissionsState

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        storagePermissionState = rememberMultiplePermissionsState(
            permissions = listOf(
                READ_MEDIA_VISUAL_USER_SELECTED,
                READ_MEDIA_IMAGES,
                READ_MEDIA_VIDEO
            )
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        storagePermissionState =
            rememberMultiplePermissionsState(
                permissions = listOf(
                    READ_MEDIA_IMAGES,
                    READ_MEDIA_VIDEO
                )
            )
    } else {
        storagePermissionState =
            rememberMultiplePermissionsState(permissions = listOf(READ_EXTERNAL_STORAGE))
    }
    if (cameraPermissionState.allPermissionsGranted && (storagePermissionState.allPermissionsGranted || storagePermissionState.permissions[0].status.isGranted)) {
        JetpackCameraNavHost(
            onPreviewViewModel = onPreviewViewModel,
            previewMode = previewMode,
            onRequestWindowColorMode = onRequestWindowColorMode
        )
    } else {
        if (!cameraPermissionState.allPermissionsGranted) {
            CameraPermission(
                modifier = Modifier.fillMaxSize(),
                cameraPermissionState = cameraPermissionState
            )
        }
        if (!storagePermissionState.allPermissionsGranted && !storagePermissionState.permissions[0].status.isGranted) {
            StoragePermission(
                modifier = Modifier.fillMaxSize(),
                storagePermissionState = storagePermissionState
            )
        }
    }
}


@Composable
private fun JetpackCameraNavHost(
    previewMode: PreviewMode,
    onPreviewViewModel: (PreviewViewModel) -> Unit,
    onRequestWindowColorMode: (Int) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(navController = navController, startDestination = PREVIEW_ROUTE) {
        composable(PREVIEW_ROUTE) {
            PreviewScreen(
                onPreviewViewModel = onPreviewViewModel,
                onNavigateToSettings = { navController.navigate(SETTINGS_ROUTE) },
                onRequestWindowColorMode = onRequestWindowColorMode,
                previewMode = previewMode
            )
        }
        composable(SETTINGS_ROUTE) {
            SettingsScreen(
                versionInfo = VersionInfoHolder(
                    versionName = BuildConfig.VERSION_NAME,
                    buildType = BuildConfig.BUILD_TYPE
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}