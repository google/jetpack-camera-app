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
package com.google.jetpackcamera.feature.preview.navigation

import android.net.Uri
import androidx.compose.animation.fadeIn
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavOptionsBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.jetpackcamera.feature.preview.PreviewScreen
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute.ARG_CAPTURE_URIS
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute.ARG_DEBUG_SETTINGS
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute.ARG_EXTERNAL_CAPTURE_MODE
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute.ARG_REVIEW_AFTER_CAPTURE
import com.google.jetpackcamera.model.CaptureEvent
import com.google.jetpackcamera.model.DebugSettings
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.SaveMode

object PreviewRoute {
    internal const val ARG_EXTERNAL_CAPTURE_MODE: String = "externalCaptureMode"

    internal const val ARG_REVIEW_AFTER_CAPTURE: String = "reviewAfterCapture"
    internal const val ARG_CAPTURE_URIS: String = "captureUris"
    internal const val ARG_DEBUG_SETTINGS: String = "debugSettings"
}

private const val BASE_ROUTE_DEF: String = "preview"
private const val FULL_ROUTE_DEF: String =
    BASE_ROUTE_DEF +
        "?${ARG_EXTERNAL_CAPTURE_MODE}={$ARG_EXTERNAL_CAPTURE_MODE}" +
        "&${ARG_REVIEW_AFTER_CAPTURE}={$ARG_REVIEW_AFTER_CAPTURE}" +
        "&${ARG_CAPTURE_URIS}={$ARG_CAPTURE_URIS}" +
        "&${ARG_DEBUG_SETTINGS}={$ARG_DEBUG_SETTINGS}"

fun NavController.navigateToPreview(
    externalCaptureMode: ExternalCaptureMode? = null,
    captureUris: List<Uri>? = null,
    debugSettings: DebugSettings? = null,
    saveMode: Boolean? = null,
    builder: (NavOptionsBuilder.() -> Unit) = {}
) {
    var route = BASE_ROUTE_DEF // Start with the base route

    // Conditionally append query parameters based on whether the arguments are provided
    val queryParams = mutableListOf<String>()

    externalCaptureMode?.let {
        queryParams.add(
            "${ARG_EXTERNAL_CAPTURE_MODE}=${NavType.EnumType(
                ExternalCaptureMode::class.java
            ).serializeAsValue(it)}"
        )
    }
    saveMode?.let {
        queryParams.add(
            "${ARG_REVIEW_AFTER_CAPTURE}=${
                NavType.BoolType.serializeAsValue(it)}"
        )
    }
    captureUris?.let {
        queryParams.add(
            "${ARG_CAPTURE_URIS}=${
                NavType.StringListType.serializeAsValue(it.map(Uri::toString))}"
        )
    }
    debugSettings?.let {
        queryParams.add(
            "${ARG_DEBUG_SETTINGS}=${
                DebugSettingsNavType.serializeAsValue(it)}"
        )
    }

    if (queryParams.isNotEmpty()) {
        route += "?" + queryParams.joinToString("&")
    }

    this.navigate(route, builder)
}

fun NavGraphBuilder.previewScreen(
    externalCaptureMode: ExternalCaptureMode,
    shouldCacheReview: Boolean,
    captureUris: List<Uri>,
    debugSettings: DebugSettings,
    onRequestWindowColorMode: (Int) -> Unit,
    onFirstFrameCaptureCompleted: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToPostCapture: () -> Unit,
    onCaptureEvent: (CaptureEvent) -> Unit
) {
    composable(
        route = FULL_ROUTE_DEF,
        arguments = listOf(
            navArgument(name = ARG_EXTERNAL_CAPTURE_MODE) {
                type = NavType.EnumType(ExternalCaptureMode::class.java)
                defaultValue = externalCaptureMode
            },
            navArgument(name = ARG_REVIEW_AFTER_CAPTURE) {
                type = NavType.BoolType
                defaultValue = shouldCacheReview
            },
            navArgument(name = ARG_CAPTURE_URIS) {
                type = NavType.StringListType
                defaultValue = captureUris.map { it.toString() }
            },
            navArgument(name = ARG_DEBUG_SETTINGS) {
                type = DebugSettingsNavType
                defaultValue = debugSettings
            }
        ),
        enterTransition = { fadeIn() }
    ) {
        PreviewScreen(
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToPostCapture = onNavigateToPostCapture,
            onRequestWindowColorMode = onRequestWindowColorMode,
            onFirstFrameCaptureCompleted = onFirstFrameCaptureCompleted,
            onCaptureEvent = onCaptureEvent
        )
    }
}

internal fun SavedStateHandle.getRequestedSaveMode(): SaveMode? {
    val requestedCaptureReview = get<Boolean>(ARG_REVIEW_AFTER_CAPTURE) ?: false
    return if (requestedCaptureReview) {
        SaveMode.CacheAndReview()
    } else {
        null
    }
}

internal fun SavedStateHandle.getExternalCaptureMode(
    defaultIfMissing: ExternalCaptureMode = ExternalCaptureMode.Standard
): ExternalCaptureMode = get(ARG_EXTERNAL_CAPTURE_MODE) ?: defaultIfMissing

internal fun SavedStateHandle.getCaptureUris(defaultIfMissing: List<Uri> = emptyList()): List<Uri> =
    get<Array<String>?>(ARG_CAPTURE_URIS)?.map { it.toUri() } ?: defaultIfMissing

internal fun SavedStateHandle.getDebugSettings(
    defaultIfMissing: DebugSettings = DebugSettings()
): DebugSettings = get<String>(ARG_DEBUG_SETTINGS)?.let(DebugSettings::parseFromString)
    ?: defaultIfMissing
