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
package com.google.jetpackcamera.core.camera.postprocess

import android.net.Uri

/**
 * An interface for performing post-processing on a captured image.
 *
 * Each post-processor should be associated with a unique [ImagePostProcessorFeatureKey].
 */
interface ImagePostProcessor {
    /**
     * Performs a post-processing operation on the image located at the given URI after an image has
     * been successfully captured and saved. Make sure the this operation does not block I/O when
     * performing the post-processing.
     *
     * @param uri The [Uri] of the saved image that needs to be processed.
     */
    suspend fun postProcessImage(uri: Uri)
}
