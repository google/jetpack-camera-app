/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.google.jetpackcamera.feature.preview

import android.content.ContentResolver
import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.data.camera.CameraSystemRepository
import com.google.jetpackcamera.data.media.testing.FakeMediaRepository
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveMode
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.testing.FakeSettingsRepository
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PreviewViewModelTest {

    private val cameraSystem = FakeCameraSystem()
    private val cameraSystemRepository = object : CameraSystemRepository {
        override val surfaceRequest = cameraSystem.getSurfaceRequest()
        override val systemConstraints = cameraSystem.getSystemConstraints()
        override val currentSettings = cameraSystem.getCurrentSettings()
        override val currentCameraState = cameraSystem.getCurrentCameraState()
        override val cameraPropertiesJSON: StateFlow<String?> = MutableStateFlow(null)

        override suspend fun getCameraSystem(): CameraSystem {
            cameraSystem.initialize(CameraAppSettings()) {}
            return cameraSystem
        }

        override suspend fun getSupportedMimeTypes(): List<String> = emptyList()
    }
    private val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
        updateSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
    }
    private val settingsRepository = FakeSettingsRepository()
    private lateinit var previewViewModel: PreviewViewModel

    @Before
    fun setup() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher())
        previewViewModel = PreviewViewModel(
            cameraSystemRepository = cameraSystemRepository,
            constraintsRepository = constraintsRepository,
            settingsRepository = settingsRepository,
            mediaRepository = FakeMediaRepository(),
            savedStateHandle = SavedStateHandle(),
            defaultSaveMode = SaveMode.Immediate
        )
        advanceUntilIdle()
    }

    @Test
    fun getPreviewUiState() = runTest(StandardTestDispatcher()) {
        startCameraUntilRunning()
        val uiState = previewViewModel.captureUiState.value
        assertThat(uiState).isInstanceOf(CaptureUiState.Ready::class.java)
    }

    @Test
    fun runCamera() = runTest(StandardTestDispatcher()) {
        startCameraUntilRunning()

        assertThat(cameraSystem.previewStarted).isTrue()
    }

    @Test
    fun captureImageWithUri() = runTest(StandardTestDispatcher()) {
        val contentResolver: ContentResolver =
            ApplicationProvider.getApplicationContext<Context>().contentResolver
        startCameraUntilRunning()
        previewViewModel.captureController.captureImage(contentResolver)
        advanceUntilIdle()
        assertThat(cameraSystem.numPicturesTaken).isEqualTo(1)
    }

    @Test
    fun startVideoRecording() = runTest(StandardTestDispatcher()) {
        startCameraUntilRunning()
        previewViewModel.captureController.startVideoRecording()
        advanceUntilIdle()
        assertThat(cameraSystem.recordingInProgress).isTrue()
    }

    @Test
    fun stopVideoRecording() = runTest(StandardTestDispatcher()) {
        startCameraUntilRunning()
        previewViewModel.captureController.startVideoRecording()
        advanceUntilIdle()
        previewViewModel.captureController.stopVideoRecording()
        advanceUntilIdle()
        assertThat(cameraSystem.recordingInProgress).isFalse()
    }

    @Test
    fun fastStartAndStopVideoRecording_cancelsRecording() = runTest(StandardTestDispatcher()) {
        startCameraUntilRunning()

        // Start and stop immediately without advancing the dispatcher
        previewViewModel.captureController.startVideoRecording()
        previewViewModel.captureController.stopVideoRecording()

        // Let the coroutines execute
        advanceUntilIdle()

        // Verify that because we cancelled the job synchronously, the start implementation
        // was bypassed completely to protect us from the channel race condition!
        assertThat(cameraSystem.numVideoRecordingStarts).isEqualTo(0)
    }

    @Test
    fun setFlash() = runTest(StandardTestDispatcher()) {
        previewViewModel.cameraController.startCamera()
        previewViewModel.quickSettingsController.setFlash(FlashMode.AUTO)
        advanceUntilIdle()

        assertIsReady(previewViewModel.captureUiState.value).also {
            assertThat(it.flashModeUiState is FlashModeUiState.Available).isTrue()
            assertThat(
                (it.flashModeUiState as FlashModeUiState.Available)
                    .selectedFlashMode
            ).isEqualTo(FlashMode.AUTO)
        }
    }

    @Test
    fun flipCamera() = runTest(StandardTestDispatcher()) {
        // initial default value should be back
        startCameraUntilRunning()
        assertIsReady(previewViewModel.captureUiState.value).also {
            assertThat(it.flipLensUiState is FlipLensUiState.Available).isTrue()
            assertThat(
                (it.flipLensUiState as FlipLensUiState.Available)
                    .selectedLensFacing
            ).isEqualTo(LensFacing.BACK)
        }
        previewViewModel.quickSettingsController.setLensFacing(LensFacing.FRONT)

        advanceUntilIdle()
        // ui state and camera should both be true now
        assertIsReady(previewViewModel.captureUiState.value).also {
            assertThat(it.flipLensUiState is FlipLensUiState.Available).isTrue()
            assertThat(
                (it.flipLensUiState as FlipLensUiState.Available)
                    .selectedLensFacing
            ).isEqualTo(LensFacing.FRONT)
        }
        assertThat(cameraSystem.isLensFacingFront).isTrue()
    }

    /**
     * Verifies that when [DynamicRange] is updated in [SettingsRepository], the change propagates
     * through [CameraAppSettings.applyDiffs] to the [CameraSystem].
     *
     * CaptureMode is switched to [CaptureMode.VIDEO_ONLY] since HDR video is not supported in
     * standard capture mode.
     */
    @Test
    fun updateDynamicRange_propagatesToCameraSystem() = runTest(StandardTestDispatcher()) {
        enableHdrAndUltraHdrConstraints()
        startCameraUntilRunning()
        previewViewModel.quickSettingsController.setCaptureMode(CaptureMode.VIDEO_ONLY)
        advanceUntilIdle()
        settingsRepository.updateDynamicRange(DynamicRange.HLG10)
        advanceUntilIdle()
        assertThat(cameraSystem.getCurrentSettings().first()?.dynamicRange)
            .isEqualTo(DynamicRange.HLG10)
    }

    /**
     * Verifies that when [ImageOutputFormat] is updated in [SettingsRepository], the change
     * propagates through [CameraAppSettings.applyDiffs] to the [CameraSystem].
     *
     * CaptureMode is switched to [CaptureMode.IMAGE_ONLY] since Ultra HDR is not supported in
     * standard capture mode.
     */
    @Test
    fun updateImageFormat_propagatesToCameraSystem() = runTest(StandardTestDispatcher()) {
        enableHdrAndUltraHdrConstraints()
        startCameraUntilRunning()
        previewViewModel.quickSettingsController.setCaptureMode(CaptureMode.IMAGE_ONLY)
        advanceUntilIdle()
        settingsRepository.updateImageFormat(ImageOutputFormat.JPEG_ULTRA_HDR)
        advanceUntilIdle()
        assertThat(cameraSystem.getCurrentSettings().first()?.imageFormat)
            .isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
    }

    private fun enableHdrAndUltraHdrConstraints() {
        constraintsRepository.updateSystemConstraints(
            TYPICAL_SYSTEM_CONSTRAINTS.copy(
                perLensConstraints = TYPICAL_SYSTEM_CONSTRAINTS.perLensConstraints
                    .mapValues { (_, constraints) ->
                        constraints.copy(
                            supportedDynamicRanges = setOf(DynamicRange.SDR, DynamicRange.HLG10),
                            supportedImageFormatsMap = mapOf(
                                false to setOf(
                                    ImageOutputFormat.JPEG,
                                    ImageOutputFormat.JPEG_ULTRA_HDR
                                ),
                                true to setOf(
                                    ImageOutputFormat.JPEG,
                                    ImageOutputFormat.JPEG_ULTRA_HDR
                                )
                            )
                        )
                    }
            )
        )
    }

    private fun TestScope.startCameraUntilRunning() {
        previewViewModel.cameraController.startCamera()
        advanceUntilIdle()
    }
}

private fun assertIsReady(viewFinderUiState: CaptureUiState): CaptureUiState.Ready =
    when (viewFinderUiState) {
        is CaptureUiState.Ready -> viewFinderUiState
        else -> throw AssertionError(
            "PreviewUiState expected to be Ready, but was ${viewFinderUiState::class}"
        )
    }
