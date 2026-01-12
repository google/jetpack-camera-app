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
package com.google.jetpackcamera.core.camera

import android.annotation.SuppressLint
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Log
import android.util.Rational
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraState as CXCameraState
import androidx.camera.core.CompositionSettings
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.lifecycle.asFlow
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.model.CameraConstraints
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "ConcurrentCameraSession"

context(CameraSessionContext)
@SuppressLint("RestrictedApi")
internal suspend fun runConcurrentCameraSession(
    sessionSettings: PerpetualSessionSettings.ConcurrentCamera,
    cameraConstraints: CameraConstraints?
) = coroutineScope {
    val primaryLensFacing = sessionSettings.primaryCameraInfo.appLensFacing
    val secondaryLensFacing = sessionSettings.secondaryCameraInfo.appLensFacing
    Log.d(
        TAG,
        "Starting new concurrent camera session " +
            "[primary: $primaryLensFacing, secondary: $secondaryLensFacing]"
    )

    val initialTransientSettings = transientSettings
        .filterNotNull()
        .first()

    val videoCapture =
        createVideoUseCase(
            cameraProvider.getCameraInfo(
                initialTransientSettings.primaryLensFacing.toCameraSelector()
            ),
            sessionSettings.aspectRatio,
            sessionSettings.captureMode,
            backgroundDispatcher,
            TARGET_FPS_AUTO,
            StabilizationMode.OFF,
            DynamicRange.SDR,
            VideoQuality.UNSPECIFIED
        )

    val useCaseGroup = createUseCaseGroup(
        cameraInfo = sessionSettings.primaryCameraInfo,
        initialTransientSettings = initialTransientSettings,
        stabilizationMode = StabilizationMode.OFF,
        aspectRatio = sessionSettings.aspectRatio,
        dynamicRange = DynamicRange.SDR,
        imageFormat = ImageOutputFormat.JPEG,
        captureMode = sessionSettings.captureMode,
        videoCaptureUseCase = videoCapture
    )

    val cameraConfigs = listOf(
        Pair(
            sessionSettings.primaryCameraInfo.cameraSelector,
            CompositionSettings.Builder()
                .setAlpha(1.0f)
                .setOffset(0.0f, 0.0f)
                .setScale(1.0f, 1.0f)
                .build()
        ),
        Pair(
            sessionSettings.secondaryCameraInfo.cameraSelector,
            CompositionSettings.Builder()
                .setAlpha(1.0f)
                .setOffset(2 / 3f - 0.1f, -2 / 3f + 0.1f)
                .setScale(1 / 3f, 1 / 3f)
                .build()
        )
    )

    cameraProvider.runWithConcurrent(cameraConfigs, useCaseGroup) { concurrentCamera ->
        Log.d(TAG, "Concurrent camera session started")
        // todo: concurrent camera only ever lists one camera
        val primaryCamera = concurrentCamera.cameras.first {
            it.cameraInfo.appLensFacing == sessionSettings.primaryCameraInfo.appLensFacing
        }

        launch {
            processFocusMeteringEvents(
                primaryCamera.cameraInfo,
                primaryCamera.cameraControl
            )
        }

        launch {
            processVideoControlEvents(
                useCaseGroup.useCases.getVideoCapture(),
                captureTypeSuffix = "DualCam"
            )
        }

        launch {
            sessionSettings.primaryCameraInfo.torchState.asFlow().collectLatest { torchState ->
                currentCameraState.update { old ->
                    old.copy(isTorchEnabled = torchState == TorchState.ON)
                }
            }
        }

        // Update CameraState to reflect when camera is running
        launch {
            primaryCamera.cameraInfo.cameraState
                .asFlow()
                .filterNotNull()
                .distinctUntilChanged()
                .onCompletion {
                    currentCameraState.update { old ->
                        old.copy(
                            isCameraRunning = false
                        )
                    }
                }
                .collectLatest { cameraState ->
                    currentCameraState.update { old ->
                        old.copy(
                            isCameraRunning = cameraState.type == CXCameraState.Type.OPEN
                        )
                    }
                }
        }

        // update cameraState to mirror the current zoomState
        launch {
            primaryCamera.cameraInfo.zoomState.asFlow().filterNotNull().distinctUntilChanged()
                .collectLatest { zoomState ->
                    val settings = transientSettings.value
                    // TODO(b/405987189): remove checks after buggy zoomState is fixed
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
                                    linearZoomScales = old.linearZoomScales.toMutableMap().apply {
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

        applyDeviceRotation(initialTransientSettings.deviceRotation, useCaseGroup.useCases)
        processTransientSettingEvents(
            primaryCamera,
            cameraConstraints,
            useCaseGroup.useCases,
            initialTransientSettings,
            transientSettings,
            null
        )
    }
}

context(CameraSessionContext)
internal suspend fun createUseCaseGroup(
    cameraInfo: CameraInfo,
    initialTransientSettings: TransientSessionSettings,
    stabilizationMode: StabilizationMode,
    aspectRatio: AspectRatio,
    videoCaptureUseCase: VideoCapture<Recorder>?,
    dynamicRange: DynamicRange,
    imageFormat: ImageOutputFormat,
    captureMode: CaptureMode,
    effect: CameraEffect? = null,
    captureResults: MutableStateFlow<TotalCaptureResult?>? = null
): UseCaseGroup {
    val previewUseCase =
        createPreviewUseCase(
            cameraInfo,
            aspectRatio,
            stabilizationMode,
            captureResults
        )

    // only create image use case in image or standard
    val imageCaptureUseCase = if (captureMode != CaptureMode.VIDEO_ONLY) {
        createImageUseCase(cameraInfo, aspectRatio, dynamicRange, imageFormat)
    } else {
        null
    }

    imageCaptureUseCase?.let {
        setFlashModeInternal(
            imageCapture = imageCaptureUseCase,
            flashMode = initialTransientSettings.flashMode,
            isFrontFacing = cameraInfo.appLensFacing == LensFacing.FRONT
        )
    }

    return UseCaseGroup.Builder().apply {
        Log.d(
            TAG,
            "Setting initial device rotation to ${initialTransientSettings.deviceRotation}"
        )
        setViewPort(
            ViewPort.Builder(
                Rational(aspectRatio.numerator, aspectRatio.denominator),
                // Initialize rotation to Preview's rotation, which comes from Display rotation
                previewUseCase.targetRotation
            ).build()
        )
        addUseCase(previewUseCase)

        // image and video use cases are only created if supported by the configuration
        imageCaptureUseCase?.let { addUseCase(imageCaptureUseCase) }
        videoCaptureUseCase?.let { addUseCase(videoCaptureUseCase) }

        effect?.let { addEffect(it) }
    }.build()
}
