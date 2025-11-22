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
package com.google.jetpackcamera.ui.uistate.capture

import androidx.compose.ui.geometry.Offset

/**
 * Represents the UI state of a focus metering/autofocus event
 */
sealed interface FocusMeteringUiState {

    data object Unspecified : FocusMeteringUiState

    data class Specified(
        val surfaceCoordinates: Offset,
        val status: Status
    ) : FocusMeteringUiState

    enum class Status {
        RUNNING,
        SUCCESS,
        FAILURE,
        CANCELLED
    }

    companion object
}
