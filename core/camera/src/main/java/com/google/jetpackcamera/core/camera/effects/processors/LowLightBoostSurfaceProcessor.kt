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
package com.google.jetpackcamera.core.camera.effects.processors

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
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private const val TAG = "LowLightBoostProcessor"

/**
 * This is a [SurfaceProcessor] that passes an input surface to Google Low Light Boost.
 */
@RequiresApi(Build.VERSION_CODES.R)
class LowLightBoostSurfaceProcessor(
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
            val outputSurfaceCompleter = CompletableDeferred<Unit>()
            val surface = outputSurface.getSurface(Runnable::run) {
                outputSurfaceCompleter.complete(Unit)
            }
            coroutineScope {
                try {
                    runLowLightBoostSessionWithOutput(surface)
                } finally {
                    withContext(NonCancellable) {
                        outputSurfaceCompleter.await()
                        outputSurface.close()
                    }
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
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

    @RequiresApi(Build.VERSION_CODES.R)
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
            val llbSessionComplete = CompletableDeferred<Unit>()
            val callback = createLlbCallback(llbSessionComplete)

            // Must wrap the await() call in a NonCancellable context so we always
            // release the session, even if the coroutine is cancelled.
            val lowLightBoostSession = withContext(NonCancellable) {
                lowLightBoostClient.createSession(llbOptions, callback).await()
            }

            lowLightBoostSession?.let { session ->
                useLowLightBoostSession(session, surfaceRequest, llbSessionComplete)
            }
        } catch (e: Exception) {
            handleLowLightBoostError(e)
            // In case there was an error, make sure the SurfaceRequest is completed
            surfaceRequest.willNotProvideSurface()
        }
    }

    private suspend fun useLowLightBoostSession(
        lowLightBoostSession: LowLightBoostSession,
        surfaceRequest: SurfaceRequest,
        llbSessionComplete: CompletableDeferred<Unit>
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
                surfaceRequest.provideSurfaceAndWaitForCompletion(llbInputSurface)
                llbInputSurface.release()
            }
        } finally {
            lowLightBoostSession.release()
            // Must wait for session to be destroyed before continuing so more sessions aren't
            // created while this one is still active.
            withContext(NonCancellable) {
                llbSessionComplete.await()
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

    private fun createLlbCallback(
        llbSessionComplete: CompletableDeferred<Unit>
    ): LowLightBoostCallback = object : LowLightBoostCallback {
        override fun onSessionDestroyed() {
            Log.d(TAG, "LLB session destroyed")
            llbSessionComplete.complete(Unit)
        }

        override fun onSessionDisconnected(status: Status) {
            Log.d(TAG, "LLB session disconnected: $status")
            onLowLightBoostErrorCallback(RuntimeException(status.statusMessage))
            llbSessionComplete.complete(Unit)
        }
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

            is CancellationException -> return // Don't call error callback on cancellation
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
