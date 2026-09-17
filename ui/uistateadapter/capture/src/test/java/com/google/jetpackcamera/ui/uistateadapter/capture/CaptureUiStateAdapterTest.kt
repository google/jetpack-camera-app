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
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.Illuminant
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.HdrUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.compound.captureUiState
import com.google.jetpackcamera.ui.uistateadapter.capture.compound.roundVideoRecordingState
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
internal class CaptureUiStateAdapterTest {

    private val cameraSystem = FakeCameraSystem()
    private val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
        updateSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
    }
    private val trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState())
    private val externalCaptureMode = ExternalCaptureMode.Standard

    private val defaultPolicy = CameraFeaturePolicy(
        aspectRatio = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
        flashMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.flashMode),
        captureMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
        imageFormat = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.imageFormat),
        dynamicRange = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.dynamicRange)
    )

    private fun createCaptureUiStateFlow(cameraFeaturePolicy: CameraFeaturePolicy? = defaultPolicy) =
        captureUiState(
            currentSettings = cameraSystem.getCurrentSettings(),
            cameraFeaturePolicy = cameraFeaturePolicy,
            systemConstraints = constraintsRepository.systemConstraints,
            currentCameraState = cameraSystem.getCurrentCameraState(),
            trackedCaptureUiState = trackedCaptureUiState,
            externalCaptureMode = externalCaptureMode
        )

    @Test
    fun roundVideoRecordingState_nanoseconds_noRounding() {
        val state = VideoRecordingState.Active.Recording(
            0L,
            AudioStreamState.Active(0.0),
            1234567890L
        )
        val rounded = roundVideoRecordingState(state, TimeUnit.NANOSECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1234567890L)
    }

    @Test
    fun roundVideoRecordingState_milliseconds_roundsToMillis() {
        val state = VideoRecordingState.Active.Recording(
            0L,
            AudioStreamState.Active(0.0),
            1234567890L
        )
        val rounded = roundVideoRecordingState(state, TimeUnit.MILLISECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1234000000L)
    }

    @Test
    fun roundVideoRecordingState_seconds_roundsToSeconds() {
        val state = VideoRecordingState.Active.Recording(
            0L,
            AudioStreamState.Active(0.0),
            1234567890L
        )
        val rounded = roundVideoRecordingState(state, TimeUnit.SECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1000000000L)
    }

    @Test
    fun roundVideoRecordingState_pausedState_roundsToSeconds() {
        val state = VideoRecordingState.Active.Paused(0L, AudioStreamState.Active(0.0), 1234567890L)
        val rounded = roundVideoRecordingState(state, TimeUnit.SECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1000000000L)
    }

    @Test
    fun captureUiState_initialEmitsReadyState() = runTest {
        val uiStateFlow = createCaptureUiStateFlow()

        val firstState = uiStateFlow.first()
        assertThat(firstState).isInstanceOf(CaptureUiState.Ready::class.java)
    }

    @Test
    fun captureUiState_aspectRatioUpdate_emitsUpdatedState() = runTest {
        val uiStateFlow = createCaptureUiStateFlow()
        val states = mutableListOf<CaptureUiState>()
        backgroundScope.launch {
            uiStateFlow.collect { states.add(it) }
        }

        runCurrent()
        val initialState = assertIsReady(states.last())
        assertThat(
            initialState.aspectRatioUiState
        ).isInstanceOf(AspectRatioUiState.Available::class.java)
        val initialRatio =
            (initialState.aspectRatioUiState as AspectRatioUiState.Available).selectedAspectRatio
        assertThat(initialRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)

        // Change aspect ratio in camera system
        cameraSystem.setAspectRatio(AspectRatio.THREE_FOUR)
        runCurrent()

        val updatedState = assertIsReady(states.last())
        assertThat(
            updatedState.aspectRatioUiState
        ).isInstanceOf(AspectRatioUiState.Available::class.java)
        val updatedRatio =
            (updatedState.aspectRatioUiState as AspectRatioUiState.Available).selectedAspectRatio
        assertThat(updatedRatio).isEqualTo(AspectRatio.THREE_FOUR)
    }

    @Test
    fun captureUiState_flashModeUpdate_emitsUpdatedState() = runTest {
        constraintsRepository.updateSystemConstraints(
            CameraSystemConstraints(
                availableLenses = listOf(LensFacing.BACK),
                perLensConstraints = mapOf(
                    LensFacing.BACK to CameraConstraints(
                        supportedFixedFrameRates = emptySet(),
                        supportedStabilizationModes = emptySet(),
                        supportedDynamicRanges = emptySet(),
                        supportedVideoQualitiesMap = emptyMap(),
                        supportedImageFormatsMap = emptyMap(),
                        supportedIlluminants = setOf(Illuminant.FLASH_UNIT),
                        supportedFlashModes = setOf(FlashMode.OFF, FlashMode.ON),
                        supportedZoomRange = null,
                        unsupportedStabilizationFpsMap = emptyMap(),
                        supportedTestPatterns = emptySet()
                    )
                )
            )
        )

        val uiStateFlow = createCaptureUiStateFlow()
        val states = mutableListOf<CaptureUiState>()
        backgroundScope.launch {
            uiStateFlow.collect { states.add(it) }
        }

        runCurrent()
        val initialState = assertIsReady(states.last())
        assertThat(
            initialState.flashModeUiState
        ).isInstanceOf(FlashModeUiState.Available::class.java)
        val initialFlash =
            (initialState.flashModeUiState as FlashModeUiState.Available).selectedFlashMode
        assertThat(initialFlash).isEqualTo(FlashMode.OFF)

        // Change flash mode in camera system
        cameraSystem.setFlashMode(FlashMode.ON)
        runCurrent()

        val updatedState = assertIsReady(states.last())
        assertThat(
            updatedState.flashModeUiState
        ).isInstanceOf(FlashModeUiState.Available::class.java)
        val updatedFlash =
            (updatedState.flashModeUiState as FlashModeUiState.Available).selectedFlashMode
        assertThat(updatedFlash).isEqualTo(FlashMode.ON)
    }

    @Test
    fun captureUiState_withRestrictedPolicy_emitsRestrictedUiStates() = runTest {
        val restrictedConfig = defaultPolicy.copy(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.IMAGE_ONLY,
                visibility = OptionVisibility.Hidden
            )
        )
        val uiStateFlow = createCaptureUiStateFlow(cameraFeaturePolicy = restrictedConfig)
        val state = assertIsReady(uiStateFlow.first())
        assertThat(
            state.quickSettingsUiState
        ).isInstanceOf(QuickSettingsUiState.Available::class.java)
        val quickSettings = state.quickSettingsUiState as QuickSettingsUiState.Available
        assertThat(quickSettings.captureModeUiState).isEqualTo(CaptureModeUiState.Unavailable)
        assertThat(state.captureModeToggleUiState).isEqualTo(CaptureModeToggleUiState.Unavailable)
    }

    @Test
    fun captureUiState_withNullPolicy_defaultsCleanly() = runTest {
        val uiStateFlow = createCaptureUiStateFlow(cameraFeaturePolicy = null)
        val state = assertIsReady(uiStateFlow.first())
        assertThat(state).isInstanceOf(CaptureUiState.Ready::class.java)
    }

    @Test
    fun captureUiState_withFlashModeAndHdrHidden_emitsUnavailableUiStates() = runTest {
        constraintsRepository.updateSystemConstraints(
            CameraSystemConstraints(
                availableLenses = listOf(LensFacing.BACK),
                perLensConstraints = mapOf(
                    LensFacing.BACK to CameraConstraints(
                        supportedFixedFrameRates = emptySet(),
                        supportedStabilizationModes = emptySet(),
                        supportedDynamicRanges = emptySet(),
                        supportedVideoQualitiesMap = emptyMap(),
                        supportedImageFormatsMap = emptyMap(),
                        supportedIlluminants = setOf(Illuminant.FLASH_UNIT),
                        supportedFlashModes = setOf(FlashMode.OFF, FlashMode.ON),
                        supportedZoomRange = null,
                        unsupportedStabilizationFpsMap = emptyMap(),
                        supportedTestPatterns = emptySet()
                    )
                )
            )
        )

        val restrictedConfig = defaultPolicy.copy(
            flashMode = SettingConfig(
                defaultValue = FlashMode.OFF,
                visibility = OptionVisibility.Hidden
            ),
            imageFormat = SettingConfig(
                defaultValue = ImageOutputFormat.JPEG,
                visibility = OptionVisibility.Hidden
            ),
            dynamicRange = SettingConfig(
                defaultValue = DynamicRange.SDR,
                visibility = OptionVisibility.Hidden
            )
        )

        val uiStateFlow = createCaptureUiStateFlow(cameraFeaturePolicy = restrictedConfig)
        val state = assertIsReady(uiStateFlow.first())
        assertThat(state.flashModeUiState).isEqualTo(FlashModeUiState.Unavailable)
        val quickSettings = state.quickSettingsUiState as QuickSettingsUiState.Available
        assertThat(quickSettings.flashModeUiState).isEqualTo(FlashModeUiState.Unavailable)
        assertThat(quickSettings.hdrUiState).isEqualTo(HdrUiState.Unavailable)
    }

    @Test
    fun captureUiState_withAspectRatioHidden_emitsUnavailableQuickSettingsAndPreservesPreviewAspectRatio() = runTest {
        val restrictedConfig = defaultPolicy.copy(
            aspectRatio = SettingConfig(
                defaultValue = AspectRatio.ONE_ONE,
                visibility = OptionVisibility.Hidden
            )
        )

        val uiStateFlow = createCaptureUiStateFlow(cameraFeaturePolicy = restrictedConfig)
        val state = assertIsReady(uiStateFlow.first())
        assertThat(state.aspectRatioUiState).isEqualTo(AspectRatioUiState.Unavailable)
        val quickSettings = state.quickSettingsUiState as QuickSettingsUiState.Available
        assertThat(quickSettings.aspectRatioUiState).isEqualTo(AspectRatioUiState.Unavailable)
        assertThat(state.previewDisplayUiState.aspectRatioUiState).isEqualTo(
            AspectRatioUiState.Available(
                selectedAspectRatio = AspectRatio.NINE_SIXTEEN,
                availableAspectRatios = listOf(
                    SingleSelectableUiState.SelectableUi(AspectRatio.NINE_SIXTEEN)
                )
            )
        )
    }

    private fun assertIsReady(uiState: CaptureUiState): CaptureUiState.Ready = when (uiState) {
        is CaptureUiState.Ready -> uiState
        else -> throw AssertionError(
            "CaptureUiState expected to be Ready, but was ${uiState::class}"
        )
    }
}
