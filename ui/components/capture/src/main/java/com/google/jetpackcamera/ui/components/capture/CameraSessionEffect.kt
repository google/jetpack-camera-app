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
package com.google.jetpackcamera.ui.components.capture

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.ui.controller.CameraController

/**
 * Composable effect that manages the camera session lifecycle and display rotation updates.
 *
 * While [enabled] is `true` and the current [LocalLifecycleOwner] is at least
 * [Lifecycle.State.STARTED], this effect:
 * 1. Starts the camera via [CameraController.startCamera] and invokes [onSessionStarted], stopping
 *    the camera via [CameraController.stopCamera] and invoking [onSessionStopped] when the
 *    lifecycle stops, [enabled] becomes `false`, or the effect leaves the composition.
 * 2. Collects [debouncedOrientationFlow] and forwards device rotation changes to
 *    [CameraController.setDisplayRotation].
 *
 * [enabled] has no default value so that host screens make an explicit readiness or runtime
 * permission decision before starting the camera session.
 *
 * @param cameraController the [CameraController] to start, stop, and update with display rotation.
 * @param enabled whether the camera session is allowed to run (for example, `true` once required
 *   camera permissions have been granted).
 * @param onSessionStarted optional callback invoked right after [CameraController.startCamera].
 * @param onSessionStopped optional callback invoked right before [CameraController.stopCamera].
 */
@Composable
fun CameraSessionEffect(
    cameraController: CameraController,
    enabled: Boolean,
    onSessionStarted: () -> Unit = {},
    onSessionStopped: () -> Unit = {}
) {
    val currentOnSessionStarted by rememberUpdatedState(onSessionStarted)
    val currentOnSessionStopped by rememberUpdatedState(onSessionStopped)

    LifecycleStartEffect(enabled, cameraController) {
        if (enabled) {
            cameraController.startCamera()
            currentOnSessionStarted()
        }
        onStopOrDispose {
            if (enabled) {
                currentOnSessionStopped()
                cameraController.stopCamera()
            }
        }
    }

    if (enabled) {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        LaunchedEffect(cameraController, context, lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                debouncedOrientationFlow(context)
                    .collect(cameraController::setDisplayRotation)
            }
        }
    }
}
