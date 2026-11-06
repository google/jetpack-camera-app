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
import android.view.Display
import android.view.OrientationEventListener
import android.view.OrientationEventListener.ORIENTATION_UNKNOWN
import com.google.jetpackcamera.model.DeviceRotation
import kotlin.math.abs
import kotlin.math.min
import kotlinx.coroutines.channels.Channel.Factory.CONFLATED
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.runningFold

/** Orientation hysteresis amount used in rounding, in degrees. */
private const val ORIENTATION_HYSTERESIS = 5

/**
 * A flow that emits the device's orientation, debounced to avoid rapid changes.
 *
 * Seeded with the current display rotation from [context] so that starting or restarting
 * collection immediately reports the active rotation before the sensor delivers a new reading.
 *
 * @param context the application or display context
 */
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
    .runningFold(initial = currentDeviceRotation(context)) { prevSnap, newDegrees ->
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

private fun currentDeviceRotation(context: Context): DeviceRotation {
    val surfaceRotation = context.getSystemService(DisplayManager::class.java)
        ?.getDisplay(Display.DEFAULT_DISPLAY)
        ?.rotation
        ?: return DeviceRotation.Natural
    return DeviceRotation.fromUiSurfaceRotation(surfaceRotation)
}
