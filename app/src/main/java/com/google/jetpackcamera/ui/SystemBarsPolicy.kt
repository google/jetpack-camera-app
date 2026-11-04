/*
 * Copyright (C) 2026 The Android Open Source Project
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

import androidx.compose.runtime.Composable
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute
import com.google.jetpackcamera.ui.Routes.POST_CAPTURE_ROUTE
import com.google.jetpackcamera.ui.components.capture.CameraSystemBarsEffect
import com.google.jetpackcamera.ui.components.capture.CameraSystemBarsPolicy

/**
 * Describes how the system bars should be configured for a given destination.
 *
 * The navigation bar is kept visible except on the preview screen when the capture layout reports
 * that the window is too short for the tallest viewfinder to clear a persistent navigation bar.
 */
internal enum class SystemBarsPolicy {
    /** Hide the status bar, keep the navigation bar. Used by standard capture surfaces. */
    HideStatusBar,

    /**
     * Hide the status bar and the navigation bar. Used by the preview screen while its layout
     * requires immersive mode.
     */
    HideStatusAndNavigationBars,

    /** Show every system bar. Used by everything else, and by multi-window. */
    ShowAll
}

/**
 * Maps a navigation route to the [SystemBarsPolicy] that destination wants.
 *
 * [route] is a route *pattern*, as reported by `NavDestination.route`, so destinations that declare
 * query arguments report them as unresolved placeholders (for example
 * `"preview?externalCaptureMode={externalCaptureMode}&..."`). Only the part before the first `?`
 * identifies the destination, so the route is normalized before matching. An unknown or `null`
 * route falls back to [SystemBarsPolicy.ShowAll].
 *
 * @param previewRequiresImmersive the latest value reported by the preview screen's layout. It only
 *   applies to the preview route, so a stale value never affects another destination.
 */
internal fun systemBarsPolicyFor(
    route: String?,
    previewRequiresImmersive: Boolean = false
): SystemBarsPolicy = when (route?.substringBefore('?')) {
    PreviewRoute.toString() -> if (previewRequiresImmersive) {
        SystemBarsPolicy.HideStatusAndNavigationBars
    } else {
        SystemBarsPolicy.HideStatusBar
    }
    POST_CAPTURE_ROUTE -> SystemBarsPolicy.HideStatusBar
    else -> SystemBarsPolicy.ShowAll
}

/**
 * The single owner of system bar visibility for the app.
 *
 * This deliberately lives above the `NavHost` rather than inside each screen. Navigation keeps both
 * the outgoing and the incoming destination composed for the duration of a transition, so a
 * per-screen effect would have the *outgoing* screen's cleanup run last and overwrite the policy
 * the incoming screen just applied.
 *
 * The policy is re-asserted on every resume and window focus gain because the system can reset bar
 * visibility underneath the app (a transient reveal timing out, keyguard dismissal, entering
 * multi-window, returning from another task).
 */
@Composable
internal fun SystemBarsPolicyEffect(policy: SystemBarsPolicy, isDarkTheme: Boolean = true) {
    val cameraPolicy = when (policy) {
        SystemBarsPolicy.HideStatusBar -> CameraSystemBarsPolicy.HideStatusBar
        SystemBarsPolicy.HideStatusAndNavigationBars ->
            CameraSystemBarsPolicy.HideStatusAndNavigationBars
        SystemBarsPolicy.ShowAll -> CameraSystemBarsPolicy.ShowAll
    }
    CameraSystemBarsEffect(
        policy = cameraPolicy,
        isDarkTheme = isDarkTheme
    )
}
