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

import android.content.res.Configuration
import android.os.Looper
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
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

        applySystemBars(window = window, view = view, hideStatusBar = false, isDarkTheme = false)
        applySystemBars(window = window, view = view, hideStatusBar = false, isDarkTheme = true)

        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        assertThat(controller.isAppearanceLightStatusBars).isFalse()
        assertThat(controller.isAppearanceLightNavigationBars).isFalse()
    }

    @Test
    fun cameraSystemBarsEffect_appliesPolicy_clampsInMultiWindow_andRestoresOnDispose() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = activity.window.decorView
        val composed = mutableStateOf(true)

        activity.setContentView(
            ComposeView(activity).apply {
                setContent {
                    if (composed.value) {
                        CameraSystemBarsEffect(
                            enabled = true,
                            isDarkTheme = false
                        )
                    }
                }
            }
        )
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)

        // Entering multi-window mode clamps policy to ShowAll.
        activity.onMultiWindowModeChanged(true, Configuration())
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)

        // Exiting multi-window mode re-hides the status bar.
        activity.onMultiWindowModeChanged(false, Configuration())
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(View.SYSTEM_UI_FLAG_FULLSCREEN)

        // Disposing the effect when not finishing/changing configurations restores the status bar.
        composed.value = false
        shadowOf(Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_FULLSCREEN)
            .isEqualTo(0)
    }
}
