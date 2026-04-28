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

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.testTagsAsResourceId
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
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraAspectRatio
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraCaptureMode
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraConcurrentCameraMode
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraDynamicRange
import com.google.jetpackcamera.ui.components.capture.quicksettings.CameraFlashMode
import com.google.jetpackcamera.ui.components.capture.quicksettings.QuickSettingsEnum
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState.Unavailable.isCaptureModeSelectable
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState

@Composable
fun CaptureModeRow(
    modifier: Modifier = Modifier,
    onSetCaptureMode: (CaptureMode) -> Unit,
    captureModeUiState: CaptureModeUiState,
) {
    if (captureModeUiState is CaptureModeUiState.Available) {
        SettingRow(
            modifier = modifier,
            title = "Capture Mode",
            stateString = captureModeUiState.selectedCaptureMode.toString(),
            settingsButtons = createCaptureModeButtons(captureModeUiState, onSetCaptureMode)
        )
    }
}

@Composable
private fun createCaptureModeButtons(
    captureModeUiState: CaptureModeUiState.Available,
    onSetCaptureMode: (CaptureMode) -> Unit
): Array<@Composable () -> Unit> {
    return captureModeUiState.availableCaptureModes
        .map { selectableMode ->
            @Composable {
                CaptureModeToggleButton(
                    modifier = Modifier
                        .testTag("CaptureMode_${selectableMode.value.name}"),
                    onClick = { onSetCaptureMode(selectableMode.value) },
                    assignedCaptureMode = selectableMode.value,
                    captureModeUiState = captureModeUiState,
                    isHighlightEnabled = true
                )
            }
        }.toTypedArray()
}

@Composable
fun CaptureModeToggleButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    captureModeUiState: CaptureModeUiState.Available,
    assignedCaptureMode: CaptureMode,
    isHighlightEnabled: Boolean = false
) {
    val enum = when (assignedCaptureMode) {
        CaptureMode.STANDARD -> CameraCaptureMode.STANDARD
        CaptureMode.VIDEO_ONLY -> CameraCaptureMode.VIDEO_ONLY
        CaptureMode.IMAGE_ONLY -> CameraCaptureMode.IMAGE_ONLY
    }

    QuickSettingToggleButton(
        modifier = modifier,
        enum = enum,
        onClick = { onClick() },
        enabled = when (assignedCaptureMode) {

            CaptureMode.STANDARD ->
                captureModeUiState.isCaptureModeSelectable(CaptureMode.STANDARD)

            CaptureMode.VIDEO_ONLY ->
                captureModeUiState.isCaptureModeSelectable(CaptureMode.VIDEO_ONLY)

            CaptureMode.IMAGE_ONLY ->
                captureModeUiState.isCaptureModeSelectable(CaptureMode.IMAGE_ONLY)
        },
        isSelected =
            isHighlightEnabled && (assignedCaptureMode == captureModeUiState.selectedCaptureMode)
    )
}

/**
 * A button in the quick settings menu that will navigate to the default settings screen
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun QuickNavSettings(onNavigateToSettings: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        modifier = modifier,
        onClick = onNavigateToSettings,
        content = { Text(text = stringResource(R.string.quick_settings_more_text)) }
    )
}


@Composable
fun QuickSetHdr(
    modifier: Modifier = Modifier,
    onClick: (DynamicRange, ImageOutputFormat) -> Unit,
    hdrUiState: HdrUiState
) {
    val isHdrOn = hdrUiState is HdrUiState.Available &&
        (
            hdrUiState.selectedDynamicRange == DEFAULT_HDR_DYNAMIC_RANGE ||
                hdrUiState.selectedImageFormat == DEFAULT_HDR_IMAGE_OUTPUT
            )

    SettingRow(
        modifier = modifier,
        title = "HDR",
        stateString = if (isHdrOn) {
            stringResource(R.string.quick_settings_dynamic_range_hdr)
        } else {
            stringResource(R.string.quick_settings_dynamic_range_sdr)
        },
        settingsButtons = arrayOf(
            {
                QuickSettingToggleButton(
                    enum = CameraDynamicRange.HDR,
                    onClick = { onClick(DEFAULT_HDR_DYNAMIC_RANGE, DEFAULT_HDR_IMAGE_OUTPUT) },
                    isSelected = isHdrOn,
                    enabled = hdrUiState is HdrUiState.Available
                )
            },
            {
                QuickSettingToggleButton(
                    enum = CameraDynamicRange.SDR,
                    onClick = { onClick(DynamicRange.SDR, ImageOutputFormat.JPEG) },
                    isSelected = !isHdrOn,
                    enabled = hdrUiState is HdrUiState.Available
                )
            }
        )
    )
}


@Composable
fun AspectRatioRow(
    modifier: Modifier = Modifier,
    onSetAspectRatio: (AspectRatio) -> Unit,
    aspectRatioUiState: AspectRatioUiState,
) {
    if (aspectRatioUiState is AspectRatioUiState.Available) {

        val settingsButtons = aspectRatioUiState.availableAspectRatios
            .map { selectableRatio ->
                @Composable {
                    val enum = when (selectableRatio.value) {
                        AspectRatio.THREE_FOUR -> CameraAspectRatio.THREE_FOUR
                        AspectRatio.NINE_SIXTEEN -> CameraAspectRatio.NINE_SIXTEEN
                        AspectRatio.ONE_ONE -> CameraAspectRatio.ONE_ONE
                        else -> CameraAspectRatio.ONE_ONE
                    }
                    QuickSettingToggleButton(
                        modifier = Modifier.testTag("AspectRatio_${selectableRatio.value.name}"),
                        onClick = { onSetAspectRatio(selectableRatio.value) },
                        enum = enum,
                        isSelected = selectableRatio.value == aspectRatioUiState.selectedAspectRatio
                    )
                }
            }.toTypedArray()

        SettingRow(
            modifier = modifier,
            title = "Aspect Ratio",
            stateString = aspectRatioUiState.selectedAspectRatio.toString(),
            settingsButtons = settingsButtons
        )
    }
}


@Composable
fun FlashRow(
    modifier: Modifier = Modifier,
    onSetFlashMode: (FlashMode) -> Unit,
    flashModeUiState: FlashModeUiState,
) {
    if (flashModeUiState is FlashModeUiState.Available) {

        SettingRow(
            modifier = modifier,
            title = "Capture Mode",
            stateString = flashModeUiState.selectedFlashMode.toString(),
            settingsButtons = flashModeUiState.availableFlashModes
                .map { selectableMode ->
                    @Composable {
                        QuickSetFlash(
                            modifier = Modifier.testTag("FlashMode_${selectableMode.value.name}"),
                            onClick = { onSetFlashMode(selectableMode.value) },
                            assignedFlashMode = selectableMode.value,
                            flashModeUiState = flashModeUiState,
                        )
                    }
                }.toTypedArray()
        )
    }
}


@Composable
fun QuickSetFlash(
    modifier: Modifier = Modifier,
    onClick: (FlashMode) -> Unit,
    assignedFlashMode: FlashMode,
    flashModeUiState: FlashModeUiState.Available
) {
    val enum = when (assignedFlashMode) {
        FlashMode.OFF -> CameraFlashMode.OFF
        FlashMode.ON -> CameraFlashMode.ON
        FlashMode.AUTO -> CameraFlashMode.AUTO
        FlashMode.LOW_LIGHT_BOOST -> when (flashModeUiState.isLowLightBoostActive) {
            true -> CameraFlashMode.LOW_LIGHT_BOOST_ACTIVE
            false -> CameraFlashMode.LOW_LIGHT_BOOST_INACTIVE
        }
    }
    QuickSettingToggleButton(
        modifier = modifier,
        enum = enum,
        isSelected = flashModeUiState.selectedFlashMode == assignedFlashMode,
        onClick = {
            onClick(
                assignedFlashMode
            )
        }
    )
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
            isSelected = concurrentCameraUiState.selectedConcurrentCameraMode ==
                    ConcurrentCameraMode.DUAL,
            enabled = concurrentCameraUiState.isEnabled
        )
    }
}

/**
 * Button to toggle quick settings
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ToggleQuickSettingsButton(
    isOpen: Boolean,
    modifier: Modifier = Modifier,
    quickSettingsController: QuickSettingsController
) {
    val buttonSize = IconButtonDefaults.mediumContainerSize(
        IconButtonDefaults.IconButtonWidthOption.Narrow
    )
    val openDescription = stringResource(R.string.quick_settings_toggle_open_description)
    val closedDescription = stringResource(R.string.quick_settings_toggle_closed_description)
    IconButton(
        modifier = modifier
            .size(buttonSize)
            .testTag(QUICK_SETTINGS_DROP_DOWN)
            .semantics {
                testTag = QUICK_SETTINGS_DROP_DOWN
                contentDescription = if (isOpen) {
                    openDescription
                } else {
                    closedDescription
                }
            },
        onClick = quickSettingsController::toggleQuickSettings,
        colors = IconButtonDefaults.iconButtonColors(
            // Set the background color of the button
            containerColor = Color.White.copy(alpha = 0.08f),
            // Set the color of the icon inside the button
            contentColor = Color.White
        )
    ) {
        Icon(
            painter = painterResource(R.drawable.settings_photo_camera_icon),
            contentDescription = stringResource(R.string.quick_settings_toggle_icon_description)
        )
    }
}

// ////////////////////////////////////////////////////
//
// subcomponents used to build completed components
//
// ////////////////////////////////////////////////////

/**
 * A modal bottom sheet composable used to display a collection of quick setting buttons.
 *
 * @param modifier The [Modifier] to be applied to this composable.
 * @param onDismiss The lambda function to be invoked when the bottom sheet is dismissed.
 * @param sheetState The [SheetState] controlling the visibility and behavior of the bottom sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsBottomSheet(
    modifier: Modifier,
    onDismiss: () -> Unit,
    sheetState: SheetState,
     content: @Composable () -> Unit
) {
    val openDescription = stringResource(R.string.quick_settings_toggle_open_description)

    ModalBottomSheet(
        modifier = modifier
            .semantics {
                // since Modal Bottom Sheet is placed above ALL other composables in the hierarchy,
                // it doesn't inherit the "testTagsAsResourceId" property.
                testTagsAsResourceId = true
                testTag = QUICK_SETTINGS_BOTTOM_SHEET
                contentDescription = openDescription
            },

        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
       content()
    }
}

@Composable
fun SettingRow(
    title: String,
    stateString: String,
    modifier: Modifier = Modifier,
    // Using vararg to accept multiple button components
    vararg settingsButtons: @Composable () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stateString,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            settingsButtons.forEach { button ->
                button()
            }
        }
    }
}

@Composable
private fun QuickSettingToggleButton(
    modifier: Modifier = Modifier,
    enum: QuickSettingsEnum,
    onClick: () -> Unit,
    isSelected: Boolean = false,
    enabled: Boolean = true
) {
    QuickSettingToggleButton(
        modifier = modifier,
        text = stringResource(id = enum.getTextResId()),
        accessibilityText = stringResource(id = enum.getDescriptionResId()),
        onClick = { onClick() },
        isSelected = isSelected,
        enabled = enabled,
        painter = enum.getPainter()
    )
}

/**
 * A customizable toggle button used within the quick settings menu. This button displays an icon
 * and a text label, and can be highlighted to indicate a selected state. It serves as a generic
 * component for various quick settings options.
 *
 * @param onClick The lambda function to be invoked when the button is clicked.
 * @param text The text label displayed below the icon.
 * @param accessibilityText The content description for accessibility purposes.
 * @param painter The [Painter] for the icon displayed inside the button.
 * @param modifier The [Modifier] to be applied to this composable.
 * @param isSelected A boolean indicating whether the button is currently in a highlighted (selected) state.
 * @param enabled A boolean indicating whether the button is interactive.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun QuickSettingToggleButton(
    onClick: () -> Unit,
    text: String,
    accessibilityText: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
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
            modifier = modifier
                .minimumInteractiveComponentSize()
                .size(buttonSize),
            checked = isSelected,
            enabled = enabled,
            onCheckedChange = { _ -> onClick() },
            // 1. Size updated to width 48.dp and height 56.dp

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
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .wrapContentWidth(),
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}


@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HdrIndicator(hdrUiState: HdrUiState, modifier: Modifier = Modifier) {
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
    Icon(
        modifier = modifier.size(IconButtonDefaults.smallIconSize),
        painter = enum.getPainter(),
        contentDescription = stringResource(enum.getDescriptionResId())
    )
}

@Composable
fun FlashModeIndicator(flashModeUiState: FlashModeUiState, modifier: Modifier = Modifier) {
    when (flashModeUiState) {
        is FlashModeUiState.Unavailable ->
            TopBarQuickSettingIcon(
                modifier = modifier,
                enum = CameraFlashMode.OFF,
                enabled = false
            )

        is FlashModeUiState.Available ->
            TopBarQuickSettingIcon(
                modifier = modifier,
                enum = flashModeUiState.selectedFlashMode.toCameraFlashMode(
                    flashModeUiState.isLowLightBoostActive
                )
            )
    }
}

/**
 * The top bar indicators for quick settings items.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun TopBarQuickSettingIcon(
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
                .size(IconButtonDefaults.smallIconSize)
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClick = onClick,
                    enabled = enabled
                )
        )
    }
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
                isSelected = false,
                enabled = true
            )

            // Instance 2: Checked state
            QuickSettingToggleButton(
                onClick = {},
                text = "Flash On",
                accessibilityText = "",
                painter = CameraFlashMode.ON.getPainter(),
                isSelected = true,
                enabled = true
            )

            // Instance 3: Disabled state
            QuickSettingToggleButton(
                onClick = {},
                text = "Flash Off",
                accessibilityText = "",
                painter = CameraFlashMode.OFF.getPainter(),
                isSelected = false,
                enabled = false
            )
        }
    }
}

@Preview(name = "JCA Setting Row - Dark Mode", showBackground = true, backgroundColor = 0xFF000000)
@Composable
private fun PreviewSettingRowDark() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Black // Consistent with Camera UI
        ) {
            SettingRow(
                title = "Video Resolution",
                stateString = "Standard Definition",
                settingsButtons = arrayOf(
                    {
                        // Off State (Highlighted per your screenshot)
                        QuickSettingToggleButton(
                            text = "SD",
                            accessibilityText = "Flash Off",
                            painter = painterResource(id = R.drawable.video_resolution_sd_icon),
                            isSelected = true,
                            onClick = {}
                        )
                    },
                    {
                        // On State
                        QuickSettingToggleButton(
                            text = "HD",
                            accessibilityText = "High Definition",
                            painter = painterResource(id = R.drawable.video_resolution_hd_icon),
                            isSelected = false,
                            onClick = {}
                        )
                    },
                    {
                        // Auto State
                        QuickSettingToggleButton(
                            text = "FHD",
                            accessibilityText = "Full High Definition",
                            painter = painterResource(id = R.drawable.video_resolution_fhd_icon),
                            isSelected = false,
                            onClick = {}
                        )
                    }
                )
            )
        }
    }
}
