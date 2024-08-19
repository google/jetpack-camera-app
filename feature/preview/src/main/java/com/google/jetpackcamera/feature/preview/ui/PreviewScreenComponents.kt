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

import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.surface.ImplementationMode
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoStable
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.VideoRecordingState
import com.google.jetpackcamera.feature.preview.ui.theme.PreviewPreviewTheme
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.LowLightBoost
import com.google.jetpackcamera.settings.model.Stabilization
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "PreviewScreen"
private const val BLINK_TIME = 100L

@Composable
fun AmplitudeVisualizer(
    modifier: Modifier = Modifier,
    size: Int = 100,
    audioAmplitude: Double,
    onToggleMute: () -> Unit
) {
    // Tweak the multiplier to amplitude to adjust the visualizer sensitivity
    val animatedScaling by animateFloatAsState(
        targetValue = EaseOutExpo.transform(1 + (1.75f * audioAmplitude.toFloat())),
        label = "AudioAnimation"
    )
    Box(modifier = modifier.clickable { onToggleMute() }) {
        // animated circle
        Canvas(
            modifier = Modifier
                .align(Alignment.Center),
            onDraw = {
                drawCircle(
                    // tweak the multiplier to size to adjust the maximum size of the visualizer
                    radius = (size * animatedScaling).coerceIn(size.toFloat(), size * 1.65f),
                    alpha = .5f,
                    color = Color.White
                )
            }
        )

        // static circle
        Canvas(
            modifier = Modifier
                .align(Alignment.Center),
            onDraw = {
                drawCircle(
                    radius = (size.toFloat()),
                    color = Color.White
                )
            }
        )

        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .size((0.5 * size).dp)
                .apply {
                    if (audioAmplitude != 0.0) {
                        testTag(AMPLITUDE_HOT_TAG)
                    } else {
                        testTag(AMPLITUDE_NONE_TAG)
                    }
                },
            tint = Color.Black,
            imageVector = if (audioAmplitude != 0.0) {
                Icons.Filled.Mic
            } else {
                Icons.Filled.MicOff
            },
            contentDescription = stringResource(id = R.string.audio_visualizer_icon)
        )
    }
}

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
fun TestableSnackbar(
    modifier: Modifier = Modifier,
    snackbarToShow: SnackbarData,
    snackbarHostState: SnackbarHostState,
    onSnackbarResult: (String) -> Unit
) {
    Box(
        // box seems to need to have some size to be detected by UiAutomator
        modifier = modifier
            .size(20.dp)
            .testTag(snackbarToShow.testTag)
    ) {
        val context = LocalContext.current
        LaunchedEffect(snackbarToShow) {
            val message = context.getString(snackbarToShow.stringResource)
            Log.d(TAG, "Snackbar Displayed with message: $message")
            try {
                val result =
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = snackbarToShow.duration,
                        withDismissAction = snackbarToShow.withDismissAction,
                        actionLabel = if (snackbarToShow.actionLabelRes == null) {
                            null
                        } else {
                            context.getString(snackbarToShow.actionLabelRes)
                        }
                    )
                when (result) {
                    SnackbarResult.ActionPerformed,
                    SnackbarResult.Dismissed -> onSnackbarResult(snackbarToShow.cookie)
                }
            } catch (e: Exception) {
                // This is equivalent to dismissing the snackbar
                onSnackbarResult(snackbarToShow.cookie)
            }
        }
    }
}

/**
 * this is the preview surface display. This view implements gestures tap to focus, pinch to zoom,
 * and double-tap to flip camera
 */
@Composable
fun PreviewDisplay(
    previewUiState: PreviewUiState.Ready,
    onTapToFocus: (x: Float, y: Float) -> Unit,
    onFlipCamera: () -> Unit,
    onZoomChange: (Float) -> Unit,
    onRequestWindowColorMode: (Int) -> Unit,
    aspectRatio: AspectRatio,
    surfaceRequest: SurfaceRequest?,
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
            var imageVisible by remember { mutableStateOf(true) }

            val imageAlpha: Float by animateFloatAsState(
                targetValue = if (imageVisible) 1f else 0f,
                animationSpec = tween(
                    durationMillis = (BLINK_TIME / 2).toInt(),
                    easing = LinearEasing
                ),
                label = ""
            )

            LaunchedEffect(previewUiState.lastBlinkTimeStamp) {
                if (previewUiState.lastBlinkTimeStamp != 0L) {
                    imageVisible = false
                    delay(BLINK_TIME)
                    imageVisible = true
                }
            }

            Box(
                modifier = Modifier
                    .width(width)
                    .height(height)
                    .transformable(state = transformableState)
                    .alpha(imageAlpha)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                CameraXViewfinder(
                    modifier = Modifier.fillMaxSize(),
                    surfaceRequest = it,
                    implementationMode = when {
                        Build.VERSION.SDK_INT > 24 -> ImplementationMode.EXTERNAL
                        else -> ImplementationMode.EMBEDDED
                    },
                    onRequestWindowColorMode = onRequestWindowColorMode,
                    onTap = { x, y -> onTapToFocus(x, y) }
                )
            }
        }
    }
}

@Composable
fun StabilizationIcon(
    videoStabilization: Stabilization,
    previewStabilization: Stabilization,
    modifier: Modifier = Modifier
) {
    if (videoStabilization == Stabilization.ON || previewStabilization == Stabilization.ON) {
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
 * LowLightBoostIcon has 3 states
 * - disabled: hidden
 * - enabled and inactive: outline
 * - enabled and active: filled
 */
@Composable
fun LowLightBoostIcon(lowLightBoost: LowLightBoost, modifier: Modifier = Modifier) {
    when (lowLightBoost) {
        LowLightBoost.ENABLED -> {
            Icon(
                imageVector = Icons.Outlined.Nightlight,
                contentDescription =
                stringResource(id = R.string.quick_settings_lowlightboost_enabled),
                modifier = modifier.alpha(0.5f)
            )
        }
        LowLightBoost.DISABLED -> {
        }
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
            imageVector = Icons.Filled.FlipCameraAndroid,
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
fun ZoomScaleText(zoomScale: Float) {
    val contentAlpha = animateFloatAsState(
        targetValue = 10f,
        label = "zoomScaleAlphaAnimation",
        animationSpec = tween()
    )
    Text(
        modifier = Modifier
            .padding(horizontal = 10.dp)
            .alpha(contentAlpha.value)
            .testTag(ZOOM_RATIO_TAG),
        text = "%.1fx".format(zoomScale),
        fontSize = 20.sp
    )
}

@Composable
fun CurrentCameraIdText(physicalCameraId: String?, logicalCameraId: String?) {
    Text(
        modifier = Modifier
            .testTag(CURRENT_CAMERA_ID_TAG),
        text = "physical id: $physicalCameraId, logical id: $logicalCameraId",
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

enum class ToggleState {
    Left,
    Right
}

@Composable
fun ToggleButton(
    leftIcon: Painter,
    rightIcon: Painter,
    modifier: Modifier = Modifier
        .width(64.dp)
        .height(32.dp),
    initialState: ToggleState = ToggleState.Left,
    onToggleStateChanged: (newState: ToggleState) -> Unit = {},
    onToggleWhenDisabled: () -> Unit = {},
    enabled: Boolean = true,
    leftIconDescription: String = "leftIcon",
    rightIconDescription: String = "rightIcon",
    iconPadding: Dp = 8.dp
) {
    val backgroundColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val disableColor = MaterialTheme.colorScheme.onSurface
    val iconSelectionColor = MaterialTheme.colorScheme.onPrimary
    val iconUnSelectionColor = MaterialTheme.colorScheme.primary
    val circleSelectionColor = MaterialTheme.colorScheme.primary
    val circleColor = if (enabled) circleSelectionColor else disableColor.copy(alpha = 0.12f)
    var toggleState by remember { mutableStateOf(initialState) }
    val animatedTogglePosition by animateFloatAsState(
        when (toggleState) {
            ToggleState.Left -> 0f
            ToggleState.Right -> 1f
        },
        label = "togglePosition"
    )
    val scope = rememberCoroutineScope()

    Surface(
        modifier = modifier
            .clip(shape = RoundedCornerShape(50))
            .then(
                Modifier.clickable {
                    scope.launch {
                        if (enabled) {
                            toggleState = when (toggleState) {
                                ToggleState.Left -> ToggleState.Right
                                ToggleState.Right -> ToggleState.Left
                            }
                            onToggleStateChanged(toggleState)
                        } else {
                            onToggleWhenDisabled()
                        }
                    }
                }
            ),
        color = backgroundColor
    ) {
        Box {
            Row(
                modifier = Modifier.matchParentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            layout(placeable.width, placeable.height) {
                                val xPos = animatedTogglePosition *
                                    (constraints.maxWidth - placeable.width)
                                placeable.placeRelative(xPos.toInt(), 0)
                            }
                        }
                        .fillMaxHeight()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(50))
                        .background(circleColor)
                )
            }
            Row(
                modifier = Modifier
                    .matchParentSize()
                    .then(
                        if (enabled) Modifier else Modifier.alpha(0.38f)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    painter = leftIcon,
                    contentDescription = leftIconDescription,
                    modifier = Modifier.padding(iconPadding),
                    tint = if (!enabled) {
                        disableColor
                    } else if (toggleState == ToggleState.Left) {
                        iconSelectionColor
                    } else {
                        iconUnSelectionColor
                    }
                )
                Icon(
                    painter = rightIcon,
                    contentDescription = rightIconDescription,
                    modifier = Modifier.padding(iconPadding),
                    tint = if (!enabled) {
                        disableColor
                    } else if (toggleState == ToggleState.Right) {
                        iconSelectionColor
                    } else {
                        iconUnSelectionColor
                    }
                )
            }
        }
    }
}

@Preview(name = "Light Mode")
@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ToggleButton_Selecting_Left() {
    val initialState = ToggleState.Left
    var toggleState by remember {
        mutableStateOf(initialState)
    }
    PreviewPreviewTheme(dynamicColor = false) {
        ToggleButton(
            leftIcon = if (toggleState == ToggleState.Left) {
                rememberVectorPainter(image = Icons.Filled.CameraAlt)
            } else {
                rememberVectorPainter(image = Icons.Outlined.CameraAlt)
            },
            rightIcon = if (toggleState == ToggleState.Right) {
                rememberVectorPainter(image = Icons.Filled.Videocam)
            } else {
                rememberVectorPainter(image = Icons.Outlined.Videocam)
            },
            initialState = ToggleState.Left,
            onToggleStateChanged = {
                toggleState = it
            }
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ToggleButton_Selecting_Right() {
    PreviewPreviewTheme(dynamicColor = false) {
        ToggleButton(
            leftIcon = rememberVectorPainter(image = Icons.Outlined.CameraAlt),
            rightIcon = rememberVectorPainter(image = Icons.Filled.Videocam),
            initialState = ToggleState.Right
        )
    }
}

@Preview(name = "Dark Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ToggleButton_Disabled() {
    PreviewPreviewTheme(dynamicColor = false) {
        ToggleButton(
            leftIcon = rememberVectorPainter(image = Icons.Outlined.CameraAlt),
            rightIcon = rememberVectorPainter(image = Icons.Filled.Videocam),
            initialState = ToggleState.Right,
            enabled = false
        )
    }
}
