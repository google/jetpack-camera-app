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

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.view.WindowManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemBarsIgnoringVisibility
import androidx.compose.foundation.layout.systemGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.jetpackcamera.model.DeviceRotation

/**
 * The system bar and display cutout insets of the host window, consistent with the window's
 * current configuration and rotation.
 *
 * @property systemBarsIgnoringVisibility the system bar insets, ignoring their visibility.
 * @property navigationBarsIgnoringVisibility the navigation bar insets, ignoring their visibility.
 * @property systemGestures the system gesture insets.
 * @property displayCutout the display cutout insets.
 * @property displayCutoutBoundingRects the bounding rectangles of the display cutout in window
 *   coordinates, or `null` if they are unavailable and should be read from the host view.
 */
@Immutable
internal class CurrentWindowInsets(
    val systemBarsIgnoringVisibility: WindowInsets,
    val navigationBarsIgnoringVisibility: WindowInsets,
    val systemGestures: WindowInsets,
    val displayCutout: WindowInsets,
    val displayCutoutBoundingRects: List<Rect>?
)

/**
 * Provides the [CurrentWindowInsets] of the capture layout, or `null` outside of it.
 */
internal val LocalCurrentWindowInsets = staticCompositionLocalOf<CurrentWindowInsets?> { null }

/**
 * Remembers the [CurrentWindowInsets] of the host window for the given [windowRotation].
 *
 * When the window rotates, it receives its new configuration and bounds one frame before its new
 * insets, so [WindowInsets] (and `View.getRootWindowInsets()`) still report the insets of the
 * previous rotation for the first frame laid out in the new rotation. Remapping those stale insets
 * with the new rotation shifts any content padded by them for that frame.
 *
 * On API 30+, the insets are instead read from [WindowManager.getCurrentWindowMetrics], which the
 * system computes for the window's current configuration. They are re-read whenever the
 * configuration, the window rotation, or the insets reported to the window change. On older API
 * levels, the insets reported to the window are used as is.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun rememberCurrentWindowInsets(windowRotation: DeviceRotation): CurrentWindowInsets {
    val reported = CurrentWindowInsets(
        systemBarsIgnoringVisibility = WindowInsets.systemBarsIgnoringVisibility,
        navigationBarsIgnoringVisibility = WindowInsets.navigationBarsIgnoringVisibility,
        systemGestures = WindowInsets.systemGestures,
        displayCutout = WindowInsets.displayCutout,
        displayCutoutBoundingRects = null
    )
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
        return reported
    }

    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    // Reading the reported insets also refreshes the window metrics for inset changes that are not
    // accompanied by a configuration change.
    val reportedValues = listOf(
        reported.systemBarsIgnoringVisibility,
        reported.navigationBarsIgnoringVisibility,
        reported.systemGestures,
        reported.displayCutout
    ).flatMap { it.values(density) }
    return remember(context, configuration, windowRotation, reportedValues) {
        context.currentWindowMetricsInsets() ?: reported
    }
}

private fun WindowInsets.values(density: Density): List<Int> = listOf(
    getLeft(density, LayoutDirection.Ltr),
    getTop(density),
    getRight(density, LayoutDirection.Ltr),
    getBottom(density)
)

@RequiresApi(Build.VERSION_CODES.R)
private fun Context.currentWindowMetricsInsets(): CurrentWindowInsets? {
    val windowManager = getSystemService(WindowManager::class.java) ?: return null
    val windowInsets = try {
        windowManager.currentWindowMetrics.windowInsets
    } catch (_: UnsupportedOperationException) {
        // Thrown when the context is not associated with a window.
        return null
    }
    return CurrentWindowInsets(
        systemBarsIgnoringVisibility = windowInsets.getInsetsIgnoringVisibility(
            android.view.WindowInsets.Type.systemBars()
        ).toWindowInsets(),
        navigationBarsIgnoringVisibility = windowInsets.getInsetsIgnoringVisibility(
            android.view.WindowInsets.Type.navigationBars()
        ).toWindowInsets(),
        systemGestures = windowInsets.getInsets(
            android.view.WindowInsets.Type.systemGestures()
        ).toWindowInsets(),
        displayCutout = windowInsets.getInsets(
            android.view.WindowInsets.Type.displayCutout()
        ).toWindowInsets(),
        displayCutoutBoundingRects = windowInsets.displayCutout?.boundingRects.orEmpty()
    )
}

private fun android.graphics.Insets.toWindowInsets(): WindowInsets =
    WindowInsets(left = left, top = top, right = right, bottom = bottom)
