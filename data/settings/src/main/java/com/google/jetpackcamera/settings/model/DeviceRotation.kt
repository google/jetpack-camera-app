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
package com.google.jetpackcamera.settings.model

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

    companion object {
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
