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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.settings.api.OptionRestrictionConfig
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CaptureModeUiStateAdapterTest {

    @Test
    fun from_notRestricted_enablesSupportedModes() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionRestrictionConfig.NotRestricted(),
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
            externalCaptureMode = ExternalCaptureMode.Standard
        )

        assertThat(uiState).isInstanceOf(CaptureModeUiState.Available::class.java)
        val available = uiState as CaptureModeUiState.Available
        assertThat(available.availableCaptureModes).isNotEmpty()
        assertThat(available.availableCaptureModes.all { it is SingleSelectableUiState.SelectableUi }).isTrue()
    }

    @Test
    fun from_fullyRestricted_disablesRestrictedModes() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionRestrictionConfig.FullyRestricted(),
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(captureMode = CaptureMode.IMAGE_ONLY),
            externalCaptureMode = ExternalCaptureMode.Standard
        )

        assertThat(uiState).isInstanceOf(CaptureModeUiState.Available::class.java)
        val available = uiState as CaptureModeUiState.Available
        val standardState = available.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.STANDARD
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.STANDARD
            }
        }
        assertThat(standardState).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        val disabled = standardState as SingleSelectableUiState.Disabled
        assertThat(disabled.disabledReason).isEqualTo(DisabledReason.VIDEO_CAPTURE_RESTRICTED)
    }
}
