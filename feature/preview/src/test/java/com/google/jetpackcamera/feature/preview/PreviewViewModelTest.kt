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
import com.google.jetpackcamera.domain.camera.test.FakeCameraUseCase
import com.google.jetpackcamera.settings.model.FlashMode
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
    private lateinit var previewViewModel: PreviewViewModel

    @Before
    fun setup() = runTest(StandardTestDispatcher()) {
        Dispatchers.setMain(StandardTestDispatcher())
        previewViewModel = PreviewViewModel(cameraUseCase)
        advanceUntilIdle()
    }

    @Test
    fun getPreviewUiState() = runTest(StandardTestDispatcher()) {
        advanceUntilIdle()
        val uiState = previewViewModel.previewUiState.value
        assertThat(uiState.cameraState).isEqualTo(CameraState.READY)
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
        previewViewModel.startVideoRecording()
        advanceUntilIdle()
        assertThat(cameraUseCase.recordingInProgress).isTrue()
    }

    @Test
    fun stopVideoRecording() = runTest(StandardTestDispatcher()) {
        previewViewModel.startCameraUntilRunning()
        previewViewModel.startVideoRecording()
        advanceUntilIdle()
        previewViewModel.stopVideoRecording()
        assertThat(cameraUseCase.recordingInProgress).isFalse()
    }

    @Test
    fun setFlash() = runTest(StandardTestDispatcher()) {
        previewViewModel.startCamera()
        previewViewModel.setFlash(FlashMode.AUTO)
        advanceUntilIdle()
        assertThat(previewViewModel.previewUiState.value.currentCameraSettings.flashMode)
            .isEqualTo(FlashMode.AUTO)
    }

    @Test
    fun flipCamera() = runTest(StandardTestDispatcher()) {
        // initial default value should be back
        previewViewModel.startCamera()
        assertThat(previewViewModel.previewUiState.value.currentCameraSettings.isFrontCameraFacing)
            .isFalse()
        previewViewModel.flipCamera()

        advanceUntilIdle()
        // ui state and camera should both be true now
        assertThat(previewViewModel.previewUiState.value.currentCameraSettings.isFrontCameraFacing)
            .isTrue()
        assertThat(cameraUseCase.isLensFacingFront).isTrue()
    }

    context(TestScope)
    private fun PreviewViewModel.startCameraUntilRunning() {
        startCamera()
        advanceUntilIdle()
    }
}
