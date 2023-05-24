/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.google.jetpackcamera.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

// with hilt will ensure datastore instance access is unique per file
@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {
    private const val FILE_LOCATION = "app_settings.pb"

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<JcaSettings> =
        DataStoreFactory.create(
            corruptionHandler = ReplaceFileCorruptionHandler { JcaSettings.getDefaultInstance() },
            //TODO(kimblebee@): Inject coroutine scope once module providing default IO dispatcher scope is implemented
            scope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
            serializer = JcaSettingsSerializer,
            produceFile = {
                context.dataStoreFile(FILE_LOCATION)
            })

    }

