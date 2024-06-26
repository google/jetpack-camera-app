/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.domain.camera.effects

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import com.google.jetpackcamera.core.common.RefCounted
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val TIMESTAMP_UNINITIALIZED = -1L

/**
 * This is a [SurfaceProcessor] that passes on the same content from the input
 * surface to the output surface. Used to make a copies of surfaces.
 */
class CopyingSurfaceProcessor(coroutineScope: CoroutineScope) : SurfaceProcessor {

    private val inputSurfaceFlow = MutableStateFlow<SurfaceRequestScope?>(null)
    private val outputSurfaceFlow = MutableStateFlow<SurfaceOutputScope?>(null)

    init {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            inputSurfaceFlow
                .filterNotNull()
                .collectLatest { surfaceRequestScope ->
                    surfaceRequestScope.withSurfaceRequest { surfaceRequest ->

                        val renderCallbacks = ShaderCopy(surfaceRequest.dynamicRange)
                        renderCallbacks.renderWithSurfaceRequest(surfaceRequest)
                    }
                }
        }
    }

    private suspend fun RenderCallbacks.renderWithSurfaceRequest(surfaceRequest: SurfaceRequest) =
        coroutineScope inputScope@{
            var currentTimestamp = TIMESTAMP_UNINITIALIZED
            val surfaceTextureRef = RefCounted<SurfaceTexture> {
                it.release()
            }
            val textureTransform = FloatArray(16)

            val frameUpdateFlow = MutableStateFlow(0)

            val initializeCallback = object : GLRenderer.EGLContextCallback {

                override fun onEGLContextCreated(eglManager: EGLManager) {
                    initRenderer()

                    val surfaceTex = createSurfaceTexture(
                        surfaceRequest.resolution.width,
                        surfaceRequest.resolution.height
                    )

                    // Initialize the reference counted surface texture
                    surfaceTextureRef.initialize(surfaceTex)

                    surfaceTex.setOnFrameAvailableListener {
                        // Increment frame counter
                        frameUpdateFlow.update { it + 1 }
                    }

                    val inputSurface = Surface(surfaceTex)
                    surfaceRequest.provideSurface(inputSurface, Runnable::run) { result ->
                        inputSurface.release()
                        surfaceTextureRef.release()
                        this@inputScope.cancel(
                            "Input surface no longer receiving frames: $result"
                        )
                    }
                }

                override fun onEGLContextDestroyed(eglManager: EGLManager) {
                    // no-op
                }
            }

            val glRenderer = GLRenderer(
                eglSpecFactory = provideEGLSpec,
                eglConfigFactory = initConfig
            )
            glRenderer.registerEGLContextCallback(initializeCallback)
            glRenderer.start(glThreadName)

            val inputRenderTarget = glRenderer.createRenderTarget(
                surfaceRequest.resolution.width,
                surfaceRequest.resolution.height,
                object : GLRenderer.RenderCallback {

                    override fun onDrawFrame(eglManager: EGLManager) {
                        surfaceTextureRef.acquire()?.also {
                            try {
                                currentTimestamp =
                                    if (currentTimestamp == TIMESTAMP_UNINITIALIZED) {
                                        // Don't perform any updates on first draw,
                                        // we're only setting up the context.
                                        0
                                    } else {
                                        it.updateTexImage()
                                        it.getTransformMatrix(textureTransform)
                                        it.timestamp
                                    }
                            } finally {
                                surfaceTextureRef.release()
                            }
                        }
                    }
                }
            )

            // Create the context and initialize the input. This will call RenderTarget.onDrawFrame,
            // but we won't actually update the frame since this triggers adding the frame callback.
            // All subsequent updates will then happen through frameUpdateFlow.
            // This should be updated when https://issuetracker.google.com/331968279 is resolved.
            inputRenderTarget.requestRender()

            // Connect the onConnectToInput callback with the onDisconnectFromInput
            // Should only be called on worker thread
            var connectedToInput = false

            // Should only be called on worker thread
            val onConnectToInput: () -> Boolean = {
                connectedToInput = surfaceTextureRef.acquire() != null
                connectedToInput
            }

            // Should only be called on worker thread
            val onDisconnectFromInput: () -> Unit = {
                if (connectedToInput) {
                    surfaceTextureRef.release()
                    connectedToInput = false
                }
            }

            // Wait for output surfaces
            outputSurfaceFlow
                .onCompletion {
                    glRenderer.stop(cancelPending = false)
                    glRenderer.unregisterEGLContextCallback(initializeCallback)
                }.filterNotNull()
                .collectLatest { surfaceOutputScope ->
                    surfaceOutputScope.withSurfaceOutput { refCountedSurface,
                                                           size,
                                                           updateTransformMatrix ->
                        // If we can't acquire the surface, then the surface output is already
                        // closed, so we'll return and wait for the next output surface.
                        val outputSurface =
                            refCountedSurface.acquire() ?: return@withSurfaceOutput

                        val surfaceTransform = FloatArray(16)
                        val outputRenderTarget = glRenderer.attach(
                            outputSurface,
                            size.width,
                            size.height,
                            object : GLRenderer.RenderCallback {

                                override fun onSurfaceCreated(
                                    spec: EGLSpec,
                                    config: EGLConfig,
                                    surface: Surface,
                                    width: Int,
                                    height: Int
                                ): EGLSurface? {
                                    return if (onConnectToInput()) {
                                        createOutputSurface(spec, config, surface, width, height)
                                    } else {
                                        null
                                    }
                                }

                                override fun onDrawFrame(eglManager: EGLManager) {
                                    val currentDrawSurface = eglManager.currentDrawSurface
                                    if (currentDrawSurface != eglManager.defaultSurface) {
                                        updateTransformMatrix(
                                            surfaceTransform,
                                            textureTransform
                                        )

                                        drawFrame(
                                            size.width,
                                            size.height,
                                            surfaceTransform
                                        )

                                        // Set timestamp
                                        val display =
                                            EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
                                        EGLExt.eglPresentationTimeANDROID(
                                            display,
                                            eglManager.currentDrawSurface,
                                            currentTimestamp
                                        )
                                    }
                                }
                            }
                        )

                        frameUpdateFlow
                            .onCompletion {
                                outputRenderTarget.detach(cancelPending = false) {
                                    onDisconnectFromInput()
                                    refCountedSurface.release()
                                }
                            }.filterNot { it == 0 } // Don't attempt render on frame count 0
                            .collectLatest {
                                inputRenderTarget.requestRender()
                                outputRenderTarget.requestRender()
                            }
                    }
                }
        }

    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        val newScope = SurfaceRequestScope(surfaceRequest)
        inputSurfaceFlow.update { old ->
            old?.cancel("New SurfaceRequest received.")
            newScope
        }
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        val newScope = SurfaceOutputScope(surfaceOutput)
        outputSurfaceFlow.update { old ->
            old?.cancel("New SurfaceOutput received.")
            newScope
        }
    }
}

interface RenderCallbacks {
    val glThreadName: String
    val provideEGLSpec: () -> EGLSpec
    val initConfig: EGLManager.() -> EGLConfig
    val initRenderer: () -> Unit
    val createSurfaceTexture: (width: Int, height: Int) -> SurfaceTexture
    val createOutputSurface: (
        eglSpec: EGLSpec,
        config: EGLConfig,
        surface: Surface,
        width: Int,
        height: Int
    ) -> EGLSurface
    val drawFrame: (outputWidth: Int, outputHeight: Int, surfaceTransform: FloatArray) -> Unit
}

private class SurfaceOutputScope(val surfaceOutput: SurfaceOutput) {
    private val surfaceLifecycleJob = SupervisorJob()
    private val refCountedSurface = RefCounted<Surface>(onRelease = {
        surfaceOutput.close()
    }).apply {
        // Ensure we don't release until after `initialize` has completed by deferring
        // the release.
        val deferredRelease = CompletableDeferred<Unit>()
        initialize(
            surfaceOutput.getSurface(Runnable::run) {
                deferredRelease.complete(Unit)
            }
        )
        CoroutineScope(Dispatchers.Unconfined).launch {
            deferredRelease.await()
            surfaceLifecycleJob.cancel("SurfaceOutput close requested.")
            this@apply.release()
        }
    }

    suspend fun <R> withSurfaceOutput(
        block: suspend CoroutineScope.(
            surface: RefCounted<Surface>,
            surfaceSize: Size,
            updateTransformMatrix: (updated: FloatArray, original: FloatArray) -> Unit
        ) -> R
    ): R {
        return CoroutineScope(coroutineContext + Job(surfaceLifecycleJob)).async(
            start = CoroutineStart.UNDISPATCHED
        ) {
            ensureActive()
            block(
                refCountedSurface,
                surfaceOutput.size,
                surfaceOutput::updateTransformMatrix
            )
        }.await()
    }

    fun cancel(message: String? = null) {
        message?.apply { surfaceLifecycleJob.cancel(message) } ?: surfaceLifecycleJob.cancel()
    }
}

private class SurfaceRequestScope(private val surfaceRequest: SurfaceRequest) {
    private val requestLifecycleJob = SupervisorJob()

    init {
        surfaceRequest.addRequestCancellationListener(Runnable::run) {
            requestLifecycleJob.cancel("SurfaceRequest cancelled.")
        }
    }

    suspend fun <R> withSurfaceRequest(
        block: suspend CoroutineScope.(
            surfaceRequest: SurfaceRequest
        ) -> R
    ): R {
        return CoroutineScope(coroutineContext + Job(requestLifecycleJob)).async(
            start = CoroutineStart.UNDISPATCHED
        ) {
            ensureActive()
            block(surfaceRequest)
        }.await()
    }

    fun cancel(message: String? = null) {
        message?.apply { requestLifecycleJob.cancel(message) } ?: requestLifecycleJob.cancel()
        // Attempt to tell frame producer we will not provide a surface. This may fail (silently)
        // if surface was already provided or the producer has cancelled the request, in which
        // case we don't have to do anything.
        surfaceRequest.willNotProvideSurface()
    }
}
