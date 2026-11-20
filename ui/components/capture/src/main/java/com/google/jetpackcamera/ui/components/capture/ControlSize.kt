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

/**
 * Size variant a container requests for its capture controls. Independent of window size classes
 * and postures; each control maps it to its own dimensions. New sizes may be added, so `when`
 * expressions over it should include an `else` branch.
 */
@JvmInline
value class ControlSize private constructor(private val value: Int) {
    override fun toString(): String = when (this) {
        Mini -> "Mini"
        Medium -> "Medium"
        Standard -> "Standard"
        else -> "ControlSize($value)"
    }

    companion object {
        /** Smallest controls for tight containers; still meets the 48dp touch target. */
        val Mini = ControlSize(0)

        /** Intermediate controls, such as foldable side rails. */
        val Medium = ControlSize(1)

        /** Default control dimensions. */
        val Standard = ControlSize(2)
    }
}
