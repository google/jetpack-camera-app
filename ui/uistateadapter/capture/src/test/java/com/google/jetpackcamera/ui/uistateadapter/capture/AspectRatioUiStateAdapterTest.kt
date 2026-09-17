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
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class AspectRatioUiStateAdapterTest {

    @Test
    fun from_defaultVisibility_returnsAllSupportedAspectRatiosAvailable() {
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(aspectRatio = AspectRatio.THREE_FOUR)
        val state = AspectRatioUiState.from(settings)

        assertThat(state).isInstanceOf(AspectRatioUiState.Available::class.java)
        val available = state as AspectRatioUiState.Available
        assertThat(available.selectedAspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
        val aspectRatios = available.availableAspectRatios.map { it.value }
        assertThat(aspectRatios).containsExactly(
            AspectRatio.NINE_SIXTEEN,
            AspectRatio.THREE_FOUR,
            AspectRatio.ONE_ONE
        ).inOrder()
    }

    @Test
    fun from_hiddenVisibility_returnsUnavailable() {
        val settings = DEFAULT_CAMERA_APP_SETTINGS
        val state = AspectRatioUiState.from(settings, OptionVisibility.Hidden)

        assertThat(state).isEqualTo(AspectRatioUiState.Unavailable)
    }

    @Test
    fun from_onlySingleOptionViaFactory_returnsUnavailable() {
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(aspectRatio = AspectRatio.THREE_FOUR)
        val state = AspectRatioUiState.from(
            settings,
            OptionVisibility.from(setOf(AspectRatio.THREE_FOUR))
        )

        // When only 1 option is provided, OptionVisibility.from resolves to Hidden and returns Unavailable
        assertThat(state).isEqualTo(AspectRatioUiState.Unavailable)
    }

    @Test
    fun from_onlySubsetOptions_returnsAvailableWithOnlyThoseOptions() {
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(aspectRatio = AspectRatio.THREE_FOUR)
        val state = AspectRatioUiState.from(
            settings,
            OptionVisibility.Only(setOf(AspectRatio.ONE_ONE, AspectRatio.THREE_FOUR))
        )

        assertThat(state).isInstanceOf(AspectRatioUiState.Available::class.java)
        val available = state as AspectRatioUiState.Available
        assertThat(available.selectedAspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
        val aspectRatios = available.availableAspectRatios.map { it.value }
        assertThat(aspectRatios).containsExactly(
            AspectRatio.THREE_FOUR,
            AspectRatio.ONE_ONE
        ).inOrder()
    }

    @Test
    fun from_onlySubsetOptions_fallsBackSelectedIfCurrentNotAllowed() {
        // Current settings is 9:16, but policy only allows 1:1 and 3:4
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(aspectRatio = AspectRatio.NINE_SIXTEEN)
        val state = AspectRatioUiState.from(
            settings,
            OptionVisibility.Only(setOf(AspectRatio.ONE_ONE, AspectRatio.THREE_FOUR))
        )

        assertThat(state).isInstanceOf(AspectRatioUiState.Available::class.java)
        val available = state as AspectRatioUiState.Available
        assertThat(available.selectedAspectRatio).isAnyOf(
            AspectRatio.ONE_ONE,
            AspectRatio.THREE_FOUR
        )
    }
}
