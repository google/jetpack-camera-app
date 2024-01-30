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

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import androidx.camera.core.DynamicRange
import androidx.camera.core.SurfaceOutput
import androidx.camera.core.SurfaceProcessor
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.impl.utils.executor.CameraXExecutors.newHandlerExecutor
import androidx.camera.core.processing.OpenGlRenderer
import androidx.camera.core.processing.ShaderProvider
import java.util.concurrent.Executor

private const val GL_THREAD_NAME = "EmptySurfaceProcessor"

/**
 * This is a [SurfaceProcessor] that passes on the same content from the input
 * surface to the output surface. Used to make a copies of surfaces.
 */
@SuppressLint("RestrictedApi")
class EmptySurfaceProcessor : SurfaceProcessor {
    private val glThread: HandlerThread = HandlerThread(GL_THREAD_NAME)
    private var glHandler: Handler
    var glExecutor: Executor
        private set

    // Members below are only accessed on GL thread.
    private val glRenderer: OpenGlRenderer = OpenGlRenderer()
    private val outputSurfaces: MutableMap<SurfaceOutput, Surface> = mutableMapOf()
    private val textureTransform: FloatArray = FloatArray(16)
    private val surfaceTransform: FloatArray = FloatArray(16)
    private var isReleased = false

    init {
        glThread.start()
        glHandler = Handler(glThread.looper)
        glExecutor = newHandlerExecutor(glHandler)
    }

    override fun onInputSurface(surfaceRequest: SurfaceRequest) {
        checkGlThread()
        if (isReleased) {
            surfaceRequest.willNotProvideSurface()
            return
        }

        // Internally, this checks if the renderer is already initialized, so it's ok to call.
        glRenderer.release()

        val shaderProvider =
            if (surfaceRequest.dynamicRange.encoding != DynamicRange.ENCODING_SDR &&
                surfaceRequest.dynamicRange.bitDepth == DynamicRange.BIT_DEPTH_10_BIT
            ) {
                object : ShaderProvider {
                    override fun createFragmentShader(
                        samplerVarName: String,
                        fragCoordsVarName: String
                    ): String {
                        return """
                            #version 300 es
                            #extension GL_OES_EGL_image_external : require
                            #extension GL_EXT_YUV_target : require
                            precision mediump float;
                            uniform __samplerExternal2DY2YEXT $samplerVarName;
                            in vec2 $fragCoordsVarName;
                            out vec4 outColor;
                            void main() {
                              vec3 srcYuv = texture($samplerVarName, $fragCoordsVarName).xyz;
                              outColor = vec4(srcYuv, 1.0);
                            }
                        """.trimIndent()
                    }
                }
            } else {
                ShaderProvider.DEFAULT
            }

        // Init with new dynamic range
        glRenderer.init(surfaceRequest.dynamicRange, shaderProvider)

        val surfaceTexture = SurfaceTexture(glRenderer.textureName)
        surfaceTexture.setDefaultBufferSize(
            surfaceRequest.resolution.width,
            surfaceRequest.resolution.height
        )
        val surface = Surface(surfaceTexture)
        surfaceRequest.provideSurface(surface, glExecutor) {
            surfaceTexture.setOnFrameAvailableListener(null)
            surfaceTexture.release()
            surface.release()
        }

        surfaceTexture.setOnFrameAvailableListener({
            checkGlThread()
            if (!isReleased) {
                surfaceTexture.updateTexImage()
                surfaceTexture.getTransformMatrix(textureTransform)
                outputSurfaces.forEach { (surfaceOutput, surface) ->
                    run {
                        surfaceOutput.updateTransformMatrix(surfaceTransform, textureTransform)
                        glRenderer.render(surfaceTexture.timestamp, surfaceTransform, surface)
                    }
                }
            }
        }, glHandler)
    }

    override fun onOutputSurface(surfaceOutput: SurfaceOutput) {
        checkGlThread()
        if (isReleased) {
            surfaceOutput.close()
            return
        }
        val surface =
            surfaceOutput.getSurface(glExecutor) {
                surfaceOutput.close()
                outputSurfaces.remove(surfaceOutput)?.let { removedSurface ->
                    glRenderer.unregisterOutputSurface(removedSurface)
                }
            }
        glRenderer.registerOutputSurface(surface)
        outputSurfaces[surfaceOutput] = surface
    }

    /**
     * Releases associated resources.
     *
     * Closes output surfaces.
     * Releases the [OpenGlRenderer].
     * Quits the GL HandlerThread.
     */
    fun release() {
        glExecutor.execute {
            releaseInternal()
        }
    }

    private fun releaseInternal() {
        checkGlThread()
        if (!isReleased) {
            // Once release is called, we can stop sending frame to output surfaces.
            for (surfaceOutput in outputSurfaces.keys) {
                surfaceOutput.close()
            }
            outputSurfaces.clear()
            glRenderer.release()
            glThread.quitSafely()
            isReleased = true
        }
    }

    private fun checkGlThread() {
        check(GL_THREAD_NAME == Thread.currentThread().name)
    }
}
