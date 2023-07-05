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
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asExecutor
import java.util.Date
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

private const val TAG = "CameraXCameraUseCase"
private val ASPECT_RATIO_16_9 = Rational(16, 9)

/**
 * CameraX based implementation for [CameraUseCase]
 */
class CameraXCameraUseCase @Inject constructor(
    private val application: Application,
    private val defaultDispatcher: CoroutineDispatcher
) : CameraUseCase {
    private lateinit var cameraProvider: ProcessCameraProvider

    private val imageCaptureUseCase = ImageCapture.Builder()
        .build()

    private val previewUseCase = Preview.Builder()
        .build()

    private val recorder = Recorder.Builder().setExecutor(defaultDispatcher.asExecutor()).build()
    private val videoCaptureUseCase = VideoCapture.withOutput(recorder)

    private val useCaseGroup = UseCaseGroup.Builder()
        .setViewPort(ViewPort.Builder(ASPECT_RATIO_16_9, previewUseCase.targetRotation).build())
        .addUseCase(previewUseCase)
        .addUseCase(imageCaptureUseCase)
        .addUseCase(videoCaptureUseCase)
        .build()

    private var recording : Recording? = null

    private var camera: Camera? = null
    override suspend fun initialize(): List<Int> {
        cameraProvider = ProcessCameraProvider.getInstance(application).await()

        val availableCameraLens =
            listOf(
                CameraSelector.LENS_FACING_BACK,
                CameraSelector.LENS_FACING_FRONT
            ).filter { lensFacing ->
                cameraProvider.hasCamera(cameraLensToSelector(lensFacing))
            }

        return availableCameraLens
    }

    override suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
        @LensFacing lensFacing: Int,
    ) = coroutineScope {
        Log.d(TAG, "startPreview")

        val cameraSelector = cameraLensToSelector(lensFacing)
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
        val name = "JCA-recording-${Date()}.mp4"
        val contentValues = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, name)
        }
        val mediaStoreOutput = MediaStoreOutputOptions.Builder(application.contentResolver,
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
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

    override fun setZoomRatio(scale: Float) {
        val zoomState = getZoomState()
        if (zoomState != null) {
            val finalScale = (zoomState.zoomRatio * scale).coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
            camera?.cameraControl?.setZoomRatio(finalScale)
        }
    }

    private fun getZoomState(): ZoomState? {
        if (camera == null) {
            return null
        }
        return camera!!.cameraInfo.zoomState.value
    }

    private fun cameraLensToSelector(@LensFacing lensFacing: Int): CameraSelector =
        when (lensFacing) {
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> throw IllegalArgumentException("Invalid lens facing type: $lensFacing")
        }
}
