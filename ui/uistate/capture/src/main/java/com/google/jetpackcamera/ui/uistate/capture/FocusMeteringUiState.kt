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
 * Defines the UI state for focus metering. See [FocusMeteringIndicator]
 *
 * This sealed interface represents the different states of the focus metering UI
 */
sealed interface FocusMeteringUiState {

    /**
     * The focus/metering state is unspecified. This is the default state where no tap-to-focus
     * gesture has been initiated.
     */
    data object Unspecified : FocusMeteringUiState

    /**
     * A specific focus metering point has been set by the user.
     *
     * @param surfaceCoordinates The coordinates on the surface where the user tapped.
     * @param status The current [Status] of the focus and metering operation.
     */
    data class Specified(
        val surfaceCoordinates: Offset,
        val status: Status
    ) : FocusMeteringUiState

    /**
     * Represents the status of a user-initiated focus and metering action.
     */
    enum class Status {
        /**
         * The focus metering operation is currently in progress.
         */
        RUNNING,

        /**
         * The focus metering operation completed successfully.
         */
        SUCCESS,

        /**
         * The focus metering operation failed.
         */
        FAILURE,

        /**
         * The focus metering operation was cancelled.
         */
        CANCELLED
    }

    companion object
}
