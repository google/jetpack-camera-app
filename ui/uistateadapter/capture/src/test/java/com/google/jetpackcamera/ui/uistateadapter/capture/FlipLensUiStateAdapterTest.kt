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
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FlipLensUiStateAdapterTest {

    @Test
    fun from_multipleLensesAvailable_returnsAvailableWithCorrectSelection() {
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(cameraLensFacing = LensFacing.BACK)
        val systemConstraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.FRONT, LensFacing.BACK)
        )

        val state = FlipLensUiState.from(settings, systemConstraints)

        assertThat(state).isInstanceOf(FlipLensUiState.Available::class.java)
        val available = state as FlipLensUiState.Available
        assertThat(available.selectedLensFacing).isEqualTo(LensFacing.BACK)
        val lenses = available.availableLensFacings.map { it.value }
        assertThat(lenses).containsExactly(
            LensFacing.FRONT,
            LensFacing.BACK
        ).inOrder()
    }

    @Test
    fun from_singleLensAvailable_returnsUnavailable() {
        val settings = DEFAULT_CAMERA_APP_SETTINGS.copy(cameraLensFacing = LensFacing.BACK)
        val systemConstraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK)
        )

        val state = FlipLensUiState.from(settings, systemConstraints)

        assertThat(state).isEqualTo(FlipLensUiState.Unavailable)
    }

    @Test
    fun from_emptyLensesAvailable_returnsUnavailable() {
        val settings = DEFAULT_CAMERA_APP_SETTINGS
        val systemConstraints = CameraSystemConstraints(
            availableLenses = emptyList()
        )

        val state = FlipLensUiState.from(settings, systemConstraints)

        assertThat(state).isEqualTo(FlipLensUiState.Unavailable)
    }
}
