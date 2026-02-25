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
import com.google.jetpackcamera.ui.components.capture.quicksettings.controller.QuickSettingsController
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
import com.google.jetpackcamera.ui.uistate.capture.compound.FocusedQuickSetting
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState

/**
 * The UI bottom sheet component for quick settings
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun QuickSettingsBottomSheet(
    modifier: Modifier = Modifier,
    quickSettingsUiState: QuickSettingsUiState,
    onNavigateToSettings: () -> Unit,
    quickSettingsController: QuickSettingsController
) {
    if (quickSettingsUiState is QuickSettingsUiState.Available) {
        val onUnFocus = { quickSettingsController.setFocusedSetting(FocusedQuickSetting.NONE) }

        val displayedQuickSettings: List<@Composable () -> Unit> =
            when (quickSettingsUiState.focusedQuickSetting) {
                FocusedQuickSetting.ASPECT_RATIO -> focusedRatioButtons(
                    onUnFocus = onUnFocus,
                    onSetAspectRatio = quickSettingsController::setAspectRatio,
                    aspectRatioUiState = quickSettingsUiState.aspectRatioUiState
                )

                FocusedQuickSetting.CAPTURE_MODE -> focusedCaptureModeButtons(
                    onUnFocus = onUnFocus,
                    onSetCaptureMode = quickSettingsController::setCaptureMode,
                    captureModeUiState = quickSettingsUiState.captureModeUiState
                )

                FocusedQuickSetting.NONE ->
                    buildList {
                        // todo(kc): change flash to expanded setting?
                        add {
                            QuickSetFlash(
                                modifier = Modifier.testTag(QUICK_SETTINGS_FLASH_BUTTON),
                                onClick = { f: FlashMode -> quickSettingsController.setFlash(f) },
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
                                    .semantics { description?.let { stateDescription = it } },
                                setCaptureMode = {
                                    quickSettingsController.setFocusedSetting(
                                        FocusedQuickSetting.CAPTURE_MODE
                                    )
                                },
                                captureModeUiState = quickSettingsUiState.captureModeUiState
                            )
                        }

                        add {
                            QuickFlipCamera(
                                modifier = Modifier.testTag(QUICK_SETTINGS_FLIP_CAMERA_BUTTON),
                                setLensFacing = { l: LensFacing ->
                                    quickSettingsController.setLensFacing(l)
                                },
                                flipLensUiState = quickSettingsUiState.flipLensUiState
                            )
                        }

                        add {
                            ToggleFocusedQuickSetRatio(
                                modifier = Modifier.testTag(QUICK_SETTINGS_RATIO_BUTTON),
                                setRatio = {
                                    quickSettingsController.setFocusedSetting(
                                        FocusedQuickSetting.ASPECT_RATIO
                                    )
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
                                setStreamConfig = { c: StreamConfig ->
                                    quickSettingsController.setStreamConfig(c)
                                },
                                streamConfigUiState = quickSettingsUiState.streamConfigUiState
                            )
                        }

                        add {
                            QuickSetHdr(
                                modifier = Modifier.testTag(QUICK_SETTINGS_HDR_BUTTON),
                                onClick = { d: DynamicRange, i: ImageOutputFormat ->
                                    quickSettingsController.setDynamicRange(d)
                                    quickSettingsController.setImageFormat(i)
                                },
                                hdrUiState = quickSettingsUiState.hdrUiState
                            )
                        }

                        add {
                            QuickSetConcurrentCamera(
                                modifier =
                                Modifier.testTag(QUICK_SETTINGS_CONCURRENT_CAMERA_MODE_BUTTON),
                                setConcurrentCameraMode = { c: ConcurrentCameraMode ->
                                    quickSettingsController.setConcurrentCameraMode(c)
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
                    onUnFocus()
                    quickSettingsController.toggleQuickSettings()
                },
                sheetState = sheetState,
                *displayedQuickSettings.toTypedArray()
            )
        }
    }
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
            onNavigateToSettings = {},
            quickSettingsController = MockQuickSettingsController()
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
            onNavigateToSettings = { },
            quickSettingsController = MockQuickSettingsController()
        )
    }
}

class MockQuickSettingsController : QuickSettingsController {
    override fun toggleQuickSettings() {}

    override fun setFocusedSetting(focusedQuickSetting: FocusedQuickSetting) {}

    override fun setLensFacing(lensFace: LensFacing) {}

    override fun setFlash(flashMode: FlashMode) {}

    override fun setAspectRatio(aspectRatio: AspectRatio) {}

    override fun setStreamConfig(streamConfig: StreamConfig) {}

    override fun setDynamicRange(dynamicRange: DynamicRange) {}

    override fun setImageFormat(imageOutputFormat: ImageOutputFormat) {}

    override fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {}

    override fun setCaptureMode(captureMode: CaptureMode) {}
}
