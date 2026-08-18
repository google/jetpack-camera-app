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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.model.DarkMode
import com.google.jetpackcamera.model.LowLightBoostPriority
import com.google.jetpackcamera.model.ConcurrentCameraMode
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.jetpackcamera.settings.ui.AspectRatioSetting
import com.google.jetpackcamera.settings.ui.ConcurrentCameraSetting
import com.google.jetpackcamera.settings.ui.DarkModeSetting
import com.google.jetpackcamera.settings.ui.DefaultCameraFacing
import com.google.jetpackcamera.settings.ui.FlashModeSetting
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

/**
 * Screen used for the Settings feature.
 */

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    versionInfo: VersionInfoHolder,
    onNavigateBack: () -> Unit,
    cameraSettingsSlot: @Composable () -> Unit = { DefaultCameraSettings() },
    recordingSettingsSlot: @Composable () -> Unit = { DefaultRecordingSettings() },
    appSettingsSlot: @Composable () -> Unit = { DefaultAppSettings(versionInfo = versionInfo) }
) {
    val viewModel: SettingsViewModel = hiltViewModel()
    val permissionStates = rememberMultiplePermissionsState(
        permissions =
        listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
    )

    viewModel.setGrantedPermissions(permissionStates)

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
        Column(
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

@Composable
fun DefaultCameraSettings(
    customEffectSlot: @Composable () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.settingsUiState.collectAsState()
    if (uiState !is SettingsUiState.Enabled) return
    val enabledState = uiState as SettingsUiState.Enabled

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

@Composable
fun DefaultRecordingSettings(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.settingsUiState.collectAsState()
    if (uiState !is SettingsUiState.Enabled) return
    val enabledState = uiState as SettingsUiState.Enabled

    DefaultRecordingSettings(
        enabledState = enabledState,
        setVideoAudio = viewModel::setVideoAudio,
        setMaxVideoDuration = viewModel::setMaxVideoDuration,
        setConcurrentCameraMode = viewModel::setConcurrentCameraMode,
        setStabilizationMode = viewModel::setStabilizationMode,
        setVideoQuality = viewModel::setVideoQuality
    )
}

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

@Composable
fun DefaultAppSettings(
    versionInfo: VersionInfoHolder,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.settingsUiState.collectAsState()
    if (uiState !is SettingsUiState.Enabled) return
    val enabledState = uiState as SettingsUiState.Enabled

    DefaultAppSettings(
        versionInfo = versionInfo,
        enabledState = enabledState,
        setDarkMode = viewModel::setDarkMode
    )
}

@Composable
fun DefaultAppSettings(
    versionInfo: VersionInfoHolder,
    enabledState: SettingsUiState.Enabled,
    setDarkMode: (DarkMode) -> Unit
) {
    SectionHeader(title = stringResource(id = R.string.section_title_app_settings))

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

data class VersionInfoHolder(val versionName: String, val buildType: String)
