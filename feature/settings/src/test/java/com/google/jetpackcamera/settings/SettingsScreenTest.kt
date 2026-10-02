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
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.location.testing.FakeLocationProvider
import com.google.jetpackcamera.core.settings.datastoreprefs.PrefsDataStoreSettingsDataSource
import com.google.jetpackcamera.core.settings.datastoreprefs.testing.FakeDataStoreModule
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.testing.FakeConstraintsRepository
import com.google.jetpackcamera.settings.ui.BTN_LOCATION_PERMISSION_DIALOG_CANCEL_TAG
import com.google.jetpackcamera.settings.ui.BTN_LOCATION_PERMISSION_DIALOG_CONFIRM_TAG
import com.google.jetpackcamera.settings.ui.BTN_SWITCH_SETTING_LOCATION_TAG
import com.google.jetpackcamera.settings.ui.DIALOG_LOCATION_PERMISSION_RATIONALE_TAG
import java.util.Optional
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPermissionsApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    private fun createSettingsViewModel(): SettingsViewModel {
        val testDataStore = FakeDataStoreModule.providePreferenceDataStore(
            scope = CoroutineScope(Dispatchers.Unconfined + SupervisorJob()),
            file = tempFolder.newFile("test_settings.preferences_pb")
        )
        val settingsRepository = LocalSettingsRepository(
            settingsDataSource = PrefsDataStoreSettingsDataSource(
                dataStore = testDataStore,
                defaultCaptureModeOverride = CaptureMode.STANDARD
            )
        )
        val constraintsRepository = FakeConstraintsRepository(TYPICAL_SYSTEM_CONSTRAINTS)
        return SettingsViewModel(
            settingsRepository,
            constraintsRepository,
            Optional.of(FakeLocationProvider())
        )
    }

    private fun locationPermissionStates(
        status: PermissionStatus,
        onLaunch: () -> Unit = {}
    ): MultiplePermissionsState = FakeMultiplePermissionsState(
        permissions = listOf(
            FakePermissionState(Manifest.permission.ACCESS_FINE_LOCATION, status),
            FakePermissionState(Manifest.permission.ACCESS_COARSE_LOCATION, status)
        ),
        onLaunch = onLaunch
    )

    private fun setAppSettingsContent(
        locationPermissionStates: MultiplePermissionsState,
        onOpenAppSettings: () -> Unit = {}
    ) {
        val settingsViewModel = createSettingsViewModel()
        composeTestRule.setContent {
            // DefaultAppSettings emits sibling rows, so it needs a layout parent.
            Column {
                DefaultAppSettings(
                    versionInfo = VersionInfoHolder("1.0", "debug"),
                    viewModel = settingsViewModel,
                    locationPermissionStates = locationPermissionStates,
                    onOpenAppSettings = onOpenAppSettings
                )
            }
        }
        composeTestRule.waitUntil(5_000) {
            settingsViewModel.settingsUiState.value is SettingsUiState.Enabled
        }
    }

    private fun clickLocationSwitch() {
        composeTestRule.onNodeWithTag(BTN_SWITCH_SETTING_LOCATION_TAG).performClick()
    }

    @Test
    fun locationPermanentlyDenied_secondToggle_showsRationaleDialogThatOpensSettings() {
        var launchCount = 0
        var openAppSettingsCalled = false
        setAppSettingsContent(
            locationPermissionStates = locationPermissionStates(
                status = PermissionStatus.Denied(shouldShowRationale = false),
                onLaunch = { launchCount++ }
            ),
            onOpenAppSettings = { openAppSettingsCalled = true }
        )

        // First toggle requests the permission.
        clickLocationSwitch()
        assertThat(launchCount).isEqualTo(1)

        // The system no longer prompts after a denial, so the second toggle shows the dialog.
        clickLocationSwitch()
        assertThat(launchCount).isEqualTo(1)
        composeTestRule.onNodeWithTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG).assertIsDisplayed()

        composeTestRule.onNodeWithTag(BTN_LOCATION_PERMISSION_DIALOG_CONFIRM_TAG).performClick()
        assertThat(openAppSettingsCalled).isTrue()
    }

    @Test
    fun locationRationaleDialog_cancel_dismissesWithoutOpeningSettings() {
        var openAppSettingsCalled = false
        setAppSettingsContent(
            locationPermissionStates = locationPermissionStates(
                status = PermissionStatus.Denied(shouldShowRationale = false)
            ),
            onOpenAppSettings = { openAppSettingsCalled = true }
        )

        clickLocationSwitch()
        clickLocationSwitch()
        composeTestRule.onNodeWithTag(BTN_LOCATION_PERMISSION_DIALOG_CANCEL_TAG).performClick()

        composeTestRule.onNodeWithTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG).assertDoesNotExist()
        assertThat(openAppSettingsCalled).isFalse()
    }

    @Test
    fun locationRationaleAvailable_toggle_requestsPermissionAgain() {
        var launchCount = 0
        setAppSettingsContent(
            locationPermissionStates = locationPermissionStates(
                status = PermissionStatus.Denied(shouldShowRationale = true),
                onLaunch = { launchCount++ }
            )
        )

        clickLocationSwitch()
        clickLocationSwitch()

        assertThat(launchCount).isEqualTo(2)
        composeTestRule.onNodeWithTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG).assertDoesNotExist()
    }

    @Test
    fun locationPermissionGranted_toggle_doesNotRequestOrShowDialog() {
        var launchCount = 0
        setAppSettingsContent(
            locationPermissionStates = locationPermissionStates(
                status = PermissionStatus.Granted,
                onLaunch = { launchCount++ }
            )
        )

        clickLocationSwitch()

        assertThat(launchCount).isEqualTo(0)
        composeTestRule.onNodeWithTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG).assertDoesNotExist()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private class FakeMultiplePermissionsState(
    override val permissions: List<PermissionState>,
    override val allPermissionsGranted: Boolean = false,
    override val revokedPermissions: List<PermissionState> = emptyList(),
    override val shouldShowRationale: Boolean = false,
    private val onLaunch: () -> Unit = {}
) : MultiplePermissionsState {
    override fun launchMultiplePermissionRequest() {
        onLaunch()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private class FakePermissionState(
    override val permission: String,
    override val status: PermissionStatus
) : PermissionState {
    override fun launchPermissionRequest() {}
}
