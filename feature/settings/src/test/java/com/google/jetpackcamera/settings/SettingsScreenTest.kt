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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.testing.FakeConstraintsRepository
import com.google.jetpackcamera.settings.testing.FakeSettingsDataSource
import com.google.jetpackcamera.settings.ui.AspectRatioSetting
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_ASPECT_RATIO_OPTION_1_1_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_ASPECT_RATIO_OPTION_3_4_TAG
import com.google.jetpackcamera.settings.ui.BTN_DIALOG_ASPECT_RATIO_OPTION_9_16_TAG
import com.google.jetpackcamera.settings.ui.BTN_LOCATION_PERMISSION_DIALOG_CANCEL_TAG
import com.google.jetpackcamera.settings.ui.BTN_LOCATION_PERMISSION_DIALOG_CONFIRM_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_ASPECT_RATIO_TAG
import com.google.jetpackcamera.settings.ui.BTN_SWITCH_SETTING_LOCATION_TAG
import com.google.jetpackcamera.settings.ui.DIALOG_LOCATION_PERMISSION_RATIONALE_TAG
import com.google.jetpackcamera.settings.ui.LocationSetting
import java.util.Optional
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalPermissionsApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun createSettingsViewModel(): SettingsViewModel {
        val settingsRepository = LocalSettingsRepository(
            settingsDataSource = FakeSettingsDataSource()
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

    // Result callback passed by DefaultAppSettings when it creates the location permission state.
    private var locationPermissionsResult: (Map<String, Boolean>) -> Unit = {}

    private fun setAppSettingsContent(
        locationPermissionStates: MultiplePermissionsState,
        settingsViewModel: SettingsViewModel = createSettingsViewModel(),
        onOpenAppSettings: () -> Unit = {}
    ) {
        composeTestRule.setContent {
            // DefaultAppSettings emits sibling rows, so it needs a layout parent.
            Column {
                DefaultAppSettings(
                    versionInfo = VersionInfoHolder("1.0", "debug"),
                    viewModel = settingsViewModel,
                    rememberLocationPermissionsState = { onPermissionsResult ->
                        locationPermissionsResult = onPermissionsResult
                        locationPermissionStates
                    },
                    onOpenAppSettings = onOpenAppSettings
                )
            }
        }
        composeTestRule.waitUntil(5_000) {
            settingsViewModel.settingsUiState.value is SettingsUiState.Enabled
        }
    }

    /**
     * Returns a location permission state whose request sets both permissions to [statusAfter] and
     * then reports a denied result, as the platform does when a request ends without a grant.
     */
    private fun deniedLocationRequest(
        statusBefore: PermissionStatus,
        statusAfter: PermissionStatus,
        onLaunch: () -> Unit = {}
    ): MultiplePermissionsState {
        val fine = FakePermissionState(Manifest.permission.ACCESS_FINE_LOCATION, statusBefore)
        val coarse = FakePermissionState(Manifest.permission.ACCESS_COARSE_LOCATION, statusBefore)
        return FakeMultiplePermissionsState(
            permissions = listOf(fine, coarse),
            onLaunch = {
                onLaunch()
                fine.status = statusAfter
                coarse.status = statusAfter
                locationPermissionsResult(
                    mapOf(fine.permission to false, coarse.permission to false)
                )
            }
        )
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
    fun locationPermanentlyDenied_requestEndsWithoutPrompt_showsRationaleDialogOnFirstToggle() {
        var launchCount = 0
        setAppSettingsContent(
            locationPermissionStates = deniedLocationRequest(
                statusBefore = PermissionStatus.Denied(shouldShowRationale = false),
                statusAfter = PermissionStatus.Denied(shouldShowRationale = false),
                onLaunch = { launchCount++ }
            )
        )

        clickLocationSwitch()

        assertThat(launchCount).isEqualTo(1)
        composeTestRule.onNodeWithTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG).assertIsDisplayed()
    }

    @Test
    fun locationDeniedForFirstTime_doesNotShowRationaleDialog() {
        setAppSettingsContent(
            locationPermissionStates = deniedLocationRequest(
                statusBefore = PermissionStatus.Denied(shouldShowRationale = false),
                statusAfter = PermissionStatus.Denied(shouldShowRationale = true)
            )
        )

        clickLocationSwitch()

        composeTestRule.onNodeWithTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG).assertDoesNotExist()
    }

    @Test
    fun locationDeniedAfterRationale_doesNotShowRationaleDialog() {
        setAppSettingsContent(
            locationPermissionStates = deniedLocationRequest(
                statusBefore = PermissionStatus.Denied(shouldShowRationale = true),
                statusAfter = PermissionStatus.Denied(shouldShowRationale = false)
            )
        )

        clickLocationSwitch()

        composeTestRule.onNodeWithTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG).assertDoesNotExist()
    }

    @Test
    fun locationPermissionGranted_isReportedToViewModel() {
        val settingsViewModel = createSettingsViewModel()
        setAppSettingsContent(
            locationPermissionStates = locationPermissionStates(status = PermissionStatus.Granted),
            settingsViewModel = settingsViewModel
        )

        composeTestRule.waitUntil(5_000) {
            (settingsViewModel.settingsUiState.value as? SettingsUiState.Enabled)
                ?.locationUiState is LocationUiState.Enabled
        }
    }

    @Test
    fun updateGrantedPermissions_leavesPermissionsOutsideTheUpdateUnchanged() {
        val settingsViewModel = createSettingsViewModel()
        setAppSettingsContent(
            locationPermissionStates = locationPermissionStates(status = PermissionStatus.Granted),
            settingsViewModel = settingsViewModel
        )
        settingsViewModel.setGrantedPermissions(mutableSetOf(Manifest.permission.RECORD_AUDIO))

        settingsViewModel.updateGrantedPermissions(
            locationPermissionStates(status = PermissionStatus.Granted)
        )

        composeTestRule.waitForIdle()
        val state = settingsViewModel.settingsUiState.value as SettingsUiState.Enabled
        assertThat(state.locationUiState).isInstanceOf(LocationUiState.Enabled::class.java)
        assertThat(state.audioUiState).isInstanceOf(AudioUiState.Enabled::class.java)
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

    @Test
    fun locationPermissionDenied_toggle_thenGrant_enablesLocationSetting() {
        val settingsViewModel = createSettingsViewModel()
        val fine = FakePermissionState(
            Manifest.permission.ACCESS_FINE_LOCATION,
            PermissionStatus.Denied(shouldShowRationale = false)
        )
        val coarse = FakePermissionState(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            PermissionStatus.Denied(shouldShowRationale = false)
        )
        val permissionStates = FakeMultiplePermissionsState(
            permissions = listOf(fine, coarse),
            onLaunch = {
                fine.status = PermissionStatus.Granted
                coarse.status = PermissionStatus.Granted
                locationPermissionsResult(
                    mapOf(fine.permission to true, coarse.permission to true)
                )
            }
        )
        setAppSettingsContent(
            locationPermissionStates = permissionStates,
            settingsViewModel = settingsViewModel
        )

        clickLocationSwitch()

        composeTestRule.waitUntil(5_000) {
            (settingsViewModel.settingsUiState.value as? SettingsUiState.Enabled)
                ?.locationUiState is LocationUiState.Enabled.On
        }
    }

    @Test
    fun locationSetting_whenHidden_isNotDisplayed() {
        composeTestRule.setContent {
            LocationSetting(
                locationUiState = LocationUiState.Hidden,
                onLocationToggled = {}
            )
        }

        composeTestRule.onNodeWithTag(BTN_SWITCH_SETTING_LOCATION_TAG).assertDoesNotExist()
    }

    @Test
    fun aspectRatioSetting_displaysOptionsInOrderAndInvokesCallback() {
        val selectedRatios = mutableListOf<AspectRatio>()
        composeTestRule.setContent {
            AspectRatioSetting(
                aspectRatioUiState = AspectRatioUiState.Enabled(AspectRatio.THREE_FOUR),
                setAspectRatio = { selectedRatios.add(it) }
            )
        }

        composeTestRule.onNodeWithTag(BTN_OPEN_DIALOG_SETTING_ASPECT_RATIO_TAG).performClick()

        val node34 = composeTestRule.onNodeWithTag(BTN_DIALOG_ASPECT_RATIO_OPTION_3_4_TAG)
            .assertIsDisplayed()
        val node11 = composeTestRule.onNodeWithTag(BTN_DIALOG_ASPECT_RATIO_OPTION_1_1_TAG)
            .assertIsDisplayed()
        val node916 = composeTestRule.onNodeWithTag(BTN_DIALOG_ASPECT_RATIO_OPTION_9_16_TAG)
            .assertIsDisplayed()

        val top34 = node34.fetchSemanticsNode().boundsInRoot.top
        val top11 = node11.fetchSemanticsNode().boundsInRoot.top
        val top916 = node916.fetchSemanticsNode().boundsInRoot.top
        assertThat(top34).isLessThan(top11)
        assertThat(top11).isLessThan(top916)

        node34.performClick()
        node11.performClick()
        node916.performClick()

        assertThat(selectedRatios).containsExactly(
            AspectRatio.THREE_FOUR,
            AspectRatio.ONE_ONE,
            AspectRatio.NINE_SIXTEEN
        ).inOrder()
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
    status: PermissionStatus
) : PermissionState {
    override var status: PermissionStatus by mutableStateOf(status)

    override fun launchPermissionRequest() {}
}
