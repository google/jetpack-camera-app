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
package com.google.jetpackcamera

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.jetpackcamera.core.common.DefaultCaptureModeOverride
import com.google.jetpackcamera.core.settings.datastoreprefs.PrefsDataStoreSettingsDataSource
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.settings.SettingsDataSource
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppSettingsModule {

    @Provides
    @Singleton
    fun providePreferencesDataStore(@ApplicationContext context: Context): DataStore<Preferences> {
        return PreferenceDataStoreFactory.create(
            produceFile = { context.preferencesDataStoreFile("app_settings.preferences_pb") }
        )
    }

    @Provides
    @Singleton
    fun provideSettingsDataSource(
        dataStore: DataStore<Preferences>,
        @DefaultCaptureModeOverride defaultCaptureMode: CaptureMode
    ): SettingsDataSource {
        return PrefsDataStoreSettingsDataSource(dataStore, defaultCaptureMode)
    }
}
