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
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.widget.Toast
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.DynamicRange as CXDynamicRange
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
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
import androidx.core.view.ViewCompat
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.feature.preview.AudioUiState
import com.google.jetpackcamera.feature.preview.CaptureButtonUiState
import com.google.jetpackcamera.feature.preview.ElapsedTimeUiState
import com.google.jetpackcamera.feature.preview.PreviewUiState
import com.google.jetpackcamera.feature.preview.R
import com.google.jetpackcamera.feature.preview.StabilizationUiState
import com.google.jetpackcamera.feature.preview.ui.theme.PreviewPreviewTheme
import com.google.jetpackcamera.settings.model.AspectRatio
import com.google.jetpackcamera.settings.model.CaptureMode
import com.google.jetpackcamera.settings.model.LensFacing
import com.google.jetpackcamera.settings.model.StabilizationMode
import com.google.jetpackcamera.settings.model.VideoQuality
import kotlin.time.Duration.Companion.nanoseconds
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "PreviewScreen"
private const val BLINK_TIME = 100L

@Composable
fun CaptureControls(
    modifier: Modifier = Modifier,
    onImageCapture: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onLockVideoRecording: (Boolean) -> Unit,
    captureButtonUiState: CaptureButtonUiState,
    captureButtonSize: Float = 80f
) {
    var currentUiState = rememberUpdatedState(captureButtonUiState)
    val firstKeyPressed = remember { mutableStateOf<CaptureSource?>(null) }
    val isLongPressing = remember { mutableStateOf<Boolean>(false) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    // todo(kc): capture button and volume button controls should be aware of who started the pressed recording
    // todo(kc): include keycode for selfie stick?

    suspend fun onLongPress() {
        delay(ViewConfiguration.getLongPressTimeout().toLong())
        withContext(NonCancellable) {
            if (isLongPressing.value == false) {
                when (val current = currentUiState.value) {
                    is CaptureButtonUiState.Enabled.Idle -> when (current.captureMode) {
                        CaptureMode.STANDARD,
                        CaptureMode.VIDEO_ONLY -> {
                            isLongPressing.value = true
                            Log.d(TAG, "Starting recording")
                            onStartRecording()
                        }

                        CaptureMode.IMAGE_ONLY -> {}
                    }

                    else -> {}
                }
            }
        }
    }

    fun onPress(captureSource: CaptureSource) {
        if (firstKeyPressed.value == null) {
            firstKeyPressed.value = captureSource
            longPressJob = scope.launch { onLongPress() }
        }
    }

    fun onKeyUp(captureSource: CaptureSource) {
        // releasing while pressed recording
        if (firstKeyPressed.value == captureSource) {
            if (isLongPressing.value) {
                if (currentUiState.value is
                        CaptureButtonUiState.Enabled.Recording.PressedRecording
                ) {
                    Log.d(TAG, "Stopping recording")
                    onStopRecording()
                }
            }
            // on click
            else {
                when (val current = currentUiState.value) {
                    is CaptureButtonUiState.Enabled.Idle -> when (current.captureMode) {
                        CaptureMode.STANDARD,
                        CaptureMode.IMAGE_ONLY -> onImageCapture()

                        CaptureMode.VIDEO_ONLY -> {
                            onLockVideoRecording(true)
                            Log.d(TAG, "Starting recording")
                            onStartRecording()
                        }
                    }

                    CaptureButtonUiState.Enabled.Recording.LockedRecording -> onStopRecording()
                    CaptureButtonUiState.Enabled.Recording.PressedRecording,
                    CaptureButtonUiState.Unavailable -> {
                    }
                }
            }
            longPressJob?.cancel()
            longPressJob = null
            isLongPressing.value = false
            firstKeyPressed.value = null
        }
    }

    VolumeButtonsHandler(
        onPress = { captureSource -> onPress(captureSource) },
        onRelease = { captureSource -> onKeyUp(captureSource) }
    )
    CaptureButton(
        modifier = modifier,
        onPress = { captureSource -> onPress(captureSource) },
        onRelease = { captureSource -> onKeyUp(captureSource) },
        captureButtonUiState = captureButtonUiState,
        captureButtonSize = captureButtonSize
    )
}

/**
 * Handler for using certain key events buttons as capture buttons.
 */
@Composable
private fun VolumeButtonsHandler(
    onPress: (CaptureSource) -> Unit,
    onRelease: (CaptureSource) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    fun keyCodeToCaptureSource(keyCode: Int): CaptureSource = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> CaptureSource.VOLUME_UP
        KeyEvent.KEYCODE_VOLUME_DOWN -> CaptureSource.VOLUME_DOWN
        else -> TODO("Keycode not assigned to CaptureSource")
    }

    DisposableEffect(context) {
        val keyEventDispatcher = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val captureSource = keyCodeToCaptureSource(event.keyCode)
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        // pressed down
                        onPress(captureSource)
                    }
                    // released
                    if (event.action == KeyEvent.ACTION_UP) {
                        onRelease(captureSource)
                    }
                    // consume the event
                    true
                }
                else -> {
                    false
                }
            }
        }

        ViewCompat.addOnUnhandledKeyEventListener(view, keyEventDispatcher)

        onDispose {
            ViewCompat.removeOnUnhandledKeyEventListener(view, keyEventDispatcher)
        }
    }
}

@Composable
fun ElapsedTimeText(modifier: Modifier = Modifier, elapsedTimeUiState: ElapsedTimeUiState.Enabled) {
    Text(
        modifier = modifier,
        text = elapsedTimeUiState.elapsedTimeNanos.nanoseconds
            .toComponents { minutes, seconds, _ -> "%02d:%02d".format(minutes, seconds) },
        textAlign = TextAlign.Center
    )
}

@Composable
fun PauseResumeToggleButton(
    modifier: Modifier = Modifier,
    onSetPause: (Boolean) -> Unit,
    size: Float = 55f,
    currentRecordingState: VideoRecordingState.Active
) {
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
    Box(
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .clickable(
                    onClick = {
                        buttonClicked = true
                        onSetPause(currentRecordingState !is VideoRecordingState.Active.Paused)
                    },
                    indication = null,
                    interactionSource = null
                )
                .size(size = size.dp)
                .scale(scale = animatedToggleScale)
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            // icon
            Icon(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size((0.75 * size).dp),
                tint = Color.Red,
                imageVector = when (currentRecordingState) {
                    is VideoRecordingState.Active.Recording -> Icons.Filled.Pause
                    is VideoRecordingState.Active.Paused -> Icons.Filled.PlayArrow
                },
                contentDescription = "pause resume toggle"
            )
        }
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
        targetValue = EaseOutExpo.transform(1 + (1.75f * currentUiState.value.amplitude.toFloat())),
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

private enum class CaptureSource {
    CAPTURE_BUTTON,
    VOLUME_UP,
    VOLUME_DOWN
}

@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    onImageCapture: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onLockVideoRecording: (Boolean) -> Unit,
    captureButtonUiState: CaptureButtonUiState,
    captureButtonSize: Float = 80f
) {
    var currentUiState = rememberUpdatedState(captureButtonUiState)
    val firstKeyPressed = remember { mutableStateOf<CaptureSource?>(null) }
    val isLongPressing = remember { mutableStateOf<Boolean>(false) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun onLongPress() {
        delay(ViewConfiguration.getLongPressTimeout().toLong())
        withContext(NonCancellable) {
            if (isLongPressing.value == false) {
                when (val current = currentUiState.value) {
                    is CaptureButtonUiState.Enabled.Idle -> when (current.captureMode) {
                        CaptureMode.STANDARD,
                        CaptureMode.VIDEO_ONLY -> {
                            isLongPressing.value = true
                            Log.d(TAG, "Starting recording")
                            onStartRecording()
                        }

                        CaptureMode.IMAGE_ONLY -> {
                            isLongPressing.value = true
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun onPress(captureSource: CaptureSource) {
        if (firstKeyPressed.value == null) {
            firstKeyPressed.value = captureSource
            longPressJob = scope.launch { onLongPress() }
        }
    }

    fun onKeyUp(captureSource: CaptureSource) {
        // releasing while pressed recording
        if (firstKeyPressed.value == captureSource) {
            if (isLongPressing.value) {
                if (currentUiState.value is
                        CaptureButtonUiState.Enabled.Recording.PressedRecording
                ) {
                    Log.d(TAG, "Stopping recording")
                    onStopRecording()
                }
            }
            // on click
            else {
                when (val current = currentUiState.value) {
                    is CaptureButtonUiState.Enabled.Idle -> when (current.captureMode) {
                        CaptureMode.STANDARD,
                        CaptureMode.IMAGE_ONLY -> onImageCapture()

                        CaptureMode.VIDEO_ONLY -> {
                            onLockVideoRecording(true)
                            Log.d(TAG, "Starting recording")
                            onStartRecording()
                        }
                    }

                    CaptureButtonUiState.Enabled.Recording.LockedRecording -> onStopRecording()
                    CaptureButtonUiState.Enabled.Recording.PressedRecording,
                    CaptureButtonUiState.Unavailable -> {}
                }
            }
            longPressJob?.cancel()
            longPressJob = null
            isLongPressing.value = false
            firstKeyPressed.value = null
        }
    }

    CaptureKeyHandler(
        onPress = { captureSource -> onPress(captureSource) },
        onRelease = { captureSource -> onKeyUp(captureSource) }
    )
    CaptureButton(
        modifier = modifier,
        onPress = { captureSource -> onPress(captureSource) },
        onRelease = { captureSource -> onKeyUp(captureSource) },
        captureButtonUiState = captureButtonUiState,
        captureButtonSize = captureButtonSize
    )
}

@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    onPress: (CaptureSource) -> Unit,
    onRelease: (CaptureSource) -> Unit,
    captureButtonUiState: CaptureButtonUiState,
    captureButtonSize: Float = 80f
) {
    var currentUiState = rememberUpdatedState(captureButtonUiState)
    var isCaptureButtonPressed by remember { mutableStateOf(false) }
    val currentColor = LocalContentColor.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    // onLongPress cannot be null, otherwise it won't detect the release if the
                    // touch is dragged off the component
                    onLongPress = {},
                    onPress = {
                        isCaptureButtonPressed = true
                        onPress(CaptureSource.CAPTURE_BUTTON)
                        awaitRelease()
                        isCaptureButtonPressed = false
                        onRelease(CaptureSource.CAPTURE_BUTTON)
                    }
                )
            }
            .size(captureButtonSize.dp)
            .border(4.dp, currentColor, CircleShape) // border is the white ring
    ) {
        // now we draw center circle
        val centerShapeSize by animateDpAsState(
            targetValue = when (val uiState = currentUiState.value) {
                // inner circle fills white ring when locked
                CaptureButtonUiState.Enabled.Recording.LockedRecording -> captureButtonSize.dp
                // larger circle while recording, but not max size
                CaptureButtonUiState.Enabled.Recording.PressedRecording ->
                    (captureButtonSize * .7f).dp

                CaptureButtonUiState.Unavailable -> 0.dp
                is CaptureButtonUiState.Enabled.Idle -> when (uiState.captureMode) {
                    // no inner circle will be visible on STANDARD
                    CaptureMode.STANDARD -> 0.dp
                    // large white circle will be visible on IMAGE_ONLY
                    CaptureMode.IMAGE_ONLY -> (captureButtonSize * .7f).dp
                    // small red circle will be visible on VIDEO_ONLY
                    CaptureMode.VIDEO_ONLY -> (captureButtonSize * .35f).dp
                }
            },
            animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
        )

        // used to fade between red/white in the center of the capture button
        val animatedColor by animateColorAsState(
            targetValue = when (val uiState = currentUiState.value) {
                is CaptureButtonUiState.Enabled.Idle -> when (uiState.captureMode) {
                    CaptureMode.STANDARD -> Color.White
                    CaptureMode.IMAGE_ONLY -> Color.White
                    CaptureMode.VIDEO_ONLY -> Color.Red
                }

                is CaptureButtonUiState.Enabled.Recording -> Color.Red
                is CaptureButtonUiState.Unavailable -> Color.Transparent
            },
            animationSpec = tween(durationMillis = 500)
        )
        // inner circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(centerShapeSize)
                .clip(CircleShape)
                .background(animatedColor)
                .alpha(
                    if (isCaptureButtonPressed &&
                        currentUiState.value ==
                        CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY)
                    ) {
                        .5f // transparency to indicate click ONLY on IMAGE_ONLY
                    } else {
                        1f // solid alpha the rest of the time
                    }
                )
                .background(animatedColor)
        ) {}
        // central "square" stop icon
        AnimatedVisibility(
            visible = currentUiState.value is
                CaptureButtonUiState.Enabled.Recording.LockedRecording,
            enter = scaleIn(initialScale = .5f) + fadeIn(),
            exit = fadeOut()
        ) {
            val smallBoxSize = (captureButtonSize / 5f).dp
            Canvas(modifier = Modifier) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(-smallBoxSize.toPx() / 2f, -smallBoxSize.toPx() / 2f),
                    size = Size(smallBoxSize.toPx(), smallBoxSize.toPx()),
                    cornerRadius = CornerRadius(smallBoxSize.toPx() * .15f)
                )
            }
        }
    }
}

/**
 * Handler for using certain key events buttons as capture buttons.
 */
@Composable
private fun CaptureKeyHandler(
    onPress: (CaptureSource) -> Unit,
    onRelease: (CaptureSource) -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    fun keyCodeToCaptureSource(keyCode: Int): CaptureSource = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> CaptureSource.VOLUME_UP
        KeyEvent.KEYCODE_VOLUME_DOWN -> CaptureSource.VOLUME_DOWN
        else -> TODO("Keycode not assigned to CaptureSource")
    }

    DisposableEffect(context) {
        val keyEventDispatcher = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val captureSource = keyCodeToCaptureSource(event.keyCode)
                    if (event.action == KeyEvent.ACTION_DOWN) {
                        // pressed down
                        onPress(captureSource)
                    }
                    // released
                    if (event.action == KeyEvent.ACTION_UP) {
                        onRelease(captureSource)
                    }
                    // consume the event
                    true
                }
                else -> {
                    false
                }
            }
        }

        ViewCompat.addOnUnhandledKeyEventListener(view, keyEventDispatcher)

        onDispose {
            ViewCompat.removeOnUnhandledKeyEventListener(view, keyEventDispatcher)
        }
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
