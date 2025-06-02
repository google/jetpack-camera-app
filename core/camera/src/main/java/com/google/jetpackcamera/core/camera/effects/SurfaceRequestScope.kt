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

import androidx.camera.core.SurfaceRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

class SurfaceRequestScope(private val surfaceRequest: SurfaceRequest) {
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
