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

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.settings.testing.FakeSettingsDataSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class LocalSettingsRepositoryTest {

    @Test
    fun unconfiguredSettingInPolicy_preservesStoredUserPreference() = runTest {
        // User previously saved 3:4 aspect ratio preference
        val storedSettings = CameraAppSettings(
            aspectRatio = AspectRatio.THREE_FOUR,
            flashMode = FlashMode.ON
        )
        val dataSource = FakeSettingsDataSource(initialSettings = storedSettings)

        // Policy only restricts flash mode to Hidden (OFF), leaving aspect ratio unconfigured
        val policy = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Hidden
            )
        )
        val repository = LocalSettingsRepository(dataSource, policy)

        val current = repository.defaultCameraAppSettings.first()
        // Stored user aspect ratio must be preserved, not overwritten by global default (16:9)
        assertThat(current.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
        // Restricted flash mode enforces the policy
        assertThat(current.flashMode).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun hiddenSetting_enforcesDeveloperDefault() = runTest {
        // User had FlashMode.ON stored
        val storedSettings = CameraAppSettings(flashMode = FlashMode.ON)
        val dataSource = FakeSettingsDataSource(initialSettings = storedSettings)

        // Policy hides flash mode and defaults to OFF
        val policy = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Hidden
            )
        )
        val repository = LocalSettingsRepository(dataSource, policy)

        val current = repository.defaultCameraAppSettings.first()
        assertThat(current.flashMode).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun onlySetting_whenStoredValueConflicts_fallsBackToDeveloperDefault() = runTest {
        // User had FlashMode.ON stored
        val storedSettings = CameraAppSettings(flashMode = FlashMode.ON)
        val dataSource = FakeSettingsDataSource(initialSettings = storedSettings)

        // Policy allows only OFF and AUTO, defaulting to OFF
        val policy = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Only(setOf(FlashMode.OFF, FlashMode.AUTO))
            )
        )
        val repository = LocalSettingsRepository(dataSource, policy)

        val current = repository.defaultCameraAppSettings.first()
        // FlashMode.ON is not in {OFF, AUTO}, so it falls back to developer default (OFF)
        assertThat(current.flashMode).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun onlySetting_whenStoredValuePermitted_preservesUserPreference() = runTest {
        // User had FlashMode.AUTO stored
        val storedSettings = CameraAppSettings(flashMode = FlashMode.AUTO)
        val dataSource = FakeSettingsDataSource(initialSettings = storedSettings)

        // Policy allows only OFF and AUTO, defaulting to OFF
        val policy = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Only(setOf(FlashMode.OFF, FlashMode.AUTO))
            )
        )
        val repository = LocalSettingsRepository(dataSource, policy)

        val current = repository.defaultCameraAppSettings.first()
        // FlashMode.AUTO is permitted in {OFF, AUTO}, so user preference is preserved
        assertThat(current.flashMode).isEqualTo(FlashMode.AUTO)
    }

    @Test
    fun getCurrentDefaultCameraAppSettings_enforcesPolicy() = runTest {
        val storedSettings = CameraAppSettings(flashMode = FlashMode.ON)
        val dataSource = FakeSettingsDataSource(initialSettings = storedSettings)
        val policy = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Hidden
            )
        )
        val repository = LocalSettingsRepository(dataSource, policy)

        val snapshot = repository.getCurrentDefaultCameraAppSettings()
        assertThat(snapshot.flashMode).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun dynamicUpdates_continueToBeEnforcedByPolicy() = runTest {
        val dataSource = FakeSettingsDataSource()
        val policy = CameraFeaturePolicy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Hidden
            )
        )
        val repository = LocalSettingsRepository(dataSource, policy)

        // Attempt to update flash mode while policy has it Hidden
        repository.updateFlashModeStatus(FlashMode.ON)

        val current = repository.defaultCameraAppSettings.first()
        assertThat(current.flashMode).isEqualTo(FlashMode.OFF)
    }

    @Test
    fun aspectRatioOnly_whenStoredValueConflicts_fallsBackToDeveloperDefault() = runTest {
        // User had AspectRatio.THREE_FOUR stored
        val storedSettings = CameraAppSettings(aspectRatio = AspectRatio.THREE_FOUR)
        val dataSource = FakeSettingsDataSource(initialSettings = storedSettings)

        // Policy allows only 1:1 and 9:16, defaulting to 1:1
        val policy = CameraFeaturePolicy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.ONE_ONE,
                visibility = OptionVisibility.Only(
                    setOf(AspectRatio.ONE_ONE, AspectRatio.NINE_SIXTEEN)
                )
            )
        )
        val repository = LocalSettingsRepository(dataSource, policy)

        val current = repository.defaultCameraAppSettings.first()
        // THREE_FOUR is not in {1:1, 9:16}, so falls back to developer default (1:1)
        assertThat(current.aspectRatio).isEqualTo(AspectRatio.ONE_ONE)
    }
}
