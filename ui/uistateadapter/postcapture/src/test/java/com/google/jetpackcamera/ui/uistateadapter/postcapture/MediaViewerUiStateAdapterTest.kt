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
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.google.jetpackcamera.data.media.Media
import com.google.jetpackcamera.data.media.MediaDescriptor
import com.google.jetpackcamera.ui.uistate.postcapture.MediaViewerUiState
import org.junit.Test

class MediaViewerUiStateAdapterTest {
    private val testUri: Uri = Uri.EMPTY

    val testBitmap: Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    val testPlayer: Player = ExoPlayer.Builder(ApplicationProvider.getApplicationContext()).build()

    @Test
    fun mediaViewerUiState_fromMediaError_returnsError() {
        val mediaDescriptor = MediaDescriptor.None
        val media = Media.Error
        val player = null
        val playerState = false
        val expectedUiState = MediaViewerUiState.Error
        val actualUiState = MediaViewerUiState.from(mediaDescriptor, media, player, playerState)
        assertThat(actualUiState).isEqualTo(expectedUiState)
    }

    @Test
    fun mediaViewerUiState_fromMediaNone_returnsLoading() {
        val mediaDescriptor = MediaDescriptor.None
        val media = Media.None
        val player = null
        val playerState = false
        val expectedUiState = MediaViewerUiState.Loading
        val actualUiState = MediaViewerUiState.from(mediaDescriptor, media, player, playerState)
        assertThat(actualUiState).isEqualTo(expectedUiState)
    }

    @Test
    fun mediaViewerUiState_fromMediaImage_returnsContentImage() {
        val mediaDescriptor = MediaDescriptor.Content.Image(testUri, testBitmap)
        val media = Media.Image(testBitmap)
        val player = null
        val playerState = false
        val expectedUiState = MediaViewerUiState.Content.Image(testBitmap)
        val actualUiState = MediaViewerUiState.from(mediaDescriptor, media, player, playerState)
        assertThat(actualUiState).isEqualTo(expectedUiState)
    }

    @Test
    fun mediaViewerUiState_fromVideoPlayerReady_returnsContentVideoReady() {
        val mediaDescriptor = MediaDescriptor.Content.Video(testUri, testBitmap)
        val media = Media.Video(testUri)
        val player = testPlayer
        val playerState = true
        val expectedUiState = MediaViewerUiState.Content.Video.Ready(testPlayer, testBitmap)
        val actualUiState = MediaViewerUiState.from(mediaDescriptor, media, player, playerState)
        assertThat(actualUiState).isEqualTo(expectedUiState)
    }

    @Test
    fun mediaViewerUiState_fromVideoPlayerNull_returnsContentVideoLoading() {
        val mediaDescriptor = MediaDescriptor.Content.Video(testUri, testBitmap)
        val media = Media.Video(testUri)
        val expectedUiState = MediaViewerUiState.Content.Video.Loading(testBitmap)
        val actualUiState = MediaViewerUiState.from(mediaDescriptor, media, null, false)
        assertThat(actualUiState).isEqualTo(expectedUiState)
    }

    @Test
    fun mediaViewerUiState_fromVideoPlayerStateFalse_returnsContentVideoLoading() {
        val mediaDescriptor = MediaDescriptor.Content.Video(testUri, testBitmap)
        val media = Media.Video(testUri)
        val player = testPlayer
        val playerState = false
        val expectedUiState = MediaViewerUiState.Content.Video.Loading(testBitmap)
        val actualUiState = MediaViewerUiState.from(mediaDescriptor, media, player, playerState)
        assertThat(actualUiState).isEqualTo(expectedUiState)
    }
}
