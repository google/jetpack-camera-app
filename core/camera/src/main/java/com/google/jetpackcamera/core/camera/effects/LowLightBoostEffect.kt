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
package com.google.jetpackcamera.core.camera.effects

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraEffect
import com.google.android.gms.cameralowlight.LowLightBoostClient
import com.google.android.gms.cameralowlight.SceneDetectorCallback
import com.google.android.gms.common.api.Status
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.effects.processors.LowLightBoostSurfaceProcessor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow

private const val TARGETS =
    CameraEffect.PREVIEW or CameraEffect.IMAGE_CAPTURE or CameraEffect.VIDEO_CAPTURE

/**
 * [CameraEffect] that applies Google Low Light Boost.
 */
@SuppressLint("RestrictedApi")
@RequiresApi(Build.VERSION_CODES.R)
class LowLightBoostEffect(
    cameraId: String,
    lowLightBoostClient: LowLightBoostClient,
    sessionContainer: LowLightBoostSessionContainer,
    coroutineScope: CoroutineScope,
    sceneDetectorCallback: SceneDetectorCallback? = null,
    onLowLightBoostErrorCallback: () -> Unit = {}
    ) : CameraEffect(
    TARGETS,
    OUTPUT_OPTION_ONE_FOR_ALL_TARGETS,
    TRANSFORMATION_CAMERA_AND_SURFACE_ROTATION,
    Runnable::run,
    LowLightBoostSurfaceProcessor(cameraId, lowLightBoostClient, sessionContainer, coroutineScope, sceneDetectorCallback, onLowLightBoostErrorCallback),
    {}
)
