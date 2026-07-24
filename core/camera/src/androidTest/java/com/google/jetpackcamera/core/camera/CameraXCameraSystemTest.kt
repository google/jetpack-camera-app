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
import android.net.Uri
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
import com.google.jetpackcamera.core.camera.postprocess.PostProcessModule.Companion.provideImagePostProcessorMap
import com.google.jetpackcamera.core.camera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.core.camera.utils.provideUpdatingSurface
import com.google.jetpackcamera.core.common.testing.FakeFilePathGenerator
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.Illuminant
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
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
    private val context = instrumentation.context
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
        cameraSystem.takePicture(context.contentResolver, SaveLocation.Default) {}

        // Assert.
        assertThat(imagePostProcessor.postProcessImageCalled).isTrue()
        assertThat(imagePostProcessor.savedContentResolver).isEqualTo(contentResolver)
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
            ) {}
        } catch (e: Exception) {}

        // Assert.
        assertThat(imagePostProcessor.postProcessImageCalled).isFalse()
        assertThat(imagePostProcessor.savedContentResolver).isNull()
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
            cameraSystem.takePicture(context.contentResolver, SaveLocation.Default) {}
        } catch (e: RuntimeException) {
            // Assert.
            assertThat(imagePostProcessor.postProcessImageCalled).isTrue()
            assertThat(imagePostProcessor.savedContentResolver).isEqualTo(contentResolver)

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
        cameraSystem.takePicture(context.contentResolver, SaveLocation.Default) {}

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
        fakeImagePostProcessor: FakeImagePostProcessor? = null
    ) = CameraXCameraSystem(
        application = application,
        defaultDispatcher = Dispatchers.Default,
        iODispatcher = Dispatchers.IO,
        availabilityCheckers = emptyMap(),
        effectProviders = emptyMap(),
        imagePostProcessors = getFakePostProcessorMap(fakeImagePostProcessor),
        filePathGenerator = FakeFilePathGenerator()
    ).apply {
        initialize(appSettings) {}
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
}

object FakeImagePostProcessorFeatureKey : ImagePostProcessorFeatureKey

class FakeImagePostProcessor(val shouldError: Boolean = false) : ImagePostProcessor {
    var postProcessImageCalled = false
    var savedUri: Uri? = null
    var savedContentResolver: ContentResolver? = null
    override suspend fun postProcessImage(uri: Uri, contentResolver: ContentResolver) {
        postProcessImageCalled = true
        savedUri = uri
        savedContentResolver = contentResolver
        if (shouldError) throw RuntimeException("Post process failed")
    }
}
