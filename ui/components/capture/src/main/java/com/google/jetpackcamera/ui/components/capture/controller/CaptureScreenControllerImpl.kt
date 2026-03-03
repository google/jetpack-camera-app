/*
 * Copyright (C) 2026 The Android Open Source Project
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
package com.google.jetpackcamera.ui.components.capture.controller

import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.model.DeviceRotation
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Implementation of [CaptureScreenController] that handles UI events on the capture screen.
 *
 * @param cameraSystem The [CameraSystem] to interact with.
 * @param updateLastCapturedMediaCallback Callback to update the last captured media.
 * @param coroutineContext The [CoroutineContext] for launching coroutines.
 */
class CaptureScreenControllerImpl(
    private val cameraSystem: CameraSystem,
    private val updateLastCapturedMediaCallback: () -> Unit,
    coroutineContext: CoroutineContext
) : CaptureScreenController {
    private val job = Job(parent = coroutineContext[Job])
    private val scope = CoroutineScope(coroutineContext + job)

    override fun setDisplayRotation(deviceRotation: DeviceRotation) {
        scope.launch {
            cameraSystem.setDeviceRotation(deviceRotation)
        }
    }

    override fun updateLastCapturedMedia() {
        updateLastCapturedMediaCallback()
    }
}
