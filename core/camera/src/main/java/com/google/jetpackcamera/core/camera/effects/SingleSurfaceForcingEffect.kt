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
package com.google.jetpackcamera.core.camera.effects

import androidx.camera.core.CameraEffect
import com.google.jetpackcamera.core.camera.effects.processors.CopyingSurfaceProcessor
import kotlinx.coroutines.CoroutineScope

private const val TARGETS =
    CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE

/**
 * [CameraEffect] that applies a no-op effect.
 *
 * Essentially copying the camera input to the targets,
 * Preview, VideoCapture and ImageCapture.
 *
 * Used as a workaround to force the above 3 use cases to use a single camera stream.
 */
class SingleSurfaceForcingEffect(coroutineScope: CoroutineScope) : CameraEffect(
    TARGETS,
    Runnable::run,
    CopyingSurfaceProcessor(coroutineScope),
    {}
)
