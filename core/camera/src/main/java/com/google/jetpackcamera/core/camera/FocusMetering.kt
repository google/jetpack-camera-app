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
package com.google.jetpackcamera.core.camera

import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceRequest
import androidx.concurrent.futures.await
import androidx.core.content.ContextCompat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

private const val TAG = "FocusMetering"
private const val SCENE_CHANGE_POST_LOCK_DELAY_MILLIS = 3000L
private const val REQUIRED_CONSECUTIVE_SCENE_CHANGE_FRAMES = 3

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun CameraSessionContext.processFocusMeteringEvents(
    cameraInfo: CameraInfo,
    cameraControl: CameraControl,
    captureResults: StateFlow<TotalCaptureResult?>? = null
) {
    surfaceRequests.flatMapLatest { surfaceRequest ->
        surfaceRequest?.let { request ->
            Log.d(
                TAG,
                "Waiting to process focus points for surface with resolution: " +
                    "${request.resolution.width} x ${request.resolution.height}"
            )

            request.createTransformationInfoFlow(ContextCompat.getMainExecutor(context))
                .filterNotNull()
                .map {
                    SurfaceToSensorMeteringPointFactory(
                        cameraInfo.sensorRect,
                        it.sensorToBufferTransform
                    )
                }
        } ?: flowOf(null)
    }.collectLatest { meteringPointFactory ->
        focusMeteringEvents
            .receiveAsFlow()
            .onCompletion {
                currentCameraState.update { old ->
                    old.copy(focusState = FocusState.Unspecified)
                }
            }
            .collectLatest { event ->
                meteringPointFactory?.apply {
                    Log.d(TAG, "tapToFocus, processing event: $event")

                    fun updateFocusState(status: FocusState.Status) {
                        currentCameraState.update { old ->
                            old.copy(
                                focusState = FocusState.Specified(
                                    x = event.x,
                                    y = event.y,
                                    status = status
                                )
                            )
                        }
                    }

                    val meteringPoint = createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(meteringPoint)
                        .disableAutoCancel()
                        .build()

                    if (!cameraInfo.isFocusMeteringSupported(action)) {
                        Log.w(TAG, "Focus metering not supported for action: $action")
                        return@apply
                    }

                    updateFocusState(FocusState.Status.RUNNING)
                    val completionStatus: FocusState.Status = try {
                        if (cameraControl.startFocusAndMetering(action).await().isFocusSuccessful) {
                            FocusState.Status.SUCCESS
                        } else {
                            FocusState.Status.FAILURE
                        }
                    } catch (_: CameraControl.OperationCanceledException) {
                        FocusState.Status.CANCELLED
                    } catch (e: IllegalArgumentException) {
                        Log.w(TAG, "tapToFocus failed", e)
                        FocusState.Status.FAILURE
                    }

                    Log.d(
                        TAG,
                        "tapToFocus, finished processing event: $event. Result: $completionStatus"
                    )

                    updateFocusState(completionStatus)

                    if (completionStatus == FocusState.Status.SUCCESS &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.P &&
                        captureResults != null
                    ) {
                        delay(SCENE_CHANGE_POST_LOCK_DELAY_MILLIS)

                        var consecutiveFrames = 0
                        captureResults.filterNotNull().first { result ->
                            val isSceneChange = result.get(CaptureResult.CONTROL_AF_SCENE_CHANGE) ==
                                CameraMetadata.CONTROL_AF_SCENE_CHANGE_DETECTED
                            if (isSceneChange) {
                                consecutiveFrames++
                            } else {
                                consecutiveFrames = 0
                            }
                            consecutiveFrames >= REQUIRED_CONSECUTIVE_SCENE_CHANGE_FRAMES
                        }
                        Log.i(
                            TAG,
                            "*** AF SCENE CHANGE DETECTED " +
                                "($REQUIRED_CONSECUTIVE_SCENE_CHANGE_FRAMES consecutive frames)! " +
                                "Cancelling focus lock ***"
                        )
                        try {
                            cameraControl.cancelFocusAndMetering().await()
                        } catch (_: CameraControl.OperationCanceledException) {
                            // Ignored if cancelled by a subsequent action
                        } catch (e: Exception) {
                            Log.w(TAG, "cancelFocusAndMetering failed", e)
                        }
                        updateFocusState(FocusState.Status.CANCELLED)
                    }
                }
            }
    }
}

private fun SurfaceRequest.createTransformationInfoFlow(
    executor: java.util.concurrent.Executor
): Flow<SurfaceRequest.TransformationInfo> = callbackFlow {
    val listener = SurfaceRequest.TransformationInfoListener { transformationInfo ->
        trySend(transformationInfo)
    }
    setTransformationInfoListener(executor, listener)

    awaitClose { clearTransformationInfoListener() }
}
