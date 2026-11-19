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
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.app.MultiWindowModeChangedInfo
import androidx.core.util.Consumer
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Describes how the system bars (status bar and navigation bar) should be configured.
 */
enum class CameraSystemBarsPolicy {
    /** Hide the status bar and keep the navigation bar visible. */
    HideStatusBar,

    /** Hide both the status bar and the navigation bar. */
    HideStatusAndNavigationBars,

    /** Show both the status bar and the navigation bar. */
    ShowAll
}

/**
 * Composable effect that manages the system bars (status bar and navigation bar) for camera
 * capture experiences, supporting `Activity`-hosted, Compose `Dialog`-hosted, and custom
 * [Window]-hosted surfaces.
 *
 * **Placement with Compose Navigation:** When used inside a multi-screen navigation graph sharing
 * a single `Activity` [Window], call this effect **once above the `NavHost`** and drive [policy]
 * from the current destination rather than calling it inside individual screen composables.
 * Navigation keeps both the outgoing and the incoming destination composed for the duration of a
 * transition, so a per-screen effect would have the outgoing screen's `onDispose` cleanup run last
 * and overwrite the policy that the incoming screen just applied. When used inside a modal
 * `Dialog` (which owns its own [Window]), calling it directly inside the dialog content is safe.
 *
 * **Window resolution:** If [window] is omitted (`null`), this effect resolves the enclosing
 * Compose `Dialog` window via [DialogWindowProvider] when present, or falls back to
 * `LocalActivity.current?.window`. Only that single hosting window is modified. Callers hosting a
 * `ComposeView` inside a platform `android.app.Dialog` or `DialogFragment` (which do not provide a
 * [DialogWindowProvider]) should pass the dialog's `window` explicitly via [window].
 *
 * This effect:
 * - Applies [policy] to the resolved window, configuring
 *   [WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE] whenever at least one
 *   system bar is hidden so hidden bars can be temporarily revealed by an edge swipe.
 * - Senses multi-window / split-screen mode and clamps the effective policy to
 *   [CameraSystemBarsPolicy.ShowAll] to avoid altering the system partition.
 * - Re-asserts the policy on lifecycle resume and window focus gain to handle transitions from
 *   other apps, keyguard dismissal, or transient reveal timeouts.
 * - Forces light (white) system bar icons whenever a bar is hidden
 *   ([CameraSystemBarsPolicy.HideStatusBar] or
 *   [CameraSystemBarsPolicy.HideStatusAndNavigationBars]) because the bars overlay a dark
 *   camera/media background; [isDarkTheme] only adapts icon contrast when all bars are shown
 *   ([CameraSystemBarsPolicy.ShowAll] or in multi-window mode).
 * - Upon disposal (when not finishing or changing configurations), restores the window's initial
 *   `systemBarsBehavior`, `isAppearanceLightStatusBars`, and `isAppearanceLightNavigationBars`
 *   values captured before the effect first ran, and shows both the status bar and navigation bar.
 *
 * @param policy the desired [CameraSystemBarsPolicy] for the hosting window.
 * @param isDarkTheme whether the surface behind visible system bars is dark when [policy] is
 *   [CameraSystemBarsPolicy.ShowAll] (or when clamped in multi-window mode). Ignored when bars are
 *   hidden, in which case icons are always light (white).
 * @param window optional explicit [Window] to manage (for example, from a platform `Dialog` or
 *   `DialogFragment`). When `null`, resolves the nearest Compose [DialogWindowProvider] or
 *   `LocalActivity.current?.window`.
 */
@Composable
fun CameraSystemBarsEffect(
    policy: CameraSystemBarsPolicy = CameraSystemBarsPolicy.HideStatusBar,
    isDarkTheme: Boolean = true,
    window: Window? = null
) {
    val activity = LocalActivity.current
    val view = LocalView.current
    val dialogWindow = generateSequence(view.parent) { it.parent }
        .filterIsInstance<DialogWindowProvider>()
        .firstOrNull()?.window
    val targetWindow = window ?: dialogWindow ?: activity?.window
    if (targetWindow == null || view.isInEditMode) return
    val targetView = if (window != null || dialogWindow != null) view else targetWindow.decorView

    // Hiding bars in multi-window would affect the whole screen, not just this app's partition,
    // so never hide anything while sharing the screen.
    var isInMultiWindowMode by remember(activity) {
        mutableStateOf(activity?.isInMultiWindowMode ?: false)
    }
    DisposableEffect(activity) {
        val componentActivity = activity as? ComponentActivity
        val listener = Consumer<MultiWindowModeChangedInfo> {
            isInMultiWindowMode = it.isInMultiWindowMode
        }
        componentActivity?.addOnMultiWindowModeChangedListener(listener)
        onDispose { componentActivity?.removeOnMultiWindowModeChangedListener(listener) }
    }

    val effectivePolicy = if (isInMultiWindowMode) CameraSystemBarsPolicy.ShowAll else policy

    // Capture the window's initial system bar behavior and icon appearance before
    // LifecycleResumeEffect applies our policy for the first time, and restore them on dispose.
    DisposableEffect(activity, targetWindow, targetView) {
        val controller = WindowCompat.getInsetsController(targetWindow, targetView)
        val initialBehavior = controller.systemBarsBehavior
        val initialLightStatusBars = controller.isAppearanceLightStatusBars
        val initialLightNavigationBars = controller.isAppearanceLightNavigationBars
        onDispose {
            // Skip the restore when the activity is going away or being recreated: the bars would
            // visibly blink during a configuration change, and a finishing activity's window state
            // is irrelevant.
            val isChangingOrFinishing =
                activity?.let { it.isFinishing || it.isChangingConfigurations } ?: false
            if (!isChangingOrFinishing) {
                if (controller.systemBarsBehavior != initialBehavior) {
                    controller.systemBarsBehavior = initialBehavior
                }
                if (controller.isAppearanceLightStatusBars != initialLightStatusBars) {
                    controller.isAppearanceLightStatusBars = initialLightStatusBars
                }
                if (controller.isAppearanceLightNavigationBars != initialLightNavigationBars) {
                    controller.isAppearanceLightNavigationBars = initialLightNavigationBars
                }
                controller.show(WindowInsetsCompat.Type.statusBars())
                controller.show(WindowInsetsCompat.Type.navigationBars())
            }
        }
    }

    LifecycleResumeEffect(
        targetWindow,
        targetView,
        effectivePolicy,
        isDarkTheme
    ) {
        applySystemBars(targetWindow, targetView, effectivePolicy, isDarkTheme)
        onPauseOrDispose {}
    }

    DisposableEffect(
        targetWindow,
        targetView,
        effectivePolicy,
        isDarkTheme
    ) {
        val focusListener = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus ->
            if (hasFocus) {
                applySystemBars(targetWindow, targetView, effectivePolicy, isDarkTheme)
            }
        }
        targetView.viewTreeObserver.addOnWindowFocusChangeListener(focusListener)
        onDispose {
            targetView.viewTreeObserver.removeOnWindowFocusChangeListener(focusListener)
        }
    }
}

/**
 * Applies the requested [CameraSystemBarsPolicy] configuration to [window].
 *
 * The platform [WindowInsetsControllerCompat] internally tracks `requestedVisibleTypes` and no-ops
 * redundant `hide`/`show` calls, while calling them unconditionally avoids stale reads from
 * `ViewCompat.getRootWindowInsets(view)` during activity launch or keyguard transitions.
 */
internal fun applySystemBars(
    window: Window,
    view: View,
    policy: CameraSystemBarsPolicy,
    isDarkTheme: Boolean = true
) {
    val controller = WindowCompat.getInsetsController(window, view)
    val hideStatusBar = policy == CameraSystemBarsPolicy.HideStatusBar ||
        policy == CameraSystemBarsPolicy.HideStatusAndNavigationBars
    val hideNavigationBar = policy == CameraSystemBarsPolicy.HideStatusAndNavigationBars

    // Only request transient bars while a bar is actually hidden. Leaving this behavior installed
    // for destinations that show all bars would let edge swipes be consumed as "reveal the bars"
    // gestures instead of reaching scrollable content.
    val desiredBehavior = if (hideStatusBar || hideNavigationBar) {
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
    // surface. On capture surfaces where a bar is hidden, both the transiently revealed bars and
    // any visible navigation bar overlay the black camera/media background, so icons should always
    // remain light (white).
    val lightSystemBars = policy == CameraSystemBarsPolicy.ShowAll && !isDarkTheme
    if (controller.isAppearanceLightStatusBars != lightSystemBars) {
        controller.isAppearanceLightStatusBars = lightSystemBars
    }
    if (controller.isAppearanceLightNavigationBars != lightSystemBars) {
        controller.isAppearanceLightNavigationBars = lightSystemBars
    }

    if (hideNavigationBar) {
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    } else {
        controller.show(WindowInsetsCompat.Type.navigationBars())
    }
}
