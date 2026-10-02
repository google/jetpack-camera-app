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
import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLocationManager
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class LocationManagerLocationProviderTest {

    private lateinit var context: Context
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var locationProvider: LocationManagerLocationProvider
    private var updatesJob: Job? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = shadowOf(locationManager)

        shadowLocationManager.setLocationEnabled(true)
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowLocationManager.setProviderEnabled(LocationManager.NETWORK_PROVIDER, true)

        locationProvider = LocationManagerLocationProvider(context)
        ShadowLooper.idleMainLooper()
    }

    @After
    fun tearDown() {
        cancelLocationUpdates()
    }

    /**
     * Launches [LocationManagerLocationProvider.runLocationUpdates] on the main dispatcher,
     * cancelling any session that is already running.
     */
    private fun launchLocationUpdates() {
        updatesJob?.cancel()
        updatesJob = CoroutineScope(Dispatchers.Main).launch {
            locationProvider.runLocationUpdates()
        }
    }

    private fun cancelLocationUpdates() {
        updatesJob?.cancel()
        updatesJob = null
    }

    private fun grantLocationPermissions(fine: Boolean = true, coarse: Boolean = true) {
        val app = shadowOf(context as Application)
        if (fine) app.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)
        if (coarse) app.grantPermissions(Manifest.permission.ACCESS_COARSE_LOCATION)
    }

    private fun createLocation(
        provider: String = LocationManager.GPS_PROVIDER,
        latitude: Double = 37.4220,
        longitude: Double = -122.0841,
        accuracy: Float = 10f,
        elapsedRealtimeNanos: Long = SystemClock.elapsedRealtimeNanos()
    ) = Location(provider).apply {
        this.latitude = latitude
        this.longitude = longitude
        this.accuracy = accuracy
        this.elapsedRealtimeNanos = elapsedRealtimeNanos
    }

    private fun deliver(location: Location) {
        shadowLocationManager.simulateLocation(location)
        ShadowLooper.idleMainLooper()
    }

    @Test
    fun getCurrentLocation_noLocationCached_returnsNull() {
        assertThat(locationProvider.getCurrentLocation()).isNull()
    }

    @Test
    fun getCurrentLocation_locationCached_returnsValidLocation() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation())

        assertThat(locationProvider.getCurrentLocation()?.latitude).isEqualTo(37.4220)
    }

    @Test
    fun getCurrentLocation_nullIsland_returnsNull() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation(latitude = 0.0, longitude = 0.0))

        assertThat(locationProvider.getCurrentLocation()).isNull()
    }

    @Test
    fun getCurrentLocation_fixWithinThirtyMinutes_returnsLocation() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(
            createLocation(
                elapsedRealtimeNanos =
                SystemClock.elapsedRealtimeNanos() - TimeUnit.MINUTES.toNanos(29)
            )
        )

        assertThat(locationProvider.getCurrentLocation()?.latitude).isEqualTo(37.4220)
    }

    @Test
    fun getCurrentLocation_fixOlderThanThirtyMinutes_returnsNull() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(
            createLocation(
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() -
                    TimeUnit.MINUTES.toNanos(30) - TimeUnit.SECONDS.toNanos(1)
            )
        )

        assertThat(locationProvider.getCurrentLocation()).isNull()
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_bothProvidersEnabled_registersGpsAndNetwork() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        assertThat(shadowLocationManager.getLocationUpdateListeners(LocationManager.GPS_PROVIDER))
            .isNotEmpty()
        assertThat(
            shadowLocationManager.getLocationUpdateListeners(LocationManager.NETWORK_PROVIDER)
        ).isNotEmpty()
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_gpsDisabled_registersNetworkOnly() {
        grantLocationPermissions()
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, false)

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        assertThat(shadowLocationManager.getLocationUpdateListeners(LocationManager.GPS_PROVIDER))
            .isEmpty()
        assertThat(
            shadowLocationManager.getLocationUpdateListeners(LocationManager.NETWORK_PROVIDER)
        ).isNotEmpty()

        deliver(createLocation(provider = LocationManager.NETWORK_PROVIDER, accuracy = 25f))

        assertThat(locationProvider.getCurrentLocation()?.provider)
            .isEqualTo(LocationManager.NETWORK_PROVIDER)
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_systemLocationDisabled_doesNotRegister() {
        grantLocationPermissions()
        shadowLocationManager.setLocationEnabled(false)

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        assertThat(shadowLocationManager.locationUpdateListeners).isEmpty()
        assertThat(locationProvider.getCurrentLocation()).isNull()

        // A later session registers once system location is re-enabled.
        shadowLocationManager.setLocationEnabled(true)
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_permissionGrantedMidSession_registersOnNextCycle() {
        locationProvider.refreshIntervalMs = 500L

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()
        assertThat(shadowLocationManager.locationUpdateListeners).isEmpty()

        grantLocationPermissions()
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)

        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()
    }

    @Test
    fun getCurrentLocation_withoutPermission_returnsNull() {
        shadowLocationManager.setLastKnownLocation(
            LocationManager.GPS_PROVIDER,
            createLocation()
        )

        assertThat(locationProvider.getCurrentLocation()).isNull()
    }

    @Test
    fun getCurrentLocation_systemLocationDisabledWithCachedFix_returnsNull() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation())
        assertThat(locationProvider.getCurrentLocation()).isNotNull()

        shadowLocationManager.setLocationEnabled(false)

        assertThat(locationProvider.getCurrentLocation()).isNull()
    }

    @Test
    fun getCurrentLocation_coarseOnly_ignoresGpsAndPassiveLastKnownLocations() {
        grantLocationPermissions(fine = false, coarse = true)
        shadowLocationManager.setLastKnownLocation(
            LocationManager.GPS_PROVIDER,
            createLocation(provider = LocationManager.GPS_PROVIDER)
        )
        shadowLocationManager.setLastKnownLocation(
            LocationManager.PASSIVE_PROVIDER,
            createLocation(provider = LocationManager.PASSIVE_PROVIDER)
        )

        assertThat(locationProvider.getCurrentLocation()).isNull()

        shadowLocationManager.setLastKnownLocation(
            LocationManager.NETWORK_PROVIDER,
            createLocation(provider = LocationManager.NETWORK_PROVIDER, accuracy = 30f)
        )

        assertThat(locationProvider.getCurrentLocation()?.provider)
            .isEqualTo(LocationManager.NETWORK_PROVIDER)
    }

    @Test
    fun locationUpdate_significantlyNewerFix_replacesMoreAccurateFix() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L
        val baseTimeNanos = SystemClock.elapsedRealtimeNanos()

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        // A 5m fix stops the updates early.
        deliver(createLocation(accuracy = 5f, elapsedRealtimeNanos = baseTimeNanos))
        assertThat(locationProvider.getCurrentLocation()?.accuracy).isEqualTo(5f)

        // Next refresh cycle re-registers listeners.
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)

        // 40m worse than the cached fix exceeds the 20m margin and the provider differs, so
        // only the >2 minute rule can accept this fix.
        deliver(
            createLocation(
                provider = LocationManager.NETWORK_PROVIDER,
                latitude = 37.7749,
                longitude = -122.4194,
                accuracy = 45f,
                elapsedRealtimeNanos = baseTimeNanos + TimeUnit.MINUTES.toNanos(2) +
                    TimeUnit.SECONDS.toNanos(1)
            )
        )

        val current = locationProvider.getCurrentLocation()
        assertThat(current?.latitude).isEqualTo(37.7749)
        assertThat(current?.accuracy).isEqualTo(45f)
        assertThat(current?.provider).isEqualTo(LocationManager.NETWORK_PROVIDER)
    }

    @Test
    fun locationUpdate_significantlyOlderFix_isRejected() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L
        val baseTimeNanos = SystemClock.elapsedRealtimeNanos()

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation(accuracy = 15f, elapsedRealtimeNanos = baseTimeNanos))
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)

        deliver(
            createLocation(
                latitude = 37.9999,
                longitude = -122.9999,
                accuracy = 2f,
                elapsedRealtimeNanos = baseTimeNanos - TimeUnit.MINUTES.toNanos(2) -
                    TimeUnit.SECONDS.toNanos(1)
            )
        )

        assertThat(locationProvider.getCurrentLocation()?.latitude).isEqualTo(37.4220)
        assertThat(locationProvider.getCurrentLocation()?.accuracy).isEqualTo(15f)
    }

    @Test
    fun locationUpdate_withinTwoMinutes_prefersMoreAccurateFix() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L
        val baseTimeNanos = SystemClock.elapsedRealtimeNanos()

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation(accuracy = 25f, elapsedRealtimeNanos = baseTimeNanos))
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)

        deliver(
            createLocation(
                latitude = 37.4225,
                longitude = -122.0845,
                accuracy = 5f,
                elapsedRealtimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(10)
            )
        )

        assertThat(locationProvider.getCurrentLocation()?.accuracy).isEqualTo(5f)
        assertThat(locationProvider.getCurrentLocation()?.latitude).isEqualTo(37.4225)
    }

    @Test
    fun locationUpdate_withinTwoMinutes_rejectsLessAccurateFixFromOtherProvider() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L
        val baseTimeNanos = SystemClock.elapsedRealtimeNanos()

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation(accuracy = 5f, elapsedRealtimeNanos = baseTimeNanos))
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)

        // 35m worse than the cached fix exceeds the 20m margin, and the provider differs.
        deliver(
            createLocation(
                provider = LocationManager.NETWORK_PROVIDER,
                latitude = 37.4230,
                longitude = -122.0850,
                accuracy = 40f,
                elapsedRealtimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(10)
            )
        )

        val current = locationProvider.getCurrentLocation()
        assertThat(current?.accuracy).isEqualTo(5f)
        assertThat(current?.provider).isEqualTo(LocationManager.GPS_PROVIDER)
    }

    @Test
    fun locationUpdate_withinTwoMinutes_rejectsSlightlyLessAccurateFixFromOtherProvider() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L
        val baseTimeNanos = SystemClock.elapsedRealtimeNanos()

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation(accuracy = 10f, elapsedRealtimeNanos = baseTimeNanos))
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)

        // A newer fix from a different provider must be at least as accurate to replace the cache.
        deliver(
            createLocation(
                provider = LocationManager.NETWORK_PROVIDER,
                latitude = 37.4230,
                longitude = -122.0850,
                accuracy = 25f,
                elapsedRealtimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(10)
            )
        )

        val current = locationProvider.getCurrentLocation()
        assertThat(current?.accuracy).isEqualTo(10f)
        assertThat(current?.provider).isEqualTo(LocationManager.GPS_PROVIDER)
    }

    @Test
    fun locationUpdate_withinTwoMinutes_acceptsSlightlyLessAccurateFixFromSameProvider() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L
        val baseTimeNanos = SystemClock.elapsedRealtimeNanos()

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation(accuracy = 10f, elapsedRealtimeNanos = baseTimeNanos))
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)

        // 15m worse from the same provider is within the 20m margin.
        deliver(
            createLocation(
                latitude = 37.4230,
                longitude = -122.0850,
                accuracy = 25f,
                elapsedRealtimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(10)
            )
        )

        val current = locationProvider.getCurrentLocation()
        assertThat(current?.accuracy).isEqualTo(25f)
        assertThat(current?.latitude).isEqualTo(37.4230)
    }

    @Test
    fun locationUpdate_withinTwoMinutes_rejectsMuchLessAccurateFixFromSameProvider() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L
        val baseTimeNanos = SystemClock.elapsedRealtimeNanos()

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation(accuracy = 5f, elapsedRealtimeNanos = baseTimeNanos))
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)

        deliver(
            createLocation(
                latitude = 37.4230,
                longitude = -122.0850,
                accuracy = 500f,
                elapsedRealtimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(10)
            )
        )

        assertThat(locationProvider.getCurrentLocation()?.accuracy).isEqualTo(5f)
    }

    @Suppress("DEPRECATION")
    @Test
    fun locationUpdate_rejectedAccurateFix_doesNotStopUpdates() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L
        val baseTimeNanos = SystemClock.elapsedRealtimeNanos()

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation(accuracy = 5f, elapsedRealtimeNanos = baseTimeNanos))
        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)
        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()

        // Within the 50m threshold, but rejected by the comparator.
        deliver(
            createLocation(
                provider = LocationManager.NETWORK_PROVIDER,
                accuracy = 30f,
                elapsedRealtimeNanos = baseTimeNanos + TimeUnit.SECONDS.toNanos(10)
            )
        )

        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()
        assertThat(locationProvider.getCurrentLocation()?.accuracy).isEqualTo(5f)
    }

    @Suppress("DEPRECATION")
    @Test
    fun locationUpdate_staleFix_isIgnoredAndDoesNotStopUpdates() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(
            createLocation(
                accuracy = 10f,
                elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos() -
                    TimeUnit.MINUTES.toNanos(31)
            )
        )

        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()
        assertThat(locationProvider.getCurrentLocation()).isNull()
    }

    @Test
    fun getCurrentLocation_permissionRevokedWithCachedFix_returnsNull() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        deliver(createLocation())
        assertThat(locationProvider.getCurrentLocation()).isNotNull()

        shadowOf(context as Application).denyPermissions(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )

        assertThat(locationProvider.getCurrentLocation()).isNull()
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_cancelled_removesListeners() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()
        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()

        cancelLocationUpdates()
        ShadowLooper.idleMainLooper()

        assertThat(shadowLocationManager.locationUpdateListeners).isEmpty()
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_cancelledBeforeStart_doesNotRegister() {
        grantLocationPermissions()

        launchLocationUpdates()
        cancelLocationUpdates()
        ShadowLooper.idleMainLooper()

        assertThat(shadowLocationManager.locationUpdateListeners).isEmpty()
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_restartedBeforeStart_registersOnce() {
        grantLocationPermissions()

        launchLocationUpdates()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        assertThat(shadowLocationManager.getLocationUpdateListeners(LocationManager.GPS_PROVIDER))
            .hasSize(1)
        assertThat(
            shadowLocationManager.getLocationUpdateListeners(LocationManager.NETWORK_PROVIDER)
        ).hasSize(1)
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_accurateFix_stopsAndRestartsAfterRefreshInterval() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()
        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()

        deliver(createLocation(accuracy = 10f))
        assertThat(shadowLocationManager.locationUpdateListeners).isEmpty()

        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)
        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_acquisitionTimeout_stopsAndRestartsAfterRefreshInterval() {
        grantLocationPermissions()
        locationProvider.refreshIntervalMs = 500L

        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        // An 80m fix does not meet the accuracy threshold, so updates continue.
        deliver(createLocation(provider = LocationManager.NETWORK_PROVIDER, accuracy = 80f))
        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()

        ShadowLooper.idleMainLooper(60L, TimeUnit.SECONDS)
        assertThat(shadowLocationManager.locationUpdateListeners).isEmpty()

        ShadowLooper.idleMainLooper(500L, TimeUnit.MILLISECONDS)
        assertThat(shadowLocationManager.locationUpdateListeners).isNotEmpty()
    }

    @Suppress("DEPRECATION")
    @Test
    fun runLocationUpdates_cancelled_doesNotRestart() {
        grantLocationPermissions()
        launchLocationUpdates()
        ShadowLooper.idleMainLooper()

        cancelLocationUpdates()
        ShadowLooper.idleMainLooper(10L, TimeUnit.MINUTES)

        assertThat(shadowLocationManager.locationUpdateListeners).isEmpty()
    }

    @Test
    fun locationServiceUnavailable_doesNotCrashAndReturnsNull() {
        grantLocationPermissions()
        val noLocationContext = object : ContextWrapper(context) {
            override fun getSystemService(name: String): Any? =
                if (name == LOCATION_SERVICE) null else super.getSystemService(name)
        }

        val provider = LocationManagerLocationProvider(noLocationContext)
        updatesJob = CoroutineScope(Dispatchers.Main).launch { provider.runLocationUpdates() }
        ShadowLooper.idleMainLooper()

        assertThat(provider.getCurrentLocation()).isNull()
        assertThat(shadowLocationManager.locationUpdateListeners).isEmpty()
    }
}
