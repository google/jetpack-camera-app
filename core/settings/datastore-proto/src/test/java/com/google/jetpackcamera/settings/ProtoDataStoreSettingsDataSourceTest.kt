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
package com.google.jetpackcamera.settings

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.proto.CameraAppSettings as CameraAppSettingsProto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ProtoDataStoreSettingsDataSourceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testDataStore: DataStore<CameraAppSettingsProto>
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var repository: ProtoDataStoreSettingsDataSource

    @Before
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
        datastoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        testDataStore = DataStoreFactory.create(
            serializer = ProtoCameraAppSettingsSerializer,
            scope = datastoreScope
        ) {
            java.io.File(tempFolder.root, "test_jca_settings.pb")
        }
        repository = ProtoDataStoreSettingsDataSource(
            jcaSettings = testDataStore,
            defaultCaptureModeOverride = CaptureMode.STANDARD
        )
    }

    @After
    fun tearDown() {
        datastoreScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun repository_can_fetch_initial_datastore() = runTest {
        val cameraAppSettings: CameraAppSettings = repository.getCurrentDefaultCameraAppSettings()

        advanceUntilIdle()
        assertThat(cameraAppSettings).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS)
    }

    @Test
    fun can_update_dark_mode() = runTest {
        val initialDarkModeStatus = repository.getCurrentDefaultCameraAppSettings().darkMode
        repository.updateDarkModeStatus(DarkMode.LIGHT)
        val newDarkModeStatus = repository.getCurrentDefaultCameraAppSettings().darkMode

        advanceUntilIdle()
        assertThat(initialDarkModeStatus).isEqualTo(DarkMode.DARK)
        assertThat(newDarkModeStatus).isEqualTo(DarkMode.LIGHT)
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

    @Test
    fun can_update_aspect_ratio() = runTest {
        val initial = repository.getCurrentDefaultCameraAppSettings().aspectRatio
        repository.updateAspectRatio(AspectRatio.ONE_ONE)
        advanceUntilIdle()
        val new = repository.getCurrentDefaultCameraAppSettings().aspectRatio

        assertThat(initial).isEqualTo(AspectRatio.NINE_SIXTEEN)
        assertThat(new).isEqualTo(AspectRatio.ONE_ONE)
    }

    @Test
    fun can_update_low_light_boost_priority() = runTest {
        val initial = repository.getCurrentDefaultCameraAppSettings().lowLightBoostPriority
        repository.updateLowLightBoostPriority(
            LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES
        )
        advanceUntilIdle()
        val new = repository.getCurrentDefaultCameraAppSettings().lowLightBoostPriority

        assertThat(initial).isEqualTo(LowLightBoostPriority.PRIORITIZE_AE_MODE)
        assertThat(new).isEqualTo(LowLightBoostPriority.PRIORITIZE_GOOGLE_PLAY_SERVICES)
    }

    @Test
    fun can_update_stabilization_mode() = runTest {
        val initial = repository.getCurrentDefaultCameraAppSettings().stabilizationMode
        repository.updateStabilizationMode(StabilizationMode.ON)
        advanceUntilIdle()
        val new = repository.getCurrentDefaultCameraAppSettings().stabilizationMode

        assertThat(initial).isEqualTo(StabilizationMode.AUTO)
        assertThat(new).isEqualTo(StabilizationMode.ON)
    }

    @Test
    fun can_update_target_frame_rate() = runTest {
        val initial = repository.getCurrentDefaultCameraAppSettings().targetFrameRate
        repository.updateTargetFrameRate(60)
        advanceUntilIdle()
        val new = repository.getCurrentDefaultCameraAppSettings().targetFrameRate

        assertThat(initial).isEqualTo(DEFAULT_CAMERA_APP_SETTINGS.targetFrameRate)
        assertThat(new).isEqualTo(60)
    }

    @Test
    fun can_update_max_video_duration() = runTest {
        val initial = repository.getCurrentDefaultCameraAppSettings().maxVideoDurationMillis
        repository.updateMaxVideoDuration(10000L)
        advanceUntilIdle()
        val new = repository.getCurrentDefaultCameraAppSettings().maxVideoDurationMillis

        assertThat(initial).isEqualTo(0L) // UNLIMITED_VIDEO_DURATION is 0L
        assertThat(new).isEqualTo(10000L)
    }

    @Test
    fun can_update_video_quality() = runTest {
        val initial = repository.getCurrentDefaultCameraAppSettings().videoQuality
        repository.updateVideoQuality(VideoQuality.FHD)
        advanceUntilIdle()
        val new = repository.getCurrentDefaultCameraAppSettings().videoQuality

        assertThat(initial).isEqualTo(VideoQuality.UNSPECIFIED)
        assertThat(new).isEqualTo(VideoQuality.FHD)
    }

    @Test
    fun can_update_audio_enabled() = runTest {
        val initial = repository.getCurrentDefaultCameraAppSettings().audioEnabled
        repository.updateAudioEnabled(false)
        advanceUntilIdle()
        val new = repository.getCurrentDefaultCameraAppSettings().audioEnabled

        assertThat(initial).isTrue()
        assertThat(new).isFalse()
    }

    @Test
    fun can_update_concurrent_camera_mode() = runTest {
        val initial = repository.getCurrentDefaultCameraAppSettings().concurrentCameraMode
        repository.updateConcurrentCameraMode(
            com.google.jetpackcamera.model.ConcurrentCameraMode.DUAL
        )
        advanceUntilIdle()
        val new = repository.getCurrentDefaultCameraAppSettings().concurrentCameraMode

        assertThat(initial).isEqualTo(com.google.jetpackcamera.model.ConcurrentCameraMode.OFF)
        assertThat(new).isEqualTo(com.google.jetpackcamera.model.ConcurrentCameraMode.DUAL)
    }
}
