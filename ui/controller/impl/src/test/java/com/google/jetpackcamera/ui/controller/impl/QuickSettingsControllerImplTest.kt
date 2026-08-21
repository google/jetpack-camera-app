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
package com.google.jetpackcamera.ui.controller.impl

import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.testing.FakeCameraSystem
import com.google.jetpackcamera.model.AspectRatio
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.DynamicRange
import com.google.jetpackcamera.model.FlashMode
import com.google.jetpackcamera.model.ImageOutputFormat
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.capture.TrackedCaptureUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(JUnit4::class)
internal class QuickSettingsControllerImplTest {
    private val testScope = TestScope()
    private val testDispatcher = StandardTestDispatcher(testScope.testScheduler)

    private val cameraSystem = FakeCameraSystem()
    private val trackedCaptureUiState = MutableStateFlow(TrackedCaptureUiState())
    private lateinit var controller: QuickSettingsControllerImpl

    @Before
    fun setup() {
        controller = QuickSettingsControllerImpl(
            trackedCaptureUiState = trackedCaptureUiState,
            cameraSystem = cameraSystem,
            coroutineContext = testDispatcher
        )
    }

    @Test
    fun toggleQuickSettings_mutatesUiState() = testScope.runTest {
        val initialValue = trackedCaptureUiState.value.isQuickSettingsOpen
        controller.toggleQuickSettings()
        assertThat(trackedCaptureUiState.value.isQuickSettingsOpen).isEqualTo(!initialValue)
    }

    @Test
    fun setLensFacing_mutatesCameraSystem() = testScope.runTest {
        controller.setLensFacing(LensFacing.FRONT)
        advanceUntilIdle()
        assertThat(
            cameraSystem.getCurrentSettings().value?.cameraLensFacing
        ).isEqualTo(LensFacing.FRONT)
    }

    @Test
    fun setFlash_mutatesCameraSystem() = testScope.runTest {
        controller.setFlash(FlashMode.ON)
        advanceUntilIdle()
        assertThat(cameraSystem.getCurrentSettings().value?.flashMode).isEqualTo(FlashMode.ON)
    }

    @Test
    fun setAspectRatio_mutatesCameraSystem() = testScope.runTest {
        controller.setAspectRatio(AspectRatio.ONE_ONE)
        advanceUntilIdle()
        assertThat(
            cameraSystem.getCurrentSettings().value?.aspectRatio
        ).isEqualTo(AspectRatio.ONE_ONE)
    }

    @Test
    fun setDynamicRange_mutatesCameraSystem() = testScope.runTest {
        controller.setDynamicRange(DynamicRange.HLG10)
        advanceUntilIdle()
        assertThat(
            cameraSystem.getCurrentSettings().value?.dynamicRange
        ).isEqualTo(DynamicRange.HLG10)
    }

    @Test
    fun setImageFormat_mutatesCameraSystem() = testScope.runTest {
        controller.setImageFormat(ImageOutputFormat.JPEG_ULTRA_HDR)
        advanceUntilIdle()
        assertThat(
            cameraSystem.getCurrentSettings().value?.imageFormat
        ).isEqualTo(ImageOutputFormat.JPEG_ULTRA_HDR)
    }

    @Test
    fun setCaptureMode_mutatesCameraSystem() = testScope.runTest {
        controller.setCaptureMode(CaptureMode.VIDEO_ONLY)
        advanceUntilIdle()
        assertThat(
            cameraSystem.getCurrentSettings().value?.captureMode
        ).isEqualTo(CaptureMode.VIDEO_ONLY)
    }

    @Test
    fun cancelScope_cancelsCoroutineScope() = testScope.runTest {
        val job = controller.cancelScope()
        job.join()
        assertThat(job.isCancelled).isTrue()
    }
}
