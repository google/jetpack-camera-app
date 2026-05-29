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

/**
 * Defines the UI state for audio recording.
 *
 * This sealed interface represents the different states of the audio input, such as whether it is
 * enabled, muted, or disabled, and provides the current audio amplitude for UI visualization.
 */
sealed interface AudioUiState {
    /**
     * The current amplitude of the microphone, normalized to a range suitable for UI rendering
     * (e.g., 0.0 to 1.0). A value of 0.0 indicates silence or that audio is disabled.
     */
    val amplitude: Double

    /**
     * Represents the states where audio recording is enabled.
     */
    sealed interface Enabled : AudioUiState {
        /**
         * Audio is actively being recorded.
         *
         * @param amplitude The current amplitude of the microphone.
         */
        data class On(override val amplitude: Double) : Enabled

        /**
         * Audio recording is enabled but currently muted by the user. The amplitude is always 0.0.
         */
        data object Mute : Enabled {
            override val amplitude = 0.0
        }
    }

    /**
     * Audio recording is disabled.
     * This can occur if the user has not granted the necessary audio permissions or if the
     * microphone is otherwise unavailable. The amplitude is always 0.0.
     */
    data object Disabled : AudioUiState {
        override val amplitude = 0.0
    }

    companion object
}
