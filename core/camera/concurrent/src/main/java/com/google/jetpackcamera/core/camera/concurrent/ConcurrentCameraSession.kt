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
@file:kotlin.OptIn(InternalCameraApi::class)

package com.google.jetpackcamera.core.camera.concurrent

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState as CXCameraState
import androidx.camera.core.CompositionSettings
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.lifecycle.asFlow
import com.google.jetpackcamera.core.camera.CameraSessionContext
import com.google.jetpackcamera.core.camera.InternalCameraApi
import com.google.jetpackcamera.core.camera.PerpetualSessionSettings
import com.google.jetpackcamera.core.camera.appLensFacing
import com.google.jetpackcamera.core.camera.applyDeviceRotation
import com.google.jetpackcamera.core.camera.attachPreviewSurfaceProvider
import com.google.jetpackcamera.core.camera.createUseCaseGroup
import com.google.jetpackcamera.core.camera.createVideoUseCase
import com.google.jetpackcamera.core.camera.getVideoCapture
import com.google.jetpackcamera.core.camera.processFocusMeteringEvents
import com.google.jetpackcamera.core.camera.processTransientSettingEvents
import com.google.jetpackcamera.core.camera.processVideoControlEvents
import com.google.jetpackcamera.core.camera.runWithConcurrent
import com.google.jetpackcamera.core.camera.toCameraError
import com.google.jetpackcamera.model.CameraError
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.model.VideoQuality
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "ConcurrentCameraSession"

@SuppressLint("RestrictedApi")
internal suspend fun CameraSessionContext.runConcurrentCameraSession(
    sessionSettings: PerpetualSessionSettings.CustomSession,
    availableConcurrentCameraInfos: List<List<CameraInfo>> =
        cameraProvider.availableConcurrentCameraInfos,
    bindConcurrentCameras: suspend (
        List<Pair<CameraSelector, CompositionSettings>>,
        UseCaseGroup,
        suspend CoroutineScope.(List<Camera>) -> Unit
    ) -> Unit = { cameraConfigs, useCaseGroup, block ->
        cameraProvider.runWithConcurrent(cameraConfigs, useCaseGroup) { concurrentCamera ->
            block(concurrentCamera.cameras)
        }
    }
) = coroutineScope {
    // The camera pair is resolved from the primary lens facing, so rebind whenever it changes
    // (e.g. when the user flips the camera).
    transientSettings
        .filterNotNull()
        .distinctUntilChanged { old, new -> old.primaryLensFacing == new.primaryLensFacing }
        .collectLatest { initialTransientSettings ->
            cameraProvider.unbindAll()
            val primaryFacing = initialTransientSettings.primaryLensFacing
            val secondaryFacing = primaryFacing.flip()
            val cameraPair =
                availableConcurrentCameraInfos.firstNotNullOfOrNull { group ->
                    var primary: CameraInfo? = null
                    var secondary: CameraInfo? = null
                    for (cameraInfo in group) {
                        when (cameraInfo.appLensFacing) {
                            primaryFacing -> primary = cameraInfo
                            secondaryFacing -> secondary = cameraInfo
                            else -> {}
                        }
                    }
                    primary?.let { nonNullPrimary ->
                        secondary?.let { nonNullSecondary ->
                            nonNullPrimary to nonNullSecondary
                        }
                    }
                }
            if (cameraPair == null) {
                currentCameraState.update { old ->
                    old.copy(
                        isCameraRunning = false,
                        cameraError = CameraError.StreamConfigError
                    )
                }
                return@collectLatest
            }
            val (primaryCameraInfo, secondaryCameraInfo) = cameraPair

            val primaryLensFacing = primaryCameraInfo.appLensFacing
            val secondaryLensFacing = secondaryCameraInfo.appLensFacing
            Log.d(
                TAG,
                "Starting new concurrent camera session " +
                    "[primary: $primaryLensFacing, secondary: $secondaryLensFacing]"
            )

            try {
                // Camera info lookup and use case construction can block, so they are kept off the
                // calling (typically main) thread.
                val useCaseGroup = withContext(backgroundDispatcher) {
                    val videoCapture = if (sessionSettings.captureMode != CaptureMode.IMAGE_ONLY) {
                        createVideoUseCase(
                            primaryCameraInfo,
                            sessionSettings.aspectRatio,
                            TARGET_FPS_AUTO,
                            StabilizationMode.OFF,
                            DynamicRange.SDR,
                            VideoQuality.UNSPECIFIED,
                            backgroundDispatcher
                        )
                    } else {
                        null
                    }

                    createUseCaseGroup(
                        cameraInfo = primaryCameraInfo,
                        initialTransientSettings = initialTransientSettings,
                        stabilizationMode = StabilizationMode.OFF,
                        aspectRatio = sessionSettings.aspectRatio,
                        imageFormat = ImageOutputFormat.JPEG,
                        captureMode = sessionSettings.captureMode,
                        videoCaptureUseCase = videoCapture
                    )
                }.apply { attachPreviewSurfaceProvider(surfaceRequests) }

                val cameraConfigs = listOf(
                    Pair(
                        primaryCameraInfo.cameraSelector,
                        CompositionSettings.Builder()
                            .setAlpha(1.0f)
                            .setOffset(0.0f, 0.0f)
                            .setScale(1.0f, 1.0f)
                            .build()
                    ),
                    Pair(
                        secondaryCameraInfo.cameraSelector,
                        CompositionSettings.Builder()
                            .setAlpha(1.0f)
                            .setOffset(2 / 3f - 0.1f, -2 / 3f + 0.1f)
                            .setScale(1 / 3f, 1 / 3f)
                            .build()
                    )
                )

                bindConcurrentCameras(cameraConfigs, useCaseGroup) { cameras ->
                    Log.d(TAG, "Concurrent camera session started")
                    // todo: concurrent camera only ever lists one camera
                    val primaryCamera = cameras.first {
                        it.cameraInfo.appLensFacing == primaryCameraInfo.appLensFacing
                    }

                    launch {
                        processFocusMeteringEvents(
                            primaryCamera.cameraInfo,
                            primaryCamera.cameraControl
                        )
                    }

                    launch {
                        processVideoControlEvents(
                            useCaseGroup.getVideoCapture(),
                            captureTypeSuffix = "DualCam"
                        )
                    }

                    launch {
                        primaryCameraInfo.torchState.asFlow().collectLatest { torchState ->
                            currentCameraState.update { old ->
                                old.copy(isTorchEnabled = torchState == TorchState.ON)
                            }
                        }
                    }

                    // Update CameraState to reflect when camera is running and any camera errors
                    launch {
                        combine(
                            primaryCameraInfo.cameraState.asFlow().filterNotNull(),
                            secondaryCameraInfo.cameraState.asFlow().filterNotNull()
                        ) { primaryState, secondaryState ->
                            primaryState to secondaryState
                        }
                            .distinctUntilChanged()
                            .onCompletion {
                                currentCameraState.update { old ->
                                    old.copy(
                                        isCameraRunning = false
                                    )
                                }
                            }
                            .collectLatest { (primaryState, secondaryState) ->
                                val mappedError =
                                    (primaryState.error ?: secondaryState.error)?.toCameraError(
                                        context
                                    )
                                val bothOpen = primaryState.type == CXCameraState.Type.OPEN &&
                                    secondaryState.type == CXCameraState.Type.OPEN
                                currentCameraState.update { old ->
                                    old.copy(
                                        isCameraRunning =
                                        primaryState.type == CXCameraState.Type.OPEN,
                                        cameraError = when {
                                            mappedError != null -> mappedError
                                            bothOpen -> null
                                            else -> old.cameraError
                                        }
                                    )
                                }
                            }
                    }

                    // update cameraState to mirror the current zoomState
                    launch {
                        primaryCamera.cameraInfo.zoomState.asFlow()
                            .filterNotNull()
                            .distinctUntilChanged()
                            .collectLatest { zoomState ->
                                val settings = transientSettings.value
                                // TODO: remove checks after buggy zoomState is fixed on API < 30
                                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                                    if (zoomState.zoomRatio != 1.0f ||
                                        settings == null ||
                                        zoomState.zoomRatio ==
                                        settings.zoomRatios[primaryCamera.cameraInfo.appLensFacing]
                                    ) {
                                        currentCameraState.update { old ->
                                            old.copy(
                                                zoomRatios = old.zoomRatios.toMutableMap().apply {
                                                    put(
                                                        primaryCamera.cameraInfo.appLensFacing,
                                                        zoomState.zoomRatio
                                                    )
                                                }.toMap(),
                                                linearZoomScales =
                                                old.linearZoomScales.toMutableMap().apply {
                                                    put(
                                                        primaryCamera.cameraInfo.appLensFacing,
                                                        zoomState.linearZoom
                                                    )
                                                }.toMap()
                                            )
                                        }
                                    }
                                }
                            }
                    }

                    applyDeviceRotation(initialTransientSettings.deviceRotation, useCaseGroup)
                    processTransientSettingEvents(
                        primaryCamera,
                        perLensConstraints[primaryLensFacing],
                        useCaseGroup,
                        initialTransientSettings,
                        transientSettings,
                        null
                    )
                }
            } catch (e: IllegalArgumentException) {
                Log.e(
                    TAG,
                    "Failed to bind concurrent use cases to lifecycle (stream config error)",
                    e
                )
                currentCameraState.update { old ->
                    old.copy(
                        isCameraRunning = false,
                        cameraError = if (cameraProvider.availableCameraInfos.isEmpty()) {
                            CameraError.CameraRemoved
                        } else {
                            CameraError.StreamConfigError
                        }
                    )
                }
                kotlinx.coroutines.awaitCancellation()
            }
        }
}
