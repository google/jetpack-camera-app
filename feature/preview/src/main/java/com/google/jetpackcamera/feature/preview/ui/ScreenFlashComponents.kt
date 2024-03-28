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

import android.app.Activity
import android.util.Log
import android.view.Window
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import com.google.jetpackcamera.feature.preview.ScreenFlash

private const val TAG = "ScreenFlashComponents"

@Composable
fun ScreenFlashScreen(
    screenFlashUiState: ScreenFlash.ScreenFlashUiState,
    onInitialBrightnessCalculated: (Float) -> Unit
) {
    ScreenFlashOverlay(screenFlashUiState)

    if (screenFlashUiState.enabled) {
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

@Composable
fun ScreenFlashOverlay(screenFlashUiState: ScreenFlash.ScreenFlashUiState) {
    // Update overlay transparency gradually
    val alpha by animateFloatAsState(
        targetValue = if (screenFlashUiState.enabled) 1f else 0f,
        label = "screenFlashAlphaAnimation",
        animationSpec = tween(),
        finishedListener = { screenFlashUiState.onChangeComplete() }
    )
    Box(
        modifier = Modifier
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

@Composable
fun BrightnessMaximization(onInitialBrightnessCalculated: (Float) -> Unit) {
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

    LaunchedEffect(Unit) {
        setBrightness(activity, WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL)
    }
}

@Composable
fun BrightnessRestoration(brightness: Float) {
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

fun getScreenBrightness(window: Window): Float = window.attributes.screenBrightness

fun setBrightness(activity: Activity, value: Float) {
    Log.d(TAG, "setBrightness: value = $value")
    val layoutParams: WindowManager.LayoutParams = activity.window.attributes
    layoutParams.screenBrightness = value
    activity.window.attributes = layoutParams
}
