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
import android.content.ContentValues
import android.provider.MediaStore
import android.util.Log
import android.view.Display
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.DisplayOrientedMeteringPointFactory
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.core.ZoomState
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import com.google.jetpackcamera.domain.camera.CameraUseCase.Companion.INVALID_ZOOM_SCALE
import com.google.jetpackcamera.settings.SettingsRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import java.util.Date
import javax.inject.Inject

private const val TAG = "CameraXCameraUseCase"

/**
 * CameraX based implementation for [CameraUseCase]
 */
class CameraXCameraUseCase
@Inject
constructor(
    private val application: Application,
    private val defaultDispatcher: CoroutineDispatcher,
    private val settingsRepository: SettingsRepository
) : CameraUseCase {
    private var camera: Camera? = null
    private lateinit var cameraProvider: ProcessCameraProvider

    // TODO apply flash from settings
    private val imageCaptureUseCase = ImageCapture.Builder().build()
    private val previewUseCase: Preview = Preview.Builder().build()

    private val recorder = Recorder.Builder().setExecutor(
        defaultDispatcher.asExecutor()
    ).build()
    private val videoCaptureUseCase = VideoCapture.withOutput(recorder)
    private var recording: Recording? = null

    private lateinit var useCaseGroup: UseCaseGroup

    private val _config: MutableStateFlow<CameraUseCase.Config> =
        MutableStateFlow(
            CameraUseCase.Config(),
        )

    override val config = _config

    override suspend fun initialize(initialConfig: CameraUseCase.Config) {
        _config.emit(initialConfig)
        updateUseCaseGroup()
        cameraProvider = ProcessCameraProvider.getInstance(application).await()

        //updates values for available camera lens if necessary
        coroutineScope {
            settingsRepository.updateAvailableCameraLens(
                cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA),
                cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
            )
        }
    }

    override suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
    ) = coroutineScope {
        Log.d(TAG, "startPreview")

        val cameraSelector = config.value.lensFacing.toCameraSelector()

        previewUseCase.setSurfaceProvider(surfaceProvider)

        cameraProvider.runWith(cameraSelector, useCaseGroup) {
            camera = it
            awaitCancellation()
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
                }

                override fun onError(exception: ImageCaptureException) {
                    super.onError(exception)
                    Log.d(TAG, "takePicture onError: $exception")
                }
            }
        )
    }

    override suspend fun startVideoRecording() {
        Log.d(TAG, "recordVideo")

        val captureModeFileIndicator =
            if(config.value.captureMode == CameraUseCase.CaptureMode.SINGLE_STREAM) {
                "SingleStream"
            } else {
                "MultiStream"
            }

        val name = "JCA-recording-${Date()}-$captureModeFileIndicator.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
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

    override fun setZoomScale(scale: Float): Float {
        val zoomState = getZoomState() ?: return INVALID_ZOOM_SCALE
        val finalScale =
            (zoomState.zoomRatio * scale).coerceIn(
                zoomState.minZoomRatio,
                zoomState.maxZoomRatio
            )
        camera?.cameraControl?.setZoomRatio(finalScale)
        return finalScale
    }

    private fun getZoomState(): ZoomState? = camera?.cameraInfo?.zoomState?.value

    override suspend fun setLensFacing(lensFacing: CameraUseCase.LensFacing) {
        _config.emit(
            config.value.copy(lensFacing = lensFacing)
        )
        rebindUseCases()
    }

    override fun tapToFocus(
        display: Display,
        surfaceWidth: Int,
        surfaceHeight: Int,
        x: Float,
        y: Float
    ) {
        if (camera != null) {
            val meteringPoint =
                DisplayOrientedMeteringPointFactory(
                    display,
                    camera!!.cameraInfo,
                    surfaceWidth.toFloat(),
                    surfaceHeight.toFloat()
                )
                    .createPoint(x, y)

            val action = FocusMeteringAction.Builder(meteringPoint).build()

            camera!!.cameraControl.startFocusAndMetering(action)
            Log.d(TAG, "Tap to focus on: $meteringPoint")
        }
    }

    override suspend fun setFlashMode(flashMode: CameraUseCase.FlashMode) {
        Log.d(TAG, "Changing FlashMode: $flashMode")
        _config.emit(
            config.value.copy(flashMode = flashMode)
        )
        imageCaptureUseCase.flashMode = flashMode.toImageCaptureFlashMode()
    }

    override suspend fun setAspectRatio(aspectRatio: CameraUseCase.AspectRatio) {
        Log.d(TAG, "Changing AspectRatio: $aspectRatio")
        _config.emit(
            config.value.copy(aspectRatio = aspectRatio)
        )
        updateUseCaseGroup()
        rebindUseCases()
    }

    override suspend fun setCaptureMode(captureMode: CameraUseCase.CaptureMode) {
        Log.d(TAG, "Changing CaptureMode: $captureMode")
        _config.emit(
            config.value.copy(captureMode = captureMode)
        )
        updateUseCaseGroup()
        rebindUseCases()
    }

    private fun updateUseCaseGroup() {
        val useCaseGroupBuilder = UseCaseGroup.Builder()
            .setViewPort(ViewPort.Builder(config.value.aspectRatio.rational, previewUseCase.targetRotation).build())
            .addUseCase(previewUseCase)
            .addUseCase(imageCaptureUseCase)
            .addUseCase(videoCaptureUseCase)

        if (config.value.captureMode == CameraUseCase.CaptureMode.SINGLE_STREAM) {
            useCaseGroupBuilder.addEffect(SingleSurfaceForcingEffect())
        }

        useCaseGroup = useCaseGroupBuilder.build()
    }

    private suspend fun rebindUseCases() {
        cameraProvider.unbindAll()
        cameraProvider.runWith(config.value.lensFacing.toCameraSelector(), useCaseGroup) {
            camera = it
            awaitCancellation()
        }
    }

    private fun CameraUseCase.LensFacing.toCameraSelector() = when(this) {
        CameraUseCase.LensFacing.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        CameraUseCase.LensFacing.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
    }

    private fun CameraUseCase.FlashMode.toImageCaptureFlashMode() = when (this) {
        CameraUseCase.FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        CameraUseCase.FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        CameraUseCase.FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }
}
