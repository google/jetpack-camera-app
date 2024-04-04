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
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
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
    onPreviewViewModel: (PreviewViewModel) -> Unit,
    shouldShowPermissionsRationale: (String) -> Boolean,
    openAppSettings: () -> Unit,
    /*TODO(b/306236646): remove after still capture*/
    previewMode: PreviewMode
) {
    val cameraPermissionState =
        rememberPermissionState(permission = Manifest.permission.CAMERA)
    val shouldOpenPermissions = remember { mutableStateOf(true) }
    // camera not being granted  is what toggles the main menu
    shouldOpenPermissions.value = !cameraPermissionState.status.isGranted


    // if camera permission is granted and permission screen is closed
    if (!shouldOpenPermissions.value) {
        JetpackCameraNavHost(
            onPreviewViewModel = onPreviewViewModel,
            previewMode = previewMode
        )
    } else {
        // you'll have the option to go through camera and all other optional permissions
        PermissionsScreen(
            modifier = Modifier.fillMaxSize(),
            permissionEnums = getUnGrantedPermissions(LocalContext.current),
            cameraPermissionState = cameraPermissionState,
            openAppSettings = openAppSettings,
            shouldShowPermissionsRequestRationale = shouldShowPermissionsRationale,
            onClosePermissionsScreen = { shouldOpenPermissions.value = false }
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private fun getUnGrantedPermissions(context: Context): List<PermissionsEnum> {
    var ungrantedPermissions = mutableListOf<PermissionsEnum>()
    if (!isPermissionGranted(context, Manifest.permission.CAMERA))
        ungrantedPermissions.add(PermissionsEnum.CAMERA)

    //todo add audio permission
    /*
    if (!isPermissionGranted(context, Manifest.permission.RECORD_AUDIO))
    ungrantedPermissions.add(PermissionsEnum.RECORD_AUDIO)
     */

    return ungrantedPermissions
}

private fun isPermissionGranted(context: Context, permission: String): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        permission
    ) == PackageManager.PERMISSION_GRANTED
}

@Composable
private fun JetpackCameraNavHost(
    onPreviewViewModel: (PreviewViewModel) -> Unit,
    navController: NavHostController = rememberNavController(),
    previewMode: PreviewMode
) {
    NavHost(navController = navController, startDestination = PREVIEW_ROUTE) {
        composable(PREVIEW_ROUTE) {
            PreviewScreen(
                onPreviewViewModel = onPreviewViewModel,
                onNavigateToSettings = { navController.navigate(SETTINGS_ROUTE) },
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
