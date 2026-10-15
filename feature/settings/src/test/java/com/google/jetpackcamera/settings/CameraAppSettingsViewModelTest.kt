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
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.location.testing.FakeLocationProvider
import com.google.jetpackcamera.core.settings.datastoreprefs.PrefsDataStoreSettingsDataSource
import com.google.jetpackcamera.core.settings.datastoreprefs.testing.FakeDataStoreModule
import com.google.jetpackcamera.model.CameraEffectId
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.NONE_EFFECT_ID
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.TARGET_FPS_30
import com.google.jetpackcamera.model.TARGET_FPS_AUTO
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.testing.FakeActiveCaptureSubModeRepository
import com.google.jetpackcamera.settings.testing.FakeConstraintsRepository
import com.google.jetpackcamera.settings.testing.FakeSettingsRepository
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FLASH_TAG
import com.google.jetpackcamera.settings.ui.FlashModeSetting
import java.io.File
import java.util.Optional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
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

private val TEST_SUB_MODE_ID = CaptureSubModeId("test_sub_mode")
private const val TEST_SUB_MODE_LABEL_RES_ID = 42
private val TEST_EFFECT_ID = CameraEffectId("test_effect")

/**
 * Constraints that support Low Light Boost, stabilization, and [TEST_EFFECT_ID], with a registered
 * capture sub-mode whose policy is [policy].
 */
private fun constraintsWithSubModePolicy(policy: CameraFeaturePolicy): CameraSystemConstraints =
    TYPICAL_SYSTEM_CONSTRAINTS.copy(
        perLensConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints.mapValues {
            it.value.copy(
                supportedFlashModes = setOf(
                    FlashMode.OFF,
                    FlashMode.ON,
                    FlashMode.AUTO,
                    FlashMode.LOW_LIGHT_BOOST
                ),
                supportedStabilizationModes = setOf(StabilizationMode.OFF, StabilizationMode.ON),
                supportedEffects = setOf(TEST_EFFECT_ID)
            )
        },
        captureSubModeDescriptors = mapOf(
            TEST_SUB_MODE_ID to CaptureSubModeDescriptor(
                id = TEST_SUB_MODE_ID,
                parentCaptureMode = CaptureMode.VIDEO_ONLY,
                labelResId = TEST_SUB_MODE_LABEL_RES_ID
            )
        ),
        captureSubModePolicies = mapOf(TEST_SUB_MODE_ID to policy)
    )

private val HIDING_SUB_MODE_POLICY = CameraFeaturePolicy(
    flashMode = SettingConfig(
        defaultValue = FlashMode.OFF,
        visibility = OptionVisibility.from(FlashMode.OFF, FlashMode.ON, FlashMode.AUTO)
    ),
    stabilizationMode = SettingConfig(StabilizationMode.OFF, OptionVisibility.Hidden),
    targetFrameRate = SettingConfig(TARGET_FPS_AUTO, OptionVisibility.Hidden),
    cameraEffect = SettingConfig(NONE_EFFECT_ID, OptionVisibility.Hidden)
)

private fun subModeRationale(affectedSettingNameResId: Int) =
    DisabledRationale.CaptureSubModeActiveRationale(
        affectedSettingNameResId = affectedSettingNameResId,
        captureSubModeLabelResId = TEST_SUB_MODE_LABEL_RES_ID
    )

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
internal class CameraAppSettingsViewModelTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var testFile: File
    private lateinit var testDataStore: DataStore<Preferences>
    private lateinit var datastoreScope: CoroutineScope
    private lateinit var settingsViewModel: SettingsViewModel
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var constraintsRepository: FakeConstraintsRepository

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
        settingsRepository = LocalSettingsRepository(
            settingsDataSource = settingsDataSource
        )
        constraintsRepository = FakeConstraintsRepository(TYPICAL_SYSTEM_CONSTRAINTS)
        settingsViewModel = SettingsViewModel(
            settingsRepository,
            constraintsRepository,
            FakeActiveCaptureSubModeRepository(),
            Optional.of(FakeLocationProvider())
        )
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        datastoreScope.cancel()
    }

    @Test
    fun locationSetting_whenLocationProviderNotPresent_isHidden() =
        runTest(StandardTestDispatcher()) {
            val vm = SettingsViewModel(
                settingsRepository,
                constraintsRepository,
                FakeActiveCaptureSubModeRepository(),
                Optional.empty()
            )
            val uiState = vm.settingsUiState.first { it is SettingsUiState.Enabled }
            assertThat(assertIsEnabled(uiState).locationUiState).isEqualTo(LocationUiState.Hidden)
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

    /**
     * Verifies that the settings UI state stays loading, rather than showing an empty screen,
     * until the camera system constraints become available.
     */
    @Test
    fun settingsUiState_whenConstraintsUnavailable_isLoading() = runTest(StandardTestDispatcher()) {
        val constraintsRepository = FakeConstraintsRepository()
        val customViewModel = SettingsViewModel(
            FakeSettingsRepository(),
            constraintsRepository,
            FakeActiveCaptureSubModeRepository()
        )
        backgroundScope.launch { customViewModel.settingsUiState.collect {} }
        advanceUntilIdle()

        assertThat(customViewModel.settingsUiState.value).isEqualTo(SettingsUiState.Loading)

        constraintsRepository.setSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
        advanceUntilIdle()

        assertThat(customViewModel.settingsUiState.value)
            .isInstanceOf(SettingsUiState.Enabled::class.java)
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
    fun setLocation_permission_granted() = runTest(StandardTestDispatcher()) {
        // Wait for first Enabled state
        settingsViewModel.setGrantedPermissions(
            mutableSetOf(Manifest.permission.ACCESS_FINE_LOCATION)
        )
        val initialState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val initialLocationState = assertIsEnabled(initialState).locationUiState
        // assert that locationUiState is Enabled.Off
        assertThat(initialLocationState).isInstanceOf(LocationUiState.Enabled.Off::class.java)

        val nextLocationUiState = LocationUiState.Enabled.On
        settingsViewModel.setLocationEnabled(true)

        advanceUntilIdle()

        assertIsEnabled(settingsViewModel.settingsUiState.value).also {
            assertThat(it.locationUiState).isEqualTo(nextLocationUiState)
        }
    }

    @Test
    fun setLocation_permission_not_granted() = runTest(StandardTestDispatcher()) {
        // Wait for first Enabled state
        val initialState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val initialLocationState = assertIsEnabled(initialState).locationUiState
        // assert that locationUiState is disabled
        assertThat(initialLocationState).isNotInstanceOf(LocationUiState.Enabled::class.java)

        settingsViewModel.setLocationEnabled(true)

        advanceUntilIdle()

        // ensure still disabled
        assertIsEnabled(settingsViewModel.settingsUiState.value).also {
            assertThat(it.locationUiState).isNotInstanceOf(LocationUiState.Enabled::class.java)
        }
    }

    @Test
    fun setLocation_coarsePermissionOnly_granted() = runTest(StandardTestDispatcher()) {
        settingsViewModel.setGrantedPermissions(
            mutableSetOf(Manifest.permission.ACCESS_COARSE_LOCATION)
        )
        advanceUntilIdle()

        val initialState = settingsViewModel.settingsUiState.first {
            it is SettingsUiState.Enabled
        }

        val initialLocationState = assertIsEnabled(initialState).locationUiState
        assertThat(initialLocationState).isInstanceOf(LocationUiState.Enabled.Off::class.java)

        settingsViewModel.setLocationEnabled(true)
        advanceUntilIdle()

        assertIsEnabled(settingsViewModel.settingsUiState.value).also {
            assertThat(it.locationUiState).isEqualTo(LocationUiState.Enabled.On)
        }
    }

    @Test
    fun locationUiState_permissionRevoked_disablesLocationUiState() =
        runTest(StandardTestDispatcher()) {
            // Start with permission granted and location enabled
            settingsViewModel.setGrantedPermissions(
                mutableSetOf(Manifest.permission.ACCESS_FINE_LOCATION)
            )
            settingsViewModel.setLocationEnabled(true)
            advanceUntilIdle()

            val stateWithPerm = assertIsEnabled(
                settingsViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            )
            assertThat(stateWithPerm.locationUiState).isEqualTo(LocationUiState.Enabled.On)

            // Revoke location permission
            settingsViewModel.setGrantedPermissions(mutableSetOf())
            advanceUntilIdle()

            val stateWithoutPerm = assertIsEnabled(settingsViewModel.settingsUiState.value)
            assertThat(stateWithoutPerm.locationUiState)
                .isInstanceOf(LocationUiState.Disabled::class.java)
            val disabledRationale =
                (stateWithoutPerm.locationUiState as LocationUiState.Disabled).disabledRationale
            assertThat(disabledRationale)
                .isInstanceOf(DisabledRationale.PermissionLocationNotGrantedRationale::class.java)
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
        defaultCaptureMode: CaptureMode = CaptureMode.VIDEO_ONLY,
        activeCaptureSubModeRepository: FakeActiveCaptureSubModeRepository =
            FakeActiveCaptureSubModeRepository()
    ): SettingsViewModel {
        val settingsDataSource = PrefsDataStoreSettingsDataSource(
            dataStore = testDataStore,
            defaultCaptureModeOverride = defaultCaptureMode
        )
        val settingsRepository = LocalSettingsRepository(
            settingsDataSource = settingsDataSource
        )
        return SettingsViewModel(
            settingsRepository,
            FakeConstraintsRepository(systemConstraints),
            activeCaptureSubModeRepository
        ).apply {
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
    fun concurrentCamera_whenCameraEffectIsActive_isDisabled() = runTest(StandardTestDispatcher()) {
        val testEffectId = com.google.jetpackcamera.model.CameraEffectId("fake_effect")
        // Set selected_camera_effect to a non-empty value first
        testDataStore.edit { prefs ->
            prefs[stringPreferencesKey("selected_camera_effect")] = testEffectId.value
        }

        val customViewModel = createViewModelWithConstraints(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
                concurrentCamerasSupported = true,
                perLensConstraints =
                TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints.mapValues { (_, constraints) ->
                    constraints.copy(supportedEffects = setOf(testEffectId))
                }
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

    /**
     * Verifies that while a capture sub-mode is active, settings that its policy hides are
     * disabled, and options that its policy excludes are disabled, with a rationale naming the
     * sub-mode.
     */
    @Test
    fun settings_whenCaptureSubModeActive_disableSettingsRestrictedByItsPolicy() =
        runTest(StandardTestDispatcher()) {
            val customViewModel = createViewModelWithConstraints(
                systemConstraints = constraintsWithSubModePolicy(HIDING_SUB_MODE_POLICY),
                activeCaptureSubModeRepository = FakeActiveCaptureSubModeRepository(
                    TEST_SUB_MODE_ID
                )
            )
            advanceUntilIdle()

            val enabledState = assertIsEnabled(
                customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            )

            assertThat(enabledState.fpsUiState)
                .isEqualTo(FpsUiState.Disabled(subModeRationale(R.string.fps_rationale_prefix)))
            assertThat(enabledState.stabilizationUiState).isEqualTo(
                StabilizationUiState.Disabled(
                    subModeRationale(R.string.stabilization_rationale_prefix)
                )
            )
            assertThat(enabledState.cameraEffectUiState).isEqualTo(
                CameraEffectUiState.Disabled(
                    subModeRationale(R.string.stream_config_rationale_prefix)
                )
            )
            val flashState = enabledState.flashUiState as FlashUiState.Enabled
            assertThat(flashState.onSelectableState).isEqualTo(SingleSelectableState.Selectable)
            assertThat(flashState.autoSelectableState).isEqualTo(SingleSelectableState.Selectable)
            assertThat(flashState.lowLightSelectableState).isEqualTo(
                SingleSelectableState.Disabled(
                    subModeRationale(R.string.flash_llb_rationale_prefix)
                )
            )
        }

    /**
     * Verifies that an [OptionVisibility.Only] restriction disables only the excluded options,
     * and that options already disabled for another reason keep that reason.
     */
    @Test
    fun fps_whenCaptureSubModeAllowsOnlySomeOptions_disablesExcludedOptions() =
        runTest(StandardTestDispatcher()) {
            val policy = CameraFeaturePolicy(
                targetFrameRate = SettingConfig(
                    defaultValue = TARGET_FPS_AUTO,
                    visibility = OptionVisibility.Only(TARGET_FPS_AUTO, TARGET_FPS_30)
                )
            )
            val customViewModel = createViewModelWithSubModePolicy(policy)
            advanceUntilIdle()

            val fpsState = assertIsEnabled(
                customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            ).fpsUiState as FpsUiState.Enabled

            assertThat(fpsState.fpsAutoState).isEqualTo(SingleSelectableState.Selectable)
            assertThat(fpsState.fpsThirtyState).isEqualTo(SingleSelectableState.Selectable)
            assertThat(fpsState.fpsFifteenState).isEqualTo(
                SingleSelectableState.Disabled(subModeRationale(R.string.fps_rationale_prefix))
            )
            // 60 fps is unsupported by the typical constraints, which stays the reason.
            val sixtyState = fpsState.fpsSixtyState as SingleSelectableState.Disabled
            assertThat(sixtyState.disabledRationale)
                .isNotInstanceOf(DisabledRationale.CaptureSubModeActiveRationale::class.java)
        }

    /**
     * Verifies that a registered sub-mode's policy only applies while that sub-mode is active.
     */
    @Test
    fun settings_whenActiveCaptureSubModeEnds_restrictionsAreRemoved() =
        runTest(StandardTestDispatcher()) {
            val activeCaptureSubModeRepository =
                FakeActiveCaptureSubModeRepository(TEST_SUB_MODE_ID)
            val customViewModel = createViewModelWithConstraints(
                systemConstraints = constraintsWithSubModePolicy(HIDING_SUB_MODE_POLICY),
                activeCaptureSubModeRepository = activeCaptureSubModeRepository
            )
            advanceUntilIdle()
            assertThat(
                assertIsEnabled(
                    customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
                ).fpsUiState
            ).isInstanceOf(FpsUiState.Disabled::class.java)

            activeCaptureSubModeRepository.setActiveCaptureSubModeId(CaptureSubModeId.DEFAULT)
            advanceUntilIdle()

            val enabledState = assertIsEnabled(
                customViewModel.settingsUiState.first {
                    it is SettingsUiState.Enabled && it.fpsUiState is FpsUiState.Enabled
                }
            )
            assertThat(enabledState.stabilizationUiState)
                .isInstanceOf(StabilizationUiState.Enabled::class.java)
            assertThat(enabledState.cameraEffectUiState)
                .isInstanceOf(CameraEffectUiState.Enabled::class.java)
            assertThat(
                (enabledState.flashUiState as FlashUiState.Enabled).lowLightSelectableState
            ).isEqualTo(SingleSelectableState.Selectable)
        }

    /** Creates a view model whose active capture sub-mode has [policy]. */
    private fun createViewModelWithSubModePolicy(
        policy: CameraFeaturePolicy,
        effects: Set<CameraEffectId> = setOf(TEST_EFFECT_ID)
    ): SettingsViewModel {
        val constraints = constraintsWithSubModePolicy(policy)
        return createViewModelWithConstraints(
            systemConstraints = constraints.copy(
                perLensConstraints = constraints.perLensConstraints.mapValues {
                    it.value.copy(supportedEffects = effects)
                }
            ),
            activeCaptureSubModeRepository = FakeActiveCaptureSubModeRepository(TEST_SUB_MODE_ID)
        )
    }

    @Test
    fun stabilization_whenCaptureSubModeAllowsOnlySomeOptions_disablesExcludedOptions() =
        runTest(StandardTestDispatcher()) {
            val customViewModel = createViewModelWithSubModePolicy(
                CameraFeaturePolicy(
                    stabilizationMode = SettingConfig(
                        defaultValue = StabilizationMode.OFF,
                        visibility = OptionVisibility.Only(
                            StabilizationMode.OFF,
                            StabilizationMode.AUTO
                        )
                    )
                )
            )
            advanceUntilIdle()

            val stabilizationState = assertIsEnabled(
                customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            ).stabilizationUiState as StabilizationUiState.Enabled

            assertThat(stabilizationState.stabilizationOnState).isEqualTo(
                SingleSelectableState.Disabled(
                    subModeRationale(R.string.stabilization_rationale_prefix)
                )
            )
        }

    @Test
    fun flash_whenCaptureSubModeExcludesOnAndAuto_disablesThemWithOptionRationales() =
        runTest(StandardTestDispatcher()) {
            val customViewModel = createViewModelWithSubModePolicy(
                CameraFeaturePolicy(
                    flashMode = SettingConfig(
                        defaultValue = FlashMode.OFF,
                        visibility = OptionVisibility.Only(
                            FlashMode.OFF,
                            FlashMode.LOW_LIGHT_BOOST
                        )
                    )
                )
            )
            advanceUntilIdle()

            val flashState = assertIsEnabled(
                customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            ).flashUiState as FlashUiState.Enabled

            assertThat(flashState.onSelectableState).isEqualTo(
                SingleSelectableState.Disabled(subModeRationale(R.string.flash_on_rationale_prefix))
            )
            assertThat(flashState.autoSelectableState).isEqualTo(
                SingleSelectableState.Disabled(
                    subModeRationale(R.string.flash_auto_rationale_prefix)
                )
            )
            assertThat(flashState.lowLightSelectableState)
                .isEqualTo(SingleSelectableState.Selectable)
        }

    @Test
    fun flash_whenCaptureSubModeHidesFlash_disablesWholeSetting() =
        runTest(StandardTestDispatcher()) {
            val customViewModel = createViewModelWithSubModePolicy(
                CameraFeaturePolicy(
                    flashMode = SettingConfig(FlashMode.OFF, OptionVisibility.Hidden)
                )
            )
            advanceUntilIdle()

            assertThat(
                assertIsEnabled(
                    customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
                ).flashUiState
            ).isEqualTo(FlashUiState.Disabled(subModeRationale(R.string.flash_rationale_prefix)))
        }

    @Test
    fun cameraEffect_whenCaptureSubModeAllowsOnlySomeEffects_offersOnlyThoseEffects() =
        runTest(StandardTestDispatcher()) {
            val otherEffect = CameraEffectId("other_effect")
            val customViewModel = createViewModelWithSubModePolicy(
                policy = CameraFeaturePolicy(
                    cameraEffect = SettingConfig(
                        defaultValue = NONE_EFFECT_ID,
                        visibility = OptionVisibility.Only(NONE_EFFECT_ID, TEST_EFFECT_ID)
                    )
                ),
                effects = setOf(TEST_EFFECT_ID, otherEffect)
            )
            advanceUntilIdle()

            val effectState = assertIsEnabled(
                customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
            ).cameraEffectUiState as CameraEffectUiState.Enabled

            assertThat(effectState.supportedEffects).containsExactly(TEST_EFFECT_ID)
        }

    /**
     * Verifies that the sub-mode isn't blamed for an effect setting that has no effects to offer
     * anyway.
     */
    @Test
    fun cameraEffect_whenNoEffectsSupported_isNotDisabledBySubMode() =
        runTest(StandardTestDispatcher()) {
            val customViewModel = createViewModelWithSubModePolicy(
                policy = CameraFeaturePolicy(
                    cameraEffect = SettingConfig(
                        defaultValue = NONE_EFFECT_ID,
                        visibility = OptionVisibility.Only(NONE_EFFECT_ID, TEST_EFFECT_ID)
                    )
                ),
                effects = emptySet()
            )
            advanceUntilIdle()

            assertThat(
                assertIsEnabled(
                    customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
                ).cameraEffectUiState
            ).isInstanceOf(CameraEffectUiState.Enabled::class.java)
        }

    /**
     * Verifies that a setting the active sub-mode hides shows the sub-mode as the reason, even if
     * the setting is also disabled for another reason, since the sub-mode is what the user can
     * change.
     */
    @Test
    fun stabilization_whenUnsupportedAndHiddenBySubMode_showsSubModeRationale() =
        runTest(StandardTestDispatcher()) {
            val policy = CameraFeaturePolicy(
                stabilizationMode = SettingConfig(StabilizationMode.OFF, OptionVisibility.Hidden)
            )
            // The typical constraints support no stabilization, so it is already disabled.
            val customViewModel = createViewModelWithConstraints(
                systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS.copy(
                    captureSubModeDescriptors = constraintsWithSubModePolicy(policy)
                        .captureSubModeDescriptors,
                    captureSubModePolicies = mapOf(TEST_SUB_MODE_ID to policy)
                ),
                activeCaptureSubModeRepository = FakeActiveCaptureSubModeRepository(
                    TEST_SUB_MODE_ID
                )
            )
            advanceUntilIdle()

            assertThat(
                assertIsEnabled(
                    customViewModel.settingsUiState.first { it is SettingsUiState.Enabled }
                ).stabilizationUiState
            ).isEqualTo(
                StabilizationUiState.Disabled(
                    subModeRationale(R.string.stabilization_rationale_prefix)
                )
            )
        }

    @Test
    fun flashSetting_whenDisabledByCaptureSubMode_showsRationale() {
        Dispatchers.resetMain()
        composeTestRule.setContent {
            FlashModeSetting(
                flashUiState = FlashUiState.Disabled(
                    DisabledRationale.CaptureSubModeActiveRationale(
                        affectedSettingNameResId = R.string.flash_rationale_prefix,
                        captureSubModeLabelResId = R.string.settings_title
                    )
                ),
                setFlashMode = {}
            )
        }

        composeTestRule.onNodeWithText("Flash is unsupported in Settings mode").assertIsDisplayed()
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

    @Test
    fun settingsScreen_defaultSlots_rendersDefaultSections() {
        Dispatchers.resetMain()
        composeTestRule.setContent {
            SettingsScreen(
                versionInfo = VersionInfoHolder(versionName = "1.0.0", buildType = "debug"),
                onNavigateBack = {},
                viewModel = settingsViewModel
            )
        }
        composeTestRule.onNodeWithTag(BTN_OPEN_DIALOG_SETTING_FLASH_TAG).assertIsDisplayed()
    }
}

private fun assertIsEnabled(settingsUiState: SettingsUiState): SettingsUiState.Enabled =
    when (settingsUiState) {
        is SettingsUiState.Enabled -> settingsUiState
        else -> throw AssertionError(
            "SettingsUiState expected to be Enabled, but was ${settingsUiState::class}"
        )
    }
