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
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.settings.datastoreprefs.PrefsDataStoreSettingsDataSource
import com.google.jetpackcamera.core.settings.datastoreprefs.testing.FakeDataStoreModule
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.testing.FakeConstraintsRepository
import com.google.jetpackcamera.settings.testing.FakeSettingsRepository
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_FLASH_TAG
import java.io.File
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
        val constraintsRepository = FakeConstraintsRepository(TYPICAL_SYSTEM_CONSTRAINTS)
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

    /**
     * Verifies that the settings UI state stays loading, rather than showing an empty screen,
     * until the camera system constraints become available.
     */
    @Test
    fun settingsUiState_whenConstraintsUnavailable_isLoading() = runTest(StandardTestDispatcher()) {
        val constraintsRepository = FakeConstraintsRepository()
        val customViewModel = SettingsViewModel(FakeSettingsRepository(), constraintsRepository)
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
