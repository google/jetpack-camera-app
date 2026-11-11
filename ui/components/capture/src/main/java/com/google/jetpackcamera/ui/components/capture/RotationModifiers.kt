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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import android.view.Surface
import android.view.View
import android.view.WindowManager
import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.model.DeviceRotation

private const val UI_ROTATION_DURATION_MS = 250
internal val DefaultGravityRotationState: State<Float> = mutableFloatStateOf(0f)

/**
 * Provides the current physical gravity [DeviceRotation] to descendant composables in the capture
 * screen.
 */
val LocalDeviceRotation: ProvidableCompositionLocal<DeviceRotation> =
    compositionLocalOf { DeviceRotation.Natural }

/**
 * Provides the current OS window/display [DeviceRotation] to descendant composables in the capture
 * screen.
 */
val LocalWindowRotation: ProvidableCompositionLocal<DeviceRotation> =
    compositionLocalOf { DeviceRotation.Natural }

/**
 * Provides a shared, root-animated [State] holding the continuous clockwise gravity compensation
 * angle in degrees. Reading [State.value] inside [Modifier.graphicsLayer] updates only the draw/
 * RenderNode phase with zero recompositions or layout passes.
 */
val LocalGravityRotationState: ProvidableCompositionLocal<State<Float>> =
    compositionLocalOf { DefaultGravityRotationState }

/**
 * Tracks the current OS display rotation synchronously on configuration changes and via
 * [DisplayManager.DisplayListener] while `STARTED` (catching seamless 180-degree
 * landscape-to-landscape flips that do not trigger configuration changes).
 */
@Composable
fun rememberWindowRotation(): DeviceRotation {
    val view = LocalView.current
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    var windowRotation by remember {
        mutableStateOf(readCurrentDisplayRotation(view, context))
    }

    LaunchedEffect(configuration, view, context) {
        windowRotation = readCurrentDisplayRotation(view, context)
    }

    LifecycleStartEffect(view, context) {
        val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit
            override fun onDisplayRemoved(displayId: Int) = Unit
            override fun onDisplayChanged(displayId: Int) {
                val targetDisplayId = view.display?.displayId ?: Display.DEFAULT_DISPLAY
                if (displayId == targetDisplayId) {
                    val updated = readCurrentDisplayRotation(view, context)
                    if (updated != windowRotation) {
                        windowRotation = updated
                    }
                }
            }
        }
        windowRotation = readCurrentDisplayRotation(view, context)
        displayManager?.registerDisplayListener(listener, null)
        onStopOrDispose {
            displayManager?.unregisterDisplayListener(listener)
        }
    }

    return windowRotation
}

private fun readCurrentDisplayRotation(view: View, context: Context): DeviceRotation {
    val surfaceRotation = runCatching {
        (view.display ?: ContextCompat.getDisplayOrDefault(context)).rotation
    }.getOrDefault(Surface.ROTATION_0)
    return DeviceRotation.fromUiSurfaceRotation(surfaceRotation)
}

/**
 * Collects [debouncedOrientationFlow] while the current [LocalLifecycleOwner] is at least
 * [Lifecycle.State.STARTED], returning the latest physical [DeviceRotation] relative to gravity.
 */
@Composable
fun rememberDeviceRotation(): DeviceRotation {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var deviceRotation by remember { mutableStateOf(currentDeviceRotation(context)) }
    LaunchedEffect(context, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            debouncedOrientationFlow(context).collect { rotation ->
                deviceRotation = rotation
            }
        }
    }
    return deviceRotation
}

/**
 * Remembers a single continuous clockwise rotation [State] in degrees that keeps UI elements
 * oriented upright against gravity inside the current window:
 * - Uses shortest-path angle interpolation (`±90°`) so transitions across `0° <-> 270°` never spin
 *   `270°` the long way around.
 * - Snaps immediately when [windowRotation] changes (since the OS is already animating the window
 *   rotation) or when [LocalDisableAnimations] is `true`.
 */
@Composable
fun rememberGravityRotationState(
    deviceRotation: DeviceRotation = LocalDeviceRotation.current,
    windowRotation: DeviceRotation = LocalWindowRotation.current
): State<Float> {
    val targetClockwiseDegrees = deviceRotation.toGravityRotationDegrees(windowRotation)
    var continuousTarget by remember { mutableFloatStateOf(targetClockwiseDegrees.toFloat()) }
    var previousWindowRotation by remember { mutableStateOf(windowRotation) }

    val windowRotationChanged = previousWindowRotation != windowRotation
    if (windowRotationChanged) {
        previousWindowRotation = windowRotation
    }

    val currentMod = ((continuousTarget % 360f) + 360f) % 360f
    val rawDelta = ((targetClockwiseDegrees - currentMod + 540f) % 360f) - 180f
    val delta = if (rawDelta == -180f) 180f else rawDelta
    if (delta != 0f) {
        continuousTarget += delta
    }

    val disableAnimations = LocalDisableAnimations.current
    return animateFloatAsState(
        targetValue = continuousTarget,
        animationSpec = if (disableAnimations || windowRotationChanged) {
            snap()
        } else {
            tween(
                durationMillis = UI_ROTATION_DURATION_MS,
                easing = FastOutSlowInEasing
            )
        },
        label = "GravityRotationDegrees"
    )
}

/**
 * Rotates a square or circular icon/button in-place around its center in the draw/layer phase so it
 * stays upright relative to physical gravity.
 */
fun Modifier.animateToUpright(rotationState: State<Float>? = null): Modifier = composed {
    val effectiveState = rotationState ?: LocalGravityRotationState.current
    graphicsLayer {
        rotationZ = effectiveState.value
    }
}

/**
 * Measures and places a rectangular composable with swapped width and height constraints when
 * [rotationDegrees] is `90°` or `270°`, centering the rotated layer within the swapped layout box
 * so rectangular overlays orient upright without clipping or misaligned hit targets.
 */
fun Modifier.rotateLayoutToGravity(rotationDegrees: Int): Modifier =
    layout { measurable, constraints ->
        val normalized = ((rotationDegrees % 360) + 360) % 360
        val swapAxes = normalized == 90 || normalized == 270
        val childConstraints = if (swapAxes) {
            val swappedMaxWidth = if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                constraints.maxWidth
            }
            val swappedMaxHeight = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                constraints.maxHeight
            }
            Constraints(
                minWidth = constraints.minHeight.coerceAtMost(swappedMaxWidth),
                maxWidth = swappedMaxWidth,
                minHeight = constraints.minWidth.coerceAtMost(swappedMaxHeight),
                maxHeight = swappedMaxHeight
            )
        } else {
            constraints
        }
        val placeable = measurable.measure(childConstraints)
        val layoutWidth = if (swapAxes) placeable.height else placeable.width
        val layoutHeight = if (swapAxes) placeable.width else placeable.height
        layout(layoutWidth, layoutHeight) {
            val offsetX = (layoutWidth - placeable.width) / 2
            val offsetY = (layoutHeight - placeable.height) / 2
            placeable.placeWithLayer(offsetX, offsetY) {
                rotationZ = normalized.toFloat()
            }
        }
    }

/**
 * Counter-rotates a compact portrait container back to the device's canonical portrait (`0°`)
 * coordinate frame when [windowRotation] is landscape ([DeviceRotation.Rotated90] or
 * [DeviceRotation.Rotated270]).
 *
 * When [windowRotation] is portrait ([DeviceRotation.Natural] or [DeviceRotation.Rotated180]),
 * measures with [Constraints] unchanged and places at `(0, 0)` with zero layer rotation.
 *
 * @param windowRotation Active OS window [DeviceRotation] to counter-rotate from.
 */
fun Modifier.counterRotateToPortrait(windowRotation: DeviceRotation): Modifier =
    layout { measurable, constraints ->
        if (!windowRotation.isLandscape) {
            val placeable = measurable.measure(constraints)
            layout(placeable.width, placeable.height) {
                placeable.place(0, 0)
            }
        } else {
            val swappedMaxWidth = if (constraints.hasBoundedHeight) {
                constraints.maxHeight
            } else {
                Constraints.Infinity
            }
            val swappedMaxHeight = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                Constraints.Infinity
            }
            val childConstraints = Constraints(
                minWidth = constraints.minHeight.coerceAtMost(swappedMaxWidth),
                maxWidth = swappedMaxWidth,
                minHeight = constraints.minWidth.coerceAtMost(swappedMaxHeight),
                maxHeight = swappedMaxHeight
            )
            val placeable = measurable.measure(childConstraints)
            val layoutWidth = placeable.height
            val layoutHeight = placeable.width
            layout(layoutWidth, layoutHeight) {
                val offsetX = (layoutWidth - placeable.width) / 2
                val offsetY = (layoutHeight - placeable.height) / 2
                placeable.placeWithLayer(offsetX, offsetY) {
                    rotationZ = windowRotation.toWindowCounterRotationDegrees()
                }
            }
        }
    }

/**
 * [WindowInsets] wrapper that remaps physical window edges from the host OS window's
 * [windowRotation] coordinate frame into the canonical portrait (`0°`) coordinate frame used inside
 * [counterRotateToPortrait].
 *
 * @property baseInsets Underlying host [WindowInsets] in the OS window's coordinate frame.
 * @property windowRotation Effective [DeviceRotation] applied by the enclosing counter-rotated
 *   container.
 */
@Immutable
class PortraitWindowInsets(
    private val baseInsets: WindowInsets,
    private val windowRotation: DeviceRotation
) : WindowInsets {
    override fun getLeft(density: Density, layoutDirection: LayoutDirection): Int =
        when (windowRotation) {
            DeviceRotation.Natural -> baseInsets.getLeft(density, LayoutDirection.Ltr)
            DeviceRotation.Rotated270 -> baseInsets.getBottom(density)
            DeviceRotation.Rotated90 -> baseInsets.getTop(density)
            DeviceRotation.Rotated180 -> baseInsets.getRight(density, LayoutDirection.Ltr)
        }

    override fun getTop(density: Density): Int = when (windowRotation) {
        DeviceRotation.Natural -> baseInsets.getTop(density)
        DeviceRotation.Rotated270 -> baseInsets.getLeft(density, LayoutDirection.Ltr)
        DeviceRotation.Rotated90 -> baseInsets.getRight(density, LayoutDirection.Ltr)
        DeviceRotation.Rotated180 -> baseInsets.getBottom(density)
    }

    override fun getRight(density: Density, layoutDirection: LayoutDirection): Int =
        when (windowRotation) {
            DeviceRotation.Natural -> baseInsets.getRight(density, LayoutDirection.Ltr)
            DeviceRotation.Rotated270 -> baseInsets.getTop(density)
            DeviceRotation.Rotated90 -> baseInsets.getBottom(density)
            DeviceRotation.Rotated180 -> baseInsets.getLeft(density, LayoutDirection.Ltr)
        }

    override fun getBottom(density: Density): Int = when (windowRotation) {
        DeviceRotation.Natural -> baseInsets.getBottom(density)
        DeviceRotation.Rotated270 -> baseInsets.getRight(density, LayoutDirection.Ltr)
        DeviceRotation.Rotated90 -> baseInsets.getLeft(density, LayoutDirection.Ltr)
        DeviceRotation.Rotated180 -> baseInsets.getTop(density)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PortraitWindowInsets) return false
        return baseInsets == other.baseInsets && windowRotation == other.windowRotation
    }

    override fun hashCode(): Int {
        var result = baseInsets.hashCode()
        result = 31 * result + windowRotation.hashCode()
        return result
    }

    override fun toString(): String =
        "PortraitWindowInsets(baseInsets=$baseInsets, windowRotation=$windowRotation)"
}

/**
 * Remaps this [WindowInsets] instance from the host OS window's [windowRotation] coordinate frame
 * into the canonical portrait (`0°`) coordinate frame.
 *
 * @param windowRotation Effective [DeviceRotation] applied by the enclosing counter-rotated
 *   container.
 * @return A [PortraitWindowInsets] remapping all four edges into portrait coordinates.
 */
fun WindowInsets.toPortraitWindowInsets(windowRotation: DeviceRotation): WindowInsets =
    PortraitWindowInsets(this, windowRotation)

/**
 * Requests [WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS] on the host [Activity] window
 * on API 26+ while [enabled] is `true`, restoring the previous window rotation animation on
 * disposal so counter-rotated compact camera chrome transitions across OS window rotations without
 * a crossfade jump.
 */
@Composable
internal fun SeamlessRotationAnimationEffect(enabled: Boolean = true) {
    val context = LocalContext.current
    val activity = LocalActivity.current ?: remember(context) { context.findActivity() }
    DisposableEffect(activity, enabled) {
        val window = activity?.window
        if (enabled && window != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val previousAnimation = window.attributes.rotationAnimation
            if (previousAnimation != WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS) {
                window.attributes = window.attributes.apply {
                    rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_SEAMLESS
                }
            }
            onDispose {
                window.attributes = window.attributes.apply {
                    rotationAnimation = previousAnimation
                }
            }
        } else {
            onDispose {}
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
