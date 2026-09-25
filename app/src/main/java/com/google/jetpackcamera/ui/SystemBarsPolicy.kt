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

import android.view.View
import android.view.ViewTreeObserver
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute
import com.google.jetpackcamera.ui.Routes.POST_CAPTURE_ROUTE

/**
 * Describes how the system bars should be configured for a given destination.
 *
 * Note that the navigation bar is always kept visible: JCA is portrait-locked and has no
 * large-screen layout, so there is no destination that benefits from hiding it, and keeping it
 * visible avoids the gesture/3-button inconsistencies that come with hiding it.
 */
internal enum class SystemBarsPolicy {
    /** Hide the status bar, keep the navigation bar. Used by the capture surfaces. */
    HideStatusBar,

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
 */
internal fun systemBarsPolicyFor(route: String?): SystemBarsPolicy =
    when (route?.substringBefore('?')) {
        PreviewRoute.toString(), POST_CAPTURE_ROUTE -> SystemBarsPolicy.HideStatusBar
        else -> SystemBarsPolicy.ShowAll
    }

/**
 * Applies the requested system bar configuration to [window].
 *
 * The platform [WindowInsetsControllerCompat] internally tracks `requestedVisibleTypes` and no-ops
 * redundant `hide`/`show` calls, while calling them unconditionally avoids stale reads from
 * `ViewCompat.getRootWindowInsets(view)` during activity launch or keyguard transitions.
 */
internal fun applySystemBars(
    window: Window,
    view: View,
    hideStatusBar: Boolean,
    isDarkTheme: Boolean = true
) {
    val controller = WindowCompat.getInsetsController(window, view)

    // Only request transient bars while a bar is actually hidden. Leaving this behavior installed
    // for destinations that show all bars would let edge swipes be consumed as "reveal the bars"
    // gestures instead of reaching scrollable content.
    val desiredBehavior = if (hideStatusBar) {
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
    }
    if (controller.systemBarsBehavior != desiredBehavior) {
        controller.systemBarsBehavior = desiredBehavior
    }

    if (hideStatusBar) {
        controller.hide(WindowInsetsCompat.Type.statusBars())
    } else {
        controller.show(WindowInsetsCompat.Type.statusBars())
    }

    // When system bars sit over a non-capture surface (such as Settings), their icon appearance
    // should match the surface contrast: dark icons on a light surface, light icons on a dark
    // surface. On capture surfaces where the status bar is hidden, both the transiently revealed
    // status bar and the visible navigation bar overlay the black camera/media background, so
    // icons should always remain light (white).
    val lightSystemBars = !hideStatusBar && !isDarkTheme
    if (controller.isAppearanceLightStatusBars != lightSystemBars) {
        controller.isAppearanceLightStatusBars = lightSystemBars
    }
    if (controller.isAppearanceLightNavigationBars != lightSystemBars) {
        controller.isAppearanceLightNavigationBars = lightSystemBars
    }

    controller.show(WindowInsetsCompat.Type.navigationBars())
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
    val activity = LocalActivity.current as? ComponentActivity
    val view = LocalView.current
    if (activity == null || view.isInEditMode) return

    // Hiding bars in multi-window would affect the whole screen, not just this app's partition, so
    // never hide anything while sharing the screen.
    var isInMultiWindowMode by remember(activity) { mutableStateOf(activity.isInMultiWindowMode) }
    DisposableEffect(activity) {
        val listener = Consumer<MultiWindowModeChangedInfo> {
            isInMultiWindowMode = it.isInMultiWindowMode
        }
        activity.addOnMultiWindowModeChangedListener(listener)
        onDispose { activity.removeOnMultiWindowModeChangedListener(listener) }
    }

    val hideStatusBar = !isInMultiWindowMode && policy == SystemBarsPolicy.HideStatusBar

    LifecycleResumeEffect(activity, view, hideStatusBar, isDarkTheme) {
        applySystemBars(activity.window, view, hideStatusBar, isDarkTheme)
        onPauseOrDispose {}
    }

    DisposableEffect(activity, view, hideStatusBar, isDarkTheme) {
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                applySystemBars(activity.window, view, hideStatusBar, isDarkTheme)
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
    }

    val currentIsDarkTheme by rememberUpdatedState(isDarkTheme)
    DisposableEffect(activity, view) {
        onDispose {
            // Skip the restore when the activity is going away or being recreated: the bars would
            // visibly blink during a configuration change, and a finishing activity's window state
            // is irrelevant.
            if (!activity.isFinishing && !activity.isChangingConfigurations) {
                applySystemBars(
                    activity.window,
                    view,
                    hideStatusBar = false,
                    isDarkTheme = currentIsDarkTheme
                )
            }
        }
    }
}
