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

import android.location.Location

/**
 * Returns this [Location] if its coordinates are within the valid geographic range, or `null`
 * otherwise.
 *
 * CameraX video output options reject coordinates outside latitude [-90, 90] and longitude
 * [-180, 180] with an [IllegalArgumentException]. Dropping such a location lets the capture
 * proceed without location metadata instead of failing.
 */
internal fun Location.takeIfInBounds(): Location? =
    takeIf { it.latitude in -90.0..90.0 && it.longitude in -180.0..180.0 }
