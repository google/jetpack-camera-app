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
 *
 * This sealed interface represents all possible states of the main capture button in the camera UI.
 * It is used to control the button's appearance and behavior, such as whether it is enabled,
 * what action it performs (photo vs. video), and its visual state during video recording.
 */
sealed interface CaptureButtonUiState {
    /**
     * The capture button is unavailable and should be disabled.
     * This is used when the camera is not ready or an operation is in progress that
     * precludes capturing.
     */
    data object Unavailable : CaptureButtonUiState

    /**
     * The capture button is enabled and ready for user interaction.
     */
    sealed interface Enabled : CaptureButtonUiState {
        /**
         * The button is in an idle state, ready to start a capture.
         *
         * @param captureMode The current [CaptureMode] (e.g., [CaptureMode.IMAGE] or
         *   [CaptureMode.VIDEO]) to indicate the button's primary action.
         */
        data class Idle(val captureMode: CaptureMode) : Enabled

        /**
         * The button is in a video recording state.
         */
        sealed interface Recording : Enabled {
            /**
             * The user is actively pressing the button to record video (press-and-hold).
             */
            data object PressedRecording : Recording

            /**
             * The video recording has been locked and will continue until the user
             * explicitly stops it.
             */
            data object LockedRecording : Recording
        }
    }

    companion object
}
