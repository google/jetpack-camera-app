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
package com.google.jetpackcamera.ui.uistateadapter.capture.compound

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
internal class QuickSettingsUiStateAdapterTest {

    @Test
    fun from_correctlyBundlesStates() {
        val captureModeUiState = CaptureModeUiState.Unavailable
        val flashModeUiState = FlashModeUiState.Unavailable
        val flipLensUiState = FlipLensUiState.Unavailable
        val aspectRatioUiState = AspectRatioUiState.Unavailable
        val hdrUiState = HdrUiState.Unavailable
        val quickSettingsIsOpen = true

        val quickSettingsUiState = QuickSettingsUiState.from(
            captureModeUiState = captureModeUiState,
            flashModeUiState = flashModeUiState,
            flipLensUiState = flipLensUiState,
            aspectRatioUiState = aspectRatioUiState,
            hdrUiState = hdrUiState,
            quickSettingsIsOpen = quickSettingsIsOpen
        )

        assertThat(quickSettingsUiState).isInstanceOf(QuickSettingsUiState.Available::class.java)
        val availableState = quickSettingsUiState as QuickSettingsUiState.Available
        assertThat(availableState.captureModeUiState).isEqualTo(captureModeUiState)
        assertThat(availableState.flashModeUiState).isEqualTo(flashModeUiState)
        assertThat(availableState.flipLensUiState).isEqualTo(flipLensUiState)
        assertThat(availableState.aspectRatioUiState).isEqualTo(aspectRatioUiState)
        assertThat(availableState.hdrUiState).isEqualTo(hdrUiState)
        assertThat(availableState.quickSettingsIsOpen).isEqualTo(quickSettingsIsOpen)
    }
}
