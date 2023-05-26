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
import android.util.Rational
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraSelector.LensFacing
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.ViewPort
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.concurrent.futures.await
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

private const val TAG = "CameraXCameraRepository"
private val ASPECT_RATIO_16_9 = Rational(16, 9)

/**
 * CameraX based implementation for [CameraUseCase]
 */
class CameraXCameraUseCase @Inject constructor(
    private val application: Application
) : CameraUseCase {
    private lateinit var cameraProvider: ProcessCameraProvider

    private val imageCaptureUseCase = ImageCapture.Builder()
        .build()

    private val previewUseCase = Preview.Builder()
        .build()

    private val useCaseGroup = UseCaseGroup.Builder()
        .setViewPort(ViewPort.Builder(ASPECT_RATIO_16_9, previewUseCase.targetRotation).build())
        .addUseCase(previewUseCase)
        .addUseCase(imageCaptureUseCase)
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

    override suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
        @LensFacing lensFacing: Int,
    ) = coroutineScope {
        Log.d(TAG, "startPreview")

        val cameraSelector = cameraLensToSelector(lensFacing)

        previewUseCase.setSurfaceProvider(surfaceProvider)

        cameraProvider.runWith(cameraSelector, useCaseGroup) {
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
