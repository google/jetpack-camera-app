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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.media3.common.util.UnstableApi
import com.google.jetpackcamera.feature.postcapture.PostCaptureViewModel.PostCaptureEvent
import com.google.jetpackcamera.feature.postcapture.ui.DeleteCurrentMediaButton
import com.google.jetpackcamera.feature.postcapture.ui.ExitPostCaptureButton
import com.google.jetpackcamera.feature.postcapture.ui.MediaViewer
import com.google.jetpackcamera.feature.postcapture.ui.PostCaptureLayout
import com.google.jetpackcamera.feature.postcapture.ui.SaveCurrentMediaButton
import com.google.jetpackcamera.feature.postcapture.ui.ShareCurrentMediaButton
import com.google.jetpackcamera.ui.components.capture.TestableSnackbar
import com.google.jetpackcamera.ui.uistate.postcapture.DeleteButtonUiState
import com.google.jetpackcamera.ui.uistate.postcapture.MediaViewerUiState
import com.google.jetpackcamera.ui.uistate.postcapture.PostCaptureUiState
import com.google.jetpackcamera.ui.uistate.postcapture.ShareButtonUiState
import utils.MediaSharing

private const val TAG = "PostCaptureScreen"

@OptIn(UnstableApi::class)
@Composable
fun PostCaptureScreen(
    onNavigateBack: () -> Unit,
    viewModel: PostCaptureViewModel = hiltViewModel()
) {
    Log.d(TAG, "PostCaptureScreen")

    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvents.collect { event ->
            when (event) {
                is PostCaptureEvent.ShareMedia -> {
                    MediaSharing.shareMedia(context, event.media)
                }
            }
        }
    }

    val uiState: PostCaptureUiState by viewModel.postCaptureUiState.collectAsState()
    PostCaptureComponent(
        uiState = uiState,
        onNavigateBack = onNavigateBack,
        onDeleteMedia = viewModel::deleteCurrentMedia,
        onSaveMedia = viewModel::saveCurrentMedia,
        onShareCurrentMedia = viewModel::onShareCurrentMedia,
        onLoadVideo = viewModel::loadCurrentVideo,
        onSnackBarResult = viewModel::onSnackBarResult
    )
}

@OptIn(UnstableApi::class)
@Composable
fun PostCaptureComponent(
    uiState: PostCaptureUiState,
    onNavigateBack: () -> Unit,
    onSaveMedia: () -> Unit,
    onShareCurrentMedia: () -> Unit,
    onDeleteMedia: (onSuccessCallback: () -> Unit) -> Unit,
    onLoadVideo: () -> Unit,
    onSnackBarResult: (String) -> Unit
) {
    when (uiState) {
        PostCaptureUiState.Loading -> {
            MediaViewer(
                modifier = Modifier,
                onLoadVideo = {},
                uiState = MediaViewerUiState.Loading
            )
        }

        is PostCaptureUiState.Ready -> {
            PostCaptureLayout(
                mediaSurface = {
                    MediaViewer(
                        modifier = it,
                        onLoadVideo = onLoadVideo,
                        uiState = uiState.viewerUiState
                    )
                },
                exitButton = {
                    ExitPostCaptureButton(
                        modifier = it,
                        onExitPostCapture = onNavigateBack
                    )
                },
                saveButton = {
                    SaveCurrentMediaButton(modifier = it, onClick = onSaveMedia)
                },
                shareButton = {
                    if (uiState.shareButtonUiState is ShareButtonUiState.Ready) {
                        ShareCurrentMediaButton(
                            modifier = it,
                            onClick = onShareCurrentMedia
                        )
                    }
                },
                deleteButton = {
                    if (uiState.deleteButtonUiState is DeleteButtonUiState.Ready) {
                        DeleteCurrentMediaButton(
                            modifier = it,
                            onClick = {
                                onDeleteMedia(onNavigateBack)
                            }
                        )
                    }
                },
                snackBar = { modifier, snackbarHostState ->
                    val snackBarData = uiState.snackBarUiState.snackBarQueue.peek()
                    if (snackBarData != null) {
                        TestableSnackbar(
                            modifier = modifier.testTag(snackBarData.testTag),
                            snackbarToShow = snackBarData,
                            snackbarHostState = snackbarHostState,
                            onSnackbarResult = onSnackBarResult
                        )
                    }
                }
            )
        }
    }
}
