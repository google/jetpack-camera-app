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
package com.google.jetpackcamera.data.settingsdatastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settingsdatastore.testing.FakeDataStoreModule
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
class LocalSettingsRepositoryInstrumentedTest {
    @get:Rule
    val tempFolder = TemporaryFolder()
    private lateinit var testFile: File

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var repository: LocalSettingsRepository

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        testFile = tempFolder.newFile()
        datastoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        testDataStore = FakeDataStoreModule.providePreferenceDataStore(
            scope = datastoreScope,
            file = testFile
        )
        repository = LocalSettingsRepository(
            dataStore = testDataStore,
            defaultCaptureModeOverride = CaptureMode.STANDARD
        )
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        datastoreScope.cancel()
    }

    @Test
    fun repository_can_fetch_initial_settings() = runTest {
        val cameraAppSettings: CameraAppSettings = repository.getCurrentDefaultCameraAppSettings()
        advanceUntilIdle()
        assertThat(cameraAppSettings).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS)
    }

    @Test
    fun can_update_default_to_front_camera() = runTest {
        val initialDefaultLensFacing =
            repository.getCurrentDefaultCameraAppSettings().cameraLensFacing
        repository.updateDefaultLensFacing(LensFacing.FRONT)
        val newDefaultLensFacing = repository.getCurrentDefaultCameraAppSettings().cameraLensFacing
        advanceUntilIdle()

        assertThat(initialDefaultLensFacing).isEqualTo(LensFacing.BACK)
        assertThat(newDefaultLensFacing).isEqualTo(LensFacing.FRONT)
    }

    @Test
    fun can_update_flash_mode() = runTest {
        val initialFlashModeStatus = repository.getCurrentDefaultCameraAppSettings().flashMode
        repository.updateFlashModeStatus(FlashMode.ON)
        val newFlashModeStatus = repository.getCurrentDefaultCameraAppSettings().flashMode
        advanceUntilIdle()

        assertThat(initialFlashModeStatus).isEqualTo(FlashMode.OFF)
        assertThat(newFlashModeStatus).isEqualTo(FlashMode.ON)
    }

    @Test
    fun can_update_dynamic_range() = runTest {
        val initialDynamicRange = repository.getCurrentDefaultCameraAppSettings().dynamicRange
        repository.updateDynamicRange(dynamicRange = DynamicRange.HLG10)
        advanceUntilIdle()

        val newDynamicRange = repository.getCurrentDefaultCameraAppSettings().dynamicRange
        assertThat(initialDynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(newDynamicRange).isEqualTo(DynamicRange.HLG10)
    }

    @Test
    fun can_update_image_format() = runTest {
        val initialImageFormat = repository.getCurrentDefaultCameraAppSettings().imageFormat
        repository.updateImageFormat(imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR)
        advanceUntilIdle()

        val newImageFormat = repository.getCurrentDefaultCameraAppSettings().imageFormat
        assertThat(initialImageFormat).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(newImageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
    }
}
