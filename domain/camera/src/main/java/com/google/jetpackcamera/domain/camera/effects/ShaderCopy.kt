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
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import android.view.Surface
import androidx.annotation.WorkerThread
import androidx.camera.core.DynamicRange
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class ShaderCopy(private val dynamicRange: DynamicRange) : RenderCallbacks {

    // Called on worker thread only
    private var externalTextureId: Int = -1
    private var programHandle = -1
    private var texMatrixLoc = -1
    private var positionLoc = -1
    private var texCoordLoc = -1
    private val use10bitPipeline: Boolean
        get() = dynamicRange.bitDepth == DynamicRange.BIT_DEPTH_10_BIT

    override val glThreadName: String
        get() = TAG

    override val provideEGLSpec: () -> EGLSpec
        get() = { if (use10bitPipeline) EGLSpecV14ES3 else EGLSpec.V14 }

    override val initConfig: EGLManager.() -> EGLConfig
        get() = {
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
                )
            ) {
                "Unable to select EGLConfig"
            }
        }

    override val initRenderer: () -> Unit
        get() = {
            createProgram(
                if (use10bitPipeline) {
                    TEN_BIT_VERTEX_SHADER
                } else {
                    DEFAULT_VERTEX_SHADER
                },
                if (use10bitPipeline) {
                    TEN_BIT_FRAGMENT_SHADER
                } else {
                    DEFAULT_FRAGMENT_SHADER
                }
            )
            loadLocations()
            createTexture()
            useAndConfigureProgram()
        }

    override val createSurfaceTexture
        get() = { width: Int, height: Int ->
            SurfaceTexture(externalTextureId).apply {
                setDefaultBufferSize(width, height)
            }
        }

    override val createOutputSurface
        get() = { eglSpec: EGLSpec,
                config: EGLConfig,
                surface: Surface,
                _: Int,
                _: Int ->
            eglSpec.eglCreateWindowSurface(
                config,
                surface,
                EGLConfigAttributes {
                    if (use10bitPipeline) {
                        EGL_GL_COLORSPACE_KHR to EGL_GL_COLORSPACE_BT2020_HLG_EXT
                    }
                }
            )
        }

    override val drawFrame
        get() = { outputWidth: Int,
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
                /*count=*/
                1,
                /*transpose=*/
                false,
                surfaceTransform,
                /*offset=*/
                0
            )
            checkGlErrorOrThrow("glUniformMatrix4fv")

            // Draw the rect.
            GLES20.glDrawArrays(
                GLES20.GL_TRIANGLE_STRIP,
                /*firstVertex=*/
                0,
                /*vertexCount=*/
                4
            )
            checkGlErrorOrThrow("glDrawArrays")
        }

    @WorkerThread
    fun createTexture() {
        checkGlThread()
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        checkGlErrorOrThrow("glGenTextures")
        val texId = textures[0]
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texId)
        checkGlErrorOrThrow("glBindTexture $texId")
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_NEAREST.toFloat()
        )
        GLES20.glTexParameterf(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR.toFloat()
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        checkGlErrorOrThrow("glTexParameter")
        externalTextureId = texId
    }

    @WorkerThread
    fun useAndConfigureProgram() {
        checkGlThread()
        // Select the program.
        GLES20.glUseProgram(programHandle)
        checkGlErrorOrThrow("glUseProgram")

        // Set the texture.
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, externalTextureId)

        // Enable the "aPosition" vertex attribute.
        GLES20.glEnableVertexAttribArray(positionLoc)
        checkGlErrorOrThrow("glEnableVertexAttribArray")

        // Connect vertexBuffer to "aPosition".
        val coordsPerVertex = 2
        val vertexStride = 0
        GLES20.glVertexAttribPointer(
            positionLoc,
            coordsPerVertex,
            GLES20.GL_FLOAT,
            /*normalized=*/
            false,
            vertexStride,
            VERTEX_BUF
        )
        checkGlErrorOrThrow("glVertexAttribPointer")

        // Enable the "aTextureCoord" vertex attribute.
        GLES20.glEnableVertexAttribArray(texCoordLoc)
        checkGlErrorOrThrow("glEnableVertexAttribArray")

        // Connect texBuffer to "aTextureCoord".
        val coordsPerTex = 2
        val texStride = 0
        GLES20.glVertexAttribPointer(
            texCoordLoc,
            coordsPerTex,
            GLES20.GL_FLOAT,
            /*normalized=*/
            false,
            texStride,
            TEX_BUF
        )
        checkGlErrorOrThrow("glVertexAttribPointer")
    }

    @WorkerThread
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
            GLES20.glGetProgramiv(
                program,
                GLES20.GL_LINK_STATUS,
                linkStatus,
                /*offset=*/
                0
            )
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

    @WorkerThread
    private fun loadLocations() {
        checkGlThread()
        positionLoc = GLES20.glGetAttribLocation(programHandle, "aPosition")
        checkLocationOrThrow(positionLoc, "aPosition")
        texCoordLoc = GLES20.glGetAttribLocation(programHandle, "aTextureCoord")
        checkLocationOrThrow(texCoordLoc, "aTextureCoord")
        texMatrixLoc = GLES20.glGetUniformLocation(programHandle, "uTexMatrix")
        checkLocationOrThrow(texMatrixLoc, "uTexMatrix")
    }

    @WorkerThread
    private fun loadShader(shaderType: Int, source: String): Int {
        checkGlThread()
        val shader = GLES20.glCreateShader(shaderType)
        checkGlErrorOrThrow("glCreateShader type=$shaderType")
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val compiled = IntArray(1)
        GLES20.glGetShaderiv(
            shader,
            GLES20.GL_COMPILE_STATUS,
            compiled,
            /*offset=*/
            0
        )
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

    @WorkerThread
    private fun checkGlErrorOrThrow(op: String) {
        val error = GLES20.glGetError()
        check(error == GLES20.GL_NO_ERROR) { op + ": GL error 0x" + Integer.toHexString(error) }
    }

    private fun checkLocationOrThrow(location: Int, label: String) {
        check(location >= 0) { "Unable to locate '$label' in program" }
    }

    companion object {
        private const val SIZEOF_FLOAT = 4

        private val VERTEX_BUF = floatArrayOf(
            // 0 bottom left
            -1.0f,
            -1.0f,
            // 1 bottom right
            1.0f,
            -1.0f,
            // 2 top left
            -1.0f,
            1.0f,
            // 3 top right
            1.0f,
            1.0f
        ).toBuffer()

        private val TEX_BUF = floatArrayOf(
            // 0 bottom left
            0.0f,
            0.0f,
            // 1 bottom right
            1.0f,
            0.0f,
            // 2 top left
            0.0f,
            1.0f,
            // 3 top right
            1.0f,
            1.0f
        ).toBuffer()

        private const val TAG = "ShaderCopy"
        private const val GL_THREAD_NAME = TAG

        private const val VAR_TEXTURE_COORD = "vTextureCoord"
        private val DEFAULT_VERTEX_SHADER =
            """
        uniform mat4 uTexMatrix;
        attribute vec4 aPosition;
        attribute vec4 aTextureCoord;
        varying vec2 $VAR_TEXTURE_COORD;
        void main() {
            gl_Position = aPosition;
            $VAR_TEXTURE_COORD = (uTexMatrix * aTextureCoord).xy;
        }
            """.trimIndent()

        private val TEN_BIT_VERTEX_SHADER =
            """
        #version 300 es
        in vec4 aPosition;
        in vec4 aTextureCoord;
        uniform mat4 uTexMatrix;
        out vec2 $VAR_TEXTURE_COORD;
        void main() {
          gl_Position = aPosition;
          $VAR_TEXTURE_COORD = (uTexMatrix * aTextureCoord).xy;
        }
            """.trimIndent()

        private const val VAR_TEXTURE = "sTexture"
        private val DEFAULT_FRAGMENT_SHADER =
            """
        #extension GL_OES_EGL_image_external : require
        precision mediump float;
        varying vec2 $VAR_TEXTURE_COORD;
        uniform samplerExternalOES $VAR_TEXTURE;
        void main() {
            gl_FragColor = texture2D($VAR_TEXTURE, $VAR_TEXTURE_COORD);
        }
            """.trimIndent()

        private val TEN_BIT_FRAGMENT_SHADER =
            """
        #version 300 es
        #extension GL_EXT_YUV_target : require
        precision mediump float;
        uniform __samplerExternal2DY2YEXT $VAR_TEXTURE;
        in vec2 $VAR_TEXTURE_COORD;
        layout (yuv) out vec3 outColor;
        
        void main() {
          outColor = texture($VAR_TEXTURE, $VAR_TEXTURE_COORD).xyz;
        }
            """.trimIndent()

        private const val EGL_GL_COLORSPACE_KHR = 0x309D
        private const val EGL_GL_COLORSPACE_BT2020_HLG_EXT = 0x3540

        private val TEN_BIT_REQUIRED_EGL_EXTENSIONS = listOf(
            "EGL_EXT_gl_colorspace_bt2020_hlg",
            "EGL_EXT_yuv_surface"
        )

        private fun FloatArray.toBuffer(): FloatBuffer {
            val bb = ByteBuffer.allocateDirect(size * SIZEOF_FLOAT)
            bb.order(ByteOrder.nativeOrder())
            val fb = bb.asFloatBuffer()
            fb.put(this)
            fb.position(0)
            return fb
        }

        private fun checkGlThread() {
            check(GL_THREAD_NAME == Thread.currentThread().name)
        }
    }
}
