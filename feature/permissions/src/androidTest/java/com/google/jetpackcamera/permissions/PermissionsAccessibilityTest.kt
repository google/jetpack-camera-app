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
package com.google.jetpackcamera.permissions

import android.Manifest
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResult.AccessibilityCheckResultType
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityValidator
import com.google.jetpackcamera.permissions.ui.PermissionTemplate
import com.google.jetpackcamera.permissions.ui.REQUEST_PERMISSION_BUTTON
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@OptIn(ExperimentalPermissionsApi::class)
@RunWith(AndroidJUnit4::class)
class PermissionsAccessibilityTest {
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

    @Test
    fun cameraPermissionPage_initialAndRationaleStates_meetAccessibilityChecks() {
        var shouldShowRationale by mutableStateOf(false)

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val cameraState = FakeMultiplePermissionsState(
                    permissions = listOf(
                        FakePermissionState(
                            permission = Manifest.permission.CAMERA,
                            status = PermissionStatus.Denied(shouldShowRationale)
                        )
                    ),
                    shouldShowRationale = shouldShowRationale
                )
                PermissionTemplate(
                    modifier = Modifier.fillMaxSize(),
                    permissionEnum = PermissionEnum.CAMERA,
                    permissionStates = cameraState,
                    onDismissPermission = {},
                    onOpenAppSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        shouldShowRationale = true
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun optionalAudioAndLocationPages_meetAccessibilityChecks() {
        var currentPermission by mutableStateOf(PermissionEnum.RECORD_AUDIO)

        composeTestRule.setContent {
            MaterialTheme(colorScheme = darkColorScheme()) {
                val permissionState = FakeMultiplePermissionsState(
                    permissions = currentPermission.getPermissions().map { permission ->
                        FakePermissionState(
                            permission = permission,
                            status = PermissionStatus.Denied(shouldShowRationale = false)
                        )
                    }
                )
                PermissionTemplate(
                    modifier = Modifier.fillMaxSize(),
                    permissionEnum = currentPermission,
                    permissionStates = permissionState,
                    onDismissPermission = {},
                    onOpenAppSettings = {}
                )
            }
        }

        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).assertIsDisplayed()
        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        currentPermission = PermissionEnum.LOCATION
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).assertIsDisplayed()
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
