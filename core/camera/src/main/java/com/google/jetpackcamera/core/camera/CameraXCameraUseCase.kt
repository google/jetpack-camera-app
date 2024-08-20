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
package com.google.jetpackcamera.core.camera

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.DynamicRange as CXDynamicRange
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.takePicture
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.Recorder
import com.google.jetpackcamera.settings.SettableConstraintsRepository
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DeviceRotation
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.LowLightBoost
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SupportedStabilizationMode
import com.google.jetpackcamera.settings.model.SystemConstraints
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject
import kotlin.properties.Delegates
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

private const val TAG = "CameraXCameraUseCase"
const val TARGET_FPS_AUTO = 0
const val TARGET_FPS_15 = 15
const val TARGET_FPS_30 = 30
const val TARGET_FPS_60 = 60

/**
 * CameraX based implementation for [CameraUseCase]
 */
@ViewModelScoped
class CameraXCameraUseCase
@Inject
constructor(
    private val application: Application,
    private val defaultDispatcher: CoroutineDispatcher,
    private val constraintsRepository: SettableConstraintsRepository
) : CameraUseCase {
    private lateinit var cameraProvider: ProcessCameraProvider

    private var imageCaptureUseCase: ImageCapture? = null

    private lateinit var systemConstraints: SystemConstraints
    private var useCaseMode by Delegates.notNull<CameraUseCase.UseCaseMode>()

    private val screenFlashEvents: MutableSharedFlow<CameraUseCase.ScreenFlashEvent> =
        MutableSharedFlow()
    private val focusMeteringEvents =
        Channel<CameraEvent.FocusMeteringEvent>(capacity = Channel.CONFLATED)
    private val videoCaptureControlEvents = Channel<VideoCaptureControlEvent>()

    private val currentSettings = MutableStateFlow<CameraAppSettings?>(null)

    // Could be improved by setting initial value only when camera is initialized
    private val _currentCameraState = MutableStateFlow(CameraState())
    override fun getCurrentCameraState(): StateFlow<CameraState> = _currentCameraState.asStateFlow()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    override fun getSurfaceRequest(): StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    override suspend fun initialize(
        cameraAppSettings: CameraAppSettings,
        useCaseMode: CameraUseCase.UseCaseMode
    ) {
        this.useCaseMode = useCaseMode
        cameraProvider = ProcessCameraProvider.awaitInstance(application)

        // updates values for available cameras
        val availableCameraLenses =
            listOf(
                LensFacing.FRONT,
                LensFacing.BACK
            ).filter {
                cameraProvider.hasCamera(it.toCameraSelector())
            }

        // Build and update the system constraints
        systemConstraints = SystemConstraints(
            availableLenses = availableCameraLenses,
            concurrentCamerasSupported = cameraProvider.availableConcurrentCameraInfos.any {
                it.map { cameraInfo -> cameraInfo.cameraSelector.toAppLensFacing() }
                    .toSet() == setOf(LensFacing.FRONT, LensFacing.BACK)
            },
            perLensConstraints = buildMap {
                val availableCameraInfos = cameraProvider.availableCameraInfos
                for (lensFacing in availableCameraLenses) {
                    val selector = lensFacing.toCameraSelector()
                    selector.filter(availableCameraInfos).firstOrNull()?.let { camInfo ->
                        val supportedDynamicRanges =
                            Recorder.getVideoCapabilities(camInfo).supportedDynamicRanges
                                .mapNotNull(CXDynamicRange::toSupportedAppDynamicRange)
                                .toSet()

                        val supportedStabilizationModes = buildSet {
                            if (camInfo.isPreviewStabilizationSupported) {
                                add(SupportedStabilizationMode.ON)
                            }

                            if (camInfo.isVideoStabilizationSupported) {
                                add(SupportedStabilizationMode.HIGH_QUALITY)
                            }
                        }

                        val supportedFixedFrameRates =
                            camInfo.filterSupportedFixedFrameRates(FIXED_FRAME_RATES)
                        val supportedImageFormats = camInfo.supportedImageFormats
                        val hasFlashUnit = camInfo.hasFlashUnit()

                        put(
                            lensFacing,
                            CameraConstraints(
                                supportedStabilizationModes = supportedStabilizationModes,
                                supportedFixedFrameRates = supportedFixedFrameRates,
                                supportedDynamicRanges = supportedDynamicRanges,
                                supportedImageFormatsMap = mapOf(
                                    // Only JPEG is supported in single-stream mode, since
                                    // single-stream mode uses CameraEffect, which does not support
                                    // Ultra HDR now.
                                    Pair(CaptureMode.SINGLE_STREAM, setOf(ImageOutputFormat.JPEG)),
                                    Pair(CaptureMode.MULTI_STREAM, supportedImageFormats)
                                ),
                                hasFlashUnit = hasFlashUnit
                            )
                        )
                    }
                }
            }
        )

        constraintsRepository.updateSystemConstraints(systemConstraints)

        currentSettings.value =
            cameraAppSettings
                .tryApplyDynamicRangeConstraints()
                .tryApplyAspectRatioForExternalCapture(this.useCaseMode)
                .tryApplyImageFormatConstraints()
                .tryApplyFrameRateConstraints()
                .tryApplyStabilizationConstraints()
                .tryApplyConcurrentCameraModeConstraints()
    }

    override suspend fun runCamera() = coroutineScope {
        Log.d(TAG, "runCamera")

        val transientSettings = MutableStateFlow<TransientSessionSettings?>(null)
        currentSettings
            .filterNotNull()
            .map { currentCameraSettings ->
                transientSettings.value = TransientSessionSettings(
                    audioMuted = currentCameraSettings.audioMuted,
                    deviceRotation = currentCameraSettings.deviceRotation,
                    flashMode = currentCameraSettings.flashMode,
                    zoomScale = currentCameraSettings.zoomScale
                )

                when (currentCameraSettings.concurrentCameraMode) {
                    ConcurrentCameraMode.OFF -> {
                        val cameraSelector = when (currentCameraSettings.cameraLensFacing) {
                            LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                            LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                        }

                        PerpetualSessionSettings.SingleCamera(
                            cameraInfo = cameraProvider.getCameraInfo(cameraSelector),
                            aspectRatio = currentCameraSettings.aspectRatio,
                            captureMode = currentCameraSettings.captureMode,
                            targetFrameRate = currentCameraSettings.targetFrameRate,
                            stabilizePreviewMode = currentCameraSettings.previewStabilization,
                            stabilizeVideoMode = currentCameraSettings.videoCaptureStabilization,
                            dynamicRange = currentCameraSettings.dynamicRange,
                            imageFormat = currentCameraSettings.imageFormat
                        )
                    }
                    ConcurrentCameraMode.DUAL -> {
                        val primaryFacing = currentCameraSettings.cameraLensFacing
                        val secondaryFacing = primaryFacing.flip()
                        cameraProvider.availableConcurrentCameraInfos.firstNotNullOf {
                            var primaryCameraInfo: CameraInfo? = null
                            var secondaryCameraInfo: CameraInfo? = null
                            it.forEach { cameraInfo ->
                                if (cameraInfo.appLensFacing == primaryFacing) {
                                    primaryCameraInfo = cameraInfo
                                } else if (cameraInfo.appLensFacing == secondaryFacing) {
                                    secondaryCameraInfo = cameraInfo
                                }
                            }

                            primaryCameraInfo?.let { nonNullPrimary ->
                                secondaryCameraInfo?.let { nonNullSecondary ->
                                    PerpetualSessionSettings.ConcurrentCamera(
                                        primaryCameraInfo = nonNullPrimary,
                                        secondaryCameraInfo = nonNullSecondary,
                                        aspectRatio = currentCameraSettings.aspectRatio
                                    )
                                }
                            }
                        }
                    }
                }
            }.distinctUntilChanged()
            .collectLatest { sessionSettings ->
                coroutineScope {
                    with(
                        CameraSessionContext(
                            context = application,
                            cameraProvider = cameraProvider,
                            backgroundDispatcher = defaultDispatcher,
                            screenFlashEvents = screenFlashEvents,
                            focusMeteringEvents = focusMeteringEvents,
                            videoCaptureControlEvents = videoCaptureControlEvents,
                            currentCameraState = _currentCameraState,
                            surfaceRequests = _surfaceRequest,
                            transientSettings = transientSettings
                        )
                    ) {
                        try {
                            when (sessionSettings) {
                                is PerpetualSessionSettings.SingleCamera -> runSingleCameraSession(
                                    sessionSettings,
                                    useCaseMode = useCaseMode
                                ) { imageCapture ->
                                    imageCaptureUseCase = imageCapture
                                }

                                is PerpetualSessionSettings.ConcurrentCamera ->
                                    runConcurrentCameraSession(
                                        sessionSettings,
                                        useCaseMode = CameraUseCase.UseCaseMode.VIDEO_ONLY
                                    )
                            }
                        } finally {
                            // TODO(tm): This shouldn't be necessary. Cancellation of the
                            //  coroutineScope by collectLatest should cause this to
                            //  occur naturally.
                            cameraProvider.unbindAll()
                        }
                    }
                }
            }
    }

    override suspend fun takePicture(onCaptureStarted: (() -> Unit)) {
        if (imageCaptureUseCase == null) {
            throw RuntimeException("Attempted take picture with null imageCapture use case")
        }
        try {
            val imageProxy = imageCaptureUseCase!!.takePicture(onCaptureStarted)
            Log.d(TAG, "onCaptureSuccess")
            imageProxy.close()
        } catch (exception: Exception) {
            Log.d(TAG, "takePicture onError: $exception")
            throw exception
        }
    }

    // TODO(b/319733374): Return bitmap for external mediastore capture without URI
    override suspend fun takePicture(
        onCaptureStarted: (() -> Unit),
        contentResolver: ContentResolver,
        imageCaptureUri: Uri?,
        ignoreUri: Boolean
    ): ImageCapture.OutputFileResults {
        if (imageCaptureUseCase == null) {
            throw RuntimeException("Attempted take picture with null imageCapture use case")
        }
        val eligibleContentValues = getEligibleContentValues()
        val outputFileOptions: OutputFileOptions
        if (ignoreUri) {
            val formatter = SimpleDateFormat(
                "yyyy-MM-dd-HH-mm-ss-SSS",
                Locale.US
            )
            val filename = "JCA-${formatter.format(Calendar.getInstance().time)}.jpg"
            val contentValues = ContentValues()
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            outputFileOptions = OutputFileOptions.Builder(
                contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            ).build()
        } else if (imageCaptureUri == null) {
            val e = RuntimeException("Null Uri is provided.")
            Log.d(TAG, "takePicture onError: $e")
            throw e
        } else {
            try {
                val outputStream = contentResolver.openOutputStream(imageCaptureUri)
                if (outputStream != null) {
                    outputFileOptions =
                        OutputFileOptions.Builder(
                            contentResolver.openOutputStream(imageCaptureUri)!!
                        ).build()
                } else {
                    val e = RuntimeException("Provider recently crashed.")
                    Log.d(TAG, "takePicture onError: $e")
                    throw e
                }
            } catch (e: FileNotFoundException) {
                Log.d(TAG, "takePicture onError: $e")
                throw e
            }
        }
        try {
            val outputFileResults = imageCaptureUseCase!!.takePicture(
                outputFileOptions,
                onCaptureStarted
            )
            val relativePath =
                eligibleContentValues.getAsString(MediaStore.Images.Media.RELATIVE_PATH)
            val displayName = eligibleContentValues.getAsString(
                MediaStore.Images.Media.DISPLAY_NAME
            )
            Log.d(TAG, "Saved image to $relativePath/$displayName")
            return outputFileResults
        } catch (exception: ImageCaptureException) {
            Log.d(TAG, "takePicture onError: $exception")
            throw exception
        }
    }

    private fun getEligibleContentValues(): ContentValues {
        val eligibleContentValues = ContentValues()
        eligibleContentValues.put(
            MediaStore.Images.Media.DISPLAY_NAME,
            Calendar.getInstance().time.toString()
        )
        eligibleContentValues.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        eligibleContentValues.put(
            MediaStore.Images.Media.RELATIVE_PATH,
            Environment.DIRECTORY_PICTURES
        )
        return eligibleContentValues
    }

    override suspend fun startVideoRecording(
        videoCaptureUri: Uri?,
        shouldUseUri: Boolean,
        onVideoRecord: (CameraUseCase.OnVideoRecordEvent) -> Unit
    ) {
        if (shouldUseUri && videoCaptureUri == null) {
            val e = RuntimeException("Null Uri is provided.")
            Log.d(TAG, "takePicture onError: $e")
            throw e
        }
        videoCaptureControlEvents.send(
            VideoCaptureControlEvent.StartRecordingEvent(
                videoCaptureUri,
                shouldUseUri,
                onVideoRecord
            )
        )
    }

    override fun stopVideoRecording() {
        videoCaptureControlEvents.trySendBlocking(VideoCaptureControlEvent.StopRecordingEvent)
    }

    override fun setZoomScale(scale: Float) {
        currentSettings.update { old ->
            old?.copy(zoomScale = scale)
        }
    }

    // Sets the camera to the designated lensFacing direction
    override suspend fun setLensFacing(lensFacing: LensFacing) {
        currentSettings.update { old ->
            if (systemConstraints.availableLenses.contains(lensFacing)) {
                old?.copy(cameraLensFacing = lensFacing)
                    ?.tryApplyDynamicRangeConstraints()
                    ?.tryApplyImageFormatConstraints()
            } else {
                old
            }
        }
    }

    private fun CameraAppSettings.tryApplyDynamicRangeConstraints(): CameraAppSettings {
        return systemConstraints.perLensConstraints[cameraLensFacing]?.let { constraints ->
            with(constraints.supportedDynamicRanges) {
                val newDynamicRange = if (contains(dynamicRange)) {
                    dynamicRange
                } else {
                    DynamicRange.SDR
                }

                this@tryApplyDynamicRangeConstraints.copy(
                    dynamicRange = newDynamicRange
                )
            }
        } ?: this
    }

    private fun CameraAppSettings.tryApplyAspectRatioForExternalCapture(
        useCaseMode: CameraUseCase.UseCaseMode
    ): CameraAppSettings {
        return when (useCaseMode) {
            CameraUseCase.UseCaseMode.STANDARD -> this
            CameraUseCase.UseCaseMode.IMAGE_ONLY ->
                this.copy(aspectRatio = AspectRatio.THREE_FOUR)

            CameraUseCase.UseCaseMode.VIDEO_ONLY ->
                this.copy(aspectRatio = AspectRatio.NINE_SIXTEEN)
        }
    }

    private fun CameraAppSettings.tryApplyImageFormatConstraints(): CameraAppSettings {
        return systemConstraints.perLensConstraints[cameraLensFacing]?.let { constraints ->
            with(constraints.supportedImageFormatsMap[captureMode]) {
                val newImageFormat = if (this != null && contains(imageFormat)) {
                    imageFormat
                } else {
                    ImageOutputFormat.JPEG
                }

                this@tryApplyImageFormatConstraints.copy(
                    imageFormat = newImageFormat
                )
            }
        } ?: this
    }

    private fun CameraAppSettings.tryApplyFrameRateConstraints(): CameraAppSettings {
        return systemConstraints.perLensConstraints[cameraLensFacing]?.let { constraints ->
            with(constraints.supportedFixedFrameRates) {
                val newTargetFrameRate = if (contains(targetFrameRate)) {
                    targetFrameRate
                } else {
                    TARGET_FPS_AUTO
                }

                this@tryApplyFrameRateConstraints.copy(
                    targetFrameRate = newTargetFrameRate
                )
            }
        } ?: this
    }

    private fun CameraAppSettings.tryApplyStabilizationConstraints(): CameraAppSettings {
        return systemConstraints.perLensConstraints[cameraLensFacing]?.let { constraints ->
            with(constraints.supportedStabilizationModes) {
                val newVideoStabilization = if (contains(SupportedStabilizationMode.HIGH_QUALITY) &&
                    (targetFrameRate != TARGET_FPS_60)
                ) {
                    // unlike shouldVideoBeStabilized, doesn't check value of previewStabilization
                    videoCaptureStabilization
                } else {
                    Stabilization.UNDEFINED
                }
                val newPreviewStabilization = if (contains(SupportedStabilizationMode.ON) &&
                    (targetFrameRate in setOf(TARGET_FPS_AUTO, TARGET_FPS_30))
                ) {
                    previewStabilization
                } else {
                    Stabilization.UNDEFINED
                }

                this@tryApplyStabilizationConstraints.copy(
                    previewStabilization = newPreviewStabilization,
                    videoCaptureStabilization = newVideoStabilization
                )
            }
        } ?: this
    }

    private fun CameraAppSettings.tryApplyConcurrentCameraModeConstraints(): CameraAppSettings =
        when (concurrentCameraMode) {
            ConcurrentCameraMode.OFF -> this
            else ->
                if (systemConstraints.concurrentCamerasSupported) {
                    copy(
                        targetFrameRate = TARGET_FPS_AUTO,
                        previewStabilization = Stabilization.OFF,
                        videoCaptureStabilization = Stabilization.OFF,
                        dynamicRange = DynamicRange.SDR,
                        captureMode = CaptureMode.MULTI_STREAM
                    )
                } else {
                    copy(concurrentCameraMode = ConcurrentCameraMode.OFF)
                }
        }

    override suspend fun tapToFocus(x: Float, y: Float) {
        focusMeteringEvents.send(CameraEvent.FocusMeteringEvent(x, y))
    }

    override fun getScreenFlashEvents() = screenFlashEvents.asSharedFlow()
    override fun getCurrentSettings() = currentSettings.asStateFlow()

    override fun setFlashMode(flashMode: FlashMode) {
        currentSettings.update { old ->
            old?.copy(flashMode = flashMode)
        }
    }

    override fun isScreenFlashEnabled() =
        imageCaptureUseCase?.flashMode == ImageCapture.FLASH_MODE_SCREEN &&
            imageCaptureUseCase?.screenFlash != null

    override suspend fun setAspectRatio(aspectRatio: AspectRatio) {
        currentSettings.update { old ->
            old?.copy(aspectRatio = aspectRatio)
        }
    }

    override suspend fun setCaptureMode(captureMode: CaptureMode) {
        currentSettings.update { old ->
            old?.copy(captureMode = captureMode)
                ?.tryApplyImageFormatConstraints()
                ?.tryApplyConcurrentCameraModeConstraints()
        }
    }

    override suspend fun setDynamicRange(dynamicRange: DynamicRange) {
        currentSettings.update { old ->
            old?.copy(dynamicRange = dynamicRange)
                ?.tryApplyConcurrentCameraModeConstraints()
        }
    }

    override fun setDeviceRotation(deviceRotation: DeviceRotation) {
        currentSettings.update { old ->
            old?.copy(deviceRotation = deviceRotation)
        }
    }

    override suspend fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {
        currentSettings.update { old ->
            old?.copy(concurrentCameraMode = concurrentCameraMode)
                ?.tryApplyConcurrentCameraModeConstraints()
        }
    }

    override suspend fun setImageFormat(imageFormat: ImageOutputFormat) {
        currentSettings.update { old ->
            old?.copy(imageFormat = imageFormat)
        }
    }

    override suspend fun setPreviewStabilization(previewStabilization: Stabilization) {
        currentSettings.update { old ->
            old?.copy(
                previewStabilization = previewStabilization
            )?.tryApplyStabilizationConstraints()
                ?.tryApplyConcurrentCameraModeConstraints()
        }
    }

    override suspend fun setVideoCaptureStabilization(videoCaptureStabilization: Stabilization) {
        currentSettings.update { old ->
            old?.copy(
                videoCaptureStabilization = videoCaptureStabilization
            )?.tryApplyStabilizationConstraints()
                ?.tryApplyConcurrentCameraModeConstraints()
        }
    }

    override suspend fun setTargetFrameRate(targetFrameRate: Int) {
        currentSettings.update { old ->
            old?.copy(targetFrameRate = targetFrameRate)?.tryApplyFrameRateConstraints()
                ?.tryApplyConcurrentCameraModeConstraints()
        }
    }

    override suspend fun setLowLightBoost(lowLightBoost: LowLightBoost) {
        currentSettings.update { old ->
            old?.copy(lowLightBoost = lowLightBoost)
        }
    }

    override suspend fun setAudioMuted(isAudioMuted: Boolean) {
        currentSettings.update { old ->
            old?.copy(audioMuted = isAudioMuted)
        }
    }

    companion object {
        private val FIXED_FRAME_RATES = setOf(TARGET_FPS_15, TARGET_FPS_30, TARGET_FPS_60)
    }
}
