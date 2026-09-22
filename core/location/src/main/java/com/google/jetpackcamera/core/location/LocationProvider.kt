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
package com.google.jetpackcamera.core.location

import android.location.Location

/**
 * Provider interface for acquiring geographic location data to geotag captured media.
 *
 * Implementations manage underlying platform location services, coordinate provider registration,
 * and maintain cached location fixes according to freshness and accuracy policies.
 */
interface LocationProvider {

    /**
     * Starts location updates to acquire and maintain geographic coordinates.
     *
     * Runs until the calling coroutine is cancelled.
     */
    suspend fun runLocationUpdates()

    /**
     * Returns the current [Location] fix for media geotagging, or `null` if disabled,
     * unpermitted, or unavailable.
     *
     * @return The most recent valid [Location] fix, or `null`.
     */
    fun getCurrentLocation(): Location?
}
