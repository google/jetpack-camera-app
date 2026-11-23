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

import com.google.jetpackcamera.model.StabilizationMode

/**
 * Defines the UI state for video stabilization settings.
 *
 * This sealed interface represents the different states of the video stabilization UI, such as
 * being disabled or enabled with a specific or automatic mode.
 */
sealed interface StabilizationUiState {
    /**
     * No video stabilization is applied, and the user did not turn it off. This is the case when
     * automatic stabilization applies no stabilization, or when the setting is locked to off.
     */
    data object Disabled : StabilizationUiState

    /**
     * The user turned video stabilization off.
     */
    data object Off : StabilizationUiState

    /**
     * Video stabilization is enabled.
     */
    sealed interface Enabled : StabilizationUiState {
        /** The currently active [StabilizationMode]. */
        val stabilizationMode: StabilizationMode

        /** Indicates whether stabilization is currently active. */
        val active: Boolean
    }

    /**
     * A specific, non-automatic stabilization mode is selected.
     *
     * @param stabilizationMode The specific [StabilizationMode] that is selected. Must not be [StabilizationMode.AUTO].
     * @param active Indicates whether the stabilization is currently active.
     */
    data class Specific(
        override val stabilizationMode: StabilizationMode,
        override val active: Boolean = true
    ) : Enabled {
        init {
            require(stabilizationMode != StabilizationMode.AUTO) {
                "Specific StabilizationUiState cannot have AUTO stabilization mode."
            }
        }
    }

    /**
     * The stabilization mode is set to automatic.
     *
     * @param stabilizationMode The mode that the camera applies for automatic stabilization, such
     *   as [StabilizationMode.ON] or [StabilizationMode.OPTICAL]. Must not be
     *   [StabilizationMode.AUTO] or [StabilizationMode.OFF].
     */
    data class Auto(override val stabilizationMode: StabilizationMode) : Enabled {
        override val active = true

        init {
            require(
                stabilizationMode != StabilizationMode.AUTO &&
                    stabilizationMode != StabilizationMode.OFF
            ) {
                "Auto StabilizationUiState must hold the mode in use, not $stabilizationMode."
            }
        }
    }

    companion object
}
