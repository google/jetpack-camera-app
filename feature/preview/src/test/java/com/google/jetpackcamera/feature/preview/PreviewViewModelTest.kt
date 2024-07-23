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
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.core.camera.test.FakeCameraUseCase
import com.google.jetpackcamera.settings.SettableConstraintsRepositoryImpl
import com.google.jetpackcamera.settings.model.FlashMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.TYPICAL_SYSTEM_CONSTRAINTS
import com.google.jetpackcamera.settings.test.FakeSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {

    private val cameraUseCase = FakeCameraUseCase()
    private val constraintsRepository = SettableConstraintsRepositoryImpl().apply {
        updateSystemConstraints(TYPICAL_SYSTEM_CONSTRAINTS)
    }
    private lateinit var previewViewModel: PreviewViewModel

    @Before
    fun setup() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher())
        previewViewModel = PreviewViewModel(
            PreviewMode.StandardMode {},
            cameraUseCase = cameraUseCase,
            constraintsRepository = constraintsRepository,
            settingsRepository = FakeSettingsRepository
        )
        advanceUntilIdle()
    }

    @Test
    fun getPreviewUiState() = runTest(StandardTestDispatcher()) {
        advanceUntilIdle()
        val uiState = previewViewModel.previewUiState.value
        assertThat(uiState).isInstanceOf(PreviewUiState.Ready::class.java)
    }

    @Test
    fun runCamera() = runTest(StandardTestDispatcher()) {
        previewViewModel.startCameraUntilRunning()

        assertThat(cameraUseCase.previewStarted).isTrue()
    }

    @Test
    fun captureImage() = runTest(StandardTestDispatcher()) {
        previewViewModel.startCameraUntilRunning()
        previewViewModel.captureImage()
        advanceUntilIdle()
        assertThat(cameraUseCase.numPicturesTaken).isEqualTo(1)
    }

    @Test
    fun captureImageWithUri() = runTest(StandardTestDispatcher()) {
        val contentResolver: ContentResolver = mock()
        previewViewModel.startCameraUntilRunning()
        previewViewModel.captureImageWithUri(contentResolver, null) {}
        advanceUntilIdle()
        assertThat(cameraUseCase.numPicturesTaken).isEqualTo(1)
    }

    @Test
    fun startVideoRecording() = runTest(StandardTestDispatcher()) {
        previewViewModel.startCameraUntilRunning()
        previewViewModel.startVideoRecording(null, true) {}
        advanceUntilIdle()
        assertThat(cameraUseCase.recordingInProgress).isTrue()
    }

    @Test
    fun stopVideoRecording() = runTest(StandardTestDispatcher()) {
        previewViewModel.startCameraUntilRunning()
        previewViewModel.startVideoRecording(null, true) {}
        advanceUntilIdle()
        previewViewModel.stopVideoRecording()
        assertThat(cameraUseCase.recordingInProgress).isFalse()
    }

    @Test
    fun setFlash() = runTest(StandardTestDispatcher()) {
        previewViewModel.startCamera()
        previewViewModel.setFlash(FlashMode.AUTO)
        advanceUntilIdle()

        assertIsReady(previewViewModel.previewUiState.value).also {
            assertThat(it.currentCameraSettings.flashMode).isEqualTo(FlashMode.AUTO)
        }
    }

    @Test
    fun flipCamera() = runTest(StandardTestDispatcher()) {
        // initial default value should be back
        previewViewModel.startCamera()
        assertIsReady(previewViewModel.previewUiState.value).also {
            assertThat(it.currentCameraSettings.cameraLensFacing).isEqualTo(LensFacing.BACK)
        }
        previewViewModel.setLensFacing(LensFacing.FRONT)

        advanceUntilIdle()
        // ui state and camera should both be true now
        assertIsReady(previewViewModel.previewUiState.value).also {
            assertThat(it.currentCameraSettings.cameraLensFacing).isEqualTo(LensFacing.FRONT)
        }
        assertThat(cameraUseCase.isLensFacingFront).isTrue()
    }

    context(TestScope)
    private fun PreviewViewModel.startCameraUntilRunning() {
        startCamera()
        advanceUntilIdle()
    }
}

private fun assertIsReady(previewUiState: PreviewUiState): PreviewUiState.Ready {
    return when (previewUiState) {
        is PreviewUiState.Ready -> previewUiState
        else -> throw AssertionError(
            "PreviewUiState expected to be Ready, but was ${previewUiState::class}"
        )
    }
}
