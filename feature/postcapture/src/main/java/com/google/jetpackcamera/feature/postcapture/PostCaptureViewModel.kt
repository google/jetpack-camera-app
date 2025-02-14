/*
 * Copyright (C) 2025 The Android Open Source Project
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

package com.google.jetpackcamera.feature.postcapture

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class PostCaptureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(PostCaptureUiState(
//        lastCapturedImagePath = "/storage/emulated/0/Pictures/JCA-2025-01-30-16-22-43-769.jpg"
                lastCapturedImagePath = "/storage/emulated/0/Pictures/JCA-2025-02-10-22-13-13-574.jpg"
    ))
    val uiState: StateFlow<PostCaptureUiState> = _uiState

    fun setCapturedImage(path: String) {
        _uiState.update { it.copy(lastCapturedImagePath = path, isImageDeleted = false) }
    }

    fun deleteImage() {
        _uiState.update { it.copy(lastCapturedImagePath = null, isImageDeleted = true) }
    }

    fun shareImage(path: String, shareImage: (String) -> Unit) {
        shareImage(path) // This will invoke the sharing functionality
    }

    fun setLastCapturedImagePath(path: String?) {
        _uiState.update { it.copy(lastCapturedImagePath = path) }
    }

}

data class PostCaptureUiState(
    val lastCapturedImagePath: String? = null,
    val isImageDeleted: Boolean = false
)