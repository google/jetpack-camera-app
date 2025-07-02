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

sealed interface AudioUiState {
    val amplitude: Double

    sealed interface Enabled : AudioUiState {
        data class On(override val amplitude: Double) : Enabled
        data object Mute : Enabled {
            override val amplitude = 0.0
        }
    }

    // todo give a disabledreason when audio permission is not granted
    data object Disabled : AudioUiState {
        override val amplitude = 0.0
    }

    companion object
}
