/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview.ui

import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.view.OrientationEventListener
import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import android.view.Surface
import android.view.WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
import android.view.WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.areSystemBarsVisible
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.max
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.jetpackcamera.settings.model.DeviceRotation
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.runningFold

/**
 * As long as this composable is active, the window will go into immersive mode and prevents the
 * rotation animation on configuration change. This will prevent the UI items from visually changing.
 *
 * When used in combination with a composable that renders the same UI in both landscape and portrait,
 * it can create a smooth continuous feel between those two orientations.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SmoothImmersiveRotationEffect(context: Context) {
    context.getActivity()?.window?.let { window ->
        val oldAreSystemBarsVisible = WindowInsets.areSystemBarsVisible
        DisposableEffect(context) {
            val oldRotationAnimation = window.attributes.rotationAnimation
            window.attributes = window.attributes.apply {
                rotationAnimation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ROTATION_ANIMATION_SEAMLESS
                } else {
                    ROTATION_ANIMATION_JUMPCUT
                }
            }
            val oldSystemBarsBehavior =
                with(WindowCompat.getInsetsController(window, window.decorView)) {
                    systemBarsBehavior.also {
                        systemBarsBehavior =
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        if (oldAreSystemBarsVisible) {
                            hide(WindowInsetsCompat.Type.systemBars())
                        }
                    }
                }

            onDispose {
                window.attributes = window.attributes.apply {
                    rotationAnimation = oldRotationAnimation
                }
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    systemBarsBehavior = oldSystemBarsBehavior
                    if (oldAreSystemBarsVisible) {
                        show(WindowInsetsCompat.Type.systemBars())
                    }
                }
            }
        }
    }
}

/**
 * Rotate a layout based on the current screen orientation.
 *
 * The UI will always be laid out in a way that matches how it would be laid out in the natural
 * orientation of the device, and is then rotated to match.
 */
@Composable
fun Modifier.rotatedLayout(baseRotation: Int = Surface.ROTATION_0): Modifier {
    val currentDisplayRotation = key(LocalConfiguration.current) {
        LocalView.current.display.rotation
    }
    val baseDegrees = surfaceRotationToRotationDegrees(baseRotation)
    val currentDegrees = surfaceRotationToRotationDegrees(currentDisplayRotation)
    val rotationAmount = ((currentDegrees - baseDegrees + 360) % 360)

    return this then Modifier
        .fillMaxSize()
        .layout { measurable, constraints ->
            val (width, height) = if (rotationAmount % 180 == 90) {
                Pair(constraints.maxHeight, constraints.maxWidth)
            } else {
                Pair(constraints.maxWidth, constraints.maxHeight)
            }

            val placeable = measurable.measure(
                Constraints.fixed(width, height)
            )
            layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    rotationZ = -rotationAmount.toFloat()
                }
            }
        }
}

/**
 * Animates orientation changes so the modified composable is upright when also using
 * [Modifier.rotatedLayout].
 */
@Composable
fun Modifier.animateToUpright(
    animationSpec: AnimationSpec<Float> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )
): Modifier {
    val newDisplayRotation = key(LocalConfiguration.current) {
        LocalView.current.display.rotation
    }
    val targetDegrees = surfaceRotationToRotationDegrees(newDisplayRotation).toFloat()
    val animatedDegrees = remember { Animatable(targetDegrees) }
    LaunchedEffect(targetDegrees) {
        // Ensure startDegrees is in [0, 360)
        val startDegrees = (animatedDegrees.value + 360) % 360
        val difference = (targetDegrees - startDegrees + 360) % 360
        val shortestPathTargetDegrees = if (difference < 180) {
            // Clockwise rotation
            startDegrees + difference
        } else if (difference > 180) {
            // Counter-clockwise rotation
            startDegrees + difference - 360
        } else {
            // Difference is 180. Use clockwise or counter-clockwise depending on startDegrees
            // so we don't always rotate the same direction for 180 degree rotations.
            startDegrees + (startDegrees - targetDegrees)
        }
        animatedDegrees.snapTo(startDegrees)
        animatedDegrees.animateTo(
            targetValue = shortestPathTargetDegrees,
            animationSpec = animationSpec
        )
    }

    return this then Modifier.graphicsLayer { rotationZ = animatedDegrees.value }
}

/**
 * Ensures the bottom padding is large enough to handle the provided insets no matter the
 * device orientation.
 *
 * This finds the maximum value of all insets in order to ensure that if the device is rotated
 * the padding will be sufficient to ensure the composable does not intersect any insets.
 *
 * This makes the assumption that the maximum inset size will not change depending on orientation.
 */
@Composable
fun Modifier.padBottomForSafeContentInAllOrientations(): Modifier {
    val layoutDirection = LocalLayoutDirection.current
    val maxPadding = WindowInsets.safeContent.asPaddingValues().let {
        val maxVertical = max(it.calculateTopPadding(), it.calculateBottomPadding())
        val maxHorizontal =
            max(it.calculateLeftPadding(layoutDirection), it.calculateRightPadding(layoutDirection))
        max(maxHorizontal, maxVertical)
    }

    return this then Modifier.padding(bottom = maxPadding)
}

private fun Context.getActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.getActivity()
    else -> null
}

/** Orientation hysteresis amount used in rounding, in degrees. */
private const val ORIENTATION_HYSTERESIS = 5
fun debouncedOrientationFlow(context: Context) = callbackFlow {
    val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            trySend(orientation)
        }
    }

    orientationListener.enable()

    awaitClose {
        orientationListener.disable()
    }
}.buffer(capacity = CONFLATED)
    .runningFold(initial = DeviceRotation.Natural) { prevSnap, newDegrees ->
        if (
            newDegrees != ORIENTATION_UNKNOWN &&
            abs(prevSnap.toClockwiseRotationDegrees() - newDegrees).let { min(it, 360 - it) } >=
            45 + ORIENTATION_HYSTERESIS
        ) {
            DeviceRotation.snapFrom(newDegrees)
        } else {
            prevSnap
        }
    }.distinctUntilChanged()

fun surfaceRotationToRotationDegrees(rotationValue: Int): Int = when (rotationValue) {
    Surface.ROTATION_0 -> 0
    Surface.ROTATION_90 -> 90
    Surface.ROTATION_180 -> 180
    Surface.ROTATION_270 -> 270
    else -> throw UnsupportedOperationException(
        "Unsupported display rotation: $rotationValue"
    )
}
