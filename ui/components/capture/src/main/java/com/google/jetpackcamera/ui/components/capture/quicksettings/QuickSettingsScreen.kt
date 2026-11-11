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

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeId
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
import com.google.jetpackcamera.ui.controller.quicksettings.QuickSettingsController
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState

/**
 * Resolves the title string resource ID for the Quick Settings panel based on
 * [QuickSettingsUiState.Available.titleResId] or the active [CaptureMode] in
 * [quickSettingsUiState].
 *
 * @param quickSettingsUiState The current [QuickSettingsUiState.Available].
 * @return The string resource ID used for the panel header and accessibility pane title.
 */
@StringRes
internal fun quickSettingsTitleRes(quickSettingsUiState: QuickSettingsUiState.Available): Int {
    quickSettingsUiState.titleResId?.let { return it }

    val captureMode = (quickSettingsUiState.captureModeUiState as? CaptureModeUiState.Available)
        ?.selectedCaptureMode ?: CaptureMode.IMAGE_ONLY

    return when (captureMode) {
        CaptureMode.VIDEO_ONLY -> R.string.quick_settings_title_video_settings
        CaptureMode.IMAGE_ONLY -> R.string.quick_settings_title_photo_settings
        CaptureMode.STANDARD -> R.string.quick_settings_title_photo_and_video_settings
    }
}

/**
 * Agnostic content for the Quick Settings panel wrapped for a BottomSheetScaffold sheet.
 *
 * @param quickSettingsUiState The current [QuickSettingsUiState].
 * @param onNavigateToSettings Callback when the user navigates to full settings.
 * @param quickSettingsController The [QuickSettingsController] to handle setting changes.
 * @param modifier The [Modifier] to apply to the content column.
 * @param showMoreSettingsButton Whether to show the "More settings" navigation button.
 */
@OptIn(ExperimentalLayoutApi::class)
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
                .windowInsetsPadding(WindowInsets.navigationBarsIgnoringVisibility)
                .padding(bottom = 24.dp)
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

    val titleRes = quickSettingsTitleRes(quickSettingsUiState)

    // Note: QUICK_SETTINGS_BOTTOM_SHEET is temporarily retained on QuickSettingsContent so existing
    // downstream tests find Quick Settings across both BottomSheetScaffold and
    // QuickSettingsOverlayCard. Future cleanup will reserve QUICK_SETTINGS_BOTTOM_SHEET for the
    // bottom sheet, QUICK_SETTINGS_OVERLAY_CARD_TAG for the floating card, and introduce a shared
    // QUICK_SETTINGS_CONTENT_TAG here.
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(QUICK_SETTINGS_BOTTOM_SHEET)
    ) {
        Text(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .semantics { heading() },
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
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
internal class NoOpQuickSettingsController : QuickSettingsController {
    override fun setLensFacing(lensFace: LensFacing) {}

    override fun setFlash(flashMode: FlashMode) {}

    override fun setAspectRatio(aspectRatio: AspectRatio) {}

    override fun setDynamicRange(dynamicRange: DynamicRange) {}

    override fun setImageFormat(imageOutputFormat: ImageOutputFormat) {}

    override fun setCaptureMode(captureMode: CaptureMode) {}

    override fun setCaptureSubMode(captureSubModeId: CaptureSubModeId) {}
}

@Preview
@Composable
private fun ExpandedQuickSettingsUiPreview() {
    MaterialTheme {
        QuickSettingsScaffoldContent(
            quickSettingsUiState = QuickSettingsUiState.Available(
                aspectRatioUiState = AspectRatioUiState.Available(
                    selectedAspectRatio = AspectRatio.NINE_SIXTEEN,
                    availableAspectRatios = listOf(
                        SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
                        SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE),
                        SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN)
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
