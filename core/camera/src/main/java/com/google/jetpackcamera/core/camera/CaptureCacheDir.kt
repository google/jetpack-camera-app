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
package com.google.jetpackcamera.core.camera

import android.content.Context
import java.io.File

/**
 * Name of the subdirectory of [Context.getCacheDir] where captures are written when
 * [com.google.jetpackcamera.model.SaveLocation.Cache] does not specify a directory.
 *
 * Keeping cached captures in a dedicated subdirectory lets an app share them through a
 * `FileProvider` without exposing the rest of its cache directory.
 */
internal const val DEFAULT_CAPTURE_CACHE_SUBDIRECTORY = "captures"

/**
 * Returns the default directory for cached captures, creating it if it does not exist.
 */
internal fun Context.defaultCaptureCacheDir(): File =
    File(cacheDir, DEFAULT_CAPTURE_CACHE_SUBDIRECTORY).apply { mkdirs() }
