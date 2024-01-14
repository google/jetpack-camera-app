/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera.domain.camera

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.opengl.GLES11Ext.GL_TEXTURE_EXTERNAL_OES
import android.opengl.GLES20
import android.util.Log
import android.util.Size
import android.view.Surface
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.graphics.opengl.GLRenderer
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import com.google.jetpackcamera.core.common.RefCounted
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import kotlin.coroutines.coroutineContext


private const val TAG = "EmptySurfaceProcessor"
private const val GL_THREAD_NAME = TAG

private const val VAR_TEXTURE_COORD = "vTextureCoord"
private const val DEFAULT_VERTEX_SHADER =
    """
uniform mat4 uTexMatrix;
attribute vec4 aPosition;
attribute vec4 aTextureCoord;
varying vec2 $VAR_TEXTURE_COORD;
void main() {
    gl_Position = aPosition;
    $VAR_TEXTURE_COORD = (uTexMatrix * aTextureCoord).xy;
}
"""

private const val TEN_BIT_VERTEX_SHADER =
    """#version 300 es
in vec4 aPosition;
in vec4 aTextureCoord;
uniform mat4 uTexMatrix;
out vec2 $VAR_TEXTURE_COORD;
void main() {
  gl_Position = aPosition;
  $VAR_TEXTURE_COORD = (uTexMatrix * aTextureCoord).xy;
}
"""

private const val VAR_TEXTURE = "sTexture"
private const val DEFAULT_FRAGMENT_SHADER =
    """
#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 $VAR_TEXTURE_COORD;
uniform samplerExternalOES $VAR_TEXTURE;
void main() {
    gl_FragColor = texture2D($VAR_TEXTURE, $VAR_TEXTURE_COORD);
}
"""

private const val TEN_BIT_FRAGMENT_SHADER =
    """#version 300 es
#extension GL_EXT_YUV_target : require
precision mediump float;
uniform __samplerExternal2DY2YEXT $VAR_TEXTURE;
in vec2 $VAR_TEXTURE_COORD;
layout (yuv) out vec3 outColor;

void main() {
  outColor = texture($VAR_TEXTURE, $VAR_TEXTURE_COORD).xyz;
}
"""

private const val EGL_GL_COLORSPACE_KHR = 0x309D
private const val EGL_GL_COLORSPACE_BT2020_HLG_EXT = 0x3540

private const val SIZEOF_FLOAT = 4

private const val TIMESTAMP_UNINITIALIZED = -1L

/**
 * This is a [SurfaceProcessor] that passes on the same content from the input
 * surface to the output surface. Used to make a copies of surfaces.
 */
class EmptySurfaceProcessor(coroutineScope: CoroutineScope) : SurfaceProcessor {

    private val inputSurfaceFlow = MutableStateFlow<SurfaceRequestScope?>(null)
    private val outputSurfaceFlow = MutableStateFlow<SurfaceOutputScope?>(null)

    // Called on worker thread only
    private var externalTextureId: Int = -1
    private var programHandle = -1
    private var texMatrixLoc = -1
    private var positionLoc = -1
    private var texCoordLoc = -1

    init {
        coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val textureTransform = FloatArray(16)

            inputSurfaceFlow
                .filterNotNull()
                .collectLatest { surfaceRequestScope ->
                surfaceRequestScope.withSurfaceRequest { surfaceRequest ->

                    val dynamicRange = surfaceRequest.dynamicRange
                    val use10bitPipeline = dynamicRange.bitDepth == DynamicRange.BIT_DEPTH_10_BIT

                    restartWithSurface(
                        surfaceRequest,
                        provideEGLSpec = { if (use10bitPipeline) EGLSpecV14ES3 else EGLSpec.V14 },
                        initConfig = {
                            checkNotNull(
                                loadConfig(
                                    EGLConfigAttributes {
                                        if (use10bitPipeline) {
                                            TEN_BIT_REQUIRED_EGL_EXTENSIONS.forEach {
                                                check(isExtensionSupported(it)) {
                                                    "Required extension for 10-bit HDR is not " +
                                                            "supported: $it"
                                                }
                                            }
                                            include(EGLConfigAttributes.RGBA_1010102)
                                            EGL14.EGL_RENDERABLE_TYPE to
                                                    EGLExt.EGL_OPENGL_ES3_BIT_KHR
                                            EGL14.EGL_SURFACE_TYPE to
                                                    (EGL14.EGL_WINDOW_BIT or EGL14.EGL_PBUFFER_BIT)
                                        } else {
                                            include(EGLConfigAttributes.RGBA_8888)
                                        }
                                    }
                                )) {
                                "Unable to select EGLConfig"
                            }
                        },
                        initRenderer = {
                            createProgram(
                                if (use10bitPipeline)
                                    TEN_BIT_VERTEX_SHADER else DEFAULT_VERTEX_SHADER,
                                if (use10bitPipeline)
                                    TEN_BIT_FRAGMENT_SHADER else DEFAULT_FRAGMENT_SHADER
                            )
                            loadLocations()
                            createTexture()
                            useAndConfigureProgram()
                        },
                        createSurfaceTexture = { texId: Int, width: Int, height: Int ->
                            SurfaceTexture(texId).apply {
                                setDefaultBufferSize(width, height)
                            }
                        },
                        updateTexture = { surfaceTexture ->
                            surfaceTexture.updateTexImage()
                            surfaceTexture.getTransformMatrix(textureTransform)
                            surfaceTexture.timestamp
                        },
                        getTextureTransform = { textureTransform },
                        createOutputSurface = { eglSpec, config, surface, _, _ ->
                            eglSpec.eglCreateWindowSurface(
                                config,
                                surface,
                                EGLConfigAttributes {
                                    if (use10bitPipeline) {
                                        EGL_GL_COLORSPACE_KHR to EGL_GL_COLORSPACE_BT2020_HLG_EXT
                                    }
                                })
                        },
                        drawFrame = {
                                outputWidth: Int,
                                outputHeight: Int,
                                surfaceTransform: FloatArray ->
                            GLES20.glViewport(
                                0,
                                0,
                                outputWidth,
                                outputHeight
                            )
                            GLES20.glScissor(
                                0,
                                0,
                                outputWidth,
                                outputHeight
                            )

                            GLES20.glUniformMatrix4fv(
                                texMatrixLoc,
                                /*count=*/1,
                                /*transpose=*/false,
                                surfaceTransform,
                                /*offset=*/0
                            )
                            checkGlErrorOrThrow("glUniformMatrix4fv")

                            // Draw the rect.
                            GLES20.glDrawArrays(
                                GLES20.GL_TRIANGLE_STRIP,
                                /*firstVertex=*/0,
                                /*vertexCount=*/4
                            )
                            checkGlErrorOrThrow("glDrawArrays")
                        })
                }
            }
        }
    }

    private suspend fun restartWithSurface(
        surfaceRequest: SurfaceRequest,
        provideEGLSpec: () -> EGLSpec,
        initConfig: EGLManager.() -> EGLConfig,
        initRenderer: () -> Unit,
        createSurfaceTexture: (texId: Int, width: Int, height: Int) -> SurfaceTexture,
        updateTexture: (surfaceTexture: SurfaceTexture) -> Long,
        getTextureTransform: () -> FloatArray,
        createOutputSurface:
            (
            eglSpec: EGLSpec,
            config: EGLConfig,
            surface: Surface,
            width: Int,
            height: Int
        ) -> EGLSurface,
        drawFrame: (outputWidth: Int, outputHeight: Int, surfaceTransform: FloatArray) -> Unit
    ) = coroutineScope inputScope@{
        var currentTimestamp = TIMESTAMP_UNINITIALIZED
        val surfaceTextureRef = RefCounted<SurfaceTexture> {
            it.release()
        }
        val frameUpdateFlow = MutableStateFlow(0)

        val initializeCallback = object : GLRenderer.EGLContextCallback {

            override fun onEGLContextCreated(eglManager: EGLManager) {
                initRenderer()

                val surfaceTex = createSurfaceTexture(
                    externalTextureId,
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

        val glRenderer = GLRenderer(eglSpecFactory = provideEGLSpec, eglConfigFactory = initConfig)
        glRenderer.registerEGLContextCallback(initializeCallback)
        glRenderer.start(GL_THREAD_NAME)

        val inputRenderTarget = glRenderer.createRenderTarget(
            surfaceRequest.resolution.width,
            surfaceRequest.resolution.height,
            object : GLRenderer.RenderCallback {

                override fun onDrawFrame(eglManager: EGLManager) {
                    surfaceTextureRef.acquire()?.also {
                        try {
                            currentTimestamp = if (currentTimestamp == TIMESTAMP_UNINITIALIZED) {
                                // Don't perform any updates on first draw, we're only setting up
                                // the context.
                                0
                            } else {
                                updateTexture(it)
                            }
                        } finally {
                            surfaceTextureRef.release()
                        }
                    }
                }
            })

            // Create the context and initialize the input. This will call RenderTarget.onDrawFrame,
            // but we won't actually update the frame since this triggers adding the frame callback.
            // All subsequent updates will then happen through frameUpdateFlow.
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
                                            getTextureTransform()
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
                            }.filterNot { it == 0 }
                            .collectLatest {
                                inputRenderTarget.requestRender()
                                outputRenderTarget.requestRender()
                            }
                    }
                }
        }


    private fun createProgram(vertShader: String, fragShader: String) {
        checkGlThread()
        var vertexShader = -1
        var fragmentShader = -1
        var program = -1
        try {
            fragmentShader = loadShader(
                GLES20.GL_FRAGMENT_SHADER,
                fragShader
            )
            vertexShader = loadShader(
                GLES20.GL_VERTEX_SHADER,
                vertShader
            )
            program = GLES20.glCreateProgram()
            checkGlErrorOrThrow("glCreateProgram")
            GLES20.glAttachShader(program, vertexShader)
            checkGlErrorOrThrow("glAttachShader")
            GLES20.glAttachShader(program, fragmentShader)
            checkGlErrorOrThrow("glAttachShader")
            GLES20.glLinkProgram(program)
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus,  /*offset=*/0)
            check(linkStatus[0] == GLES20.GL_TRUE) {
                "Could not link program: " + GLES20.glGetProgramInfoLog(
                    program
                )
            }
            programHandle = program
        } catch (e: Exception) {
            if (vertexShader != -1) {
                GLES20.glDeleteShader(vertexShader)
            }
            if (fragmentShader != -1) {
                GLES20.glDeleteShader(fragmentShader)
            }
            if (program != -1) {
                GLES20.glDeleteProgram(program)
            }
            throw e
        }
    }

    private fun loadLocations() {
        checkGlThread()
        positionLoc = GLES20.glGetAttribLocation(programHandle, "aPosition")
        checkLocationOrThrow(positionLoc, "aPosition")
        texCoordLoc = GLES20.glGetAttribLocation(programHandle, "aTextureCoord")
        checkLocationOrThrow(texCoordLoc, "aTextureCoord")
        texMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uTexMatrix")
        checkLocationOrThrow(texMatrixLoc, "uTexMatrix")
    }

    private fun loadShader(shaderType: Int, source: String): Int {
        checkGlThread()
        val shader = GLES20.glCreateShader(shaderType)
        checkGlErrorOrThrow("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled,  /*offset=*/0)
        check(compiled[0] == GLES20.GL_TRUE) {
            Log.w(TAG, "Could not compile shader: $source")
            try {
                return@check "Could not compile shader type " +
                        "$shaderType: ${GLES20.glGetShaderInfoLog(shader)}"
            } finally {
                GLES20.glDeleteShader(shader)
            }
        }
        return shader
    }

    private fun createTexture() {
        checkGlThread()
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        checkGlErrorOrThrow("glGenTextures")
        val texId = textures[0]
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, texId)
        checkGlErrorOrThrow("glBindTexture $texId")
        GLES20.glTexParameterf(
            GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        checkGlErrorOrThrow("glTexParameter")
        externalTextureId = texId
    }

    private fun useAndConfigureProgram() {
        checkGlThread()
        // Select the program.
        GLES20.glUseProgram(programHandle)
        checkGlErrorOrThrow("glUseProgram")

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, externalTextureId)

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(positionLoc)
        checkGlErrorOrThrow("glEnableVertexAttribArray")

        // Connect vertexBuffer to "aPosition".
        val coordsPerVertex = 2
        val vertexStride = 0
        GLES20.glVertexAttribPointer(
            positionLoc, coordsPerVertex, GLES20.GL_FLOAT,  /*normalized=*/
            false, vertexStride, VERTEX_BUF
        )
        checkGlErrorOrThrow("glVertexAttribPointer")

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(texCoordLoc)
        checkGlErrorOrThrow("glEnableVertexAttribArray")

        // Connect texBuffer to "aTextureCoord".
        val coordsPerTex = 2
        val texStride = 0
        GLES20.glVertexAttribPointer(
            texCoordLoc, coordsPerTex, GLES20.GL_FLOAT,  /*normalized=*/
            false, texStride, TEX_BUF
        )
        checkGlErrorOrThrow("glVertexAttribPointer")
    }

    private fun checkGlErrorOrThrow(op: String) {
        val error = GLES20.glGetError()
        check(error == GLES20.GL_NO_ERROR) { op + ": GL error 0x" + Integer.toHexString(error) }
    }

    private fun checkLocationOrThrow(location: Int, label: String) {
        check(location >= 0) { "Unable to locate '$label' in program" }
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

    private fun checkGlThread() {
        check(GL_THREAD_NAME == Thread.currentThread().name)
    }

    companion object {
        private fun FloatArray.toBuffer(): FloatBuffer {
            val bb = ByteBuffer.allocateDirect(size * SIZEOF_FLOAT)
            bb.order(ByteOrder.nativeOrder())
            val fb = bb.asFloatBuffer()
            fb.put(this)
            fb.position(0)
            return fb
        }

        private val VERTEX_BUF = floatArrayOf(
            -1.0f, -1.0f,  // 0 bottom left
            1.0f, -1.0f,  // 1 bottom right
            -1.0f, 1.0f,  // 2 top left
            1.0f, 1.0f
        ).toBuffer()

        private val TEX_BUF = floatArrayOf(
            0.0f, 0.0f,  // 0 bottom left
            1.0f, 0.0f,  // 1 bottom right
            0.0f, 1.0f,  // 2 top left
            1.0f, 1.0f // 3 top right
        ).toBuffer()

        private val TEN_BIT_REQUIRED_EGL_EXTENSIONS = listOf(
            "EGL_EXT_gl_colorspace_bt2020_hlg",
            "EGL_EXT_yuv_surface"
        )
    }
}

private class SurfaceOutputScope(val surfaceOutput: SurfaceOutput) {
    private val surfaceLifecycleJob = SupervisorJob()
    private val refCountedSurface = RefCounted<Surface>(onRelease = {
        surfaceOutput.close()
    }).apply {
        // Ensure we don't release until after `initialize` has completed by deferring
        // the release.
        val deferredRelease = CompletableDeferred<Unit>()
        initialize(surfaceOutput.getSurface(Runnable::run) {
            deferredRelease.complete(Unit)
        })
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