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
package com.google.jetpackcamera.core.location.locationmanager.di

import android.content.Context
import com.google.jetpackcamera.core.location.LocationProvider
import com.google.jetpackcamera.core.location.locationmanager.LocationManagerLocationProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module binding [LocationManagerLocationProvider] as the concrete [LocationProvider].
 *
 * This module is an explicit opt-in: applications that want framework
 * [android.location.LocationManager] backed geotagging depend on this module, while the optional
 * contract declared in `:core:location:location-di` keeps location features dormant for
 * applications that do not.
 *
 * Including this module merges [android.Manifest.permission.ACCESS_COARSE_LOCATION] into the app
 * manifest. To obtain precise location, declare
 * [android.Manifest.permission.ACCESS_FINE_LOCATION] in the app manifest and request it at runtime
 * together with [android.Manifest.permission.ACCESS_COARSE_LOCATION]; no other changes are needed.
 * See [LocationManagerLocationProvider] for details.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object LocationManagerModule {

    /**
     * Provides the singleton [LocationManagerLocationProvider] instance to satisfy
     * the optional location provider binding.
     */
    @Provides
    @Singleton
    fun provideLocationProvider(@ApplicationContext context: Context): LocationProvider {
        return LocationManagerLocationProvider(context)
    }
}
