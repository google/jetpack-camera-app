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
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.LensFacing
import java.io.File
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
internal class CameraAppSettingsViewModelTest {
    private val testContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testDataStore: DataStore<JcaSettings>
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var repository: LocalSettingsRepository
    private lateinit var settingsViewModel: SettingsViewModel

    @Before
    fun setup() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher())
        datastoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        testDataStore = DataStoreFactory.create(
            serializer = JcaSettingsSerializer,
            scope = datastoreScope
        ) {
            testContext.dataStoreFile("test_jca_settings.pb")
        }
        repository = LocalSettingsRepository(testDataStore)
        settingsViewModel = SettingsViewModel(repository)
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        File(
            ApplicationProvider.getApplicationContext<Context>().filesDir,
            "datastore"
        ).deleteRecursively()

        datastoreScope.cancel()
    }

    @Test
    fun getSettingsUiState() = runTest(StandardTestDispatcher()) {
        // giving ViewModel time to call init, otherwise settings will stay disabled
        delay(100)
        val uiState = settingsViewModel.settingsUiState.value
        advanceUntilIdle()
        assertEquals(
            uiState,
            SettingsUiState(cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS, disabled = false)
        )
    }

    @Test
    fun setDefaultToFrontCamera() = runTest(StandardTestDispatcher()) {
        val initialCameraLensFacing =
            settingsViewModel.settingsUiState.value.cameraAppSettings.cameraLensFacing
        val nextCameraLensFacing = if (initialCameraLensFacing == LensFacing.FRONT) {
            LensFacing.BACK
        } else {
            LensFacing.FRONT
        }
        settingsViewModel.setDefaultLensFacing(nextCameraLensFacing)

        advanceUntilIdle()

        assertThat(settingsViewModel.settingsUiState.value.cameraAppSettings.cameraLensFacing)
            .isEqualTo(nextCameraLensFacing)
    }

    @Test
    fun setDarkMode() = runTest(StandardTestDispatcher()) {
        val initialDarkMode = settingsViewModel.settingsUiState.value.cameraAppSettings.darkMode
        settingsViewModel.setDarkMode(DarkMode.DARK)
        advanceUntilIdle()

        val newDarkMode = settingsViewModel.settingsUiState.value.cameraAppSettings.darkMode

        assertEquals(initialDarkMode, DarkMode.SYSTEM)
        assertEquals(DarkMode.DARK, newDarkMode)
    }
}
