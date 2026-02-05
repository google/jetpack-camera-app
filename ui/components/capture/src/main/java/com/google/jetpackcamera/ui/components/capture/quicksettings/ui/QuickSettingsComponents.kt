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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
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
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_IMAGE_ONLY
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_OPTION_STANDARD
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_VIDEO_ONLY
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_CLOSE_EXPANDED_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_DROP_DOWN
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_1_1_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_3_4_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_9_16_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_SCROLL_CONTAINER
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
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
fun QuickSetRatio(
    onClick: () -> Unit,
    assignedRatio: AspectRatio,
    aspectRatioUiState: AspectRatioUiState,
    modifier: Modifier = Modifier,
    isHighlightEnabled: Boolean = false
) {
    if (aspectRatioUiState is AspectRatioUiState.Available) {
        val enum =
            when (assignedRatio) {
                AspectRatio.THREE_FOUR -> CameraAspectRatio.THREE_FOUR
                AspectRatio.NINE_SIXTEEN -> CameraAspectRatio.NINE_SIXTEEN
                AspectRatio.ONE_ONE -> CameraAspectRatio.ONE_ONE
                else -> CameraAspectRatio.ONE_ONE
            }
        QuickSettingToggleButton(
            modifier = modifier,
            enum = enum,
            onClick = { onClick() },
            isHighLighted = isHighlightEnabled &&
                (assignedRatio == aspectRatioUiState.selectedAspectRatio)
        )
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
                            .testTag(BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_IMAGE_ONLY),
                        onClick = { onSetCaptureMode(CaptureMode.IMAGE_ONLY) },
                        assignedCaptureMode = CaptureMode.IMAGE_ONLY,
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

/**
 * A button in the quick settings menu that will navigate to the default settings screen
 */
@Composable
fun QuickNavSettings(onNavigateToSettings: () -> Unit, modifier: Modifier = Modifier) {
    QuickSettingToggleButton(
        onClick = onNavigateToSettings,
        text = stringResource(R.string.quick_settings_more_text),
        accessibilityText = stringResource(R.string.quick_settings_more_description),
        painter = rememberVectorPainter(Icons.Filled.MoreHoriz),
        modifier = modifier.testTag(SETTINGS_BUTTON)
    )
}

@Composable
fun ToggleFocusedQuickSetCaptureMode(
    setCaptureMode: (captureMode: CaptureMode) -> Unit,
    captureModeUiState: CaptureModeUiState,
    modifier: Modifier = Modifier,
    isHighlightEnabled: Boolean = false
) {
    if (captureModeUiState is CaptureModeUiState.Available) {
        val enum =
            when (captureModeUiState.selectedCaptureMode) {
                CaptureMode.STANDARD -> CameraCaptureMode.STANDARD
                CaptureMode.VIDEO_ONLY -> CameraCaptureMode.VIDEO_ONLY
                CaptureMode.IMAGE_ONLY -> CameraCaptureMode.IMAGE_ONLY
            }

        QuickSettingToggleButton(
            modifier = modifier,
            enum = enum,
            isHighLighted = isHighlightEnabled,
            enabled = captureModeUiState.availableCaptureModes.count {
                it is SingleSelectableUiState.SelectableUi
            } >= 2,
            onClick = {
                setCaptureMode(
                    captureModeUiState.availableCaptureModes.getNextSelectableItem(
                        captureModeUiState.selectedCaptureMode
                    )
                )
            }

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
fun ToggleFocusedQuickSetRatio(
    setRatio: (aspectRatio: AspectRatio) -> Unit,
    aspectRatioUiState: AspectRatioUiState,
    modifier: Modifier = Modifier,
    isHighlightEnabled: Boolean = false
) {
    if (aspectRatioUiState is AspectRatioUiState.Available) {
        val enum =
            when (aspectRatioUiState.selectedAspectRatio) {
                AspectRatio.THREE_FOUR -> CameraAspectRatio.THREE_FOUR
                AspectRatio.NINE_SIXTEEN -> CameraAspectRatio.NINE_SIXTEEN
                AspectRatio.ONE_ONE -> CameraAspectRatio.ONE_ONE
            }
        QuickSettingToggleButton(
            modifier = modifier,
            enum = enum,
            isHighLighted = isHighlightEnabled,
            onClick = {
                setRatio(
                    aspectRatioUiState.availableAspectRatios.getNextSelectableItem(
                        aspectRatioUiState.selectedAspectRatio
                    )
                )
            }
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
                    flashModeUiState.isLowLightBoostActive
                ),
                isHighLighted = flashModeUiState.selectedFlashMode != FlashMode.OFF,
                onClick = {
                    onClick(
                        flashModeUiState.availableFlashModes.getNextSelectableItem(
                            flashModeUiState.selectedFlashMode
                        )
                    )
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
            isHighLighted = concurrentCameraUiState.selectedConcurrentCameraMode ==
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
    toggleBottomSheet: () -> Unit,
    isOpen: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
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
                contentDescription = if (isOpen) {
                    openDescription
                } else {
                    closedDescription
                }
            },
        enabled = enabled,
        onClick = toggleBottomSheet,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsBottomSheet(
    modifier: Modifier,
    onDismiss: () -> Unit,
    sheetState: SheetState,
    vararg quickSettingButtons: @Composable () -> Unit
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
        QuickSettingsBottomSheetRow(
            modifier = Modifier,
            quickSettingButtons = quickSettingButtons
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun focusedRatioButtons(
    onUnFocus: () -> Unit,
    onSetAspectRatio: (AspectRatio) -> Unit,
    aspectRatioUiState: AspectRatioUiState
): List<@Composable () -> Unit> = listOf(
    {
        CloseExpandedSettingsButton(onUnFocus)
    },
    {
        QuickSetRatio(
            modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_3_4_BUTTON),
            onClick = { onSetAspectRatio(AspectRatio.THREE_FOUR) },
            assignedRatio = AspectRatio.THREE_FOUR,
            aspectRatioUiState = aspectRatioUiState,
            isHighlightEnabled = true
        )
    },
    {
        QuickSetRatio(
            modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_9_16_BUTTON),
            onClick = { onSetAspectRatio(AspectRatio.NINE_SIXTEEN) },
            assignedRatio = AspectRatio.NINE_SIXTEEN,
            aspectRatioUiState = aspectRatioUiState,
            isHighlightEnabled = true
        )
    },
    {
        QuickSetRatio(
            modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_1_1_BUTTON),
            onClick = { onSetAspectRatio(AspectRatio.ONE_ONE) },
            assignedRatio = AspectRatio.ONE_ONE,
            aspectRatioUiState = aspectRatioUiState,
            isHighlightEnabled = true
        )
    }
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun focusedCaptureModeButtons(
    onUnFocus: () -> Unit,
    onSetCaptureMode: (CaptureMode) -> Unit,
    captureModeUiState: CaptureModeUiState
): List<@Composable () -> Unit> = listOf(
    {
        CloseExpandedSettingsButton(onUnFocus)
    },
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
                .testTag(BTN_QUICK_SETTINGS_FOCUSED_CAPTURE_MODE_IMAGE_ONLY),
            onClick = { onSetCaptureMode(CaptureMode.IMAGE_ONLY) },
            assignedCaptureMode = CaptureMode.IMAGE_ONLY,
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
    }
)

@Composable
private fun CloseExpandedSettingsButton(onUnFocus: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(
        modifier = modifier.testTag(QUICK_SETTINGS_CLOSE_EXPANDED_BUTTON),
        onClick = onUnFocus
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = stringResource(
                R.string.quick_settings_btn_close_expanded_settings_description
            )
        )
    }
}

/**
 * A horizontally scrollable row of quick setting buttons for a bottom sheet.
 * This row will only enable scrolling if its content overflows the screen width.
 *
 * @param quickSettingButtons A list of [QuickSettingCarouselButton].
 * @param modifier The Modifier to be applied to this composable.
 */
@Composable
private fun QuickSettingsBottomSheetRow(
    modifier: Modifier = Modifier,
    vararg quickSettingButtons: @Composable () -> Unit
) {
    // LazyRow is inherently scrollable if content exceeds bounds.
    // It handles the "overflow to the right" behavior by default.
    LazyRow(
        modifier = modifier
            .testTag(QUICK_SETTINGS_SCROLL_CONTAINER)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(quickSettingButtons.toList()) { index, quickSetting ->
            quickSetting()
        }
    }
}

/**
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
            modifier = modifier
                .minimumInteractiveComponentSize()
                .size(buttonSize),
            checked = isHighlighted,
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
fun TopBarQuickSettingIcon(
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

private fun <T> List<SingleSelectableUiState<T>>.getNextSelectableItem(selectedItem: T): T {
    // Filter out only the selectable modes to cycle through them.
    val selectableModes = this
        .filterIsInstance<SingleSelectableUiState.SelectableUi<T>>()
        .map { it.value } // 'this' is already the list

    val currentIndex = selectableModes.indexOf(selectedItem) // selectedItem is passed directly
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
                .width(200.dp),
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
