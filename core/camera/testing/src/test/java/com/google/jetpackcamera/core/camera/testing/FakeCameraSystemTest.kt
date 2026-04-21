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
package com.google.jetpackcamera.core.camera.testing

import android.graphics.SurfaceTexture
import android.view.Surface
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.CameraSystem
import com.google.jetpackcamera.core.camera.PreviewSurfaceRequest
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class FakeCameraSystemTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    private val cameraSystem = FakeCameraSystem()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun canRunCamera() = runTest(testDispatcher) {
        cameraSystem.initialize(DEFAULT_CAMERA_APP_SETTINGS) {}
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraSystem.runCamera()
        }
        advanceUntilIdle()

        // Fulfill surface request to let runCamera continue
        val request = cameraSystem.getSurfaceRequest().filterNotNull().first()
        (request as PreviewSurfaceRequest.Viewfinder).surfaceDeferred.complete(
            Surface(SurfaceTexture(1))
        )

        advanceUntilIdle()
        assertThat(cameraSystem.isPreviewStarted()).isTrue()
        job.cancel()
    }

    @Test
    fun surfaceRequest_emitsAndViewfinderFulfills() = runTest(testDispatcher) {
        cameraSystem.initialize(DEFAULT_CAMERA_APP_SETTINGS) {}
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraSystem.runCamera()
        }
        advanceUntilIdle()

        val request = cameraSystem.getSurfaceRequest().filterNotNull().first()
        assertThat(request).isInstanceOf(PreviewSurfaceRequest.Viewfinder::class.java)
        val viewfinderRequest = request as PreviewSurfaceRequest.Viewfinder

        // Camera should not be running yet
        assertThat(cameraSystem.getCurrentCameraState().value.isCameraRunning).isFalse()

        // Provide surface
        val surface = Surface(SurfaceTexture(1))
        viewfinderRequest.surfaceDeferred.complete(surface)

        advanceUntilIdle()

        // Now camera should be running
        assertThat(cameraSystem.getCurrentCameraState().value.isCameraRunning).isTrue()
        job.cancel()
    }

    @Test
    fun canSetLensFacing() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraSystem.setLensFacing(lensFacing = LensFacing.FRONT)
        advanceUntilIdle()

        assertThat(cameraSystem.getCurrentSettings().value?.cameraLensFacing)
            .isEqualTo(LensFacing.FRONT)
    }

    @Test
    fun canSetFlashMode() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraSystem.setFlashMode(flashMode = FlashMode.ON)
        advanceUntilIdle()

        assertThat(cameraSystem.getCurrentSettings().value?.flashMode)
            .isEqualTo(FlashMode.ON)
    }

    @Test
    fun screenFlashEnabled_whenFlashModeOnAndFrontCamera() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraSystem.setLensFacing(lensFacing = LensFacing.FRONT)
        cameraSystem.setFlashMode(flashMode = FlashMode.ON)
        advanceUntilIdle()

        val events = mutableListOf<CameraSystem.ScreenFlashEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraSystem.getScreenFlashEvents().consumeAsFlow().toList(events)
        }

        cameraSystem.takePicture {}
        advanceUntilIdle()

        assertThat(events.map { it.type }).contains(CameraSystem.ScreenFlashEvent.Type.APPLY_UI)
        job.cancel()
    }

    @Test
    fun screenFlashDisabled_whenFlashModeOffAndFrontCamera() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraSystem.setLensFacing(lensFacing = LensFacing.FRONT)
        cameraSystem.setFlashMode(flashMode = FlashMode.OFF)
        advanceUntilIdle()

        val events = mutableListOf<CameraSystem.ScreenFlashEvent>()
        val job = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraSystem.getScreenFlashEvents().consumeAsFlow().toList(events)
        }

        cameraSystem.takePicture {}
        advanceUntilIdle()

        assertThat(events.map { it.type })
            .doesNotContain(CameraSystem.ScreenFlashEvent.Type.APPLY_UI)
        job.cancel()
    }

    private suspend fun TestScope.initAndRunCamera() {
        cameraSystem.initialize(
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS
        ) {}
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraSystem.runCamera()
        }
        advanceUntilIdle()
        val request = cameraSystem.getSurfaceRequest().filterNotNull().first()
        (request as PreviewSurfaceRequest.Viewfinder).surfaceDeferred.complete(
            Surface(SurfaceTexture(1))
        )
        advanceUntilIdle()
    }
}
