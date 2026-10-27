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
package com.google.jetpackcamera.model

import android.view.Surface

enum class DeviceRotation {
    Natural,
    Rotated90,
    Rotated180,
    Rotated270;

    /**
     * Returns the rotation of the UI, expressed as a [Surface] rotation constant, needed to
     * compensate for device rotation.
     *
     * These values do not match up with the device rotation angle. When the device is rotated,
     * the UI must rotate in the opposite direction to compensate, so the angles 90 and 270 will
     * be swapped in UI rotation compared to device rotation.
     */
    fun toUiSurfaceRotation(): Int {
        return when (this) {
            Natural -> Surface.ROTATION_0
            Rotated90 -> Surface.ROTATION_270
            Rotated180 -> Surface.ROTATION_180
            Rotated270 -> Surface.ROTATION_90
        }
    }

    fun toClockwiseRotationDegrees(): Int {
        return when (this) {
            Natural -> 0
            Rotated90 -> 90
            Rotated180 -> 180
            Rotated270 -> 270
        }
    }

    /**
     * Returns the clockwise rotation in degrees needed to orient UI elements upright within a
     * natural-portrait coordinate frame.
     *
     * [Rotated180] (reverse portrait) is mapped to `0f` so camera controls do not flip upside-down.
     */
    fun toUiRotationDegrees(): Float {
        return when (this) {
            Natural, Rotated180 -> 0f
            Rotated90 -> -90f
            Rotated270 -> 90f
        }
    }

    /**
     * Returns the clockwise rotation in degrees needed to counter-rotate a root container back to
     * the device's natural portrait coordinate frame when the OS window is rotated to `this`
     * orientation.
     */
    fun toWindowCounterRotationDegrees(): Float {
        return when (this) {
            Natural -> 0f
            Rotated90 -> 90f
            Rotated180 -> 180f
            Rotated270 -> -90f
        }
    }

    val isLandscape: Boolean
        get() = this == Rotated90 || this == Rotated270

    companion object {
        fun fromSurfaceRotation(surfaceRotation: Int): DeviceRotation {
            return when (surfaceRotation) {
                Surface.ROTATION_0 -> Natural
                Surface.ROTATION_90 -> Rotated270
                Surface.ROTATION_180 -> Rotated180
                Surface.ROTATION_270 -> Rotated90
                else -> Natural
            }
        }

        fun snapFrom(degrees: Int): DeviceRotation {
            check(degrees in 0..359) {
                "Degrees must be in the range [0, 360)"
            }

            return when (val snappedDegrees = ((degrees + 45) / 90 * 90) % 360) {
                0 -> Natural
                90 -> Rotated90
                180 -> Rotated180
                270 -> Rotated270
                else -> throw IllegalStateException(
                    "Unexpected snapped degrees: $snappedDegrees" +
                        ". Should be one of 0, 90, 180 or 270."
                )
            }
        }
    }
}
