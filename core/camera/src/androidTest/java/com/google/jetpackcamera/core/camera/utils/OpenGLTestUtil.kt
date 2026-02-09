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
package com.google.jetpackcamera.core.camera.utils

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.EGL14.EGL_CONTEXT_CLIENT_VERSION
import android.opengl.EGL14.EGL_DEFAULT_DISPLAY
import android.opengl.EGL14.EGL_HEIGHT
import android.opengl.EGL14.EGL_NONE
import android.opengl.EGL14.EGL_NO_CONTEXT
import android.opengl.EGL14.EGL_NO_DISPLAY
import android.opengl.EGL14.EGL_NO_SURFACE
import android.opengl.EGL14.EGL_OPENGL_ES2_BIT
import android.opengl.EGL14.EGL_PBUFFER_BIT
import android.opengl.EGL14.EGL_RENDERABLE_TYPE
import android.opengl.EGL14.EGL_SURFACE_TYPE
import android.opengl.EGL14.EGL_WIDTH
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.view.Surface
import androidx.camera.core.SurfaceRequest
import java.util.Objects
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext

/**
 * Provides a surface to the [SurfaceRequest] that automatically updates its texture image
 * when a frame is available.
 *
 * This function creates a [SurfaceTexture] attached to an OpenGL context and a [Surface] backed by it.
 * It handles the lifecycle of the surface and ensures that frames are updated on a separate thread.
 *
 * This is useful for tests that require a valid surface provider but do not need to display the output.
 */
suspend fun SurfaceRequest.provideUpdatingSurface() {
    var isReleased = false
    val executor = Executors.newFixedThreadPool(1)

    val surfaceTexture =
        withContext(executor.asCoroutineDispatcher()) {
            SurfaceTexture(0).apply {
                setDefaultBufferSize(resolution.width, resolution.height)
                detachFromGLContext()
                attachToGLContext(GLUtil.getTexIdFromGLContext())
                setOnFrameAvailableListener {
                    try {
                        executor.execute {
                            if (!isReleased) {
                                updateTexImage()
                            }
                        }
                    } catch (_: RejectedExecutionException) {
                        // Ignored since frame updating is no longer needed after surface
                        // and executor are released.
                    }
                }
            }
        }
    val surface = Surface(surfaceTexture)

    provideSurface(surface, executor) {
        surfaceTexture.release()
        surface.release()
        executor.shutdown()
        isReleased = true
    }
}

/**
 * Utility class containing helper methods for OpenGL ES operations in tests.
 */
private object GLUtil {
    /**
     * Generates a new texture ID bound to the current OpenGL context.
     *
     * This method initializes a GL context before generating the texture.
     *
     * @return The generated texture ID.
     */
    fun getTexIdFromGLContext(): Int {
        setupGLContext()
        val texIds = IntArray(1)
        GLES20.glGenTextures(1, texIds, 0)
        return texIds[0]
    }

    /**
     * Sets up an EGL context with a 1x1 pbuffer surface.
     *
     * This method initializes the EGL display, chooses a configuration, creates a pbuffer surface,
     * creates a context, and makes it current. This is necessary to execute OpenGL commands
     * that require an active context.
     *
     * @throws RuntimeException if any step of the EGL setup fails.
     */
    fun setupGLContext() {
        val eglDisplay = EGL14.eglGetDisplay(EGL_DEFAULT_DISPLAY)
        if (Objects.equals(eglDisplay, EGL_NO_DISPLAY)) {
            throw RuntimeException("Unable to get EGL display.")
        }
        val majorVer = IntArray(1)
        val majorOffset = 0
        val minorVer = IntArray(1)
        val minorOffset = 0
        if (!EGL14.eglInitialize(eglDisplay, majorVer, majorOffset, minorVer, minorOffset)) {
            throw RuntimeException("Unable to initialize EGL.")
        }

        val configAttribs =
            intArrayOf(
                EGL_RENDERABLE_TYPE,
                EGL_OPENGL_ES2_BIT,
                EGL_SURFACE_TYPE,
                EGL_PBUFFER_BIT,
                EGL_NONE
            )
        val configAttribsOffset = 0
        val configs = arrayOfNulls<EGLConfig>(1)
        val configsOffset = 0
        val numConfigs = IntArray(1)
        val numConfigsOffset = 0
        if (!EGL14.eglChooseConfig(
                eglDisplay,
                configAttribs,
                configAttribsOffset,
                configs,
                configsOffset,
                configs.size,
                numConfigs,
                numConfigsOffset
            )
        ) {
            throw RuntimeException("No appropriate EGL config exists on device.")
        }
        val eglConfig = configs[0]

        // Use a 1x1 pbuffer as our surface
        val pbufferAttribs =
            intArrayOf(
                EGL_WIDTH,
                1,
                EGL_HEIGHT,
                1,
                EGL_NONE
            )
        val pbufferAttribsOffset = 0
        val eglPbuffer =
            EGL14.eglCreatePbufferSurface(
                eglDisplay,
                eglConfig,
                pbufferAttribs,
                pbufferAttribsOffset
            )
        if (Objects.equals(eglPbuffer, EGL_NO_SURFACE)) {
            throw RuntimeException("Unable to create pbuffer surface.")
        }

        val contextAttribs = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 2, EGL_NONE)
        val contextAttribsOffset = 0
        val eglContext =
            EGL14.eglCreateContext(
                eglDisplay,
                eglConfig,
                EGL_NO_CONTEXT,
                contextAttribs,
                contextAttribsOffset
            )
        if (Objects.equals(eglContext, EGL_NO_CONTEXT)) {
            throw RuntimeException("Unable to create EGL context.")
        }

        if (!EGL14.eglMakeCurrent(eglDisplay, eglPbuffer, eglPbuffer, eglContext)) {
            throw RuntimeException("Failed to make EGL context current.")
        }
    }
}
