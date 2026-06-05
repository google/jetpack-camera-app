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
package com.google.jetpackcamera.core.settings.datastoreprefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.core.settings.datastoreprefs.testing.FakeDataStoreModule
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
class LocalSettingsDataSourceInstrumentedTest {
    @get:Rule
    val tempFolder = TemporaryFolder()
    private lateinit var testFile: File

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var dataSource: LocalSettingsDataSource

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        testFile = tempFolder.newFile("test_settings.preferences_pb")
        datastoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        testDataStore = FakeDataStoreModule.providePreferenceDataStore(
            scope = datastoreScope,
            file = testFile
        )
        dataSource = LocalSettingsDataSource(
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
    fun datasource_can_fetch_initial_settings() = runTest {
        val cameraAppSettings: CameraAppSettings = dataSource.getCurrentDefaultCameraAppSettings()
        advanceUntilIdle()
        assertThat(cameraAppSettings).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS)
    }

    @Test
    fun can_update_default_to_front_camera() = runTest {
        val initialDefaultLensFacing =
            dataSource.getCurrentDefaultCameraAppSettings().cameraLensFacing
        dataSource.updateDefaultLensFacing(LensFacing.FRONT)
        val newDefaultLensFacing = dataSource.getCurrentDefaultCameraAppSettings().cameraLensFacing
        advanceUntilIdle()

        assertThat(initialDefaultLensFacing).isEqualTo(LensFacing.BACK)
        assertThat(newDefaultLensFacing).isEqualTo(LensFacing.FRONT)
    }

    @Test
    fun can_update_flash_mode() = runTest {
        val initialFlashModeStatus = dataSource.getCurrentDefaultCameraAppSettings().flashMode
        dataSource.updateFlashModeStatus(FlashMode.ON)
        val newFlashModeStatus = dataSource.getCurrentDefaultCameraAppSettings().flashMode
        advanceUntilIdle()

        assertThat(initialFlashModeStatus).isEqualTo(FlashMode.OFF)
        assertThat(newFlashModeStatus).isEqualTo(FlashMode.ON)
    }

    @Test
    fun can_update_dynamic_range() = runTest {
        val initialDynamicRange = dataSource.getCurrentDefaultCameraAppSettings().dynamicRange
        dataSource.updateDynamicRange(dynamicRange = DynamicRange.HLG10)
        advanceUntilIdle()

        val newDynamicRange = dataSource.getCurrentDefaultCameraAppSettings().dynamicRange
        assertThat(initialDynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(newDynamicRange).isEqualTo(DynamicRange.HLG10)
    }

    @Test
    fun can_update_image_format() = runTest {
        val initialImageFormat = dataSource.getCurrentDefaultCameraAppSettings().imageFormat
        dataSource.updateImageFormat(imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR)
        advanceUntilIdle()

        val newImageFormat = dataSource.getCurrentDefaultCameraAppSettings().imageFormat
        assertThat(initialImageFormat).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(newImageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
    }
}
