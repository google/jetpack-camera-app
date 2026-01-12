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

import com.google.jetpackcamera.model.CaptureMode

/**
 * Defines the UI state for the capture button.
 */
sealed interface CaptureButtonUiState {
    /**
     * Whether the capture button is enabled and can be interacted with.
     */
    val isEnabled: Boolean

    /**
     * The capture button is unavailable and should not be shown or interacted with.
     */
    data object Unavailable : CaptureButtonUiState {
        override val isEnabled: Boolean = false
    }

    /**
     * The capture button is available to be shown.
     */
    sealed interface Available : CaptureButtonUiState {
        /**
         * The capture button is idle and ready to capture.
         *
         * @property captureMode The current capture mode.
         * @property isEnabled Whether the button is enabled for interaction.
         */
        data class Idle(
            val captureMode: CaptureMode,
            override val isEnabled: Boolean = true
        ) : Available

        /**
         * The capture button is currently recording video.
         */
        sealed interface Recording : Available {
            override val isEnabled: Boolean get() = true

            /**
             * The user is actively pressing the capture button to record.
             */
            data object PressedRecording : Recording

            /**
             * The recording is locked and continues without user interaction.
             */
            data object LockedRecording : Recording
        }
    }

    companion object
}
