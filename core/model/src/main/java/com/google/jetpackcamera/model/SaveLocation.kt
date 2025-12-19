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

/**
 * Represents the intended save location strategy for captured media.
 */
sealed interface SaveLocation {

    /**
     * Indicates that the application should capture media to a temporary file.
     *
     * @param cacheDir the [Uri] where captured media should be cached.
     *
     * If [cacheDir] is null, then the app's cache location will default to
     * [android.content.Context.getCacheDir].
     */
    data class Cache(val cacheDir: Uri? = null) : SaveLocation

    /**
     * Indicates that the application's default logic for capturing media
     * should determine the save location and filename. The CameraUseCase typically
     * creates a new entry in the MediaStore with a generated name.
     */
    object Default : SaveLocation

    /**
     * Specifies a fully-defined [Uri] where the captured media should be saved.
     * The CameraUseCase will pass this Uri directly to the underlying camera API.
     *
     * @property locationUri The non-null, complete [Uri] for the output file.
     */
    data class Explicit(val locationUri: Uri) : SaveLocation
}
