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
package com.google.jetpackcamera.core.camera.test

import com.google.common.truth.Truth
import com.google.jetpackcamera.core.camera.CameraUseCase
import com.google.jetpackcamera.settings.model.DEFAULT_CAMERA_APP_SETTINGS
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

@OptIn(ExperimentalCoroutinesApi::class)
class FakeCameraUseCaseTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    private val cameraUseCase = FakeCameraUseCase(testScope)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun canInitialize() = runTest(testDispatcher) {
        cameraUseCase.initialize(
            cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
            useCaseMode = CameraUseCase.UseCaseMode.STANDARD
        )
    }

    @Test
    fun canRunCamera() = runTest(testDispatcher) {
        initAndRunCamera()
        Truth.assertThat(cameraUseCase.isPreviewStarted())
    }

    @Test
    fun screenFlashDisabled_whenFlashModeOffAndFrontCamera() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraUseCase.setLensFacing(lensFacing = LensFacing.FRONT)
        cameraUseCase.setFlashMode(flashMode = FlashMode.OFF)
        advanceUntilIdle()

        Truth.assertThat(cameraUseCase.isScreenFlashEnabled()).isFalse()
    }

    @Test
    fun screenFlashDisabled_whenFlashModeOnAndNotFrontCamera() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraUseCase.setLensFacing(lensFacing = LensFacing.BACK)
        cameraUseCase.setFlashMode(flashMode = FlashMode.ON)
        advanceUntilIdle()

        Truth.assertThat(cameraUseCase.isScreenFlashEnabled()).isFalse()
    }

    @Test
    fun screenFlashDisabled_whenFlashModeAutoAndNotFrontCamera() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraUseCase.setLensFacing(lensFacing = LensFacing.BACK)
        cameraUseCase.setFlashMode(flashMode = FlashMode.AUTO)
        advanceUntilIdle()

        Truth.assertThat(cameraUseCase.isScreenFlashEnabled()).isFalse()
    }

    @Test
    fun screenFlashEnabled_whenFlashModeOnAndFrontCamera() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraUseCase.setLensFacing(lensFacing = LensFacing.FRONT)
        cameraUseCase.setFlashMode(flashMode = FlashMode.ON)
        advanceUntilIdle()

        Truth.assertThat(cameraUseCase.isScreenFlashEnabled()).isTrue()
    }

    @Test
    fun screenFlashEnabled_whenFlashModeAutoAndFrontCamera() = runTest(testDispatcher) {
        initAndRunCamera()

        cameraUseCase.setLensFacing(lensFacing = LensFacing.FRONT)
        cameraUseCase.setFlashMode(flashMode = FlashMode.AUTO)
        advanceUntilIdle()

        Truth.assertThat(cameraUseCase.isScreenFlashEnabled()).isTrue()
    }

    @Test
    fun captureScreenFlashImage_screenFlashEventsEmittedInCorrectSequence() = runTest(
        testDispatcher
    ) {
        initAndRunCamera()
        val events = mutableListOf<CameraUseCase.ScreenFlashEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraUseCase.getScreenFlashEvents().toList(events)
        }

        // FlashMode.ON in front facing camera automatically enables screen flash
        cameraUseCase.setLensFacing(lensFacing = LensFacing.FRONT)
        cameraUseCase.setFlashMode(FlashMode.ON)
        advanceUntilIdle()
        cameraUseCase.takePicture()

        advanceUntilIdle()
        Truth.assertThat(events.map { it.type }).containsExactlyElementsIn(
            listOf(
                CameraUseCase.ScreenFlashEvent.Type.APPLY_UI,
                CameraUseCase.ScreenFlashEvent.Type.CLEAR_UI
            )
        ).inOrder()
    }

    private fun TestScope.initAndRunCamera() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            cameraUseCase.initialize(
                cameraAppSettings = DEFAULT_CAMERA_APP_SETTINGS,
                useCaseMode = CameraUseCase.UseCaseMode.STANDARD
            )
            cameraUseCase.runCamera()
        }
    }
}
