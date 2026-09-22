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
     * Returns the most recent cached [Location] fix if available and not stale.
     *
     * This call does not perform hardware I/O or wait for satellite/network acquisition, making
     * it suitable for synchronous access during shutter capture without introducing latency.
     *
     * @return The cached [Location], or `null` if no valid non-stale fix is available.
     */
    fun getCachedLocation(): Location?

    /**
     * Queries the active location provider for a current [Location] fix.
     *
     * Suspends until a location fix is retrieved or an internal timeout occurs. If permissions are
     * missing or all providers are disabled, returns `null` immediately.
     *
     * @return The fresh [Location], or `null` if unavailable.
     */
    suspend fun getCurrentLocation(): Location?

    /**
     * Initiates active location updates from available location providers.
     *
     * Should be called when the camera preview becomes active to warm up location hardware and
     * populate the cache prior to photo capture or video recording. Has no effect if location
     * permissions have not been granted.
     *
     * Implementations should ensure this method is idempotent or reference-counted across
     * multiple concurrent callers so that overlapping lifecycles do not prematurely stop or
     * duplicate hardware listener registrations.
     */
    fun startLocationUpdates()

    /**
     * Stops active location updates and releases hardware resources.
     *
     * Should be called when the camera preview is paused or stopped to conserve battery power.
     *
     * Implementations should ensure this method is idempotent or reference-counted, releasing
     * underlying hardware listeners only when all active callers have stopped requesting updates.
     */
    fun stopLocationUpdates()
}
