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

import android.app.Application
import android.content.ContentResolver
import android.content.ContentValues
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.view.Display
import androidx.camera.core.CameraInfo
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCapture.OutputFileOptions
import androidx.camera.core.ImageCapture.ScreenFlash
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import com.google.jetpackcamera.domain.camera.CameraUseCase.ScreenFlashEvent.Type
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SupportedStabilizationMode
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.FileNotFoundException
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    private val coroutineScope: CoroutineScope,
    private val defaultDispatcher: CoroutineDispatcher,
    private val settingsRepository: SettingsRepository
) : CameraUseCase {
    private val fixedFrameRates = setOf(15, 30, 60)
    private lateinit var cameraProvider: ProcessCameraProvider

    private val imageCaptureUseCase = ImageCapture.Builder().build()

    private val recorder = Recorder.Builder().setExecutor(defaultDispatcher.asExecutor()).build()
    private lateinit var videoCaptureUseCase: VideoCapture<Recorder>
    private var recording: Recording? = null
    private lateinit var captureMode: CaptureMode

    private val screenFlashEvents: MutableSharedFlow<CameraUseCase.ScreenFlashEvent> =
        MutableSharedFlow()

    private val currentSettings = MutableStateFlow<CameraAppSettings?>(null)

    override suspend fun initialize() {
        cameraProvider = ProcessCameraProvider.getInstance(application).await()

        // updates values for available camera lens
        val availableCameraLens =
            listOf(
                LensFacing.FRONT,
                LensFacing.BACK
            ).filter { lensFacing ->
                cameraProvider.hasCamera(lensFacing.toCameraSelector())
            }

        settingsRepository.updateAvailableCameraLens(
            availableCameraLens.contains(LensFacing.FRONT),
            availableCameraLens.contains(LensFacing.BACK)
        )

        currentSettings.value = settingsRepository.cameraAppSettings.first()
    }

    private fun getSupportedFrameRates(): Set<Int> {
        val supportedFixedFrameRates = mutableSetOf<Int>()
        cameraProvider.availableCameraInfos.forEach { cameraInfo ->
            cameraInfo.supportedFrameRateRanges.forEach { e ->
                if (e.upper == e.lower && fixedFrameRates.contains(e.upper)) {
                    supportedFixedFrameRates.add(e.upper)
                }
            }
        }
        return supportedFixedFrameRates
    }

    /**
     * Camera settings that persist as long as a camera is running.
     *
     * Any change in these settings will require calling [ProcessCameraProvider.runWith] with
     * updates [CameraSelector] and/or [UseCaseGroup]
     */
    private data class PerpetualSessionSettings(
        val cameraSelector: CameraSelector,
        val aspectRatio: AspectRatio,
        val captureMode: CaptureMode,
        val targetFrameRate: Int,
        val stabilizePreviewMode: Stabilization,
        val stabilizeVideoMode: Stabilization
    )

    /**
     * Camera settings that can change while the camera is running.
     *
     * Any changes in these settings can be applied either directly to use cases via their
     * setter methods or to [androidx.camera.core.CameraControl].
     * The use cases typically will not need to be re-bound.
     */
    private data class TransientSessionSettings(
        val flashMode: FlashMode,
        val zoomScale: Float
    )

    override suspend fun runCamera() = coroutineScope {
        Log.d(TAG, "runCamera")

        val transientSettings = MutableStateFlow<TransientSessionSettings?>(null)
        currentSettings
            .filterNotNull()
            .map { currentCameraSettings ->
                transientSettings.value = TransientSessionSettings(
                    flashMode = currentCameraSettings.flashMode,
                    zoomScale = currentCameraSettings.zoomScale
                )

                val cameraSelector = when (currentCameraSettings.cameraLensFacing) {
                    LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
                    LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
                }

                PerpetualSessionSettings(
                    cameraSelector = cameraSelector,
                    aspectRatio = currentCameraSettings.aspectRatio,
                    captureMode = currentCameraSettings.captureMode,
                    targetFrameRate = currentCameraSettings.targetFrameRate,
                    stabilizePreviewMode = currentCameraSettings.previewStabilization,
                    stabilizeVideoMode = currentCameraSettings.videoCaptureStabilization
                )
            }.distinctUntilChanged()
            .collectLatest { sessionSettings ->
                Log.d(TAG, "Starting new camera session")
                val cameraInfo = sessionSettings.cameraSelector.filter(
                    cameraProvider.availableCameraInfos
                ).first()

                settingsRepository.updateSupportedFixedFrameRate(
                    getSupportedFrameRates(),
                    sessionSettings.targetFrameRate
                )
                settingsRepository.updatePreviewStabilizationSupported(
                    isPreviewStabilizationSupported(cameraInfo)
                )
                settingsRepository.updateVideoStabilizationSupported(
                    isVideoStabilizationSupported(cameraInfo)
                )

                val supportedStabilizationModes =
                    settingsRepository.cameraAppSettings.first().supportedStabilizationModes

                val initialTransientSettings = transientSettings
                    .filterNotNull()
                    .first()

                val useCaseGroup = createUseCaseGroup(
                    sessionSettings,
                    initialTransientSettings,
                    supportedStabilizationModes
                )

                var prevTransientSettings = initialTransientSettings
                cameraProvider.runWith(sessionSettings.cameraSelector, useCaseGroup) { camera ->
                    Log.d(TAG, "Camera session started")
                    transientSettings.filterNotNull().collectLatest { newTransientSettings ->
                        // Apply camera control settings
                        if (prevTransientSettings.zoomScale != newTransientSettings.zoomScale) {
                            cameraInfo.zoomState.value?.let { zoomState ->
                                val finalScale =
                                    (zoomState.zoomRatio * newTransientSettings.zoomScale).coerceIn(
                                        zoomState.minZoomRatio,
                                        zoomState.maxZoomRatio
                                    )
                                camera.cameraControl.setZoomRatio(finalScale)
                                _zoomScale.value = finalScale
                            }
                        }

                        if (prevTransientSettings.flashMode != newTransientSettings.flashMode) {
                            setFlashModeInternal(
                                flashMode = newTransientSettings.flashMode,
                                isFrontFacing = sessionSettings.cameraSelector
                                    == CameraSelector.DEFAULT_FRONT_CAMERA
                            )
                        }

                        prevTransientSettings = newTransientSettings
                    }
                }
            }
    }

    override suspend fun takePicture() {
        val imageDeferred = CompletableDeferred<ImageProxy>()

        imageCaptureUseCase.takePicture(
            defaultDispatcher.asExecutor(),
            object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(imageProxy: ImageProxy) {
                    Log.d(TAG, "onCaptureSuccess")
                    imageDeferred.complete(imageProxy)
                    imageProxy.close()
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.d(TAG, "takePicture onError: $exception")
                    imageDeferred.completeExceptionally(exception)
                }
            }
        )
        imageDeferred.await()
    }

    // TODO(b/319733374): Return bitmap for external mediastore capture without URI
    override suspend fun takePicture(contentResolver: ContentResolver, imageCaptureUri: Uri?) {
        val imageDeferred = CompletableDeferred<ImageCapture.OutputFileResults>()
        val eligibleContentValues = getEligibleContentValues()
        val outputFileOptions: OutputFileOptions
        if (imageCaptureUri == null) {
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
        imageCaptureUseCase.takePicture(
            outputFileOptions,
            defaultDispatcher.asExecutor(),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    val relativePath =
                        eligibleContentValues.getAsString(MediaStore.Images.Media.RELATIVE_PATH)
                    val displayName = eligibleContentValues.getAsString(
                        MediaStore.Images.Media.DISPLAY_NAME
                    )
                    Log.d(TAG, "Saved image to $relativePath/$displayName")
                    imageDeferred.complete(outputFileResults)
                }

                override fun onError(exception: ImageCaptureException) {
                    Log.d(TAG, "takePicture onError: $exception")
                    imageDeferred.completeExceptionally(exception)
                }
            }
        )
        imageDeferred.await()
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

    override suspend fun startVideoRecording() {
        Log.d(TAG, "recordVideo")
        val captureTypeString =
            when (captureMode) {
                CaptureMode.MULTI_STREAM -> "MultiStream"
                CaptureMode.SINGLE_STREAM -> "SingleStream"
            }
        val name = "JCA-recording-${Date()}-$captureTypeString.mp4"
        val contentValues =
            ContentValues().apply {
                put(MediaStore.Video.Media.DISPLAY_NAME, name)
            }
        val mediaStoreOutput =
            MediaStoreOutputOptions.Builder(
                application.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI
            )
                .setContentValues(contentValues)
                .build()
        recording =
            videoCaptureUseCase.output
                .prepareRecording(application, mediaStoreOutput)
                .start(ContextCompat.getMainExecutor(application)) { videoRecordEvent ->
                    run {
                        Log.d(TAG, videoRecordEvent.toString())
                    }
                }
    }

    override fun stopVideoRecording() {
        Log.d(TAG, "stopRecording")
        recording?.stop()
    }

    override fun setZoomScale(scale: Float) {
        currentSettings.update { old ->
            old?.copy(zoomScale = scale)
        }
    }

    // Could be improved by setting initial value only when camera is initialized
    private val _zoomScale = MutableStateFlow(1f)
    override fun getZoomScale(): StateFlow<Float> = _zoomScale.asStateFlow()

    private val _surfaceRequest = MutableStateFlow<SurfaceRequest?>(null)
    override fun getSurfaceRequest(): StateFlow<SurfaceRequest?> = _surfaceRequest.asStateFlow()

    // Sets the camera to the designated lensFacing direction
    override suspend fun setLensFacing(lensFacing: LensFacing) {
        currentSettings.update { old ->
            old?.copy(cameraLensFacing = lensFacing)
        }
    }

    override fun tapToFocus(
        display: Display,
        surfaceWidth: Int,
        surfaceHeight: Int,
        x: Float,
        y: Float
    ) {
        // TODO(tm):Convert API to use SurfaceOrientedMeteringPointFactory and
        // use a Channel to get result of FocusMeteringAction
    }

    override fun getScreenFlashEvents() = screenFlashEvents.asSharedFlow()
    override fun getCurrentSettings() = currentSettings.asStateFlow()

    override fun setFlashMode(flashMode: FlashMode) {
        currentSettings.update { old ->
            old?.copy(flashMode = flashMode)
        }
    }

    private fun setFlashModeInternal(flashMode: FlashMode, isFrontFacing: Boolean) {
        val isScreenFlashRequired =
            isFrontFacing && (flashMode == FlashMode.ON || flashMode == FlashMode.AUTO)

        if (isScreenFlashRequired) {
            imageCaptureUseCase.screenFlash = object : ScreenFlash {
                override fun apply(
                    expirationTimeMillis: Long,
                    listener: ImageCapture.ScreenFlashListener
                ) {
                    Log.d(TAG, "ImageCapture.ScreenFlash: apply")
                    coroutineScope.launch {
                        screenFlashEvents.emit(
                            CameraUseCase.ScreenFlashEvent(Type.APPLY_UI) {
                                listener.onCompleted()
                            }
                        )
                    }
                }

                override fun clear() {
                    Log.d(TAG, "ImageCapture.ScreenFlash: clear")
                    coroutineScope.launch {
                        screenFlashEvents.emit(
                            CameraUseCase.ScreenFlashEvent(Type.CLEAR_UI) {}
                        )
                    }
                }
            }
        }

        imageCaptureUseCase.flashMode = when (flashMode) {
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
        Log.d(TAG, "Set flash mode to: ${imageCaptureUseCase.flashMode}")
    }

    override fun isScreenFlashEnabled() =
        imageCaptureUseCase.flashMode == ImageCapture.FLASH_MODE_SCREEN &&
            imageCaptureUseCase.screenFlash != null

    override suspend fun setAspectRatio(aspectRatio: AspectRatio) {
        currentSettings.update { old ->
            old?.copy(aspectRatio = aspectRatio)
        }
    }

    override suspend fun setCaptureMode(captureMode: CaptureMode) {
        currentSettings.update { old ->
            old?.copy(captureMode = captureMode)
        }
    }

    private fun createUseCaseGroup(
        sessionSettings: PerpetualSessionSettings,
        initialTransientSettings: TransientSessionSettings,
        supportedStabilizationModes: List<SupportedStabilizationMode>
    ): UseCaseGroup {
        val previewUseCase = createPreviewUseCase(sessionSettings, supportedStabilizationModes)
        videoCaptureUseCase = createVideoUseCase(sessionSettings, supportedStabilizationModes)

        setFlashModeInternal(
            flashMode = initialTransientSettings.flashMode,
            isFrontFacing = sessionSettings.cameraSelector == CameraSelector.DEFAULT_FRONT_CAMERA
        )

        return UseCaseGroup.Builder().apply {
            setViewPort(
                ViewPort.Builder(
                    sessionSettings.aspectRatio.ratio,
                    previewUseCase.targetRotation
                ).build()
            )
            addUseCase(previewUseCase)
            addUseCase(imageCaptureUseCase)
            addUseCase(videoCaptureUseCase)

            if (sessionSettings.captureMode == CaptureMode.SINGLE_STREAM) {
                addEffect(SingleSurfaceForcingEffect())
            }
            captureMode = sessionSettings.captureMode
        }.build()
    }

    private fun createVideoUseCase(
        sessionSettings: PerpetualSessionSettings,
        supportedStabilizationMode: List<SupportedStabilizationMode>
    ): VideoCapture<Recorder> {
        val videoCaptureBuilder = VideoCapture.Builder(recorder)

        // set video stabilization

        if (shouldVideoBeStabilized(sessionSettings, supportedStabilizationMode)
        ) {
            videoCaptureBuilder.setVideoStabilizationEnabled(true)
        }
        // set target fps
        if (sessionSettings.targetFrameRate != TARGET_FPS_AUTO) {
            videoCaptureBuilder.setTargetFrameRate(
                Range(sessionSettings.targetFrameRate, sessionSettings.targetFrameRate)
            )
        }
        return videoCaptureBuilder.build()
    }

    private fun shouldVideoBeStabilized(
        sessionSettings: PerpetualSessionSettings,
        supportedStabilizationModes: List<SupportedStabilizationMode>
    ): Boolean {
        // video is on and target fps is not 60
        return (sessionSettings.targetFrameRate != TARGET_FPS_60) &&
            (supportedStabilizationModes.contains(SupportedStabilizationMode.HIGH_QUALITY)) &&
            // high quality (video only) selected
            (
                sessionSettings.stabilizeVideoMode == Stabilization.ON &&
                    sessionSettings.stabilizePreviewMode == Stabilization.UNDEFINED
                )
    }

    private fun createPreviewUseCase(
        sessionSettings: PerpetualSessionSettings,
        supportedStabilizationModes: List<SupportedStabilizationMode>
    ): Preview {
        val previewUseCaseBuilder = Preview.Builder()
        // set preview stabilization
        if (shouldPreviewBeStabilized(sessionSettings, supportedStabilizationModes)) {
            previewUseCaseBuilder.setPreviewStabilizationEnabled(true)
        }

        return previewUseCaseBuilder.build().apply {
            setSurfaceProvider { surfaceRequest ->
                _surfaceRequest.value = surfaceRequest
            }
        }
    }

    private fun shouldPreviewBeStabilized(
        sessionSettings: PerpetualSessionSettings,
        supportedStabilizationModes: List<SupportedStabilizationMode>
    ): Boolean {
        // only supported if target fps is 30 or none
        return (
            when (sessionSettings.targetFrameRate) {
                TARGET_FPS_AUTO, TARGET_FPS_30 -> true
                else -> false
            }
            ) &&
            (
                supportedStabilizationModes.contains(SupportedStabilizationMode.ON) &&
                    sessionSettings.stabilizePreviewMode == Stabilization.ON
                )
    }

    private fun LensFacing.toCameraSelector(): CameraSelector = when (this) {
        LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
    }

    companion object {
        /**
         * Checks if preview stabilization is supported by the device.
         *
         */
        private fun isPreviewStabilizationSupported(cameraInfo: CameraInfo): Boolean {
            return Preview.getPreviewCapabilities(cameraInfo).isStabilizationSupported
        }

        /**
         * Checks if video stabilization is supported by the device.
         *
         */
        private fun isVideoStabilizationSupported(cameraInfo: CameraInfo): Boolean {
            return Recorder.getVideoCapabilities(cameraInfo).isStabilizationSupported
        }
    }
}
