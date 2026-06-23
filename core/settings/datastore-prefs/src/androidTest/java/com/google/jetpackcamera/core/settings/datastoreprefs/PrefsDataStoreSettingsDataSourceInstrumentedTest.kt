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
import com.google.jetpackcamera.core.settings.datastoreprefs.testing.FakeDataStoreModule
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.UNLIMITED_VIDEO_DURATION
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
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
class PrefsDataStoreSettingsDataSourceInstrumentedTest {
    @get:Rule
    val tempFolder = TemporaryFolder()
    private lateinit var testFile: File

    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var dataSource: PrefsDataStoreSettingsDataSource

    @Before
    fun setup() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        testFile = tempFolder.newFile("test_settings.preferences_pb")
        datastoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        testDataStore = FakeDataStoreModule.providePreferenceDataStore(
            scope = datastoreScope,
            file = testFile
        )
        dataSource = PrefsDataStoreSettingsDataSource(
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

    @Test
    fun can_update_dark_mode() = runTest {
        val initialDarkMode = dataSource.getCurrentDefaultCameraAppSettings().darkMode
        dataSource.updateDarkModeStatus(DarkMode.LIGHT)
        advanceUntilIdle()

        val newDarkMode = dataSource.getCurrentDefaultCameraAppSettings().darkMode
        assertThat(initialDarkMode).isEqualTo(DarkMode.DARK)
        assertThat(newDarkMode).isEqualTo(DarkMode.LIGHT)
    }

    @Test
    fun can_update_target_frame_rate() = runTest {
        val initialFrameRate = dataSource.getCurrentDefaultCameraAppSettings().targetFrameRate
        dataSource.updateTargetFrameRate(30)
        advanceUntilIdle()

        val newFrameRate = dataSource.getCurrentDefaultCameraAppSettings().targetFrameRate
        assertThat(initialFrameRate).isEqualTo(0)
        assertThat(newFrameRate).isEqualTo(30)
    }

    @Test
    fun can_update_aspect_ratio() = runTest {
        val initialAspectRatio = dataSource.getCurrentDefaultCameraAppSettings().aspectRatio
        dataSource.updateAspectRatio(AspectRatio.THREE_FOUR)
        advanceUntilIdle()

        val newAspectRatio = dataSource.getCurrentDefaultCameraAppSettings().aspectRatio
        assertThat(initialAspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
        assertThat(newAspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
    }

    @Test
    fun can_update_selected_camera_effect() = runTest {
        val initialEffect = dataSource.getCurrentDefaultCameraAppSettings().selectedCameraEffect
        dataSource.updateSelectedCameraEffect("test_effect")
        advanceUntilIdle()

        val newEffect = dataSource.getCurrentDefaultCameraAppSettings().selectedCameraEffect
        assertThat(initialEffect).isEqualTo("")
        assertThat(newEffect).isEqualTo("test_effect")
    }

    @Test
    fun can_update_stabilization_mode() = runTest {
        val initialStabilizationMode =
            dataSource.getCurrentDefaultCameraAppSettings().stabilizationMode
        dataSource.updateStabilizationMode(StabilizationMode.HIGH_QUALITY)
        advanceUntilIdle()

        val newStabilizationMode =
            dataSource.getCurrentDefaultCameraAppSettings().stabilizationMode
        assertThat(initialStabilizationMode).isEqualTo(StabilizationMode.AUTO)
        assertThat(newStabilizationMode).isEqualTo(StabilizationMode.HIGH_QUALITY)
    }

    @Test
    fun can_update_max_video_duration() = runTest {
        val initialDuration =
            dataSource.getCurrentDefaultCameraAppSettings().maxVideoDurationMillis
        dataSource.updateMaxVideoDuration(60000L)
        advanceUntilIdle()

        val newDuration =
            dataSource.getCurrentDefaultCameraAppSettings().maxVideoDurationMillis
        assertThat(initialDuration).isEqualTo(UNLIMITED_VIDEO_DURATION)
        assertThat(newDuration).isEqualTo(60000L)
    }

    @Test
    fun can_update_video_quality() = runTest {
        val initialVideoQuality =
            dataSource.getCurrentDefaultCameraAppSettings().videoQuality
        dataSource.updateVideoQuality(VideoQuality.HD)
        advanceUntilIdle()

        val newVideoQuality =
            dataSource.getCurrentDefaultCameraAppSettings().videoQuality
        assertThat(initialVideoQuality).isEqualTo(VideoQuality.UNSPECIFIED)
        assertThat(newVideoQuality).isEqualTo(VideoQuality.HD)
    }

    @Test
    fun can_update_low_light_boost_priority() = runTest {
        val initialLowLightBoostPriority =
            dataSource.getCurrentDefaultCameraAppSettings().lowLightBoostPriority
        dataSource.updateLowLightBoostPriority(
            LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES
        )
        advanceUntilIdle()

        val newLowLightBoostPriority =
            dataSource.getCurrentDefaultCameraAppSettings().lowLightBoostPriority
        assertThat(initialLowLightBoostPriority)
            .isEqualTo(LowLightBoostPriority.PRIORITIZE_AE_MODE)
        assertThat(newLowLightBoostPriority)
            .isEqualTo(LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES)
    }

    @Test
    fun can_update_audio_enabled() = runTest {
        val initialAudioEnabled = dataSource.getCurrentDefaultCameraAppSettings().audioEnabled
        dataSource.updateAudioEnabled(false)
        advanceUntilIdle()

        val newAudioEnabled = dataSource.getCurrentDefaultCameraAppSettings().audioEnabled
        assertThat(initialAudioEnabled).isTrue()
        assertThat(newAudioEnabled).isFalse()
    }
}
