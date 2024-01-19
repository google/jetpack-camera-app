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
package com.google.jetpackcamera.feature.preview.ui

import android.util.Log
import android.view.Display
import android.view.View
import android.widget.Toast
import androidx.camera.core.Preview
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.VideoRecordingState
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.viewfinder.CameraPreview
import kotlinx.coroutines.CompletableDeferred

private const val TAG = "PreviewScreen"

/**
 * An invisible box that will display a [Toast] with specifications set by a [ToastMessage].
 *
 * @param toastMessage the specifications for the [Toast].
 * @param onToastShown called once the Toast has been displayed.
 */
@Composable
fun ShowTestableToast(
    modifier: Modifier = Modifier,
    toastMessage: ToastMessage,
    onToastShown: () -> Unit
) {
    val toastShownStatus = remember { mutableStateOf(false) }
    Box(
        // box seems to need to have some size to be detected by UiAutomator
        modifier = modifier
            .size(20.dp)
            .testTag(toastMessage.testTag)
    ) {
        // prevents toast from being spammed
        if (!toastShownStatus.value) {
            Toast.makeText(
                LocalContext.current,
                stringResource(id = toastMessage.stringResource),
                toastMessage.toastLength
            )
                .show()
            toastShownStatus.value = true
            onToastShown()
        }
    }
    Log.d(TAG, "Toast Displayed with message: ${stringResource(id = toastMessage.stringResource)}")
}

/**
 * this is the preview surface display. This view implements gestures tap to focus, pinch to zoom,
 * and double-tap to flip camera
 */
@Composable
fun PreviewDisplay(
    onTapToFocus: (Display, Int, Int, Float, Float) -> Unit,
    onFlipCamera: () -> Unit,
    onZoomChange: (Float) -> Unit,
    aspectRatio: AspectRatio,
    deferredSurfaceProvider: CompletableDeferred<Preview.SurfaceProvider>
) {
    val transformableState = rememberTransformableState(
        onTransformation = { zoomChange, _, _ ->
            onZoomChange(zoomChange)
        }
    )
    val onSurfaceProviderReady: (Preview.SurfaceProvider) -> Unit = {
        Log.d(TAG, "onSurfaceProviderReady")
        deferredSurfaceProvider.complete(it)
    }
    lateinit var viewInfo: View

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { offset ->
                        // double tap to flip camera
                        Log.d(TAG, "onDoubleTap $offset")
                        onFlipCamera()
                    },
                    onTap = { offset ->
                        // tap to focus
                        try {
                            onTapToFocus(
                                viewInfo.display,
                                viewInfo.width,
                                viewInfo.height,
                                offset.x,
                                offset.y
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
        val aspectRatioFloat: Float = aspectRatio.ratio.toFloat()
        val shouldUseMaxWidth = maxAspectRatio <= aspectRatioFloat
        val width = if (shouldUseMaxWidth) maxWidth else maxHeight * aspectRatioFloat
        val height = if (!shouldUseMaxWidth) maxHeight else maxWidth / aspectRatioFloat
        Box(
            modifier = Modifier
                .width(width)
                .height(height)
                .transformable(state = transformableState)

        ) {
            CameraPreview(
                modifier = Modifier
                    .fillMaxSize(),
                onSurfaceProviderReady = onSurfaceProviderReady,
                onRequestBitmapReady = {
                    it.invoke()
                },
                setSurfaceView = { s: View ->
                    viewInfo = s
                }
            )
        }
    }
}

/**
 * A temporary button that can be added to preview for quick testing purposes
 */
@Composable
fun TestingButton(modifier: Modifier = Modifier, onClick: () -> Unit, text: String) {
    SuggestionChip(
        onClick = { onClick() },
        modifier = modifier,
        label = {
            Text(text = text)
        }
    )
}

@Composable
fun FlipCameraButton(
    modifier: Modifier = Modifier,
    enabledCondition: Boolean,
    onClick: () -> Unit
) {
    IconButton(
        modifier = modifier
            .size(40.dp),
        onClick = onClick,
        enabled = enabledCondition
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            tint = Color.White,
            contentDescription = stringResource(id = R.string.flip_camera_content_description),
            modifier = Modifier.size(72.dp)
        )
    }
}

@Composable
fun SettingsNavButton(modifier: Modifier, onNavigateToSettings: () -> Unit) {
    IconButton(
        modifier = modifier,
        onClick = onNavigateToSettings
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            tint = Color.White,
            contentDescription = stringResource(R.string.settings_content_description),
            modifier = Modifier.size(72.dp)
        )
    }
}

@Composable
fun ZoomScaleText(zoomScale: Float) {
    val contentAlpha = animateFloatAsState(
        targetValue = 10f,
        label = "zoomScaleAlphaAnimation",
        animationSpec = tween()
    )
    Text(
        modifier = Modifier.alpha(contentAlpha.value),
        text = "%.1fx".format(zoomScale),
        fontSize = 20.sp,
        color = Color.White
    )
}

@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
    videoRecordingState: VideoRecordingState
) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress()
                    },
                    onPress = {
                        awaitRelease()
                        onRelease()
                    },
                    onTap = { onClick() }
                )
            }
            .size(120.dp)
            .padding(18.dp)
            .border(4.dp, Color.White, CircleShape)
    ) {
        Canvas(modifier = Modifier.size(110.dp), onDraw = {
            drawCircle(
                color =
                when (videoRecordingState) {
                    VideoRecordingState.INACTIVE -> Color.Transparent
                    VideoRecordingState.ACTIVE -> Color.Red
                }
            )
        })
    }
}
