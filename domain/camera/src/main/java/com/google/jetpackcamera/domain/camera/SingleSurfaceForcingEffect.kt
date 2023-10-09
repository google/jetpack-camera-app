/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.google.jetpackcamera.domain.camera

import androidx.camera.core.CameraEffect

private const val TARGETS =
    CameraEffect.PREVIEW or CameraEffect.VIDEO_CAPTURE or CameraEffect.IMAGE_CAPTURE

private val emptySurfaceProcessor = EmptySurfaceProcessor()

/**
 * [CameraEffect] that applies a no-op effect.
 *
 * Essentially copying the camera input to the targets,
 * Preview, VideoCapture and ImageCapture.
 *
 * Used as a workaround to force the above 3 use cases to use a single camera stream.
 */
class SingleSurfaceForcingEffect : CameraEffect(
    TARGETS,
    emptySurfaceProcessor.glExecutor,
    emptySurfaceProcessor,
    {}
) {
    // TODO(b/304547401): Invoke this to release the processor properly
    @SuppressWarnings("unused")
    fun release() {
        emptySurfaceProcessor.release()
    }
}
