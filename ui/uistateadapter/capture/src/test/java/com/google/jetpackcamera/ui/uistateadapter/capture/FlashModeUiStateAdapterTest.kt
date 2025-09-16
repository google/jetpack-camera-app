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
package com.google.jetpackcamera.ui.uistateadapter.capture

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class FlashModeUiStateAdapterTest {

    private val emptyCameraConstraints = CameraConstraints(
        supportedStabilizationModes = emptySet(),
        supportedFixedFrameRates = emptySet(),
        supportedDynamicRanges = emptySet(),
        supportedVideoQualitiesMap = emptyMap(),
        supportedImageFormatsMap = emptyMap(),
        supportedIlluminants = emptySet(),
        supportedFlashModes = emptySet(),
        supportedZoomRange = null,
        unsupportedStabilizationFpsMap = emptyMap(),
        supportedTestPatterns = emptySet()
    )

    private val defaultCameraAppSettings = CameraAppSettings()
    private val defaultCameraState = CameraState()

    @Test
    fun from_noFlashModes_returnsUnavailable() {
        // Given a system with no available flash modes
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                defaultCameraAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF)
                )
            )
        )

        // When creating the UI state from these constraints
        val flashModeUiState = FlashModeUiState.from(defaultCameraAppSettings, systemConstraints)

        // Then the UI state is Unavailable
        assertThat(flashModeUiState).isInstanceOf(FlashModeUiState.Unavailable::class.java)
    }

    @Test
    fun updateFrom_sameAvailableFlashModes_noUpdate() {
        // Given an initial UI state
        val initialSystemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                defaultCameraAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.ON, FlashMode.AUTO)
                )
            )
        )
        val uiState = FlashModeUiState.from(defaultCameraAppSettings, initialSystemConstraints)

        // When updating with the same constraints
        val updatedUiState = uiState.updateFrom(
            defaultCameraAppSettings,
            initialSystemConstraints,
            defaultCameraState
        )

        // Then the state does not change, and the same object is returned
        assertThat(updatedUiState).isSameInstanceAs(uiState)
    }

    @Test
    fun updateFrom_differentAvailableFlashModes_updatesSelectableValues() {
        // Given an initial UI state
        val initialSystemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                defaultCameraAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.ON)
                )
            )
        )
        val uiState = FlashModeUiState.from(defaultCameraAppSettings, initialSystemConstraints)

        // When the available flash modes change
        val newSystemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                defaultCameraAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.ON, FlashMode.AUTO)
                )
            )
        )
        val updatedUiState = uiState.updateFrom(
            defaultCameraAppSettings,
            newSystemConstraints,
            defaultCameraState
        )

        // Then the selectable values are updated
        assertThat(updatedUiState).isInstanceOf(FlashModeUiState.Available::class.java)
        val availableUiState = updatedUiState as FlashModeUiState.Available
        assertThat(availableUiState.availableFlashModes.map { it.value }).containsExactly(
            FlashMode.OFF,
            FlashMode.ON,
            FlashMode.AUTO
        )
    }

    @Test
    fun updateFrom_unavailablePreviouslySelectedFlashMode_throwsException() {
        // Given an initial UI state with ON selected
        val initialAppSettings = defaultCameraAppSettings.copy(flashMode = FlashMode.ON)
        val initialSystemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                initialAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.ON)
                )
            )
        )
        val uiState = FlashModeUiState.from(initialAppSettings, initialSystemConstraints)

        // When the selected flash mode (ON) is no longer available
        val newSystemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                initialAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.AUTO)
                )
            )
        )

        // Then an IllegalStateException is thrown
        assertThrows(IllegalStateException::class.java) {
            uiState.updateFrom(
                initialAppSettings,
                newSystemConstraints,
                defaultCameraState
            )
        }
    }
}
