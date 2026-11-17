/*
 * Copyright (C) 2026 The Android Open Source Project
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

import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState

private val SAMPLE_ASPECT_RATIO_UI_STATE: AspectRatioUiState = AspectRatioUiState.Available(
    selectedAspectRatio = AspectRatio.NINE_SIXTEEN,
    availableAspectRatios = listOf(
        SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN),
        SingleSelectableUiState.SelectableUi(AspectRatio.THREE_FOUR),
        SingleSelectableUiState.SelectableUi(AspectRatio.ONE_ONE)
    )
)

private val SAMPLE_CAPTURE_MODE_UI_STATE: CaptureModeUiState = CaptureModeUiState.Available(
    selectedCaptureMode = CaptureMode.STANDARD,
    availableCaptureModes = listOf(
        SingleSelectableUiState.SelectableUi(CaptureMode.STANDARD),
        SingleSelectableUiState.SelectableUi(CaptureMode.VIDEO_ONLY),
        SingleSelectableUiState.SelectableUi(CaptureMode.IMAGE_ONLY)
    )
)

private val SAMPLE_FLASH_MODE_UI_STATE: FlashModeUiState = FlashModeUiState.Available(
    selectedFlashMode = FlashMode.OFF,
    availableFlashModes = listOf(
        SingleSelectableUiState.SelectableUi(FlashMode.OFF),
        SingleSelectableUiState.SelectableUi(FlashMode.ON),
        SingleSelectableUiState.SelectableUi(FlashMode.AUTO)
    ),
    isLowLightBoostActive = false
)

private val SAMPLE_FLIP_LENS_UI_STATE: FlipLensUiState = FlipLensUiState.Available(
    selectedLensFacing = LensFacing.BACK,
    availableLensFacings = listOf(
        SingleSelectableUiState.SelectableUi(LensFacing.BACK),
        SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
    )
)

private val SAMPLE_HDR_UI_STATE: HdrUiState = HdrUiState.Available(
    selectedImageFormat = ImageOutputFormat.JPEG,
    selectedDynamicRange = DynamicRange.SDR,
    isSupported = true
)

/**
 * Builds a fully populated [QuickSettingsUiState.Available] for tests, with per-row overrides.
 */
fun sampleQuickSettingsUiState(
    aspectRatioUiState: AspectRatioUiState = SAMPLE_ASPECT_RATIO_UI_STATE,
    captureModeUiState: CaptureModeUiState = SAMPLE_CAPTURE_MODE_UI_STATE,
    flashModeUiState: FlashModeUiState = SAMPLE_FLASH_MODE_UI_STATE,
    flipLensUiState: FlipLensUiState = SAMPLE_FLIP_LENS_UI_STATE,
    hdrUiState: HdrUiState = SAMPLE_HDR_UI_STATE,
    titleResId: Int? = null
): QuickSettingsUiState.Available = QuickSettingsUiState.Available(
    aspectRatioUiState = aspectRatioUiState,
    captureModeUiState = captureModeUiState,
    flashModeUiState = flashModeUiState,
    flipLensUiState = flipLensUiState,
    hdrUiState = hdrUiState,
    titleResId = titleResId
)
