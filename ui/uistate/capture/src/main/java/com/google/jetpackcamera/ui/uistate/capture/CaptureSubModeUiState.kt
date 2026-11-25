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
package com.google.jetpackcamera.ui.uistate.capture

import androidx.annotation.StringRes
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState

/**
 * UI model for a single item in the capture sub-mode selector carousel.
 */
data class CaptureSubModeOption(
    val id: CaptureSubModeId,
    @param:StringRes val labelResId: Int
)

/**
 * UI state for the horizontal capture sub-mode selector carousel.
 *
 * Reports [Unavailable] when only the default mode (`CaptureSubModeId.DEFAULT`) is supported
 * for the active parent `CaptureMode` on the current lens, so no single-pill carousel is rendered.
 */
sealed interface CaptureSubModeUiState {
    data object Unavailable : CaptureSubModeUiState

    /**
     * The carousel is shown.
     *
     * @param selectedSubMode The selected sub-mode.
     * @param availableSubModes The sub-modes shown in the carousel, in display order. Each one is
     * either selectable or disabled with a rationale.
     * @param isEnabled Whether the user can change the sub-mode with the carousel. When `false`,
     * the whole carousel is shown disabled and ignores taps and drags.
     */
    data class Available(
        val selectedSubMode: CaptureSubModeId,
        val availableSubModes: List<SingleSelectableUiState<CaptureSubModeOption>>,
        val isEnabled: Boolean = true
    ) : CaptureSubModeUiState

    companion object
}
