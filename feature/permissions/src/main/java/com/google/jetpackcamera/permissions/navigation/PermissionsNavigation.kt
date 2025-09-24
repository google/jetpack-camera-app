/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.permissions.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.jetpackcamera.permissions.PermissionEnum
import com.google.jetpackcamera.permissions.PermissionsScreen
import com.google.jetpackcamera.permissions.navigation.PermissionsRoute.ARG_REQUESTABLE_PERMISSIONS
private const val BASE_ROUTE_DEF = "permissions"

private const val FULL_ROUTE_DEF = "$BASE_ROUTE_DEF?" +
    "$ARG_REQUESTABLE_PERMISSIONS={$ARG_REQUESTABLE_PERMISSIONS}"

object PermissionsRoute {
    internal const val ARG_REQUESTABLE_PERMISSIONS = "requestable_permissions"

    override fun toString(): String = BASE_ROUTE_DEF
}

fun NavController.navigateToPermissions(
    requestablePermissions: List<String>? = null,
    builder: (NavOptionsBuilder.() -> Unit) = {}
) {
    var route = BASE_ROUTE_DEF // Start with the base route

    requestablePermissions?.let {
        route += "?" +
            "${ARG_REQUESTABLE_PERMISSIONS}=${NavType.StringListType.serializeAsValue(it)}"
    }

    this.navigate(route, builder)
}

@OptIn(ExperimentalPermissionsApi::class)
fun NavGraphBuilder.permissionsScreen(
    requestablePermissions: List<String>,
    onAllPermissionsGranted: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    composable(
        route = FULL_ROUTE_DEF, // Use the route with arg placeholders
        arguments = listOf(
            navArgument(ARG_REQUESTABLE_PERMISSIONS) {
                type = NavType.StringListType
                defaultValue = requestablePermissions
            }
        )
    ) {
        val permissionStates = rememberMultiplePermissionsState(
            permissions = requestablePermissions
        )

        PermissionsScreen(
            permissionStates = permissionStates,
            onAllPermissionsGranted = onAllPermissionsGranted,
            onOpenAppSettings = onOpenAppSettings
        )
    }
}

fun NavOptionsBuilder.popUpToPermissions() {
    popUpTo(BASE_ROUTE_DEF) {
        inclusive = true
    }
}

internal fun SavedStateHandle.getRequestablePermissions(): List<PermissionEnum> =
    get<Array<String>?>(ARG_REQUESTABLE_PERMISSIONS)?.map(PermissionEnum::fromString) ?: emptyList()
