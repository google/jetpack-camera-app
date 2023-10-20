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

import androidx.datastore.core.DataStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.settings.test.FakeDataStoreModule
import com.google.jetpackcamera.settings.test.FakeJcaSettingsSerializer
import java.io.File
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class DataStoreModuleTest {
    @get:Rule
    val tempFolder = TemporaryFolder()
    private lateinit var testFile: File

    @Before
    fun setUp() {
        testFile = tempFolder.newFile()
    }

    @Test
    fun dataStoreModule_read_can_handle_corrupted_file() = runTest {
        // should handle exception and replace file information
        val dataStore: DataStore<JcaSettings> = FakeDataStoreModule.provideDataStore(
            scope = this,
            serializer = FakeJcaSettingsSerializer(failReadWithCorruptionException = true),
            file = testFile
        )
        val datastoreValue = dataStore.data.first()
        advanceUntilIdle()

        assertEquals(datastoreValue, JcaSettings.getDefaultInstance())
    }
}
