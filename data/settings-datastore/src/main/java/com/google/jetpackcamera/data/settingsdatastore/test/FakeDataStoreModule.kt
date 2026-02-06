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
package com.google.jetpackcamera.data.settingsdatastore.test

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import com.google.jetpackcamera.settings.JcaSettings
import java.io.File
import kotlinx.coroutines.CoroutineScope

/** test implementation of DataStoreModule */
object FakeDataStoreModule {

    fun provideDataStore(
        scope: CoroutineScope,
        serializer: FakeJcaSettingsSerializer,
        file: File
    ): DataStore<JcaSettings> = DataStoreFactory.create(
        corruptionHandler = ReplaceFileCorruptionHandler { JcaSettings.getDefaultInstance() },
        scope = scope,
        serializer = serializer,
        produceFile = { file }
    )
}
