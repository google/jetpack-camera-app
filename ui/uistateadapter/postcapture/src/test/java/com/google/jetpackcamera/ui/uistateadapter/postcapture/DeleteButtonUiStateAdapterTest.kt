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
package com.google.jetpackcamera.ui.uistateadapter.postcapture

import android.net.Uri
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.postcapture.DeleteButtonUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class DeleteButtonUiStateAdapterTest {
    private val testUri: Uri = Uri.EMPTY

    @Test
    fun deleteButtonUiState_fromNonCachedContent_returnsReady() {
        val mediaDescriptor = MediaDescriptor.Content.Video(
            uri = testUri,
            thumbnail = null,
            isCached = false
        )
        val expectedUiState = DeleteButtonUiState.Ready
        val actualUiState = DeleteButtonUiState.from(mediaDescriptor)
        assertEquals(expectedUiState, actualUiState)
    }

    @Test
    fun deleteButtonUiState_fromCachedContent_returnsUnavailable() {
        val mediaDescriptor = MediaDescriptor.Content.Video(
            uri = testUri,
            thumbnail = null,
            isCached = true
        )
        val expectedUiState = DeleteButtonUiState.Unavailable
        val actualUiState = DeleteButtonUiState.from(mediaDescriptor)
        assertEquals(expectedUiState, actualUiState)
    }

    @Test
    fun deleteButtonUiState_fromNone_returnsUnavailable() {
        val mediaDescriptor = MediaDescriptor.None
        val expectedUiState = DeleteButtonUiState.Unavailable
        val actualUiState = DeleteButtonUiState.from(mediaDescriptor)
        assertEquals(expectedUiState, actualUiState)
    }
}
