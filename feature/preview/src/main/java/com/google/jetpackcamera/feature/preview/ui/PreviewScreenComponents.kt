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
import android.widget.Toast
import androidx.camera.core.SurfaceRequest
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoStable
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SupportedStabilizationMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val TAG = "PreviewScreen"

/**
 * An invisible box that will display a [Toast] with specifications set by a [ToastMessage].
 *
 * @param toastMessage the specifications for the [Toast].
 * @param onToastShown called once the Toast has been displayed.
 *
 */
@Composable
fun TestableToast(
    toastMessage: ToastMessage,
    onToastShown: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        // box seems to need to have some size to be detected by UiAutomator
        modifier = modifier
            .size(20.dp)
            .testTag(toastMessage.testTag)
    ) {
        val context = LocalContext.current
        LaunchedEffect(toastMessage) {
            if (toastMessage.shouldShowToast) {
                Toast.makeText(
                    context,
                    context.getText(toastMessage.stringResource),
                    toastMessage.toastLength
                ).show()
            }

            onToastShown()
        }
        Log.d(
            TAG,
            "Toast Displayed with message: ${stringResource(id = toastMessage.stringResource)}"
        )
    }
}

@Composable
fun TestableSnackBar(
    modifier: Modifier = Modifier,
    snackBarToShow: SnackBarData,
    scope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    onSnackBarResult: () -> Unit
) {
    Box(
        // box seems to need to have some size to be detected by UiAutomator
        modifier = modifier
            .size(20.dp)
            .testTag(snackBarToShow.testTag)
    ) {
        val context = LocalContext.current
        scope.launch {
            val result =
                snackbarHostState.showSnackbar(
                    message = context.getString(snackBarToShow.stringResource),
                    duration = snackBarToShow.duration,
                    withDismissAction = snackBarToShow.withDismissAction,
                    actionLabel = if (snackBarToShow.actionLabelRes == null) {
                        null
                    } else {
                        context.getString(snackBarToShow.actionLabelRes)
                    }
                )
            when (result) {
                SnackbarResult.ActionPerformed -> {
                    onSnackBarResult()
                }
                SnackbarResult.Dismissed -> {
                    onSnackBarResult()
                }
            }
        }
        Log.d(
            TAG,
            "Snackbar Displayed with message: ${stringResource(snackBarToShow.stringResource)}"
        )
    }
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
    onRequestWindowColorMode: (Int) -> Unit,
    aspectRatio: AspectRatio,
    surfaceRequest: SurfaceRequest?,
    blinkState: BlinkState,
    modifier: Modifier = Modifier
) {
    val transformableState = rememberTransformableState(
        onTransformation = { zoomChange, _, _ ->
            onZoomChange(zoomChange)
        }
    )

    val currentOnFlipCamera by rememberUpdatedState(onFlipCamera)

    surfaceRequest?.let {
        BoxWithConstraints(
            Modifier
                .testTag(PREVIEW_DISPLAY)
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            // double tap to flip camera
                            Log.d(TAG, "onDoubleTap $offset")
                            currentOnFlipCamera()
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
                    .alpha(blinkState.alpha)

            ) {
                CameraXViewfinder(
                    modifier = Modifier.fillMaxSize(),
                    surfaceRequest = it,
                    onRequestWindowColorMode = onRequestWindowColorMode
                )
            }
        }
    }
}

@Composable
fun StabilizationIcon(
    supportedStabilizationMode: List<SupportedStabilizationMode>,
    videoStabilization: Stabilization,
    previewStabilization: Stabilization,
    modifier: Modifier = Modifier
) {
    if (supportedStabilizationMode.isNotEmpty() &&
        (videoStabilization == Stabilization.ON || previewStabilization == Stabilization.ON)
    ) {
        val descriptionText = if (videoStabilization == Stabilization.ON) {
            stringResource(id = R.string.stabilization_icon_description_preview_and_video)
        } else {
            // previewStabilization will not be on for high quality
            stringResource(id = R.string.stabilization_icon_description_video_only)
        }
        Icon(
            imageVector = Icons.Filled.VideoStable,
            contentDescription = descriptionText,
            modifier = modifier
        )
    }
}

/**
 * A temporary button that can be added to preview for quick testing purposes
 */
@Composable
fun TestingButton(onClick: () -> Unit, text: String, modifier: Modifier = Modifier) {
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
    enabledCondition: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        modifier = modifier.size(40.dp),
        onClick = onClick,
        enabled = enabledCondition
    ) {
        Icon(
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(id = R.string.flip_camera_content_description),
            modifier = Modifier.size(72.dp)
        )
    }
}

@Composable
fun SettingsNavButton(onNavigateToSettings: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(
        modifier = modifier,
        onClick = onNavigateToSettings
    ) {
        Icon(
            imageVector = Icons.Filled.Settings,
            contentDescription = stringResource(R.string.settings_content_description),
            modifier = Modifier.size(72.dp)
        )
    }
}

@Composable
fun ZoomScaleText(zoomScale: Float, modifier: Modifier = Modifier) {
    val contentAlpha = animateFloatAsState(
        targetValue = 10f,
        label = "zoomScaleAlphaAnimation",
        animationSpec = tween()
    )
    Text(
        modifier = Modifier.alpha(contentAlpha.value),
        text = "%.1fx".format(zoomScale),
        fontSize = 20.sp
    )
}

@Composable
fun CaptureButton(
    onClick: () -> Unit,
    onLongPress: () -> Unit,
    onRelease: () -> Unit,
    videoRecordingState: VideoRecordingState,
    modifier: Modifier = Modifier
) {
    var isPressedDown by remember {
        mutableStateOf(false)
    }
    val currentColor = LocalContentColor.current
    Box(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongPress()
                    },
                    // TODO: @kimblebee - stopVideoRecording is being called every time the capture
                    // button is pressed -- regardless of tap or long press
                    onPress = {
                        isPressedDown = true
                        awaitRelease()
                        isPressedDown = false
                        onRelease()
                    },
                    onTap = { onClick() }
                )
            }
            .size(120.dp)
            .padding(18.dp)
            .border(4.dp, currentColor, CircleShape)
    ) {
        Canvas(modifier = Modifier.size(110.dp), onDraw = {
            drawCircle(
                color =
                when (videoRecordingState) {
                    VideoRecordingState.INACTIVE -> {
                        if (isPressedDown) currentColor else Color.Transparent
                    }

                    VideoRecordingState.ACTIVE -> Color.Red
                }
            )
        })
    }
}
