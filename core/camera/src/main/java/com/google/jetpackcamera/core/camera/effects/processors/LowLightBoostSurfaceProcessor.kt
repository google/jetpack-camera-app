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
import com.google.android.gms.common.api.Status
import com.google.jetpackcamera.core.camera.effects.SurfaceOutputScope
import com.google.jetpackcamera.core.camera.effects.SurfaceRequestScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.Executors

/**
 * This is a [SurfaceProcessor] that passes an input surface to Google Low Light Boost.
 */
@RequiresApi(Build.VERSION_CODES.R)
class LowLightBoostSurfaceProcessor(
    private val cameraId: String,
    private val lowLightBoostClient: LowLightBoostClient,
    private val coroutineScope: CoroutineScope
) : SurfaceProcessor {

    private val outputSurfaceFlow = MutableStateFlow<SurfaceOutputScope?>(null)
    private var lowLightBoostSession: LowLightBoostSession? = null
    private var inputSurfaceRequestScope: SurfaceRequestScope? = null

    // Executor for provideSurface callback
    private val glExecutor = Executors.newSingleThreadExecutor()

    init {
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
                lowLightBoostSession = null
            }

            override fun onSessionDisconnected(status: Status) {
                lowLightBoostSession = null
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
            Log.d("LowLightBoostProcessor", "InputSurfaceRequest cancelled: $request")
            inputSurfaceRequestScope?.cancel("SurfaceRequest cancelled by CameraX.")
            inputSurfaceRequestScope = null
            releaseLowLightBoostSession()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun tryCreateLowLightBoostSession(outputSurfaceForLlb: Surface) {
        inputSurfaceRequestScope?.withSurfaceRequest { currentInputRequest ->

            coroutineScope.launch { // Launch in your processor's scope
                try{
                    // 1. Create LowLightBoostOptions with the OUTPUT surface for LLB
                    //    and dimensions from the INPUT SurfaceRequest.
                    val llbOptions = LowLightBoostOptions(
                        outputSurfaceForLlb, // This is where LLB writes its output
                        cameraId,
                        currentInputRequest.resolution.width,
                        currentInputRequest.resolution.height,
                        true
                    )

                    // 2. Create the LowLightBoostSession
                    lowLightBoostSession = lowLightBoostClient
                        .createSession(llbOptions, createLowLightBoostCallback())
                        .await()

                    // 3. Get the INPUT surface FROM the LowLightBoostSession
                    //    (The surface CameraX should render TO for LLB processing)
                    //    The exact method name might differ, e.g., getProcessingSurface, getInputSurface
                    val llbInputSurface = lowLightBoostSession?.getCameraSurface()
                        ?: throw IllegalStateException("LowLightBoostSession did not provide an input surface.")


                    Log.d(
                        "LowLightBoostProcessor",
                        "LLB Session created. Providing LLB input surface to CameraX."
                    )

                    // 4. Fulfill the CameraX SurfaceRequest with LLB's input surface
                    currentInputRequest.provideSurface(llbInputSurface, glExecutor) { result ->
                        Log.d(
                            "LowLightBoostProcessor",
                            "CameraX SurfaceRequest result: ${result.resultCode}"
                        )
                        when (result.resultCode) {
                            SurfaceRequest.Result.RESULT_SURFACE_USED_SUCCESSFULLY -> {
                                Log.i(
                                    "LowLightBoostProcessor",
                                    "CameraX is using LLB input surface."
                                )
                            }

                            SurfaceRequest.Result.RESULT_REQUEST_CANCELLED -> {
                                // Should have been handled by addRequestCancellationListener
                            }

                            else -> {
                                Log.e(
                                    "LowLightBoostProcessor",
                                    "SurfaceRequest failed: ${result.resultCode}"
                                )
                                // Potentially release LLB session if CameraX won't use the surface
                                releaseLowLightBoostSession()
                            }
                        }
                    }

                } catch (e: Exception) {
                    Log.e(
                        "LowLightBoostProcessor",
                        "Failed to create LowLightBoostSession or provide surface",
                        e
                    )
                    // Signal error to CameraX for the input request if it hasn't been fulfilled yet.
                    currentInputRequest.willNotProvideSurface()
                    releaseLowLightBoostSession()
                }
            }
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        Log.d("LowLightBoostProcessor", "New OutputSurface received: $surfaceOutput, resolution: ${surfaceOutput.size}")
        val newScope = SurfaceOutputScope(surfaceOutput)
        outputSurfaceFlow.update { old ->
            old?.cancel("New SurfaceOutput received.")
            newScope
        }
    }

    @SuppressLint("NewApi")
    fun releaseLowLightBoostSession() {
        lowLightBoostSession?.release()
        lowLightBoostSession = null
    }
}
