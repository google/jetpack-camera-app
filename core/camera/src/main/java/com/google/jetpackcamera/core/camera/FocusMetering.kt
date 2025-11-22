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
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.concurrent.futures.await
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

private const val TAG = "FocusMetering"

context(CameraSessionContext)
internal suspend fun processFocusMeteringEvents(cameraControl: CameraControl) {
    surfaceRequests.map { surfaceRequest ->
        surfaceRequest?.resolution?.run {
            Log.d(
                TAG,
                "Waiting to process focus points for surface with resolution: " +
                    "$width x $height"
            )
            SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
        }
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
                    val action = FocusMeteringAction.Builder(meteringPoint).build()
                    val completionStatus: FocusState.Status = try {
                        if (cameraControl.startFocusAndMetering(action).await().isFocusSuccessful) {
                            FocusState.Status.SUCCESS
                        } else {
                            FocusState.Status.FAILURE
                        }
                    } catch (_: CameraControl.OperationCanceledException) {
                        // New calls to startFocusAndMetering and switching the camera will cancel
                        // the previous focus and metering request.
                        FocusState.Status.CANCELLED
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
