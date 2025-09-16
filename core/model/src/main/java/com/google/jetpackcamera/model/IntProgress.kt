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

/**
 * Represents the progress of an integer value within a defined [IntRange].
 *
 * This class is used to track a current value that must stay within the bounds
 * of a given minimum and maximum (inclusive). It ensures that the `currentValue`
 * is always valid with respect to its `range`.
 */
data class IntProgress(
    val currentValue: Int,
    val range: IntRange
) {
    init {
        if (currentValue !in range) {
            throw IllegalArgumentException("Invalid progress. $currentValue not in range $range")
        }
    }

    operator fun inc() = IntProgress(currentValue + 1, range)
}
