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

import android.Manifest
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.dataStoreFile
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import com.google.jetpackcamera.model.proto.ConcurrentCameraMode as ConcurrentCameraModeProto
import com.google.jetpackcamera.model.proto.FlashMode as FlashModeProto
import com.google.jetpackcamera.model.proto.ImageOutputFormat as ImageOutputFormatProto
import com.google.jetpackcamera.model.proto.StabilizationMode as StabilizationModeProto
import com.google.jetpackcamera.model.proto.StreamConfig as StreamConfigProto

private val STABILIZATION_SUPPORTED_CONSTRAINTS = TYPICAL_SYSTEM_CONSTRAINTS.copy(
    concurrentCamerasSupported = true,
    perLensConstraints = buildMap {
        for (lensFacing in listOf(LensFacing.FRONT, LensFacing.BACK)) {
            put(
                lensFacing,
                TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[lensFacing]!!.copy(
                    supportedStabilizationModes = setOf(StabilizationMode.OFF, StabilizationMode.ON)
                )
            )
        }
    }
)

private val LLB_SUPPORTED_CONSTRAINTS = TYPICAL_SYSTEM_CONSTRAINTS.copy(
    concurrentCamerasSupported = true,
    perLensConstraints = buildMap {
        for (lensFacing in listOf(LensFacing.FRONT, LensFacing.BACK)) {
            put(
                lensFacing,
                TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints[lensFacing]!!.copy(
                    supportedFlashModes = setOf(
                        FlashMode.OFF,
                        FlashMode.ON,
                        FlashMode.AUTO,
                        FlashMode.LOW_LIGHT_BOOST
                    )
                )
            )
        }
    }
)

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class CameraAppSettingsViewModelTest {
    private val testContext: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var testDataStore: DataStore<JcaSettings>
    private lateinit var datastoreScope: CoroutineScope
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
        val settingsRepository = LocalSettingsRepository(
            jcaSettings = testDataStore,
            defaultCaptureModeOverride = CaptureMode.STANDARD
        )
        val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
            updateSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
        }
        settingsViewModel = SettingsViewModel(
            settingsRepository,
            constraintsRepository
        )
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
        settingsViewModel.setGrantedPermissions(
            mutableSetOf(Manifest.permission.RECORD_AUDIO)
        )
        val uiState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        assertThat(uiState).isEqualTo(
            TYPICAL_SETTINGS_UISTATE
        )
    }

    @Test
    fun setMute_permission_granted() = runTest(StandardTestDispatcher()) {
        // permission must be granted or the setting will be disabled
        // Wait for first Enabled state
        settingsViewModel.setGrantedPermissions(
            mutableSetOf(Manifest.permission.RECORD_AUDIO)
        )
        val initialState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val initialAudioState = assertIsEnabled(initialState).audioUiState
        // assert that muteUiState is Enabled
        assertThat(initialAudioState).isInstanceOf(AudioUiState.Enabled.On::class.java)

        val nextAudioUiState = AudioUiState.Enabled.Mute()
        settingsViewModel.setVideoAudio(false)

        advanceUntilIdle()

        assertIsEnabled(settingsViewModel.settingsUiState.value).also {
            assertThat(it.audioUiState).isEqualTo(nextAudioUiState)
        }
    }

    @Test
    fun setMute_permission_not_granted() = runTest(StandardTestDispatcher()) {
        // Wait for first Enabled state
        val initialState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val initialAudioState = assertIsEnabled(initialState).audioUiState
        // assert that muteUiState is disabled
        assertThat(initialAudioState).isNotInstanceOf(AudioUiState.Enabled::class.java)

        settingsViewModel.setVideoAudio(false)

        advanceUntilIdle()

        // ensure still disabled
        assertIsEnabled(settingsViewModel.settingsUiState.value).also {
            assertThat(it.audioUiState).isNotInstanceOf(AudioUiState.Enabled::class.java)
        }
    }

    @Test
    fun setDefaultToFrontCamera() = runTest(StandardTestDispatcher()) {
        // Wait for first Enabled state
        val initialState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val initialCameraLensFacing =
            assertIsEnabled(initialState).lensFlipUiState.currentLensFacing
        val nextCameraLensFacing = if (initialCameraLensFacing == LensFacing.FRONT) {
            LensFacing.BACK
        } else {
            LensFacing.FRONT
        }
        settingsViewModel.setDefaultLensFacing(nextCameraLensFacing)

        advanceUntilIdle()

        assertIsEnabled(settingsViewModel.settingsUiState.value).also {
            assertThat(it.lensFlipUiState.currentLensFacing).isEqualTo(nextCameraLensFacing)
        }
    }

    @Test
    fun setDarkMode() = runTest(StandardTestDispatcher()) {
        // Wait for first Enabled state
        val initialState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val initialDarkMode =
            (assertIsEnabled(initialState).darkModeUiState as DarkModeUiState.Enabled)
                .currentDarkMode

        settingsViewModel.setDarkMode(DarkMode.SYSTEM)

        advanceUntilIdle()

        val newDarkMode =
            (
                assertIsEnabled(settingsViewModel.settingsUiState.value)
                    .darkModeUiState as DarkModeUiState.Enabled
                )
                .currentDarkMode

        assertThat(initialDarkMode).isEqualTo(DarkMode.DARK)
        assertThat(newDarkMode).isEqualTo(DarkMode.SYSTEM)
    }

    /**
     * Verifies that the Concurrent Camera setting is enabled by default
     * on devices that physically support concurrent cameras, with no other
     * conflicting settings active.
     */
    @Test
    fun concurrentCamera_whenSupported_isEnabled() = runTest(StandardTestDispatcher()) {
        val settingsRepository = LocalSettingsRepository(
            jcaSettings = testDataStore,
            defaultCaptureModeOverride = CaptureMode.STANDARD
        )
        val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
            updateSystemConstraints(
                TYPICAL_SYSTEM_CONSTRAINTS.copy(concurrentCamerasSupported = true)
            )
        }
        val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
        customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
        advanceUntilIdle()

        val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
        val enabledState = assertIsEnabled(uiState)

        assertThat(enabledState.concurrentCameraUiState)
            .isInstanceOf(ConcurrentCameraUiState.Enabled::class.java)
        val concurrentCameraState =
            enabledState.concurrentCameraUiState as ConcurrentCameraUiState.Enabled
        assertThat(concurrentCameraState.currentConcurrentCameraMode)
            .isEqualTo(ConcurrentCameraMode.OFF)
    }

    /**
     * Verifies that the Concurrent Camera setting is disabled if Stream
     * Configuration is set to SINGLE_STREAM, since concurrent camera
     * strictly requires MULTI_STREAM.
     */
    @Test
    fun concurrentCamera_whenStreamConfigIsSingleStream_isDisabled() =
        runTest(StandardTestDispatcher()) {
            // Set StreamConfig to SINGLE_STREAM first
            testDataStore.updateData {
                it.toBuilder()
                    .setStreamConfigStatus(StreamConfigProto.STREAM_CONFIG_SINGLE_STREAM)
                    .build()
            }

            val settingsRepository = LocalSettingsRepository(
                jcaSettings = testDataStore,
                defaultCaptureModeOverride = CaptureMode.VIDEO_ONLY
            )
            val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
                updateSystemConstraints(
                    TYPICAL_SYSTEM_CONSTRAINTS.copy(concurrentCamerasSupported = true)
                )
            }
            val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
            customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
            advanceUntilIdle()

            val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            val enabledState = assertIsEnabled(uiState)

            assertThat(enabledState.concurrentCameraUiState).isInstanceOf(
                ConcurrentCameraUiState.Disabled::class.java
            )
            val disabledState =
                enabledState.concurrentCameraUiState as ConcurrentCameraUiState.Disabled
            assertThat(disabledState.disabledRationale).isInstanceOf(
                DisabledRationale.ConcurrentCameraStreamConfigRationale::class.java
            )
        }

    /**
     * Verifies that the Concurrent Camera setting is disabled if Flash
     * Mode is set to LOW_LIGHT_BOOST (LLB), as LLB is incompatible with
     * concurrent camera mode.
     */
    @Test
    fun concurrentCamera_whenFlashLlbIsActive_isDisabled() = runTest(StandardTestDispatcher()) {
        // Set FlashMode to LOW_LIGHT_BOOST first
        testDataStore.updateData {
            it.toBuilder().setFlashModeStatus(FlashModeProto.FLASH_MODE_LOW_LIGHT_BOOST).build()
        }

        val settingsRepository = LocalSettingsRepository(
            jcaSettings = testDataStore,
            defaultCaptureModeOverride = CaptureMode.VIDEO_ONLY
        )
        val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
            updateSystemConstraints(LLB_SUPPORTED_CONSTRAINTS)
        }
        val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
        customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
        advanceUntilIdle()

        val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
        val enabledState = assertIsEnabled(uiState)

        assertThat(enabledState.concurrentCameraUiState)
            .isInstanceOf(ConcurrentCameraUiState.Disabled::class.java)
        val disabledState = enabledState.concurrentCameraUiState as ConcurrentCameraUiState.Disabled
        assertThat(disabledState.disabledRationale)
            .isInstanceOf(DisabledRationale.ConcurrentCameraDisabledRationale::class.java)
        assertThat(
            disabledState.disabledRationale.reasonTextResId
        ).isEqualTo(R.string.flash_llb_active_unsupported)
    }

    /**
     * Verifies that the Concurrent Camera setting is disabled if Video
     * Stabilization is explicitly enabled, as concurrent camera does
     * not support stabilized video streams.
     */
    @Test
    fun concurrentCamera_whenStabilizationIsActive_isDisabled() =
        runTest(StandardTestDispatcher()) {
            // Set StabilizationMode to ON first
            testDataStore.updateData {
                it.toBuilder().setStabilizationMode(StabilizationModeProto.STABILIZATION_MODE_ON)
                    .build()
            }

            val settingsRepository = LocalSettingsRepository(
                jcaSettings = testDataStore,
                defaultCaptureModeOverride = CaptureMode.VIDEO_ONLY
            )
            val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
                updateSystemConstraints(STABILIZATION_SUPPORTED_CONSTRAINTS)
            }
            val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
            customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
            advanceUntilIdle()

            val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            val enabledState = assertIsEnabled(uiState)

            assertThat(
                enabledState.concurrentCameraUiState
            ).isInstanceOf(ConcurrentCameraUiState.Disabled::class.java)
            val disabledState =
                enabledState.concurrentCameraUiState as ConcurrentCameraUiState.Disabled
            assertThat(
                disabledState.disabledRationale
            ).isInstanceOf(DisabledRationale.ConcurrentCameraDisabledRationale::class.java)
            assertThat(
                disabledState.disabledRationale.reasonTextResId
            ).isEqualTo(R.string.stabilization_active_unsupported)
        }

    /**
     * Verifies that the Concurrent Camera setting is disabled if a fixed
     * frame rate is active (e.g. 30 FPS), as concurrent camera requires
     * auto frame rate resolution.
     */
    @Test
    fun concurrentCamera_whenFixedFpsIsActive_isDisabled() = runTest(StandardTestDispatcher()) {
        // Set targetFrameRate to fixed 30 FPS first
        testDataStore.updateData { it.toBuilder().setTargetFrameRate(30).build() }

        val settingsRepository = LocalSettingsRepository(
            jcaSettings = testDataStore,
            defaultCaptureModeOverride = CaptureMode.VIDEO_ONLY
        )
        val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
            updateSystemConstraints(
                TYPICAL_SYSTEM_CONSTRAINTS.copy(concurrentCamerasSupported = true)
            )
        }
        val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
        customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
        advanceUntilIdle()

        val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
        val enabledState = assertIsEnabled(uiState)

        assertThat(
            enabledState.concurrentCameraUiState
        ).isInstanceOf(ConcurrentCameraUiState.Disabled::class.java)
        val disabledState = enabledState.concurrentCameraUiState as ConcurrentCameraUiState.Disabled
        assertThat(
            disabledState.disabledRationale
        ).isInstanceOf(DisabledRationale.ConcurrentCameraDisabledRationale::class.java)
        assertThat(
            disabledState.disabledRationale.reasonTextResId
        ).isEqualTo(R.string.fixed_fps_active_unsupported)
    }

    /**
     * Verifies that Stream Configuration is disabled if Concurrent Camera is enabled.
     */
    @Test
    fun streamConfig_whenConcurrentCameraIsEnabled_isDisabled() =
        runTest(StandardTestDispatcher()) {
            // Set ConcurrentCameraMode to DUAL first
            testDataStore.updateData {
                it.toBuilder()
                    .setConcurrentCameraModeStatus(
                        ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_DUAL
                    )
                    .build()
            }

            val settingsRepository = LocalSettingsRepository(
                jcaSettings = testDataStore,
                defaultCaptureModeOverride = CaptureMode.VIDEO_ONLY
            )
            val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
                updateSystemConstraints(
                    TYPICAL_SYSTEM_CONSTRAINTS.copy(concurrentCamerasSupported = true)
                )
            }
            val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
            customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
            advanceUntilIdle()

            val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            val enabledState = assertIsEnabled(uiState)

            assertThat(
                enabledState.streamConfigUiState
            ).isInstanceOf(StreamConfigUiState.Disabled::class.java)
            val disabledState = enabledState.streamConfigUiState as StreamConfigUiState.Disabled
            assertThat(disabledState.disabledRationale)
                .isInstanceOf(DisabledRationale.ConcurrentCameraUnsupportedRationale::class.java)
        }

    /**
     * Verifies that the Low Light Boost (LLB) option in Flash Mode is
     * disabled if Concurrent Camera is enabled, as they are mutually
     * incompatible.
     */
    @Test
    fun flashLlb_whenConcurrentCameraIsEnabled_isDisabled() = runTest(StandardTestDispatcher()) {
        // Set ConcurrentCameraMode to DUAL first
        testDataStore.updateData {
            it.toBuilder()
                .setConcurrentCameraModeStatus(
                    ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_DUAL
                )
                .build()
        }

        val settingsRepository = LocalSettingsRepository(
            jcaSettings = testDataStore,
            defaultCaptureModeOverride = CaptureMode.VIDEO_ONLY
        )
        val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
            updateSystemConstraints(LLB_SUPPORTED_CONSTRAINTS)
        }
        val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
        customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
        advanceUntilIdle()

        val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
        val enabledState = assertIsEnabled(uiState)

        val flashState = enabledState.flashUiState as FlashUiState.Enabled
        assertThat(flashState.lowLightSelectableState)
            .isInstanceOf(SingleSelectableState.Disabled::class.java)

        val disabledState = flashState.lowLightSelectableState as SingleSelectableState.Disabled
        assertThat(disabledState.disabledRationale)
            .isInstanceOf(DisabledRationale.ConcurrentCameraUnsupportedRationale::class.java)
    }

    /**
     * Verifies that the Video Stabilization setting is completely disabled
     * (locked to OFF) if Concurrent Camera is enabled.
     */
    @Test
    fun stabilization_whenConcurrentCameraIsEnabled_isDisabled() =
        runTest(StandardTestDispatcher()) {
            // Set ConcurrentCameraMode to DUAL first
            testDataStore.updateData {
                it.toBuilder()
                    .setConcurrentCameraModeStatus(
                        ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_DUAL
                    )
                    .build()
            }

            val settingsRepository = LocalSettingsRepository(
                jcaSettings = testDataStore,
                defaultCaptureModeOverride = CaptureMode.VIDEO_ONLY
            )
            val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
                updateSystemConstraints(STABILIZATION_SUPPORTED_CONSTRAINTS)
            }
            val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
            customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
            advanceUntilIdle()

            val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            val enabledState = assertIsEnabled(uiState)

            assertThat(enabledState.stabilizationUiState)
                .isInstanceOf(StabilizationUiState.Disabled::class.java)
            val disabledState = enabledState.stabilizationUiState as StabilizationUiState.Disabled
            assertThat(disabledState.disabledRationale)
                .isInstanceOf(DisabledRationale.ConcurrentCameraUnsupportedRationale::class.java)
        }

    /**
     * Verifies that fixed FPS options (15, 30, 60) are disabled if
     * Concurrent Camera is enabled, locking the user to auto FPS.
     */
    @Test
    fun fps_whenConcurrentCameraIsEnabled_isDisabled() = runTest(StandardTestDispatcher()) {
        // Set ConcurrentCameraMode to DUAL first
        testDataStore.updateData {
            it.toBuilder()
                .setConcurrentCameraModeStatus(
                    ConcurrentCameraModeProto.CONCURRENT_CAMERA_MODE_DUAL
                )
                .build()
        }

        val settingsRepository = LocalSettingsRepository(
            jcaSettings = testDataStore,
            defaultCaptureModeOverride = CaptureMode.VIDEO_ONLY
        )
        val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
            updateSystemConstraints(
                TYPICAL_SYSTEM_CONSTRAINTS.copy(concurrentCamerasSupported = true)
            )
        }
        val customViewModel = SettingsViewModel(settingsRepository, constraintsRepository)
        customViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
        advanceUntilIdle()

        val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
        val enabledState = assertIsEnabled(uiState)

        assertThat(enabledState.fpsUiState).isInstanceOf(FpsUiState.Disabled::class.java)
        val disabledState = enabledState.fpsUiState as FpsUiState.Disabled
        assertThat(disabledState.disabledRationale)
            .isInstanceOf(DisabledRationale.ConcurrentCameraUnsupportedRationale::class.java)
    }

    @Test
    fun streamConfigDisabled_whenUltraHdrEnabled() = runTest(StandardTestDispatcher()) {
        // Set image format to Ultra HDR in datastore
        testDataStore.updateData { currentSettings ->
            currentSettings.toBuilder()
                .setImageFormatStatus(
                    ImageOutputFormatProto.IMAGE_OUTPUT_FORMAT_JPEG_ULTRA_HDR
                )
                .build()
        }
        advanceUntilIdle()

        val uiState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val streamConfigUiState = assertIsEnabled(uiState).streamConfigUiState
        assertThat(streamConfigUiState).isInstanceOf(StreamConfigUiState.Disabled::class.java)
        val disabledRationale =
            (streamConfigUiState as StreamConfigUiState.Disabled).disabledRationale
        assertThat(disabledRationale)
            .isInstanceOf(DisabledRationale.UltraHdrUnsupportedRationale::class.java)
    }
}

private fun assertIsEnabled(settingsUiState: SettingsUiState): SettingsUiState.Enabled =
    when (settingsUiState) {
        is SettingsUiState.Enabled -> settingsUiState
        else -> throw AssertionError(
            "SettingsUiState expected to be Enabled, but was ${settingsUiState::class}"
        )
    }
