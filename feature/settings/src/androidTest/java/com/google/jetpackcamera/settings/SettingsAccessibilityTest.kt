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
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.jetpackcamera.core.location.testing.FakeLocationProvider
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.testing.FakeConstraintsRepository
import com.google.jetpackcamera.settings.testing.FakeSettingsDataSource
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_ASPECT_RATIO_TAG
import com.google.jetpackcamera.settings.ui.BTN_OPEN_DIALOG_SETTING_DARK_MODE_TAG
import com.google.jetpackcamera.settings.ui.CLOSE_BUTTON
import com.google.jetpackcamera.settings.ui.DIALOG_LOCATION_PERMISSION_RATIONALE_TAG
import com.google.jetpackcamera.settings.ui.SETTINGS_TITLE
import com.google.jetpackcamera.settings.ui.SettingsPageHeader
import com.google.jetpackcamera.settings.ui.theme.SettingsPreviewTheme
import java.util.Optional
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class SettingsAccessibilityTest {
    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(
            AccessibilityValidator()
                .setRunChecksFromRootView(true)
                .setThrowExceptionFor(AccessibilityCheckResultType.ERROR)
        )
    }

    private fun createSettingsViewModel(): SettingsViewModel {
        val settingsRepository = LocalSettingsRepository(
            settingsDataSource = FakeSettingsDataSource()
        )
        val constraintsRepository = FakeConstraintsRepository(TYPICAL_SYSTEM_CONSTRAINTS)
        return SettingsViewModel(
            settingsRepository,
            constraintsRepository,
            Optional.of(FakeLocationProvider())
        ).apply {
            setGrantedPermissions(
                mutableSetOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    @Test
    fun settingsScreen_andPopupDialogs_meetAccessibilityChecks() {
        val viewModel = createSettingsViewModel()
        val grantedLocationPermissions = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    PermissionStatus.Granted
                ),
                FakePermissionState(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    PermissionStatus.Granted
                )
            )
        )

        composeTestRule.setContent {
            SettingsPreviewTheme {
                val uiState by viewModel.settingsUiState.collectAsState()
                val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(
                    rememberTopAppBarState()
                )
                Scaffold(
                    modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                    topBar = {
                        SettingsPageHeader(
                            modifier = Modifier.testTag(SETTINGS_TITLE),
                            title = stringResource(id = R.string.settings_title),
                            navBack = {},
                            scrollBehavior = scrollBehavior
                        )
                    }
                ) { innerPadding ->
                    if (uiState is SettingsUiState.Enabled) {
                        Column(
                            modifier = Modifier
                                .padding(innerPadding)
                                .verticalScroll(rememberScrollState())
                                .background(color = MaterialTheme.colorScheme.background)
                        ) {
                            DefaultCameraSettings(viewModel = viewModel)
                            DefaultRecordingSettings(viewModel = viewModel)
                            DefaultAppSettings(
                                versionInfo = VersionInfoHolder("1.0.0", "debug"),
                                viewModel = viewModel,
                                rememberLocationPermissionsState = { grantedLocationPermissions },
                                onOpenAppSettings = {}
                            )
                        }
                    }
                }
            }
        }

        composeTestRule.waitUntil(5_000) {
            viewModel.settingsUiState.value is SettingsUiState.Enabled
        }

        // Check main Settings screen list (top bar back button, switch rows, dialog rows)
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        // Open Aspect Ratio popup dialog and verify radio rows + Close button
        composeTestRule.onNodeWithTag(BTN_OPEN_DIALOG_SETTING_ASPECT_RATIO_TAG)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(CLOSE_BUTTON).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithTag(CLOSE_BUTTON).performClick()

        // Scroll down to App Settings, open Dark Mode popup dialog, and verify
        composeTestRule.onNodeWithTag(BTN_OPEN_DIALOG_SETTING_DARK_MODE_TAG)
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithTag(CLOSE_BUTTON).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
        composeTestRule.onNodeWithTag(CLOSE_BUTTON).performClick()
    }

    @Test
    fun locationPermissionRationaleDialog_meetsAccessibilityChecks() {
        composeTestRule.setContent {
            SettingsPreviewTheme {
                LocationPermissionRationaleDialog(
                    onConfirm = {},
                    onDismiss = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}

@OptIn(ExperimentalPermissionsApi::class)
private class FakeMultiplePermissionsState(
    override val permissions: List<PermissionState>,
    override val allPermissionsGranted: Boolean = false,
    override val revokedPermissions: List<PermissionState> = emptyList(),
    override val shouldShowRationale: Boolean = false
) : MultiplePermissionsState {
    override fun launchMultiplePermissionRequest() {}
}

@OptIn(ExperimentalPermissionsApi::class)
private class FakePermissionState(
    override val permission: String,
    status: PermissionStatus
) : PermissionState {
    override var status: PermissionStatus by mutableStateOf(status)
    override fun launchPermissionRequest() {}
}
