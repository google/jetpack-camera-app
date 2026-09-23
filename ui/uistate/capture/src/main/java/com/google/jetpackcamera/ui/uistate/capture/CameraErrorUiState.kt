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
import com.google.jetpackcamera.model.CameraError

/**
 * Defines the UI state for the camera error alert dialog.
 */
sealed interface CameraErrorUiState {
    /** No error dialog should be shown. */
    data object Hidden : CameraErrorUiState

    /**
     * An error dialog should be shown.
     *
     * @property error The underlying [CameraError].
     * @property titleResId The string resource ID for the dialog title.
     * @property bodyResId The optional string resource ID for the dialog body message.
     * @property confirmButtonTextResId The string resource ID for the confirm button.
     * @property shouldExitAppOnConfirm Whether confirming the dialog should close the camera activity.
     */
    data class Showing(
        val error: CameraError,
        @StringRes val titleResId: Int,
        @StringRes val bodyResId: Int?,
        @StringRes val confirmButtonTextResId: Int,
        val shouldExitAppOnConfirm: Boolean
    ) : CameraErrorUiState

    companion object
}
