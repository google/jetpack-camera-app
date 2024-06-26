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

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import androidx.graphics.opengl.egl.EGLSpec

val EGLSpec.Companion.V14ES3: EGLSpec
    get() = object : EGLSpec by V14 {

        private val contextAttributes = intArrayOf(
            // GLES VERSION 3
            EGL14.EGL_CONTEXT_CLIENT_VERSION,
            3,
            // HWUI provides the ability to configure a context priority as well but that only
            // seems to be configured on SystemUIApplication. This might be useful for
            // front buffer rendering situations for performance.
            EGL14.EGL_NONE
        )

        override fun eglCreateContext(config: EGLConfig): EGLContext {
            return EGL14.eglCreateContext(
                EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY),
                config,
                // not creating from a shared context
                EGL14.EGL_NO_CONTEXT,
                contextAttributes,
                0
            )
        }
    }
