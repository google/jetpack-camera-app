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
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
import android.view.WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.google.jetpackcamera.settings.model.DisplayRotation
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
@Composable
fun SmoothImmersiveRotationEffect(context: Context) = DisposableEffect(context) {
    var currentRotationAnimation: Int? = null
    context.getActivity()?.window?.let { window ->
        window.attributes = window.attributes.apply {
            currentRotationAnimation = rotationAnimation
            rotationAnimation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ROTATION_ANIMATION_SEAMLESS
            } else {
                ROTATION_ANIMATION_JUMPCUT
            }
        }
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
    onDispose {
        context.getActivity()?.window?.let { window ->
            if (currentRotationAnimation != null) {
                window.attributes = window.attributes.apply {
                    rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_ROTATE
                }
            }
            WindowCompat.getInsetsController(window, window.decorView).apply {
                show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}

/**
 * Rotate a layout based on the current screen orientation. The UI will always be laid out in a way
 * that width <= height, and rotated afterwards.
 */
@Composable
fun Modifier.rotatedLayout(): Modifier {
    var currentOrientation by remember { mutableIntStateOf(Surface.ROTATION_0) }
    val currentDegrees = currentOrientation * 90f
    val newOrientation = LocalConfiguration.current.orientation
    val display = LocalView.current.display
    LaunchedEffect(newOrientation, display) {
        val newRotation = display.rotation
        if (currentOrientation != newRotation) {
            currentOrientation = newRotation
        }
    }
    return this then Modifier
        .fillMaxSize()
        .layout { measurable, constraints ->
            val height = maxOf(constraints.maxWidth, constraints.maxHeight)
            val width = minOf(constraints.maxWidth, constraints.maxHeight)
            val placeable = measurable.measure(
                Constraints.fixed(width, height)
            )
            layout(placeable.width, placeable.height) {
                placeable.placeWithLayer(0, 0) {
                    if (constraints.maxWidth > constraints.maxHeight) {
                        rotationZ = -currentDegrees
                    }
                }
            }
        }
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
    .runningFold(initial = DisplayRotation.Natural) { prevSnap, newDegrees ->
        if (
            newDegrees != ORIENTATION_UNKNOWN &&
            abs(prevSnap.toClockwiseRotationDegrees() - newDegrees).let { min(it, 360 - it) } >=
            45 + ORIENTATION_HYSTERESIS
        ) {
            DisplayRotation.snapFrom(newDegrees)
        } else {
            prevSnap
        }
    }.distinctUntilChanged()
