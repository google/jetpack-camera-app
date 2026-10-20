/*
 * Copyright (C) 2025 The Android Open Source Project
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
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Surface
import android.view.WindowManager
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.google.jetpackcamera.model.DeviceRotation
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

private const val UI_ROTATION_DURATION_MS = 250

/**
 * Provides the current physical [DeviceRotation] (from [debouncedOrientationFlow], or frozen while
 * video recording is active) to descendant composables in the capture screen.
 */
internal val LocalDeviceRotation = compositionLocalOf { DeviceRotation.Natural }

/**
 * Provides the current OS window [DeviceRotation] so child composables (such as [PreviewDisplay])
 * know whether the host window is currently rotated away from natural portrait.
 */
internal val LocalWindowRotation = compositionLocalOf { DeviceRotation.Natural }

/**
 * Tracks the current OS display rotation synchronously with configuration changes and
 * [DisplayManager.DisplayListener] callbacks (including 180-degree landscape-to-landscape flips).
 */
@Composable
internal fun rememberWindowRotation(): DeviceRotation {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var surfaceRotation by remember(configuration) {
        mutableIntStateOf(context.currentDisplayRotation())
    }

    DisposableEffect(context) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) {}
            override fun onDisplayRemoved(displayId: Int) {}
            override fun onDisplayChanged(displayId: Int) {
                surfaceRotation = context.currentDisplayRotation()
            }
        }
        displayManager?.registerDisplayListener(listener, null)
        onDispose {
            displayManager?.unregisterDisplayListener(listener)
        }
    }

    return DeviceRotation.fromSurfaceRotation(surfaceRotation)
}

@Suppress("DEPRECATION")
private fun Context.currentDisplayRotation(): Int {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display?.rotation ?: Surface.ROTATION_0
        } else {
            val windowManager = getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            windowManager?.defaultDisplay?.rotation ?: Surface.ROTATION_0
        }
    } catch (_: UnsupportedOperationException) {
        Surface.ROTATION_0
    }
}

/**
 * Counter-rotates the root phone camera layout back to the device's canonical Portrait (`0°`)
 * coordinate system whenever the OS window rotates.
 *
 * Includes a synchronous window-bounds check so the counter-rotation is only applied when the
 * incoming layout constraints match the display rotation's aspect orientation.
 */
internal fun Modifier.counterRotateToPortrait(windowRotation: DeviceRotation): Modifier =
    layout { measurable, constraints ->
        val isConstraintsLandscape = constraints.maxWidth > constraints.maxHeight
        val effectiveWindowRotation = when {
            !isConstraintsLandscape && windowRotation.isLandscape -> DeviceRotation.Natural
            isConstraintsLandscape && !windowRotation.isLandscape -> DeviceRotation.Rotated270
            else -> windowRotation
        }

        val portraitMinWidth = min(constraints.minWidth, constraints.minHeight)
        val portraitMaxWidth = min(constraints.maxWidth, constraints.maxHeight)
        val portraitMinHeight = max(constraints.minWidth, constraints.minHeight)
        val portraitMaxHeight = max(constraints.maxWidth, constraints.maxHeight)

        val placeable = measurable.measure(
            constraints.copy(
                minWidth = portraitMinWidth,
                maxWidth = portraitMaxWidth,
                minHeight = portraitMinHeight,
                maxHeight = portraitMaxHeight
            )
        )

        val layoutWidth = if (effectiveWindowRotation.isLandscape) {
            placeable.height
        } else {
            placeable.width
        }
        val layoutHeight = if (effectiveWindowRotation.isLandscape) {
            placeable.width
        } else {
            placeable.height
        }

        layout(layoutWidth, layoutHeight) {
            val offsetX = (layoutWidth - placeable.width) / 2
            val offsetY = (layoutHeight - placeable.height) / 2
            placeable.placeWithLayer(offsetX, offsetY) {
                rotationZ = effectiveWindowRotation.toWindowCounterRotationDegrees()
            }
        }
    }

/**
 * Computes and animates the clockwise rotation in degrees needed to keep a UI element upright in a
 * canonical portrait coordinate frame:
 * - [DeviceRotation.Rotated180] (reverse portrait) maps to `0f` (never upside-down).
 * - `90°` transitions animate over `250ms` with [FastOutSlowInEasing].
 * - `180°` transitions (`Rotated90 <-> Rotated270`) or [LocalDisableAnimations] snap (`0ms`).
 */
@Composable
internal fun rememberAnimatedUiRotationDegrees(
    deviceRotation: DeviceRotation = LocalDeviceRotation.current
): Float {
    val disableAnimations = LocalDisableAnimations.current
    val targetDegrees = deviceRotation.toUiRotationDegrees()
    val animatable = remember { Animatable(targetDegrees) }

    LaunchedEffect(targetDegrees, disableAnimations) {
        val current = animatable.value
        val rotationDiff = (((targetDegrees - current) % 360f) + 540f) % 360f - 180f
        val newTarget = current + rotationDiff
        if (disableAnimations || abs(rotationDiff) > 90f) {
            animatable.snapTo(newTarget)
        } else if (rotationDiff != 0f) {
            animatable.animateTo(
                targetValue = newTarget,
                animationSpec = tween(
                    durationMillis = UI_ROTATION_DURATION_MS,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }

    return if (disableAnimations) targetDegrees else animatable.value
}

/**
 * Smoothly rotates a square/circular icon or button in-place around its center so it remains
 * upright as [deviceRotation] changes.
 */
internal fun Modifier.animateToUpright(deviceRotation: DeviceRotation? = null): Modifier =
    composed {
        val effectiveRotation = deviceRotation ?: LocalDeviceRotation.current
        val rotationDegrees = rememberAnimatedUiRotationDegrees(effectiveRotation)
        graphicsLayer {
            rotationZ = rotationDegrees
        }
    }

/**
 * Measures and places a rectangular composable with swapped width and height constraints when
 * [deviceRotation] is landscape ([DeviceRotation.Rotated90] or [DeviceRotation.Rotated270]),
 * rotating its layer so the content renders upright without clipping.
 */
internal fun Modifier.rotatedLayout(deviceRotation: DeviceRotation): Modifier =
    layout { measurable, constraints ->
        val isLandscape = deviceRotation.isLandscape
        val placeable = measurable.measure(
            if (isLandscape) {
                constraints.copy(
                    minWidth = constraints.minHeight,
                    maxWidth = constraints.maxHeight,
                    minHeight = constraints.minWidth,
                    maxHeight = constraints.maxWidth
                )
            } else {
                constraints
            }
        )
        layout(
            width = if (isLandscape) placeable.height else placeable.width,
            height = if (isLandscape) placeable.width else placeable.height
        ) {
            val offsetX = if (isLandscape) (placeable.height - placeable.width) / 2 else 0
            val offsetY = if (isLandscape) (placeable.width - placeable.height) / 2 else 0
            placeable.placeWithLayer(offsetX, offsetY) {
                rotationZ = deviceRotation.toUiRotationDegrees()
            }
        }
    }

/**
 * Remaps [WindowInsets] from the host OS window's coordinate frame into the canonical Portrait
 * (`0°`) coordinate frame used inside [counterRotateToPortrait].
 */
internal fun WindowInsets.toPortraitWindowInsets(windowRotation: DeviceRotation): WindowInsets =
    object : WindowInsets {
        override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int =
            when (windowRotation) {
                DeviceRotation.Natural ->
                    this@toPortraitWindowInsets.getLeft(density, LayoutDirection.Ltr)
                DeviceRotation.Rotated270 ->
                    this@toPortraitWindowInsets.getBottom(density)
                DeviceRotation.Rotated90 ->
                    this@toPortraitWindowInsets.getTop(density)
                DeviceRotation.Rotated180 ->
                    this@toPortraitWindowInsets.getRight(density, LayoutDirection.Ltr)
            }

        override fun getTop(density: Density): Int = when (windowRotation) {
            DeviceRotation.Natural ->
                this@toPortraitWindowInsets.getTop(density)
            DeviceRotation.Rotated270 ->
                this@toPortraitWindowInsets.getLeft(density, LayoutDirection.Ltr)
            DeviceRotation.Rotated90 ->
                this@toPortraitWindowInsets.getRight(density, LayoutDirection.Ltr)
            DeviceRotation.Rotated180 ->
                this@toPortraitWindowInsets.getBottom(density)
        }

        override fun getRight(density: Density, layoutDirection: LayoutDirection): Int =
            when (windowRotation) {
                DeviceRotation.Natural ->
                    this@toPortraitWindowInsets.getRight(density, LayoutDirection.Ltr)
                DeviceRotation.Rotated270 ->
                    this@toPortraitWindowInsets.getTop(density)
                DeviceRotation.Rotated90 ->
                    this@toPortraitWindowInsets.getBottom(density)
                DeviceRotation.Rotated180 ->
                    this@toPortraitWindowInsets.getLeft(density, LayoutDirection.Ltr)
            }

        override fun getBottom(density: Density): Int = when (windowRotation) {
            DeviceRotation.Natural ->
                this@toPortraitWindowInsets.getBottom(density)
            DeviceRotation.Rotated270 ->
                this@toPortraitWindowInsets.getRight(density, LayoutDirection.Ltr)
            DeviceRotation.Rotated90 ->
                this@toPortraitWindowInsets.getLeft(density, LayoutDirection.Ltr)
            DeviceRotation.Rotated180 ->
                this@toPortraitWindowInsets.getTop(density)
        }
    }
