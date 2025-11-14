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

import android.graphics.Bitmap
import android.net.Uri
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.postcapture.ShareButtonUiState
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock

class ShareButtonUiStateAdapterTest {
    private val testUri: Uri = mock(Uri::class.java)

    val mockBitmap: Bitmap = mock(Bitmap::class.java)

    @Test
    fun shareButtonUiState_fromContentImage_returnsReady() {
        val mediaDescriptor = MediaDescriptor.Content.Image(testUri, mockBitmap)
        val expectedUiState = ShareButtonUiState.Ready
        val actualUiState = ShareButtonUiState.from(mediaDescriptor)
        assertEquals(expectedUiState, actualUiState)
    }

    @Test
    fun shareButtonUiState_fromContentVideo_returnsReady() {
        val mediaDescriptor = MediaDescriptor.Content.Video(testUri, mockBitmap)
        val expectedUiState = ShareButtonUiState.Ready
        val actualUiState = ShareButtonUiState.from(mediaDescriptor)
        assertEquals(expectedUiState, actualUiState)
    }

    @Test
    fun shareButtonUiState_fromNone_returnsUnavailable() {
        val mediaDescriptor = MediaDescriptor.None
        val expectedUiState = ShareButtonUiState.Unavailable
        val actualUiState = ShareButtonUiState.from(mediaDescriptor)
        assertEquals(expectedUiState, actualUiState)
    }
}
