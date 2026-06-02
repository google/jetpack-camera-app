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
package com.google.jetpackcamera.core.camera

import androidx.camera.core.CameraEffect
import kotlinx.coroutines.CoroutineScope

/**
 * Provider for the camera effect that forces a single surface for use cases.
 */
interface SingleStreamEffectProvider {
    /**
     * Creates a new [CameraEffect] instance.
     *
     * @param coroutineScope The [CoroutineScope] in which the effect should run.
     */
    fun create(coroutineScope: CoroutineScope): CameraEffect
}
