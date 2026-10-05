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
import android.location.Location
import androidx.lifecycle.SavedStateHandle
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.CameraState
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.core.location.LocationProvider
import com.google.jetpackcamera.core.location.testing.FakeLocationProvider
import com.google.jetpackcamera.data.camera.CameraSystemRepository
import com.google.jetpackcamera.data.media.testing.FakeMediaRepository
import com.google.jetpackcamera.feature.preview.navigation.PreviewRoute
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.model.applyExternalCaptureMode
import com.google.jetpackcamera.settings.testing.FakeSettingsRepository
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlashModeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.CaptureUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.QuickSettingsUiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreviewViewModelTest {

    private fun createFakeCameraSystemRepository(
        cameraSystem: FakeCameraSystem,
        externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard
    ) = object : CameraSystemRepository {
        override val surfaceRequest = cameraSystem.getSurfaceRequest()
        override val systemConstraints = cameraSystem.getSystemConstraints()
        override val currentSettings = cameraSystem.getCurrentSettings()
        override val currentCameraState = cameraSystem.getCurrentCameraState()
        override val cameraPropertiesJSON: StateFlow<String?> = MutableStateFlow(null)

        override suspend fun getCameraSystem(): CameraSystem {
            cameraSystem.initialize(
                CameraAppSettings().applyExternalCaptureMode(externalCaptureMode)
            ) {}
            return cameraSystem
        }
        override suspend fun getInitialDefaultCameraAppSettings(): CameraAppSettings =
            CameraAppSettings()
        override suspend fun getSupportedMimeTypes(): List<String> = emptyList()
    }

    private val cameraSystem = FakeCameraSystem().apply {
        setSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
    }
    private val cameraSystemRepository = createFakeCameraSystemRepository(cameraSystem)
    private val defaultTestPolicy = CameraFeaturePolicy(
        aspectRatio = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.aspectRatio),
        flashMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.flashMode),
        captureMode = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.captureMode),
        imageFormat = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.imageFormat),
        dynamicRange = SettingConfig(DEFAULT_CAMERA_APP_SETTINGS.dynamicRange)
    )
    private lateinit var previewViewModel: PreviewViewModel

    @Before
    fun setup() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher())
        previewViewModel = PreviewViewModel(
            cameraSystemRepository = cameraSystemRepository,
            settingsRepository = FakeSettingsRepository(),
            mediaRepository = FakeMediaRepository(),
            locationProvider = java.util.Optional.of(FakeLocationProvider()),
            savedStateHandle = SavedStateHandle(),
            defaultSaveMode = SaveMode.Immediate,
            cameraFeaturePolicy = defaultTestPolicy
        )
        advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
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

    @Test
    fun captureUiState_withRestrictedPolicy_appliesRestrictions() =
        runTest(StandardTestDispatcher()) {
            val restrictedPolicy = defaultTestPolicy.copy(
                captureMode = SettingConfig(
                    defaultValue = CaptureMode.IMAGE_ONLY,
                    visibility = OptionVisibility.Hidden
                )
            )
            val viewModel = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.empty(),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate,
                cameraFeaturePolicy = restrictedPolicy
            )
            advanceUntilIdle()
            startCameraUntilRunning(viewModel)

            val uiState = viewModel.captureUiState.value
            assertThat(uiState).isInstanceOf(CaptureUiState.Ready::class.java)
            val readyState = uiState as CaptureUiState.Ready
            val quickSettings = readyState.quickSettingsUiState as QuickSettingsUiState.Available
            assertThat(
                quickSettings.captureModeUiState
            ).isInstanceOf(CaptureModeUiState.Unavailable::class.java)
            assertThat(readyState.captureModeToggleUiState)
                .isEqualTo(CaptureModeToggleUiState.Unavailable)
        }

    @Test
    fun captureUiState_withDefaultPolicy_doesNotApplyRestrictions() =
        runTest(StandardTestDispatcher()) {
            val viewModel = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.empty(),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate,
                cameraFeaturePolicy = defaultTestPolicy
            )
            advanceUntilIdle()
            startCameraUntilRunning(viewModel)

            val uiState = viewModel.captureUiState.value
            assertThat(uiState).isInstanceOf(CaptureUiState.Ready::class.java)
            val readyState = uiState as CaptureUiState.Ready
            val quickSettings = readyState.quickSettingsUiState as QuickSettingsUiState.Available
            val captureModeState = quickSettings.captureModeUiState as CaptureModeUiState.Available
            val standardState = captureModeState.availableCaptureModes.find {
                it.value == CaptureMode.STANDARD
            }
            assertThat(standardState).isInstanceOf(SingleSelectableUiState.SelectableUi::class.java)
        }

    @Test
    fun captureUiState_whenExternalCaptureModeImageCapture_captureModeToggleIsUnavailable() =
        runTest(StandardTestDispatcher()) {
            val testCameraSystem = FakeCameraSystem().apply {
                setSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
            }
            val viewModel = PreviewViewModel(
                cameraSystemRepository = createFakeCameraSystemRepository(
                    testCameraSystem,
                    ExternalCaptureMode.ImageCapture
                ),
                settingsRepository = FakeSettingsRepository(),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.empty(),
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        PreviewRoute.ARG_EXTERNAL_CAPTURE_MODE to ExternalCaptureMode.ImageCapture
                    )
                ),
                defaultSaveMode = SaveMode.Immediate,
                cameraFeaturePolicy = defaultTestPolicy
            )
            advanceUntilIdle()
            startCameraUntilRunning(viewModel)

            val uiState = viewModel.captureUiState.value
            assertThat(uiState).isInstanceOf(CaptureUiState.Ready::class.java)
            val readyState = uiState as CaptureUiState.Ready
            assertThat(readyState.captureModeToggleUiState)
                .isEqualTo(CaptureModeToggleUiState.Unavailable)
        }

    @Test
    fun captureUiState_whenExternalCaptureModeVideoCapture_captureModeToggleIsUnavailable() =
        runTest(StandardTestDispatcher()) {
            val testCameraSystem = FakeCameraSystem().apply {
                setSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
            }
            val viewModel = PreviewViewModel(
                cameraSystemRepository = createFakeCameraSystemRepository(
                    testCameraSystem,
                    ExternalCaptureMode.VideoCapture
                ),
                settingsRepository = FakeSettingsRepository(),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.empty(),
                savedStateHandle = SavedStateHandle(
                    mapOf(
                        PreviewRoute.ARG_EXTERNAL_CAPTURE_MODE to ExternalCaptureMode.VideoCapture
                    )
                ),
                defaultSaveMode = SaveMode.Immediate,
                cameraFeaturePolicy = defaultTestPolicy
            )
            advanceUntilIdle()
            startCameraUntilRunning(viewModel)

            val uiState = viewModel.captureUiState.value
            assertThat(uiState).isInstanceOf(CaptureUiState.Ready::class.java)
            val readyState = uiState as CaptureUiState.Ready
            assertThat(readyState.captureModeToggleUiState)
                .isEqualTo(CaptureModeToggleUiState.Unavailable)
        }

    @Test
    fun defaultSettingsChangedBeforeCreation_propagatesToCameraSystem() =
        runTest(StandardTestDispatcher()) {
            previewViewModel = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(cameraLensFacing = LensFacing.FRONT)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.empty(),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate,
                cameraFeaturePolicy = defaultTestPolicy
            )
            startCameraUntilRunning()

            assertThat(cameraSystem.isLensFacingFront).isTrue()
        }

    @Test
    fun locationUpdates_withLocationProviderPresent_triggersUpdates() =
        runTest(StandardTestDispatcher()) {
            val fakeLocation = FakeLocationProvider()
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(locationEnabled = true)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(fakeLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            assertThat(fakeLocation.isUpdatesRunning).isFalse()
            vm.startLocationUpdates()
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isTrue()
            vm.stopLocationUpdates()
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()
        }

    @Test
    fun locationUpdates_withLocationProviderEmpty_doesNotThrow() =
        runTest(StandardTestDispatcher()) {
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(locationEnabled = true)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.empty(),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            // Verify safe no-op when provider is absent
            vm.startLocationUpdates()
            vm.stopLocationUpdates()
        }

    @Test
    fun locationUpdates_locationSettingToggled_startsAndStopsUpdates() =
        runTest(StandardTestDispatcher()) {
            val fakeLocation = FakeLocationProvider()
            val settingsRepo = FakeSettingsRepository(CameraAppSettings(locationEnabled = false))
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = settingsRepo,
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(fakeLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            advanceUntilIdle()

            vm.startLocationUpdates()
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()

            settingsRepo.updateLocationEnabled(true)
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isTrue()

            settingsRepo.updateLocationEnabled(false)
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()
        }

    @Test
    fun captureImage_locationSettingDisabled_doesNotPassLocationToCameraSystem() =
        runTest(StandardTestDispatcher()) {
            val contentResolver: ContentResolver =
                ApplicationProvider.getApplicationContext<Context>().contentResolver
            val fakeLocation = FakeLocationProvider()
            fakeLocation.setLocation(latitude = 37.4220, longitude = -122.0841)
            val settingsRepo = FakeSettingsRepository(CameraAppSettings(locationEnabled = false))
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = settingsRepo,
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(fakeLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            vm.cameraController.startCamera()
            advanceUntilIdle()

            vm.captureController.captureImage(contentResolver)
            advanceUntilIdle()
            assertThat(cameraSystem.lastPictureTakenLocation).isNull()

            settingsRepo.updateLocationEnabled(true)
            advanceUntilIdle()

            vm.captureController.captureImage(contentResolver)
            advanceUntilIdle()
            assertThat(cameraSystem.lastPictureTakenLocation?.latitude).isEqualTo(37.4220)
        }

    @Test
    fun locationUpdates_videoRecordingStarts_cancelsLocationUpdates() =
        runTest(StandardTestDispatcher()) {
            val fakeLocation = FakeLocationProvider()
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(locationEnabled = true)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(fakeLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            advanceUntilIdle()

            vm.startLocationUpdates()
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isTrue()

            cameraSystem.setCurrentCameraState(
                CameraState(
                    videoRecordingState = VideoRecordingState.Active.Recording(
                        maxDurationMillis = 0,
                        audioStreamState = AudioStreamState.Disabled,
                        elapsedTimeNanos = 0
                    )
                )
            )
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()
        }

    @Test
    fun locationUpdates_videoRecordingStarting_cancelsLocationUpdates() =
        runTest(StandardTestDispatcher()) {
            val fakeLocation = FakeLocationProvider()
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(locationEnabled = true)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(fakeLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            advanceUntilIdle()

            vm.startLocationUpdates()
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isTrue()

            cameraSystem.setCurrentCameraState(
                CameraState(videoRecordingState = VideoRecordingState.Starting())
            )
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()
        }

    @Test
    fun locationUpdates_videoRecordingStops_resumesLocationUpdatesIfPreviewActive() =
        runTest(StandardTestDispatcher()) {
            val fakeLocation = FakeLocationProvider()
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(locationEnabled = true)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(fakeLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            advanceUntilIdle()

            vm.startLocationUpdates()
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isTrue()

            cameraSystem.setCurrentCameraState(
                CameraState(
                    videoRecordingState = VideoRecordingState.Active.Recording(
                        maxDurationMillis = 0,
                        audioStreamState = AudioStreamState.Disabled,
                        elapsedTimeNanos = 0
                    )
                )
            )
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()

            cameraSystem.setCurrentCameraState(
                CameraState(
                    videoRecordingState = VideoRecordingState.Inactive()
                )
            )
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isTrue()
        }

    @Test
    fun locationUpdates_providerThrows_doesNotCrashAndRestartsOnNextTrigger() =
        runTest(StandardTestDispatcher()) {
            val failingLocation = FailingLocationProvider()
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(locationEnabled = true)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(failingLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            advanceUntilIdle()

            vm.startLocationUpdates()
            advanceUntilIdle()
            assertThat(failingLocation.runCount).isEqualTo(1)

            // A recording pauses location updates, and its end restarts them.
            cameraSystem.setCurrentCameraState(
                CameraState(videoRecordingState = VideoRecordingState.Starting())
            )
            advanceUntilIdle()
            cameraSystem.setCurrentCameraState(
                CameraState(videoRecordingState = VideoRecordingState.Inactive())
            )
            advanceUntilIdle()
            assertThat(failingLocation.runCount).isEqualTo(2)
        }

    @Test
    fun locationUpdates_videoRecordingStops_doesNotResumeIfPreviewInactive() =
        runTest(StandardTestDispatcher()) {
            val fakeLocation = FakeLocationProvider()
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(locationEnabled = true)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(fakeLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            advanceUntilIdle()

            vm.startLocationUpdates()
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isTrue()

            cameraSystem.setCurrentCameraState(
                CameraState(
                    videoRecordingState = VideoRecordingState.Active.Recording(
                        maxDurationMillis = 0,
                        audioStreamState = AudioStreamState.Disabled,
                        elapsedTimeNanos = 0
                    )
                )
            )
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()

            vm.stopLocationUpdates()
            advanceUntilIdle()

            cameraSystem.setCurrentCameraState(
                CameraState(
                    videoRecordingState = VideoRecordingState.Inactive()
                )
            )
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()
        }

    @Test
    fun locationUpdates_startWhileVideoRecording_doesNotTriggerUpdates() =
        runTest(StandardTestDispatcher()) {
            val fakeLocation = FakeLocationProvider()
            val vm = PreviewViewModel(
                cameraSystemRepository = cameraSystemRepository,
                settingsRepository = FakeSettingsRepository(
                    CameraAppSettings(locationEnabled = true)
                ),
                mediaRepository = FakeMediaRepository(),
                locationProvider = java.util.Optional.of(fakeLocation),
                savedStateHandle = SavedStateHandle(),
                defaultSaveMode = SaveMode.Immediate
            )
            advanceUntilIdle()

            cameraSystem.setCurrentCameraState(
                CameraState(
                    videoRecordingState = VideoRecordingState.Active.Recording(
                        maxDurationMillis = 0,
                        audioStreamState = AudioStreamState.Disabled,
                        elapsedTimeNanos = 0
                    )
                )
            )
            advanceUntilIdle()

            vm.startLocationUpdates()
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isFalse()

            cameraSystem.setCurrentCameraState(
                CameraState(
                    videoRecordingState = VideoRecordingState.Inactive()
                )
            )
            advanceUntilIdle()
            assertThat(fakeLocation.isUpdatesRunning).isTrue()
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

private class FailingLocationProvider : LocationProvider {
    var runCount = 0
        private set

    override fun getCurrentLocation(): Location? = null

    override suspend fun runLocationUpdates() {
        runCount++
        throw IllegalStateException("Location hardware unavailable")
    }
}
