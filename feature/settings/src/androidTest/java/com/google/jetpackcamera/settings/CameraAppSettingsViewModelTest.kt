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

import android.Manifest
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.settings.datastoreprefs.PrefsDataStoreSettingsDataSource
import com.google.jetpackcamera.core.settings.datastoreprefs.testing.FakeDataStoreModule
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import java.io.File
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
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

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
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testFile: File
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var settingsViewModel: SettingsViewModel

    @Before
    fun setup() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher())
        testFile = tempFolder.newFile("test_settings.preferences_pb")
        datastoreScope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob())

        testDataStore = FakeDataStoreModule.providePreferenceDataStore(
            scope = datastoreScope,
            file = testFile
        )

        val settingsDataSource = PrefsDataStoreSettingsDataSource(
            dataStore = testDataStore,
            defaultCaptureModeOverride = CaptureMode.STANDARD
        )
        val settingsRepository = LocalSettingsRepository(
            settingsDataSource = settingsDataSource
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

    private fun createViewModelWithConstraints(
        systemConstraints: CameraSystemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
        defaultCaptureMode: CaptureMode = CaptureMode.VIDEO_ONLY
    ): SettingsViewModel {
        val settingsDataSource = PrefsDataStoreSettingsDataSource(
            dataStore = testDataStore,
            defaultCaptureModeOverride = defaultCaptureMode
        )
        val settingsRepository = LocalSettingsRepository(
            settingsDataSource = settingsDataSource
        )
        val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
            updateSystemConstraints(systemConstraints)
        }
        return SettingsViewModel(settingsRepository, constraintsRepository).apply {
            setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))
        }
    }

    /**
     * Verifies that the Concurrent Camera setting is enabled by default
     * on devices that physically support concurrent cameras, with no other
     * conflicting settings active.
     */
    @Test
    fun concurrentCamera_whenSupported_isEnabled() = runTest(StandardTestDispatcher()) {
        val customViewModel = createViewModelWithConstraints(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(concurrentCamerasSupported = true),
            defaultCaptureMode = CaptureMode.STANDARD
        )
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
     * Verifies that the Concurrent Camera setting is disabled if Camera
     * Effect is active, since concurrent camera strictly requires no effects.
     */
    @Test
    fun concurrentCamera_whenCameraEffectIsActive_isDisabled() =
        runTest(StandardTestDispatcher()) {
            // Set selected_camera_effect to a non-empty value first
            testDataStore.edit { prefs ->
                prefs[stringPreferencesKey("selected_camera_effect")] = "single_stream"
            }

            val customViewModel = createViewModelWithConstraints(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
                    concurrentCamerasSupported = true
                )
            )
            advanceUntilIdle()

            val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            val enabledState = assertIsEnabled(uiState)

            assertThat(enabledState.concurrentCameraUiState).isInstanceOf(
                ConcurrentCameraUiState.Disabled::class.java
            )
            val disabledState =
                enabledState.concurrentCameraUiState as ConcurrentCameraUiState.Disabled
            assertThat(disabledState.disabledRationale).isInstanceOf(
                DisabledRationale.ConcurrentCameraDisabledRationale::class.java
            )
            assertThat(disabledState.disabledRationale.reasonTextResId)
                .isEqualTo(R.string.concurrent_camera_stream_config_unsupported)
        }

    /**
     * Verifies that the Concurrent Camera setting is disabled if Flash
     * Mode is set to LOW_LIGHT_BOOST (LLB), as LLB is incompatible with
     * concurrent camera mode.
     */
    @Test
    fun concurrentCamera_whenFlashLlbIsActive_isDisabled() = runTest(StandardTestDispatcher()) {
        // Set FlashMode to LOW_LIGHT_BOOST first
        testDataStore.edit { prefs ->
            prefs[stringPreferencesKey("flash_mode")] = FlashMode.LOW_LIGHT_BOOST.name
        }

        val customViewModel = createViewModelWithConstraints(
            systemConstraints = LLB_SUPPORTED_CONSTRAINTS
        )
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
            testDataStore.edit { prefs ->
                prefs[stringPreferencesKey("stabilization_mode")] = StabilizationMode.ON.name
            }

            val customViewModel = createViewModelWithConstraints(
                systemConstraints = STABILIZATION_SUPPORTED_CONSTRAINTS
            )
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
        testDataStore.edit { prefs ->
            prefs[intPreferencesKey("target_frame_rate")] = 30
        }

        val customViewModel = createViewModelWithConstraints(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(concurrentCamerasSupported = true)
        )
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
     * Verifies that Camera Effect selection is disabled if Concurrent Camera is enabled.
     */
    @Test
    fun cameraEffect_whenConcurrentCameraIsEnabled_isDisabled() =
        runTest(StandardTestDispatcher()) {
            // Set ConcurrentCameraMode to DUAL first
            testDataStore.edit { prefs ->
                prefs[
                    stringPreferencesKey(
                        "concurrent_camera_mode"
                    )
                ] = ConcurrentCameraMode.DUAL.name
            }

            val customViewModel = createViewModelWithConstraints(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
                    concurrentCamerasSupported = true
                )
            )
            advanceUntilIdle()

            val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            val enabledState = assertIsEnabled(uiState)

            assertThat(
                enabledState.cameraEffectUiState
            ).isInstanceOf(CameraEffectUiState.Disabled::class.java)
            val disabledState = enabledState.cameraEffectUiState as CameraEffectUiState.Disabled
            assertThat(disabledState.disabledRationale)
                .isInstanceOf(DisabledRationale.ConcurrentCameraActiveRationale::class.java)
        }

    /**
     * Verifies that the Low Light Boost (LLB) option in Flash Mode is
     * disabled if Concurrent Camera is enabled, as they are mutually
     * incompatible.
     */
    @Test
    fun flashLlb_whenConcurrentCameraIsEnabled_isDisabled() = runTest(StandardTestDispatcher()) {
        // Set ConcurrentCameraMode to DUAL first
        testDataStore.edit { prefs ->
            prefs[stringPreferencesKey("concurrent_camera_mode")] = ConcurrentCameraMode.DUAL.name
        }

        val customViewModel = createViewModelWithConstraints(
            systemConstraints = LLB_SUPPORTED_CONSTRAINTS
        )
        advanceUntilIdle()

        val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
        val enabledState = assertIsEnabled(uiState)

        val flashState = enabledState.flashUiState as FlashUiState.Enabled
        assertThat(flashState.lowLightSelectableState)
            .isInstanceOf(SingleSelectableState.Disabled::class.java)

        val disabledState = flashState.lowLightSelectableState as SingleSelectableState.Disabled
        assertThat(disabledState.disabledRationale)
            .isInstanceOf(DisabledRationale.ConcurrentCameraActiveRationale::class.java)
    }

    /**
     * Verifies that the Video Stabilization setting is completely disabled
     * (locked to OFF) if Concurrent Camera is enabled.
     */
    @Test
    fun stabilization_whenConcurrentCameraIsEnabled_isDisabled() =
        runTest(StandardTestDispatcher()) {
            // Set ConcurrentCameraMode to DUAL first
            testDataStore.edit { prefs ->
                prefs[
                    stringPreferencesKey(
                        "concurrent_camera_mode"
                    )
                ] = ConcurrentCameraMode.DUAL.name
            }

            val customViewModel = createViewModelWithConstraints(
                systemConstraints = STABILIZATION_SUPPORTED_CONSTRAINTS
            )
            advanceUntilIdle()

            val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            val enabledState = assertIsEnabled(uiState)

            assertThat(enabledState.stabilizationUiState)
                .isInstanceOf(StabilizationUiState.Disabled::class.java)
            val disabledState = enabledState.stabilizationUiState as StabilizationUiState.Disabled
            assertThat(disabledState.disabledRationale)
                .isInstanceOf(DisabledRationale.ConcurrentCameraActiveRationale::class.java)
        }

    /**
     * Verifies that fixed FPS options (15, 30, 60) are disabled if
     * Concurrent Camera is enabled, locking the user to auto FPS.
     */
    @Test
    fun fps_whenConcurrentCameraIsEnabled_isDisabled() = runTest(StandardTestDispatcher()) {
        // Set ConcurrentCameraMode to DUAL first
        testDataStore.edit { prefs ->
            prefs[stringPreferencesKey("concurrent_camera_mode")] = ConcurrentCameraMode.DUAL.name
        }

        val customViewModel = createViewModelWithConstraints(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(concurrentCamerasSupported = true)
        )
        advanceUntilIdle()

        val uiState = customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
        val enabledState = assertIsEnabled(uiState)

        assertThat(enabledState.fpsUiState).isInstanceOf(FpsUiState.Disabled::class.java)
        val disabledState = enabledState.fpsUiState as FpsUiState.Disabled
        assertThat(disabledState.disabledRationale)
            .isInstanceOf(DisabledRationale.ConcurrentCameraActiveRationale::class.java)
    }

    @Test
    fun cameraEffectDisabled_whenUltraHdrEnabled() = runTest(StandardTestDispatcher()) {
        // Set image format to Ultra HDR in datastore
        testDataStore.edit { prefs ->
            prefs[stringPreferencesKey("image_format")] = ImageOutputFormat.JPEG_ULTRA_HDR.name
        }
        advanceUntilIdle()

        val uiState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val cameraEffectUiState = assertIsEnabled(uiState).cameraEffectUiState
        assertThat(cameraEffectUiState).isInstanceOf(CameraEffectUiState.Disabled::class.java)
        val disabledRationale =
            (cameraEffectUiState as CameraEffectUiState.Disabled).disabledRationale
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
