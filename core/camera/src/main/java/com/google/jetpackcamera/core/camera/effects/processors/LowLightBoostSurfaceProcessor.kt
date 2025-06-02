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

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.util.Log
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import com.google.android.gms.cameralowlight.LowLightBoost
import com.google.android.gms.cameralowlight.LowLightBoostCallback
import com.google.android.gms.cameralowlight.LowLightBoostClient
import com.google.android.gms.cameralowlight.LowLightBoostOptions
import com.google.android.gms.cameralowlight.LowLightBoostSession
import com.google.android.gms.common.api.Status
import com.google.jetpackcamera.core.camera.effects.RenderCallbacks
import com.google.jetpackcamera.core.camera.effects.ShaderCopy
import com.google.jetpackcamera.core.camera.effects.SurfaceOutputScope
import com.google.jetpackcamera.core.camera.effects.SurfaceRequestScope
import com.google.jetpackcamera.core.common.RefCounted
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TIMESTAMP_UNINITIALIZED = -1L

/**
 * This is a [SurfaceProcessor] that passes an input surface to Google Low Light Boost.
 */
class LowLightBoostSurfaceProcessor(cameraId: String, lowLightBoostClient: LowLightBoostClient, coroutineScope: CoroutineScope) : SurfaceProcessor {

    private val outputSurfaceFlow = MutableStateFlow<SurfaceOutputScope?>(null)
    private lateinit var lowLightBoostOptions: LowLightBoostOptions
    private var lowLightBoostSession: LowLightBoostSession? = null

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

                        lowLightBoostOptions = LowLightBoostOptions(
                            outputSurface,
                            cameraId,
                            size.width,
                            size.height,
                            true
                        )

                        try {
                            lowLightBoostSession = lowLightBoostClient
                                .createSession(lowLightBoostOptions, createLowLightBoostCallback()).await()
                        } catch (e: Exception) {
                            lowLightBoostSession = null
                        }
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

    override fun onInputSurface(request: SurfaceRequest) {
        // No-op, surface request not needed
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        val newScope = SurfaceOutputScope(surfaceOutput)
        outputSurfaceFlow.update { old ->
            old?.cancel("New SurfaceOutput received.")
            newScope
        }
    }
}





