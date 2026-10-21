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
package com.google.jetpackcamera.ui.controller.impl

import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.ui.controller.ScreenFlashController
import com.google.jetpackcamera.ui.uistate.capture.ScreenFlashUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.job
import kotlinx.coroutines.launch

/**
 * Default minimum time between receiving a screen flash APPLY_UI event and reporting its
 * completion. This matches the delay used by CameraX's own screen flash UI implementations
 * (`ScreenFlashView` and `CameraXViewfinder`), based on measured brightness ramp times of about a
 * second across devices.
 */
private const val DEFAULT_MIN_APPLY_UI_DURATION_MILLIS = 1000L

/**
 * Implementation of [ScreenFlashController] that handles screen flash actions.
 *
 * When screen flash is applied, the UI maximizes the screen brightness. The display system applies
 * brightness changes asynchronously and may ramp them gradually (taking more than a second on some
 * devices), and there is no public API to know when the change has completed. To keep the camera
 * from metering and capturing before the screen is bright, completion of an
 * [CameraSystem.ScreenFlashEvent.Type.APPLY_UI] event is reported only after both the UI change has
 * completed and [minApplyUiDurationMillis] has elapsed since the event was received.
 *
 * @param cameraSystemProvider Provider for the initialized [CameraSystem].
 * @param trackedCaptureUiState The [MutableStateFlow] of [TrackedCaptureUiState] to update the flash state.
 * @param coroutineContext The [CoroutineContext] for launching coroutines.
 * @param minApplyUiDurationMillis The minimum time, in milliseconds, between receiving an
 *   [CameraSystem.ScreenFlashEvent.Type.APPLY_UI] event and reporting its completion.
 */
class ScreenFlashControllerImpl(
    private val cameraSystemProvider: suspend () -> CameraSystem,
    private val trackedCaptureUiState: MutableStateFlow<TrackedCaptureUiState>,
    coroutineContext: CoroutineContext,
    private val minApplyUiDurationMillis: Long = DEFAULT_MIN_APPLY_UI_DURATION_MILLIS
) : ScreenFlashController {
    private val job = Job(parent = coroutineContext[Job.Key])
    private val scope = CoroutineScope(coroutineContext + job)

    /** Reports completion of the latest APPLY_UI event. Cancelled when a newer event arrives. */
    private var pendingApplyUiCompletion: Job? = null

    init {
        scope.launch {
            for (event in cameraSystemProvider().getScreenFlashEvents()) {
                pendingApplyUiCompletion?.cancel()
                pendingApplyUiCompletion = null
                // Created outside of update {}, which may invoke its block more than once.
                val onChangeComplete: () -> Unit = when (event.type) {
                    CameraSystem.ScreenFlashEvent.Type.APPLY_UI ->
                        startApplyUiCompletion(event.onComplete)

                    CameraSystem.ScreenFlashEvent.Type.CLEAR_UI -> {
                        {
                            event.onComplete()
                            // reset ui state on CLEAR_UI event completion
                            scope.launch {
                                trackedCaptureUiState.update { o ->
                                    o.copy(screenFlashUiState = ScreenFlashUiState())
                                }
                            }
                        }
                    }
                }
                trackedCaptureUiState.update { old ->
                    old.copy(
                        screenFlashUiState = old.screenFlashUiState.copy(
                            enabled = event.type == CameraSystem.ScreenFlashEvent.Type.APPLY_UI,
                            onChangeComplete = onChangeComplete
                        )
                    )
                }
            }
        }
    }

    /**
     * Starts tracking completion of an APPLY_UI event and returns the callback that the UI invokes
     * once its changes are complete.
     *
     * [onComplete] is invoked once the UI changes are complete and [minApplyUiDurationMillis] has
     * elapsed since this method was called, unless a newer screen flash event arrives first.
     */
    private fun startApplyUiCompletion(onComplete: () -> Unit): () -> Unit {
        val uiChangeComplete = CompletableDeferred<Unit>()
        pendingApplyUiCompletion = scope.launch {
            delay(minApplyUiDurationMillis)
            uiChangeComplete.await()
            onComplete()
        }
        return { uiChangeComplete.complete(Unit) }
    }

    override fun setClearUiScreenBrightness(brightness: Float) {
        trackedCaptureUiState.update { old ->
            old.copy(
                screenFlashUiState = old.screenFlashUiState.copy(
                    screenBrightnessToRestore = brightness
                )
            )
        }
    }

    /**
     * Initiates the cancellation of this controller's scope and returns its Job.
     * To wait for cancellation to complete, call .join() on the returned Job.
     */
    fun cancelScope(): Job {
        scope.cancel()
        return scope.coroutineContext.job
    }
}
