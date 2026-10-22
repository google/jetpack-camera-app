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

import android.app.Dialog
import android.content.res.Configuration
import android.os.Looper
import android.view.View
import android.view.Window
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog as ComposeDialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [29])
class CameraSystemBarsEffectTest {

    @Test
    fun applySystemBars_hideStatusBar_setsTransientBehaviorAndLightIcons() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val window = activity.window
        val view = window.decorView
        val controller = WindowCompat.getInsetsController(window, view)

        applySystemBars(
            window = window,
            view = view,
            policy = CameraSystemBarsPolicy.HideStatusBar,
            isDarkTheme = false
        )

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(0)
        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        assertThat(controller.isAppearanceLightStatusBars).isFalse()
        assertThat(controller.isAppearanceLightNavigationBars).isFalse()
    }

    @Test
    fun applySystemBars_hideStatusAndNavigationBars_hidesBothBarsWithTransientBehavior() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val window = activity.window
        val view = window.decorView
        val controller = WindowCompat.getInsetsController(window, view)

        applySystemBars(
            window = window,
            view = view,
            policy = CameraSystemBarsPolicy.HideStatusAndNavigationBars,
            isDarkTheme = false
        )

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
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

        applySystemBars(
            window = window,
            view = view,
            policy = CameraSystemBarsPolicy.HideStatusAndNavigationBars,
            isDarkTheme = false
        )
        applySystemBars(
            window = window,
            view = view,
            policy = CameraSystemBarsPolicy.ShowAll,
            isDarkTheme = false
        )

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
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

        applySystemBars(
            window = window,
            view = view,
            policy = CameraSystemBarsPolicy.ShowAll,
            isDarkTheme = false
        )
        applySystemBars(
            window = window,
            view = view,
            policy = CameraSystemBarsPolicy.ShowAll,
            isDarkTheme = true
        )

        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        assertThat(controller.isAppearanceLightStatusBars).isFalse()
        assertThat(controller.isAppearanceLightNavigationBars).isFalse()
    }

    @Test
    fun cameraSystemBarsEffect_appliesAndSwitchesPolicy_clampsInMultiWindow_andRestoresOnDispose() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = activity.window.decorView
        val controller = WindowCompat.getInsetsController(activity.window, view)

        // Pre-configure the host Activity window with custom state to verify restore on dispose.
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        val composed = mutableStateOf(true)
        val policy = mutableStateOf(CameraSystemBarsPolicy.HideStatusBar)

        activity.setContentView(
            ComposeView(activity).apply {
                setContent {
                    if (composed.value) {
                        CameraSystemBarsEffect(
                            policy = policy.value,
                            isDarkTheme = true
                        )
                    }
                }
            }
        )
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(0)

        // Switching to HideStatusAndNavigationBars hides both bars.
        policy.value = CameraSystemBarsPolicy.HideStatusAndNavigationBars
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        // Switching to ShowAll shows both bars and resets behavior to BEHAVIOR_DEFAULT.
        policy.value = CameraSystemBarsPolicy.ShowAll
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(0)
        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)

        // Switching back to HideStatusAndNavigationBars re-hides both bars.
        policy.value = CameraSystemBarsPolicy.HideStatusAndNavigationBars
        shadowOf(Looper.getMainLooper()).idle()

        // Entering multi-window mode clamps policy to ShowAll.
        activity.onMultiWindowModeChanged(true, Configuration())
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(0)

        // Exiting multi-window mode re-applies HideStatusAndNavigationBars.
        activity.onMultiWindowModeChanged(false, Configuration())
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        // Disposing the effect restores both bars and the host's initial behavior and appearance.
        composed.value = false
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(0)
        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_BARS_BY_TOUCH)
        assertThat(controller.isAppearanceLightStatusBars).isTrue()
        assertThat(controller.isAppearanceLightNavigationBars).isTrue()
    }

    @Test
    fun cameraSystemBarsEffect_inDialog_modifiesOnlyDialogWindow_andLeavesActivityWindowAlone() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val activityView = activity.window.decorView
        val activityController = WindowCompat.getInsetsController(activity.window, activityView)

        // Configure the host Activity with light status/nav bar icons and default behavior.
        activityController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        activityController.isAppearanceLightStatusBars = true
        activityController.isAppearanceLightNavigationBars = true

        val showDialog = mutableStateOf(true)
        var capturedDialogWindow: Window? = null

        activity.setContentView(
            ComposeView(activity).apply {
                setContent {
                    if (showDialog.value) {
                        ComposeDialog(
                            onDismissRequest = { showDialog.value = false },
                            properties = DialogProperties(decorFitsSystemWindows = false)
                        ) {
                            val localView = LocalView.current
                            capturedDialogWindow = generateSequence(localView.parent) { it.parent }
                                .filterIsInstance<DialogWindowProvider>()
                                .firstOrNull()?.window
                            CameraSystemBarsEffect(
                                policy = CameraSystemBarsPolicy.HideStatusBar,
                                isDarkTheme = true
                            )
                        }
                    }
                }
            }
        )
        shadowOf(Looper.getMainLooper()).idle()

        val dialogWindow = checkNotNull(capturedDialogWindow)
        assertThat(dialogWindow).isNotSameInstanceAs(activity.window)
        val dialogDecorView = dialogWindow.decorView
        val dialogController = WindowCompat.getInsetsController(dialogWindow, dialogDecorView)

        // The Dialog window has its status bar hidden and transient swipe behavior enabled.
        @Suppress("DEPRECATION")
        assertThat(dialogDecorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
        assertThat(dialogController.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)

        // The host Activity window is untouched while the Dialog is showing.
        @Suppress("DEPRECATION")
        assertThat(activityView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
        assertThat(activityController.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        assertThat(activityController.isAppearanceLightStatusBars).isTrue()
        assertThat(activityController.isAppearanceLightNavigationBars).isTrue()

        // Dismissing the Dialog leaves the host Activity window untouched.
        showDialog.value = false
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(activityView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
        assertThat(activityController.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        assertThat(activityController.isAppearanceLightStatusBars).isTrue()
        assertThat(activityController.isAppearanceLightNavigationBars).isTrue()
    }

    @Test
    fun cameraSystemBarsEffect_explicitWindow_modifiesProvidedWindowInsteadOfActivityWindow() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val activityView = activity.window.decorView
        val platformDialog = Dialog(activity)
        platformDialog.show()
        val explicitWindow = checkNotNull(platformDialog.window)

        activity.setContentView(
            ComposeView(activity).apply {
                setContent {
                    CameraSystemBarsEffect(
                        policy = CameraSystemBarsPolicy.HideStatusBar,
                        window = explicitWindow
                    )
                }
            }
        )
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(explicitWindow.decorView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
        @Suppress("DEPRECATION")
        assertThat(activityView.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
    }

    @Test
    fun cameraSystemBarsEffect_skipsRestoreOnDispose_whenActivityIsChangingConfigurations() {
        val activity = Robolectric.buildActivity(TestComponentActivity::class.java).setup().get()
        val view = activity.window.decorView
        val composed = mutableStateOf(true)

        activity.setContentView(
            ComposeView(activity).apply {
                setContent {
                    if (composed.value) {
                        CameraSystemBarsEffect(
                            policy = CameraSystemBarsPolicy.HideStatusBar
                        )
                    }
                }
            }
        )
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)

        activity.changingConfigurations = true
        composed.value = false
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    private class TestComponentActivity : ComponentActivity() {
        var changingConfigurations: Boolean = false
        override fun isChangingConfigurations(): Boolean = changingConfigurations
    }

    @Test
    @Config(sdk = [30])
    fun cameraSystemBarsEffect_api30_appliesAndRestoresBehaviorAndAppearance() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val window = activity.window
        val view = window.decorView
        val controller = WindowCompat.getInsetsController(window, view)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true

        val composed = mutableStateOf(true)

        activity.setContentView(
            ComposeView(activity).apply {
                setContent {
                    if (composed.value) {
                        CameraSystemBarsEffect(
                            policy = CameraSystemBarsPolicy.HideStatusBar,
                            isDarkTheme = true
                        )
                    }
                }
            }
        )
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE)
        assertThat(controller.isAppearanceLightStatusBars).isFalse()
        assertThat(controller.isAppearanceLightNavigationBars).isFalse()

        composed.value = false
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        assertThat(controller.isAppearanceLightStatusBars).isTrue()
        assertThat(controller.isAppearanceLightNavigationBars).isTrue()
    }
}
