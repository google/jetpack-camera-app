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


import androidx.camera.core.Preview.SurfaceProvider
import com.google.jetpackcamera.domain.camera.CameraUseCase
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations

@OptIn(ExperimentalCoroutinesApi::class)
class PreviewViewModelTest {

    @Mock private lateinit var mockCameraUseCase : CameraUseCase
    private lateinit var previewViewModel : PreviewViewModel

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        Dispatchers.setMain(StandardTestDispatcher())
        previewViewModel = PreviewViewModel(mockCameraUseCase)
    }

    @Test
    fun getPreviewUiState() = runTest(StandardTestDispatcher()) {
        var uiState = previewViewModel.previewUiState.value
        assertEquals(CameraState.NOT_READY, uiState.cameraState)
        advanceUntilIdle()
        uiState = previewViewModel.previewUiState.value
        assertEquals(CameraState.READY, uiState.cameraState)
    }

    @Test
    fun runCamera() = runTest(StandardTestDispatcher()) {
        val surfaceProvider : SurfaceProvider = mock()
        previewViewModel.runCamera(surfaceProvider)
        advanceUntilIdle()
        val previewUiState = previewViewModel.previewUiState.value

        verify(mockCameraUseCase).runCamera(
            surfaceProvider,
            previewUiState.lensFacing
        )
    }

    @Test
    fun flipCamera() {
        // TODO(yasith)
    }
}