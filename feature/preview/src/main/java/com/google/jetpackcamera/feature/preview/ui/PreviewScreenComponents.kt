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

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.DynamicRange as CXDynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoStable
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CameraAlt
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.feature.preview.AudioUiState
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.StabilizationUiState
import com.google.jetpackcamera.feature.preview.ui.theme.PreviewPreviewTheme
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.VideoQuality
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion

private const val TAG = "PreviewScreen"
private const val BLINK_TIME = 100L

@Composable
fun ElapsedTimeText(
    modifier: Modifier = Modifier,
    videoRecordingState: VideoRecordingState,
    elapsedNs: Long
) {
    AnimatedVisibility(
        visible = (videoRecordingState is VideoRecordingState.Active),
        enter = fadeIn(),
        exit = fadeOut(animationSpec = tween(delayMillis = 1000))
    ) {
        Text(
            modifier = modifier,
            text = elapsedNs.nanoseconds.toComponents { minutes, seconds, _ ->
                "%02d:%02d".format(minutes, seconds)
            },
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PauseResumeToggleButton(
    modifier: Modifier = Modifier,
    onSetPause: (Boolean) -> Unit,
    size: Float = 80f,
    currentRecordingState: VideoRecordingState.Active
) {
    Box(
        modifier = modifier.clickable {
            onSetPause(currentRecordingState !is VideoRecordingState.Active.Paused)
        }
    ) {
        // static circle
        Canvas(
            modifier = Modifier
                .align(Alignment.Center),
            onDraw = {
                drawCircle(
                    radius = (size),
                    color = Color.White
                )
            }
        )

        // icon
        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .size((0.5 * size).dp),
            tint = Color.Red,

            imageVector = when (currentRecordingState) {
                is VideoRecordingState.Active.Recording -> Icons.Filled.Pause
                is VideoRecordingState.Active.Paused -> Icons.Filled.PlayArrow
            },
            contentDescription = "pause resume toggle"
        )
    }
}

@Composable
fun AmplitudeVisualizer(
    modifier: Modifier = Modifier,
    size: Float = 75f,
    audioUiState: AudioUiState,
    onToggleAudio: () -> Unit
) {
    val currentUiState = rememberUpdatedState(audioUiState)
    var buttonClicked by remember { mutableStateOf(false) }
    // animation value for the toggle icon itself
    val animatedToggleScale by animateFloatAsState(
        targetValue = if (buttonClicked) 1.1f else 1f, // Scale up to 110%
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        finishedListener = {
            buttonClicked = false // Reset the trigger
        }
    )

    // Tweak the multiplier to amplitude to adjust the visualizer sensitivity
    val animatedAudioScale by animateFloatAsState(
        targetValue = 1 + (1.75f * currentUiState.value.amplitude.toFloat()),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "AudioAnimation"
    )
    Box(
        modifier = modifier.clickable(
            onClick = {
                buttonClicked = true
                onToggleAudio()
            },
            interactionSource = null,
            // removes the greyish background animation that appears when clicking
            indication = null
        )
    ) {
        // animated audio circle
        Canvas(
            modifier = Modifier
                .align(Alignment.Center),
            onDraw = {
                drawCircle(
                    // tweak the multiplier to size to adjust the maximum size of the visualizer
                    radius = (size * animatedAudioScale).coerceIn(size, size * 1.65f),
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
                    radius = (size * animatedToggleScale),
                    color = Color.White
                )
            }
        )

        Icon(
            modifier = Modifier
                .align(Alignment.Center)
                .size((0.5 * size).dp)
                .scale(animatedToggleScale)
                .apply {
                    if (currentUiState.value is AudioUiState.Enabled.On) {
                        testTag(AMPLITUDE_HOT_TAG)
                    } else {
                        testTag(AMPLITUDE_NONE_TAG)
                    }
                },
            tint = Color.Black,
            imageVector = if (currentUiState.value is AudioUiState.Enabled.On) {
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

@Composable
fun DetectWindowColorModeChanges(
    surfaceRequest: SurfaceRequest,
    implementationMode: ImplementationMode,
    onRequestWindowColorMode: (Int) -> Unit
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val currentSurfaceRequest: SurfaceRequest by rememberUpdatedState(surfaceRequest)
        val currentImplementationMode: ImplementationMode by rememberUpdatedState(
            implementationMode
        )
        val currentOnRequestWindowColorMode: (Int) -> Unit by rememberUpdatedState(
            onRequestWindowColorMode
        )

        LaunchedEffect(Unit) {
            val colorModeSnapshotFlow =
                snapshotFlow { Pair(currentSurfaceRequest.dynamicRange, currentImplementationMode) }
                    .map { (dynamicRange, implMode) ->
                        val isSourceHdr = dynamicRange.encoding != CXDynamicRange.ENCODING_SDR
                        val destSupportsHdr = implMode == ImplementationMode.EXTERNAL
                        if (isSourceHdr && destSupportsHdr) {
                            ActivityInfo.COLOR_MODE_HDR
                        } else {
                            ActivityInfo.COLOR_MODE_DEFAULT
                        }
                    }.distinctUntilChanged()

            val callbackSnapshotFlow = snapshotFlow { currentOnRequestWindowColorMode }

            // Combine both flows so that we call the callback every time it changes or the
            // window color mode changes.
            // We'll also reset to default when this LaunchedEffect is disposed
            combine(colorModeSnapshotFlow, callbackSnapshotFlow) { colorMode, callback ->
                Pair(colorMode, callback)
            }.onCompletion {
                currentOnRequestWindowColorMode(ActivityInfo.COLOR_MODE_DEFAULT)
            }.collect { (colorMode, callback) ->
                callback(colorMode)
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

    surfaceRequest?.let {
        BoxWithConstraints(
            modifier
                .testTag(PREVIEW_DISPLAY)
                .fillMaxSize()
                .background(Color.Black),
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
                val implementationMode = when {
                    Build.VERSION.SDK_INT > 24 -> ImplementationMode.EXTERNAL
                    else -> ImplementationMode.EMBEDDED
                }

                DetectWindowColorModeChanges(
                    surfaceRequest = surfaceRequest,
                    implementationMode = implementationMode,
                    onRequestWindowColorMode = onRequestWindowColorMode
                )

                val coordinateTransformer = remember { MutableCoordinateTransformer() }
                CameraXViewfinder(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onDoubleTap = { offset ->
                                    // double tap to flip camera
                                    Log.d(TAG, "onDoubleTap $offset")
                                    onFlipCamera()
                                },
                                onTap = {
                                    with(coordinateTransformer) {
                                        val surfaceCoords = it.transform()
                                        Log.d(
                                            "TAG",
                                            "onTapToFocus: " +
                                                "input{$it} -> surface{$surfaceCoords}"
                                        )
                                        onTapToFocus(surfaceCoords.x, surfaceCoords.y)
                                    }
                                }
                            )
                        },
                    surfaceRequest = it,
                    implementationMode = implementationMode,
                    coordinateTransformer = coordinateTransformer
                )
            }
        }
    }
}

@Composable
fun StabilizationIcon(
    stabilizationUiState: StabilizationUiState.Enabled,
    modifier: Modifier = Modifier
) {
    val contentColor = Color.White.let {
        if (!stabilizationUiState.active) it.copy(alpha = 0.38f) else it
    }
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        if (stabilizationUiState.stabilizationMode != StabilizationMode.OFF) {
            Icon(
                painter = when (stabilizationUiState) {
                    is StabilizationUiState.Specific ->
                        when (stabilizationUiState.stabilizationMode) {
                            StabilizationMode.AUTO ->
                                throw IllegalStateException(
                                    "AUTO is not a specific StabilizationUiState."
                                )

                            StabilizationMode.HIGH_QUALITY ->
                                painterResource(R.drawable.video_stable_hq_filled_icon)

                            StabilizationMode.OPTICAL ->
                                painterResource(R.drawable.video_stable_ois_filled_icon)

                            StabilizationMode.ON ->
                                rememberVectorPainter(Icons.Filled.VideoStable)

                            else ->
                                TODO(
                                    "Cannot retrieve icon for unimplemented stabilization mode:" +
                                        "${stabilizationUiState.stabilizationMode}"
                                )
                        }

                    is StabilizationUiState.Auto -> {
                        when (stabilizationUiState.stabilizationMode) {
                            StabilizationMode.ON ->
                                painterResource(R.drawable.video_stable_auto_filled_icon)

                            StabilizationMode.OPTICAL ->
                                painterResource(R.drawable.video_stable_ois_auto_filled_icon)

                            else ->
                                TODO(
                                    "Auto stabilization not yet implemented for " +
                                        "${stabilizationUiState.stabilizationMode}, " +
                                        "unable to retrieve icon."
                                )
                        }
                    }
                },
                contentDescription = when (stabilizationUiState.stabilizationMode) {
                    StabilizationMode.AUTO ->
                        stringResource(R.string.stabilization_icon_description_auto)

                    StabilizationMode.ON ->
                        stringResource(R.string.stabilization_icon_description_preview_and_video)

                    StabilizationMode.HIGH_QUALITY ->
                        stringResource(R.string.stabilization_icon_description_video_only)

                    StabilizationMode.OPTICAL ->
                        stringResource(R.string.stabilization_icon_description_optical)

                    else -> null
                },
                modifier = modifier
            )
        }
    }
}

@Composable
fun VideoQualityIcon(videoQuality: VideoQuality, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        if (videoQuality != VideoQuality.UNSPECIFIED) {
            Icon(
                painter = when (videoQuality) {
                    VideoQuality.SD ->
                        painterResource(R.drawable.video_resolution_sd_icon)

                    VideoQuality.HD ->
                        painterResource(R.drawable.video_resolution_hd_icon)

                    VideoQuality.FHD ->
                        painterResource(R.drawable.video_resolution_fhd_icon)

                    VideoQuality.UHD ->
                        painterResource(R.drawable.video_resolution_uhd_icon)

                    else ->
                        throw IllegalStateException(
                            "Illegal video quality state"
                        )
                },
                contentDescription = when (videoQuality) {
                    VideoQuality.SD ->
                        stringResource(R.string.video_quality_description_sd)

                    VideoQuality.HD ->
                        stringResource(R.string.video_quality_description_hd)

                    VideoQuality.FHD ->
                        stringResource(R.string.video_quality_description_fhd)

                    VideoQuality.UHD ->
                        stringResource(R.string.video_quality_description_uhd)

                    else -> null
                },
                modifier = modifier
            )
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
    lensFacing: LensFacing,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var rotation by remember { mutableFloatStateOf(0f) }
    val animatedRotation = remember { Animatable(0f) }
    var initialLaunch by remember { mutableStateOf(false) }

    // spin animate whenever lensfacing changes
    LaunchedEffect(lensFacing) {
        if (initialLaunch) {
            // full 360
            rotation -= 180f
            animatedRotation.animateTo(
                targetValue = rotation,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessVeryLow
                )
            )
        }
        // dont rotate on the initial launch
        else {
            initialLaunch = true
        }
    }
    IconButton(
        modifier = modifier.size(40.dp),
        onClick = onClick,
        enabled = enabledCondition
    ) {
        Icon(
            imageVector = Icons.Filled.FlipCameraAndroid,
            contentDescription = stringResource(id = R.string.flip_camera_content_description),
            modifier = Modifier
                .size(72.dp)
                .rotate(animatedRotation.value)
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
            .alpha(contentAlpha.value)
            .testTag(ZOOM_RATIO_TAG),
        text = stringResource(id = R.string.zoom_scale_text, zoomScale)
    )
}

@Composable
fun CurrentCameraIdText(physicalCameraId: String?, logicalCameraId: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row {
            Text(text = stringResource(R.string.debug_text_logical_camera_id_prefix))
            Text(
                modifier = Modifier.testTag(LOGICAL_CAMERA_ID_TAG),
                text = logicalCameraId ?: "---"
            )
        }
        Row {
            Text(text = stringResource(R.string.debug_text_physical_camera_id_prefix))
            Text(
                modifier = Modifier.testTag(PHYSICAL_CAMERA_ID_TAG),
                text = physicalCameraId ?: "---"
            )
        }
    }
}

@Composable
private fun CaptureModeDropDown() {
    var isExpanded by remember { mutableStateOf(false) }
    var selectedOption by remember { mutableStateOf("Default") }

    Column {
        Box(
            modifier = Modifier
                .clickable { isExpanded = !isExpanded }
                .padding(8.dp)
        ) {
            Text(text = selectedOption, modifier = Modifier.padding(16.dp))
        }
        AnimatedVisibility(visible = isExpanded) {
            Column {
                Text(
                    text = "Default",
                    modifier = Modifier
                        .clickable {
                            selectedOption = "Default"
                            isExpanded = false
                        }
                        .padding(16.dp)
                )
                Text(
                    text = "Image Only",
                    modifier = Modifier
                        .clickable {
                            selectedOption = "Image Only"
                            isExpanded = false
                        }
                        .padding(16.dp)
                )
                Text(
                    text = "Video Only",
                    modifier = Modifier
                        .clickable {
                            selectedOption = "Video Only"
                            isExpanded = false
                        }
                        .padding(16.dp)
                )
            }
        }
    }
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
        Canvas(
            modifier = Modifier
                .size(110.dp),
            onDraw = {
                drawCircle(
                    alpha = when (videoRecordingState) {
                        is VideoRecordingState.Active.Paused -> .37f
                        else -> 1f
                    },
                    color =
                    when (videoRecordingState) {
                        is VideoRecordingState.Inactive -> {
                            if (isPressedDown) currentColor else Color.Transparent
                        }

                        is VideoRecordingState.Active.Recording,
                        is VideoRecordingState.Active.Paused -> Color.Red

                        VideoRecordingState.Starting -> currentColor
                    }
                )
            }
        )
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
    modifier: Modifier = Modifier,
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

    Surface(
        modifier = modifier
            .clip(shape = RoundedCornerShape(50))
            .then(
                Modifier.clickable(
                    role = Role.Switch
                ) {
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
            )
            .semantics {
                stateDescription = when (toggleState) {
                    ToggleState.Left -> leftIconDescription
                    ToggleState.Right -> rightIconDescription
                }
            }
            .width(64.dp)
            .height(32.dp),
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
