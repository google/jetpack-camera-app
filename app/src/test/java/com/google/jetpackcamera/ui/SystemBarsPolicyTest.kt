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
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute
import com.google.jetpackcamera.permissions.navigation.PermissionsRoute
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [systemBarsPolicyFor] and [applySystemBars].
 *
 * The important cases here are the destinations that declare navigation arguments: their
 * `NavDestination.route` is the route *pattern*, so it still contains unresolved `{placeholder}`
 * query parameters. Matching on the raw string would silently fall through to the default policy.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class SystemBarsPolicyTest {

    private val previewRouteWithArgs = "$PreviewRoute" +
        "?externalCaptureMode={externalCaptureMode}" +
        "&reviewAfterCapture={reviewAfterCapture}" +
        "&captureUris={captureUris}" +
        "&debugSettings={debugSettings}"

    private val permissionsRouteWithArgs =
        "$PermissionsRoute?requestable_permissions={requestable_permissions}"

    @Test
    fun systemBarsPolicyFor_returnsExpectedPolicyPerRoute() {
        val cases = listOf(
            // Route pattern as reported by navigation for a destination with arguments.
            previewRouteWithArgs to SystemBarsPolicy.HideStatusBar,
            // Bare base route, e.g. when navigating without any arguments.
            "$PreviewRoute" to SystemBarsPolicy.HideStatusBar,
            Routes.POST_CAPTURE_ROUTE to SystemBarsPolicy.HideStatusBar,
            Routes.SETTINGS_ROUTE to SystemBarsPolicy.ShowAll,
            permissionsRouteWithArgs to SystemBarsPolicy.ShowAll,
            "$PermissionsRoute" to SystemBarsPolicy.ShowAll,
            null to SystemBarsPolicy.ShowAll,
            "someUnknownRoute" to SystemBarsPolicy.ShowAll,
            "someUnknownRoute?arg={arg}" to SystemBarsPolicy.ShowAll
        )

        for ((route, expected) in cases) {
            assertWithMessage("Unexpected policy for route: %s", route)
                .that(systemBarsPolicyFor(route))
                .isEqualTo(expected)
        }
    }

    @Test
    fun systemBarsPolicyFor_doesNotMatchRoutesThatMerelyStartWithACaptureRoute() {
        assertThat(systemBarsPolicyFor("${PreviewRoute}Something"))
            .isEqualTo(SystemBarsPolicy.ShowAll)
    }

    @Test
    fun applySystemBars_hideStatusBar_setsTransientBehaviorAndLightIcons() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val window = activity.window
        val view = window.decorView
        val controller = WindowCompat.getInsetsController(window, view)

        applySystemBars(window = window, view = view, hideStatusBar = true, isDarkTheme = false)

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        assertThat(controller.isAppearanceLightStatusBars).isFalse()
        assertThat(controller.isAppearanceLightNavigationBars).isFalse()
    }

    @Test
    fun applySystemBars_showAllInLightTheme_setsDefaultBehaviorAndDarkIcons() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val window = activity.window
        val view = window.decorView
        val controller = WindowCompat.getInsetsController(window, view)

        applySystemBars(window = window, view = view, hideStatusBar = true, isDarkTheme = false)
        applySystemBars(window = window, view = view, hideStatusBar = false, isDarkTheme = false)

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        assertThat(controller.isAppearanceLightStatusBars).isTrue()
        assertThat(controller.isAppearanceLightNavigationBars).isTrue()
    }

    @Test
    fun applySystemBars_showAllInDarkTheme_setsDefaultBehaviorAndLightIcons() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val window = activity.window
        val view = window.decorView
        val controller = WindowCompat.getInsetsController(window, view)

        // First set dark icons so we also exercise the true -> false transition.
        applySystemBars(window = window, view = view, hideStatusBar = false, isDarkTheme = false)
        applySystemBars(window = window, view = view, hideStatusBar = false, isDarkTheme = true)

        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        assertThat(controller.isAppearanceLightStatusBars).isFalse()
        assertThat(controller.isAppearanceLightNavigationBars).isFalse()
    }

    @Test
    fun systemBarsPolicyEffect_appliesPolicy_clampsInMultiWindow_andRestoresOnDispose() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = activity.window.decorView
        val composed = androidx.compose.runtime.mutableStateOf(true)

        activity.setContentView(
            androidx.compose.ui.platform.ComposeView(activity).apply {
                setContent {
                    if (composed.value) {
                        SystemBarsPolicyEffect(
                            policy = SystemBarsPolicy.HideStatusBar,
                            isDarkTheme = false
                        )
                    }
                }
            }
        )
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)

        // Entering multi-window mode clamps policy to ShowAll.
        activity.onMultiWindowModeChanged(true, android.content.res.Configuration())
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)

        // Exiting multi-window mode re-hides the status bar.
        activity.onMultiWindowModeChanged(false, android.content.res.Configuration())
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)

        // Disposing the effect when not finishing/changing configurations restores the status bar.
        composed.value = false
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
    }
}
