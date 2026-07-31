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

import android.util.Size
import android.view.Surface
import androidx.camera.core.SurfaceOutput
import com.google.jetpackcamera.core.common.RefCounted
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch

class SurfaceOutputScope(val surfaceOutput: SurfaceOutput) {
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
