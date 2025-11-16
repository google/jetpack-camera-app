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

import android.util.Log
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.google.jetpackcamera.feature.postcapture.ui.DeleteCurrentMediaButton
import com.google.jetpackcamera.feature.postcapture.ui.MediaViewer
import com.google.jetpackcamera.feature.postcapture.ui.ShareCurrentMediaButton
import com.google.jetpackcamera.ui.uistate.postcapture.PostCaptureUiState

private const val TAG = "PostCaptureScreen"

@OptIn(UnstableApi::class)
@Composable
fun PostCaptureScreen(viewModel: PostCaptureViewModel = hiltViewModel()) {
    Log.d(TAG, "PostCaptureScreen")

    val uiState: PostCaptureUiState by viewModel.postCaptureUiState.collectAsState()

    when (val currentUiState = uiState) {
        is PostCaptureUiState.Loading -> {}
        is PostCaptureUiState.Ready -> {
            Box(modifier = Modifier.fillMaxSize()) {
                MediaViewer(
                    uiState = currentUiState.viewerUiState,
                    onLoadVideo = viewModel::loadCurrentVideo
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    // Delete Image Button
                    DeleteCurrentMediaButton(onClick = viewModel::deleteCurrentMedia)
                    Spacer(modifier = Modifier.weight(1f))

                    // Share Media Button
                    ShareCurrentMediaButton(onClick = viewModel::shareCurrentMedia)
                }
            }
        }
    }
}
