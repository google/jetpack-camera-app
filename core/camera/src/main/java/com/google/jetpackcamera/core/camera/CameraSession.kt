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

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.TotalCaptureResult
import android.net.Uri
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import androidx.annotation.OptIn
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraEffect
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalImageCaptureOutputFormat
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.core.TorchState
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.video.VideoRecordEvent.Finalize.ERROR_NONE
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.checkSelfPermission
import androidx.lifecycle.asFlow
import com.google.jetpackcamera.core.camera.effects.SingleSurfaceForcingEffect
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DeviceRotation
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.Stabilization
import java.io.File
import java.util.Date
import java.util.concurrent.Executor
import kotlin.coroutines.ContinuationInterceptor
import kotlin.math.abs
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TAG = "CameraSession"

context(CameraSessionContext)
internal suspend fun runSingleCameraSession(
    sessionSettings: PerpetualSessionSettings.SingleCamera,
    useCaseMode: CameraUseCase.UseCaseMode,
    // TODO(tm): ImageCapture should go through an event channel like VideoCapture
    onImageCaptureCreated: (ImageCapture) -> Unit = {}
) = coroutineScope {
    val lensFacing = sessionSettings.cameraInfo.appLensFacing
    Log.d(TAG, "Starting new single camera session for $lensFacing")

    val initialTransientSettings = transientSettings
        .filterNotNull()
        .first()

    val useCaseGroup = createUseCaseGroup(
        cameraInfo = sessionSettings.cameraInfo,
        initialTransientSettings = initialTransientSettings,
        stabilizePreviewMode = sessionSettings.stabilizePreviewMode,
        stabilizeVideoMode = sessionSettings.stabilizeVideoMode,
        aspectRatio = sessionSettings.aspectRatio,
        targetFrameRate = sessionSettings.targetFrameRate,
        dynamicRange = sessionSettings.dynamicRange,
        imageFormat = sessionSettings.imageFormat,
        useCaseMode = useCaseMode,
        effect = when (sessionSettings.captureMode) {
            CaptureMode.SINGLE_STREAM -> SingleSurfaceForcingEffect(this@coroutineScope)
            CaptureMode.MULTI_STREAM -> null
        }
    ).apply {
        getImageCapture()?.let(onImageCaptureCreated)
    }

    cameraProvider.runWith(sessionSettings.cameraInfo.cameraSelector, useCaseGroup) { camera ->
        Log.d(TAG, "Camera session started")

        launch {
            processFocusMeteringEvents(camera.cameraControl)
        }

        launch {
            processVideoControlEvents(
                camera,
                useCaseGroup.getVideoCapture(),
                captureTypeSuffix = when (sessionSettings.captureMode) {
                    CaptureMode.MULTI_STREAM -> "MultiStream"
                    CaptureMode.SINGLE_STREAM -> "SingleStream"
                }
            )
        }

        launch {
            camera.cameraInfo.torchState.asFlow().collectLatest { torchState ->
                currentCameraState.update { old ->
                    old.copy(torchEnabled = torchState == TorchState.ON)
                }
            }
        }

        applyDeviceRotation(initialTransientSettings.deviceRotation, useCaseGroup)
        processTransientSettingEvents(
            camera,
            useCaseGroup,
            initialTransientSettings,
            transientSettings
        )
    }
}

context(CameraSessionContext)
internal suspend fun processTransientSettingEvents(
    camera: Camera,
    useCaseGroup: UseCaseGroup,
    initialTransientSettings: TransientSessionSettings,
    transientSettings: StateFlow<TransientSessionSettings?>
) {
    var prevTransientSettings = initialTransientSettings
    transientSettings.filterNotNull().collectLatest { newTransientSettings ->
        // Apply camera control settings
        if (prevTransientSettings.zoomScale != newTransientSettings.zoomScale) {
            camera.cameraInfo.zoomState.value?.let { zoomState ->
                val finalScale =
                    (zoomState.zoomRatio * newTransientSettings.zoomScale).coerceIn(
                        zoomState.minZoomRatio,
                        zoomState.maxZoomRatio
                    )
                camera.cameraControl.setZoomRatio(finalScale)
                currentCameraState.update { old ->
                    old.copy(zoomScale = finalScale)
                }
            }
        }

        useCaseGroup.getImageCapture()?.let { imageCapture ->
            if (prevTransientSettings.flashMode != newTransientSettings.flashMode) {
                setFlashModeInternal(
                    imageCapture = imageCapture,
                    flashMode = newTransientSettings.flashMode,
                    isFrontFacing = camera.cameraInfo.appLensFacing == LensFacing.FRONT
                )
            }
        }

        if (prevTransientSettings.deviceRotation
            != newTransientSettings.deviceRotation
        ) {
            Log.d(
                TAG,
                "Updating device rotation from " +
                    "${prevTransientSettings.deviceRotation} -> " +
                    "${newTransientSettings.deviceRotation}"
            )
            applyDeviceRotation(newTransientSettings.deviceRotation, useCaseGroup)
        }

        prevTransientSettings = newTransientSettings
    }
}

internal fun applyDeviceRotation(deviceRotation: DeviceRotation, useCaseGroup: UseCaseGroup) {
    val targetRotation = deviceRotation.toUiSurfaceRotation()
    useCaseGroup.useCases.forEach {
        when (it) {
            is Preview -> {
                // Preview's target rotation should not be updated with device rotation.
                // Instead, preview rotation should match the display rotation.
                // When Preview is created, it is initialized with the display rotation.
                // This will need to be updated separately if the display rotation is not
                // locked. Currently the app is locked to portrait orientation.
            }

            is ImageCapture -> {
                it.targetRotation = targetRotation
            }

            is VideoCapture<*> -> {
                it.targetRotation = targetRotation
            }
        }
    }
}

context(CameraSessionContext)
internal fun createUseCaseGroup(
    cameraInfo: CameraInfo,
    initialTransientSettings: TransientSessionSettings,
    stabilizePreviewMode: Stabilization,
    stabilizeVideoMode: Stabilization,
    aspectRatio: AspectRatio,
    targetFrameRate: Int,
    dynamicRange: DynamicRange,
    imageFormat: ImageOutputFormat,
    useCaseMode: CameraUseCase.UseCaseMode,
    effect: CameraEffect? = null
): UseCaseGroup {
    val previewUseCase =
        createPreviewUseCase(
            cameraInfo,
            aspectRatio,
            stabilizePreviewMode
        )
    val imageCaptureUseCase = if (useCaseMode != CameraUseCase.UseCaseMode.VIDEO_ONLY) {
        createImageUseCase(cameraInfo, aspectRatio, dynamicRange, imageFormat)
    } else {
        null
    }
    val videoCaptureUseCase = if (useCaseMode != CameraUseCase.UseCaseMode.IMAGE_ONLY) {
        createVideoUseCase(
            cameraInfo,
            aspectRatio,
            targetFrameRate,
            stabilizeVideoMode,
            dynamicRange,
            backgroundDispatcher
        )
    } else {
        null
    }

    imageCaptureUseCase?.let {
        setFlashModeInternal(
            imageCapture = imageCaptureUseCase,
            flashMode = initialTransientSettings.flashMode,
            isFrontFacing = cameraInfo.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        )
    }

    return UseCaseGroup.Builder().apply {
        Log.d(
            TAG,
            "Setting initial device rotation to ${initialTransientSettings.deviceRotation}"
        )
        setViewPort(
            ViewPort.Builder(
                aspectRatio.ratio,
                // Initialize rotation to Preview's rotation, which comes from Display rotation
                previewUseCase.targetRotation
            ).build()
        )
        addUseCase(previewUseCase)
        imageCaptureUseCase?.let {
            if (dynamicRange == DynamicRange.SDR ||
                imageFormat == ImageOutputFormat.JPEG_ULTRA_HDR
            ) {
                addUseCase(imageCaptureUseCase)
            }
        }

        // Not to bind VideoCapture when Ultra HDR is enabled to keep the app design simple.
        videoCaptureUseCase?.let {
            if (imageFormat == ImageOutputFormat.JPEG) {
                addUseCase(videoCaptureUseCase)
            }
        }

        effect?.let { addEffect(it) }
    }.build()
}

@OptIn(ExperimentalImageCaptureOutputFormat::class)
private fun createImageUseCase(
    cameraInfo: CameraInfo,
    aspectRatio: AspectRatio,
    dynamicRange: DynamicRange,
    imageFormat: ImageOutputFormat
): ImageCapture {
    val builder = ImageCapture.Builder()
    builder.setResolutionSelector(
        getResolutionSelector(cameraInfo.sensorLandscapeRatio, aspectRatio)
    )
    if (dynamicRange != DynamicRange.SDR && imageFormat == ImageOutputFormat.JPEG_ULTRA_HDR
    ) {
        builder.setOutputFormat(ImageCapture.OUTPUT_FORMAT_JPEG_ULTRA_HDR)
    }
    return builder.build()
}

private fun createVideoUseCase(
    cameraInfo: CameraInfo,
    aspectRatio: AspectRatio,
    targetFrameRate: Int,
    stabilizeVideoMode: Stabilization,
    dynamicRange: DynamicRange,
    backgroundDispatcher: CoroutineDispatcher
): VideoCapture<Recorder> {
    val sensorLandscapeRatio = cameraInfo.sensorLandscapeRatio
    val recorder = Recorder.Builder()
        .setAspectRatio(
            getAspectRatioForUseCase(sensorLandscapeRatio, aspectRatio)
        )
        .setExecutor(backgroundDispatcher.asExecutor()).build()
    return VideoCapture.Builder(recorder).apply {
        // set video stabilization
        if (stabilizeVideoMode == Stabilization.ON) {
            setVideoStabilizationEnabled(true)
        }
        // set target fps
        if (targetFrameRate != TARGET_FPS_AUTO) {
            setTargetFrameRate(Range(targetFrameRate, targetFrameRate))
        }

        setDynamicRange(dynamicRange.toCXDynamicRange())
    }.build()
}

private fun getAspectRatioForUseCase(sensorLandscapeRatio: Float, aspectRatio: AspectRatio): Int {
    return when (aspectRatio) {
        AspectRatio.THREE_FOUR -> androidx.camera.core.AspectRatio.RATIO_4_3
        AspectRatio.NINE_SIXTEEN -> androidx.camera.core.AspectRatio.RATIO_16_9
        else -> {
            // Choose the aspect ratio which maximizes FOV by being closest to the sensor ratio
            if (
                abs(sensorLandscapeRatio - AspectRatio.NINE_SIXTEEN.landscapeRatio.toFloat()) <
                abs(sensorLandscapeRatio - AspectRatio.THREE_FOUR.landscapeRatio.toFloat())
            ) {
                androidx.camera.core.AspectRatio.RATIO_16_9
            } else {
                androidx.camera.core.AspectRatio.RATIO_4_3
            }
        }
    }
}

context(CameraSessionContext)
private fun createPreviewUseCase(
    cameraInfo: CameraInfo,
    aspectRatio: AspectRatio,
    stabilizePreviewMode: Stabilization
): Preview = Preview.Builder().apply {
    updateCameraStateWithCaptureResults()

    // set preview stabilization
    if (stabilizePreviewMode == Stabilization.ON) {
        setPreviewStabilizationEnabled(true)
    }

    setResolutionSelector(
        getResolutionSelector(cameraInfo.sensorLandscapeRatio, aspectRatio)
    )
}.build()
    .apply {
        setSurfaceProvider { surfaceRequest ->
            surfaceRequests.update { surfaceRequest }
        }
    }

private fun getResolutionSelector(
    sensorLandscapeRatio: Float,
    aspectRatio: AspectRatio
): ResolutionSelector {
    val aspectRatioStrategy = when (aspectRatio) {
        AspectRatio.THREE_FOUR -> AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
        AspectRatio.NINE_SIXTEEN -> AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
        else -> {
            // Choose the resolution selector strategy which maximizes FOV by being closest
            // to the sensor aspect ratio
            if (
                abs(sensorLandscapeRatio - AspectRatio.NINE_SIXTEEN.landscapeRatio.toFloat()) <
                abs(sensorLandscapeRatio - AspectRatio.THREE_FOUR.landscapeRatio.toFloat())
            ) {
                AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY
            } else {
                AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY
            }
        }
    }
    return ResolutionSelector.Builder().setAspectRatioStrategy(aspectRatioStrategy).build()
}

context(CameraSessionContext)
private fun setFlashModeInternal(
    imageCapture: ImageCapture,
    flashMode: FlashMode,
    isFrontFacing: Boolean
) {
    val isScreenFlashRequired =
        isFrontFacing && (flashMode == FlashMode.ON || flashMode == FlashMode.AUTO)

    if (isScreenFlashRequired) {
        imageCapture.screenFlash = object : ImageCapture.ScreenFlash {
            override fun apply(
                expirationTimeMillis: Long,
                listener: ImageCapture.ScreenFlashListener
            ) {
                Log.d(TAG, "ImageCapture.ScreenFlash: apply")
                screenFlashEvents.tryEmit(
                    CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.APPLY_UI) {
                        listener.onCompleted()
                    }
                )
            }

            override fun clear() {
                Log.d(TAG, "ImageCapture.ScreenFlash: clear")
                screenFlashEvents.tryEmit(
                    CameraUseCase.ScreenFlashEvent(CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI) {}
                )
            }
        }
    }

    imageCapture.flashMode = when (flashMode) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF // 2

        FlashMode.ON -> if (isScreenFlashRequired) {
            ImageCapture.FLASH_MODE_SCREEN // 3
        } else {
            ImageCapture.FLASH_MODE_ON // 1
        }

        FlashMode.AUTO -> if (isScreenFlashRequired) {
            ImageCapture.FLASH_MODE_SCREEN // 3
        } else {
            ImageCapture.FLASH_MODE_AUTO // 0
        }
    }
    Log.d(TAG, "Set flash mode to: ${imageCapture.flashMode}")
}

private suspend fun startVideoRecordingInternal(
    initialMuted: Boolean,
    videoCaptureUseCase: VideoCapture<Recorder>,
    captureTypeSuffix: String,
    context: Context,
    videoCaptureUri: Uri?,
    shouldUseUri: Boolean,
    onVideoRecord: (CameraUseCase.OnVideoRecordEvent) -> Unit
): Recording {
    Log.d(TAG, "recordVideo")
    // todo(b/336886716): default setting to enable or disable audio when permission is granted

    // ok. there is a difference between MUTING and ENABLING audio
    // audio must be enabled in order to be muted
    // if the video recording isnt started with audio enabled, you will not be able to unmute it
    // the toggle should only affect whether or not the audio is muted.
    // the permission will determine whether or not the audio is enabled.
    val audioEnabled = checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val pendingRecord = if (shouldUseUri) {
        val fileOutputOptions = FileOutputOptions.Builder(
            File(videoCaptureUri!!.getPath())
        ).build()
        videoCaptureUseCase.output.prepareRecording(context, fileOutputOptions)
    } else {
        val name = "JCA-recording-${Date()}-$captureTypeSuffix.mp4"
        val contentValues =
            ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
            }
        val mediaStoreOutput =
            MediaStoreOutputOptions.Builder(
                context.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()
        videoCaptureUseCase.output.prepareRecording(context, mediaStoreOutput)
    }
    pendingRecord.apply {
        if (audioEnabled) {
            withAudioEnabled()
        }
    }
    val callbackExecutor: Executor =
        (
            currentCoroutineContext()[ContinuationInterceptor] as?
                CoroutineDispatcher
            )?.asExecutor() ?: ContextCompat.getMainExecutor(context)
    return pendingRecord.start(callbackExecutor) { onVideoRecordEvent ->
        Log.d(TAG, onVideoRecordEvent.toString())
        when (onVideoRecordEvent) {
            is VideoRecordEvent.Finalize -> {
                when (onVideoRecordEvent.error) {
                    ERROR_NONE ->
                        onVideoRecord(
                            CameraUseCase.OnVideoRecordEvent.OnVideoRecorded(
                                onVideoRecordEvent.outputResults.outputUri
                            )
                        )

                    else ->
                        onVideoRecord(
                            CameraUseCase.OnVideoRecordEvent.OnVideoRecordError(
                                onVideoRecordEvent.cause
                            )
                        )
                }
            }

            is VideoRecordEvent.Status -> {
                onVideoRecord(
                    CameraUseCase.OnVideoRecordEvent.OnVideoRecordStatus(
                        onVideoRecordEvent.recordingStats.audioStats
                            .audioAmplitude
                    )
                )
            }
        }
    }.apply {
        mute(initialMuted)
    }
}

private suspend fun runVideoRecording(
    camera: Camera,
    videoCapture: VideoCapture<Recorder>,
    captureTypeSuffix: String,
    context: Context,
    transientSettings: StateFlow<TransientSessionSettings?>,
    videoCaptureUri: Uri?,
    shouldUseUri: Boolean,
    onVideoRecord: (CameraUseCase.OnVideoRecordEvent) -> Unit
) {
    var currentSettings = transientSettings.filterNotNull().first()

    startVideoRecordingInternal(
        initialMuted = currentSettings.audioMuted,
        videoCapture,
        captureTypeSuffix,
        context,
        videoCaptureUri,
        shouldUseUri,
        onVideoRecord
    ).use { recording ->

        fun TransientSessionSettings.isFlashModeOn() = flashMode == FlashMode.ON
        val isFrontCameraSelector =
            camera.cameraInfo.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA

        if (currentSettings.isFlashModeOn()) {
            if (!isFrontCameraSelector) {
                camera.cameraControl.enableTorch(true).await()
            } else {
                Log.d(TAG, "Unable to enable torch for front camera.")
            }
        }

        transientSettings.filterNotNull()
            .onCompletion {
                // Could do some fancier tracking of whether the torch was enabled before
                // calling this.
                camera.cameraControl.enableTorch(false)
            }
            .collectLatest { newTransientSettings ->
                if (currentSettings.audioMuted != newTransientSettings.audioMuted) {
                    recording.mute(newTransientSettings.audioMuted)
                }
                if (currentSettings.isFlashModeOn() != newTransientSettings.isFlashModeOn()) {
                    if (!isFrontCameraSelector) {
                        camera.cameraControl.enableTorch(newTransientSettings.isFlashModeOn())
                    } else {
                        Log.d(TAG, "Unable to update torch for front camera.")
                    }
                }
                currentSettings = newTransientSettings
            }
    }
}

context(CameraSessionContext)
internal suspend fun processFocusMeteringEvents(cameraControl: CameraControl) {
    surfaceRequests.map { surfaceRequest ->
        surfaceRequest?.resolution?.run {
            Log.d(
                TAG,
                "Waiting to process focus points for surface with resolution: " +
                    "$width x $height"
            )
            SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
        }
    }.collectLatest { meteringPointFactory ->
        for (event in focusMeteringEvents) {
            meteringPointFactory?.apply {
                Log.d(TAG, "tapToFocus, processing event: $event")
                val meteringPoint = createPoint(event.x, event.y)
                val action = FocusMeteringAction.Builder(meteringPoint).build()
                cameraControl.startFocusAndMetering(action)
            } ?: run {
                Log.w(TAG, "Ignoring event due to no SurfaceRequest: $event")
            }
        }
    }
}

context(CameraSessionContext)
internal suspend fun processVideoControlEvents(
    camera: Camera,
    videoCapture: VideoCapture<Recorder>?,
    captureTypeSuffix: String
) = coroutineScope {
    var recordingJob: Job? = null

    for (event in videoCaptureControlEvents) {
        when (event) {
            is VideoCaptureControlEvent.StartRecordingEvent -> {
                if (videoCapture == null) {
                    throw RuntimeException(
                        "Attempted video recording with null videoCapture"
                    )
                }

                recordingJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    runVideoRecording(
                        camera,
                        videoCapture,
                        captureTypeSuffix,
                        context,
                        transientSettings,
                        event.videoCaptureUri,
                        event.shouldUseUri,
                        event.onVideoRecord
                    )
                }
            }

            VideoCaptureControlEvent.StopRecordingEvent -> {
                recordingJob?.cancel()
                recordingJob = null
            }
        }
    }
}

/**
 * Applies a CaptureCallback to the provided image capture builder
 */
context(CameraSessionContext)
@OptIn(ExperimentalCamera2Interop::class)
private fun Preview.Builder.updateCameraStateWithCaptureResults(): Preview.Builder {
    val isFirstFrameTimestampUpdated = atomic(false)
    Camera2Interop.Extender(this).setSessionCaptureCallback(
        object : CameraCaptureSession.CaptureCallback() {
            override fun onCaptureCompleted(
                session: CameraCaptureSession,
                request: CaptureRequest,
                result: TotalCaptureResult
            ) {
                super.onCaptureCompleted(session, request, result)
                try {
                    if (!isFirstFrameTimestampUpdated.value) {
                        currentCameraState.update { old ->
                            old.copy(
                                sessionFirstFrameTimestamp = SystemClock.elapsedRealtimeNanos()
                            )
                        }
                        isFirstFrameTimestampUpdated.value = true
                    }
                } catch (_: Exception) {
                }
            }
        }
    )
    return this
}
