/*
 * Copyright (C) 2024 The Android Open Source Project
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
package com.google.jetpackcamera.core.camera

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import androidx.camera.camera2.interop.cameraCharacteristics
import androidx.camera.core.CameraInfo
import androidx.camera.core.DynamicRange as CXDynamicRange
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.video.Recorder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent.OnVideoRecordError
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent.OnVideoRecorded
import com.google.jetpackcamera.core.camera.postprocess.ImagePostProcessor
import com.google.jetpackcamera.core.camera.postprocess.ImagePostProcessorFeatureKey
import com.google.jetpackcamera.core.camera.postprocess.di.PostProcessModule.Companion.provideImagePostProcessorMap
import com.google.jetpackcamera.core.camera.submode.CameraSessionBinding
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeFeatureKey
import com.google.jetpackcamera.core.camera.submode.CaptureSubModeProvider
import com.google.jetpackcamera.core.camera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.core.camera.utils.provideUpdatingSurface
import com.google.jetpackcamera.core.common.ignoreResult
import com.google.jetpackcamera.core.common.testing.FakeFilePathGenerator
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.CaptureSubModeDescriptor
import com.google.jetpackcamera.model.CaptureSubModeId
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.Illuminant
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraFeaturePolicy
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.OptionVisibility
import com.google.jetpackcamera.settings.model.SettingConfig
import com.google.jetpackcamera.settings.model.applyExternalCaptureMode
import com.google.jetpackcamera.settings.model.forCurrentLens
import java.io.File
import java.util.AbstractMap
import javax.inject.Provider
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CameraXCameraSystemTest {

    companion object {
        private const val CAMERA_START_TIMEOUT_MS = 10_000L
        private const val GENERAL_TIMEOUT_MS = 3_000L
        private const val RECORDING_TIMEOUT_MS = 10_000L
        private const val RECORDING_START_DURATION_MS = 500L
    }

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(APP_REQUIRED_PERMISSIONS).toTypedArray())

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val application = context.applicationContext as Application
    private val filesToDelete = mutableSetOf<Uri>()
    private lateinit var cameraSystemScope: CoroutineScope
    private var cameraJob: Job? = null

    private lateinit var contentResolver: ContentResolver

    @Before
    fun setup() {
        cameraSystemScope = CoroutineScope(Dispatchers.Main)
        contentResolver = context.contentResolver
    }

    @After
    fun tearDown() {
        runBlocking {
            cameraJob?.cancelAndJoin()
        }
        deleteFiles(filesToDelete)
    }

    @Test
    fun canCaptureImage(): Unit = runBlocking {
        // Arrange.
        val cameraSystem =
            createAndInitCameraXCameraSystem()
        cameraSystem.startCameraAndWaitUntilRunning()

        // Act.
        val result = cameraSystem.takePicture(context.contentResolver, SaveLocation.Default) {}

        // Assert.
        val savedUri = result.savedUri
        assertThat(savedUri).isNotNull()
        if (savedUri != null) {
            filesToDelete.add(savedUri)
        }
    }

    @Test
    fun captureImage_withPostProcessor_postProcessIsCalled(): Unit = runBlocking {
        // Arrange.
        val imagePostProcessor = FakeImagePostProcessor()
        val cameraSystem =
            createAndInitCameraXCameraSystem(fakeImagePostProcessor = imagePostProcessor)
        cameraSystem.startCameraAndWaitUntilRunning()

        // Act.
        cameraSystem.takePicture(context.contentResolver, SaveLocation.Default) {}.ignoreResult()

        // Assert.
        assertThat(imagePostProcessor.postProcessImageCalled).isTrue()
    }

    @Test
    fun captureImage_withInvalidUri_postProcessNotCalled(): Unit = runBlocking {
        // Arrange.
        val imagePostProcessor = FakeImagePostProcessor()
        val cameraSystem =
            createAndInitCameraXCameraSystem(fakeImagePostProcessor = imagePostProcessor)
        cameraSystem.startCameraAndWaitUntilRunning()

        // Act.
        try {
            cameraSystem.takePicture(
                context.contentResolver,
                SaveLocation.Explicit(Uri.parse("asdfasdf"))
            ) {}.ignoreResult()
        } catch (e: Exception) {}

        // Assert.
        assertThat(imagePostProcessor.postProcessImageCalled).isFalse()
    }

    @Test
    fun captureImage_withFailingPostProcessor_imageStillSaved(): Unit = runBlocking {
        // Arrange.
        val imagePostProcessor = FakeImagePostProcessor(shouldError = true)
        val cameraSystem =
            createAndInitCameraXCameraSystem(fakeImagePostProcessor = imagePostProcessor)
        cameraSystem.startCameraAndWaitUntilRunning()
        val uri = Uri.parse(FakeFilePathGenerator().generateImageFilename())

        // Act.
        try {
            cameraSystem.takePicture(
                context.contentResolver,
                SaveLocation.Default
            ) {}.ignoreResult()
        } catch (e: RuntimeException) {
            // Assert.
            assertThat(imagePostProcessor.postProcessImageCalled).isTrue()

            val savedUri = imagePostProcessor.savedUri
            assertThat(savedUri).isNotNull()
            if (savedUri != null) {
                filesToDelete.add(savedUri)
            }
        }
    }

    @Test
    fun captureImage_noPostProcessor(): Unit = runBlocking {
        // Arrange.
        val imagePostProcessor = FakeImagePostProcessor()
        val cameraSystem =
            createAndInitCameraXCameraSystem()
        cameraSystem.startCameraAndWaitUntilRunning()

        // Act.
        cameraSystem.takePicture(context.contentResolver, SaveLocation.Default) {}.ignoreResult()

        // Assert.
        assertThat(imagePostProcessor.postProcessImageCalled).isFalse()
    }

    @Test
    fun canRecordVideo(): Unit = runBlocking {
        // Arrange.
        val cameraSystem = createAndInitCameraXCameraSystem()
        cameraSystem.startCameraAndWaitUntilRunning()

        // Act.
        val recordingComplete = CompletableDeferred<Unit>()
        cameraSystem.startRecording {
            when (it) {
                is OnVideoRecorded -> {
                    recordingComplete.complete(Unit)
                }

                is OnVideoRecordError -> recordingComplete.completeExceptionally(it.error)
            }
        }

        cameraSystem.stopVideoRecording()

        // Assert.
        recordingComplete.await()
    }

    @Test
    fun setStabilizationMode_on_updatesCameraState(): Unit = runBlocking {
        runSetStabilizationModeTest(StabilizationMode.ON)
    }

    @Test
    fun setStabilizationMode_optical_updatesCameraState(): Unit = runBlocking {
        runSetStabilizationModeTest(StabilizationMode.OPTICAL)
    }

    @Test
    fun setStabilizationMode_highQuality_updatesCameraState(): Unit = runBlocking {
        runSetStabilizationModeTest(StabilizationMode.HIGH_QUALITY)
    }

    @Test
    fun setStabilizationMode_off_imageOnly_updatesCameraState(): Unit = runBlocking {
        runSetStabilizationModeTest(StabilizationMode.OFF, CaptureMode.IMAGE_ONLY)
    }

    private suspend fun CoroutineScope.runSetStabilizationModeTest(
        stabilizationMode: StabilizationMode,
        captureMode: CaptureMode? = null
    ) {
        // Arrange.
        val cameraSystem = createAndInitCameraXCameraSystem(
            appSettings = CameraAppSettings(stabilizationMode = StabilizationMode.OFF)
        )
        val cameraConstraints =
            cameraSystem.getSystemConstraints().value?.forCurrentLens(
                DEFAULT_CAMERA_APP_SETTINGS
            )
        assume().withMessage("Stabilisation $stabilizationMode not supported, skip the test.")
            .that(
                cameraConstraints != null &&
                    cameraConstraints.supportedStabilizationModes.contains(
                        stabilizationMode
                    )
            ).isTrue()
        var initialStabilizationMode: StabilizationMode? = StabilizationMode.OFF
        if (stabilizationMode == StabilizationMode.OFF) {
            initialStabilizationMode = cameraConstraints?.supportedStabilizationModes
                ?.firstOrNull { it != StabilizationMode.OFF }
            assume().withMessage("No stabilisation other than OFF is supported, skip the test.")
                .that(initialStabilizationMode != null).isTrue()
            initialStabilizationMode?.let {
                cameraSystem.setStabilizationMode(initialStabilizationMode)
            }
        }

        cameraSystem.startCameraAndWaitUntilRunning()
        val stabilizationCheck: ReceiveChannel<StabilizationMode> =
            cameraSystem.getCurrentCameraState()
                .map { it.stabilizationMode }
                .produceIn(this)

        // Ensure we start in a state with the initial stabilization mode
        stabilizationCheck.awaitValue(initialStabilizationMode)

        // Act.
        cameraSystem.setStabilizationMode(stabilizationMode)
        captureMode?.let { cameraSystem.setCaptureMode(it) }

        // Assert.
        stabilizationCheck.awaitValue(stabilizationMode)

        // Clean-up.
        stabilizationCheck.cancel()
    }

    @Test
    fun recordVideoWithFlashModeOn_shouldEnableTorch(): Unit = runBlocking {
        // Arrange.
        val lensFacing = LensFacing.BACK
        val cameraSystem = createAndInitCameraXCameraSystem()
        cameraSystem.startCameraAndWaitUntilRunning()
        val hasFlashUnit = cameraSystem.getSystemConstraints().value?.perLensConstraints?.get(
            lensFacing
        )?.supportedIlluminants?.contains(Illuminant.FLASH_UNIT) == true
        assume().withMessage("No flash unit, skip the test.")
            .that(hasFlashUnit).isTrue()

        // Arrange: Create a ReceiveChannel to observe the torch enabled state.
        val torchEnabled: ReceiveChannel<Boolean> = cameraSystem.getCurrentCameraState()
            .map { it.isTorchEnabled }
            .produceIn(this)

        // Assert: The initial torch enabled should be false.
        torchEnabled.awaitValue(false)

        // Act: Start recording with FlashMode.ON
        val recordingComplete = CompletableDeferred<Unit>()
        cameraSystem.setFlashMode(FlashMode.ON)
        cameraSystem.startRecording {
            when (it) {
                is OnVideoRecorded -> {
                    recordingComplete.complete(Unit)
                }

                is OnVideoRecordError -> recordingComplete.completeExceptionally(it.error)
            }
        }

        // Assert: Torch enabled transitions to true.
        torchEnabled.awaitValue(true)

        cameraSystem.stopVideoRecording()

        // Assert: Torch enabled transitions to false.
        torchEnabled.awaitValue(false)

        // Clean-up.
        recordingComplete.await()
        torchEnabled.cancel()
    }

    private suspend fun createAndInitCameraXCameraSystem(
        appSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
        fakeImagePostProcessor: FakeImagePostProcessor? = null,
        captureSubModeProvider: CaptureSubModeProvider? = null,
        extraCaptureSubModeProviders:
        Map<CaptureSubModeFeatureKey, Provider<CaptureSubModeProvider>> = emptyMap(),
        defaultCaptureSubModes: Map<CaptureMode, CaptureSubModeFeatureKey> = emptyMap(),
        externalCaptureMode: ExternalCaptureMode = ExternalCaptureMode.Standard
    ) = CameraXCameraSystem(
        application = application,
        defaultDispatcher = Dispatchers.Default,
        iODispatcher = Dispatchers.IO,
        availabilityCheckers = emptyMap(),
        effectProviders = emptyMap(),
        imagePostProcessors = getFakePostProcessorMap(fakeImagePostProcessor),
        cameraEffectProviders = emptyMap(),
        filePathGenerator = FakeFilePathGenerator(),
        captureSubModeProviders = (
            captureSubModeProvider?.let { provider ->
                mapOf<CaptureSubModeFeatureKey, Provider<CaptureSubModeProvider>>(
                    FakeCaptureSubModeFeatureKey to Provider { provider }
                )
            } ?: emptyMap()
            ) + extraCaptureSubModeProviders,
        defaultCaptureSubModes = defaultCaptureSubModes
    ).apply {
        initialize(appSettings.applyExternalCaptureMode(externalCaptureMode)) {}
        providePreviewSurface()
    }

    private suspend fun <T> ReceiveChannel<T>.awaitValue(
        expectedValue: T,
        timeoutMs: Long = GENERAL_TIMEOUT_MS
    ) {
        val result = withTimeoutOrNull(timeoutMs) {
            for (value in this@awaitValue) {
                if (value == expectedValue) return@withTimeoutOrNull
            }
        }
        assertWithMessage("Timeout while waiting for expected value: $expectedValue").that(result)
            .isNotNull()
    }

    private suspend fun CameraXCameraSystem.startRecording(
        onVideoRecord: (OnVideoRecordEvent) -> Unit
    ) {
        // Start recording
        startVideoRecording(SaveLocation.Default) { event ->
            // Track files that need to be deleted
            if (event is OnVideoRecorded) {
                val videoUri = event.savedUri
                if (videoUri != Uri.EMPTY) {
                    filesToDelete.add(videoUri)
                }
            }

            // Forward event to provided callback
            onVideoRecord(event)
        }

        // Wait for recording duration to reach start duration to consider it started
        withTimeout(RECORDING_TIMEOUT_MS) {
            getCurrentCameraState().transform { cameraState ->
                (cameraState.videoRecordingState as? VideoRecordingState.Active)?.let {
                    emit(
                        it.elapsedTimeNanos.toDuration(DurationUnit.NANOSECONDS).inWholeMilliseconds
                    )
                }
            }.first { elapsedTimeMs ->
                elapsedTimeMs >= RECORDING_START_DURATION_MS
            }
        }
    }

    private fun CameraXCameraSystem.providePreviewSurface() {
        cameraSystemScope.launch {
            getSurfaceRequest().filterNotNull().collect {
                it.provideUpdatingSurface()
            }
        }
    }

    private suspend fun CameraXCameraSystem.startCameraAndWaitUntilRunning() {
        cameraJob = cameraSystemScope.launch { runCamera() }
        // Wait for camera to be running.
        val cameraStarted = cameraSystemScope.async {
            withTimeoutOrNull(CAMERA_START_TIMEOUT_MS) {
                getCurrentCameraState().filterNotNull().first {
                    it.isCameraRunning
                }
            }
        }.await() != null
        assertWithMessage("Camera timed out while starting.").that(cameraStarted).isTrue()
        instrumentation.waitForIdleSync()
    }

    private fun deleteFiles(uris: Set<Uri>) {
        for (uri in uris) {
            when (uri.scheme) {
                ContentResolver.SCHEME_CONTENT -> {
                    try {
                        context.contentResolver.delete(uri, null, null)
                    } catch (_: RuntimeException) {
                        // Ignore any exception.
                    }
                }

                ContentResolver.SCHEME_FILE -> {
                    File(uri.path!!).delete()
                }
            }
        }
    }

    private fun getFakePostProcessorMap(
        imagePostProcessor: FakeImagePostProcessor?
    ): Map<ImagePostProcessorFeatureKey, @JvmSuppressWildcards Provider<ImagePostProcessor>> {
        if (imagePostProcessor == null) {
            return emptyMap()
        }
        return provideImagePostProcessorMap(
            entries = setOf(
                AbstractMap.SimpleImmutableEntry(
                    FakeImagePostProcessorFeatureKey,
                    Provider { imagePostProcessor }
                )
            )
        )
    }

    @Test
    fun switchCaptureMode_toStandard_disablesHdr_back(): Unit = runBlocking {
        runSwitchCaptureMode_toStandard_disablesHdr_test(LensFacing.BACK)
    }

    @Test
    fun switchCaptureMode_toStandard_disablesHdr_front(): Unit = runBlocking {
        runSwitchCaptureMode_toStandard_disablesHdr_test(LensFacing.FRONT)
    }

    @Test
    fun switchCaptureMode_toStandard_disablesImageHdr_back(): Unit = runBlocking {
        runSwitchCaptureMode_toStandard_disablesImageHdr_test(LensFacing.BACK)
    }

    @Test
    fun switchCaptureMode_toStandard_disablesImageHdr_front(): Unit = runBlocking {
        runSwitchCaptureMode_toStandard_disablesImageHdr_test(LensFacing.FRONT)
    }

    @Test
    fun switchCaptureMode_preservesVideoHdr_back(): Unit = runBlocking {
        runSwitchCaptureMode_preservesVideoHdr_test(LensFacing.BACK)
    }

    @Test
    fun switchCaptureMode_preservesVideoHdr_front(): Unit = runBlocking {
        runSwitchCaptureMode_preservesVideoHdr_test(LensFacing.FRONT)
    }

    @Test
    fun switchCaptureMode_preservesImageHdr_back(): Unit = runBlocking {
        runSwitchCaptureMode_preservesImageHdr_test(LensFacing.BACK)
    }

    @Test
    fun switchCaptureMode_preservesImageHdr_front(): Unit = runBlocking {
        runSwitchCaptureMode_preservesImageHdr_test(LensFacing.FRONT)
    }

    private suspend fun CoroutineScope.runSwitchCaptureMode_toStandard_disablesHdr_test(
        lensFacing: LensFacing
    ) {
        // Arrange. Initialize with default settings to query constraints safely.
        val cameraSystem = createAndInitCameraXCameraSystem()
        val systemConstraints = cameraSystem.getSystemConstraints().value
        val cameraConstraints = systemConstraints?.perLensConstraints?.get(lensFacing)

        // This instrumented test runs on real hardware/emulator. Since we cannot mock the
        // device's actual HDR capabilities, we use assume() to gracefully skip the test
        // if the specified lens is not available or does not support HDR video (HLG10).
        assume().withMessage("HDR video not supported on $lensFacing, skip the test.")
            .that(
                cameraConstraints != null &&
                    cameraConstraints.supportedDynamicRanges.contains(DynamicRange.HLG10)
            ).isTrue()

        // Configure the camera to use the target lens and enable HDR video
        cameraSystem.setLensFacing(lensFacing)
        cameraSystem.setCaptureMode(CaptureMode.VIDEO_ONLY)
        cameraSystem.setDynamicRange(DynamicRange.HLG10)

        cameraSystem.startCameraAndWaitUntilRunning()

        val dynamicRangeCheck = cameraSystem.getCurrentSettings()
            .filterNotNull()
            .map { it.dynamicRange }
            .produceIn(this)

        // Ensure we start in HLG10
        dynamicRangeCheck.awaitValue(DynamicRange.HLG10)

        // Act. Switch to STANDARD mode
        cameraSystem.setCaptureMode(CaptureMode.STANDARD)

        // Assert. Dynamic range should fallback to SDR because STANDARD doesn't support HDR
        dynamicRangeCheck.awaitValue(DynamicRange.SDR)

        // Clean-up.
        dynamicRangeCheck.cancel()
    }

    private suspend fun CoroutineScope.runSwitchCaptureMode_toStandard_disablesImageHdr_test(
        lensFacing: LensFacing
    ) {
        val cameraSystem = createAndInitCameraXCameraSystem()
        val systemConstraints = cameraSystem.getSystemConstraints().value
        val cameraConstraints = systemConstraints?.perLensConstraints?.get(lensFacing)

        // Skip test if Ultra HDR is not supported on the target lens
        assume().withMessage("Ultra HDR not supported on $lensFacing, skip the test.")
            .that(
                cameraConstraints != null &&
                    cameraConstraints.supportedImageFormatsMap[false]?.contains(
                        ImageOutputFormat.JPEG_ULTRA_HDR
                    ) == true
            ).isTrue()

        cameraSystem.setLensFacing(lensFacing)
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)
        cameraSystem.setImageFormat(ImageOutputFormat.JPEG_ULTRA_HDR)

        cameraSystem.startCameraAndWaitUntilRunning()

        val imageFormatCheck = cameraSystem.getCurrentSettings()
            .filterNotNull()
            .map { it.imageFormat }
            .produceIn(this)

        imageFormatCheck.awaitValue(ImageOutputFormat.JPEG_ULTRA_HDR)

        cameraSystem.setCaptureMode(CaptureMode.STANDARD)

        imageFormatCheck.awaitValue(ImageOutputFormat.JPEG)

        imageFormatCheck.cancel()
    }

    private suspend fun CoroutineScope.runSwitchCaptureMode_preservesVideoHdr_test(
        lensFacing: LensFacing
    ) {
        // Arrange. Initialize with default settings to query constraints safely.
        val cameraSystem = createAndInitCameraXCameraSystem()
        val systemConstraints = cameraSystem.getSystemConstraints().value
        val cameraConstraints = systemConstraints?.perLensConstraints?.get(lensFacing)

        // This instrumented test runs on real hardware/emulator. Since we cannot mock the
        // device's actual HDR capabilities, we use assume() to gracefully skip the test
        // if the specified lens is not available or does not support HDR video (HLG10).
        assume().withMessage("HDR video not supported on $lensFacing, skip the test.")
            .that(
                cameraConstraints != null &&
                    cameraConstraints.supportedDynamicRanges.contains(DynamicRange.HLG10)
            ).isTrue()

        // Configure the camera to use the target lens and enable HDR video
        cameraSystem.setLensFacing(lensFacing)
        cameraSystem.setCaptureMode(CaptureMode.VIDEO_ONLY)
        cameraSystem.setDynamicRange(DynamicRange.HLG10)
        cameraSystem.setImageFormat(ImageOutputFormat.JPEG)

        cameraSystem.startCameraAndWaitUntilRunning()

        val settingsCheck = cameraSystem.getCurrentSettings()
            .filterNotNull()
            .produceIn(this)

        // Ensure we start in VIDEO_ONLY with HLG10
        var settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(settings.dynamicRange).isEqualTo(DynamicRange.HLG10)
        assertThat(settings.imageFormat).isEqualTo(ImageOutputFormat.JPEG)

        // Act. Switch to IMAGE_ONLY
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)

        // Assert. Image format should be JPEG (SDR), but dynamicRange should still be HLG10 in settings
        settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(settings.imageFormat).isEqualTo(ImageOutputFormat.JPEG)
        assertThat(settings.dynamicRange).isEqualTo(DynamicRange.HLG10) // Preserved!

        // Act. Switch back to VIDEO_ONLY
        cameraSystem.setCaptureMode(CaptureMode.VIDEO_ONLY)

        // Assert. Should be back to VIDEO_ONLY with HLG10
        settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(settings.dynamicRange).isEqualTo(DynamicRange.HLG10)

        // Clean-up.
        settingsCheck.cancel()
    }

    private suspend fun CoroutineScope.runSwitchCaptureMode_preservesImageHdr_test(
        lensFacing: LensFacing
    ) {
        // Arrange. Initialize with default settings to query constraints safely.
        val cameraSystem = createAndInitCameraXCameraSystem()
        val systemConstraints = cameraSystem.getSystemConstraints().value
        val cameraConstraints = systemConstraints?.perLensConstraints?.get(lensFacing)

        // This instrumented test runs on real hardware/emulator. Since we cannot mock the
        // device's actual Ultra HDR capabilities, we use assume() to gracefully skip the test
        // if the specified lens is not available or does not support Ultra HDR.
        assume().withMessage("Ultra HDR not supported on $lensFacing, skip the test.")
            .that(
                cameraConstraints != null &&
                    cameraConstraints.supportedImageFormatsMap[false]?.contains(
                        ImageOutputFormat.JPEG_ULTRA_HDR
                    ) == true
            ).isTrue()

        // Configure the camera to use the target lens and enable Ultra HDR
        cameraSystem.setLensFacing(lensFacing)
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)
        cameraSystem.setImageFormat(ImageOutputFormat.JPEG_ULTRA_HDR)
        cameraSystem.setDynamicRange(DynamicRange.SDR)

        cameraSystem.startCameraAndWaitUntilRunning()

        val settingsCheck = cameraSystem.getCurrentSettings()
            .filterNotNull()
            .produceIn(this)

        // Ensure we start in IMAGE_ONLY with ULTRA_HDR
        var settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(settings.imageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
        assertThat(settings.dynamicRange).isEqualTo(DynamicRange.SDR)

        // Act. Switch to VIDEO_ONLY
        cameraSystem.setCaptureMode(CaptureMode.VIDEO_ONLY)

        // Assert. Dynamic range should be SDR, but imageFormat should still be ULTRA_HDR in settings
        settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(settings.dynamicRange).isEqualTo(DynamicRange.SDR)
        assertThat(settings.imageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR) // Preserved!

        // Act. Switch back to IMAGE_ONLY
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)

        // Assert. Should be back to IMAGE_ONLY with ULTRA_HDR
        settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(settings.imageFormat).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)

        // Clean-up.
        settingsCheck.cancel()
    }

    @Test
    fun leaveCaptureSubMode_restoresSettingsReplacedByItsPolicy(): Unit = runBlocking {
        // Arrange. The sub-mode's policy locks the aspect ratio to 9:16.
        val cameraSystem = createAndInitCameraXCameraSystem(
            captureSubModeProvider = FakeCaptureSubModeProvider(
                CameraFeaturePolicy(
                    aspectRatio = SettingConfig(AspectRatio.NINE_SIXTEEN, OptionVisibility.Hidden)
                )
            )
        )
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)
        cameraSystem.setAspectRatio(AspectRatio.ONE_ONE)

        // Act. Enter the sub-mode.
        cameraSystem.setCaptureSubMode(FAKE_CAPTURE_SUB_MODE_ID)

        // Assert. The policy replaces the aspect ratio, and the replaced value is recorded.
        var settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
        assertThat(settings.captureSubModeOverrides).isNotNull()

        // Act. Leave the sub-mode.
        cameraSystem.setCaptureSubMode(CaptureSubModeId.DEFAULT)

        // Assert. The previous aspect ratio is restored, and the record is cleared.
        settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.ONE_ONE)
        assertThat(settings.captureSubModeOverrides).isNull()
    }

    @Test
    fun leaveCaptureSubMode_keepsSettingsChangedWhileActive(): Unit = runBlocking {
        // Arrange. The sub-mode's policy limits the aspect ratio to 9:16 or 1:1.
        val cameraSystem = createAndInitCameraXCameraSystem(
            captureSubModeProvider = FakeCaptureSubModeProvider(
                CameraFeaturePolicy(
                    aspectRatio = SettingConfig(
                        defaultValue = AspectRatio.NINE_SIXTEEN,
                        visibility = OptionVisibility.from(
                            AspectRatio.NINE_SIXTEEN,
                            AspectRatio.ONE_ONE
                        )
                    )
                )
            )
        )
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)
        cameraSystem.setAspectRatio(AspectRatio.THREE_FOUR)
        cameraSystem.setCaptureSubMode(FAKE_CAPTURE_SUB_MODE_ID)
        assertThat(cameraSystem.getCurrentSettings().value!!.aspectRatio)
            .isEqualTo(AspectRatio.NINE_SIXTEEN)

        // Act. Choose another permitted aspect ratio, then leave the sub-mode.
        cameraSystem.setAspectRatio(AspectRatio.ONE_ONE)
        cameraSystem.setCaptureSubMode(CaptureSubModeId.DEFAULT)

        // Assert. The aspect ratio chosen in the sub-mode is kept.
        assertThat(cameraSystem.getCurrentSettings().value!!.aspectRatio)
            .isEqualTo(AspectRatio.ONE_ONE)
    }

    @Test
    fun switchCaptureMode_fromCaptureSubMode_appliesNewModeDefaults(): Unit = runBlocking {
        // Arrange. The sub-mode's policy locks the aspect ratio to 9:16.
        val cameraSystem = createAndInitCameraXCameraSystem(
            captureSubModeProvider = FakeCaptureSubModeProvider(
                CameraFeaturePolicy(
                    aspectRatio = SettingConfig(AspectRatio.NINE_SIXTEEN, OptionVisibility.Hidden)
                )
            )
        )
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)
        cameraSystem.setAspectRatio(AspectRatio.ONE_ONE)
        cameraSystem.setCaptureSubMode(FAKE_CAPTURE_SUB_MODE_ID)

        // Act. Switching capture mode ends the sub-mode.
        cameraSystem.setCaptureMode(CaptureMode.VIDEO_ONLY)

        // Assert. The video mode's aspect ratio takes precedence over the restored 1:1.
        val settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
    }

    @Test
    fun setCaptureSubMode_fromStandard_adoptsParentCaptureMode(): Unit = runBlocking {
        // Arrange. Start in STANDARD with 4:3, with a VIDEO_ONLY sub-mode available.
        val cameraSystem = createAndInitCameraXCameraSystem(
            appSettings = CameraAppSettings(
                captureMode = CaptureMode.STANDARD,
                aspectRatio = AspectRatio.THREE_FOUR
            ),
            captureSubModeProvider = FakeCaptureSubModeProvider(
                parentCaptureMode = CaptureMode.VIDEO_ONLY
            )
        )

        // Act.
        cameraSystem.setCaptureSubMode(FAKE_CAPTURE_SUB_MODE_ID)

        // Assert. The parent capture mode and its default aspect ratio are applied, and the
        // sub-mode is active.
        val settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
        assertThat(settings.captureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
    }

    @Test
    fun setCaptureSubMode_fromOtherExplicitCaptureMode_isIgnored(): Unit = runBlocking {
        // Arrange. IMAGE_ONLY is selected explicitly, and the sub-mode belongs to VIDEO_ONLY.
        val cameraSystem = createAndInitCameraXCameraSystem(
            captureSubModeProvider = FakeCaptureSubModeProvider(
                parentCaptureMode = CaptureMode.VIDEO_ONLY
            )
        )
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)

        // Act.
        cameraSystem.setCaptureSubMode(FAKE_CAPTURE_SUB_MODE_ID)

        // Assert. The explicit capture mode is kept, so the sub-mode cannot become active.
        val settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
    }

    @Test
    fun initialize_offersHdrVideoOnlyWithTenBitCapability(): Unit = runBlocking {
        // Arrange.
        val cameraSystem = createAndInitCameraXCameraSystem()
        val perLensConstraints = cameraSystem.getSystemConstraints().value?.perLensConstraints
        assertThat(perLensConstraints).isNotNull()
        val cameraInfos = ProcessCameraProvider.awaitInstance(application).availableCameraInfos

        // Assert. HLG10 may only be offered on lenses whose camera advertises the
        // DYNAMIC_RANGE_TEN_BIT capability. CameraX will not bind 10-bit streams without it,
        // even if the camera lists 10-bit dynamic range profiles.
        for ((lensFacing, constraints) in perLensConstraints!!) {
            val cameraInfo = lensFacing.toCameraSelector().filter(cameraInfos).first()
            if (!cameraInfo.cameraCharacteristics.isTenBitDynamicRangeSupported) {
                assertWithMessage(
                    "$lensFacing offers HDR video without the DYNAMIC_RANGE_TEN_BIT capability."
                ).that(constraints.supportedDynamicRanges).containsExactly(DynamicRange.SDR)
            } else {
                val reportedRanges =
                    Recorder.getVideoCapabilities(cameraInfo)
                        .supportedDynamicRanges
                        .mapNotNull(CXDynamicRange::toSupportedAppDynamicRange)
                        .toSet()
                assertWithMessage(
                    "$lensFacing should match reported dynamic ranges when 10-bit capability " +
                        "is supported."
                ).that(constraints.supportedDynamicRanges)
                    .containsExactlyElementsIn(reportedRanges)
            }
        }
    }

    @Test
    fun switchCaptureMode_updatesAspectRatio(): Unit = runBlocking {
        // Arrange. Start with STANDARD mode and 4:3 aspect ratio
        val cameraSystem =
            createAndInitCameraXCameraSystem(
                appSettings =
                CameraAppSettings(
                    captureMode = CaptureMode.STANDARD,
                    aspectRatio = AspectRatio.THREE_FOUR
                )
            )
        cameraSystem.startCameraAndWaitUntilRunning()

        val settingsCheck = cameraSystem.getCurrentSettings().filterNotNull().produceIn(this)

        // Ensure we start in STANDARD with 4:3
        var settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.STANDARD)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)

        // Act. Switch to VIDEO_ONLY
        cameraSystem.setCaptureMode(CaptureMode.VIDEO_ONLY)

        // Assert. Aspect ratio should be overridden to NINE_SIXTEEN
        settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.VIDEO_ONLY)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)

        // Act. Switch to IMAGE_ONLY
        cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)

        // Assert. Aspect ratio should be overridden to THREE_FOUR
        settings = settingsCheck.receive()
        assertThat(settings.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)

        // Clean-up.
        settingsCheck.cancel()
    }

    @Test
    fun defaultCaptureSubMode_enforcesPolicyOnEnterAndRestoresOnCaptureModeExit(): Unit =
        runBlocking {
            val cameraSystem = createAndInitCameraXCameraSystem(
                captureSubModeProvider = FakeCaptureSubModeProvider(
                    CameraFeaturePolicy(
                        aspectRatio = SettingConfig(
                            AspectRatio.NINE_SIXTEEN,
                            OptionVisibility.Hidden
                        )
                    )
                ),
                defaultCaptureSubModes = mapOf(
                    CaptureMode.IMAGE_ONLY to FakeCaptureSubModeFeatureKey
                )
            )
            cameraSystem.setCaptureMode(CaptureMode.STANDARD)
            assertThat(cameraSystem.getCurrentSettings().value!!.activeCaptureSubModeId)
                .isEqualTo(CaptureSubModeId.DEFAULT)

            // Act. Enter IMAGE_ONLY (whose base default is 3:4), where FakeCaptureSubModeFeatureKey
            // is the default override and forces 9:16.
            cameraSystem.setCaptureMode(CaptureMode.IMAGE_ONLY)

            // Assert. captureSubModeId stays DEFAULT while activeCaptureSubModeId is the override.
            var settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)

            // Act. Leave IMAGE_ONLY back to STANDARD (which preserves aspect ratio unless restored).
            cameraSystem.setCaptureMode(CaptureMode.STANDARD)

            // Assert. The 3:4 aspect ratio replaced by the default override is restored.
            settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
        }

    @Test
    fun defaultCaptureSubMode_switchingBetweenDefaultAndSecondarySubMode_swapsPolicies(): Unit =
        runBlocking {
            val secondaryKey = object : CaptureSubModeFeatureKey {
                override val id = SECOND_FAKE_CAPTURE_SUB_MODE_ID
            }
            val defaultProvider = FakeCaptureSubModeProvider(
                CameraFeaturePolicy(
                    aspectRatio = SettingConfig(AspectRatio.NINE_SIXTEEN, OptionVisibility.Hidden)
                )
            )
            val secondaryProvider = FakeCaptureSubModeProvider(
                featurePolicy = CameraFeaturePolicy(
                    aspectRatio = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Hidden)
                ),
                subModeId = SECOND_FAKE_CAPTURE_SUB_MODE_ID
            )
            val cameraSystem = createAndInitCameraXCameraSystem(
                appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                    captureMode = CaptureMode.IMAGE_ONLY,
                    aspectRatio = AspectRatio.THREE_FOUR
                ),
                extraCaptureSubModeProviders = mapOf(
                    FakeCaptureSubModeFeatureKey to Provider { defaultProvider },
                    secondaryKey to Provider { secondaryProvider }
                ),
                defaultCaptureSubModes = mapOf(
                    CaptureMode.IMAGE_ONLY to FakeCaptureSubModeFeatureKey
                )
            )

            // Initially in IMAGE_ONLY with DEFAULT slot -> defaultProvider active (9:16).
            var settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)

            // Act. Select secondary sub-mode -> secondaryProvider active (1:1).
            cameraSystem.setCaptureSubMode(SECOND_FAKE_CAPTURE_SUB_MODE_ID)
            settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureSubModeId).isEqualTo(SECOND_FAKE_CAPTURE_SUB_MODE_ID)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(SECOND_FAKE_CAPTURE_SUB_MODE_ID)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.ONE_ONE)

            // Act. Return to DEFAULT slot -> defaultProvider active again (9:16).
            cameraSystem.setCaptureSubMode(CaptureSubModeId.DEFAULT)
            settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
        }

    @Test
    fun defaultCaptureSubMode_supportedOnBackOnly_fallsBackOnFrontAndReappliesOnBack(): Unit =
        runBlocking {
            val cameraSystem = createAndInitCameraXCameraSystem(
                appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                    cameraLensFacing = LensFacing.BACK,
                    captureMode = CaptureMode.IMAGE_ONLY,
                    aspectRatio = AspectRatio.THREE_FOUR
                ),
                captureSubModeProvider = FakeCaptureSubModeProvider(
                    featurePolicy = CameraFeaturePolicy(
                        aspectRatio = SettingConfig(
                            AspectRatio.NINE_SIXTEEN,
                            OptionVisibility.Hidden
                        )
                    ),
                    supportedLenses = setOf(LensFacing.BACK)
                ),
                defaultCaptureSubModes = mapOf(
                    CaptureMode.IMAGE_ONLY to FakeCaptureSubModeFeatureKey
                )
            )
            val availableLenses = cameraSystem.getSystemConstraints().value!!.availableLenses
            assume()
                .withMessage("Both FRONT and BACK lenses required, skip the test.")
                .that(availableLenses.containsAll(listOf(LensFacing.FRONT, LensFacing.BACK)))
                .isTrue()

            // On BACK, the default override is active and forces 9:16 (replacing 3:4).
            var settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)

            // Flip to FRONT, where the default override is unsupported -> restores 3:4.
            cameraSystem.setLensFacing(LensFacing.FRONT)
            settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)

            // Choose 1:1 on FRONT, then flip back to BACK -> default override forces 9:16 again.
            cameraSystem.setAspectRatio(AspectRatio.ONE_ONE)
            cameraSystem.setLensFacing(LensFacing.BACK)
            settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)

            // Flip to FRONT again -> restores the 1:1 chosen on FRONT.
            cameraSystem.setLensFacing(LensFacing.FRONT)
            settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.ONE_ONE)
        }

    @Test
    fun defaultCaptureSubMode_whenIncompatible_fallsBackAndRecovers(): Unit = runBlocking {
        val cameraSystem = createAndInitCameraXCameraSystem(
            appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY,
                aspectRatio = AspectRatio.THREE_FOUR
            ),
            captureSubModeProvider = FakeCaptureSubModeProvider(
                featurePolicy = CameraFeaturePolicy(
                    flashMode = SettingConfig(FlashMode.OFF, OptionVisibility.Hidden)
                ),
                isCompatiblePredicate = { settings -> settings.aspectRatio != AspectRatio.ONE_ONE }
            ),
            defaultCaptureSubModes = mapOf(
                CaptureMode.IMAGE_ONLY to FakeCaptureSubModeFeatureKey
            )
        )

        var settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)

        // Setting aspect ratio to 1:1 makes the default override incompatible -> falls back.
        cameraSystem.setAspectRatio(AspectRatio.ONE_ONE)
        settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
        assertThat(settings.activeCaptureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)

        // Returning to 4:3 restores compatibility -> default override activates again.
        cameraSystem.setAspectRatio(AspectRatio.THREE_FOUR)
        settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
        assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
    }

    @Test
    fun defaultCaptureSubMode_withExternalImageCaptureIntent_activatesDefaultOverride(): Unit =
        runBlocking {
            val cameraSystem = createAndInitCameraXCameraSystem(
                appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                    captureMode = CaptureMode.STANDARD,
                    aspectRatio = AspectRatio.THREE_FOUR
                ),
                captureSubModeProvider = FakeCaptureSubModeProvider(
                    featurePolicy = CameraFeaturePolicy(
                        aspectRatio = SettingConfig(
                            AspectRatio.NINE_SIXTEEN,
                            OptionVisibility.Hidden
                        )
                    )
                ),
                defaultCaptureSubModes = mapOf(
                    CaptureMode.IMAGE_ONLY to FakeCaptureSubModeFeatureKey
                ),
                externalCaptureMode = ExternalCaptureMode.ImageCapture
            )

            val settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureMode).isEqualTo(CaptureMode.IMAGE_ONLY)
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)
        }

    @Test(expected = IllegalArgumentException::class)
    fun defaultCaptureSubMode_whenParentCaptureModeMismatches_throws(): Unit = runBlocking {
        // FakeCaptureSubModeProvider has parentCaptureMode = IMAGE_ONLY; binding it as the
        // default override for VIDEO_ONLY must fail fast during CameraXCameraSystem construction.
        createAndInitCameraXCameraSystem(
            captureSubModeProvider = FakeCaptureSubModeProvider(CameraFeaturePolicy()),
            defaultCaptureSubModes = mapOf(
                CaptureMode.VIDEO_ONLY to FakeCaptureSubModeFeatureKey
            )
        )
    }

    @Test
    fun setCaptureSubMode_switchingSubModesWithConflictingOverrides_activatesSecond(): Unit =
        runBlocking {
        val secondaryKey = object : CaptureSubModeFeatureKey {
            override val id = SECOND_FAKE_CAPTURE_SUB_MODE_ID
        }
        // First provider forces aspectRatio = ONE_ONE.
        val firstProvider = FakeCaptureSubModeProvider(
            featurePolicy = CameraFeaturePolicy(
                aspectRatio = SettingConfig(AspectRatio.ONE_ONE, OptionVisibility.Hidden)
            )
        )
        // Second provider forces aspectRatio = NINE_SIXTEEN and is incompatible with ONE_ONE.
        val secondProvider = FakeCaptureSubModeProvider(
            featurePolicy = CameraFeaturePolicy(
                aspectRatio = SettingConfig(AspectRatio.NINE_SIXTEEN, OptionVisibility.Hidden)
            ),
            subModeId = SECOND_FAKE_CAPTURE_SUB_MODE_ID,
            isCompatiblePredicate = { settings -> settings.aspectRatio != AspectRatio.ONE_ONE }
        )
        val cameraSystem = createAndInitCameraXCameraSystem(
            appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY,
                aspectRatio = AspectRatio.THREE_FOUR
            ),
            extraCaptureSubModeProviders = mapOf(
                FakeCaptureSubModeFeatureKey to Provider { firstProvider },
                secondaryKey to Provider { secondProvider }
            )
        )

        // Activate first sub-mode -> forces 1:1.
        cameraSystem.setCaptureSubMode(FAKE_CAPTURE_SUB_MODE_ID)
        var settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
        assertThat(settings.activeCaptureSubModeId).isEqualTo(FAKE_CAPTURE_SUB_MODE_ID)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.ONE_ONE)

        // Switch directly to second sub-mode -> first sub-mode's 1:1 override is restored to 3:4
        // before evaluating secondProvider, so secondProvider is accepted and enforces 9:16.
        cameraSystem.setCaptureSubMode(SECOND_FAKE_CAPTURE_SUB_MODE_ID)
        settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureSubModeId).isEqualTo(SECOND_FAKE_CAPTURE_SUB_MODE_ID)
        assertThat(settings.activeCaptureSubModeId).isEqualTo(SECOND_FAKE_CAPTURE_SUB_MODE_ID)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.NINE_SIXTEEN)

        // Return to DEFAULT -> original 3:4 is restored.
        cameraSystem.setCaptureSubMode(CaptureSubModeId.DEFAULT)
        settings = cameraSystem.getCurrentSettings().value!!
        assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
        assertThat(settings.activeCaptureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
        assertThat(settings.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
    }

    @Test
    fun startCamera_withSingleCameraSubMode_transformsCameraSelector(): Unit = runBlocking {
        var transformInvoked = false
        val provider = FakeCaptureSubModeProvider(
            featurePolicy = CameraFeaturePolicy(),
            sessionBinding = CameraSessionBinding.SingleCamera { _, baseSelector ->
                transformInvoked = true
                baseSelector
            }
        )
        val cameraSystem = createAndInitCameraXCameraSystem(
            appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            captureSubModeProvider = provider,
            defaultCaptureSubModes = mapOf(
                CaptureMode.IMAGE_ONLY to FakeCaptureSubModeFeatureKey
            )
        )

        cameraSystem.startCameraAndWaitUntilRunning()
        assertThat(transformInvoked).isTrue()
    }

    @Test
    fun startCamera_withCustomSessionSubMode_invokesCustomRunSession(): Unit = runBlocking {
        val customSessionStarted = CompletableDeferred<Boolean>()
        val provider = FakeCaptureSubModeProvider(
            featurePolicy = CameraFeaturePolicy(),
            sessionBinding = CameraSessionBinding.Custom {
                customSessionStarted.complete(true)
                kotlinx.coroutines.awaitCancellation()
            }
        )
        val cameraSystem = createAndInitCameraXCameraSystem(
            appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                captureMode = CaptureMode.IMAGE_ONLY
            ),
            captureSubModeProvider = provider,
            defaultCaptureSubModes = mapOf(
                CaptureMode.IMAGE_ONLY to FakeCaptureSubModeFeatureKey
            )
        )

        cameraJob = cameraSystemScope.launch { cameraSystem.runCamera() }
        assertThat(withTimeout(GENERAL_TIMEOUT_MS) { customSessionStarted.await() }).isTrue()
    }

    @Test
    fun setCaptureSubMode_fromStandard_whenRejectedAndParentHasDefault_doesNotAdoptParent(): Unit =
        runBlocking {
            val rejectedKey = object : CaptureSubModeFeatureKey {
                override val id = SECOND_FAKE_CAPTURE_SUB_MODE_ID
            }
            val defaultVideoProvider = FakeCaptureSubModeProvider(
                parentCaptureMode = CaptureMode.VIDEO_ONLY,
                subModeId = FAKE_CAPTURE_SUB_MODE_ID
            )
            val rejectedVideoProvider = FakeCaptureSubModeProvider(
                parentCaptureMode = CaptureMode.VIDEO_ONLY,
                subModeId = SECOND_FAKE_CAPTURE_SUB_MODE_ID,
                isCompatiblePredicate = { false }
            )
            val cameraSystem = createAndInitCameraXCameraSystem(
                appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                    captureMode = CaptureMode.STANDARD,
                    aspectRatio = AspectRatio.THREE_FOUR
                ),
                extraCaptureSubModeProviders = mapOf(
                    FakeCaptureSubModeFeatureKey to Provider { defaultVideoProvider },
                    rejectedKey to Provider { rejectedVideoProvider }
                ),
                defaultCaptureSubModes = mapOf(
                    CaptureMode.VIDEO_ONLY to FakeCaptureSubModeFeatureKey
                )
            )

            // Act. Request a rejected VIDEO_ONLY sub-mode while in STANDARD.
            cameraSystem.setCaptureSubMode(SECOND_FAKE_CAPTURE_SUB_MODE_ID)

            // Assert. Parent mode is NOT adopted and VIDEO_ONLY's default override is NOT activated.
            val settings = cameraSystem.getCurrentSettings().value!!
            assertThat(settings.captureMode).isEqualTo(CaptureMode.STANDARD)
            assertThat(settings.captureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.activeCaptureSubModeId).isEqualTo(CaptureSubModeId.DEFAULT)
            assertThat(settings.aspectRatio).isEqualTo(AspectRatio.THREE_FOUR)
        }
}

object FakeImagePostProcessorFeatureKey : ImagePostProcessorFeatureKey

private val FAKE_CAPTURE_SUB_MODE_ID = CaptureSubModeId("fake_sub_mode")
private val SECOND_FAKE_CAPTURE_SUB_MODE_ID = CaptureSubModeId("second_fake_sub_mode")

private object FakeCaptureSubModeFeatureKey : CaptureSubModeFeatureKey {
    override val id = FAKE_CAPTURE_SUB_MODE_ID
}

/**
 * A sub-mode of [parentCaptureMode] that enforces [featurePolicy].
 */
private class FakeCaptureSubModeProvider(
    override val featurePolicy: CameraFeaturePolicy = CameraFeaturePolicy(),
    parentCaptureMode: CaptureMode = CaptureMode.IMAGE_ONLY,
    subModeId: CaptureSubModeId = FAKE_CAPTURE_SUB_MODE_ID,
    private val supportedLenses: Set<LensFacing> = setOf(LensFacing.FRONT, LensFacing.BACK),
    private val isCompatiblePredicate: (CameraAppSettings) -> Boolean = { true },
    override val sessionBinding: CameraSessionBinding =
        CameraSessionBinding.SingleCamera { _, baseSelector -> baseSelector }
) : CaptureSubModeProvider {
    override val descriptor = CaptureSubModeDescriptor(
        id = subModeId,
        parentCaptureMode = parentCaptureMode,
        labelResId = 0
    )

    override suspend fun isSupported(
        context: Context,
        cameraProvider: ProcessCameraProvider,
        cameraInfo: CameraInfo
    ): Boolean = cameraInfo.appLensFacing in supportedLenses

    override fun isCompatibleWith(
        settings: CameraAppSettings,
        systemConstraints: CameraSystemConstraints,
        externalCaptureMode: ExternalCaptureMode
    ): Boolean = super.isCompatibleWith(settings, systemConstraints, externalCaptureMode) &&
        isCompatiblePredicate(settings)
}

class FakeImagePostProcessor(val shouldError: Boolean = false) : ImagePostProcessor {
    var postProcessImageCalled = false
    var savedUri: Uri? = null
    override suspend fun postProcessImage(uri: Uri) {
        postProcessImageCalled = true
        savedUri = uri
        if (shouldError) throw RuntimeException("Post process failed")
    }
}
