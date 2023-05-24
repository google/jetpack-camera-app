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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.jetpackcamera.settings.DataStoreModule.provideDataStore
import com.google.jetpackcamera.settings.model.DarkModeStatus
import com.google.jetpackcamera.settings.model.Settings
import com.google.jetpackcamera.settings.model.getDefaultSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*
import org.junit.Before


/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LocalSettingsRepositoryInstrumentedTest {
    private val testContext: Context = ApplicationProvider.getApplicationContext()
    private lateinit var testDataStore: DataStore<JcaSettings>
    private lateinit var repository: LocalSettingsRepository


    @Before
    fun setup() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher())
        testDataStore = provideDataStore(testContext)
        repository = LocalSettingsRepository(testDataStore)
        advanceUntilIdle()
    }

    @Test
    fun repository_can_fetch_initial_datastore() = runTest(StandardTestDispatcher()) {
        //assertEquals(repository.getSettings(), expectedDefaultSettings)
         var settings:Settings = repository.getSettings()

        advanceUntilIdle()
        assertTrue(settings == getDefaultSettings())
    }

    @Test
    fun can_update_dark_mode() = runTest(StandardTestDispatcher()) {
        //assertEquals(repository.getSettings(), expectedDefaultSettings)
        var initialDarkModeStatus = repository.getSettings().dark_mode_status
        repository.updateDarkModeStatus(DarkModeStatus.LIGHT)
        val newDarkModeStatus = repository.getSettings().dark_mode_status

        advanceUntilIdle()
        assertFalse(initialDarkModeStatus == newDarkModeStatus)
        assertTrue(initialDarkModeStatus == DarkModeStatus.SYSTEM)
        assertTrue(newDarkModeStatus == DarkModeStatus.LIGHT)
    }

    @Test
    fun can_update_default_to_front_camera() = runTest (StandardTestDispatcher()) {
        // default to front camera starts false
        val initalFrontCameraDefault = repository.getSettings().default_front_camera
        repository.updateDefaultToFrontCamera()
        // default to front camera is now true
        val frontCameraDefault = repository.getSettings().default_front_camera
        advanceUntilIdle()

        assertFalse(initalFrontCameraDefault)
        assertTrue(frontCameraDefault)
    }
}