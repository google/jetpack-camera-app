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
package com.google.jetpackcamera.core.location.testing

import android.location.Location
import android.os.SystemClock
import com.google.jetpackcamera.core.location.LocationProvider
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * Fake implementation of [LocationProvider] for unit and integration testing.
 *
 * Allows test suites to simulate location updates, control cache values, and toggle
 * location availability.
 *
 * @property isUpdatesRunning Whether location updates are currently marked as active.
 * @property locationEnabled Whether location reporting is enabled; when `false`, all calls return `null`.
 */
class FakeLocationProvider(
    initialMockLocation: Location? = null,
    initialIsUpdatesRunning: Boolean = false,
    initialLocationEnabled: Boolean = true
) : LocationProvider {

    private val mockLocation = AtomicReference<Location?>(initialMockLocation)
    private val _isUpdatesRunning = AtomicBoolean(initialIsUpdatesRunning)
    private val _locationEnabled = AtomicBoolean(initialLocationEnabled)

    var isUpdatesRunning: Boolean
        get() = _isUpdatesRunning.get()
        set(value) {
            _isUpdatesRunning.set(value)
        }

    var locationEnabled: Boolean
        get() = _locationEnabled.get()
        set(value) {
            _locationEnabled.set(value)
        }

    /**
     * Sets the simulated location coordinates and accuracy with current timestamps.
     *
     * @param latitude The latitude in degrees.
     * @param longitude The longitude in degrees.
     * @param accuracy The horizontal accuracy radius in meters (defaults to 5.0m).
     */
    fun setLocation(latitude: Double, longitude: Double, accuracy: Float = 5.0f) {
        val loc = Location("test").apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = accuracy
            this.time = System.currentTimeMillis()
            this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
        mockLocation.set(loc)
    }

    /**
     * Clears any configured mock location fix.
     */
    fun clearLocation() {
        mockLocation.set(null)
    }

    override fun getCachedLocation(): Location? = if (locationEnabled) mockLocation.get() else null

    override suspend fun getCurrentLocation(): Location? =
        if (locationEnabled) mockLocation.get() else null

    override fun startLocationUpdates() {
        if (!locationEnabled) return
        _isUpdatesRunning.set(true)
    }

    override fun stopLocationUpdates() {
        _isUpdatesRunning.set(false)
    }
}
