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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ConcurrentCameraMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.AspectRatioRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.CaptureModeRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSetHdr
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.ConcurrentCameraUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.StreamConfigUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSettingsBottomSheet as BottomSheetComponent

/**
 * The UI bottom sheet component for quick settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsBottomSheet(
    quickSettingsUiState: QuickSettingsUiState,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit,
    quickSettingsController: QuickSettingsController
) {
    if (quickSettingsUiState is QuickSettingsUiState.Available &&
        quickSettingsUiState.quickSettingsIsOpen
    ) {
        val sheetState = rememberModalBottomSheetState()
        BottomSheetComponent(
            modifier = modifier,
            onDismiss = quickSettingsController::toggleQuickSettings,
            sheetState = sheetState
        ) {
            (quickSettingsUiState.captureModeUiState as? CaptureModeUiState.Available)?.let {
                when (
                    it.selectedCaptureMode
                ) {
                    CaptureMode.VIDEO_ONLY -> VideoQuickSettings(
                        quickSettingsUiState = quickSettingsUiState,
                        quickSettingsController = quickSettingsController
                    )

                    CaptureMode.IMAGE_ONLY -> ImageQuickSettings(
                        quickSettingsUiState = quickSettingsUiState,
                        quickSettingsController = quickSettingsController
                    )

                    CaptureMode.STANDARD -> HybridQuickSettings(
                        quickSettingsUiState = quickSettingsUiState,
                        quickSettingsController = quickSettingsController
                    )
                }
            } ?: ImageQuickSettings(
                quickSettingsUiState = quickSettingsUiState,
                quickSettingsController = quickSettingsController
            )
        }
    }
}

@Composable
fun HybridQuickSettings(
    quickSettingsUiState: QuickSettingsUiState.Available,
    quickSettingsController: QuickSettingsController
) {
    Column {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(id = R.string.quick_settings_title_photo_and_video_settings),
            style = MaterialTheme.typography.titleLarge
        )

        // Flash Mode settings
        if (quickSettingsUiState.flashModeUiState is FlashModeUiState.Available) {
            FlashRow(
                onSetFlashMode = quickSettingsController::setFlash,
                flashModeUiState = quickSettingsUiState.flashModeUiState
            )
        }

        // Capture Mode settings
        if (quickSettingsUiState.captureModeUiState is CaptureModeUiState.Available) {
            CaptureModeRow(
                onSetCaptureMode = quickSettingsController::setCaptureMode,
                captureModeUiState = quickSettingsUiState.captureModeUiState
            )
        }

        // Aspect Ratio settings
        if (quickSettingsUiState.aspectRatioUiState is AspectRatioUiState.Available) {
            AspectRatioRow(
                aspectRatioUiState = quickSettingsUiState.aspectRatioUiState,
                onSetAspectRatio = quickSettingsController::setAspectRatio
            )
        }

        // HDR settings
        if (quickSettingsUiState.hdrUiState is HdrUiState.Available) {
            QuickSetHdr(
                onClick = { d: DynamicRange, i: ImageOutputFormat ->
                    quickSettingsController.setDynamicRange(d)
                    quickSettingsController.setImageFormat(i)
                },
                hdrUiState = quickSettingsUiState.hdrUiState
            )
        }
    }
}

@Composable
fun VideoQuickSettings(
    quickSettingsUiState: QuickSettingsUiState.Available,
    quickSettingsController: QuickSettingsController
) {
    Column {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(id = R.string.quick_settings_title_video_settings),
            style = MaterialTheme.typography.titleLarge
        )

        // Flash Mode settings
        if (quickSettingsUiState.flashModeUiState is FlashModeUiState.Available) {
            FlashRow(
                onSetFlashMode = quickSettingsController::setFlash,
                flashModeUiState = quickSettingsUiState.flashModeUiState
            )
        }

        // HDR settings
        if (quickSettingsUiState.hdrUiState is HdrUiState.Available) {
            QuickSetHdr(
                onClick = { d: DynamicRange, i: ImageOutputFormat ->
                    quickSettingsController.setDynamicRange(d)
                    quickSettingsController.setImageFormat(i)
                },
                hdrUiState = quickSettingsUiState.hdrUiState
            )
        }
        // TODO: Add FPS setting
        // TODO: Add Resolution setting
        // TODO: Add Stabilization setting
    }
}

@Composable
fun ImageQuickSettings(
    quickSettingsUiState: QuickSettingsUiState.Available,
    quickSettingsController: QuickSettingsController
) {
    Column {
        Text(
            modifier = Modifier.padding(16.dp),
            text = stringResource(id = R.string.quick_settings_title_photo_settings),
            style = MaterialTheme.typography.titleLarge
        )

        // Flash Mode settings
        if (quickSettingsUiState.flashModeUiState is FlashModeUiState.Available) {
            FlashRow(
                onSetFlashMode = quickSettingsController::setFlash,
                flashModeUiState = quickSettingsUiState.flashModeUiState
            )
        }

        // Aspect Ratio settings
        if (quickSettingsUiState.aspectRatioUiState is AspectRatioUiState.Available) {
            AspectRatioRow(
                aspectRatioUiState = quickSettingsUiState.aspectRatioUiState,
                onSetAspectRatio = quickSettingsController::setAspectRatio
            )
        }

        // TODO: Add pre-capture timer setting
    }
}


/**
 * A no-op implementation of [QuickSettingsController] for use in Compose previews and tests.
 */
class NoOpQuickSettingsController : QuickSettingsController {
    override fun toggleQuickSettings() {}

    override fun setLensFacing(lensFace: LensFacing) {}

    override fun setFlash(flashMode: FlashMode) {}

    override fun setAspectRatio(aspectRatio: AspectRatio) {}

    override fun setStreamConfig(streamConfig: StreamConfig) {}

    override fun setDynamicRange(dynamicRange: DynamicRange) {}

    override fun setImageFormat(imageOutputFormat: ImageOutputFormat) {}

    override fun setConcurrentCameraMode(concurrentCameraMode: ConcurrentCameraMode) {}

    override fun setCaptureMode(captureMode: CaptureMode) {}
}
