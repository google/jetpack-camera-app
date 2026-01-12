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
import android.graphics.SurfaceTexture
import android.net.Uri
import android.util.Log
import android.view.Surface
import androidx.annotation.GuardedBy
import androidx.concurrent.futures.DirectExecutor
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.core.camera.CameraXCameraSystemTest.Feature.DYNAMIC_RANGE_HLG10
import com.google.jetpackcamera.core.camera.CameraXCameraSystemTest.Feature.FPS_60
import com.google.jetpackcamera.core.camera.CameraXCameraSystemTest.Feature.IMAGE_FORMAT_JPEG_ULTRA_HDR
import com.google.jetpackcamera.core.camera.CameraXCameraSystemTest.Feature.STABILIZATION_MODE_ON
import com.google.jetpackcamera.core.camera.CameraXCameraSystemTest.Feature.STREAM_CONFIG_SINGLE
import com.google.jetpackcamera.core.camera.CameraXCameraSystemTest.Feature.VIDEO_QUALITY_UHD
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent.OnVideoRecordError
import com.google.jetpackcamera.core.camera.OnVideoRecordEvent.OnVideoRecorded
import com.google.jetpackcamera.core.camera.postprocess.ImagePostProcessor
import com.google.jetpackcamera.core.camera.postprocess.ImagePostProcessorFeatureKey
import com.google.jetpackcamera.core.camera.postprocess.PostProcessModule.Companion.provideImagePostProcessorMap
import com.google.jetpackcamera.core.camera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.core.common.FakeFilePathGenerator
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.Illuminant
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.model.SaveLocation
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.StreamConfig
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.settings.ConstraintsRepository
import com.google.jetpackcamera.settings.SettableConstraintsRepository
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.CameraConstraints
import com.google.jetpackcamera.settings.model.CameraSystemConstraints
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import java.io.File
import java.util.AbstractMap
import javax.inject.Provider
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
        private const val TAG = "CameraXCameraSystemTest"
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
    fun recordVideoWithFlashModeOn_shouldEnableTorch(): Unit = runBlocking {
        // Arrange.
        val lensFacing = LensFacing.BACK
        val constraintsRepository = SettableConstraintsRepositoryImpl()
        val cameraSystem = createAndInitCameraXCameraSystem(
            constraintsRepository = constraintsRepository
        )
        assume().withMessage("No flash unit, skip the test.")
            .that(constraintsRepository.hasFlashUnit(lensFacing)).isTrue()
        cameraSystem.startCameraAndWaitUntilRunning()

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

    @Test
    fun setMultipleFeatures_systemConstraintsUpdatedAndFeaturesSetIfSupported() = runBlocking {
        // TODO: Add STREAM_CONFIG_SINGLE to the featuresToTest list. This currently leads to flaky
        //  crashes due to some camera effect related surface not being cleaned up properly somehow.
        //  This doesn't seem to be related to the primary purpose of this test, so simply excluding
        //  it for now.
        val featuresToTest = listOf(
            DYNAMIC_RANGE_HLG10,
            FPS_60,
            VIDEO_QUALITY_UHD
        )

        featuresToTest.permutations().forEach { orderedFeatures ->
            Log.d(TAG, "Testing $orderedFeatures")

            // Setup
            val constraintsRepository = ObservableConstraintsRepository()
            val cameraSystem = createAndInitCameraXCameraSystem(
                constraintsRepository = constraintsRepository
            )

            // Initial run: each camera run/update should lead to a new systemConstraints update
            var currentConstraints = constraintsRepository.observeNextUpdate().let {
                cameraSystem.startCameraAndWaitUntilRunning()
                it.awaitUntil()
            }

            val lensFacing =
                requireNotNull(cameraSystem.getCurrentSettings().value?.cameraLensFacing)

            orderedFeatures.forEach { feature ->
                currentConstraints = when (feature) {
                    DYNAMIC_RANGE_HLG10 -> feature.tryApplyFeature(
                        expectedValue = DynamicRange.HLG10,
                        lensFacing = lensFacing,
                        cameraSystemConstraints = currentConstraints,
                        constraintsRepository = constraintsRepository,
                        cameraSystem = cameraSystem,
                        setFeature = { cameraSystem.setDynamicRange(DynamicRange.HLG10) },
                        getNewFeatureValue = { it?.dynamicRange }
                    ) { constraints ->
                        constraints
                            ?.supportedDynamicRanges
                            ?.contains(DynamicRange.HLG10) == true
                    }

                    FPS_60 -> feature.tryApplyFeature(
                        expectedValue = 60,
                        lensFacing = lensFacing,
                        cameraSystemConstraints = currentConstraints,
                        constraintsRepository = constraintsRepository,
                        cameraSystem = cameraSystem,
                        setFeature = { cameraSystem.setTargetFrameRate(60) },
                        getNewFeatureValue = { it?.targetFrameRate }
                    ) { constraints ->
                        constraints
                            ?.supportedFixedFrameRates
                            ?.contains(60) == true
                    }

                    VIDEO_QUALITY_UHD -> feature.tryApplyFeature(
                        expectedValue = VideoQuality.UHD,
                        lensFacing = lensFacing,
                        cameraSystemConstraints = currentConstraints,
                        constraintsRepository = constraintsRepository,
                        cameraSystem = cameraSystem,
                        setFeature = { cameraSystem.setVideoQuality(VideoQuality.UHD) },
                        getNewFeatureValue = { it?.videoQuality }
                    ) { constraints ->
                        constraints
                            ?.supportedVideoQualitiesMap
                            ?.get(cameraSystem.getCurrentSettings().value?.dynamicRange)
                            ?.contains(VideoQuality.UHD) == true
                    }

                    STABILIZATION_MODE_ON -> feature.tryApplyFeature(
                        expectedValue = StabilizationMode.ON,
                        lensFacing = lensFacing,
                        cameraSystemConstraints = currentConstraints,
                        constraintsRepository = constraintsRepository,
                        cameraSystem = cameraSystem,
                        setFeature = { cameraSystem.setStabilizationMode(StabilizationMode.ON) },
                        getNewFeatureValue = { it?.stabilizationMode }
                    ) { constraints ->
                        constraints
                            ?.supportedStabilizationModes
                            ?.contains(StabilizationMode.ON) == true
                    }

                    IMAGE_FORMAT_JPEG_ULTRA_HDR -> feature.tryApplyFeature(
                        expectedValue = ImageOutputFormat.JPEG_ULTRA_HDR,
                        lensFacing = lensFacing,
                        cameraSystemConstraints = currentConstraints,
                        constraintsRepository = constraintsRepository,
                        cameraSystem = cameraSystem,
                        setFeature = {
                            cameraSystem.setImageFormat(
                                ImageOutputFormat.JPEG_ULTRA_HDR
                            )
                        },
                        getNewFeatureValue = { it?.imageFormat }
                    ) { constraints ->
                        constraints
                            ?.supportedImageFormatsMap
                            ?.get(cameraSystem.getCurrentSettings().value?.streamConfig)
                            ?.contains(ImageOutputFormat.JPEG_ULTRA_HDR) == true
                    }

                    STREAM_CONFIG_SINGLE -> feature.tryApplyFeature(
                        expectedValue = StreamConfig.SINGLE_STREAM,
                        lensFacing = lensFacing,
                        cameraSystemConstraints = currentConstraints,
                        constraintsRepository = constraintsRepository,
                        cameraSystem = cameraSystem,
                        setFeature = { cameraSystem.setStreamConfig(StreamConfig.SINGLE_STREAM) },
                        getNewFeatureValue = { it?.streamConfig }
                    ) { constraints ->
                        constraints
                            ?.supportedStreamConfigs
                            ?.contains(StreamConfig.SINGLE_STREAM) == true
                    }
                }
            }
        }
    }

    suspend fun <T> Deferred<T>.awaitUntil(timeout: Duration = 2.seconds): T {
        return withTimeout(timeout) {
            await()
        }
    }

    private suspend fun createAndInitCameraXCameraSystem(
        appSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
        constraintsRepository: SettableConstraintsRepository = SettableConstraintsRepositoryImpl(),
        fakeImagePostProcessor: FakeImagePostProcessor? = null
    ) = CameraXCameraSystem(
        application = application,
        defaultDispatcher = Dispatchers.Default,
        iODispatcher = Dispatchers.IO,
        constraintsRepository = constraintsRepository,
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
                        it.elapsedTimeNanos
                            .toDuration(DurationUnit.NANOSECONDS).inWholeMilliseconds
                    )
                }
            }.first { elapsedTimeMs ->
                elapsedTimeMs >= RECORDING_START_DURATION_MS
            }
        }
    }

    private fun CameraXCameraSystem.providePreviewSurface() {
        cameraSystemScope.launch {
            getSurfaceRequest().filterNotNull().first().let { surfaceRequest ->
                val surfaceTexture = SurfaceTexture(0)
                surfaceTexture.setDefaultBufferSize(640, 480)
                val surface = Surface(surfaceTexture)
                surfaceRequest.provideSurface(surface, DirectExecutor.INSTANCE) {
                    surface.release()
                    surfaceTexture.release()
                }
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

    private suspend fun ConstraintsRepository.hasFlashUnit(lensFacing: LensFacing): Boolean =
        Illuminant.FLASH_UNIT in
            systemConstraints.first()!!.perLensConstraints[lensFacing]!!.supportedIlluminants

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

    suspend fun <T> Feature.tryApplyFeature(
        expectedValue: T,
        lensFacing: LensFacing,
        cameraSystemConstraints: CameraSystemConstraints,
        constraintsRepository: ObservableConstraintsRepository,
        cameraSystem: CameraSystem,
        setFeature: suspend () -> Unit,
        getNewFeatureValue: (CameraAppSettings?) -> T?,
        isSupported: (CameraConstraints?) -> Boolean
    ): CameraSystemConstraints {
        // Check support
        if (!isSupported(cameraSystemConstraints.perLensConstraints[lensFacing])) {
            Log.d(TAG, "Skipping $this: Not supported by current constraints.")
            return cameraSystemConstraints
        }

        Log.d(TAG, "Applying $this...")

        // Prepare observer
        val nextUpdate = constraintsRepository.observeNextUpdate()

        setFeature()

        // Wait to verify constraints is updated
        val newConstraints = nextUpdate.awaitUntil()

        // Verify feature is set according to current settings
        assertThat(getNewFeatureValue(cameraSystem.getCurrentSettings().value)).isEqualTo(
            expectedValue
        )

        return newConstraints
    }

    fun <T> List<T>.permutations(): List<List<T>> {
        if (isEmpty()) {
            // Base case: an empty list has one permutation (the empty list itself)
            return listOf(emptyList())
        }

        val result = mutableListOf<List<T>>()
        val head = first() // Take the first element
        val tail = drop(1) // Get the rest of the list

        // Recursively get permutations of the tail
        tail.permutations().forEach { permOfTail ->
            // Insert the head element at all possible positions in each permutation of the tail
            for (i in 0..permOfTail.size) {
                val newPerm = permOfTail.toMutableList()
                newPerm.add(i, head)
                result.add(newPerm)
            }
        }
        return result
    }

    enum class Feature {
        DYNAMIC_RANGE_HLG10,
        FPS_60,
        VIDEO_QUALITY_UHD,
        STABILIZATION_MODE_ON,
        IMAGE_FORMAT_JPEG_ULTRA_HDR,
        STREAM_CONFIG_SINGLE
    }
}

object FakeImagePostProcessorFeatureKey : ImagePostProcessorFeatureKey

class FakeImagePostProcessor(val shouldError: Boolean = false) : ImagePostProcessor {
    var postProcessImageCalled = false
    var savedUri: Uri? = null
    override suspend fun postProcessImage(uri: Uri) {
        postProcessImageCalled = true
        savedUri = uri
        if (shouldError) throw RuntimeException("Post process failed")
    }
}

class ObservableConstraintsRepository : SettableConstraintsRepository {
    private val lock = Object()

    override val systemConstraints: StateFlow<CameraSystemConstraints?> =
        MutableStateFlow(null)

    @GuardedBy("lock")
    private var updateDeferredList =
        mutableListOf<CompletableDeferred<CameraSystemConstraints>>()

    override fun updateSystemConstraints(systemConstraints: CameraSystemConstraints) {
        synchronized(lock) {
            updateDeferredList.forEach {
                it.complete(systemConstraints)
            }
            updateDeferredList.clear()
        }
    }

    fun observeNextUpdate(): Deferred<CameraSystemConstraints> {
        return synchronized(lock) {
            val deferred = CompletableDeferred<CameraSystemConstraints>()
            updateDeferredList.add(deferred)
            deferred
        }
    }
}
