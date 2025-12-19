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

import android.net.Uri
import android.os.Build
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.EaseIn
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.jetpackcamera.BuildConfig
import com.google.jetpackcamera.feature.postcapture.PostCaptureScreen
import com.google.jetpackcamera.feature.preview.navigation.navigateToPreview
import com.google.jetpackcamera.feature.preview.navigation.popUpToPreview
import com.google.jetpackcamera.feature.preview.navigation.previewScreen
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.permissions.navigation.PermissionsRoute
import com.google.jetpackcamera.permissions.navigation.navigateToPermissions
import com.google.jetpackcamera.permissions.navigation.permissionsScreen
import com.google.jetpackcamera.permissions.navigation.popUpToPermissions
import com.google.jetpackcamera.settings.SettingsScreen
import com.google.jetpackcamera.settings.VersionInfoHolder
import com.google.jetpackcamera.ui.Routes.POST_CAPTURE_ROUTE
import com.google.jetpackcamera.ui.Routes.SETTINGS_ROUTE

@Composable
fun JcaApp(
    externalCaptureMode: ExternalCaptureMode,
    shouldReviewAfterCapture: Boolean,
    captureUris: List<Uri>,
    debugSettings: DebugSettings,
    onRequestWindowColorMode: (Int) -> Unit,
    onFirstFrameCaptureCompleted: () -> Unit,
    openAppSettings: () -> Unit,
    onCaptureEvent: (CaptureEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    JetpackCameraNavHost(
        modifier = modifier,
        externalCaptureMode = externalCaptureMode,
        shouldReviewAfterCapture = shouldReviewAfterCapture,
        captureUris = captureUris,
        debugSettings = debugSettings,
        onOpenAppSettings = openAppSettings,
        onRequestWindowColorMode = onRequestWindowColorMode,
        onFirstFrameCaptureCompleted = onFirstFrameCaptureCompleted,
        onCaptureEvent = onCaptureEvent
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
private fun JetpackCameraNavHost(
    modifier: Modifier = Modifier,
    externalCaptureMode: ExternalCaptureMode,
    shouldReviewAfterCapture: Boolean,
    captureUris: List<Uri>,
    debugSettings: DebugSettings,
    onOpenAppSettings: () -> Unit,
    onRequestWindowColorMode: (Int) -> Unit,
    onFirstFrameCaptureCompleted: () -> Unit,
    onCaptureEvent: (CaptureEvent) -> Unit,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = PermissionsRoute.toString(),
        modifier = modifier
    ) {
        val requestablePermissions = buildList {
            add(android.Manifest.permission.CAMERA)
            if (externalCaptureMode == ExternalCaptureMode.Standard) {
                add(android.Manifest.permission.RECORD_AUDIO)
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                    add(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }
        }
        permissionsScreen(
            requestablePermissions = requestablePermissions,
            onAllPermissionsGranted = {
                navController.navigateToPreview {
                    popUpToPermissions()
                }
            },
            onOpenAppSettings = onOpenAppSettings
        )

        previewScreen(
            externalCaptureMode = externalCaptureMode,
            shouldCacheReview = shouldReviewAfterCapture,
            captureUris = captureUris,
            debugSettings = debugSettings,
            onRequestWindowColorMode = onRequestWindowColorMode,
            onFirstFrameCaptureCompleted = onFirstFrameCaptureCompleted,
            onNavigateToSettings = { navController.navigate(SETTINGS_ROUTE) },
            onNavigateToPostCapture = { navController.navigate(POST_CAPTURE_ROUTE) },
            onNavigateToPermissions = {
                navController.navigateToPermissions {
                    popUpToPreview()
                }
            },
            onCaptureEvent = onCaptureEvent
        )

        composable(
            route = SETTINGS_ROUTE,
            enterTransition = {
                fadeIn(
                    animationSpec = tween(easing = LinearEasing)
                ) + slideIntoContainer(
                    animationSpec = tween(easing = EaseIn),
                    towards = AnimatedContentTransitionScope.SlideDirection.Start
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    animationSpec = tween(easing = EaseOut),
                    towards = AnimatedContentTransitionScope.SlideDirection.End
                )
            }
        ) {
            SettingsScreen(
                versionInfo = VersionInfoHolder(
                    versionName = BuildConfig.VERSION_NAME,
                    buildType = BuildConfig.BUILD_TYPE
                ),
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            POST_CAPTURE_ROUTE
        ) {
            PostCaptureScreen(onNavigateBack = { navController.popBackStack() })
        }
    }
}
