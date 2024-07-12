/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.settings

import com.google.jetpackcamera.settings.ui.DEVICE_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.FPS_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.LENS_UNSUPPORTED_TAG
import com.google.jetpackcamera.settings.ui.STABILIZATION_UNSUPPORTED_TAG

val deviceUnsupported =
    SettingEnabledState.Disabled(disabledRationale = setOf(DisabledRationale.DEVICE_UNSUPPORTED))

sealed interface SettingUiState {
    val settingEnabledState: SettingEnabledState
}

sealed interface SettingEnabledState {
    val isEnabled: Boolean

    data class Disabled(
        val disabledRationale: Set<DisabledRationale>
    ) : SettingEnabledState {
        override val isEnabled: Boolean = false
    }

    data object Enabled : SettingEnabledState {
        override val isEnabled: Boolean = true
    }
}

class FpsUiState(
    override val settingEnabledState: SettingEnabledState,
    val optionsStates: Map<Int, SettingEnabledState>
) : SettingUiState

class FlipLensUiState(override val settingEnabledState: SettingEnabledState) : SettingUiState
class StabilizationUiState(
    override val settingEnabledState: SettingEnabledState,
    val previewStabilizationState: SettingEnabledState,
    val videoStabilizationState: SettingEnabledState
) : SettingUiState

enum class DisabledRationale(val testTag: String, val reasonTextResId: Int) {
    DEVICE_UNSUPPORTED(
        DEVICE_UNSUPPORTED_TAG,
        R.string.device_unsupported
    ),
    FPS(FPS_UNSUPPORTED_TAG, R.string.fps_unsupported),
    STABILIZATION_UNSUPPORTED(STABILIZATION_UNSUPPORTED_TAG, R.string.stabilization_unsupported),
    LENS_UNSUPPORTED(LENS_UNSUPPORTED_TAG, R.string.lens_unsupported)
}
