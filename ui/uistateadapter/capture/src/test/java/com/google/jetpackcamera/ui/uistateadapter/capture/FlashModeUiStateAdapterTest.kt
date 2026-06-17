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
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.LowLightBoostState
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
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

    @Test
    fun updateFrom_lowLightBoostActive_updatesStrength() {
        // Given an initial UI state with LOW_LIGHT_BOOST selected
        val initialAppSettings = defaultCameraAppSettings.copy(
            flashMode = FlashMode.LOW_LIGHT_BOOST
        )
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                initialAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.LOW_LIGHT_BOOST)
                )
            )
        )
        val uiState = FlashModeUiState.from(initialAppSettings, systemConstraints)
        assertThat(uiState).isInstanceOf(FlashModeUiState.Available::class.java)

        // When the camera state reports active low light boost
        val cameraState = defaultCameraState.copy(
            lowLightBoostState = LowLightBoostState.Active(strength = 0.7f)
        )
        val updatedUiState = uiState.updateFrom(
            initialAppSettings,
            systemConstraints,
            cameraState
        )

        // Then isLowLightBoostActive is updated in the UI state
        assertThat(updatedUiState).isInstanceOf(FlashModeUiState.Available::class.java)
        val availableUiState = updatedUiState as FlashModeUiState.Available
        assertThat(availableUiState.isLowLightBoostActive).isEqualTo(true)
    }

    @Test
    fun updateFrom_lowLightBoostInactive_resetsStrength() {
        // Given an initial UI state with active low light boost strength
        val initialAppSettings = defaultCameraAppSettings.copy(
            flashMode = FlashMode.LOW_LIGHT_BOOST
        )
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                initialAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.LOW_LIGHT_BOOST)
                )
            )
        )
        val initialCameraState = defaultCameraState.copy(
            lowLightBoostState = LowLightBoostState.Active(strength = 0.7f)
        )
        var uiState = FlashModeUiState.from(initialAppSettings, systemConstraints)
        uiState = uiState.updateFrom(initialAppSettings, systemConstraints, initialCameraState)
        assertThat((uiState as FlashModeUiState.Available).isLowLightBoostActive).isEqualTo(true)

        // When the camera state reports inactive low light boost
        val newCameraState = defaultCameraState.copy(
            lowLightBoostState = LowLightBoostState.Inactive
        )
        val updatedUiState = uiState.updateFrom(
            initialAppSettings,
            systemConstraints,
            newCameraState
        )

        // Then isLowLightBoostActive is reset to false
        assertThat(updatedUiState).isInstanceOf(FlashModeUiState.Available::class.java)
        val availableUiState = updatedUiState as FlashModeUiState.Available
        assertThat(availableUiState.isLowLightBoostActive).isEqualTo(false)
    }

    @Test
    fun updateFrom_lowLightBoostError_resetsStrength() {
        // Given an initial UI state with active low light boost strength
        val initialAppSettings = defaultCameraAppSettings.copy(
            flashMode = FlashMode.LOW_LIGHT_BOOST
        )
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                initialAppSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.LOW_LIGHT_BOOST)
                )
            )
        )
        val initialCameraState = defaultCameraState.copy(
            lowLightBoostState = LowLightBoostState.Active(strength = 0.7f)
        )
        var uiState = FlashModeUiState.from(initialAppSettings, systemConstraints)
        uiState = uiState.updateFrom(initialAppSettings, systemConstraints, initialCameraState)
        assertThat((uiState as FlashModeUiState.Available).isLowLightBoostActive).isEqualTo(true)

        // When the camera state reports a low light boost error
        val newCameraState = defaultCameraState.copy(
            lowLightBoostState = LowLightBoostState.Error(Throwable())
        )
        val updatedUiState = uiState.updateFrom(
            initialAppSettings,
            systemConstraints,
            newCameraState
        )

        // Then isLowLightBoostActive is reset to false
        assertThat(updatedUiState).isInstanceOf(FlashModeUiState.Available::class.java)
        val availableUiState = updatedUiState as FlashModeUiState.Available
        assertThat(availableUiState.isLowLightBoostActive).isEqualTo(false)
    }

    @Test
    fun from_hdrImageOn_lowLightBoostDisabled() {
        // Given a system supporting LLB, but HDR image is ON
        val appSettings = defaultCameraAppSettings.copy(flashMode = FlashMode.OFF)
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.LOW_LIGHT_BOOST)
                )
            )
        )
        val hdrUiState = HdrUiState.Available(
            selectedImageFormat = ImageOutputFormat.JPEG_ULTRA_HDR,
            selectedDynamicRange = DynamicRange.SDR
        )

        // When
        val flashModeUiState = FlashModeUiState.from(appSettings, systemConstraints, hdrUiState)

        // Then LLB is disabled because of HDR
        assertThat(flashModeUiState).isInstanceOf(FlashModeUiState.Available::class.java)
        val availableUiState = flashModeUiState as FlashModeUiState.Available
        val llbState = availableUiState.availableFlashModes.find {
            it.value == FlashMode.LOW_LIGHT_BOOST
        }
        assertThat(llbState).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        val disabledLlb = llbState as SingleSelectableUiState.Disabled
        assertThat(disabledLlb.disabledReason).isEqualTo(DisabledReason.LLB_DISABLED_BY_HDR)
    }

    @Test
    fun from_hdrVideoOn_lowLightBoostDisabled() {
        // Given a system supporting LLB, but HDR video is ON
        val appSettings = defaultCameraAppSettings.copy(flashMode = FlashMode.OFF)
        val systemConstraints = CameraSystemConstraints(
            perLensConstraints = mapOf(
                appSettings.cameraLensFacing to emptyCameraConstraints.copy(
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.LOW_LIGHT_BOOST)
                )
            )
        )
        val hdrUiState = HdrUiState.Available(
            selectedImageFormat = ImageOutputFormat.JPEG,
            selectedDynamicRange = DynamicRange.HLG10
        )

        // When
        val flashModeUiState = FlashModeUiState.from(appSettings, systemConstraints, hdrUiState)

        // Then LLB is disabled because of HDR
        assertThat(flashModeUiState).isInstanceOf(FlashModeUiState.Available::class.java)
        val availableUiState = flashModeUiState as FlashModeUiState.Available
        val llbState =
            availableUiState.availableFlashModes.find { it.value == FlashMode.LOW_LIGHT_BOOST }
        assertThat(llbState).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        val disabledLlb = llbState as SingleSelectableUiState.Disabled
        assertThat(disabledLlb.disabledReason).isEqualTo(DisabledReason.LLB_DISABLED_BY_HDR)
    }

    @Test
    fun from_flashUnsupportedOnCurrentLens_flashModeDisabled() {
        // Given a device that supports FLASH_ON on some lens, but NOT on the lens
        val appSettings = defaultCameraAppSettings.copy(
            cameraLensFacing = LensFacing.BACK,
            flashMode = FlashMode.OFF
        )
        val systemConstraints = CameraSystemConstraints(
            availableLenses = listOf(LensFacing.BACK, LensFacing.FRONT),
            perLensConstraints = mapOf(
                LensFacing.BACK to emptyCameraConstraints.copy(
                    // Current lens doesn't support ON
                    supportedFlashModes = setOf(FlashMode.OFF)
                ),
                LensFacing.FRONT to emptyCameraConstraints.copy(
                    // Other lens supports ON
                    supportedFlashModes = setOf(FlashMode.OFF, FlashMode.ON)
                )
            )
        )

        // When
        val flashModeUiState = FlashModeUiState.from(appSettings, systemConstraints)

        // Then FLASH_ON is disabled because it is unsupported on the current lens
        assertThat(flashModeUiState).isInstanceOf(FlashModeUiState.Available::class.java)
        val availableUiState = flashModeUiState as FlashModeUiState.Available
        val flashOnState = availableUiState.availableFlashModes.find { it.value == FlashMode.ON }
        assertThat(flashOnState).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        val disabledFlashOn = flashOnState as SingleSelectableUiState.Disabled
        assertThat(disabledFlashOn.disabledReason)
            .isEqualTo(DisabledReason.FLASH_UNSUPPORTED_ON_LENS)
    }
}
