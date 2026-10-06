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
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.permissions.ui.PermissionTemplate
import com.google.jetpackcamera.permissions.ui.REQUEST_PERMISSION_BUTTON
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@OptIn(ExperimentalPermissionsApi::class)
@RunWith(RobolectricTestRunner::class)
// The permission layout needs a phone-sized screen for the request button to be on screen.
@Config(qualifiers = "w411dp-h891dp")
class PermissionsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun whenLocationFirstClick_launchesPermissionRequest() {
        var launchCount = 0

        val fakeFine = FakePermissionState(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            status = PermissionStatus.Denied(shouldShowRationale = false)
        )
        val fakeCoarse = FakePermissionState(
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
            status = PermissionStatus.Denied(shouldShowRationale = false)
        )
        val fakeState = FakeMultiplePermissionsState(
            permissions = listOf(fakeFine, fakeCoarse),
            onLaunch = { launchCount++ }
        )

        composeTestRule.setContent {
            PermissionTemplate(
                permissionEnum = PermissionEnum.LOCATION,
                permissionStates = fakeState,
                onDismissPermission = {},
                onOpenAppSettings = {}
            )
        }

        // First click: attempts system permission request
        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).performClick()
        assertThat(launchCount).isEqualTo(1)
    }

    @Test
    fun whenLocationRequestShowsNoPrompt_clickingAgain_requestsAgainWithoutDismissing() {
        var dismissCalled = false
        var launchCount = 0

        val fakeFine = FakePermissionState(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            status = PermissionStatus.Denied(shouldShowRationale = false)
        )
        val fakeCoarse = FakePermissionState(
            permission = Manifest.permission.ACCESS_COARSE_LOCATION,
            status = PermissionStatus.Denied(shouldShowRationale = false)
        )
        val fakeState = FakeMultiplePermissionsState(
            permissions = listOf(fakeFine, fakeCoarse),
            onLaunch = { launchCount++ }
        )

        composeTestRule.setContent {
            PermissionTemplate(
                permissionEnum = PermissionEnum.LOCATION,
                permissionStates = fakeState,
                onDismissPermission = { dismissCalled = true },
                onOpenAppSettings = {}
            )
        }

        // First click: attempts system permission request
        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).performClick()
        assertThat(launchCount).isEqualTo(1)
        assertThat(dismissCalled).isFalse()

        // Advancing after a request that ends without a grant is handled by the request result
        // callback (see optionalPermissionRequest_endsWithoutGrant_dismissesPermission), so a
        // second click requests again.
        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).performClick()
        assertThat(launchCount).isEqualTo(2)
        assertThat(dismissCalled).isFalse()
    }

    @Test
    fun whenOptionalPermissionDeniedOnce_launchedEffect_invokesDismissPermission() {
        var dismissCalled = false

        val fakeFine = FakePermissionState(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            status = PermissionStatus.Denied(shouldShowRationale = true)
        )
        val fakeState = FakeMultiplePermissionsState(
            permissions = listOf(fakeFine),
            shouldShowRationale = true
        )

        composeTestRule.setContent {
            PermissionTemplate(
                permissionEnum = PermissionEnum.LOCATION,
                permissionStates = fakeState,
                onDismissPermission = { dismissCalled = true },
                onOpenAppSettings = {}
            )
        }

        composeTestRule.waitForIdle()
        assertThat(dismissCalled).isTrue()
    }

    @Test
    fun whenAudioRequestedThenLocationShown_clickingAllow_launchesLocationRequest() {
        var audioLaunchCount = 0
        var locationLaunchCount = 0
        var currentEnum by mutableStateOf(PermissionEnum.RECORD_AUDIO)

        val audioState = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    permission = Manifest.permission.RECORD_AUDIO,
                    status = PermissionStatus.Denied(shouldShowRationale = false)
                )
            ),
            onLaunch = { audioLaunchCount++ }
        )
        val locationState = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    permission = Manifest.permission.ACCESS_FINE_LOCATION,
                    status = PermissionStatus.Denied(shouldShowRationale = false)
                ),
                FakePermissionState(
                    permission = Manifest.permission.ACCESS_COARSE_LOCATION,
                    status = PermissionStatus.Denied(shouldShowRationale = false)
                )
            ),
            onLaunch = { locationLaunchCount++ }
        )

        composeTestRule.setContent {
            key(currentEnum) {
                PermissionTemplate(
                    permissionEnum = currentEnum,
                    permissionStates = if (currentEnum == PermissionEnum.RECORD_AUDIO) {
                        audioState
                    } else {
                        locationState
                    },
                    onDismissPermission = {},
                    onOpenAppSettings = {}
                )
            }
        }

        // First click on Audio: launches audio request
        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).performClick()
        assertThat(audioLaunchCount).isEqualTo(1)

        // Switch to Location screen
        currentEnum = PermissionEnum.LOCATION
        composeTestRule.waitForIdle()

        // Click on Location: must launch location request and not be skipped
        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).performClick()
        assertThat(locationLaunchCount).isEqualTo(1)
    }

    @Test
    fun optionalPermissionRequest_endsWithoutGrant_dismissesPermission() {
        var dismissCalled = false
        lateinit var activity: ComponentActivity
        composeTestRule.setContent {
            activity = LocalContext.current as ComponentActivity
            PermissionTemplate(
                permissionEnum = PermissionEnum.LOCATION,
                onDismissPermission = { dismissCalled = true },
                onOpenAppSettings = {}
            )
        }

        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).performClick()
        // Rationale stays false, as after a dismissed prompt or one the system did not show.
        deliverPermissionResult(activity, granted = false)

        assertThat(dismissCalled).isTrue()
    }

    @Test
    fun mandatoryPermissionRequest_endsWithoutGrant_doesNotDismissPermission() {
        var dismissCalled = false
        lateinit var activity: ComponentActivity
        composeTestRule.setContent {
            activity = LocalContext.current as ComponentActivity
            PermissionTemplate(
                permissionEnum = PermissionEnum.CAMERA,
                onDismissPermission = { dismissCalled = true },
                onOpenAppSettings = {}
            )
        }

        composeTestRule.onNodeWithTag(REQUEST_PERMISSION_BUTTON).performClick()
        deliverPermissionResult(activity, granted = false)

        assertThat(dismissCalled).isFalse()
    }

    /** Answers the last runtime permission request made by [activity]. */
    @Suppress("DEPRECATION")
    private fun deliverPermissionResult(activity: ComponentActivity, granted: Boolean) {
        composeTestRule.waitForIdle()
        val request = checkNotNull(shadowOf(activity).lastRequestedPermission) {
            "No permission request was made"
        }
        val result =
            if (granted) PackageManager.PERMISSION_GRANTED else PackageManager.PERMISSION_DENIED
        activity.onRequestPermissionsResult(
            request.requestCode,
            request.requestedPermissions,
            IntArray(request.requestedPermissions.size) { result }
        )
        composeTestRule.waitForIdle()
    }

    @Test
    fun permissionTemplate_statusMutatedInPlaceToGranted_dismissesPermission() {
        var dismissed = false
        val fineLocationState = FakePermissionState(
            permission = Manifest.permission.ACCESS_FINE_LOCATION,
            status = PermissionStatus.Denied(shouldShowRationale = false)
        )
        val permissionStates = FakeMultiplePermissionsState(
            permissions = listOf(fineLocationState)
        )

        composeTestRule.setContent {
            PermissionTemplate(
                permissionEnum = PermissionEnum.LOCATION,
                permissionStates = permissionStates,
                onDismissPermission = { dismissed = true },
                onOpenAppSettings = {}
            )
        }

        composeTestRule.waitForIdle()
        assertThat(dismissed).isFalse()

        fineLocationState.status = PermissionStatus.Granted
        composeTestRule.waitForIdle()
        assertThat(dismissed).isTrue()
    }
}
