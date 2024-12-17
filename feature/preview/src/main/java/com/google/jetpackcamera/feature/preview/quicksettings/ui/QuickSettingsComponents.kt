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
package com.google.jetpackcamera.feature.preview.quicksettings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.feature.preview.FlashModeUiState
import com.google.jetpackcamera.feature.preview.LowLightBoostUiState
import com.google.jetpackcamera.feature.preview.PreviewMode
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.quicksettings.CameraAspectRatio
import com.google.jetpackcamera.feature.preview.quicksettings.CameraCaptureMode
import com.google.jetpackcamera.feature.preview.quicksettings.CameraConcurrentCameraMode
import com.google.jetpackcamera.feature.preview.quicksettings.CameraDynamicRange
import com.google.jetpackcamera.feature.preview.quicksettings.CameraFlashMode
import com.google.jetpackcamera.feature.preview.quicksettings.CameraLensFace
import com.google.jetpackcamera.feature.preview.quicksettings.QuickSettingsEnum
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.ConcurrentCameraMode
import com.google.jetpackcamera.settings.model.DynamicRange
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.ImageOutputFormat
import com.google.jetpackcamera.settings.model.LensFacing
import kotlin.math.min

// completed components ready to go into preview screen

@Composable
fun ExpandedQuickSetRatio(
    setRatio: (aspectRatio: AspectRatio) -> Unit,
    currentRatio: AspectRatio,
    modifier: Modifier = Modifier
) {
    val buttons: Array<@Composable () -> Unit> =
        arrayOf(
            {
                QuickSetRatio(
                    modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_3_4_BUTTON),
                    onClick = { setRatio(AspectRatio.THREE_FOUR) },
                    ratio = AspectRatio.THREE_FOUR,
                    currentRatio = currentRatio,
                    isHighlightEnabled = true
                )
            },
            {
                QuickSetRatio(
                    modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_9_16_BUTTON),
                    onClick = { setRatio(AspectRatio.NINE_SIXTEEN) },
                    ratio = AspectRatio.NINE_SIXTEEN,
                    currentRatio = currentRatio,
                    isHighlightEnabled = true
                )
            },
            {
                QuickSetRatio(
                    modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_1_1_BUTTON),
                    onClick = { setRatio(AspectRatio.ONE_ONE) },
                    ratio = AspectRatio.ONE_ONE,
                    currentRatio = currentRatio,
                    isHighlightEnabled = true
                )
            }
        )
    ExpandedQuickSetting(modifier = modifier, quickSettingButtons = buttons)
}

@Composable
fun QuickSetHdr(
    modifier: Modifier = Modifier,
    onClick: (dynamicRange: DynamicRange, imageOutputFormat: ImageOutputFormat) -> Unit,
    selectedDynamicRange: DynamicRange,
    selectedImageOutputFormat: ImageOutputFormat,
    hdrDynamicRange: DynamicRange,
    hdrImageFormat: ImageOutputFormat,
    hdrDynamicRangeSupported: Boolean,
    previewMode: PreviewMode,
    enabled: Boolean
) {
    val enum =
        if (selectedDynamicRange == hdrDynamicRange ||
            selectedImageOutputFormat == hdrImageFormat
        ) {
            CameraDynamicRange.HDR
        } else {
            CameraDynamicRange.SDR
        }

    QuickSettingUiItem(
        modifier = modifier,
        enum = enum,
        onClick = {
            val newDynamicRange =
                if (selectedDynamicRange == DynamicRange.SDR && hdrDynamicRangeSupported) {
                    hdrDynamicRange
                } else {
                    DynamicRange.SDR
                }
            val newImageOutputFormat =
                if (!hdrDynamicRangeSupported ||
                    previewMode is PreviewMode.ExternalImageCaptureMode
                ) {
                    hdrImageFormat
                } else {
                    ImageOutputFormat.JPEG
                }
            onClick(newDynamicRange, newImageOutputFormat)
        },
        isHighLighted = (selectedDynamicRange != DynamicRange.SDR),
        enabled = enabled
    )
}

@Composable
fun QuickSetRatio(
    onClick: () -> Unit,
    ratio: AspectRatio,
    currentRatio: AspectRatio,
    modifier: Modifier = Modifier,
    isHighlightEnabled: Boolean = false
) {
    val enum =
        when (ratio) {
            AspectRatio.THREE_FOUR -> CameraAspectRatio.THREE_FOUR
            AspectRatio.NINE_SIXTEEN -> CameraAspectRatio.NINE_SIXTEEN
            AspectRatio.ONE_ONE -> CameraAspectRatio.ONE_ONE
            else -> CameraAspectRatio.ONE_ONE
        }
    QuickSettingUiItem(
        modifier = modifier,
        enum = enum,
        onClick = { onClick() },
        isHighLighted = isHighlightEnabled && (ratio == currentRatio)
    )
}

@Composable
fun QuickSetFlash(
    onClick: (FlashMode) -> Unit,
    flashModeUiState: FlashModeUiState,
    lowLightBoostUiState: LowLightBoostUiState,
    modifier: Modifier = Modifier
) {
    when (flashModeUiState) {
        is FlashModeUiState.Unavailable ->
            QuickSettingUiItem(
                modifier = modifier,
                enum = CameraFlashMode.OFF,
                enabled = false,
                onClick = {}
            )
        is FlashModeUiState.Available ->
            QuickSettingUiItem(
                modifier = modifier,
                enum = flashModeUiState.selectedFlashMode.toCameraFlashMode(lowLightBoostUiState),
                isHighLighted = flashModeUiState.selectedFlashMode == FlashMode.ON,
                onClick = {
                    onClick(flashModeUiState.getNextFlashMode())
                }
            )
    }
}

@Composable
fun QuickFlipCamera(
    setLensFacing: (LensFacing) -> Unit,
    currentLensFacing: LensFacing,
    modifier: Modifier = Modifier
) {
    val enum =
        when (currentLensFacing) {
            LensFacing.FRONT -> CameraLensFace.FRONT
            LensFacing.BACK -> CameraLensFace.BACK
        }
    QuickSettingUiItem(
        modifier = modifier,
        enum = enum,
        onClick = { setLensFacing(currentLensFacing.flip()) }
    )
}

@Composable
fun QuickSetCaptureMode(
    setCaptureMode: (CaptureMode) -> Unit,
    currentCaptureMode: CaptureMode,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val enum: CameraCaptureMode =
        when (currentCaptureMode) {
            CaptureMode.MULTI_STREAM -> CameraCaptureMode.MULTI_STREAM
            CaptureMode.SINGLE_STREAM -> CameraCaptureMode.SINGLE_STREAM
        }
    QuickSettingUiItem(
        modifier = modifier,
        enum = enum,
        onClick = {
            when (currentCaptureMode) {
                CaptureMode.MULTI_STREAM -> setCaptureMode(CaptureMode.SINGLE_STREAM)
                CaptureMode.SINGLE_STREAM -> setCaptureMode(CaptureMode.MULTI_STREAM)
            }
        },
        enabled = enabled
    )
}

@Composable
fun QuickSetConcurrentCamera(
    setConcurrentCameraMode: (ConcurrentCameraMode) -> Unit,
    currentConcurrentCameraMode: ConcurrentCameraMode,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val enum: CameraConcurrentCameraMode =
        when (currentConcurrentCameraMode) {
            ConcurrentCameraMode.OFF -> CameraConcurrentCameraMode.OFF
            ConcurrentCameraMode.DUAL -> CameraConcurrentCameraMode.DUAL
        }
    QuickSettingUiItem(
        modifier = modifier,
        enum = enum,
        onClick = {
            when (currentConcurrentCameraMode) {
                ConcurrentCameraMode.OFF -> setConcurrentCameraMode(ConcurrentCameraMode.DUAL)
                ConcurrentCameraMode.DUAL -> setConcurrentCameraMode(ConcurrentCameraMode.OFF)
            }
        },
        enabled = enabled
    )
}

/**
 * Button to toggle quick settings
 */
@Composable
fun ToggleQuickSettingsButton(
    toggleDropDown: () -> Unit,
    isOpen: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // dropdown icon
        Icon(
            imageVector = Icons.Filled.ExpandMore,
            contentDescription = if (isOpen) {
                stringResource(R.string.quick_settings_dropdown_open_description)
            } else {
                stringResource(R.string.quick_settings_dropdown_closed_description)
            },
            modifier = Modifier
                .testTag(QUICK_SETTINGS_DROP_DOWN)
                .size(72.dp)
                .clickable {
                    toggleDropDown()
                }
                .scale(1f, if (isOpen) -1f else 1f)
        )
    }
}

// subcomponents used to build completed components

@Composable
fun QuickSettingUiItem(
    enum: QuickSettingsEnum,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighLighted: Boolean = false,
    enabled: Boolean = true
) {
    QuickSettingUiItem(
        modifier = modifier,
        painter = enum.getPainter(),
        text = stringResource(id = enum.getTextResId()),
        accessibilityText = stringResource(id = enum.getDescriptionResId()),
        onClick = { onClick() },
        isHighLighted = isHighLighted,
        enabled = enabled
    )
}

/**
 * The itemized UI component representing each button in quick settings.
 */
@Composable
fun QuickSettingUiItem(
    text: String,
    painter: Painter,
    accessibilityText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isHighLighted: Boolean = false,
    enabled: Boolean = true
) {
    Column(
        modifier =
        modifier
            .wrapContentSize()
            .padding(dimensionResource(id = R.dimen.quick_settings_ui_item_padding))
            .clickable(onClick = onClick, enabled = enabled),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val contentColor = (if (isHighLighted) Color.Yellow else Color.White).let {
            // When in disabled state, material3 guidelines say the element's opacity should be 38%
            // See: https://m3.material.io/foundations/interaction/states/applying-states#3c3032e8-b07a-42ac-a508-a32f573cc7e1
            // and: https://developer.android.com/develop/ui/compose/designsystems/material2-material3#emphasis-and
            if (!enabled) it.copy(alpha = 0.38f) else it
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            Icon(
                painter = painter,
                contentDescription = accessibilityText,
                modifier = Modifier.size(
                    dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size)
                )
            )

            Text(text = text, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Should you want to have an expanded view of a single quick setting
 */
@Composable
fun ExpandedQuickSetting(
    modifier: Modifier = Modifier,
    vararg quickSettingButtons: @Composable () -> Unit
) {
    val expandedNumOfColumns =
        min(
            quickSettingButtons.size,
            (
                (
                    LocalConfiguration.current.screenWidthDp.dp - (
                        dimensionResource(
                            id = R.dimen.quick_settings_ui_horizontal_padding
                        ) * 2
                        )
                    ) /
                    (
                        dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size) +
                            (dimensionResource(id = R.dimen.quick_settings_ui_item_padding) * 2)
                        )
                ).toInt()
        )
    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(count = expandedNumOfColumns)
    ) {
        items(quickSettingButtons.size) { i ->
            quickSettingButtons[i]()
        }
    }
}

/**
 * Algorithm to determine dimensions of QuickSettings Icon layout
 */
@Composable
fun QuickSettingsGrid(
    modifier: Modifier = Modifier,
    quickSettingsButtons: List<@Composable () -> Unit>
) {
    val initialNumOfColumns =
        min(
            quickSettingsButtons.size,
            (
                (
                    LocalConfiguration.current.screenWidthDp.dp - (
                        dimensionResource(
                            id = R.dimen.quick_settings_ui_horizontal_padding
                        ) * 2
                        )
                    ) /
                    (
                        dimensionResource(id = R.dimen.quick_settings_ui_item_icon_size) +
                            (dimensionResource(id = R.dimen.quick_settings_ui_item_padding) * 2)
                        )
                ).toInt()
        )

    LazyVerticalGrid(
        modifier = modifier.fillMaxWidth(),
        columns = GridCells.Fixed(count = initialNumOfColumns)
    ) {
        items(quickSettingsButtons.size) { i ->
            quickSettingsButtons[i]()
        }
    }
}

/**
 * The top bar indicators for quick settings items.
 */
@Composable
fun TopBarSettingIndicator(
    enum: QuickSettingsEnum,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit = {}
) {
    val contentColor = Color.White.let {
        if (!enabled) it.copy(alpha = 0.38f) else it
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Icon(
            painter = enum.getPainter(),
            contentDescription = stringResource(id = enum.getDescriptionResId()),
            modifier = modifier
                .size(dimensionResource(id = R.dimen.quick_settings_indicator_size))
                .clickable(onClick = onClick, enabled = enabled)
        )
    }
}

@Composable
fun FlashModeIndicator(
    flashModeUiState: FlashModeUiState,
    lowLightBoostUiState: LowLightBoostUiState,
    onClick: (flashMode: FlashMode) -> Unit
) {
    when (flashModeUiState) {
        is FlashModeUiState.Unavailable ->
            TopBarSettingIndicator(
                enum = CameraFlashMode.OFF,
                enabled = false
            )
        is FlashModeUiState.Available ->
            TopBarSettingIndicator(
                enum = flashModeUiState.selectedFlashMode.toCameraFlashMode(lowLightBoostUiState),
                onClick = {
                    onClick(flashModeUiState.getNextFlashMode())
                }
            )
    }
}

@Composable
fun QuickSettingsIndicators(
    flashModeUiState: FlashModeUiState,
    lowLightBoostUiState: LowLightBoostUiState,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier) {
        FlashModeIndicator(
            flashModeUiState,
            lowLightBoostUiState,
            onFlashModeClick
        )
    }
}

private fun FlashModeUiState.Available.getNextFlashMode(): FlashMode = availableFlashModes.run {
    get((indexOf(selectedFlashMode) + 1) % size)
}

private fun FlashMode.toCameraFlashMode(lowLightBoostUiState: LowLightBoostUiState) = when (this) {
    FlashMode.OFF -> CameraFlashMode.OFF
    FlashMode.AUTO -> CameraFlashMode.AUTO
    FlashMode.ON -> CameraFlashMode.ON
    FlashMode.LOW_LIGHT_BOOST -> {
        when(lowLightBoostUiState) {
            LowLightBoostUiState.Active -> CameraFlashMode.LOW_LIGHT_BOOST_ACTIVE
            LowLightBoostUiState.Inactive -> CameraFlashMode.LOW_LIGHT_BOOST_INACTIVE
        }
    }
}
