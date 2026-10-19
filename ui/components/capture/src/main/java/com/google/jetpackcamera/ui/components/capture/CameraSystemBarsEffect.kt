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

import android.os.Build
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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Drop-in composable effect that manages the system bars (status bar and navigation bar) for
 * camera capture experiences, supporting both Activity-hosted and Compose Dialog-hosted surfaces.
 *
 * When [enabled], this effect:
 * - Hides the status bar while keeping the navigation bar visible (unless [keepNavigationBar] is false).
 * - Configures [WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE] so that the
 *   status bar can be temporarily revealed by swiping down from the top edge.
 * - Senses multi-window / split-screen mode and keeps all bars visible to avoid altering the system
 *   partition.
 * - Re-asserts bar visibility on lifecycle resume and window focus gain to handle transitions from
 *   other apps, keyguard dismissal, or transient timeouts.
 * - Adapts status bar icon appearance according to [isDarkTheme] (light icons over dark viewfinders).
 * - Restores system bars upon disposal or when [enabled] becomes false (skipping when the activity
 *   is finishing or changing configurations to avoid visual blinks).
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
    val dialogWindow = generateSequence(view.parent) { it.parent }
        .filterIsInstance<DialogWindowProvider>()
        .firstOrNull()?.window
    if ((activity == null && dialogWindow == null) || view.isInEditMode) return

    // Hiding bars in multi-window would affect the whole screen, not just this app's partition,
    // so never hide anything while sharing the screen.
    var isInMultiWindowMode by remember(activity) {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                activity?.isInMultiWindowMode ?: false
            } else {
                false
            }
        )
    }
    DisposableEffect(activity) {
        val listener = Consumer<MultiWindowModeChangedInfo> {
            isInMultiWindowMode = it.isInMultiWindowMode
        }
        activity?.addOnMultiWindowModeChangedListener(listener)
        onDispose { activity?.removeOnMultiWindowModeChangedListener(listener) }
    }

    val shouldHideStatusBar = enabled && !isInMultiWindowMode && hideStatusBar
    val shouldHideNavigationBar = enabled && !isInMultiWindowMode && !keepNavigationBar

    val applyToAllWindows: (Boolean, Boolean, Boolean) -> Unit =
        { hideStatus, keepNav, darkTheme ->
            dialogWindow?.let { win ->
                applySystemBars(
                    window = win,
                    view = view,
                    hideStatusBar = hideStatus,
                    keepNavigationBar = keepNav,
                    isDarkTheme = darkTheme
                )
            }
            activity?.window?.let { win ->
                applySystemBars(
                    window = win,
                    view = win.decorView,
                    hideStatusBar = hideStatus,
                    keepNavigationBar = keepNav,
                    isDarkTheme = darkTheme
                )
            }
        }

    LifecycleResumeEffect(
        activity,
        dialogWindow,
        view,
        shouldHideStatusBar,
        shouldHideNavigationBar,
        isDarkTheme
    ) {
        applyToAllWindows(shouldHideStatusBar, !shouldHideNavigationBar, isDarkTheme)
        onPauseOrDispose {}
    }

    DisposableEffect(
        activity,
        dialogWindow,
        view,
        shouldHideStatusBar,
        shouldHideNavigationBar,
        isDarkTheme
    ) {
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                applyToAllWindows(shouldHideStatusBar, !shouldHideNavigationBar, isDarkTheme)
            }
        }
        view.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        onDispose {
            view.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
    }

    val currentIsDarkTheme by rememberUpdatedState(isDarkTheme)
    DisposableEffect(activity, dialogWindow, view) {
        onDispose {
            // Skip the restore when the activity is going away or being recreated: the bars would
            // visibly blink during a configuration change, and a finishing activity's window state
            // is irrelevant.
            val isChangingOrFinishing =
                activity?.let { it.isFinishing || it.isChangingConfigurations } ?: false
            if (!isChangingOrFinishing) {
                dialogWindow?.let { win ->
                    applySystemBars(
                        window = win,
                        view = view,
                        hideStatusBar = false,
                        keepNavigationBar = true,
                        isDarkTheme = currentIsDarkTheme
                    )
                }
                activity?.window?.let { win ->
                    applySystemBars(
                        window = win,
                        view = win.decorView,
                        hideStatusBar = false,
                        keepNavigationBar = true,
                        isDarkTheme = currentIsDarkTheme
                    )
                }
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
    val desiredBehavior = if (hideStatusBar || !keepNavigationBar) {
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

    if (keepNavigationBar) {
        controller.show(WindowInsetsCompat.Type.navigationBars())
    } else {
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }
}
