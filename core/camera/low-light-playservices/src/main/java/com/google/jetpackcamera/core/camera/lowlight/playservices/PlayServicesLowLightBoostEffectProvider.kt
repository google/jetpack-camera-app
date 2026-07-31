/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.core.camera.lowlight.playservices

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraEffect
import com.google.android.gms.cameralowlight.LowLightBoost
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.android.gms.cameralowlight.SceneDetectorCallback
import com.google.jetpackcamera.core.camera.lowlight.LowLightBoostEffectProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow

class PlayServicesLowLightBoostEffectProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : LowLightBoostEffectProvider {
    @SuppressLint("RestrictedApi")
    @RequiresApi(Build.VERSION_CODES.R)
    override fun create(
        cameraId: String,
        captureResults: StateFlow<TotalCaptureResult?>,
        coroutineScope: CoroutineScope,
        onSceneBrightnessChanged: (Float) -> Unit,
        onLowLightBoostError: (Exception) -> Unit
    ): CameraEffect {
        val sceneDetectorCallback = object : SceneDetectorCallback {
            override fun onSceneBrightnessChanged(
                session: LowLightBoostSession,
                boostStrength: Float
            ) {
                onSceneBrightnessChanged(boostStrength)
            }
        }
        return PlayServicesLowLightBoostEffect(
            cameraId = cameraId,
            lowLightBoostClient = LowLightBoost.getClient(context),
            captureResults = captureResults,
            coroutineScope = coroutineScope,
            sceneDetectorCallback = sceneDetectorCallback,
            onLowLightBoostErrorCallback = onLowLightBoostError
        )
    }
}
