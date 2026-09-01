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

class FakeLocationProvider(
    private var mockLocation: Location? = null,
    var isUpdatesRunning: Boolean = false
) : LocationProvider {
    fun setLocation(latitude: Double, longitude: Double, accuracy: Float = 5.0f) {
        mockLocation = Location("test").apply {
            this.latitude = latitude
            this.longitude = longitude
            this.accuracy = accuracy
            this.time = System.currentTimeMillis()
            this.elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }
    }

    fun clearLocation() {
        mockLocation = null
    }

    override fun getCachedLocation(): Location? = mockLocation
    override suspend fun getCurrentLocation(): Location? = mockLocation
    override fun startLocationUpdates() {
        isUpdatesRunning = true
    }
    override fun stopLocationUpdates() {
        isUpdatesRunning = false
    }
}
