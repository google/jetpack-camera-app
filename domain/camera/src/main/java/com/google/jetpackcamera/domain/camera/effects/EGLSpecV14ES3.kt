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

import android.hardware.HardwareBuffer
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLSurface
import android.os.Build
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.graphics.opengl.egl.EGLConfigAttributes
import androidx.graphics.opengl.egl.EGLException
import androidx.graphics.opengl.egl.EGLSpec
import androidx.graphics.opengl.egl.EGLVersion
import androidx.opengl.EGLExt
import androidx.opengl.EGLImageKHR
import androidx.opengl.EGLSyncKHR

object EGLSpecV14ES3 : EGLSpec {

    // Tuples of attribute identifiers along with their corresponding values.
    // EGL_NONE is used as a termination value similar to a null terminated string
    private val contextAttributes = intArrayOf(
        // GLES VERSION 3
        EGL14.EGL_CONTEXT_CLIENT_VERSION,
        3,
        // HWUI provides the ability to configure a context priority as well but that only
        // seems to be configured on SystemUIApplication. This might be useful for
        // front buffer rendering situations for performance.
        EGL14.EGL_NONE
    )

    override fun eglInitialize(): EGLVersion {
        // eglInitialize is destructive so create 2 separate arrays to store the major and
        // minor version
        val major = intArrayOf(1)
        val minor = intArrayOf(1)
        val initializeResult =
            EGL14.eglInitialize(getDefaultDisplay(), major, 0, minor, 0)
        if (initializeResult) {
            return EGLVersion(major[0], minor[0])
        } else {
            throw EGLException(EGL14.eglGetError(), "Unable to initialize default display")
        }
    }

    override fun eglGetCurrentReadSurface(): EGLSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)

    override fun eglGetCurrentDrawSurface(): EGLSurface = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)

    override fun eglQueryString(nameId: Int): String =
        EGL14.eglQueryString(getDefaultDisplay(), nameId)

    override fun eglCreatePBufferSurface(
        config: EGLConfig,
        configAttributes: EGLConfigAttributes?
    ): EGLSurface = EGL14.eglCreatePbufferSurface(
        getDefaultDisplay(),
        config,
        configAttributes?.toArray(),
        0
    )

    override fun eglCreateWindowSurface(
        config: EGLConfig,
        surface: Surface,
        configAttributes: EGLConfigAttributes?
    ): EGLSurface = EGL14.eglCreateWindowSurface(
        getDefaultDisplay(),
        config,
        surface,
        configAttributes?.toArray() ?: DefaultWindowSurfaceConfig.toArray(),
        0
    )

    override fun eglSwapBuffers(surface: EGLSurface): Boolean =
        EGL14.eglSwapBuffers(getDefaultDisplay(), surface)

    override fun eglQuerySurface(
        surface: EGLSurface,
        attribute: Int,
        result: IntArray,
        offset: Int
    ): Boolean = EGL14.eglQuerySurface(getDefaultDisplay(), surface, attribute, result, offset)

    override fun eglDestroySurface(surface: EGLSurface) =
        EGL14.eglDestroySurface(getDefaultDisplay(), surface)

    override fun eglMakeCurrent(
        context: EGLContext,
        drawSurface: EGLSurface,
        readSurface: EGLSurface
    ): Boolean = EGL14.eglMakeCurrent(
        getDefaultDisplay(),
        drawSurface,
        readSurface,
        context
    )

    override fun loadConfig(configAttributes: EGLConfigAttributes): EGLConfig? {
        val configs = arrayOfNulls<EGLConfig?>(1)
        return if (EGL14.eglChooseConfig(
                getDefaultDisplay(),
                configAttributes.toArray(),
                0,
                configs,
                0,
                1,
                intArrayOf(1),
                0
            )
        ) {
            configs[0]
        } else {
            null
        }
    }

    override fun eglCreateContext(config: EGLConfig): EGLContext {
        return EGL14.eglCreateContext(
            getDefaultDisplay(),
            config,
            // not creating from a shared context
            EGL14.EGL_NO_CONTEXT,
            contextAttributes,
            0
        )
    }

    override fun eglDestroyContext(eglContext: EGLContext) {
        if (!EGL14.eglDestroyContext(getDefaultDisplay(), eglContext)) {
            throw EGLException(EGL14.eglGetError(), "Unable to destroy EGLContext")
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun eglCreateImageFromHardwareBuffer(hardwareBuffer: HardwareBuffer): EGLImageKHR? =
        EGLExt.eglCreateImageFromHardwareBuffer(getDefaultDisplay(), hardwareBuffer)

    override fun eglDestroyImageKHR(image: EGLImageKHR): Boolean =
        EGLExt.eglDestroyImageKHR(getDefaultDisplay(), image)

    override fun eglCreateSyncKHR(type: Int, attributes: EGLConfigAttributes?): EGLSyncKHR? =
        EGLExt.eglCreateSyncKHR(getDefaultDisplay(), type, attributes)

    override fun eglGetSyncAttribKHR(
        sync: EGLSyncKHR,
        attribute: Int,
        value: IntArray,
        offset: Int
    ): Boolean = EGLExt.eglGetSyncAttribKHR(getDefaultDisplay(), sync, attribute, value, offset)

    override fun eglDestroySyncKHR(sync: EGLSyncKHR): Boolean =
        EGLExt.eglDestroySyncKHR(getDefaultDisplay(), sync)

    override fun eglGetError(): Int = EGL14.eglGetError()

    override fun eglClientWaitSyncKHR(sync: EGLSyncKHR, flags: Int, timeoutNanos: Long): Int =
        EGLExt.eglClientWaitSyncKHR(getDefaultDisplay(), sync, flags, timeoutNanos)

    private fun getDefaultDisplay() = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)

    /**
     * EglConfigAttribute that provides the default attributes for an EGL window surface
     */
    private val DefaultWindowSurfaceConfig = EGLConfigAttributes {}
}
