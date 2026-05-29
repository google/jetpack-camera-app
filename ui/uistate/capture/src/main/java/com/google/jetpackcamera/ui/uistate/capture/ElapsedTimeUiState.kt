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
 * Defines the UI state for the elapsed time display, typically used during video recording.
 *
 * This sealed interface represents the different states of the timer, which can be either
 * unavailable (not shown) or enabled, displaying the current duration of the recording.
 */
sealed interface ElapsedTimeUiState {
    /**
     * The elapsed time display is unavailable.
     * This state is used when video recording is not in progress.
     */
    data object Unavailable : ElapsedTimeUiState

    /**
     * The elapsed time display is enabled and showing the current recording time.
     *
     * @param elapsedTimeNanos The elapsed time in nanoseconds.
     */
    data class Enabled(val elapsedTimeNanos: Long) : ElapsedTimeUiState

    companion object
}
