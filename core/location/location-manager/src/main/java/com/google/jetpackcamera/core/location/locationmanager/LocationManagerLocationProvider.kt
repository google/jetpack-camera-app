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
package com.google.jetpackcamera.core.location.locationmanager

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.location.LocationListenerCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.location.LocationRequestCompat
import com.google.jetpackcamera.core.location.LocationProvider
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "LocationManagerLocationProvider"
private const val ACQUISITION_TIMEOUT_MS = 60_000L
private const val ACCURACY_THRESHOLD_METERS = 50f
private const val SIGNIFICANT_ACCURACY_DELTA_METERS = 20f
private const val LOCATION_UPDATE_INTERVAL_MS = 1_000L
private const val LOCATION_UPDATE_MIN_DISTANCE_METERS = 0f
private val REFRESH_INTERVAL_MS = TimeUnit.MINUTES.toMillis(5)
private val RETRY_INTERVAL_MS = TimeUnit.SECONDS.toMillis(15)
private val STALE_LOCATION_THRESHOLD_NANOS = TimeUnit.MINUTES.toNanos(30)
private val SIGNIFICANT_TIME_DELTA_NANOS = TimeUnit.MINUTES.toNanos(2)

/**
 * Implementation of [LocationProvider] backed by the Android platform [LocationManager] via
 * [LocationManagerCompat].
 *
 * Concurrently registers available hardware providers ([LocationManager.GPS_PROVIDER],
 * [LocationManager.NETWORK_PROVIDER], and [LocationManager.FUSED_PROVIDER] on API 31+)
 * to provide fast acquisition indoors and outdoors. Implements time-aware comparator heuristics
 * to prevent coordinate anchoring, evicts stale fixes older than 30 minutes, applies a
 * 60-second hardware timeout to conserve battery, and periodically refreshes the location
 * fix every 5 minutes during extended preview sessions.
 *
 * ## Permissions
 * This module declares [Manifest.permission.ACCESS_COARSE_LOCATION], which is sufficient for
 * approximate location from the network and fused providers. To obtain precise location, the app
 * must declare [Manifest.permission.ACCESS_FINE_LOCATION] in its own manifest and request it at
 * runtime together with [Manifest.permission.ACCESS_COARSE_LOCATION]. No other integration is
 * required: permissions are re-checked on every update cycle, and the GPS provider is registered
 * automatically once precise location is granted.
 *
 * Declaring [Manifest.permission.ACCESS_FINE_LOCATION] does not upgrade an existing approximate
 * grant. Users who previously granted approximate location keep it until the app requests
 * precise location again or the user changes the grant in system settings.
 *
 * @param context Application context used to retrieve location services and verify permissions.
 */
class LocationManagerLocationProvider(private val context: Context) : LocationProvider {

    // Context.getSystemService is documented to return null when a service is unavailable. Treat
    // a missing LocationManager as "location not available" rather than crashing at construction.
    private val locationManager: LocationManager? =
        ContextCompat.getSystemService(context, LocationManager::class.java)

    // Written from the main looper; read from any thread by capture via getCurrentLocation().
    private val cachedLocation = AtomicReference<Location?>(null)
    private val isUpdating = AtomicBoolean(false)
    private val accurateFixDeferred = AtomicReference<CompletableDeferred<Unit>?>(null)
    private val hasCheckedPreciseLocationDeclaration = AtomicBoolean(false)

    // Interval between periodic refresh cycles (5 minutes by default, configurable for testing)
    internal var refreshIntervalMs: Long = REFRESH_INTERVAL_MS

    // Interval before retrying when a cycle could not register any provider, for example because
    // system location was off (15 seconds by default, configurable for testing)
    internal var retryIntervalMs: Long = RETRY_INTERVAL_MS

    private val locationListener = object : LocationListenerCompat {
        override fun onLocationChanged(location: Location) {
            handleLocationUpdate(location)
        }
    }

    override fun getCurrentLocation(): Location? {
        if (!isLocationAvailable()) {
            return null
        }

        val location = cachedLocation.get()
        if (location != null && isValidLocation(location) && !isStale(location)) {
            return location
        }

        return getBestLastKnownLocation()?.also { lastKnown ->
            cachedLocation.compareAndSet(location, lastKnown)
        }
    }

    /**
     * Runs a location update session every [refreshIntervalMs] until cancelled. Each cycle re-checks
     * permissions and enabled providers, so changes made mid-session are applied on the next cycle.
     * A cycle that cannot register any provider is retried after the shorter [retryIntervalMs], so
     * turning on system location or granting permission takes effect quickly.
     */
    override suspend fun runLocationUpdates() = coroutineScope {
        logIfPreciseLocationNotDeclared()
        while (isActive) {
            val sessionRan = runUpdateSession()
            delay(if (sessionRan) refreshIntervalMs else retryIntervalMs)
        }
    }

    /**
     * Logs once per provider instance if the app manifest does not declare
     * [Manifest.permission.ACCESS_FINE_LOCATION]. A missing declaration is a supported
     * configuration, so this is informational only; it makes approximate-only behavior visible to
     * integrators who expected precise location.
     */
    private fun logIfPreciseLocationNotDeclared() {
        if (hasCheckedPreciseLocationDeclaration.getAndSet(true)) return
        if (!isPermissionDeclared(Manifest.permission.ACCESS_FINE_LOCATION)) {
            Log.i(
                TAG,
                "ACCESS_FINE_LOCATION is not declared in the app manifest; location is limited " +
                    "to approximate accuracy."
            )
        }
    }

    private fun isPermissionDeclared(permission: String): Boolean {
        val packageInfo = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.PackageInfoFlags.of(PackageManager.GET_PERMISSIONS.toLong())
                )
            } else {
                // The int-flags overload is deprecated on API 33+ but is the only option below it.
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_PERMISSIONS
                )
            }
        } catch (e: PackageManager.NameNotFoundException) {
            Log.w(TAG, "Unable to read declared permissions", e)
            return false
        }
        return packageInfo.requestedPermissions?.contains(permission) == true
    }

    /**
     * Registers the active providers and waits for an accurate fix or [ACQUISITION_TIMEOUT_MS].
     *
     * @return `true` if at least one provider was registered, `false` if the session could not
     * start.
     */
    private suspend fun runUpdateSession(): Boolean {
        val locationManager = locationManager ?: return false
        if (!hasAnyLocationPermission()) return false

        // Pre-seed cache with last known location if available
        getBestLastKnownLocation()?.let { lastKnown ->
            cachedLocation.compareAndSet(null, lastKnown)
        }

        val activeProviders = getActiveProviders()
        if (activeProviders.isEmpty()) return false

        val accurateFixReceived = CompletableDeferred<Unit>()
        accurateFixDeferred.set(accurateFixReceived)

        val request = LocationRequestCompat.Builder(LOCATION_UPDATE_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_UPDATE_INTERVAL_MS)
            .setMinUpdateDistanceMeters(LOCATION_UPDATE_MIN_DISTANCE_METERS)
            .setQuality(LocationRequestCompat.QUALITY_HIGH_ACCURACY)
            .build()

        var registeredCount = 0
        for (provider in activeProviders) {
            try {
                LocationManagerCompat.requestLocationUpdates(
                    locationManager,
                    provider,
                    request,
                    locationListener,
                    Looper.getMainLooper()
                )
                registeredCount++
                Log.d(TAG, "Registered location updates for provider: $provider")
            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException requesting updates for $provider", e)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "IllegalArgumentException requesting updates for $provider", e)
            }
        }

        if (registeredCount == 0) {
            accurateFixDeferred.set(null)
            return false
        }

        isUpdating.set(true)
        Log.d(TAG, "Started location updates across $registeredCount providers")

        // Updates are stopped only here: on an accurate fix (handleLocationUpdate completes
        // accurateFixReceived), on timeout, or on cancellation.
        try {
            withTimeoutOrNull(ACQUISITION_TIMEOUT_MS) {
                accurateFixReceived.await()
            }
        } finally {
            accurateFixDeferred.set(null)
            stopHardwareUpdates()
        }
        return true
    }

    private fun stopHardwareUpdates() {
        val locationManager = locationManager ?: return
        if (isUpdating.getAndSet(false)) {
            removeLocationUpdates(locationManager)
            Log.d(TAG, "Stopped location updates across all providers.")
        }
    }

    /**
     * Unregisters [locationListener] from all providers. Unregistering must succeed even if
     * permission was revoked after registration, so it is intentionally not gated on a permission
     * check. The platform does not require a location permission to remove a listener.
     */
    @SuppressLint("MissingPermission")
    private fun removeLocationUpdates(locationManager: LocationManager) {
        LocationManagerCompat.removeUpdates(locationManager, locationListener)
    }

    private fun getBestLastKnownLocation(): Location? {
        val locationManager = locationManager ?: return null
        if (!isLocationAvailable()) return null

        var bestLocation: Location? = null
        val hasFine = hasFinePermission()
        val providers = buildList {
            // GPS and PASSIVE providers require ACCESS_FINE_LOCATION.
            if (hasFine) add(LocationManager.GPS_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            if (hasFine) add(LocationManager.PASSIVE_PROVIDER)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                add(LocationManager.FUSED_PROVIDER)
            }
        }.filter { LocationManagerCompat.hasProvider(locationManager, it) }
        for (provider in providers) {
            try {
                val loc = locationManager.getLastKnownLocation(provider)
                if (loc != null && isValidLocation(loc) && !isStale(loc)) {
                    if (bestLocation == null || isBetterLocation(loc, bestLocation)) {
                        bestLocation = loc
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "SecurityException reading last known location from $provider", e)
            } catch (e: IllegalArgumentException) {
                Log.w(TAG, "IllegalArgumentException reading last known location from $provider", e)
            }
        }
        return bestLocation
    }

    private fun handleLocationUpdate(location: Location) {
        if (!isValidLocation(location) || isStale(location)) return

        var accepted = false
        while (true) {
            val oldLoc = cachedLocation.get()
            if (!isBetterLocation(location, oldLoc)) break
            if (cachedLocation.compareAndSet(oldLoc, location)) {
                accepted = true
                Log.d(
                    TAG,
                    "Updated cached location: provider=${location.provider}, " +
                        "acc=${location.accuracy}m"
                )
                break
            }
        }

        // A rejected fix leaves the cache unchanged, so it must not end the update session.
        // Completing the deferred resumes runUpdateSession(), which stops the updates.
        if (accepted && location.accuracyOrMax <= ACCURACY_THRESHOLD_METERS) {
            accurateFixDeferred.get()?.complete(Unit)
        }
    }

    /**
     * Evaluates whether a candidate [Location] fix is superior to the currently cached fix.
     *
     * A fix more than 2 minutes newer always wins, which prevents coordinate anchoring when
     * travelling. Within the 2-minute window, a fix is accepted if it is more accurate, if it is
     * newer and at least as accurate, or if it is newer, from the same provider, and no more than
     * [SIGNIFICANT_ACCURACY_DELTA_METERS] less accurate. A fix that does not report accuracy
     * ranks below any fix that does.
     *
     * @param newLoc Candidate fix received from a platform location provider.
     * @param currentLoc The currently cached fix, or `null`.
     * @return `true` if [newLoc] should replace [currentLoc], `false` otherwise.
     */
    private fun isBetterLocation(newLoc: Location, currentLoc: Location?): Boolean {
        if (currentLoc == null) return true
        if (isStale(currentLoc)) return true

        val timeDeltaNanos = newLoc.elapsedRealtimeNanos - currentLoc.elapsedRealtimeNanos
        val isSignificantlyNewer = timeDeltaNanos > SIGNIFICANT_TIME_DELTA_NANOS
        val isSignificantlyOlder = timeDeltaNanos < -SIGNIFICANT_TIME_DELTA_NANOS

        if (isSignificantlyNewer) return true
        if (isSignificantlyOlder) return false

        val isNewer = timeDeltaNanos > 0
        val accuracyDelta = newLoc.accuracyOrMax - currentLoc.accuracyOrMax
        val isMoreAccurate = accuracyDelta < 0f
        val isLessAccurate = accuracyDelta > 0f
        val isSignificantlyLessAccurate = accuracyDelta > SIGNIFICANT_ACCURACY_DELTA_METERS
        val isFromSameProvider = newLoc.provider != null && newLoc.provider == currentLoc.provider

        return when {
            isMoreAccurate -> true
            isNewer && !isLessAccurate -> true
            isNewer && isFromSameProvider && !isSignificantlyLessAccurate -> true
            else -> false
        }
    }

    /**
     * The horizontal accuracy of this fix in meters, or [Float.MAX_VALUE] if the fix does not
     * report one. [Location.getAccuracy] returns 0 when no accuracy is set, which would otherwise
     * rank the fix as the most accurate possible.
     */
    private val Location.accuracyOrMax: Float
        get() = if (hasAccuracy()) accuracy else Float.MAX_VALUE

    private fun isValidLocation(location: Location): Boolean {
        if (location.latitude.isNaN() || location.longitude.isNaN()) return false
        if (location.latitude.isInfinite() || location.longitude.isInfinite()) return false
        if (location.latitude == 0.0 && location.longitude == 0.0) return false
        return true
    }

    private fun isStale(location: Location): Boolean {
        val ageNanos = SystemClock.elapsedRealtimeNanos() - location.elapsedRealtimeNanos
        return ageNanos > STALE_LOCATION_THRESHOLD_NANOS
    }

    private fun hasAnyLocationPermission(): Boolean {
        return hasFinePermission() || hasCoarsePermission()
    }

    /**
     * Returns `true` if a location permission is granted and location services are enabled on
     * the device. Cached fixes must not be returned when the user has turned off system location,
     * even though runtime permissions remain granted.
     */
    private fun isLocationAvailable(): Boolean {
        val locationManager = locationManager ?: return false
        return hasAnyLocationPermission() && LocationManagerCompat.isLocationEnabled(
            locationManager
        )
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

    @SuppressLint("InlinedApi")
    private fun getActiveProviders(): List<String> {
        val locationManager = locationManager ?: return emptyList()
        if (!LocationManagerCompat.isLocationEnabled(locationManager)) {
            return emptyList()
        }

        val providers = mutableListOf<String>()
        val hasFine = hasFinePermission()
        val hasCoarse = hasCoarsePermission()

        if (hasFine &&
            LocationManagerCompat.hasProvider(locationManager, LocationManager.GPS_PROVIDER) &&
            locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        ) {
            providers.add(LocationManager.GPS_PROVIDER)
        }

        if ((hasFine || hasCoarse) &&
            LocationManagerCompat.hasProvider(locationManager, LocationManager.NETWORK_PROVIDER) &&
            locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        ) {
            providers.add(LocationManager.NETWORK_PROVIDER)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            (hasFine || hasCoarse) &&
            LocationManagerCompat.hasProvider(locationManager, LocationManager.FUSED_PROVIDER) &&
            locationManager.isProviderEnabled(LocationManager.FUSED_PROVIDER)
        ) {
            providers.add(LocationManager.FUSED_PROVIDER)
        }

        return providers
    }
}
