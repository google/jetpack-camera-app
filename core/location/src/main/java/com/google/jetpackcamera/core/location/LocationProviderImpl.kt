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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.google.jetpackcamera.settings.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicReference
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val TAG = "LocationProviderImpl"

@Singleton
class LocationProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository
) : LocationProvider {

    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
        as LocationManager
    private val cachedLocation = AtomicReference<Location?>(null)
    private var isUpdating = false
    private var timeoutJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    private val locationListener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }
    }

    override fun getCachedLocation(): Location? {
        val location = cachedLocation.get() ?: return null
        if (!isValidLocation(location) || isStale(location)) {
            cachedLocation.set(null)
            return null
        }
        return location
    }

    override suspend fun getCurrentLocation(): Location? {
        val locationEnabled = settingsRepository.defaultCameraAppSettings.first().locationEnabled
        if (!locationEnabled || !hasAnyLocationPermission()) {
            return null
        }

        return getCachedLocation()
    }

    @SuppressLint("MissingPermission")
    override fun startLocationUpdates() {
        if (isUpdating) return

        scope.launch {
            val locationEnabled =
                settingsRepository.defaultCameraAppSettings.first().locationEnabled
            if (!locationEnabled || !hasAnyLocationPermission()) {
                return@launch
            }

            isUpdating = true

            try {
                val provider = getBestProvider() ?: return@launch

                val request = LocationRequestCompat.Builder(0L)
                    .setMinUpdateDistanceMeters(0f)
                    .build()

                LocationManagerCompat.requestLocationUpdates(
                    locationManager,
                    provider,
                    request,
                    locationListener,
                    Looper.getMainLooper()
                )

                // 60s timeout
                timeoutJob = scope.launch {
                    delay(60_000L)
                    stopLocationUpdates()
                }
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException requesting location updates", e)
                stopLocationUpdates()
            }
        }
    }

    override fun stopLocationUpdates() {
        if (!isUpdating) return
        isUpdating = false
        timeoutJob?.cancel()
        LocationManagerCompat.removeUpdates(locationManager, locationListener)
    }

    private fun handleLocationUpdate(location: Location) {
        if (!isValidLocation(location)) return

        val oldLoc = cachedLocation.get()
        if (oldLoc == null || location.accuracy <= oldLoc.accuracy || isStale(oldLoc)) {
            cachedLocation.set(location)
        }

        // 50m accuracy threshold
        if (location.accuracy <= 50f) {
            stopLocationUpdates()
        }
    }

    private fun isValidLocation(location: Location): Boolean {
        if (location.latitude.isNaN() || location.longitude.isNaN()) return false
        if (location.latitude.isInfinite() || location.longitude.isInfinite()) return false
        if (location.latitude == 0.0 && location.longitude == 0.0) return false
        return true
    }

    private fun isStale(location: Location): Boolean {
        // 30m freshness validation
        val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        val maxAgeNanos = 30L * 60L * 1000L * 1_000_000L
        return ageNanos > maxAgeNanos
    }

    private fun hasAnyLocationPermission(): Boolean {
        return hasFinePermission() || hasCoarsePermission()
    }

    private fun hasFinePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasCoarsePermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun getBestProvider(): String? {
        val hasGps =
            LocationManagerCompat.hasProvider(locationManager, LocationManager.GPS_PROVIDER)
        if (hasFinePermission() && hasGps) {
            return LocationManager.GPS_PROVIDER
        }
        val hasNetwork =
            LocationManagerCompat.hasProvider(locationManager, LocationManager.NETWORK_PROVIDER)
        if (hasCoarsePermission() && hasNetwork) {
            return LocationManager.NETWORK_PROVIDER
        }
        val hasFused =
            LocationManagerCompat.hasProvider(locationManager, LocationManager.FUSED_PROVIDER)
        if (hasCoarsePermission() && hasFused) {
            return LocationManager.FUSED_PROVIDER
        }
        return null
    }
}
