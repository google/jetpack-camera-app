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

import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import androidx.camera.core.Preview.SurfaceProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.google.jetpackcamera.feature.quicksettings.QuickSettingsScreen
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.viewfinder.CameraPreview
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation

private const val TAG = "PreviewScreen"

/**
 * Screen used for the Preview feature.
 */
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalMaterial3Api
@Composable
fun PreviewScreen(
    onNavigateToSettings: () -> Unit,
    viewModel: PreviewViewModel = hiltViewModel()
) {
    Log.d(TAG, "PreviewScreen")

    val previewUiState: PreviewUiState by viewModel.previewUiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current

    val deferredSurfaceProvider = remember { CompletableDeferred<SurfaceProvider>() }
    val onSurfaceProviderReady: (SurfaceProvider) -> Unit = {
        Log.d(TAG, "onSurfaceProviderReady")
        deferredSurfaceProvider.complete(it)
    }
    lateinit var viewInfo: View
    var zoomScale by remember { mutableStateOf(1f) }
    var zoomScaleShow by remember { mutableStateOf(false) }
    val zoomHandler = Handler(Looper.getMainLooper())
    val transformableState = rememberTransformableState(onTransformation = { zoomChange, _, _ ->
        zoomScale = viewModel.setZoomScale(zoomChange)
        zoomScaleShow = true
        zoomHandler.postDelayed({ zoomScaleShow = false }, 3000)
    })

    LaunchedEffect(lifecycleOwner) {
        val surfaceProvider = deferredSurfaceProvider.await()
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.runCamera(surfaceProvider)
            try {
                awaitCancellation()
            } finally {
                viewModel.stopCamera()
            }
        }
    }

    if (previewUiState.cameraState == CameraState.NOT_READY) {
        Text(text = stringResource(R.string.camera_not_ready))
    } else if (previewUiState.cameraState == CameraState.READY) {
        BoxWithConstraints(
            Modifier
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            Log.d(TAG, "onDoubleTap $offset")
                            viewModel.flipCamera()
                        },
                        onTap = { offset ->
                            // tap to focus
                            try {
                                viewModel.tapToFocus(
                                    viewInfo.display,
                                    viewInfo.width,
                                    viewInfo.height,
                                    offset.x, offset.y
                                )
                                Log.d(TAG, "onTap $offset")
                            } catch (e: UninitializedPropertyAccessException) {
                                Log.d(TAG, "onTap $offset")
                                e.printStackTrace()
                            }
                        }
                    )
                },

            contentAlignment = Alignment.Center
        ) {
            val maxAspectRatio: Float = maxWidth / maxHeight
            val aspectRatio: Float =
                previewUiState.currentCameraSettings.aspect_ratio.ratio.toFloat()
            val shouldUseMaxWidth = maxAspectRatio <= aspectRatio
            val width = if (shouldUseMaxWidth) maxWidth else maxHeight * aspectRatio
            val height = if (!shouldUseMaxWidth) maxHeight else maxWidth / aspectRatio
            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .transformable(state = transformableState)
                ) {
                    CameraPreview(
                        modifier = Modifier
                            .fillMaxSize(),
                        onSurfaceProviderReady = onSurfaceProviderReady,
                        onRequestBitmapReady = {
                            val bitmap = it.invoke()
                        },
                        setSurfaceView = { s: View ->
                            viewInfo = s
                        }
                    )
                }
            }

            QuickSettingsScreen(
                modifier = Modifier.fillMaxSize(),
                isOpen = previewUiState.quickSettingsIsOpen,
                toggleIsOpen = { viewModel.toggleQuickSettings() },
                currentCameraSettings = previewUiState.currentCameraSettings,
                onLensFaceClick = viewModel::flipCamera,
                onFlashModeClick = viewModel::setFlash,
                onAspectRatioClick = {
                    viewModel.setAspectRatio(it)
                },
                //onTimerClick = {}/*TODO*/
            )

            IconButton(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(12.dp),
                onClick = onNavigateToSettings,
            ) {
                Icon(
                    Icons.Filled.Settings,
                    contentDescription = stringResource(R.string.settings_content_description),
                    modifier = Modifier.size(72.dp)
                )
            }

            SuggestionChip(
                onClick = { viewModel.toggleCaptureMode() },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                label = {
                    Text(
                        stringResource(
                            when (previewUiState.currentCameraSettings.captureMode) {
                                CaptureMode.SINGLE_STREAM -> R.string.capture_mode_single_stream
                                CaptureMode.MULTI_STREAM -> R.string.capture_mode_multi_stream

                            }
                        )
                    )
                }
            )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                ZoomScaleText(
                    zoomScale = zoomScale,
                    show = zoomScaleShow
                )
                Row(
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Row {
                        CaptureButton(
                            onClick = { viewModel.captureImage() },
                            onLongPress = { viewModel.startVideoRecording() },
                            onRelease = { viewModel.stopVideoRecording() },
                            state = previewUiState.videoRecordingState
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ZoomScaleText(zoomScale: Float, show: Boolean) {
    val contentAlpha = animateFloatAsState(
        targetValue = if (show) 1f else 0f, label = "zoomScaleAlphaAnimation",
        animationSpec = tween()
    )
    Text(
        modifier = Modifier.alpha(contentAlpha.value),
        text = String.format("%.1fx", zoomScale),
        fontSize = 20.sp,
        color = Color.White
    )
}

@Composable
fun CaptureButton(
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
    state: VideoRecordingState
) {
    Box(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress()
                    }, onPress = {
                        awaitRelease()
                        onRelease()
                    }, onTap = { onClick() })
            }
            .size(120.dp)
            .padding(18.dp)
            .border(4.dp, Color.White, CircleShape)
    ) {
        Canvas(modifier = Modifier.size(110.dp), onDraw = {
            drawCircle(
                color =
                when (state) {
                    VideoRecordingState.INACTIVE -> Color.Transparent
                    VideoRecordingState.ACTIVE -> Color.Red
                }
            )
        })
    }
}