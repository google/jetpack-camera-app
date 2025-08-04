/*
 * Copyright (C) 2025 The Android Open Source Project
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
package com.google.jetpackcamera.ui.components.capture.quicksettings.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DEFAULT_HDR_DYNAMIC_RANGE
import com.google.jetpackcamera.model.DEFAULT_HDR_IMAGE_OUTPUT
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_IMAGE_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_VIDEO_ONLY
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_3_4_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_9_16_BUTTON
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraAspectRatio
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraCaptureMode
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraConcurrentCameraMode
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraDynamicRange
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraFlashMode
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraLensFace
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraStreamConfig
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsEnum
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState.Unavailable.isCaptureModeSelectable
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState
import kotlin.math.min

@Composable
fun FocusedQuickSetRatio(
    setRatio: (aspectRatio: AspectRatio) -> Unit,
    aspectRatioUiState: AspectRatioUiState,
    modifier: Modifier = Modifier
) {
    if (aspectRatioUiState is AspectRatioUiState.Available) {
        val buttons: Array<@Composable () -> Unit> =
            arrayOf(
                {
                    QuickSetRatio(
                        modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_3_4_BUTTON),
                        onClick = { setRatio(AspectRatio.THREE_FOUR) },
                        ratio = AspectRatio.THREE_FOUR,
                        aspectRatioUiState = aspectRatioUiState,
                        isHighlightEnabled = true
                    )
                },
                {
                    QuickSetRatio(
                        modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_9_16_BUTTON),
                        onClick = { setRatio(AspectRatio.NINE_SIXTEEN) },
                        ratio = AspectRatio.NINE_SIXTEEN,
                        aspectRatioUiState = aspectRatioUiState,
                        isHighlightEnabled = true
                    )
                },
                {
                    QuickSetRatio(
                        modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_1_1_BUTTON),
                        onClick = { setRatio(AspectRatio.ONE_ONE) },
                        ratio = AspectRatio.ONE_ONE,
                        aspectRatioUiState = aspectRatioUiState,
                        isHighlightEnabled = true
                    )
                }
            )
        ExpandedQuickSetting(modifier = modifier, quickSettingButtons = buttons)
    }
}

@Composable
fun FocusedQuickSetCaptureMode(
    modifier: Modifier = Modifier,
    onSetCaptureMode: (CaptureMode) -> Unit,
    captureModeUiState: CaptureModeUiState
) {
    val buttons: Array<@Composable () -> Unit> =
        if (captureModeUiState is CaptureModeUiState.Available) {
            arrayOf(
                {
                    QuickSetCaptureMode(
                        modifier = Modifier
                            .testTag(BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_OPTION_STANDARD),
                        onClick = { onSetCaptureMode(CaptureMode.STANDARD) },
                        assignedCaptureMode = CaptureMode.STANDARD,
                        captureModeUiState = captureModeUiState,
                        isHighlightEnabled = true
                    )
                },
                {
                    QuickSetCaptureMode(
                        modifier = Modifier
                            .testTag(BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_VIDEO_ONLY),
                        onClick = { onSetCaptureMode(CaptureMode.VIDEO_ONLY) },
                        assignedCaptureMode = CaptureMode.VIDEO_ONLY,
                        captureModeUiState = captureModeUiState,
                        isHighlightEnabled = true
                    )
                },
                {
                    QuickSetCaptureMode(
                        modifier = Modifier
                            .testTag(BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_IMAGE_ONLY),
                        onClick = { onSetCaptureMode(CaptureMode.IMAGE_ONLY) },
                        assignedCaptureMode = CaptureMode.IMAGE_ONLY,
                        captureModeUiState = captureModeUiState,
                        isHighlightEnabled = true
                    )
                }
            )
        } else {
            emptyArray()
        }
    ExpandedQuickSetting(modifier = modifier, quickSettingButtons = buttons)
}

@Composable
fun QuickSetCaptureMode(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    captureModeUiState: CaptureModeUiState,
    assignedCaptureMode: CaptureMode?,
    isHighlightEnabled: Boolean = false
) {
    if (captureModeUiState is CaptureModeUiState.Available) {
        val captureToUse = assignedCaptureMode ?: captureModeUiState.selectedCaptureMode
        val enum = when (captureToUse) {
            CaptureMode.STANDARD -> CameraCaptureMode.STANDARD
            CaptureMode.VIDEO_ONLY -> CameraCaptureMode.VIDEO_ONLY
            CaptureMode.IMAGE_ONLY -> CameraCaptureMode.IMAGE_ONLY
        }

        QuickSettingToggleButton(
            modifier = modifier,
            enum = enum,
            onClick = { onClick() },
            enabled = when (assignedCaptureMode) {
                null -> {
                    // only enabled if there are at least 2 supported capturemodes
                    captureModeUiState.availableCaptureModes.count {
                        it is SingleSelectableUiState.SelectableUi
                    } >= 2
                }

                CaptureMode.STANDARD ->
                    captureModeUiState.isCaptureModeSelectable(CaptureMode.STANDARD)

                CaptureMode.VIDEO_ONLY ->
                    captureModeUiState.isCaptureModeSelectable(CaptureMode.VIDEO_ONLY)

                CaptureMode.IMAGE_ONLY ->
                    captureModeUiState.isCaptureModeSelectable(CaptureMode.IMAGE_ONLY)
            },
            isHighLighted =
            isHighlightEnabled && (assignedCaptureMode == captureModeUiState.selectedCaptureMode)
        )
    }
}

@Composable
fun QuickSetHdr(
    modifier: Modifier = Modifier,
    onClick: (DynamicRange, ImageOutputFormat) -> Unit,
    hdrUiState: HdrUiState
) {
    val enum =
        if (hdrUiState is HdrUiState.Available &&
            (
                hdrUiState.selectedDynamicRange == DEFAULT_HDR_DYNAMIC_RANGE ||
                    hdrUiState.selectedImageFormat == DEFAULT_HDR_IMAGE_OUTPUT
                )
        ) {
            CameraDynamicRange.HDR
        } else {
            CameraDynamicRange.SDR
        }

    val newVideoDynamicRange = if (
        hdrUiState is HdrUiState.Available &&
        enum == CameraDynamicRange.SDR
    ) {
        DEFAULT_HDR_DYNAMIC_RANGE
    } else {
        DynamicRange.SDR
    }

    val newImageOutputFormat = if (
        hdrUiState is HdrUiState.Available &&
        enum == CameraDynamicRange.SDR
    ) {
        DEFAULT_HDR_IMAGE_OUTPUT
    } else {
        ImageOutputFormat.JPEG
    }

    QuickSettingToggleButton(
        modifier = modifier,
        enum = enum,
        onClick = {
            onClick(newVideoDynamicRange, newImageOutputFormat)
        },
        isHighLighted = (
            hdrUiState is HdrUiState.Available &&
                (
                    hdrUiState.selectedDynamicRange == DEFAULT_HDR_DYNAMIC_RANGE ||
                        hdrUiState.selectedImageFormat == DEFAULT_HDR_IMAGE_OUTPUT
                    )
            ),
        enabled = hdrUiState is HdrUiState.Available
    )
}

@Composable
fun QuickSetRatio(
    onClick: () -> Unit,
    ratio: AspectRatio,
    aspectRatioUiState: AspectRatioUiState,
    modifier: Modifier = Modifier,
    isHighlightEnabled: Boolean = false
) {
    if (aspectRatioUiState is AspectRatioUiState.Available) {
        val enum =
            when (ratio) {
                AspectRatio.THREE_FOUR -> CameraAspectRatio.THREE_FOUR
                AspectRatio.NINE_SIXTEEN -> CameraAspectRatio.NINE_SIXTEEN
                AspectRatio.ONE_ONE -> CameraAspectRatio.ONE_ONE
                else -> CameraAspectRatio.ONE_ONE
            }
        QuickSettingToggleButton(
            modifier = modifier,
            enum = enum,
            onClick = { onClick() },
            isHighLighted = isHighlightEnabled && (ratio == aspectRatioUiState.selectedAspectRatio)
        )
    }
}

@Composable
fun QuickSetFlash(
    modifier: Modifier = Modifier,
    onClick: (FlashMode) -> Unit,
    flashModeUiState: FlashModeUiState
) {
    when (flashModeUiState) {
        is FlashModeUiState.Unavailable ->
            QuickSettingToggleButton(
                modifier = modifier,
                enum = CameraFlashMode.OFF,
                enabled = false,
                onClick = {}
            )

        is FlashModeUiState.Available ->
            QuickSettingToggleButton(
                modifier = modifier,
                enum = flashModeUiState.selectedFlashMode.toCameraFlashMode(
                    flashModeUiState.isActive
                ),
                isHighLighted = flashModeUiState.selectedFlashMode != FlashMode.OFF,
                onClick = {
                    onClick(flashModeUiState.getNextFlashMode())
                }
            )
    }
}

@Composable
fun QuickFlipCamera(
    setLensFacing: (LensFacing) -> Unit,
    flipLensUiState: FlipLensUiState,
    modifier: Modifier = Modifier
) {
    if (flipLensUiState is FlipLensUiState.Available) {
        val enum =
            when (flipLensUiState.selectedLensFacing) {
                LensFacing.FRONT -> CameraLensFace.FRONT
                LensFacing.BACK -> CameraLensFace.BACK
            }
        QuickSettingToggleButton(
            modifier = modifier,
            enum = enum,
            onClick = { setLensFacing(flipLensUiState.selectedLensFacing.flip()) }
        )
    }
}

@Composable
fun QuickSetStreamConfig(
    setStreamConfig: (StreamConfig) -> Unit,
    streamConfigUiState: StreamConfigUiState,
    modifier: Modifier = Modifier
) {
    if (streamConfigUiState is StreamConfigUiState.Available) {
        val enum: CameraStreamConfig =
            when (streamConfigUiState.selectedStreamConfig) {
                StreamConfig.MULTI_STREAM -> CameraStreamConfig.MULTI_STREAM
                StreamConfig.SINGLE_STREAM -> CameraStreamConfig.SINGLE_STREAM
            }
        QuickSettingToggleButton(
            modifier = modifier,
            enum = enum,
            onClick = {
                when (streamConfigUiState.selectedStreamConfig) {
                    StreamConfig.MULTI_STREAM -> setStreamConfig(StreamConfig.SINGLE_STREAM)
                    StreamConfig.SINGLE_STREAM -> setStreamConfig(StreamConfig.MULTI_STREAM)
                }
            },
            enabled = streamConfigUiState.isActive
        )
    }
}

@Composable
fun QuickSetConcurrentCamera(
    setConcurrentCameraMode: (ConcurrentCameraMode) -> Unit,
    concurrentCameraUiState: ConcurrentCameraUiState,
    modifier: Modifier = Modifier
) {
    if (concurrentCameraUiState is ConcurrentCameraUiState.Available) {
        val enum: CameraConcurrentCameraMode =
            when (concurrentCameraUiState.selectedConcurrentCameraMode) {
                ConcurrentCameraMode.OFF -> CameraConcurrentCameraMode.OFF
                ConcurrentCameraMode.DUAL -> CameraConcurrentCameraMode.DUAL
            }
        QuickSettingToggleButton(
            modifier = modifier,
            enum = enum,
            onClick = {
                when (concurrentCameraUiState.selectedConcurrentCameraMode) {
                    ConcurrentCameraMode.OFF -> setConcurrentCameraMode(ConcurrentCameraMode.DUAL)
                    ConcurrentCameraMode.DUAL -> setConcurrentCameraMode(ConcurrentCameraMode.OFF)
                }
            },
            enabled = concurrentCameraUiState.isEnabled
        )
    }
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
    val rotationAngle by animateFloatAsState(
        targetValue = if (isOpen) -180f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessLow) // Adjust duration as needed
    )
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.rotate(rotationAngle)
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    // removes the greyish background animation that appears when clicking on a clickable
                    indication = null,
                    onClick = toggleDropDown
                )
        )
    }
}

@Composable
fun QuickSettingToggleButton(
    modifier: Modifier = Modifier,
    enum: QuickSettingsEnum,
    onClick: () -> Unit,
    isHighLighted: Boolean = false,
    enabled: Boolean = true
) {
    QuickSettingToggleButton(
        modifier = modifier,
        text = stringResource(id = enum.getTextResId()),
        accessibilityText = stringResource(id = enum.getDescriptionResId()),
        onClick = { onClick() },
        isHighlighted = isHighLighted,
        enabled = enabled,
        painter = enum.getPainter()
    )
}

/**
 *
 * @param isHighlighted true if the button is currently checked; false otherwise.
 * @param onClick will be called when the user clicks the button.
 * @param text The text label to display below the icon.
 * @param painter The icon to display inside the button.
 * @param modifier The Modifier to be applied to the button.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickSettingToggleButton(
    onClick: () -> Unit,
    text: String,
    accessibilityText: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    enabled: Boolean = true
) {
    val buttonSize = IconButtonDefaults.mediumContainerSize(
        IconButtonDefaults.IconButtonWidthOption.Narrow
    )

    Column(
        modifier = Modifier.width(width = buttonSize.width),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        FilledIconToggleButton(
            checked = isHighlighted,
            enabled = enabled,
            onCheckedChange = { _ -> onClick() },
            // 1. Size updated to width 48.dp and height 56.dp
            modifier = modifier
                .minimumInteractiveComponentSize()
                .size(buttonSize),
            shapes = IconButtonDefaults.toggleableShapes(),
            colors = IconButtonDefaults.filledIconToggleButtonColors()
                .copy(containerColor = Color.White.copy(alpha = .17f))
        ) {
            Icon(
                modifier = Modifier.size(IconButtonDefaults.mediumIconSize),
                painter = painter,
                contentDescription = accessibilityText
            )
        }

        Spacer(Modifier.height(4.dp))
        Text(
            modifier = modifier.width(IntrinsicSize.Max),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Visible
        )
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
                        dimensionResource(
                            id = R.dimen.quick_settings_ui_item_icon_size
                        ) +
                            (
                                dimensionResource(
                                    id = R.dimen.quick_settings_ui_item_padding
                                ) *
                                    2
                                )
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
                        dimensionResource(
                            id = R.dimen.quick_settings_ui_item_icon_size
                        ) +
                            (
                                dimensionResource(
                                    id = R.dimen.quick_settings_ui_item_padding
                                ) *
                                    2
                                )
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
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                    enabled = enabled
                )
        )
    }
}

@Composable
fun FlashModeIndicator(
    flashModeUiState: FlashModeUiState,
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
                enum = flashModeUiState.selectedFlashMode.toCameraFlashMode(
                    flashModeUiState.isActive
                ),
                onClick = {
                    onClick(flashModeUiState.getNextFlashMode())
                }
            )
    }
}

@Composable
fun QuickSettingsIndicators(
    modifier: Modifier = Modifier,
    flashModeUiState: FlashModeUiState,
    onFlashModeClick: (flashMode: FlashMode) -> Unit
) {
    Row(modifier = modifier) {
        FlashModeIndicator(
            flashModeUiState,
            onFlashModeClick
        )
    }
}

private fun FlashModeUiState.Available.getNextFlashMode(): FlashMode {
    // Filter out only the selectable flash modes to cycle through them.
    val selectableModes = this.availableFlashModes
        .filterIsInstance<SingleSelectableUiState.SelectableUi<FlashMode>>()
        .map { it.value } // Extract the FlashMode items

    val currentIndex = selectableModes.indexOf(this.selectedFlashMode)
    val nextIndex = (currentIndex + 1) % selectableModes.size

    return selectableModes[nextIndex]
}

private fun FlashMode.toCameraFlashMode(isActive: Boolean) = when (this) {
    FlashMode.OFF -> CameraFlashMode.OFF
    FlashMode.AUTO -> CameraFlashMode.AUTO
    FlashMode.ON -> CameraFlashMode.ON
    FlashMode.LOW_LIGHT_BOOST -> {
        when (isActive) {
            true -> CameraFlashMode.LOW_LIGHT_BOOST_ACTIVE
            false -> CameraFlashMode.LOW_LIGHT_BOOST_INACTIVE
        }
    }
}

@Preview
@Composable
private fun QuickSettingToggleButtonPreview() {
    // A sample state variable to make the preview interactive in live edit

    Box(
        modifier = Modifier
            .background(color = Color.DarkGray)
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier
                .width(300.dp)
                .height(180.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Instance 1: Unchecked state
            QuickSettingToggleButton(
                onClick = {},
                text = "Flash Off",
                accessibilityText = "",
                painter = CameraFlashMode.OFF.getPainter(),
                isHighlighted = false,
                enabled = true
            )

            // Instance 2: Checked state
            QuickSettingToggleButton(
                onClick = {},
                text = "Flash On",
                accessibilityText = "",
                painter = CameraFlashMode.ON.getPainter(),
                isHighlighted = true,
                enabled = true
            )

            // Instance 3: Disabled state
            QuickSettingToggleButton(
                onClick = {},
                text = "Flash Off",
                accessibilityText = "",
                painter = CameraFlashMode.OFF.getPainter(),
                isHighlighted = false,
                enabled = false
            )
        }
    }
}
