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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
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
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.VideoRecordingState
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.Stabilization
import com.google.jetpackcamera.settings.model.SupportedStabilizationMode
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope

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
        Box(
            modifier
                .testTag(PREVIEW_DISPLAY)
                .fillMaxSize()
                .background(Color.Black)
                .wrapContentSize()
                .pointerInput(Unit) {
                    detectTapGestures(
                        onDoubleTap = { offset ->
                            // double tap to flip camera
                            Log.d(TAG, "onDoubleTap $offset")
                            currentOnFlipCamera()
                        }
                    )
                }
                .layout { measurable, constraints ->
                    val maxWidth = constraints.maxWidth.toFloat()
                    val maxHeight = constraints.maxHeight.toFloat()
                    val maxAspectRatio: Float = maxWidth / maxHeight
                    val aspectRatioFloat: Float = aspectRatio.ratio.toFloat()

                    val correctAspectRation = if (
                        (maxAspectRatio > 1 && aspectRatioFloat < 1) ||
                        (maxAspectRatio < 1 && aspectRatioFloat > 1)
                    ) {
                        1 / aspectRatioFloat
                    } else {
                        aspectRatioFloat
                    }
                    val shouldUseMaxWidth = maxAspectRatio <= correctAspectRation
                    val width = if (shouldUseMaxWidth) maxWidth else maxHeight * correctAspectRation
                    val height =
                        if (!shouldUseMaxWidth) maxHeight else maxWidth / correctAspectRation

                    val placeable = measurable.measure(
                        Constraints.fixed(
                            width.roundToInt(),
                            height.roundToInt()
                        )
                    )

                    layout(placeable.width, placeable.height) {
                        placeable.place(0, 0)
                    }
                }
                .transformable(state = transformableState)
                .alpha(blinkState.alpha),

            contentAlignment = Alignment.Center
        ) {
            CameraXViewfinder(
                modifier = Modifier.fillMaxSize(),
                surfaceRequest = it
            )
        }
    }
}

class BlinkState(
    initialAlpha: Float = 1F,
    coroutineScope: CoroutineScope
) {
    private val animatable = Animatable(initialAlpha)
    val alpha: Float get() = animatable.value
    val scope = coroutineScope

    suspend fun play() {
        animatable.snapTo(0F)
        animatable.animateTo(1F, animationSpec = tween(800))
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
            painter = painterResource(id = R.drawable.baseline_video_stable_24),
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
