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
package com.google.jetpackcamera.core.camera.submode

import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider

/**
 * Defines how an active [CaptureSubModeProvider] binds its camera session.
 *
 * Two binding strategies are provided because camera sub-modes fall into two architectural
 * categories in CameraX:
 * 1. [SingleCamera] — Sub-modes backed by CameraX Extensions (`ExtensionMode.NIGHT`, `BOKEH`,
 *    `HDR`, `FACE_RETOUCH`, `AUTO`) operate on a single camera using the standard `Preview` +
 *    `ImageCapture` use-case graph; CameraX activates the extension by wrapping the base
 *    [CameraSelector] via `ExtensionsManager.getExtensionEnabledCameraSelector(...)`. Providing a
 *    [SingleCamera] selector transform lets extension sub-modes reuse 100% of
 *    `runSingleCameraSession` (focus/metering, zoom, flash, aspect ratio, rotation, and image
 *    capture) without duplicating its ~240-line session loop.
 * 2. [Custom] — Sub-modes that require a non-standard session topology (such as
 *    `ConcurrentCameraSession`, which binds two physical cameras simultaneously via
 *    `ProcessCameraProvider.bindToLifecycle(lifecycleOwner, ConcurrentCameraConfig)` with PiP
 *    `CompositionSettings`) replace `runSingleCameraSession` with their own session coroutine.
 */
sealed interface CameraSessionBinding {
    /**
     * Reuses the standard single-camera session (`runSingleCameraSession`) and transforms the
     * [CameraSelector] prior to binding use cases.
     */
    fun interface SingleCamera : CameraSessionBinding {
        suspend fun transformCameraSelector(
            cameraProvider: ProcessCameraProvider,
            baseSelector: CameraSelector
        ): CameraSelector
    }

    /**
     * Replaces the standard single-camera session with a custom session runner
     * (for example, binding a dual-camera concurrent session).
     *
     * @param sessionScope The active camera session context provided by `:core:camera`.
     */
    fun interface Custom : CameraSessionBinding {
        suspend fun runSession(sessionScope: Any)
    }
}
