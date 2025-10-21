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
package com.google.jetpackcamera.ui.components.capture.quicksettings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.components.capture.BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_FLASH_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_FLIP_CAMERA_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_HDR_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_RATIO_BUTTON
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_STREAM_CONFIG_BUTTON
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.SETTINGS_BUTTON
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickFlipCamera
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickNavSettings
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSetConcurrentCamera
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSetFlash
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSetHdr
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSetStreamConfig
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSettingsBottomSheet as BottomSheetComponent
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleFocusedQuickSetCaptureMode
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.ToggleFocusedQuickSetRatio
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.focusedCaptureModeButtons
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.focusedRatioButtons
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState

/**
 * The UI bottom sheet component for quick settings
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuickSettingsBottomSheet(
    modifier: Modifier = Modifier,
    quickSettingsUiState: QuickSettingsUiState,
    toggleQuickSettings: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onLensFaceClick: (lensFace: LensFacing) -> Unit,
    onFlashModeClick: (flashMode: FlashMode) -> Unit,
    onAspectRatioClick: (aspectRation: AspectRatio) -> Unit,
    onStreamConfigClick: (streamConfig: StreamConfig) -> Unit,
    onDynamicRangeClick: (dynamicRange: DynamicRange) -> Unit,
    onImageOutputFormatClick: (imageOutputFormat: ImageOutputFormat) -> Unit,
    onConcurrentCameraModeClick: (concurrentCameraMode: ConcurrentCameraMode) -> Unit,
    onCaptureModeClick: (CaptureMode) -> Unit
) {
    if (quickSettingsUiState is QuickSettingsUiState.Available) {
        var focusedQuickSetting by remember {
            mutableStateOf(FocusedQuickSetting.NONE)
        }
        val onUnFocus = { focusedQuickSetting = FocusedQuickSetting.NONE }

        val displayedQuickSettings: List<@Composable () -> Unit> =
            when (focusedQuickSetting) {
                FocusedQuickSetting.ASPECT_RATIO -> focusedRatioButtons(
                    onUnFocus = onUnFocus,
                    onSetAspectRatio = onAspectRatioClick,
                    aspectRatioUiState = quickSettingsUiState.aspectRatioUiState
                )

                FocusedQuickSetting.CAPTURE_MODE -> focusedCaptureModeButtons(
                    onUnFocus = onUnFocus,
                    onSetCaptureMode = onCaptureModeClick,
                    captureModeUiState = quickSettingsUiState.captureModeUiState
                )

                FocusedQuickSetting.NONE ->
                    buildList {
                        // todo(kc): change flash to expanded setting?
                        add {
                            QuickSetFlash(
                                modifier = Modifier.testTag(QUICK_SETTINGS_FLASH_BUTTON),
                                onClick = { f: FlashMode -> onFlashModeClick(f) },
                                flashModeUiState = quickSettingsUiState.flashModeUiState
                            )
                        }

                        add {
                            val description =
                                quickSettingsUiState.captureModeUiState.stateDescription()
                                    ?.let {
                                        stringResource(it)
                                    }
                            ToggleFocusedQuickSetCaptureMode(
                                modifier = Modifier
                                    .testTag(BTN_QUICK_SETTINGS_FOCUS_CAPTURE_MODE)
                                    .apply {
                                        if (description != null) {
                                            semantics { stateDescription = description }
                                        }
                                    },
                                setCaptureMode = {
                                    focusedQuickSetting = FocusedQuickSetting.CAPTURE_MODE
                                },
                                captureModeUiState = quickSettingsUiState.captureModeUiState
                            )
                        }

                        add {
                            QuickFlipCamera(
                                modifier = Modifier.testTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON),
                                setLensFacing = { l: LensFacing -> onLensFaceClick(l) },
                                flipLensUiState = quickSettingsUiState.flipLensUiState
                            )
                        }

                        add {
                            ToggleFocusedQuickSetRatio(
                                modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_BUTTON),
                                setRatio = {
                                    focusedQuickSetting = FocusedQuickSetting.ASPECT_RATIO
                                },
                                isHighlightEnabled = false,
                                aspectRatioUiState = quickSettingsUiState.aspectRatioUiState
                            )
                        }

                        add {
                            QuickSetStreamConfig(
                                modifier = Modifier.testTag(
                                    QUICK_SETTINGS_STREAM_CONFIG_BUTTON
                                ),
                                setStreamConfig = { c: StreamConfig -> onStreamConfigClick(c) },
                                streamConfigUiState = quickSettingsUiState.streamConfigUiState
                            )
                        }

                        add {
                            QuickSetHdr(
                                modifier = Modifier.testTag(QUICK_SETTINGS_HDR_BUTTON),
                                onClick = { d: DynamicRange, i: ImageOutputFormat ->
                                    onDynamicRangeClick(d)
                                    onImageOutputFormatClick(i)
                                },
                                hdrUiState = quickSettingsUiState.hdrUiState
                            )
                        }

                        add {
                            QuickSetConcurrentCamera(
                                modifier =
                                    Modifier.testTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON),
                                setConcurrentCameraMode = { c: ConcurrentCameraMode ->
                                    onConcurrentCameraModeClick(c)
                                },
                                concurrentCameraUiState = quickSettingsUiState
                                    .concurrentCameraUiState
                            )
                        }

                        add {
                            QuickNavSettings(
                                modifier = Modifier
                                    .testTag(SETTINGS_BUTTON),
                                onNavigateToSettings = onNavigateToSettings
                            )
                        }
                    }
            }

        val sheetState = rememberModalBottomSheetState()

        if (quickSettingsUiState.quickSettingsIsOpen) {
            BottomSheetComponent(
                modifier = modifier,
                onDismiss = {
                    focusedQuickSetting = FocusedQuickSetting.NONE
                    toggleQuickSettings()
                },
                sheetState = sheetState,
                *displayedQuickSettings.toTypedArray()
            )
        }
    }
}

// enum representing which individual quick setting is currently focused
private enum class FocusedQuickSetting {
    NONE,
    ASPECT_RATIO,
    CAPTURE_MODE
}

private fun CaptureModeUiState.stateDescription() = (this as? CaptureModeUiState.Available)?.let {
    when (selectedCaptureMode) {
        CaptureMode.STANDARD -> R.string.quick_settings_description_capture_mode_standard
        CaptureMode.VIDEO_ONLY -> R.string.quick_settings_description_capture_mode_video_only
        CaptureMode.IMAGE_ONLY -> R.string.quick_settings_description_capture_mode_image_only
    }
}

@Preview
@Composable
fun ExpandedQuickSettingsUiPreview() {
    MaterialTheme {
        QuickSettingsBottomSheet(
            quickSettingsUiState = QuickSettingsUiState.Available(
                aspectRatioUiState = AspectRatioUiState.Available(
                    selectedAspectRatio = AspectRatio.NINE_SIXTEEN,
                    availableAspectRatios = listOf(
                        SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
                        SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                        SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
                    )
                ),
                captureModeUiState = CaptureModeUiState.Available(
                    selectedCaptureMode = CaptureMode.STANDARD,
                    availableCaptureModes = listOf(
                        SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
                        SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                        SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
                    )
                ),
                concurrentCameraUiState = ConcurrentCameraUiState.Available(
                    selectedConcurrentCameraMode = ConcurrentCameraMode.OFF,
                    isEnabled = false
                ),
                flashModeUiState = FlashModeUiState.Available(
                    selectedFlashMode = FlashMode.OFF,
                    availableFlashModes = listOf(
                        SingleSelectableUiState.SelectableUi(FlashMode.OFF),
                        SingleSelectableUiState.SelectableUi(FlashMode.ON),
                        SingleSelectableUiState.SelectableUi(FlashMode.AUTO)
                    ),
                    isLowLightBoostActive = false
                ),
                flipLensUiState = FlipLensUiState.Available(
                    selectedLensFacing = LensFacing.BACK,
                    availableLensFacings = listOf(
                        SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                        SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
                    )
                ),
                hdrUiState = HdrUiState.Unavailable,
                streamConfigUiState = StreamConfigUiState.Available(
                    selectedStreamConfig = StreamConfig.MULTI_STREAM,
                    availableStreamConfigs = listOf(
                        SingleSelectableUiState.SelectableUi(StreamConfig.SINGLE_STREAM),
                        SingleSelectableUiState.SelectableUi(StreamConfig.MULTI_STREAM)
                    ),
                    isActive = false
                ),
                quickSettingsIsOpen = true
            ),
            onLensFaceClick = { },
            onFlashModeClick = { },
            onAspectRatioClick = { },
            onStreamConfigClick = { },
            onDynamicRangeClick = { },
            onImageOutputFormatClick = { },
            onConcurrentCameraModeClick = { },
            toggleQuickSettings = { },
            onNavigateToSettings = { },
            onCaptureModeClick = { },
        )
    }
}

@Preview
@Composable
fun ExpandedQuickSettingsUiPreview_WithHdr() {
    MaterialTheme {
        QuickSettingsBottomSheet(
            quickSettingsUiState = QuickSettingsUiState.Available(
                aspectRatioUiState = AspectRatioUiState.Available(
                    selectedAspectRatio = AspectRatio.NINE_SIXTEEN,
                    availableAspectRatios = listOf(
                        SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
                        SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                        SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
                    )
                ),
                captureModeUiState = CaptureModeUiState.Available(
                    selectedCaptureMode = CaptureMode.STANDARD,
                    availableCaptureModes = listOf(
                        SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
                        SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
                        SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
                    )
                ),
                concurrentCameraUiState = ConcurrentCameraUiState.Available(
                    selectedConcurrentCameraMode = ConcurrentCameraMode.OFF,
                    isEnabled = false
                ),
                flashModeUiState = FlashModeUiState.Available(
                    selectedFlashMode = FlashMode.OFF,
                    availableFlashModes = listOf(
                        SingleSelectableUiState.SelectableUi(FlashMode.OFF),
                        SingleSelectableUiState.SelectableUi(FlashMode.ON),
                        SingleSelectableUiState.SelectableUi(FlashMode.AUTO)
                    ),
                    isLowLightBoostActive = false
                ),
                flipLensUiState = FlipLensUiState.Available(
                    selectedLensFacing = LensFacing.BACK,
                    availableLensFacings = listOf(
                        SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                        SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
                    )
                ),
                hdrUiState = HdrUiState.Available(
                    selectedDynamicRange = DynamicRange.HLG10,
                    selectedImageFormat = ImageOutputFormat.JPEG_ULTRA_HDR
                ),
                streamConfigUiState = StreamConfigUiState.Available(
                    selectedStreamConfig = StreamConfig.MULTI_STREAM,
                    availableStreamConfigs = listOf(
                        SingleSelectableUiState.SelectableUi(StreamConfig.SINGLE_STREAM),
                        SingleSelectableUiState.SelectableUi(StreamConfig.MULTI_STREAM)
                    ),
                    isActive = false
                ),
                quickSettingsIsOpen = true
            ),
            onLensFaceClick = { },
            onFlashModeClick = { },
            onAspectRatioClick = { },
            onStreamConfigClick = { },
            onDynamicRangeClick = { },
            onImageOutputFormatClick = { },
            onConcurrentCameraModeClick = { },
            toggleQuickSettings = { },
            onNavigateToSettings = { },
            onCaptureModeClick = { },
        )
    }
}
