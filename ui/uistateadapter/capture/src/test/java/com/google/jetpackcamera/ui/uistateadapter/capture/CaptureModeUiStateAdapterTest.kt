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
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.UNLIMITED_VIDEO_DURATION
import com.google.jetpackcamera.settings.api.OptionAvailabilityConfig
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
class CaptureModeUiStateAdapterTest {

    @Test
    fun from_notRestricted_enablesSupportedModes() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
            externalCaptureMode = ExternalCaptureMode.Standard
        )

        assertThat(uiState).isInstanceOf(CaptureModeUiState.Available::class.java)
        val available = uiState as CaptureModeUiState.Available
        assertThat(available.availableCaptureModes).isNotEmpty()
        assertThat(
            available.availableCaptureModes.all { it is SingleSelectableUiState.SelectableUi }
        ).isTrue()
    }

    @Test
    fun from_hidden_returnsUnavailable() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionAvailabilityConfig.Hidden,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
            externalCaptureMode = ExternalCaptureMode.Standard
        )

        assertThat(uiState).isEqualTo(CaptureModeUiState.Unavailable)
    }

    @Test
    fun from_optionsEnabled_enablesOnlySpecifiedModes() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionAvailabilityConfig.OptionsEnabled(
                setOf(CaptureMode.IMAGE_ONLY, CaptureMode.VIDEO_ONLY)
            ),
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            externalCaptureMode = ExternalCaptureMode.Standard
        )

        assertThat(uiState).isInstanceOf(CaptureModeUiState.Available::class.java)
        val available = uiState as CaptureModeUiState.Available

        val standardState = available.availableCaptureModes.find {
            it.value == CaptureMode.STANDARD
        }
        assertThat(standardState).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        assertThat((standardState as SingleSelectableUiState.Disabled).disabledReason)
            .isEqualTo(DisabledReason.HYBRID_CAPTURE_RESTRICTED)

        val imageState = available.availableCaptureModes.find { it.value == CaptureMode.IMAGE_ONLY }
        assertThat(imageState).isInstanceOf(SingleSelectableUiState.SelectableUi::class.java)

        val videoState = available.availableCaptureModes.find { it.value == CaptureMode.VIDEO_ONLY }
        assertThat(videoState).isInstanceOf(SingleSelectableUiState.SelectableUi::class.java)
    }

    @Test
    fun from_optionsEnabledExcludingImageMode_disablesImageModeWithRestrictedReason() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionAvailabilityConfig.OptionsEnabled(
                setOf(CaptureMode.STANDARD, CaptureMode.VIDEO_ONLY)
            ),
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.VIDEO_ONLY
            ),
            externalCaptureMode = ExternalCaptureMode.Standard
        )

        assertThat(uiState).isInstanceOf(CaptureModeUiState.Available::class.java)
        val available = uiState as CaptureModeUiState.Available

        val imageState = available.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.IMAGE_ONLY
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.IMAGE_ONLY
            }
        }
        assertThat(imageState).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        assertThat((imageState as SingleSelectableUiState.Disabled).disabledReason)
            .isEqualTo(DisabledReason.IMAGE_CAPTURE_RESTRICTED)
    }

    @Test
    fun from_optionsEnabledExcludingVideoMode_disablesVideoModeWithRestrictedReason() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionAvailabilityConfig.OptionsEnabled(
                setOf(CaptureMode.STANDARD, CaptureMode.IMAGE_ONLY)
            ),
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            externalCaptureMode = ExternalCaptureMode.Standard
        )

        assertThat(uiState).isInstanceOf(CaptureModeUiState.Available::class.java)
        val available = uiState as CaptureModeUiState.Available

        val videoState = available.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.VIDEO_ONLY
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.VIDEO_ONLY
            }
        }
        assertThat(videoState).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        assertThat((videoState as SingleSelectableUiState.Disabled).disabledReason)
            .isEqualTo(DisabledReason.VIDEO_CAPTURE_RESTRICTED)
    }

    // Toggle tests
    @Test
    fun toggleFrom_notRestricted_returnsAvailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.Standard,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted
        )

        assertThat(uiState).isInstanceOf(CaptureModeToggleUiState.Available::class.java)
        val available = uiState as CaptureModeToggleUiState.Available
        assertThat(available.selectedCaptureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
    }

    @Test
    fun toggleFrom_optionsEnabledWithImageAndVideo_returnsAvailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.Standard,
            restrictionConfig = OptionAvailabilityConfig.OptionsEnabled(
                setOf(CaptureMode.IMAGE_ONLY, CaptureMode.VIDEO_ONLY)
            )
        )
        assertThat(uiState).isInstanceOf(CaptureModeToggleUiState.Available::class.java)
    }

    @Test
    fun toggleFrom_videoRecordingActive_returnsUnavailable() {
        val activeState = CameraState(
            videoRecordingState = VideoRecordingState.Active.Recording(
                maxDurationMillis = UNLIMITED_VIDEO_DURATION,
                audioStreamState = AudioStreamState.Active(0.0),
                elapsedTimeNanos = 1000L
            )
        )
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            cameraState = activeState,
            externalCaptureMode = ExternalCaptureMode.Standard,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted
        )

        assertThat(uiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun toggleFrom_captureModeStandard_returnsUnavailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.STANDARD
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.Standard,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted
        )

        assertThat(uiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun toggleFrom_hiddenCaptureMode_returnsUnavailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.Standard,
            restrictionConfig = OptionAvailabilityConfig.Hidden
        )

        assertThat(uiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun toggleFrom_externalCaptureModeImageCapture_returnsUnavailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.ImageCapture,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted
        )

        assertThat(uiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun toggleFrom_externalCaptureModeMultipleImageCapture_returnsUnavailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.MultipleImageCapture,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted
        )

        assertThat(uiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun toggleFrom_externalCaptureModeVideoCapture_returnsUnavailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.VIDEO_ONLY
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.VideoCapture,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted
        )

        assertThat(uiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun toggleFrom_optionsEnabledExcludingVideo_returnsUnavailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.Standard,
            restrictionConfig = OptionAvailabilityConfig.OptionsEnabled(
                setOf(CaptureMode.STANDARD, CaptureMode.IMAGE_ONLY)
            )
        )

        assertThat(uiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun toggleFrom_optionsEnabledExcludingImage_returnsUnavailable() {
        val uiState = CaptureModeToggleUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.VIDEO_ONLY
            ),
            cameraState = CameraState(),
            externalCaptureMode = ExternalCaptureMode.Standard,
            restrictionConfig = OptionAvailabilityConfig.OptionsEnabled(
                setOf(CaptureMode.STANDARD, CaptureMode.VIDEO_ONLY)
            )
        )

        assertThat(uiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun from_externalCaptureModeImageCapture_disablesVideoAndHybrid() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            externalCaptureMode = ExternalCaptureMode.ImageCapture
        )

        assertThat(uiState).isInstanceOf(CaptureModeUiState.Available::class.java)
        val available = uiState as CaptureModeUiState.Available

        val imageMode = available.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.IMAGE_ONLY
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.IMAGE_ONLY
            }
        }
        assertThat(imageMode).isInstanceOf(SingleSelectableUiState.SelectableUi::class.java)

        val videoMode = available.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.VIDEO_ONLY
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.VIDEO_ONLY
            }
        }
        assertThat(videoMode).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        assertThat((videoMode as SingleSelectableUiState.Disabled).disabledReason)
            .isEqualTo(DisabledReason.VIDEO_CAPTURE_EXTERNAL_UNSUPPORTED)
    }

    @Test
    fun from_externalCaptureModeVideoCapture_disablesImageAndHybrid() {
        val uiState = CaptureModeUiState.from(
            systemConstraints = TYPICAL_SYSTEM_CONSTRAINTS,
            restrictionConfig = OptionAvailabilityConfig.NotRestricted,
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.VIDEO_ONLY
            ),
            externalCaptureMode = ExternalCaptureMode.VideoCapture
        )

        assertThat(uiState).isInstanceOf(CaptureModeUiState.Available::class.java)
        val available = uiState as CaptureModeUiState.Available

        val videoMode = available.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.VIDEO_ONLY
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.VIDEO_ONLY
            }
        }
        assertThat(videoMode).isInstanceOf(SingleSelectableUiState.SelectableUi::class.java)

        val imageMode = available.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.IMAGE_ONLY
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.IMAGE_ONLY
            }
        }
        assertThat(imageMode).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
        assertThat((imageMode as SingleSelectableUiState.Disabled).disabledReason)
            .isEqualTo(DisabledReason.IMAGE_CAPTURE_EXTERNAL_UNSUPPORTED)
    }
}
