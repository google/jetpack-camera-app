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

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.camera.core.Preview.SurfaceProvider
import com.google.jetpackcamera.data.camera.CameraRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * [ViewModel] for [PreviewScreen].
 */
@HiltViewModel
class PreviewViewModel @Inject constructor(
    private val cameraRepository: CameraRepository
) : ViewModel() {

    private val _previewUiState: MutableStateFlow<PreviewUiState> =
        MutableStateFlow(PreviewUiState())
    val previewUiState: StateFlow<PreviewUiState> = _previewUiState

    init {
        initializeCamera()
    }

    private fun initializeCamera() {
        viewModelScope.launch {
            cameraRepository.initialize()
            _previewUiState.emit(
                previewUiState.value.copy(
                    cameraState = CameraState.READY
                )
            )
        }
    }

    fun startPreview(
        lifecycleOwner: LifecycleOwner,
        surfaceProvider: SurfaceProvider
    ) {
        cameraRepository.startPreview(
            lifecycleOwner,
            surfaceProvider,
            previewUiState.value.lensFacing
        )
    }

    fun flipCamera() {
        // TODO(yasith)
    }
}