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

import android.annotation.SuppressLint
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
import com.google.jetpackcamera.core.camera.effects.SurfaceOutputScope
import com.google.jetpackcamera.core.camera.effects.SurfaceRequestScope
import com.google.jetpackcamera.model.LowLightBoostSessionState
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "LowLightBoostProcessor"

/**
 * This is a [SurfaceProcessor] that passes an input surface to Google Low Light Boost.
 */
@RequiresApi(Build.VERSION_CODES.R)
class LowLightBoostSurfaceProcessor(
    private val cameraId: String,
    private val lowLightBoostClient: LowLightBoostClient,
    private val coroutineScope: CoroutineScope,
    private val sceneDetectorCallback: SceneDetectorCallback?,
    private val lowLightBoostSessionState: MutableStateFlow<LowLightBoostSessionState>,
    private val onLowLightBoostErrorCallback: (Exception) -> Unit
) : SurfaceProcessor {

    private val outputSurfaceFlow = MutableStateFlow<SurfaceOutputScope?>(null)
    private var lowLightBoostSession: LowLightBoostSession? = null
    private var inputSurfaceRequestScope: SurfaceRequestScope? = null

    // Executor for provideSurface callback
    private val glExecutor = Executors.newSingleThreadExecutor()

    init {
        coroutineScope.launch {
            lowLightBoostSessionState.collect { state ->
                if (state is LowLightBoostSessionState.Processing) {
                    lowLightBoostSession?.processCaptureResult(state.result)
                }
            }
        }

        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            outputSurfaceFlow
                .filterNotNull()
                .collectLatest { surfaceOutputScope ->
                    surfaceOutputScope.withSurfaceOutput { refCountedSurface,
                                                           size,
                                                           updateTransformMatrix ->
                        // If we can't acquire the surface, then the surface output is already
                        // closed, so we'll return and wait for the next output surface.
                        val outputSurface =
                            refCountedSurface.acquire() ?: return@withSurfaceOutput

                        tryCreateLowLightBoostSession(outputSurface)
                    }
                }
        }
    }

    private fun createLowLightBoostCallback(): LowLightBoostCallback =
        object : LowLightBoostCallback {
            override fun onSessionDestroyed() {
                Log.d(TAG, "LLB session destroyed")
                lowLightBoostSessionState.update { LowLightBoostSessionState.Released }
            }

            override fun onSessionDisconnected(status: Status) {
                Log.d(TAG, "LLB session disconnected: $status")
                releaseLowLightBoostSession()
                lowLightBoostSessionState.update { LowLightBoostSessionState.Released }
                onLowLightBoostErrorCallback(RuntimeException(status.statusMessage))
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onInputSurface(request: SurfaceRequest) {
        // Cancel any previous request handling
        inputSurfaceRequestScope?.cancel("New SurfaceRequest received.")
        val newScope = SurfaceRequestScope(request) // Helper to manage request lifecycle
        inputSurfaceRequestScope = newScope

        outputSurfaceFlow.value?.surfaceOutput?.let { outputSurface ->
            coroutineScope.launch {
                val surface = outputSurface.getSurface(glExecutor, {})
                tryCreateLowLightBoostSession(surface)
            }
        }

        // Clean up when the request is released (e.g., camera is closed)
        request.addRequestCancellationListener(glExecutor) { // Replace context if not available
            Log.d(TAG, "InputSurfaceRequest cancelled: $request")
            inputSurfaceRequestScope?.cancel("SurfaceRequest cancelled by CameraX.")
            inputSurfaceRequestScope = null
            releaseLowLightBoostSession()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun tryCreateLowLightBoostSession(outputSurfaceForLlb: Surface) {
        inputSurfaceRequestScope?.withSurfaceRequest { currentInputRequest ->

            try {
                // Create LowLightBoostOptions with the output surface for LLB
                // and dimensions from the input SurfaceRequest.
                val llbOptions = LowLightBoostOptions(
                    outputSurfaceForLlb, // This is where LLB writes its output
                    cameraId,
                    currentInputRequest.resolution.width,
                    currentInputRequest.resolution.height,
                    true
                )

                if (lowLightBoostSession != null) {
                    releaseLowLightBoostSession()
                }

                lowLightBoostSession = lowLightBoostClient
                    .createSession(llbOptions, createLowLightBoostCallback())
                    .await()

                lowLightBoostSessionState.update { LowLightBoostSessionState.Ready }

                sceneDetectorCallback.let { cb ->
                    lowLightBoostSession?.setSceneDetectorCallback(cb, null)
                }

                // Get the input surface from the LowLightBoostSession
                val llbInputSurface = lowLightBoostSession?.getCameraSurface()
                    ?: throw IllegalStateException(
                        "LowLightBoostSession did not provide an input surface."
                    )

                Log.d(TAG, "LLB Session created. Providing LLB input surface to CameraX.")

                // Fulfill the CameraX SurfaceRequest with LLB's input surface
                currentInputRequest.provideSurface(llbInputSurface, glExecutor) { result ->
                    Log.d(TAG, "CameraX SurfaceRequest result: ${result.resultCode}")
                    llbInputSurface.release()
                    releaseLowLightBoostSession()
                    when (result.resultCode) {
                        SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY -> {
                            Log.i(TAG, "CameraX used LLB input surface.")
                        }

                        SurfaceRequest.Result.RESULT_REQUEST_CANCELLED -> {
                            Log.e(TAG, "SurfaceRequest cancelled: ${result.resultCode}")
                        }

                        else -> {
                            Log.e(TAG, "SurfaceRequest failed: ${result.resultCode}")
                        }
                    }
                }
            } catch (e: Exception) {
                when (e) {
                    is ApiException ->
                        Log.e(TAG, "Google Play Services module for Low Light Boost is not " +
                                "available on this device. This might be due to the version of " +
                                "Google Play Services being too old.", e)
                    else ->
                        Log.e(TAG, "Failed to create LowLightBoostSession or provide surface", e)
                }

                onLowLightBoostErrorCallback(e)

                // Signal error to CameraX for the input request if it hasn't been fulfilled yet.
                currentInputRequest.willNotProvideSurface()
                releaseLowLightBoostSession()
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        Log.d(TAG, "New OutputSurface received: $surfaceOutput, resolution: ${surfaceOutput.size}")
        val newScope = SurfaceOutputScope(surfaceOutput)
        outputSurfaceFlow.update { old ->
            old?.cancel("New SurfaceOutput received.")
            newScope
        }
    }

    @SuppressLint("NewApi")
    fun releaseLowLightBoostSession() {
        Log.d(TAG, "Releasing LLB session")
        lowLightBoostSession?.release()
        lowLightBoostSession = null
    }
}
