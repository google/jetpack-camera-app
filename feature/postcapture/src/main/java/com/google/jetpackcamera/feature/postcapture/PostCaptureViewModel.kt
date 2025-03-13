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

import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class PostCaptureViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(PostCaptureUiState())
    val uiState: StateFlow<PostCaptureUiState> = _uiState

    fun setLastCapturedImageUri(imageUri: Uri?) {
        _uiState.update { it.copy(imageUri = imageUri, isImageDeleted = false) }
    }

    fun deleteImage(contentResolver: ContentResolver) {
        contentResolver.delete(uiState.value.imageUri!!, null, null)
        _uiState.update { it.copy(imageUri = null, isImageDeleted = true) }
    }
}

data class PostCaptureUiState(
    val imageUri: Uri? = null,
    val isImageDeleted: Boolean = false
)
