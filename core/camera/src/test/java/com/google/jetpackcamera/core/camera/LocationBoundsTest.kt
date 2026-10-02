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
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationBoundsTest {

    @Test
    fun takeIfInBounds_validCoordinates_returnsLocation() {
        val location = location(latitude = 37.4220, longitude = -122.0841)

        assertThat(location.takeIfInBounds()).isSameInstanceAs(location)
    }

    @Test
    fun takeIfInBounds_boundaryCoordinates_returnsLocation() {
        assertThat(location(latitude = 90.0, longitude = 180.0).takeIfInBounds()).isNotNull()
        assertThat(location(latitude = -90.0, longitude = -180.0).takeIfInBounds()).isNotNull()
    }

    @Test
    fun takeIfInBounds_latitudeOutOfRange_returnsNull() {
        assertThat(location(latitude = 90.5, longitude = 0.5).takeIfInBounds()).isNull()
        assertThat(location(latitude = -90.5, longitude = 0.5).takeIfInBounds()).isNull()
    }

    @Test
    fun takeIfInBounds_longitudeOutOfRange_returnsNull() {
        assertThat(location(latitude = 0.5, longitude = 180.5).takeIfInBounds()).isNull()
        assertThat(location(latitude = 0.5, longitude = -180.5).takeIfInBounds()).isNull()
    }

    @Test
    fun takeIfInBounds_nanCoordinates_returnsNull() {
        assertThat(location(latitude = Double.NaN, longitude = 0.5).takeIfInBounds()).isNull()
        assertThat(location(latitude = 0.5, longitude = Double.NaN).takeIfInBounds()).isNull()
    }

    private fun location(latitude: Double, longitude: Double) = Location("test").apply {
        this.latitude = latitude
        this.longitude = longitude
    }
}
