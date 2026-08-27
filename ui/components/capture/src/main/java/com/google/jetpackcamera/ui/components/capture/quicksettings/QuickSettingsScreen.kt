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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.QUICK_SETTINGS_BOTTOM_SHEET
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.AspectRatioRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.CaptureModeRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.HdrRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickNavSettings
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSettingsModalBottomSheet
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState

/**
 * Agnostic content for the Quick Settings panel wrapped for a BottomSheetScaffold sheet.
 */
@Composable
fun QuickSettingsScaffoldContent(
    quickSettingsUiState: QuickSettingsUiState,
    onNavigateToSettings: () -> Unit,
    quickSettingsController: QuickSettingsController,
    modifier: Modifier = Modifier,
    showMoreSettingsButton: Boolean = true
) {
    if (quickSettingsUiState is QuickSettingsUiState.Available) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .testTag(QUICK_SETTINGS_BOTTOM_SHEET)
        ) {
            QuickSettingsContent(
                quickSettingsUiState = quickSettingsUiState,
                quickSettingsController = quickSettingsController,
                onNavigateToSettings = onNavigateToSettings,
                showMoreSettingsButton = showMoreSettingsButton
            )
        }
    }
}

/**
 * The UI bottom sheet component for quick settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    quickSettingsUiState: QuickSettingsUiState,
    onNavigateToSettings: () -> Unit,
    quickSettingsController: QuickSettingsController,
    modifier: Modifier = Modifier,
    showMoreSettingsButton: Boolean = true
) {
    if (isOpen && quickSettingsUiState is QuickSettingsUiState.Available) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        QuickSettingsModalBottomSheet(
            modifier = modifier,
            onDismiss = onDismiss,
            sheetState = sheetState
        ) {
            QuickSettingsContent(
                quickSettingsUiState = quickSettingsUiState,
                quickSettingsController = quickSettingsController,
                onNavigateToSettings = onNavigateToSettings,
                showMoreSettingsButton = showMoreSettingsButton
            )
        }
    }
}

/**
 * Agnostic content for the Quick Settings panel containing the title header, option rows
 * (Flash, Capture Mode, Aspect Ratio, HDR), and an optional navigation button to full settings.
 *
 * @param quickSettingsUiState The current [QuickSettingsUiState.Available].
 * @param quickSettingsController The [QuickSettingsController] to handle setting changes.
 * @param onNavigateToSettings Callback when the user navigates to full settings.
 * @param modifier The [Modifier] to apply to the content column.
 * @param showMoreSettingsButton Whether to show the "More settings" navigation button.
 */
@Composable
internal fun QuickSettingsContent(
    quickSettingsUiState: QuickSettingsUiState.Available,
    quickSettingsController: QuickSettingsController,
    onNavigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
    showMoreSettingsButton: Boolean = true
) {
    val captureMode = (quickSettingsUiState.captureModeUiState as? CaptureModeUiState.Available)
        ?.selectedCaptureMode ?: CaptureMode.IMAGE_ONLY

    val titleRes = when (captureMode) {
        CaptureMode.VIDEO_ONLY -> R.string.quick_settings_title_video_settings
        CaptureMode.IMAGE_ONLY -> R.string.quick_settings_title_photo_settings
        CaptureMode.STANDARD -> R.string.quick_settings_title_photo_and_video_settings
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.titleLarge
        )

        // Flash Mode settings
        if (quickSettingsUiState.flashModeUiState is FlashModeUiState.Available) {
            FlashRow(
                onSetFlashMode = quickSettingsController::setFlash,
                flashModeUiState = quickSettingsUiState.flashModeUiState
            )
        }

        // Capture Mode settings (Standard only)
        if (captureMode == CaptureMode.STANDARD
        ) {
            CaptureModeRow(
                onSetCaptureMode = quickSettingsController::setCaptureMode,
                captureModeUiState = quickSettingsUiState.captureModeUiState
            )
        }

        // Aspect Ratio settings (Standard and Image only)
        if ((captureMode == CaptureMode.STANDARD || captureMode == CaptureMode.IMAGE_ONLY) &&
            quickSettingsUiState.aspectRatioUiState is AspectRatioUiState.Available
        ) {
            AspectRatioRow(
                aspectRatioUiState = quickSettingsUiState.aspectRatioUiState,
                onSetAspectRatio = quickSettingsController::setAspectRatio
            )
        }

        // HDR settings
        if (quickSettingsUiState.hdrUiState is HdrUiState.Available) {
            HdrRow(
                onClick = { d: DynamicRange, i: ImageOutputFormat ->
                    when (captureMode) {
                        CaptureMode.STANDARD -> {
                            quickSettingsController.setDynamicRange(d)
                            quickSettingsController.setImageFormat(i)
                        }

                        CaptureMode.VIDEO_ONLY -> quickSettingsController.setDynamicRange(d)
                        CaptureMode.IMAGE_ONLY -> quickSettingsController.setImageFormat(i)
                    }
                },
                hdrUiState = quickSettingsUiState.hdrUiState
            )
        }

        if (showMoreSettingsButton) {
            QuickNavSettings(
                onNavigateToSettings = onNavigateToSettings,
                modifier = Modifier.padding(top = 12.dp)
            )
        }
    }
}

/**
 * A no-op implementation of [QuickSettingsController] for use in Compose previews and tests.
 */
class NoOpQuickSettingsController : QuickSettingsController {
    override fun setLensFacing(lensFace: LensFacing) {}

    override fun setFlash(flashMode: FlashMode) {}

    override fun setAspectRatio(aspectRatio: AspectRatio) {}

    override fun setDynamicRange(dynamicRange: DynamicRange) {}

    override fun setImageFormat(imageOutputFormat: ImageOutputFormat) {}

    override fun setCaptureMode(captureMode: CaptureMode) {}
}

@Preview
@Composable
fun ExpandedQuickSettingsUiPreview() {
    MaterialTheme {
        QuickSettingsBottomSheet(
            isOpen = true,
            onDismiss = {},
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
                hdrUiState = HdrUiState.Unavailable
            ),
            onNavigateToSettings = {},
            quickSettingsController = NoOpQuickSettingsController()
        )
    }
}
