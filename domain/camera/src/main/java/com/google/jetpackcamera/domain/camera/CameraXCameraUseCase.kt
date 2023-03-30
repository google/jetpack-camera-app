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
import android.util.Log
import androidx.camera.core.AspectRatio
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import androidx.lifecycle.LifecycleOwner
import javax.inject.Inject

private const val TAG = "CameraXCameraRepository"

/**
 * CameraX based implementation for [CameraUseCase]
 */
class CameraXCameraUseCase @Inject constructor(
    private val application: Application
) : CameraUseCase {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var camera: Camera

    private val imageCaptureUseCase = ImageCapture.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()

    private val previewUseCase = Preview.Builder()
        .setTargetAspectRatio(AspectRatio.RATIO_16_9)
        .build()

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

    override fun startPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: Preview.SurfaceProvider,
        @LensFacing lensFacing: Int,
    ) {
        Log.d(TAG, "startPreview")

        val cameraSelector = cameraLensToSelector(lensFacing)

        previewUseCase.setSurfaceProvider(surfaceProvider)

        cameraProvider.unbindAll()
        camera = cameraProvider.bindToLifecycle(
            lifecycleOwner,
            cameraSelector,
            previewUseCase,
            imageCaptureUseCase
        )
    }

    private fun cameraLensToSelector(@LensFacing lensFacing: Int): CameraSelector =
        when (lensFacing) {
            CameraSelector.LENS_FACING_FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
            CameraSelector.LENS_FACING_BACK -> CameraSelector.DEFAULT_BACK_CAMERA
            else -> throw IllegalArgumentException("Invalid lens facing type: $lensFacing")
        }
}