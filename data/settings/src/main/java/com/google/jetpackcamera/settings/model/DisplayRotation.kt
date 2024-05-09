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

enum class DisplayRotation(val value: Int) {
    Natural(Surface.ROTATION_0),
    Rotated90(Surface.ROTATION_90),
    Rotated180(Surface.ROTATION_180),
    Rotated270(Surface.ROTATION_270);

    companion object {
        fun of(value: Int): DisplayRotation {
            return when (value) {
                Surface.ROTATION_0 -> Natural
                Surface.ROTATION_90 -> Rotated90
                Surface.ROTATION_180 -> Rotated180
                Surface.ROTATION_270 -> Rotated270
                else -> throw IllegalArgumentException("Unsupported screen rotation: $value")
            }
        }
    }
}
