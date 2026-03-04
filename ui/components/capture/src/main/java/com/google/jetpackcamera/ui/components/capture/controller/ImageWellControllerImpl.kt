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
package com.google.jetpackcamera.ui.components.capture.controller

import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.data.media.MediaRepository
import com.google.jetpackcamera.ui.components.capture.controller.Utils.postCurrentMediaToMediaRepository
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Implementation of [ImageWellController] that handles image well actions.
 *
 * @param mediaRepository The [MediaRepository] for accessing media.
 * @param updateLastCapturedMediaCallback Callback to update the last captured media.
 * @param coroutineContext The [CoroutineContext] for launching coroutines.
 */
class ImageWellControllerImpl(
    private val mediaRepository: MediaRepository,
    private val updateLastCapturedMediaCallback: () -> Unit,
    coroutineContext: CoroutineContext
) : ImageWellController {
    private val job = Job(parent = coroutineContext[Job])
    private val scope = CoroutineScope(coroutineContext + job)
    override fun imageWellToRepository(mediaDescriptor: MediaDescriptor) {
        scope.launch {
            postCurrentMediaToMediaRepository(
                mediaRepository,
                mediaDescriptor
            )
        }
    }
    override fun updateLastCapturedMedia() {
        updateLastCapturedMediaCallback()
    }

    suspend fun join() {
        job.join()
    }

    fun cancel() {
        job.cancel()
    }

    fun close() {
        job.cancel()
    }
}
