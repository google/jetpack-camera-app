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

package com.google.jetpackcamera.domain.camera.test

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.google.jetpackcamera.domain.camera.CameraUseCase
import kotlin.IllegalStateException

class FakeCameraUseCase : CameraUseCase {

    private val availableLenses = listOf(CameraSelector.LENS_FACING_FRONT, CameraSelector.LENS_FACING_BACK)
    private var initialized = false
    private var useCasesBinded = false

    var previewStarted = false
    var numPicturesTaken = 0

    override suspend fun initialize(): List<Int> {
        initialized = true
        return availableLenses
    }

    override suspend fun runCamera(
        surfaceProvider: Preview.SurfaceProvider,
        lensFacing: Int
    ) {
        if (!initialized)     {
            throw IllegalStateException("CameraProvider not initialized")
        }
        if (!availableLenses.contains(lensFacing)) {
            throw IllegalStateException("Requested lens not available")
        }
        useCasesBinded = true
        previewStarted = true
    }

    override suspend fun takePicture() {
        if(!useCasesBinded) {
            throw IllegalStateException("Usecases not binded")
        }
        numPicturesTaken += 1
    }
}