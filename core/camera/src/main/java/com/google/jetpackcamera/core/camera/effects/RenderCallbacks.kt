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

package com.google.jetpackcamera.core.camera.effects

import android.graphics.SurfaceTexture
import android.opengl.EGLConfig
import android.opengl.EGLSurface
import android.view.Surface
import androidx.graphics.opengl.egl.EGLManager
import androidx.graphics.opengl.egl.EGLSpec

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