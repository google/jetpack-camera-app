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

import android.util.Log
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.CameraInputTransformer
import androidx.camera.core.SurfaceRequest.TransformationInfo as CXTransformationInfo
import androidx.camera.viewfinder.compose.Viewfinder
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.camera.viewfinder.surface.TransformationInfo
import androidx.camera.viewfinder.surface.ViewfinderSurfaceRequest
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

private const val TAG = "CameraXViewfinder"

/**
 * A composable viewfinder that adapts CameraX's [Preview.SurfaceProvider] to [Viewfinder]
 *
 * This adapter code will eventually be upstreamed to CameraX, but for now can be copied
 * in its entirety to connect CameraX to [Viewfinder].
 *
 * @param[modifier] the modifier to be applied to the layout
 * @param[surfaceRequest] a [SurfaceRequest] from [Preview.SurfaceProvider].
 * @param[implementationMode] the implementation mode, either [ImplementationMode.PERFORMANCE] or
 * [ImplementationMode.COMPATIBLE]. Currently, only [ImplementationMode.PERFORMANCE] will produce
 * the correct orientation.
 */
@Composable
fun CameraXViewfinder(
    modifier: Modifier = Modifier,
    surfaceRequest: SurfaceRequest,
    implementationMode: ImplementationMode = ImplementationMode.PERFORMANCE
) {
    val currentImplementationMode by rememberUpdatedState(implementationMode)
    var cameraInputTransformer = remember { CameraInputTransformer() }

    val viewfinderArgs by produceState<ViewfinderArgs?>(initialValue = null, surfaceRequest) {
        val viewfinderSurfaceRequest = ViewfinderSurfaceRequest.Builder(surfaceRequest.resolution)
            .build()

        surfaceRequest.addRequestCancellationListener(Runnable::run) {
            viewfinderSurfaceRequest.markSurfaceSafeToRelease()
        }

        // Launch undispatched so we always reach the try/finally in this coroutine
        launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                val surface = viewfinderSurfaceRequest.getSurface()
                surfaceRequest.provideSurface(surface, Runnable::run) {
                    viewfinderSurfaceRequest.markSurfaceSafeToRelease()
                }
            } finally {
                // If we haven't provided the surface, such as if we're cancelled
                // while suspending on getSurface(), this call will succeed. Otherwise
                // it will be a no-op.
                surfaceRequest.willNotProvideSurface()
            }
        }

        val transformationInfos = MutableStateFlow<CXTransformationInfo?>(null)
        surfaceRequest.setTransformationInfoListener(Runnable::run) {
            transformationInfos.value = it
        }

        // The ImplementationMode that will be used for all TransformationInfo updates.
        // This is locked in once we have updated ViewfinderArgs and won't change until
        // this produceState block is cancelled and restarted
        var snapshotImplementationMode: ImplementationMode? = null

        snapshotFlow { currentImplementationMode }
            .combine(transformationInfos.filterNotNull()) { implMode, transformInfo ->
                Pair(implMode, transformInfo)
            }.takeWhile { (implMode, _) ->
                val shouldAbort =
                    snapshotImplementationMode != null && implMode != snapshotImplementationMode
                if (shouldAbort) {
                    // Abort flow and invalidate SurfaceRequest so a new one will be sent
                    surfaceRequest.invalidate()
                }
                !shouldAbort
            }.collectLatest { (implMode, transformInfo) ->
                // We'll only ever get here with a single non-null implMode,
                // so setting it every time is ok
                snapshotImplementationMode = implMode
                value = ViewfinderArgs(
                    viewfinderSurfaceRequest,
                    implMode,
                    TransformationInfo(
                        transformInfo.rotationDegrees,
                        transformInfo.cropRect.left,
                        transformInfo.cropRect.right,
                        transformInfo.cropRect.top,
                        transformInfo.cropRect.bottom,
                        transformInfo.isMirroring
                    )
                )
            }
    }

    viewfinderArgs?.let { args ->
        Viewfinder(
            surfaceRequest = args.viewfinderSurfaceRequest,
            implementationMode = args.implementationMode,
            transformationInfo = args.transformationInfo,
            modifier = modifier.fillMaxSize().pointerInput(Unit) {
                detectTapGestures {
                    with(cameraInputTransformer) {
                        Log.d(TAG, "onTap: $it, onTapCameraSpace: ${it.toCameraSpace()}")
                    }
                }
            },
            cameraInputTransformer = cameraInputTransformer,
        )
    }
}

private data class ViewfinderArgs(
    val viewfinderSurfaceRequest: ViewfinderSurfaceRequest,
    val implementationMode: ImplementationMode,
    val transformationInfo: TransformationInfo
)
