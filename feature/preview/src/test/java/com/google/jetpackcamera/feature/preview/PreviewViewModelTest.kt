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
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.data.camera.CameraSystemRepository
import com.google.jetpackcamera.data.media.testing.FakeMediaRepository
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveMode
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.settings.api.DeveloperAppConfig
import com.google.jetpackcamera.settings.api.OptionRestrictionConfig
import com.google.jetpackcamera.settings.api.SettingConfig
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.testing.FakeSettingsRepository
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        override val cameraSystem = this@PreviewViewModelTest.cameraSystem
    }
    private val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
        updateSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
    }
    private val defaultTestAppConfig = DeveloperAppConfig(
        aspectRatio = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
        flashMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.flashMode),
        captureMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
        imageOutputFormat = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.imageFormat),
        videoDynamicRange = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.dynamicRange)
    )
    private lateinit var previewViewModel: PreviewViewModel

    @Before
    fun setup() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher())
        previewViewModel = PreviewViewModel(
            cameraSystemRepository = cameraSystemRepository,
            constraintsRepository = constraintsRepository,
            settingsRepository = FakeSettingsRepository(),
            mediaRepository = FakeMediaRepository(),
            savedStateHandle = SavedStateHandle(),
            defaultSaveMode = SaveMode.Immediate,
            appConfig = defaultTestAppConfig
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

    @Test
    fun toggleQuickSettings() = runTest(StandardTestDispatcher()) {
        startCameraUntilRunning()
        // Initial state should be closed
        assertIsReady(previewViewModel.captureUiState.value).also {
            val quickSettings = it.quickSettingsUiState as QuickSettingsUiState.Available
            assertThat(quickSettings.quickSettingsIsOpen).isFalse()
        }

        // Toggle to open
        previewViewModel.quickSettingsController.toggleQuickSettings()
        advanceUntilIdle()
        assertIsReady(previewViewModel.captureUiState.value).also {
            val quickSettings = it.quickSettingsUiState as QuickSettingsUiState.Available
            assertThat(quickSettings.quickSettingsIsOpen).isTrue()
        }

        // Toggle back to closed
        previewViewModel.quickSettingsController.toggleQuickSettings()
        advanceUntilIdle()
        assertIsReady(previewViewModel.captureUiState.value).also {
            val quickSettings = it.quickSettingsUiState as QuickSettingsUiState.Available
            assertThat(quickSettings.quickSettingsIsOpen).isFalse()
        }
    }

    @Test
    fun captureUiState_whenUseDeveloperConfigTrue_appliesRestrictions() = runTest(StandardTestDispatcher()) {
        val restrictedAppConfig = defaultTestAppConfig.copy(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.IMAGE_ONLY,
                uiRestriction = OptionRestrictionConfig.FullyRestricted()
            )
        )
        val viewModel = PreviewViewModel(
            cameraSystemRepository = cameraSystemRepository,
            constraintsRepository = constraintsRepository,
            settingsRepository = FakeSettingsRepository(),
            mediaRepository = FakeMediaRepository(),
            savedStateHandle = SavedStateHandle(mapOf(PreviewRoute.ARG_USE_DEVELOPER_CONFIG to true)),
            defaultSaveMode = SaveMode.Immediate,
            appConfig = restrictedAppConfig
        )
        advanceUntilIdle()
        viewModel.cameraController.startCamera()
        advanceUntilIdle()

        val uiState = viewModel.captureUiState.value
        assertThat(uiState).isInstanceOf(CaptureUiState.Ready::class.java)
        val readyState = uiState as CaptureUiState.Ready
        val quickSettings = readyState.quickSettingsUiState as QuickSettingsUiState.Available
        val captureModeState = quickSettings.captureModeUiState as CaptureModeUiState.Available
        val standardState = captureModeState.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.STANDARD
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.STANDARD
            }
        }
        assertThat(standardState).isInstanceOf(SingleSelectableUiState.Disabled::class.java)
    }

    @Test
    fun captureUiState_whenUseDeveloperConfigFalse_ignoresRestrictions() = runTest(StandardTestDispatcher()) {
        val restrictedAppConfig = defaultTestAppConfig.copy(
            captureMode = SettingConfig(
                defaultValue = CaptureMode.IMAGE_ONLY,
                uiRestriction = OptionRestrictionConfig.FullyRestricted()
            )
        )
        val viewModel = PreviewViewModel(
            cameraSystemRepository = cameraSystemRepository,
            constraintsRepository = constraintsRepository,
            settingsRepository = FakeSettingsRepository(),
            mediaRepository = FakeMediaRepository(),
            savedStateHandle = SavedStateHandle(mapOf(PreviewRoute.ARG_USE_DEVELOPER_CONFIG to false)),
            defaultSaveMode = SaveMode.Immediate,
            appConfig = restrictedAppConfig
        )
        advanceUntilIdle()
        viewModel.cameraController.startCamera()
        advanceUntilIdle()

        val uiState = viewModel.captureUiState.value
        assertThat(uiState).isInstanceOf(CaptureUiState.Ready::class.java)
        val readyState = uiState as CaptureUiState.Ready
        val quickSettings = readyState.quickSettingsUiState as QuickSettingsUiState.Available
        val captureModeState = quickSettings.captureModeUiState as CaptureModeUiState.Available
        val standardState = captureModeState.availableCaptureModes.find {
            when (it) {
                is SingleSelectableUiState.SelectableUi -> it.value == CaptureMode.STANDARD
                is SingleSelectableUiState.Disabled -> it.value == CaptureMode.STANDARD
            }
        }
        assertThat(standardState).isInstanceOf(SingleSelectableUiState.SelectableUi::class.java)
    }

    private fun TestScope.startCameraUntilRunning(viewModel: PreviewViewModel? = null) {
        (viewModel ?: previewViewModel).cameraController.startCamera()
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
