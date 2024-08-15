/*
 * Copyright (C) 2024 The Android Open Source Project
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

import android.content.Context
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Context that can be shared by all functions in a camera session.
 *
 * Can be used to confer context (such as reactive state or session-wide parameters)
 * on context receivers using [with] in a camera session.
 */
internal data class CameraSessionContext(
    val context: Context,
    val cameraProvider: ProcessCameraProvider,
    val backgroundDispatcher: CoroutineDispatcher,
    val screenFlashEvents: MutableSharedFlow<CameraUseCase.ScreenFlashEvent>,
    val focusMeteringEvents: Channel<CameraEvent.FocusMeteringEvent>,
    val videoCaptureControlEvents: Channel<VideoCaptureControlEvent>,
    val currentCameraState: MutableStateFlow<CameraState>,
    val surfaceRequests: MutableStateFlow<SurfaceRequest?>,
    val transientSettings: StateFlow<TransientSessionSettings?>
)
