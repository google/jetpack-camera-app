/*
 * Copyright (C) 2023 The Android Open Source Project
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
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.MultiplePermissionsState
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.shouldShowRationale
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.ui.AspectRatioSetting
import com.google.jetpackcamera.settings.ui.BTN_LOCATION_PERMISSION_DIALOG_CANCEL_TAG
import com.google.jetpackcamera.settings.ui.BTN_LOCATION_PERMISSION_DIALOG_CONFIRM_TAG
import com.google.jetpackcamera.settings.ui.ConcurrentCameraSetting
import com.google.jetpackcamera.settings.ui.DIALOG_LOCATION_PERMISSION_RATIONALE_TAG
import com.google.jetpackcamera.settings.ui.DarkModeSetting
import com.google.jetpackcamera.settings.ui.DefaultCameraFacing
import com.google.jetpackcamera.settings.ui.FlashModeSetting
import com.google.jetpackcamera.settings.ui.LocationSetting
import com.google.jetpackcamera.settings.ui.LowLightBoostPrioritySetting
import com.google.jetpackcamera.settings.ui.MaxVideoDurationSetting
import com.google.jetpackcamera.settings.ui.RecordingAudioSetting
import com.google.jetpackcamera.settings.ui.SETTINGS_TITLE
import com.google.jetpackcamera.settings.ui.SectionHeader
import com.google.jetpackcamera.settings.ui.SettingsPageHeader
import com.google.jetpackcamera.settings.ui.StabilizationSetting
import com.google.jetpackcamera.settings.ui.TargetFpsSetting
import com.google.jetpackcamera.settings.ui.VersionInfo
import com.google.jetpackcamera.settings.ui.VideoQualitySetting

private val LOADING_INDICATOR_SIZE = 50.dp

/**
 * Screen used for the Settings feature.
 *
 * @param versionInfo Holder for application version and build type information.
 * @param onNavigateBack Callback when the user navigates back from settings.
 * @param viewModel The [SettingsViewModel] providing the settings state.
 * @param onOpenAppSettings Optional callback when user chooses to open system app settings.
 * @param cameraSettingsSlot Slot for the camera settings section.
 * @param recordingSettingsSlot Slot for the recording settings section.
 * @param appSettingsSlot Slot for the general application settings section.
 */
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    versionInfo: VersionInfoHolder,
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
    onOpenAppSettings: (() -> Unit)? = null,
    cameraSettingsSlot: @Composable () -> Unit = { DefaultCameraSettings(viewModel = viewModel) },
    recordingSettingsSlot: @Composable () -> Unit = {
        DefaultRecordingSettings(viewModel = viewModel)
    },
    appSettingsSlot: @Composable () -> Unit = {
        DefaultAppSettings(
            versionInfo = versionInfo,
            viewModel = viewModel,
            onOpenAppSettings = onOpenAppSettings
        )
    }
) {
    val permissionStates = rememberMultiplePermissionsState(
        permissions =
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    )

    viewModel.setGrantedPermissions(permissionStates)

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
                navBack = onNavigateBack,
                scrollBehavior = scrollBehavior
            )
        }
    ) { innerPadding ->
        when (uiState) {
            SettingsUiState.Loading -> Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(color = MaterialTheme.colorScheme.background),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(LOADING_INDICATOR_SIZE))
            }

            is SettingsUiState.Enabled -> Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .background(color = MaterialTheme.colorScheme.background)
            ) {
                cameraSettingsSlot()
                recordingSettingsSlot()
                appSettingsSlot()
            }
        }
    }
}

/**
 * Stateful wrapper for the default camera settings section.
 *
 * @param customEffectSlot A slot for injecting custom camera effects.
 * @param viewModel The [SettingsViewModel] providing the settings state.
 */
@Composable
fun DefaultCameraSettings(
    customEffectSlot: @Composable () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.settingsUiState.collectAsState()
    val enabledState = uiState as? SettingsUiState.Enabled ?: return

    DefaultCameraSettings(
        customEffectSlot = customEffectSlot,
        enabledState = enabledState,
        setDefaultLensFacing = viewModel::setDefaultLensFacing,
        setFlashMode = viewModel::setFlashMode,
        setTargetFrameRate = viewModel::setTargetFrameRate,
        setAspectRatio = viewModel::setAspectRatio,
        setLowLightBoostPriority = viewModel::setLowLightBoostPriority
    )
}

/**
 * Stateless default camera settings section.
 *
 * @param customEffectSlot A slot for injecting custom camera effects.
 * @param enabledState The current [SettingsUiState.Enabled] state.
 * @param setDefaultLensFacing Callback to set default lens facing.
 * @param setFlashMode Callback to set flash mode.
 * @param setTargetFrameRate Callback to set target frame rate.
 * @param setAspectRatio Callback to set aspect ratio.
 * @param setLowLightBoostPriority Callback to set low light boost priority.
 */
@Composable
fun DefaultCameraSettings(
    customEffectSlot: @Composable () -> Unit,
    enabledState: SettingsUiState.Enabled,
    setDefaultLensFacing: (LensFacing) -> Unit,
    setFlashMode: (FlashMode) -> Unit,
    setTargetFrameRate: (Int) -> Unit,
    setAspectRatio: (AspectRatio) -> Unit,
    setLowLightBoostPriority: (LowLightBoostPriority) -> Unit
) {
    SectionHeader(title = stringResource(id = R.string.section_title_camera_settings))

    DefaultCameraFacing(
        lensUiState = enabledState.lensFlipUiState,
        setDefaultLensFacing = setDefaultLensFacing
    )

    FlashModeSetting(
        flashUiState = enabledState.flashUiState,
        setFlashMode = setFlashMode
    )

    TargetFpsSetting(
        fpsUiState = enabledState.fpsUiState,
        setTargetFps = setTargetFrameRate
    )

    AspectRatioSetting(
        aspectRatioUiState = enabledState.aspectRatioUiState,
        setAspectRatio = setAspectRatio
    )

    customEffectSlot()

    LowLightBoostPrioritySetting(
        lowLightBoostPriorityUiState = enabledState.lowLightBoostPriorityUiState,
        setLowLightBoostPriority = setLowLightBoostPriority
    )
}

/**
 * Stateful wrapper for the default recording settings section.
 *
 * @param viewModel The [SettingsViewModel] providing the settings state.
 */
@Composable
fun DefaultRecordingSettings(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.settingsUiState.collectAsState()
    val enabledState = uiState as? SettingsUiState.Enabled ?: return

    DefaultRecordingSettings(
        enabledState = enabledState,
        setVideoAudio = viewModel::setVideoAudio,
        setMaxVideoDuration = viewModel::setMaxVideoDuration,
        setConcurrentCameraMode = viewModel::setConcurrentCameraMode,
        setStabilizationMode = viewModel::setStabilizationMode,
        setVideoQuality = viewModel::setVideoQuality
    )
}

/**
 * Stateless default recording settings section.
 *
 * @param enabledState The current [SettingsUiState.Enabled] state.
 * @param setVideoAudio Callback to set video audio state.
 * @param setMaxVideoDuration Callback to set max video duration.
 * @param setConcurrentCameraMode Callback to set concurrent camera mode.
 * @param setStabilizationMode Callback to set stabilization mode.
 * @param setVideoQuality Callback to set video quality.
 */
@Composable
fun DefaultRecordingSettings(
    enabledState: SettingsUiState.Enabled,
    setVideoAudio: (Boolean) -> Unit,
    setMaxVideoDuration: (Long) -> Unit,
    setConcurrentCameraMode: (ConcurrentCameraMode) -> Unit,
    setStabilizationMode: (StabilizationMode) -> Unit,
    setVideoQuality: (VideoQuality) -> Unit
) {
    SectionHeader(title = stringResource(R.string.section_title_recording_settings))

    RecordingAudioSetting(
        audioUiState = enabledState.audioUiState,
        setDefaultAudio = setVideoAudio
    )

    MaxVideoDurationSetting(
        maxVideoDurationUiState = enabledState.maxVideoDurationUiState,
        setMaxDuration = setMaxVideoDuration
    )

    ConcurrentCameraSetting(
        concurrentCameraUiState = enabledState.concurrentCameraUiState,
        setConcurrentCameraMode = setConcurrentCameraMode
    )

    StabilizationSetting(
        stabilizationUiState = enabledState.stabilizationUiState,
        setStabilizationMode = setStabilizationMode
    )

    VideoQualitySetting(
        videQualityUiState = enabledState.videoQualityUiState,
        setVideoQuality = setVideoQuality
    )
}

/**
 * Stateful wrapper for the default app settings section.
 *
 * @param versionInfo The [VersionInfoHolder] containing app version information.
 * @param viewModel The [SettingsViewModel] providing the settings state.
 * @param locationPermissionStates The [MultiplePermissionsState] for location permissions.
 * @param onOpenAppSettings Optional callback when user chooses to open system app settings.
 */
@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DefaultAppSettings(
    versionInfo: VersionInfoHolder,
    viewModel: SettingsViewModel = hiltViewModel(),
    locationPermissionStates: MultiplePermissionsState = rememberMultiplePermissionsState(
        permissions = listOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    ),
    onOpenAppSettings: (() -> Unit)? = null
) {
    val uiState by viewModel.settingsUiState.collectAsState()
    val enabledState = uiState as? SettingsUiState.Enabled ?: return

    val context = LocalContext.current
    val openSettingsHandler = onOpenAppSettings ?: {
        Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        ).also(context::startActivity)
    }

    var showLocationRationaleDialog by rememberSaveable { mutableStateOf(false) }
    var hasAttemptedLocationRequest by rememberSaveable { mutableStateOf(false) }
    var pendingLocationEnable by rememberSaveable { mutableStateOf(false) }

    val hasLocationPermission = locationPermissionStates.permissions.any { it.status.isGranted }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission && pendingLocationEnable) {
            viewModel.setLocationEnabled(true)
            pendingLocationEnable = false
        }
    }

    DefaultAppSettings(
        versionInfo = versionInfo,
        enabledState = enabledState,
        setDarkMode = viewModel::setDarkMode,
        setLocationEnabled = { enabled ->
            if (enabled) {
                if (hasLocationPermission) {
                    viewModel.setLocationEnabled(true)
                } else {
                    pendingLocationEnable = true
                    val canShowRationale =
                        locationPermissionStates.permissions.any { it.status.shouldShowRationale }
                    // After a request has been denied without a rationale, the system no longer
                    // shows the prompt, so direct the user to app settings instead.
                    if (canShowRationale || !hasAttemptedLocationRequest) {
                        hasAttemptedLocationRequest = true
                        locationPermissionStates.launchMultiplePermissionRequest()
                    } else {
                        showLocationRationaleDialog = true
                    }
                }
            } else {
                pendingLocationEnable = false
                viewModel.setLocationEnabled(false)
            }
        }
    )

    if (showLocationRationaleDialog) {
        LocationPermissionRationaleDialog(
            onConfirm = {
                showLocationRationaleDialog = false
                pendingLocationEnable = true
                openSettingsHandler()
            },
            onDismiss = {
                showLocationRationaleDialog = false
                pendingLocationEnable = false
            }
        )
    }
}

/**
 * Stateless default app settings section.
 *
 * @param versionInfo The [VersionInfoHolder] containing app version information.
 * @param enabledState The current [SettingsUiState.Enabled] state.
 * @param setDarkMode Callback to set the dark mode.
 * @param setLocationEnabled Callback to set whether location is enabled.
 */
@Composable
fun DefaultAppSettings(
    versionInfo: VersionInfoHolder,
    enabledState: SettingsUiState.Enabled,
    setDarkMode: (DarkMode) -> Unit,
    setLocationEnabled: (Boolean) -> Unit = {}
) {
    SectionHeader(title = stringResource(id = R.string.section_title_app_settings))

    if (enabledState.locationUiState !is LocationUiState.Hidden) {
        LocationSetting(
            locationUiState = enabledState.locationUiState,
            onLocationToggled = setLocationEnabled
        )
    }

    DarkModeSetting(
        darkModeUiState = enabledState.darkModeUiState,
        setDarkMode = setDarkMode
    )

    SectionHeader(title = stringResource(id = R.string.section_title_software_info))

    VersionInfo(
        versionName = versionInfo.versionName,
        buildType = versionInfo.buildType
    )
}

/**
 * Dialog informing the user that location permissions are required for geotagging and
 * providing an action to navigate to system app settings.
 *
 * @param onConfirm Callback when the user confirms navigating to app settings.
 * @param onDismiss Callback when the dialog is dismissed or cancelled.
 * @param modifier Modifier for the dialog layout.
 */
@Composable
fun LocationPermissionRationaleDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        modifier = modifier.testTag(DIALOG_LOCATION_PERMISSION_RATIONALE_TAG),
        onDismissRequest = onDismiss,
        title = {
            Text(text = stringResource(R.string.location_permission_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.location_permission_dialog_message))
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag(BTN_LOCATION_PERMISSION_DIALOG_CONFIRM_TAG),
                onClick = onConfirm
            ) {
                Text(text = stringResource(R.string.location_permission_dialog_open_settings))
            }
        },
        dismissButton = {
            TextButton(
                modifier = Modifier.testTag(BTN_LOCATION_PERMISSION_DIALOG_CANCEL_TAG),
                onClick = onDismiss
            ) {
                Text(text = stringResource(R.string.location_permission_dialog_cancel))
            }
        }
    )
}

data class VersionInfoHolder(val versionName: String, val buildType: String)
