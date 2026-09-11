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
package com.google.jetpackcamera.core.camera

import android.app.Application
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.GrantPermissionRule
import com.google.common.truth.Truth.assertWithMessage
import com.google.common.truth.TruthJUnit.assume
import com.google.jetpackcamera.core.camera.CameraSystem.Companion.applyDiffs
import com.google.jetpackcamera.core.camera.utils.APP_REQUIRED_PERMISSIONS
import com.google.jetpackcamera.core.camera.utils.provideUpdatingSurface
import com.google.jetpackcamera.core.common.testing.FakeFilePathGenerator
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.CameraAppSettings
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.produceIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * Instrumented tests for [CameraAppSettings.applyDiffs] executing across all available lenses
 * with the [Parameterized] test runner.
 *
 * Each test runs independently per lens and skips gracefully via [assume] if the target device
 * or lens does not support the setting.
 */
@LargeTest
@RunWith(Parameterized::class)
class CameraSystemApplyDiffsDeviceTest(private val lensFacing: LensFacing) {

    companion object {
        private const val CAMERA_START_TIMEOUT_MS = 10_000L
        private const val GENERAL_TIMEOUT_MS = 3_000L

        /**
         * Provides the camera lens orientations to test.
         */
        @JvmStatic
        @Parameterized.Parameters(name = "lensFacing={0}")
        fun data(): Array<LensFacing> = arrayOf(LensFacing.BACK, LensFacing.FRONT)
    }

    @get:Rule
    val permissionsRule: GrantPermissionRule =
        GrantPermissionRule.grant(*(APP_REQUIRED_PERMISSIONS).toTypedArray())

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val application = context.applicationContext as Application
    private lateinit var cameraSystemScope: CoroutineScope
    private var cameraJob: Job? = null

    @Before
    fun setup() {
        cameraSystemScope = CoroutineScope(Dispatchers.Main)
    }

    @After
    fun tearDown() {
        runBlocking {
            cameraJob?.cancelAndJoin()
        }
        cameraSystemScope.cancel()
    }

    /**
     * Verifies that [applyDiffs] propagates changes to [DynamicRange] to [CameraSystem]
     * for the parameterized lens, or skips if the device/lens does not support HDR video.
     */
    @Test
    fun applyDiffs_dynamicRange_propagatesChange(): Unit = runBlocking {
        // Arrange. Initialize camera in VIDEO_ONLY mode (required for HDR video capture).
        val cameraSystem = createAndInitCameraXCameraSystem(
            appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = lensFacing,
                captureMode = CaptureMode.VIDEO_ONLY,
                dynamicRange = DynamicRange.SDR
            )
        )

        val systemConstraints = cameraSystem.getSystemConstraints().value
        val isLensAvailable = systemConstraints?.availableLenses?.contains(lensFacing) == true
        assume().withMessage("Lens $lensFacing is not available on this device, skipping.")
            .that(isLensAvailable).isTrue()

        val lensConstraints = systemConstraints?.perLensConstraints?.get(lensFacing)
        val supportsHdr = lensConstraints?.supportedDynamicRanges?.contains(
            DynamicRange.HLG10
        ) == true
        assume().withMessage("HDR video (HLG10) is not supported on $lensFacing, skipping.")
            .that(supportsHdr).isTrue()

        cameraSystem.startCameraAndWaitUntilRunning()

        val dynamicRangeCheck = cameraSystem.getCurrentSettings()
            .filterNotNull()
            .map { it.dynamicRange }
            .produceIn(this)

        try {
            dynamicRangeCheck.awaitValue(DynamicRange.SDR)

            // Act: Apply diff with HLG10
            val currentSettings = checkNotNull(cameraSystem.getCurrentSettings().value) {
                "Settings not initialized"
            }
            val newSettings = currentSettings.copy(dynamicRange = DynamicRange.HLG10)
            currentSettings.applyDiffs(newSettings, cameraSystem)

            // Assert: Dynamic range is updated to HLG10
            dynamicRangeCheck.awaitValue(DynamicRange.HLG10)

            // Act: Apply diff back to SDR
            val updatedSettings = checkNotNull(cameraSystem.getCurrentSettings().value) {
                "Settings not initialized"
            }
            val resetSettings = updatedSettings.copy(dynamicRange = DynamicRange.SDR)
            updatedSettings.applyDiffs(resetSettings, cameraSystem)

            // Assert: Dynamic range is reset to SDR
            dynamicRangeCheck.awaitValue(DynamicRange.SDR)
        } finally {
            dynamicRangeCheck.cancel()
        }
    }

    /**
     * Verifies that [applyDiffs] propagates changes to [ImageOutputFormat] to [CameraSystem]
     * for the parameterized lens, or skips if the device/lens does not support Ultra HDR.
     */
    @Test
    fun applyDiffs_imageFormat_propagatesChange(): Unit = runBlocking {
        // Arrange. Initialize camera in IMAGE_ONLY mode (required for Ultra HDR image capture).
        val cameraSystem = createAndInitCameraXCameraSystem(
            appSettings = DEFAULT_CAMERA_APP_SETTINGS.copy(
                cameraLensFacing = lensFacing,
                captureMode = CaptureMode.IMAGE_ONLY,
                imageFormat = ImageOutputFormat.JPEG
            )
        )

        val systemConstraints = cameraSystem.getSystemConstraints().value
        val isLensAvailable = systemConstraints?.availableLenses?.contains(lensFacing) == true
        assume().withMessage("Lens $lensFacing is not available on this device, skipping.")
            .that(isLensAvailable).isTrue()

        val lensConstraints = systemConstraints?.perLensConstraints?.get(lensFacing)
        val supportsUltraHdr =
            lensConstraints?.supportedImageFormatsMap?.get(false)
                ?.contains(ImageOutputFormat.JPEG_ULTRA_HDR) == true
        assume().withMessage("Ultra HDR is not supported on $lensFacing, skipping.")
            .that(supportsUltraHdr).isTrue()

        cameraSystem.startCameraAndWaitUntilRunning()

        val imageFormatCheck = cameraSystem.getCurrentSettings()
            .filterNotNull()
            .map { it.imageFormat }
            .produceIn(this)

        try {
            imageFormatCheck.awaitValue(ImageOutputFormat.JPEG)

            // Act: Apply diff with Ultra HDR
            val currentSettings = checkNotNull(cameraSystem.getCurrentSettings().value) {
                "Settings not initialized"
            }
            val newSettings = currentSettings.copy(imageFormat = ImageOutputFormat.JPEG_ULTRA_HDR)
            currentSettings.applyDiffs(newSettings, cameraSystem)

            // Assert: Image format is updated to JPEG_ULTRA_HDR
            imageFormatCheck.awaitValue(ImageOutputFormat.JPEG_ULTRA_HDR)

            // Act: Apply diff back to JPEG
            val updatedSettings = checkNotNull(cameraSystem.getCurrentSettings().value) {
                "Settings not initialized"
            }
            val resetSettings = updatedSettings.copy(imageFormat = ImageOutputFormat.JPEG)
            updatedSettings.applyDiffs(resetSettings, cameraSystem)

            // Assert: Image format is reset to JPEG
            imageFormatCheck.awaitValue(ImageOutputFormat.JPEG)
        } finally {
            imageFormatCheck.cancel()
        }
    }

    private suspend fun createAndInitCameraXCameraSystem(
        appSettings: CameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
    ) = CameraXCameraSystem(
        application = application,
        defaultDispatcher = Dispatchers.Default,
        iODispatcher = Dispatchers.IO,
        availabilityCheckers = emptyMap(),
        effectProviders = emptyMap(),
        imagePostProcessors = emptyMap(),
        cameraEffectProviders = emptyMap(),
        filePathGenerator = FakeFilePathGenerator()
    ).apply {
        initialize(appSettings) {}
        providePreviewSurface()
    }

    private suspend fun <T> ReceiveChannel<T>.awaitValue(
        expectedValue: T,
        timeoutMs: Long = GENERAL_TIMEOUT_MS
    ) {
        val found = withTimeoutOrNull(timeoutMs) {
            for (value in this@awaitValue) {
                if (value == expectedValue) return@withTimeoutOrNull true
            }
            false
        } ?: false
        assertWithMessage("Expected value $expectedValue was not received within ${timeoutMs}ms")
            .that(found)
            .isTrue()
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
        val cameraStarted = withTimeoutOrNull(CAMERA_START_TIMEOUT_MS) {
            getCurrentCameraState().filterNotNull().first {
                it.isCameraRunning
            }
        } != null
        assertWithMessage("Camera timed out while starting.").that(cameraStarted).isTrue()
        instrumentation.waitForIdleSync()
    }
}
