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

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.ui.AspectRatioSetting
import com.google.jetpackcamera.settings.ui.CaptureModeSetting
import com.google.jetpackcamera.settings.ui.DarkModeSetting
import com.google.jetpackcamera.settings.ui.DefaultCameraFacing
import com.google.jetpackcamera.settings.ui.FlashModeSetting
import com.google.jetpackcamera.settings.ui.SectionHeader
import com.google.jetpackcamera.settings.ui.SettingsPageHeader
import com.google.jetpackcamera.settings.ui.StabilizationSetting
import com.google.jetpackcamera.settings.ui.VersionInfo
import com.google.jetpackcamera.settings.ui.theme.SettingsPreviewTheme
import com.google.jetpackcamera.settings.ui.TargetFpsSetting

/**
 * Screen used for the Settings feature.
 */
@Composable
fun SettingsScreen(
    versionInfo: VersionInfoHolder,
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val settingsUiState by viewModel.settingsUiState.collectAsState()

    SettingsScreen(
        uiState = settingsUiState,
        versionInfo = versionInfo,
        onNavigateBack = onNavigateBack,
        setDefaultLensFacing = viewModel::setDefaultLensFacing,
        setFlashMode = viewModel::setFlashMode,
        setTargetFrameRate = viewModel::setTargetFrameRate,
        setAspectRatio = viewModel::setAspectRatio,
        setCaptureMode = viewModel::setCaptureMode,
        setVideoStabilization = viewModel::setVideoStabilization,
        setPreviewStabilization = viewModel::setPreviewStabilization,
        setDarkMode = viewModel::setDarkMode
    )
}

@Composable
private fun SettingsScreen(
    uiState: SettingsUiState,
    versionInfo: VersionInfoHolder,
    onNavigateBack: () -> Unit = {},
    setDefaultLensFacing: (LensFacing) -> Unit = {},
    setFlashMode: (FlashMode) -> Unit = {},
    setTargetFrameRate: (Int) -> Unit = {},
    setAspectRatio: (AspectRatio) -> Unit = {},
    setCaptureMode: (CaptureMode) -> Unit = {},
    setVideoStabilization: (Stabilization) -> Unit = {},
    setPreviewStabilization: (Stabilization) -> Unit = {},
    setDarkMode: (DarkMode) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
    ) {
        SettingsPageHeader(
            title = stringResource(id = R.string.settings_title),
            navBack = onNavigateBack
        )
        SettingsList(
            uiState = uiState,
            versionInfo = versionInfo,
            setDefaultLensFacing = setDefaultLensFacing,
            setFlashMode = setFlashMode,
            setTargetFrameRate = setTargetFrameRate,
            setAspectRatio = setAspectRatio,
            setCaptureMode = setCaptureMode,
            setVideoStabilization = setVideoStabilization,
            setPreviewStabilization = setPreviewStabilization,
            setDarkMode = setDarkMode
        )
    }
}

@Composable
fun SettingsList(
    uiState: SettingsUiState,
    versionInfo: VersionInfoHolder,
    setDefaultLensFacing: (LensFacing) -> Unit = {},
    setFlashMode: (FlashMode) -> Unit = {},
    setTargetFrameRate: (Int) -> Unit = {},
    setAspectRatio: (AspectRatio) -> Unit = {},
    setCaptureMode: (CaptureMode) -> Unit = {},
    setVideoStabilization: (Stabilization) -> Unit = {},
    setPreviewStabilization: (Stabilization) -> Unit = {},
    setDarkMode: (DarkMode) -> Unit = {}
) {
    SectionHeader(title = stringResource(id = R.string.section_title_camera_settings))

    DefaultCameraFacing(
        cameraAppSettings = uiState.cameraAppSettings,
        setDefaultLensFacing = setDefaultLensFacing
    )

    FlashModeSetting(
        currentFlashMode = uiState.cameraAppSettings.flashMode,
        setFlashMode = setFlashMode
    )

    TargetFpsSetting(
        currentTargetFps = uiState.cameraAppSettings.targetFrameRate,
        supportedFps = uiState.cameraAppSettings.supportedFixedFrameRates,
        setTargetFps = setTargetFrameRate
    )

    AspectRatioSetting(
        currentAspectRatio = uiState.cameraAppSettings.aspectRatio,
        setAspectRatio = setAspectRatio
    )

    CaptureModeSetting(
        currentCaptureMode = uiState.cameraAppSettings.captureMode,
        setCaptureMode = setCaptureMode
    )

    StabilizationSetting(
        currentVideoStabilization = uiState.cameraAppSettings.videoCaptureStabilization,
        currentPreviewStabilization = uiState.cameraAppSettings.previewStabilization,
        currentTargetFps = uiState.cameraAppSettings.targetFrameRate,
        supportedStabilizationMode = uiState.cameraAppSettings.supportedStabilizationModes,
        setVideoStabilization = setVideoStabilization,
        setPreviewStabilization = setPreviewStabilization
    )

    SectionHeader(title = stringResource(id = R.string.section_title_app_settings))

    DarkModeSetting(
        currentDarkMode = uiState.cameraAppSettings.darkMode,
        setDarkMode = setDarkMode
    )

    SectionHeader(title = stringResource(id = R.string.section_title_software_info))

    VersionInfo(
        versionName = versionInfo.versionName,
        buildType = versionInfo.buildType
    )
}

data class VersionInfoHolder(
    val versionName: String,
    val buildType: String
)

@Preview(name = "Light Mode", showBackground = true)
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
fun Preview_SettingsScreen() {
    SettingsPreviewTheme {
        SettingsScreen(
            uiState = SettingsUiState(DEFAULT_CAMERA_APP_SETTINGS),
            versionInfo = VersionInfoHolder(
                versionName = "1.0.0",
                buildType = "release"
            )
        )
    }
}
