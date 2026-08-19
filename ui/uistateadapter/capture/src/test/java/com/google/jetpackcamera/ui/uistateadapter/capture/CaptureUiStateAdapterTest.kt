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
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.Illuminant
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.settings.api.DeveloperAppConfig
import com.google.jetpackcamera.settings.api.SettingConfig
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
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
class CaptureUiStateAdapterTest {

    private val defaultAppConfig = DeveloperAppConfig(
        aspectRatio = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
        flashMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.flashMode),
        captureMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
        imageOutputFormat = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.imageFormat),
        videoDynamicRange = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.dynamicRange)
    )

    @Test
    fun roundVideoRecordingState_nanoseconds_noRounding() {
        val state = VideoRecordingState.Active.Recording(0L, 0.0, 1234567890L)
        val rounded = roundVideoRecordingState(state, TimeUnit.NANOSECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1234567890L)
    }

    @Test
    fun roundVideoRecordingState_milliseconds_roundsToMillis() {
        val state = VideoRecordingState.Active.Recording(0L, 0.0, 1234567890L)
        val rounded = roundVideoRecordingState(state, TimeUnit.MILLISECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1234000000L)
    }

    @Test
    fun roundVideoRecordingState_seconds_roundsToSeconds() {
        val state = VideoRecordingState.Active.Recording(0L, 0.0, 1234567890L)
        val rounded = roundVideoRecordingState(state, TimeUnit.SECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1000000000L)
    }

    @Test
    fun roundVideoRecordingState_pausedState_roundsToSeconds() {
        val state = VideoRecordingState.Active.Paused(0L, 0.0, 1234567890L)
        val rounded = roundVideoRecordingState(state, TimeUnit.SECONDS)
        assertThat((rounded as VideoRecordingState.Active).elapsedTimeNanos).isEqualTo(1000000000L)
    }

    @Test
    fun captureUiState_initialEmitsReadyState() = runTest {
        val cameraSystem = FakeCameraSystem()
        val constraintsRepository = SettableConstraintsRepositoryImpl()
        constraintsRepository.updateSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
        val trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState())
        val externalCaptureMode = ExternalCaptureMode.Standard

        val uiStateFlow = captureUiState(
            cameraSystem = cameraSystem,
            appConfig = defaultAppConfig,
            constraintsRepository = constraintsRepository,
            trackedCaptureUiState = trackedCaptureUiState,
            externalCaptureMode = externalCaptureMode
        )

        val firstState = uiStateFlow.first()
        assertThat(firstState).isInstanceOf(CaptureUiState.Ready::class.java)
    }

    @Test
    fun captureUiState_aspectRatioUpdate_emitsUpdatedState() = runTest {
        val cameraSystem = FakeCameraSystem()
        val constraintsRepository = SettableConstraintsRepositoryImpl()
        constraintsRepository.updateSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
        val trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState())
        val externalCaptureMode = ExternalCaptureMode.Standard

        val uiStateFlow = captureUiState(
            cameraSystem = cameraSystem,
            appConfig = defaultAppConfig,
            constraintsRepository = constraintsRepository,
            trackedCaptureUiState = trackedCaptureUiState,
            externalCaptureMode = externalCaptureMode
        )

        val states = mutableListOf<CaptureUiState>()
        val job = launch {
            uiStateFlow.collect { states.add(it) }
        }

        runCurrent()
        assertThat(states).isNotEmpty()
        val initialState = states.last() as CaptureUiState.Ready
        assertThat(initialState.aspectRatioUiState is AspectRatioUiState.Available).isTrue()
        val initialRatio =
            (initialState.aspectRatioUiState as AspectRatioUiState.Available).selectedAspectRatio
        assertThat(initialRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)

        // Change aspect ratio in camera system
        cameraSystem.setAspectRatio(AspectRatio.THREE_FOUR)
        runCurrent()

        assertThat(states.size).isAtLeast(2)
        val updatedState = states.last() as CaptureUiState.Ready
        val updatedRatio = (updatedState.aspectRatioUiState as AspectRatioUiState.Available)
            .selectedAspectRatio
        assertThat(updatedRatio).isEqualTo(AspectRatio.THREE_FOUR)

        job.cancel()
    }

    @Test
    fun captureUiState_flashModeUpdate_emitsUpdatedState() = runTest {
        val cameraSystem = FakeCameraSystem()
        val constraintsRepository = SettableConstraintsRepositoryImpl()

        // We need constraints that support flash ON and OFF
        val systemConstraints = CameraSystemConstraints(
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
        constraintsRepository.updateSystemConstraints(systemConstraints)
        val trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState())
        val externalCaptureMode = ExternalCaptureMode.Standard

        val uiStateFlow = captureUiState(
            cameraSystem = cameraSystem,
            appConfig = defaultAppConfig,
            constraintsRepository = constraintsRepository,
            trackedCaptureUiState = trackedCaptureUiState,
            externalCaptureMode = externalCaptureMode
        )

        val states = mutableListOf<CaptureUiState>()
        val job = launch {
            uiStateFlow.collect { states.add(it) }
        }

        runCurrent()
        assertThat(states).isNotEmpty()
        val initialState = states.last() as CaptureUiState.Ready
        assertThat(initialState.flashModeUiState is FlashModeUiState.Available).isTrue()
        val initialFlash = (initialState.flashModeUiState as FlashModeUiState.Available)
            .selectedFlashMode
        assertThat(initialFlash).isEqualTo(FlashMode.OFF)

        // Change flash mode in camera system
        cameraSystem.setFlashMode(FlashMode.ON)
        runCurrent()

        assertThat(states.size).isAtLeast(2)
        val updatedState = states.last() as CaptureUiState.Ready
        val updatedFlash =
            (updatedState.flashModeUiState as FlashModeUiState.Available).selectedFlashMode
        assertThat(updatedFlash).isEqualTo(FlashMode.ON)

        job.cancel()
    }
}
