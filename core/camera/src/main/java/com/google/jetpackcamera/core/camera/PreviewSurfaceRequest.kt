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

import android.view.Surface
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.core.ViewfinderSurfaceRequest
import kotlinx.coroutines.CompletableDeferred

/**
 * A sealed interface representing a request for a preview surface, abstracting the specific
 * viewfinder implementation required.
 */
sealed interface PreviewSurfaceRequest {
    /**
     * Wraps a CameraX [SurfaceRequest] for the production CameraXViewfinder.
     *
     * @property surfaceRequest The CameraX [SurfaceRequest].
     */
    data class CameraX(val surfaceRequest: SurfaceRequest) : PreviewSurfaceRequest

    /**
     * Wraps a [ViewfinderSurfaceRequest] for the standalone Viewfinder composable.
     *
     * @property surfaceRequest The standalone [ViewfinderSurfaceRequest].
     * @property surfaceDeferred A [CompletableDeferred] that will be completed with the [Surface]
     * once provided by the UI.
     */
    class Viewfinder(
        val surfaceRequest: ViewfinderSurfaceRequest,
        val surfaceDeferred: CompletableDeferred<Surface> = CompletableDeferred()
    ) : PreviewSurfaceRequest
}
