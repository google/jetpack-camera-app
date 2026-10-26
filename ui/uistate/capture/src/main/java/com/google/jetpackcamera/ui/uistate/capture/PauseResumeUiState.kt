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

/**
 * Defines the UI state for the pause/resume toggle button during video recording.
 */
sealed interface PauseResumeUiState {
    /**
     * The pause/resume toggle button is hidden and should not be displayed.
     */
    data object Hidden : PauseResumeUiState

    /**
     * The pause/resume toggle button is visible.
     *
     * @property isPaused Whether the active video recording is currently paused.
     */
    data class Visible(val isPaused: Boolean = false) : PauseResumeUiState

    companion object
}
