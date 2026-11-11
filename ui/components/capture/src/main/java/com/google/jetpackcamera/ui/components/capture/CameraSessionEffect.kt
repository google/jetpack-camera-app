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

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.model.DeviceRotation
import com.google.jetpackcamera.ui.controller.CameraController

/**
 * Composable effect that manages the camera session lifecycle, display rotation updates, and
 * recording orientation lock.
 *
 * While [enabled] is `true` and the current [LocalLifecycleOwner] is at least
 * [Lifecycle.State.STARTED], this effect:
 * 1. Starts the camera via [CameraController.startCamera] and invokes [onSessionStarted], stopping
 *    the camera via [CameraController.stopCamera] and invoking [onSessionStopped] when the
 *    lifecycle stops, [enabled] becomes `false`, or the effect leaves the composition.
 * 2. Collects [debouncedOrientationFlow] and forwards device rotation changes to
 *    [CameraController.setDisplayRotation] and [onDeviceRotationChanged] while [isRecording] is
 *    `false`.
 * 3. While [isRecording] is `true`, freezes display rotation updates and locks the host
 *    [Activity]'s `requestedOrientation` to [ActivityInfo.SCREEN_ORIENTATION_LOCKED], restoring its
 *    previous `requestedOrientation` and flushing any pending sensor rotation once recording ends.
 *
 * [enabled] has no default value so that host screens make an explicit readiness or runtime
 * permission decision before starting the camera session.
 *
 * @param cameraController the [CameraController] to start, stop, and update with display rotation.
 * @param enabled whether the camera session is allowed to run (for example, `true` once required
 *   camera permissions have been granted).
 * @param isRecording whether an active or starting video recording session is in progress.
 * @param onDeviceRotationChanged optional callback notified with the active (unfrozen) physical
 *   [DeviceRotation].
 * @param onSessionStarted optional callback invoked right after [CameraController.startCamera].
 * @param onSessionStopped optional callback invoked right before [CameraController.stopCamera].
 */
@Composable
fun CameraSessionEffect(
    cameraController: CameraController,
    enabled: Boolean,
    isRecording: Boolean = false,
    onDeviceRotationChanged: (DeviceRotation) -> Unit = {},
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
        val currentIsRecording by rememberUpdatedState(isRecording)
        val currentOnDeviceRotationChanged by rememberUpdatedState(onDeviceRotationChanged)
        var pendingRotation by remember(cameraController) { mutableStateOf<DeviceRotation?>(null) }
        var lastDeliveredRotation by remember(cameraController) {
            mutableStateOf<DeviceRotation?>(null)
        }

        LaunchedEffect(cameraController, context, lifecycleOwner) {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                debouncedOrientationFlow(context).collect { rotation ->
                    pendingRotation = rotation
                    if (!currentIsRecording) {
                        lastDeliveredRotation = rotation
                        cameraController.setDisplayRotation(rotation)
                        currentOnDeviceRotationChanged(rotation)
                    }
                }
            }
        }

        LaunchedEffect(isRecording, cameraController) {
            if (!isRecording) {
                val rotation = pendingRotation
                if (rotation != null && rotation != lastDeliveredRotation) {
                    lastDeliveredRotation = rotation
                    cameraController.setDisplayRotation(rotation)
                    currentOnDeviceRotationChanged(rotation)
                }
            }
        }

        val activity = LocalActivity.current ?: remember(context) { context.findSessionActivity() }
        DisposableEffect(activity, isRecording) {
            if (activity != null && isRecording) {
                val previousOrientation = activity.requestedOrientation
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LOCKED
                onDispose {
                    activity.requestedOrientation = previousOrientation
                }
            } else {
                onDispose {}
            }
        }
    }
}

private tailrec fun Context.findSessionActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findSessionActivity()
    else -> null
}
