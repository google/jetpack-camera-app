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
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.components.capture.R
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.AspectRatioRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.CaptureModeRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.FlashRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.HdrRow
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickNavSettings
import com.google.jetpackcamera.ui.components.capture.quicksettings.ui.QuickSettingsModalBottomSheet
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState

/**
 * Events representing user interactions with the Quick Settings UI.
 */
sealed interface QuickSettingsEvent {
    data class SetFlashMode(val flashMode: FlashMode) : QuickSettingsEvent
    data class SetCaptureMode(val captureMode: CaptureMode) : QuickSettingsEvent
    data class SetAspectRatio(val aspectRatio: AspectRatio) : QuickSettingsEvent
    data class SetHdr(val dynamicRange: DynamicRange, val imageFormat: ImageOutputFormat) : QuickSettingsEvent
    data object ToggleSheet : QuickSettingsEvent
}

/**
 * The UI bottom sheet component for quick settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickSettingsBottomSheet(
    quickSettingsUiState: QuickSettingsUiState,
    onEvent: (QuickSettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToSettings: () -> Unit = {},
    showMoreSettingsButton: Boolean = true
) {
    if (quickSettingsUiState is QuickSettingsUiState.Available &&
        quickSettingsUiState.quickSettingsIsOpen
    ) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        QuickSettingsModalBottomSheet(
            modifier = modifier,
            onDismiss = { onEvent(QuickSettingsEvent.ToggleSheet) },
            sheetState = sheetState
        ) {
            QuickSettingsContent(
                quickSettingsUiState = quickSettingsUiState,
                onEvent = onEvent,
                onNavigateToSettings = onNavigateToSettings,
                showMoreSettingsButton = showMoreSettingsButton
            )
        }
    }
}

@Composable
private fun QuickSettingsLayout(
    @StringRes titleRes: Int,
    showMoreSettingsButton: Boolean,
    onNavigateToSettings: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Column {
        Text(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
            text = stringResource(id = titleRes),
            style = MaterialTheme.typography.titleLarge
        )
        content()
    }
    if (showMoreSettingsButton) {
        QuickNavSettings(onNavigateToSettings = onNavigateToSettings)
    }
}

@Composable
private fun QuickSettingsContent(
    quickSettingsUiState: QuickSettingsUiState.Available,
    onEvent: (QuickSettingsEvent) -> Unit,
    onNavigateToSettings: () -> Unit,
    showMoreSettingsButton: Boolean
) {
    val captureMode = (quickSettingsUiState.captureModeUiState as? CaptureModeUiState.Available)
        ?.selectedCaptureMode ?: CaptureMode.IMAGE_ONLY

    val titleRes = when (captureMode) {
        CaptureMode.VIDEO_ONLY -> R.string.quick_settings_title_video_settings
        CaptureMode.IMAGE_ONLY -> R.string.quick_settings_title_photo_settings
        CaptureMode.STANDARD -> R.string.quick_settings_title_photo_and_video_settings
    }

    QuickSettingsLayout(
        titleRes = titleRes,
        showMoreSettingsButton = showMoreSettingsButton,
        onNavigateToSettings = onNavigateToSettings
    ) {
        // Flash Mode settings
        if (quickSettingsUiState.flashModeUiState is FlashModeUiState.Available) {
            FlashRow(
                onSetFlashMode = { onEvent(QuickSettingsEvent.SetFlashMode(it)) },
                flashModeUiState = quickSettingsUiState.flashModeUiState
            )
        }

        // Capture Mode settings (Standard only)
        if (captureMode == CaptureMode.STANDARD &&
            quickSettingsUiState.captureModeUiState is CaptureModeUiState.Available
        ) {
            CaptureModeRow(
                onSetCaptureMode = { onEvent(QuickSettingsEvent.SetCaptureMode(it)) },
                captureModeUiState = quickSettingsUiState.captureModeUiState
            )
        }

        // Aspect Ratio settings (Standard and Image only)
        if ((captureMode == CaptureMode.STANDARD || captureMode == CaptureMode.IMAGE_ONLY) &&
            quickSettingsUiState.aspectRatioUiState is AspectRatioUiState.Available
        ) {
            AspectRatioRow(
                aspectRatioUiState = quickSettingsUiState.aspectRatioUiState,
                onSetAspectRatio = { onEvent(QuickSettingsEvent.SetAspectRatio(it)) }
            )
        }

        // HDR settings
        if (quickSettingsUiState.hdrUiState is HdrUiState.Available) {
            HdrRow(
                onClick = { d: DynamicRange, i: ImageOutputFormat ->
                    onEvent(QuickSettingsEvent.SetHdr(d, i))
                },
                hdrUiState = quickSettingsUiState.hdrUiState
            )
        }
    }
}
