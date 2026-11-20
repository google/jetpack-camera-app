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
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import org.junit.Test

class StabilizationUiStateAdapterTest {

    @Test
    fun off_returnsOff() {
        val state = stabilizationUiState(
            selected = StabilizationMode.OFF,
            inUse = StabilizationMode.OFF
        )

        assertThat(state).isEqualTo(StabilizationUiState.Off)
    }

    @Test
    fun off_restrictedToSomeOptions_returnsOff() {
        val state = stabilizationUiState(
            selected = StabilizationMode.OFF,
            inUse = StabilizationMode.OFF,
            optionVisibility = OptionVisibility.Only(StabilizationMode.OFF, StabilizationMode.ON)
        )

        assertThat(state).isEqualTo(StabilizationUiState.Off)
    }

    @Test
    fun off_settingHidden_returnsDisabled() {
        val state = stabilizationUiState(
            selected = StabilizationMode.OFF,
            inUse = StabilizationMode.OFF,
            optionVisibility = OptionVisibility.Hidden
        )

        assertThat(state).isEqualTo(StabilizationUiState.Disabled)
    }

    @Test
    fun auto_noStabilizationInUse_returnsDisabled() {
        val state = stabilizationUiState(
            selected = StabilizationMode.AUTO,
            inUse = StabilizationMode.OFF
        )

        assertThat(state).isEqualTo(StabilizationUiState.Disabled)
    }

    @Test
    fun auto_previewStabilizationInUse_returnsAutoWithModeInUse() {
        val state = stabilizationUiState(
            selected = StabilizationMode.AUTO,
            inUse = StabilizationMode.ON
        )

        assertThat(state).isEqualTo(StabilizationUiState.Auto(StabilizationMode.ON))
    }

    @Test
    fun on_inUse_returnsActiveSpecific() {
        val state = stabilizationUiState(
            selected = StabilizationMode.ON,
            inUse = StabilizationMode.ON
        )

        assertThat(state).isEqualTo(
            StabilizationUiState.Specific(StabilizationMode.ON, active = true)
        )
    }

    @Test
    fun on_notInUse_returnsInactiveSpecific() {
        val state = stabilizationUiState(
            selected = StabilizationMode.ON,
            inUse = StabilizationMode.OFF
        )

        assertThat(state).isEqualTo(
            StabilizationUiState.Specific(StabilizationMode.ON, active = false)
        )
    }

    private fun stabilizationUiState(
        selected: StabilizationMode,
        inUse: StabilizationMode,
        optionVisibility: OptionVisibility<StabilizationMode>? = null
    ): StabilizationUiState = StabilizationUiState.from(
        cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(stabilizationMode = selected),
        cameraState = CameraState(stabilizationMode = inUse),
        optionVisibility = optionVisibility
    )
}
