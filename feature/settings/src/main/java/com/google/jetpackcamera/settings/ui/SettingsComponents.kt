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
package com.google.jetpackcamera.settings.ui

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.settings.AspectRatioUiState
import com.google.jetpackcamera.settings.CaptureModeUiState
import com.google.jetpackcamera.settings.DarkModeUiState
import com.google.jetpackcamera.settings.DisabledRationale
import com.google.jetpackcamera.settings.FIVE_SECONDS_DURATION
import com.google.jetpackcamera.settings.FPS_15
import com.google.jetpackcamera.settings.FPS_30
import com.google.jetpackcamera.settings.FPS_60
import com.google.jetpackcamera.settings.FPS_AUTO
import com.google.jetpackcamera.settings.FlashUiState
import com.google.jetpackcamera.settings.FlipLensUiState
import com.google.jetpackcamera.settings.FpsUiState
import com.google.jetpackcamera.settings.MaxVideoDurationUiState
import com.google.jetpackcamera.settings.R
import com.google.jetpackcamera.settings.SIXTY_SECONDS_DURATION
import com.google.jetpackcamera.settings.SettingsViewModel
import com.google.jetpackcamera.settings.SingleSelectableState
import com.google.jetpackcamera.settings.StabilizationUiState
import com.google.jetpackcamera.settings.TEN_SECONDS_DURATION
import com.google.jetpackcamera.settings.THIRTY_SECONDS_DURATION
import com.google.jetpackcamera.settings.UNLIMITED_VIDEO_DURATION
import com.google.jetpackcamera.settings.VideoQualityUiState
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.DarkMode
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.VideoQuality
import com.google.jetpackcamera.settings.ui.theme.SettingsPreviewTheme

/**
 * MAJOR SETTING UI COMPONENTS
 * these are ready to be popped into the ui
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageHeader(title: String, navBack: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(title)
        },
        navigationIcon = {
            IconButton(
                modifier = Modifier.testTag(BACK_BUTTON),
                onClick = { navBack() }
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    stringResource(id = R.string.nav_back_accessibility)
                )
            }
        }
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(start = 20.dp, top = 10.dp),
        text = title,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 18.sp
    )
}

@Composable
fun DarkModeSetting(
    darkModeUiState: DarkModeUiState,
    setDarkMode: (DarkMode) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPopupSetting(
        modifier = modifier,
        title = stringResource(id = R.string.dark_mode_title),
        leadingIcon = null,
        enabled = true,
        description = when (darkModeUiState) {
            is DarkModeUiState.Enabled -> {
                when (darkModeUiState.currentDarkMode) {
                    DarkMode.SYSTEM -> stringResource(id = R.string.dark_mode_description_system)
                    DarkMode.DARK -> stringResource(id = R.string.dark_mode_description_dark)
                    DarkMode.LIGHT -> stringResource(id = R.string.dark_mode_description_light)
                }
            }
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    text = stringResource(id = R.string.dark_mode_selector_dark),
                    selected = darkModeUiState.currentDarkMode == DarkMode.DARK,
                    enabled = true,
                    onClick = { setDarkMode(DarkMode.DARK) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.dark_mode_selector_light),
                    selected = darkModeUiState.currentDarkMode == DarkMode.LIGHT,
                    enabled = true,
                    onClick = { setDarkMode(DarkMode.LIGHT) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.dark_mode_selector_system),
                    selected = darkModeUiState.currentDarkMode == DarkMode.SYSTEM,
                    enabled = true,
                    onClick = { setDarkMode(DarkMode.SYSTEM) }
                )
            }
        }
    )
}

@Composable
fun DefaultCameraFacing(
    modifier: Modifier = Modifier,
    lensUiState: FlipLensUiState,
    setDefaultLensFacing: (LensFacing) -> Unit
) {
    SwitchSettingUI(
        modifier = modifier.apply {
            if (lensUiState is FlipLensUiState.Disabled) {
                testTag(lensUiState.disabledRationale.testTag)
            }
        },
        title = stringResource(id = R.string.default_facing_camera_title),
        description = when (lensUiState) {
            is FlipLensUiState.Disabled -> {
                disabledRationaleString(disabledRationale = lensUiState.disabledRationale)
            }

            is FlipLensUiState.Enabled -> {
                null
            }
        },
        leadingIcon = null,
        onSwitchChanged = { on ->
            setDefaultLensFacing(if (on) LensFacing.FRONT else LensFacing.BACK)
        },
        settingValue = lensUiState.currentLensFacing == LensFacing.FRONT,
        enabled = lensUiState is FlipLensUiState.Enabled
    )
}

@Composable
fun FlashModeSetting(
    flashUiState: FlashUiState,
    setFlashMode: (FlashMode) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPopupSetting(
        modifier = modifier,
        title = stringResource(id = R.string.flash_mode_title),
        leadingIcon = null,
        enabled = true,
        description =
        if (flashUiState is FlashUiState.Enabled) {
            when (flashUiState.currentFlashMode) {
                FlashMode.AUTO -> stringResource(id = R.string.flash_mode_description_auto)
                FlashMode.ON -> stringResource(id = R.string.flash_mode_description_on)
                FlashMode.OFF -> stringResource(id = R.string.flash_mode_description_off)
                FlashMode.LOW_LIGHT_BOOST -> stringResource(
                    id = R.string.flash_mode_description_llb
                )
            }
        } else {
            TODO("flash mode currently has no disabled criteria")
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    text = stringResource(id = R.string.flash_mode_selector_auto),
                    selected = flashUiState.currentFlashMode == FlashMode.AUTO,
                    enabled = true,
                    onClick = { setFlashMode(FlashMode.AUTO) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.flash_mode_selector_on),
                    selected = flashUiState.currentFlashMode == FlashMode.ON,
                    enabled = true,
                    onClick = { setFlashMode(FlashMode.ON) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.flash_mode_selector_off),
                    selected = flashUiState.currentFlashMode == FlashMode.OFF,
                    enabled = true,
                    onClick = { setFlashMode(FlashMode.OFF) }
                )
                // TODO(yasith): Add logic to only show LLB toggle if current lens supports LLB
                SingleChoiceSelector(
                    text = stringResource(id = R.string.flash_mode_selector_llb),
                    selected = flashUiState.currentFlashMode == FlashMode.LOW_LIGHT_BOOST,
                    enabled = true,
                    onClick = { setFlashMode(FlashMode.LOW_LIGHT_BOOST) }
                )
            }
        }
    )
}

@Composable
fun AspectRatioSetting(
    aspectRatioUiState: AspectRatioUiState,
    setAspectRatio: (AspectRatio) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPopupSetting(
        modifier = modifier,
        title = stringResource(id = R.string.aspect_ratio_title),
        leadingIcon = null,
        description =
        if (aspectRatioUiState is AspectRatioUiState.Enabled) {
            when (aspectRatioUiState.currentAspectRatio) {
                AspectRatio.NINE_SIXTEEN -> stringResource(
                    id = R.string.aspect_ratio_description_9_16
                )

                AspectRatio.THREE_FOUR -> stringResource(id = R.string.aspect_ratio_description_3_4)
                AspectRatio.ONE_ONE -> stringResource(id = R.string.aspect_ratio_description_1_1)
            }
        } else {
            TODO("aspect ratio currently has no disabled criteria")
        },
        enabled = true,
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    text = stringResource(id = R.string.aspect_ratio_selector_9_16),
                    selected = aspectRatioUiState.currentAspectRatio == AspectRatio.NINE_SIXTEEN,
                    enabled = true,
                    onClick = { setAspectRatio(AspectRatio.NINE_SIXTEEN) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.aspect_ratio_selector_3_4),
                    selected = aspectRatioUiState.currentAspectRatio == AspectRatio.THREE_FOUR,
                    enabled = true,
                    onClick = { setAspectRatio(AspectRatio.THREE_FOUR) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.aspect_ratio_selector_1_1),
                    selected = aspectRatioUiState.currentAspectRatio == AspectRatio.ONE_ONE,
                    enabled = true,
                    onClick = { setAspectRatio(AspectRatio.ONE_ONE) }
                )
            }
        }
    )
}

@Composable
fun CaptureModeSetting(
    captureModeUiState: CaptureModeUiState,
    setCaptureMode: (CaptureMode) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPopupSetting(
        modifier = modifier,
        title = stringResource(R.string.capture_mode_title),
        leadingIcon = null,
        enabled = true,
        description =
        if (captureModeUiState is CaptureModeUiState.Enabled) {
            when (captureModeUiState.currentCaptureMode) {
                CaptureMode.MULTI_STREAM -> stringResource(
                    id = R.string.capture_mode_description_multi_stream
                )

                CaptureMode.SINGLE_STREAM -> stringResource(
                    id = R.string.capture_mode_description_single_stream
                )
            }
        } else {
            TODO("capture mode currently has no disabled criteria")
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    text = stringResource(id = R.string.capture_mode_selector_multi_stream),
                    selected = captureModeUiState.currentCaptureMode == CaptureMode.MULTI_STREAM,
                    enabled = true,
                    onClick = { setCaptureMode(CaptureMode.MULTI_STREAM) }
                )
                SingleChoiceSelector(
                    text = stringResource(id = R.string.capture_mode_description_single_stream),
                    selected = captureModeUiState.currentCaptureMode == CaptureMode.SINGLE_STREAM,
                    enabled = true,
                    onClick = { setCaptureMode(CaptureMode.SINGLE_STREAM) }
                )
            }
        }
    )
}

@Composable
fun MaxVideoDurationSetting(
    maxVideoDurationUiState: MaxVideoDurationUiState.Enabled,
    setMaxDuration: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPopupSetting(
        modifier = modifier,
        enabled = true,
        title = stringResource(R.string.duration_title),
        leadingIcon = null,
        description = when (val maxDuration = maxVideoDurationUiState.currentMaxDurationMillis) {
            UNLIMITED_VIDEO_DURATION -> stringResource(R.string.duration_description_none)
            else -> stringResource(R.string.duration_description_seconds, (maxDuration / 1000))
        },
        popupContents = {
            Column(Modifier.selectableGroup()) {
                SingleChoiceSelector(
                    enabled = true,
                    text = stringResource(R.string.duration_description_none),
                    selected = maxVideoDurationUiState.currentMaxDurationMillis
                        == UNLIMITED_VIDEO_DURATION,
                    onClick = { setMaxDuration(UNLIMITED_VIDEO_DURATION) }
                )
                listOf(
                    FIVE_SECONDS_DURATION,
                    TEN_SECONDS_DURATION,
                    THIRTY_SECONDS_DURATION,
                    SIXTY_SECONDS_DURATION
                ).forEach { maxDuration ->
                    SingleChoiceSelector(
                        enabled = true,
                        text = stringResource(
                            R.string.duration_description_seconds,
                            (maxDuration / 1000)
                        ),
                        selected = maxVideoDurationUiState.currentMaxDurationMillis == maxDuration,
                        onClick = { setMaxDuration(maxDuration) }
                    )
                }
            }
        }
    )
}

@Composable
fun TargetFpsSetting(
    fpsUiState: FpsUiState,
    setTargetFps: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPopupSetting(
        modifier = modifier.apply {
            if (fpsUiState is FpsUiState.Disabled) {
                testTag(fpsUiState.disabledRationale.testTag)
            }
        },
        title = stringResource(id = R.string.fps_title),
        enabled = fpsUiState is FpsUiState.Enabled,
        leadingIcon = null,
        description = if (fpsUiState is FpsUiState.Enabled) {
            when (fpsUiState.currentSelection) {
                FPS_15 -> stringResource(id = R.string.fps_description, FPS_15)
                FPS_30 -> stringResource(id = R.string.fps_description, FPS_30)
                FPS_60 -> stringResource(id = R.string.fps_description, FPS_60)
                else -> stringResource(
                    id = R.string.fps_description_auto
                )
            }
        } else {
            disabledRationaleString((fpsUiState as FpsUiState.Disabled).disabledRationale)
        },
        popupContents = {
            if (fpsUiState is FpsUiState.Enabled) {
                Column(Modifier.selectableGroup()) {
                    Text(
                        text = stringResource(id = R.string.fps_stabilization_disclaimer),
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    SingleChoiceSelector(
                        text = stringResource(id = R.string.fps_selector_auto),
                        selected = fpsUiState.currentSelection == FPS_AUTO,
                        onClick = { setTargetFps(FPS_AUTO) },
                        enabled = fpsUiState.fpsAutoState is SingleSelectableState.Selectable
                    )
                    listOf(FPS_15, FPS_30, FPS_60).forEach { fpsOption ->
                        SingleChoiceSelector(
                            text = "%d".format(fpsOption),
                            selected = fpsUiState.currentSelection == fpsOption,
                            onClick = { setTargetFps(fpsOption) },
                            enabled = when (fpsOption) {
                                FPS_15 ->
                                    fpsUiState.fpsFifteenState is
                                        SingleSelectableState.Selectable

                                FPS_30 ->
                                    fpsUiState.fpsThirtyState is
                                        SingleSelectableState.Selectable

                                FPS_60 ->
                                    fpsUiState.fpsSixtyState is
                                        SingleSelectableState.Selectable

                                else -> false
                            }
                        )
                    }
                }
            }
        }
    )
}

/**
 * Returns the description text depending on the preview/video stabilization configuration.
 * On - preview is on and video is NOT off.
 * High Quality - preview is unspecified and video is ON.
 * Off - Every other configuration.
 */
private fun getStabilizationStringRes(stabilizationMode: StabilizationMode): Int =
    when (stabilizationMode) {
        StabilizationMode.OFF -> R.string.stabilization_description_off
        StabilizationMode.AUTO -> R.string.stabilization_description_auto
        StabilizationMode.ON -> R.string.stabilization_description_on
        StabilizationMode.HIGH_QUALITY -> R.string.stabilization_description_high_quality
    }

private fun getVideoQualityStringRes(videoQuality: VideoQuality): Int =
    when (videoQuality) {
        VideoQuality.AUTO -> R.string.video_quality_value_auto
        VideoQuality.LOWEST -> R.string.video_quality_value_lowest
        VideoQuality.HIGHEST -> R.string.video_quality_value_highest
        VideoQuality.SD -> R.string.video_quality_value_sd
        VideoQuality.HD -> R.string.video_quality_value_hd
        VideoQuality.FHD -> R.string.video_quality_value_fhd
        VideoQuality.UHD -> R.string.video_quality_value_uhd
    }

private fun getVideoQualitySecondaryStringRes(videoQuality: VideoQuality): Int =
    when (videoQuality) {
        VideoQuality.AUTO -> R.string.video_quality_value_auto_info
        VideoQuality.LOWEST -> R.string.video_quality_value_lowest_info
        VideoQuality.HIGHEST -> R.string.video_quality_value_highest_info
        VideoQuality.SD -> R.string.video_quality_value_sd_info
        VideoQuality.HD -> R.string.video_quality_value_hd_info
        VideoQuality.FHD -> R.string.video_quality_value_fhd_info
        VideoQuality.UHD -> R.string.video_quality_value_uhd_info
    }

private fun getVideoQualityOptionTestTag(quality: VideoQuality): String =
    when (quality) {
        VideoQuality.AUTO -> VIDEO_QUALITY_OPTION_AUTO_TAG
        VideoQuality.LOWEST -> VIDEO_QUALITY_OPTION_LOWEST_TAG
        VideoQuality.HIGHEST -> VIDEO_QUALITY_OPTION_HIGHEST_TAG
        VideoQuality.SD -> VIDEO_QUALITY_OPTION_SD_TAG
        VideoQuality.HD -> VIDEO_QUALITY_OPTION_HD_TAG
        VideoQuality.FHD -> VIDEO_QUALITY_OPTION_FHD_TAG
        VideoQuality.UHD -> VIDEO_QUALITY_OPTION_UHD_TAG
    }

/**
 * A Setting to set preview and video stabilization.
 *
 * ON - Both preview and video are stabilized.
 * HIGH_QUALITY - Video will be stabilized, preview might be stabilized, depending on the device.
 * OFF - Preview and video stabilization is disabled.
 *
 * @param stabilizationUiState the state for this setting.
 */
@Composable
fun StabilizationSetting(
    stabilizationUiState: StabilizationUiState,
    setStabilizationMode: (StabilizationMode) -> Unit,
    modifier: Modifier = Modifier
) {
    // entire setting disabled when no available fps or target fps = 60
    // stabilization is unsupported >30 fps
    BasicPopupSetting(
        modifier = modifier.apply {
            when (stabilizationUiState) {
                is StabilizationUiState.Disabled ->
                    testTag(stabilizationUiState.disabledRationale.testTag)

                else -> {}
            }
        },
        title = stringResource(R.string.video_stabilization_title),
        leadingIcon = null,
        enabled = stabilizationUiState is StabilizationUiState.Enabled,
        description = when (stabilizationUiState) {
            is StabilizationUiState.Enabled ->
                stringResource(
                    id = getStabilizationStringRes(stabilizationUiState.currentStabilizationMode)
                )

            is StabilizationUiState.Disabled -> {
                // disabled setting description
                disabledRationaleString(stabilizationUiState.disabledRationale)
            }
        },

        popupContents = {
            Column(Modifier.selectableGroup()) {
                Text(
                    text = stringResource(id = R.string.lens_stabilization_disclaimer),
                    fontStyle = FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                // on (preview) selector
                // disabled if target fps != (30 or off)
                // TODO(b/328223562): device always resolves to 30fps when using preview stabilization
                when (stabilizationUiState) {
                    is StabilizationUiState.Enabled -> {
                        SingleChoiceSelector(
                            modifier = Modifier.apply {
                                if (stabilizationUiState.stabilizationAutoState
                                        is SingleSelectableState.Disabled
                                ) {
                                    testTag(
                                        stabilizationUiState.stabilizationAutoState
                                            .disabledRationale.testTag
                                    )
                                }
                            },
                            text = stringResource(id = R.string.stabilization_selector_auto),
                            secondaryText = stringResource(
                                id = R.string.stabilization_selector_auto_info
                            ),
                            enabled = stabilizationUiState.stabilizationAutoState is
                                SingleSelectableState.Selectable,
                            selected = stabilizationUiState.currentStabilizationMode
                                == StabilizationMode.AUTO,
                            onClick = {
                                setStabilizationMode(StabilizationMode.AUTO)
                            }
                        )

                        SingleChoiceSelector(
                            modifier = Modifier.apply {
                                if (stabilizationUiState.stabilizationOnState
                                        is SingleSelectableState.Disabled
                                ) {
                                    testTag(
                                        stabilizationUiState.stabilizationOnState
                                            .disabledRationale.testTag
                                    )
                                }
                            },
                            text = stringResource(id = R.string.stabilization_selector_on),
                            secondaryText = stringResource(
                                id = R.string.stabilization_selector_on_info
                            ),
                            enabled = stabilizationUiState.stabilizationOnState is
                                SingleSelectableState.Selectable,
                            selected = stabilizationUiState.currentStabilizationMode
                                == StabilizationMode.ON,
                            onClick = {
                                setStabilizationMode(StabilizationMode.ON)
                            }
                        )

                        // high quality selector
                        // disabled if target fps = 60 (see VideoCapabilities.isStabilizationSupported)
                        SingleChoiceSelector(
                            modifier = Modifier.apply {
                                if (stabilizationUiState.stabilizationHighQualityState
                                        is SingleSelectableState.Disabled
                                ) {
                                    testTag(
                                        stabilizationUiState.stabilizationHighQualityState
                                            .disabledRationale.testTag
                                    )
                                }
                            },
                            text = stringResource(
                                id = R.string.stabilization_selector_high_quality
                            ),
                            secondaryText = stringResource(
                                id = R.string.stabilization_selector_high_quality_info
                            ),
                            enabled = stabilizationUiState.stabilizationHighQualityState
                                == SingleSelectableState.Selectable,

                            selected = stabilizationUiState.currentStabilizationMode
                                == StabilizationMode.HIGH_QUALITY,
                            onClick = {
                                setStabilizationMode(StabilizationMode.HIGH_QUALITY)
                            }
                        )

                        // off selector
                        SingleChoiceSelector(
                            text = stringResource(id = R.string.stabilization_selector_off),
                            selected = stabilizationUiState.currentStabilizationMode
                                == StabilizationMode.OFF,
                            onClick = {
                                setStabilizationMode(StabilizationMode.OFF)
                            },
                            enabled = true
                        )
                    }

                    else -> {}
                }
            }
        }
    )
}

@Composable
fun VideoQualitySetting(
    videQualityUiState: VideoQualityUiState,
    setVideoQuality: (VideoQuality) -> Unit,
    modifier: Modifier = Modifier
) {
    BasicPopupSetting(
        modifier = modifier.testTag(VIDEO_QUALITY_SELECTOR_TAG),
        title = stringResource(R.string.video_quality_title),
        leadingIcon = null,
        enabled = videQualityUiState is VideoQualityUiState.Enabled,
        description = when (videQualityUiState) {
            is VideoQualityUiState.Enabled ->
                stringResource(
                    R.string.video_quality_description_current,
                    stringResource(getVideoQualityStringRes(videQualityUiState.currentVideoQuality))
                )

            is VideoQualityUiState.Disabled -> {
                    disabledRationaleString(disabledRationale = videQualityUiState.disabledRationale)
            }
        },
        popupContents = {
            Column(Modifier.selectableGroup().verticalScroll(rememberScrollState())) {
                SingleChoiceSelector(
                    modifier = Modifier.testTag(getVideoQualityOptionTestTag(VideoQuality.AUTO)),
                    text = stringResource(getVideoQualityStringRes(VideoQuality.AUTO)),
                    secondaryText = stringResource(
                        getVideoQualitySecondaryStringRes(
                            VideoQuality.AUTO
                        )
                    ),
                    selected = (videQualityUiState as VideoQualityUiState.Enabled)
                        .currentVideoQuality == VideoQuality.AUTO,
                    enabled = videQualityUiState.videoQualityAutoState is
                            SingleSelectableState.Selectable,
                    onClick = { setVideoQuality(VideoQuality.AUTO) }
                )
                SingleChoiceSelector(
                    modifier = Modifier.testTag(getVideoQualityOptionTestTag(VideoQuality.SD)),
                    text = stringResource(getVideoQualityStringRes(VideoQuality.SD)),
                    secondaryText = stringResource(
                        getVideoQualitySecondaryStringRes(
                            VideoQuality.SD
                        )
                    ),
                    selected = videQualityUiState.currentVideoQuality == VideoQuality.SD,
                    enabled = videQualityUiState.videoQualitySDState is
                            SingleSelectableState.Selectable,
                    onClick = { setVideoQuality(VideoQuality.SD) }
                )
                SingleChoiceSelector(
                    modifier = Modifier.testTag(getVideoQualityOptionTestTag(VideoQuality.HD)),
                    text = stringResource(getVideoQualityStringRes(VideoQuality.HD)),
                    secondaryText = stringResource(
                        getVideoQualitySecondaryStringRes(
                            VideoQuality.HD
                        )
                    ),
                    selected = videQualityUiState.currentVideoQuality == VideoQuality.HD,
                    enabled = videQualityUiState.videoQualityHDState is
                            SingleSelectableState.Selectable,
                    onClick = { setVideoQuality(VideoQuality.HD) }
                )
                SingleChoiceSelector(
                    modifier = Modifier.testTag(getVideoQualityOptionTestTag(VideoQuality.FHD)),
                    text = stringResource(getVideoQualityStringRes(VideoQuality.FHD)),
                    secondaryText = stringResource(
                        getVideoQualitySecondaryStringRes(
                            VideoQuality.FHD
                        )
                    ),
                    selected = videQualityUiState.currentVideoQuality == VideoQuality.FHD,
                    enabled = videQualityUiState.videoQualityFHDState is
                            SingleSelectableState.Selectable,
                    onClick = { setVideoQuality(VideoQuality.FHD) }
                )
                SingleChoiceSelector(
                    modifier = Modifier.testTag(getVideoQualityOptionTestTag(VideoQuality.UHD)),
                    text = stringResource(getVideoQualityStringRes(VideoQuality.UHD)),
                    secondaryText = stringResource(
                        getVideoQualitySecondaryStringRes(
                            VideoQuality.UHD
                        )
                    ),
                    selected = videQualityUiState.currentVideoQuality == VideoQuality.UHD,
                    enabled = videQualityUiState.videoQualityUHDState is
                            SingleSelectableState.Selectable,
                    onClick = { setVideoQuality(VideoQuality.UHD) }
                )
                SingleChoiceSelector(
                    modifier = Modifier.testTag(getVideoQualityOptionTestTag(VideoQuality.HIGHEST)),
                    text = stringResource(getVideoQualityStringRes(VideoQuality.HIGHEST)),
                    secondaryText = stringResource(
                        getVideoQualitySecondaryStringRes(
                            VideoQuality.HIGHEST
                        )
                    ),
                    selected = videQualityUiState.currentVideoQuality == VideoQuality.HIGHEST,
                    enabled = videQualityUiState.videoQualityHighestState is
                            SingleSelectableState.Selectable,
                    onClick = { setVideoQuality(VideoQuality.HIGHEST) }
                )
                SingleChoiceSelector(
                    modifier = Modifier.testTag(getVideoQualityOptionTestTag(VideoQuality.LOWEST)),
                    text = stringResource(getVideoQualityStringRes(VideoQuality.LOWEST)),
                    secondaryText = stringResource(
                        getVideoQualitySecondaryStringRes(
                            VideoQuality.LOWEST
                        )
                    ),
                    selected = videQualityUiState.currentVideoQuality == VideoQuality.LOWEST,
                    enabled = videQualityUiState.videoQualityLowestState is
                            SingleSelectableState.Selectable,
                    onClick = { setVideoQuality(VideoQuality.LOWEST) }
                )
            }
        }
    )
}

@Composable
fun VersionInfo(versionName: String, modifier: Modifier = Modifier, buildType: String = "") {
    SettingUI(
        modifier = modifier,
        title = stringResource(id = R.string.version_info_title),
        leadingIcon = null,
        enabled = true
    ) {
        val versionString = versionName +
            if (buildType.isNotEmpty()) {
                "/${buildType.toUpperCase(Locale.current)}"
            } else {
                ""
            }
        Text(text = versionString)
    }
}

/*
 * Setting UI sub-Components
 * small and whimsical :)
 * don't use these directly, use them to build the ready-to-use setting components
 */

/** a composable for creating a simple popup setting **/

@Composable
fun BasicPopupSetting(
    title: String,
    description: String?,
    leadingIcon: @Composable (() -> Unit)?,
    popupContents: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    popupStatus: MutableState<Boolean> = remember { mutableStateOf(false) }
) {
    SettingUI(
        modifier = modifier.clickable(enabled = enabled) { popupStatus.value = true },
        title = title,
        enabled = enabled,
        description = description,
        leadingIcon = leadingIcon,
        trailingContent = null
    )
    if (popupStatus.value) {
        AlertDialog(
            onDismissRequest = { popupStatus.value = false },
            confirmButton = {
                Text(
                    text = "Close",
                    modifier = Modifier.clickable { popupStatus.value = false }
                )
            },
            title = { Text(text = title) },
            text = {
                MaterialTheme(
                    colorScheme = MaterialTheme.colorScheme.copy(surface = Color.Transparent),
                    content = popupContents
                )
            }
        )
    }
}

/**
 * A composable for creating a setting with a Switch.
 *
 * <p> the value should correspond to the setting's UI state value. the switch will only change
 * appearance if the UI state has been successfully updated
 */
@Composable
fun SwitchSettingUI(
    title: String,
    description: String?,
    leadingIcon: @Composable (() -> Unit)?,
    onSwitchChanged: (Boolean) -> Unit,
    settingValue: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    SettingUI(
        modifier = modifier.toggleable(
            enabled = enabled,
            role = Role.Switch,
            value = settingValue,
            onValueChange = { value -> onSwitchChanged(value) }
        ),
        enabled = enabled,
        title = title,
        description = description,
        leadingIcon = leadingIcon,
        trailingContent = {
            Switch(
                enabled = enabled,
                checked = settingValue,
                onCheckedChange = { value ->
                    onSwitchChanged(value)
                }
            )
        }
    )
}

/**
 * A composable used as a template used to construct other settings components
 */
@Composable
fun SettingUI(
    title: String,
    leadingIcon: @Composable (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean,
    description: String? = null,
    trailingContent: @Composable (() -> Unit)?
) {
    ListItem(
        modifier = modifier,
        headlineContent = {
            if (enabled) {
                Text(title)
            } else {
                Text(text = title, color = LocalContentColor.current.copy(alpha = .7f))
            }
        },
        supportingContent = {
            if (description != null) {
                if (enabled) {
                    Text(description)
                } else {
                    Text(
                        text = description,
                        color = LocalContentColor.current.copy(alpha = .7f)
                    )
                }
            }
        },
        leadingContent = leadingIcon,
        trailingContent = trailingContent
    )
}

/**
 * A component for a single-choice selector for a multiple choice list
 */
@Composable
fun SingleChoiceSelector(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    secondaryText: String? = null,
    enabled: Boolean
) {
    Row(
        modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
                enabled = enabled
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingUI(
            title = text,
            description = secondaryText,
            enabled = enabled,
            leadingIcon = {
                RadioButton(
                    selected = selected,
                    onClick = onClick,
                    enabled = enabled
                )
            },
            trailingContent = null
        )
    }
}

@Composable
@ReadOnlyComposable
fun disabledRationaleString(disabledRationale: DisabledRationale): String {
    return when (disabledRationale) {
        is DisabledRationale.DeviceUnsupportedRationale -> stringResource(

            disabledRationale.reasonTextResId,
            stringResource(disabledRationale.affectedSettingNameResId)
        )

        is DisabledRationale.FpsUnsupportedRationale -> stringResource(
            disabledRationale.reasonTextResId,
            stringResource(disabledRationale.affectedSettingNameResId),
            disabledRationale.currentFps
        )

        is DisabledRationale.LensUnsupportedRationale -> stringResource(
            disabledRationale.reasonTextResId,
            stringResource(disabledRationale.affectedSettingNameResId)
        )

        is DisabledRationale.StabilizationUnsupportedRationale -> stringResource(
            disabledRationale.reasonTextResId,
            stringResource(disabledRationale.affectedSettingNameResId)
        )

        is DisabledRationale.VideoQualityUnsupportedRationale -> stringResource(
            disabledRationale.reasonTextResId,
            stringResource(disabledRationale.affectedSettingNameResId)
        )
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_VersionInfo() {
    SettingsPreviewTheme {
        VersionInfo(versionName = "0.1.0", buildType = "debug")
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_Popup() {
    SettingsPreviewTheme {
        BasicPopupSetting(
            title = "Test Popup",
            description = "Test Description",
            leadingIcon = null,
            popupContents = {
                Column(Modifier.selectableGroup()) {
                    Text(
                        text = "Test sub-text",
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    SingleChoiceSelector(
                        text = "Option 1",
                        selected = true,
                        enabled = true,
                        onClick = { }
                    )
                    SingleChoiceSelector(
                        text = "Option 2",
                        selected = false,
                        enabled = true,
                        onClick = { }
                    )
                }
            },
            enabled = true,
            popupStatus = remember { mutableStateOf(true) }
        )
    }
}
