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
package com.google.jetpackcamera.model

import android.net.Uri

sealed interface SaveMode {
    /**
     * (Default) Saves media directly to MediaStore or an Explicit Uri.
     */
    data object Immediate : SaveMode

    /**
     * Saves media to a temporary cache file
     *
     * @param allowMultipleImages If true, the user can capture multiple images
     * in a single review session.
     */
    data class CacheAndReview(
        // todo(kc): how to handle 1, max limit (>1), or unlimited (0)
        val allowMultipleImages: Boolean = false,
        val cacheDir: Uri? = null
    ) : SaveMode
}
