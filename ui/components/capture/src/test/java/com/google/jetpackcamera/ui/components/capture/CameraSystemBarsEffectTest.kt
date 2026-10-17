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
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests for [applySystemBars], [CameraSystemBarsEffect], and [ImmersiveNavigationBarEffect].
 */
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

        // First set dark icons so we also exercise the true -> false transition.
        applySystemBars(window = window, view = view, hideStatusBar = false, isDarkTheme = false)
        applySystemBars(window = window, view = view, hideStatusBar = false, isDarkTheme = true)

        assertThat(controller.systemBarsBehavior)
            .isEqualTo(WindowInsetsControllerCompat.BEHAVIOR_DEFAULT)
        assertThat(controller.isAppearanceLightStatusBars).isFalse()
        assertThat(controller.isAppearanceLightNavigationBars).isFalse()
    }

    @Test
    fun immersiveNavigationBarEffect_hidesNavigationBar_andRestoresOnDispose() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        val view = activity.window.decorView
        val requiresImmersive = androidx.compose.runtime.mutableStateOf(true)

        activity.setContentView(
            androidx.compose.ui.platform.ComposeView(activity).apply {
                setContent {
                    ImmersiveNavigationBarEffect(requiresImmersive = requiresImmersive.value)
                }
            }
        )
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)

        requiresImmersive.value = false
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()

        @Suppress("DEPRECATION")
        assertThat(view.systemUiVisibility and View.SYSTEM_UI_FLAG_HIDE_NAVIGATION)
            .isEqualTo(0)
    }
}
