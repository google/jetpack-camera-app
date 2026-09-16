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
package com.google.jetpackcamera.ui.components.capture

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalView
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Drop-in composable effect that manages the system bars (status bar and navigation bar) for
 * camera capture experiences.
 *
 * When [enabled], this effect:
 * - Hides the status bar while keeping the navigation bar visible.
 * - Configures [WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE] so that the
 *   status bar can be temporarily revealed by swiping down from the top edge.
 * - Senses multi-window / split-screen mode and keeps all bars visible to avoid altering the system
 *   partition.
 * - Re-asserts bar visibility on lifecycle resume and window focus gain to handle transitions from
 *   other apps, keyguard dismissal, or transient timeouts.
 * - Adapts status bar icon appearance according to [isDarkTheme] (light icons over dark viewfinders).
 * - Restores system bars upon disposal or when [enabled] becomes false (skipping when the activity is finishing
 *   or changing configurations to avoid visual blinks).
 *
 * Developers can call this inside a single camera screen, or hoist it above a navigation host driven
 * by the current destination route.
 *
 * @param enabled whether the immersive system bar policy should be actively enforced.
 * @param isDarkTheme whether the theme or surface behind transient bars is dark.
 * @param hideStatusBar whether the status bar should be hidden when [enabled] is true and not in multi-window.
 * @param keepNavigationBar whether the navigation bar should remain visible.
 */
@Composable
fun CameraSystemBarsEffect(
    enabled: Boolean = true,
    isDarkTheme: Boolean = true,
    hideStatusBar: Boolean = true,
    keepNavigationBar: Boolean = true
) {
    val activity = LocalActivity.current as? ComponentActivity
    val view = LocalView.current
    if (activity == null || view.isInEditMode) return

    // Hiding bars in multi-window would affect the whole screen, not just this app's partition,
    // so never hide anything while sharing the screen.
    var isInMultiWindowMode by remember(activity) { mutableStateOf(activity.isInMultiWindowMode) }
    DisposableEffect(activity) {
        val listener = Consumer<MultiWindowModeChangedInfo> {
            isInMultiWindowMode = it.isInMultiWindowMode
        }
        activity.addOnMultiWindowModeChangedListener(listener)
        onDispose { activity.removeOnMultiWindowModeChangedListener(listener) }
    }

    val shouldHideStatusBar = enabled && !isInMultiWindowMode && hideStatusBar

    LifecycleResumeEffect(activity, view, shouldHideStatusBar, isDarkTheme, keepNavigationBar) {
        applySystemBars(
            window = activity.window,
            view = view,
            hideStatusBar = shouldHideStatusBar,
            keepNavigationBar = keepNavigationBar,
            isDarkTheme = isDarkTheme
        )
        onPauseOrDispose {}
    }

    DisposableEffect(activity, view, shouldHideStatusBar, isDarkTheme, keepNavigationBar) {
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                applySystemBars(
                    window = activity.window,
                    view = view,
                    hideStatusBar = shouldHideStatusBar,
                    keepNavigationBar = keepNavigationBar,
                    isDarkTheme = isDarkTheme
                )
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
    }

    DisposableEffect(activity, view, isDarkTheme, keepNavigationBar) {
        onDispose {
            // Skip the restore when the activity is going away or being recreated: the bars would
            // visibly blink during a configuration change, and a finishing activity's window state
            // is irrelevant.
            if (!activity.isFinishing && !activity.isChangingConfigurations) {
                applySystemBars(
                    window = activity.window,
                    view = view,
                    hideStatusBar = false,
                    keepNavigationBar = keepNavigationBar,
                    isDarkTheme = isDarkTheme
                )
            }
        }
    }
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
    keepNavigationBar: Boolean = true,
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

    // When the status bar is visible on a non-capture surface (such as Settings), its icon
    // appearance should match the surface contrast: dark icons on a light surface, light icons on
    // a dark surface. On capture surfaces where the status bar is hidden, any transient reveal
    // overlays the black viewfinder background, so icons should always remain light (white).
    val lightStatusBars = !hideStatusBar && !isDarkTheme
    if (controller.isAppearanceLightStatusBars != lightStatusBars) {
        controller.isAppearanceLightStatusBars = lightStatusBars
    }

    if (keepNavigationBar) {
        controller.show(WindowInsetsCompat.Type.navigationBars())
    }
}
