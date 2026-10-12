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

import android.hardware.camera2.TotalCaptureResult
import android.os.Build
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import com.google.android.gms.cameralowlight.LowLightBoostCallback
import com.google.android.gms.cameralowlight.LowLightBoostClient
import com.google.android.gms.cameralowlight.LowLightBoostOptions
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.android.gms.cameralowlight.SceneDetectorCallback
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Status
import kotlin.coroutines.resume
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "PsLlbProcessor"

@RequiresApi(Build.VERSION_CODES.R)
internal class PlayServicesLowLightBoostSurfaceProcessor(
    private val cameraId: String,
    private val lowLightBoostClient: LowLightBoostClient,
    private val captureResults: StateFlow<TotalCaptureResult?>,
    coroutineScope: CoroutineScope,
    private val sceneDetectorCallback: SceneDetectorCallback?,
    private val onLowLightBoostErrorCallback: (Exception) -> Unit = {}
) : SurfaceProcessor {

    private val inputSurfaceRequests =
        Channel<SurfaceRequest>(Channel.BUFFERED, onBufferOverflow = BufferOverflow.DROP_OLDEST) {
            Log.w(
                TAG,
                "SurfaceRequest dropped. Channel is closed or new SurfaceRequest received: $it"
            )
            it.willNotProvideSurface()
        }
    private val outputSurfaces =
        Channel<SurfaceOutput>(Channel.BUFFERED, onBufferOverflow = BufferOverflow.DROP_OLDEST) {
            Log.w(
                TAG,
                "SurfaceOutput dropped. Channel is closed or new SurfaceOutput received: $it"
            )
            it.close()
        }

    init {
        coroutineScope.launch { runLowLightBoostSession() }.invokeOnCompletion {
            inputSurfaceRequests.close()
            outputSurfaces.close()
        }
    }

    private suspend fun runLowLightBoostSession() {
        for (outputSurface in outputSurfaces) {
            val outputSurfaceEvents = Channel<SurfaceOutput.Event>(Channel.UNLIMITED)
            val surface = outputSurface.getSurface(Runnable::run) { event ->
                outputSurfaceEvents.trySendOrLog(event)
            }
            coroutineScope {
                try {
                    runLowLightBoostSessionWithOutput(surface)
                } finally {
                    withContext(NonCancellable) {
                        // The camera owns the output surface until it requests a close.
                        for (event in outputSurfaceEvents) {
                            if (event.eventCode == SurfaceOutput.Event.EVENT_REQUEST_CLOSE) break
                        }
                        outputSurface.close()
                    }
                }
            }
        }
    }

    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        Log.d(
            TAG,
            "New InputSurface received: $surfaceRequest, resolution: ${surfaceRequest.resolution}"
        )
        val result = inputSurfaceRequests.trySend(surfaceRequest)
        if (result.isFailure) {
            Log.w(TAG, "SurfaceRequest dropped. Channel is closed: $surfaceRequest")
            surfaceRequest.willNotProvideSurface()
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        Log.d(TAG, "New OutputSurface received: $surfaceOutput, resolution: ${surfaceOutput.size}")
        val result = outputSurfaces.trySend(surfaceOutput)
        if (result.isFailure) {
            Log.w(TAG, "SurfaceOutput dropped. Channel is closed: $surfaceOutput")
            surfaceOutput.close()
        }
    }

    private suspend fun runLowLightBoostSessionWithOutput(outputSurfaceForLlb: Surface) {
        for (currentRequest in inputSurfaceRequests) {
            handleSurfaceRequest(currentRequest, outputSurfaceForLlb)
        }
    }

    private suspend fun handleSurfaceRequest(
        surfaceRequest: SurfaceRequest,
        outputSurfaceForLlb: Surface
    ) {
        Log.d(TAG, "Creating new LowLightBoostSession for $surfaceRequest")
        try {
            val llbOptions = createLlbOptions(surfaceRequest, outputSurfaceForLlb)
            val sessionEvents = Channel<SessionEvent>(Channel.UNLIMITED)
            val callback = createLlbCallback(sessionEvents)

            // Must wrap the await() call in a NonCancellable context so we always
            // release the session, even if the coroutine is cancelled.
            val lowLightBoostSession = withContext(NonCancellable) {
                lowLightBoostClient.createSession(llbOptions, callback).await()
            }

            lowLightBoostSession?.let { session ->
                useLowLightBoostSession(session, surfaceRequest, sessionEvents)
            }
        } catch (_: CancellationException) {
            // Don't call error callback on cancellation, but ensure cancellation is propagated
            currentCoroutineContext().ensureActive()
        } catch (e: Exception) {
            // In case there was an error, make sure the SurfaceRequest is completed
            surfaceRequest.willNotProvideSurface()
            handleLowLightBoostError(e)
        }
    }

    private suspend fun useLowLightBoostSession(
        lowLightBoostSession: LowLightBoostSession,
        surfaceRequest: SurfaceRequest,
        sessionEvents: ReceiveChannel<SessionEvent>
    ) = coroutineScope {
        Log.d(
            TAG,
            "LowLightBoostSession created: 0x${lowLightBoostSession.hashCode().toString(16)}"
        )
        try {
            sceneDetectorCallback?.let { cb ->
                lowLightBoostSession.setSceneDetectorCallback(cb, null)
            }

            val llbInputSurface = lowLightBoostSession.getCameraSurface()

            launch {
                captureResults.filterNotNull().collectLatest {
                    lowLightBoostSession.processCaptureResult(it)
                }
            }

            // Must wait for camera to be finished with surface before releasing. Don't allow
            // cancellation of this coroutine here.
            withContext(NonCancellable) {
                val result = surfaceRequest.provideSurfaceAndWaitForCompletion(llbInputSurface)
                if (result.resultCode != SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY) {
                    Log.w(TAG, "LLB input surface not used successfully: $result")
                }
                llbInputSurface.release()
            }
        } finally {
            lowLightBoostSession.release()
            // Must wait for session to be destroyed before continuing so more sessions aren't
            // created while this one is still active.
            withContext(NonCancellable) {
                // The callback closes the channel after the session's final event.
                for (event in sessionEvents) {
                    Log.d(TAG, "LLB session event: $event")
                }
            }
            Log.d(
                TAG,
                "LowLightBoostSession released: 0x${lowLightBoostSession.hashCode().toString(16)}"
            )
        }
    }

    private fun createLlbOptions(
        surfaceRequest: SurfaceRequest,
        outputSurfaceForLlb: Surface
    ): LowLightBoostOptions = LowLightBoostOptions(
        outputSurfaceForLlb,
        cameraId,
        surfaceRequest.resolution.width,
        surfaceRequest.resolution.height,
        true
    )

    private fun createLlbCallback(sessionEvents: SendChannel<SessionEvent>): LowLightBoostCallback =
        object : LowLightBoostCallback {
            override fun onSessionDestroyed() {
                sessionEvents.trySendOrLog(SessionEvent.Destroyed)
                sessionEvents.close()
            }

            override fun onSessionDisconnected(status: Status) {
                Log.d(TAG, "LLB session disconnected: $status")
                // Report immediately: the session can disconnect while the camera is still using
                // its input surface, long before the session's events are drained during cleanup.
                onLowLightBoostErrorCallback(RuntimeException(status.statusMessage))
                sessionEvents.trySendOrLog(SessionEvent.Disconnected(status))
                sessionEvents.close()
            }
        }

    /** Events reported by a [LowLightBoostSession] through its [LowLightBoostCallback]. */
    private sealed interface SessionEvent {
        /** The session was destroyed after [LowLightBoostSession.release]. Terminal. */
        data object Destroyed : SessionEvent

        /** The session lost its connection to the service. Terminal. */
        data class Disconnected(val status: Status) : SessionEvent
    }

    private fun handleLowLightBoostError(e: Exception) {
        when (e) {
            is ApiException ->
                Log.e(
                    TAG,
                    "Google Play Services module for Low Light Boost is not " +
                        "available on this device. This might be due to the version of " +
                        "Google Play Services being too old.",
                    e
                )
            else ->
                Log.e(
                    TAG,
                    "Failed to create LowLightBoostSession or provide surface",
                    e
                )
        }
        onLowLightBoostErrorCallback(e)
    }
}

/** Sends [event] without suspending, logging it if the channel is already closed. */
private fun <E> SendChannel<E>.trySendOrLog(event: E) {
    if (trySend(event).isFailure) {
        Log.w(TAG, "Dropped $event: channel is closed")
    }
}

private suspend fun SurfaceRequest.provideSurfaceAndWaitForCompletion(
    surface: Surface
): SurfaceRequest.Result = suspendCancellableCoroutine { continuation ->
    provideSurface(surface, Runnable::run) { continuation.resume(it) }

    continuation.invokeOnCancellation {
        assert(false) {
            "provideSurfaceAndWaitForCompletion should always be called in a " +
                "NonCancellable context to ensure the Surface is not closed before the " +
                "frame source has finished using it."
        }
    }
}
