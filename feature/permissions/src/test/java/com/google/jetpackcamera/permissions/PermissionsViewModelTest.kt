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

import androidx.lifecycle.SavedStateHandle
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionStatus
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.settings.testing.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@OptIn(ExperimentalPermissionsApi::class, ExperimentalCoroutinesApi::class)
class PermissionsViewModelTest {

    private lateinit var viewModel: PermissionsViewModel
    private lateinit var settingsRepository: FakeSettingsRepository
    private lateinit var permissionsRepository: FakePermissionsRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        settingsRepository = FakeSettingsRepository()
        permissionsRepository = FakePermissionsRepository()
        val savedStateHandle = SavedStateHandle()
        viewModel = PermissionsViewModel(
            savedStateHandle = savedStateHandle,
            settingsRepository = settingsRepository,
            permissionsRepository = permissionsRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun updatePermissionStates_locationTransitionedToGranted_updatesSettingsRepository() = runTest {
        // ensure initial is false
        settingsRepository.updateLocationEnabled(false)

        // Initial state: location not granted
        val initialState = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    PermissionStatus.Denied(false)
                )
            )
        )
        viewModel.updatePermissionStates(initialState)
        advanceUntilIdle()

        var settings = settingsRepository.getCurrentDefaultCameraAppSettings()
        assertThat(settings.locationEnabled).isFalse()

        // State 2: location granted
        val grantedState = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    PermissionStatus.Granted
                )
            )
        )
        viewModel.updatePermissionStates(grantedState)
        advanceUntilIdle()

        settings = settingsRepository.getCurrentDefaultCameraAppSettings()
        assertThat(settings.locationEnabled).isTrue()
    }

    @Test
    fun updatePermissionStates_initialLocationGranted_doesNotUpdateSettingsRepository() = runTest {
        // Initial state: user has location disabled manually via settings (perhaps)
        settingsRepository.updateLocationEnabled(false)

        val initialState = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    PermissionStatus.Granted
                )
            )
        )

        viewModel.updatePermissionStates(initialState)
        advanceUntilIdle()

        val settings = settingsRepository.getCurrentDefaultCameraAppSettings()
        assertThat(settings.locationEnabled).isFalse()
    }

    @Test
    fun updatePermissionStates_optionalPermissionInRequestedPermissions_isSkipped() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.permissionsUiState.collect()
        }
        permissionsRepository.requestedPermissions.value = setOf(PermissionEnum.LOCATION.name)

        val state = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    android.Manifest.permission.CAMERA,
                    PermissionStatus.Denied(false)
                ),
                FakePermissionState(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    PermissionStatus.Denied(false)
                )
            )
        )

        viewModel.updatePermissionStates(state)
        advanceUntilIdle()

        val uiState = viewModel.permissionsUiState.value
        assertThat(uiState).isInstanceOf(PermissionsUiState.PermissionsNeeded::class.java)
        // Camera is still needed, but Location is excluded because it was already requested
        assertThat((uiState as PermissionsUiState.PermissionsNeeded).currentPermission)
            .isEqualTo(PermissionEnum.CAMERA)
    }

    @Test
    fun updatePermissionStates_whenCameraGrantedOnFirstRun_doesNotSkipOptionalPermissions() =
        runTest {
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.permissionsUiState.collect()
            }
            val state = FakeMultiplePermissionsState(
                permissions = listOf(
                    FakePermissionState(
                        android.Manifest.permission.CAMERA,
                        PermissionStatus.Granted
                    ),
                    FakePermissionState(
                        android.Manifest.permission.RECORD_AUDIO,
                        PermissionStatus.Denied(false)
                    ),
                    FakePermissionState(
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                        PermissionStatus.Denied(false)
                    )
                )
            )

            viewModel.updatePermissionStates(state)
            advanceUntilIdle()

            // When camera is granted on first run, optional permissions (Audio, Location)
            // are not yet requested and must NOT be skipped.
            val uiState = viewModel.permissionsUiState.value
            assertThat(uiState).isInstanceOf(PermissionsUiState.PermissionsNeeded::class.java)
            assertThat((uiState as PermissionsUiState.PermissionsNeeded).currentPermission)
                .isEqualTo(PermissionEnum.RECORD_AUDIO)
        }

    @Test
    fun updatePermissionStates_whenCameraGrantedAndOptionalRequested_allGranted() = runTest {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.permissionsUiState.collect()
        }
        permissionsRepository.requestedPermissions.value = setOf(
            PermissionEnum.RECORD_AUDIO.name,
            PermissionEnum.LOCATION.name
        )
        val state = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    android.Manifest.permission.CAMERA,
                    PermissionStatus.Granted
                ),
                FakePermissionState(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    PermissionStatus.Denied(false)
                ),
                FakePermissionState(
                    android.Manifest.permission.RECORD_AUDIO,
                    PermissionStatus.Denied(false)
                )
            )
        )

        viewModel.updatePermissionStates(state)
        advanceUntilIdle()

        // When optional permissions were already requested, they are skipped
        // and AllPermissionsGranted is reached.
        val uiState = viewModel.permissionsUiState.value
        assertThat(uiState).isEqualTo(PermissionsUiState.AllPermissionsGranted)
    }

    @Test
    fun getRequestablePermissions_rationaleOnAnyLocationPermission_skipsLocation() {
        val state = FakeMultiplePermissionsState(
            permissions = listOf(
                FakePermissionState(
                    android.Manifest.permission.CAMERA,
                    PermissionStatus.Granted
                ),
                FakePermissionState(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    PermissionStatus.Denied(shouldShowRationale = true)
                ),
                FakePermissionState(
                    android.Manifest.permission.ACCESS_COARSE_LOCATION,
                    PermissionStatus.Denied(shouldShowRationale = false)
                )
            )
        )

        assertThat(getRequestablePermissions(state)).isEmpty()
    }

    @Test
    fun dismissPermission_marksPermissionRequestedInRepository() = runTest {
        viewModel.dismissPermission(PermissionEnum.LOCATION)
        advanceUntilIdle()

        val requested = permissionsRepository.requestedPermissions.first()
        assertThat(requested).contains(PermissionEnum.LOCATION.name)
    }
}
