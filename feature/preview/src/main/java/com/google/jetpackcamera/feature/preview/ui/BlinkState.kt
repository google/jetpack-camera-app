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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import kotlinx.coroutines.CoroutineScope

class BlinkState(
    initialAlpha: Float = 1F,
    coroutineScope: CoroutineScope
) {
    private val animatable = Animatable(initialAlpha)
    val alpha: Float get() = animatable.value
    val scope = coroutineScope

    suspend fun play() {
        animatable.snapTo(0F)
        animatable.animateTo(1F, animationSpec = tween(800))
    }
}