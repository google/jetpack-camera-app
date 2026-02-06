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


enum class AspectRatio(val numerator: Int, val denominator: Int) {
    THREE_FOUR(3, 4),
    NINE_SIXTEEN(9, 16),
    ONE_ONE(1, 1);

    /**
     * Returns the [Float] representation of the [AspectRatio].
     */
    fun toFloat(): Float = numerator.toFloat() / denominator

    /**
     * Returns the landscape aspect ratio as a [Float].
     */
    fun toLandscapeFloat(): Float = denominator.toFloat() / numerator


}
