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
import android.util.Rational
import android.view.Display
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
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
import androidx.core.util.Consumer
import com.google.jetpackcamera.domain.camera.CameraUseCase.Companion.INVALID_ZOOM_SCALE
import com.google.jetpackcamera.settings.SettingsRepository
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.FlashModeStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import java.util.Date
import javax.inject.Inject

private const val TAG = "CameraXCameraUseCase"
private val ASPECT_RATIO_16_9 = Rational(16, 9)

/**
 * CameraX based implementation for [CameraUseCase]
 */
class CameraXCameraUseCase @Inject constructor(
    private val application: Application,
    private val defaultDispatcher: CoroutineDispatcher,
    private val settingsRepository: SettingsRepository
) : CameraUseCase {

    private var camera: Camera? = null
    private lateinit var cameraProvider: ProcessCameraProvider

    //TODO apply flash from settings
    private val imageCaptureUseCase = ImageCapture.Builder().build()
    private val previewUseCase = Preview.Builder().build()

    private val recorder = Recorder.Builder().setExecutor(defaultDispatcher.asExecutor()).build()
    private val videoCaptureUseCase = VideoCapture.withOutput(recorder)
    private var recording: Recording? = null

    private lateinit var useCaseGroup: UseCaseGroup

    private lateinit var aspectRatio: AspectRatio
    private lateinit var captureMode: CaptureMode
    private var isFrontFacing = true

    override suspend fun initialize(currentCameraSettings: CameraAppSettings): List<Int> {
        this.aspectRatio = currentCameraSettings.aspect_ratio
        this.captureMode = currentCameraSettings.captureMode
        setFlashMode(currentCameraSettings.flash_mode_status)
        updateUseCaseGroup()
        cameraProvider = ProcessCameraProvider.getInstance(application).await()

        val availableCameraLens =
            listOf(
                CameraSelector.LENS_FACING_BACK,
                CameraSelector.LENS_FACING_FRONT
            ).filter { lensFacing ->
                cameraProvider.hasCamera(cameraLensToSelector(lensFacing))
            }

        //updates values for available camera lens if necessary
        coroutineScope {
            settingsRepository.updateAvailableCameraLens(
                availableCameraLens.contains(CameraSelector.LENS_FACING_FRONT),
                availableCameraLens.contains(CameraSelector.LENS_FACING_BACK)
            )
        }

        return availableCameraLens
    }

    override suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
        currentCameraSettings: CameraAppSettings,
    ) = coroutineScope {
        Log.d(TAG, "startPreview")

        val cameraSelector =
            cameraLensToSelector(getLensFacing(currentCameraSettings.default_front_camera))

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
            })
    }

    override suspend fun startVideoRecording() {
        Log.d(TAG, "recordVideo")
        val captureTypeString = when (captureMode) {
            CaptureMode.MULTI_STREAM -> "SingleStream"
            CaptureMode.SINGLE_STREAM -> "MultiStream"
        }
        val name = "JCA-recording-${Date()}-$captureTypeString.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(
            application.contentResolver,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        )
            .setContentValues(contentValues)
            .build()

        recording = videoCaptureUseCase.output
            .prepareRecording(application, mediaStoreOutput)
            .start(ContextCompat.getMainExecutor(application), Consumer { videoRecordEvent ->
                run {
                    Log.d(TAG, videoRecordEvent.toString())
                }
            })
    }

    override fun stopVideoRecording() {
        Log.d(TAG, "stopRecording")
        recording?.stop()
    }

    override fun setZoomScale(scale: Float): Float {
        val zoomState = getZoomState() ?: return INVALID_ZOOM_SCALE
        val finalScale =
            (zoomState.zoomRatio * scale).coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        camera?.cameraControl?.setZoomRatio(finalScale)
        return finalScale
    }

    private fun getZoomState(): ZoomState? = camera?.cameraInfo?.zoomState?.value

    // flips the camera to the designated lensFacing direction
    override suspend fun flipCamera(isFrontFacing: Boolean) {
        this.isFrontFacing = isFrontFacing
        rebindUseCases(

        )
    }

    override fun tapToFocus(
        display: Display,
        surfaceWidth: Int,
        surfaceHeight: Int,
        x: Float,
        y: Float
    ) {
        if (camera != null) {
            val meteringPoint = DisplayOrientedMeteringPointFactory(
                display,
                camera!!.cameraInfo,
                surfaceWidth.toFloat(),
                surfaceHeight.toFloat()
            )
                .createPoint(x, y);

            val action = FocusMeteringAction.Builder(meteringPoint).build()

            camera!!.cameraControl.startFocusAndMetering(action)
            Log.d(TAG, "Tap to focus on: $meteringPoint")
        }
    }

    override fun setFlashMode(flashModeStatus: FlashModeStatus) {
        imageCaptureUseCase.flashMode = when (flashModeStatus) {
            FlashModeStatus.OFF -> ImageCapture.FLASH_MODE_OFF // 2
            FlashModeStatus.ON -> ImageCapture.FLASH_MODE_ON // 1
            FlashModeStatus.AUTO -> ImageCapture.FLASH_MODE_AUTO // 0
        }
        Log.d(TAG, "Set flash mode to: ${imageCaptureUseCase.flashMode}")
    }

    override suspend fun setAspectRatio(aspectRatio: AspectRatio, isFrontFacing: Boolean) {
        this.aspectRatio = aspectRatio
        updateUseCaseGroup()
        rebindUseCases()
    }

    override suspend fun setCaptureMode(newCaptureMode: CaptureMode) {
        captureMode = newCaptureMode
        Log.d(
            TAG,
            "Changing CaptureMode: singleStreamCaptureEnabled:" +
                    (captureMode == CaptureMode.SINGLE_STREAM)
        )
        updateUseCaseGroup()
        rebindUseCases()
    }


    private fun updateUseCaseGroup() {
        val useCaseGroupBuilder = UseCaseGroup.Builder()
            .setViewPort(ViewPort.Builder(aspectRatio.ratio, previewUseCase.targetRotation).build())
            .addUseCase(previewUseCase)
            .addUseCase(imageCaptureUseCase)
            .addUseCase(videoCaptureUseCase)

        if (captureMode == CaptureMode.SINGLE_STREAM) {
            useCaseGroupBuilder.addEffect(SingleSurfaceForcingEffect())
        }

        useCaseGroup = useCaseGroupBuilder.build()
    }

    // converts LensFacing from datastore to @LensFacing Int value
    private fun getLensFacing(isFrontFacing: Boolean): Int =
        when (isFrontFacing) {
            true -> CameraSelector.LENS_FACING_FRONT
            false -> CameraSelector.LENS_FACING_BACK
        }

    private suspend fun rebindUseCases() {
        val cameraSelector = cameraLensToSelector(
            getLensFacing(isFrontFacing)
        )
        cameraProvider.unbindAll()
        cameraProvider.runWith(cameraSelector, useCaseGroup) {
            camera = it
            awaitCancellation()
        }
    }

    private fun cameraLensToSelector(@LensFacing lensFacing: Int): CameraSelector =
        when (lensFacing) {
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> throw IllegalArgumentException("Invalid lens facing type: $lensFacing")
        }
}
