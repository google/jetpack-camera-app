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
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.SystemClock
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.settings.testing.FakeSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLocationManager

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LocationProviderImplTest {

    private lateinit var context: Context
    private lateinit var shadowLocationManager: ShadowLocationManager
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var locationProvider: LocationProviderImpl

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager = shadowOf(locationManager)

        fakeSettingsRepository = FakeSettingsRepository()
        kotlinx.coroutines.runBlocking { fakeSettingsRepository.updateLocationEnabled(true) }

        locationProvider = LocationProviderImpl(context, fakeSettingsRepository)
    }

    @Test
    fun `getCachedLocation returns null when no location cached`() {
        assertThat(locationProvider.getCachedLocation()).isNull()
    }

    @Test
    fun `getCachedLocation returns valid location`() {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 37.4220
            longitude = -122.0841
            accuracy = 10f
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowOf(context as android.app.Application)
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        locationProvider.startLocationUpdates()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        shadowLocationManager.simulateLocation(location)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertThat(locationProvider.getCachedLocation()).isNotNull()
        assertThat(locationProvider.getCachedLocation()?.latitude).isEqualTo(37.4220)
    }

    @Test
    fun `isStale rejects locations older than 30 minutes`() {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 37.4220
            longitude = -122.0841
            accuracy = 10f
            // 31 minutes ago
            elapsedRealtimeNanos =
                SystemClock.elapsedRealtimeNanos() - (31L * 60L * 1000L * 1_000_000L)
        }

        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowOf(context as android.app.Application)
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        locationProvider.startLocationUpdates()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        shadowLocationManager.simulateLocation(location)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        // Should be rejected by isStale check
        assertThat(locationProvider.getCachedLocation()).isNull()
    }

    @Test
    fun `isValidLocation rejects Null Island`() {
        val location = Location(LocationManager.GPS_PROVIDER).apply {
            latitude = 0.0
            longitude = 0.0
            accuracy = 10f
            elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
        }

        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowOf(context as android.app.Application)
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        locationProvider.startLocationUpdates()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()
        shadowLocationManager.simulateLocation(location)
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertThat(locationProvider.getCachedLocation()).isNull()
    }

    @Test
    fun `startLocationUpdates requests updates when permission granted`() = runTest {
        shadowLocationManager.setProviderEnabled(LocationManager.GPS_PROVIDER, true)
        shadowOf(context as android.app.Application)
            .grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION)

        locationProvider.startLocationUpdates()
        org.robolectric.shadows.ShadowLooper.idleMainLooper()

        assertThat(shadowLocationManager.getRequestLocationUpdateListeners())
        assertThat(shadowLocationManager.getRequestLocationUpdateListeners()).isNotEmpty()
    }
}
