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

import android.app.Activity
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import kotlinx.coroutines.yield

private const val TAG = "ScreenFlashComponents"

/**
 * Width of the white frame drawn around the viewfinder while the video screen torch is active.
 * The frame provides most of the illumination while leaving the preview and controls usable.
 */
private val VIDEO_SCREEN_TORCH_BORDER_WIDTH = 56.dp

/**
 * A composable that manages the screen illumination effects used for capture.
 *
 * This component displays a full-screen white overlay to simulate a photo flash and maximizes the
 * device's screen brightness while either the photo screen flash or the video screen torch is
 * active. After illumination ends, it restores the screen to its original brightness.
 *
 * @param screenFlashUiState The [ScreenFlashUiState] that controls the screen flash's state.
 * @param onInitialBrightnessCalculated Callback to provide the initial screen brightness before maximization.
 * @param isVideoScreenTorchEnabled Whether the screen should act as a continuous torch while
 * recording video with the front camera. When enabled, the screen brightness is maximized until
 * both the photo screen flash and the video screen torch are inactive.
 */
@Composable
fun ScreenFlashScreen(
    screenFlashUiState: ScreenFlashUiState,
    onInitialBrightnessCalculated: (Float) -> Unit,
    isVideoScreenTorchEnabled: Boolean = false
) {
    ScreenFlashOverlay(screenFlashUiState)

    if (screenFlashUiState.enabled || isVideoScreenTorchEnabled) {
        BrightnessMaximization(onInitialBrightnessCalculated = onInitialBrightnessCalculated)
    } else {
        screenFlashUiState.screenBrightnessToRestore?.let {
            // non-null brightness value means there is a value to restore
            BrightnessRestoration(
                brightness = it
            )
        }
    }
}

/**
 * A composable that displays a white overlay to simulate a screen flash.
 *
 * @param screenFlashUiState The [ScreenFlashUiState] that controls the overlay's visibility and behavior.
 * @param modifier The modifier for this composable.
 */
@Composable
private fun ScreenFlashOverlay(
    screenFlashUiState: ScreenFlashUiState,
    modifier: Modifier = Modifier
) {
    // Update overlay transparency gradually
    val disableAnimations = LocalDisableAnimations.current
    val targetAlpha = if (screenFlashUiState.enabled) 1f else 0f
    val alpha by animateFloatAsState(
        targetValue = targetAlpha,
        label = "screenFlashAlphaAnimation",
        animationSpec = if (disableAnimations) snap() else tween(),
        finishedListener = if (disableAnimations) {
            null
        } else {
            { _ ->
                screenFlashUiState.onChangeComplete()
            }
        }
    )

    if (disableAnimations) {
        LaunchedEffect(targetAlpha) {
            yield() // yield to avoid infinite recomposition
            screenFlashUiState.onChangeComplete()
        }
    }
    Box(
        modifier = modifier
            .run {
                if (screenFlashUiState.enabled) {
                    this.testTag(SCREEN_FLASH_OVERLAY)
                } else {
                    this
                }
            }
            .fillMaxSize()
            .background(color = Color.White.copy(alpha = alpha))
    )
}

/**
 * A composable that draws a white frame to use the screen as a torch while recording video.
 *
 * The frame maximizes the light emitted towards the subject while leaving the center of the screen,
 * where the preview is, untouched. It is drawn inside the viewfinder slot so that the controls stay
 * visible and usable, and it does not consume pointer input.
 *
 * @param modifier The modifier for this composable.
 */
@Composable
fun VideoScreenTorchFrame(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .testTag(VIDEO_SCREEN_TORCH_OVERLAY)
            .fillMaxSize()
    ) {
        val borderWidth = VIDEO_SCREEN_TORCH_BORDER_WIDTH.toPx()
            .coerceAtMost(size.minDimension / 2f)
        // Draw four bars instead of a stroked rectangle so that the frame is fully opaque and the
        // center of the screen stays transparent.
        drawRect(
            color = Color.White,
            size = Size(width = size.width, height = borderWidth)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(x = 0f, y = size.height - borderWidth),
            size = Size(width = size.width, height = borderWidth)
        )
        drawRect(
            color = Color.White,
            size = Size(width = borderWidth, height = size.height)
        )
        drawRect(
            color = Color.White,
            topLeft = Offset(x = size.width - borderWidth, y = 0f),
            size = Size(width = borderWidth, height = size.height)
        )
    }
}

@Composable
private fun BrightnessMaximization(onInitialBrightnessCalculated: (Float) -> Unit) {
    // This Composable is attached to Activity in current code, so will have Activity context.
    // If the Composable is attached to somewhere else in future, this needs to be updated too.
    val activity = LocalContext.current as? Activity ?: run {
        Log.e(TAG, "ScreenBrightness: could not find Activity context")
        return
    }

    val initialScreenBrightness = remember {
        getScreenBrightness(activity.window)
    }
    LaunchedEffect(initialScreenBrightness) {
        onInitialBrightnessCalculated(initialScreenBrightness)
    }

    DisposableEffect(activity) {
        setBrightness(activity, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL)
        onDispose {
            // Restore the original brightness when illumination ends. This also covers the case
            // where a photo screen flash clears the shared UI state while the video screen torch
            // is still active, which would otherwise leave the screen at maximum brightness.
            setBrightness(activity, initialScreenBrightness)
        }
    }
}

@Composable
private fun BrightnessRestoration(brightness: Float) {
    // This Composable is attached to Activity right now, so will have Activity context.
    // If the Composable is attached to somewhere else in future, this needs to be updated too.
    val activity = LocalContext.current as? Activity ?: run {
        Log.e(TAG, "ScreenBrightness: could not find Activity context")
        return
    }

    LaunchedEffect(brightness) {
        setBrightness(activity, brightness)
    }
}

private fun getScreenBrightness(window: Window): Float = window.attributes.screenBrightness

private fun setBrightness(activity: Activity, value: Float) {
    Log.d(TAG, "setBrightness: value = $value")
    val layoutParams: WindowManager.LayoutParams = activity.window.attributes
    layoutParams.screenBrightness = value
    activity.window.attributes = layoutParams
}
