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

import android.util.Log
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraInfo
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceRequest
import androidx.concurrent.futures.await
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

private const val TAG = "FocusMetering"
private const val AUTO_FOCUS_TIMEOUT_MILLIS = 2500L

@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun CameraSessionContext.processFocusMeteringEvents(
    cameraInfo: CameraInfo,
    cameraControl: CameraControl
) {
    surfaceRequests.flatMapLatest { surfaceRequest ->
        surfaceRequest?.let { request ->
            Log.d(
                TAG,
                "Waiting to process focus points for surface with resolution: " +
                    "${request.resolution.width} x ${request.resolution.height}"
            )

            request.transformationInfoFlow.filterNotNull().map {
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

                    updateFocusState(FocusState.Status.RUNNING)
                    val meteringPoint = createPoint(event.x, event.y)
                    val action = FocusMeteringAction.Builder(meteringPoint)
                        .setAutoCancelDuration(AUTO_FOCUS_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)
                        .build()
                    val completionStatus: FocusState.Status = try {
                        if (cameraControl.startFocusAndMetering(action).await().isFocusSuccessful) {
                            FocusState.Status.SUCCESS
                        } else {
                            FocusState.Status.FAILURE
                        }
                    } catch (_: CameraControl.OperationCanceledException) {
                        FocusState.Status.FAILURE
                    }

                    Log.d(
                        TAG,
                        "tapToFocus, finished processing event: $event. Result: $completionStatus"
                    )

                    updateFocusState(completionStatus)
                }
            }
    }
}

private val SurfaceRequest.transformationInfoFlow: StateFlow<SurfaceRequest.TransformationInfo?>
    get() = MutableStateFlow<SurfaceRequest.TransformationInfo?>(null)
        .also { stateFlow ->
            // Set a callback to update this state flow
            setTransformationInfoListener(Runnable::run) { transformInfo ->
                // Set the next value of the flow
                stateFlow.value = transformInfo
            }
        }
        .asStateFlow()
