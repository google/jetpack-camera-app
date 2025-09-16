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
package com.google.jetpackcamera.ui.components.capture

import JcaSwitch
import android.content.ContentResolver
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.SurfaceRequest
import androidx.camera.viewfinder.compose.MutableCoordinateTransformer
import androidx.camera.viewfinder.core.ImplementationMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutExpo
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VideoStable
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
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
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.ExternalCaptureMode
import com.google.jetpackcamera.model.StabilizationMode
import com.google.jetpackcamera.model.VideoQuality
import com.google.jetpackcamera.ui.components.capture.theme.PreviewPreviewTheme
import com.google.jetpackcamera.ui.uistate.DisableRationale
import com.google.jetpackcamera.ui.uistate.capture.AspectRatioUiState
import com.google.jetpackcamera.ui.uistate.capture.AudioUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureModeToggleUiState.Unavailable.isCaptureModeSelectable
import com.google.jetpackcamera.ui.uistate.capture.ElapsedTimeUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState
import com.google.jetpackcamera.ui.uistate.capture.SnackbarData
import com.google.jetpackcamera.ui.uistate.capture.StabilizationUiState
import com.google.jetpackcamera.ui.uistate.capture.compound.PreviewDisplayUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlin.time.Duration.Companion.nanoseconds
import androidx.camera.core.DynamicRange as CXDynamicRange


private const val TAG = "PreviewScreen"
private const val BLINK_TIME = 100L

@Composable
fun ElapsedTimeText(modifier: Modifier = Modifier, elapsedTimeUiState: ElapsedTimeUiState.Enabled) {
    Text(
        modifier = modifier,
        text = elapsedTimeUiState.elapsedTimeNanos.nanoseconds
            .toComponents { minutes, seconds, _ -> "%02d:%02d".format(minutes, seconds) },
        textAlign = TextAlign.Center
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PauseResumeToggleButton(
    modifier: Modifier = Modifier,
    onSetPause: (Boolean) -> Unit,
    size: Dp = ButtonDefaults.MediumContainerHeight,
    currentRecordingState: VideoRecordingState
) {
    if (currentRecordingState is VideoRecordingState.Active) {
        FilledIconToggleButton(
            checked = currentRecordingState is VideoRecordingState.Active.Recording,
            onCheckedChange = {
                onSetPause(
                    currentRecordingState !is VideoRecordingState.Active.Paused
                )
            },
            modifier = modifier.size(size)
        ) {
            Icon(
                modifier = Modifier
                    .size(ButtonDefaults.MediumIconSize),
                imageVector = when (currentRecordingState) {
                    is VideoRecordingState.Active.Recording -> Icons.Filled.Pause
                    is VideoRecordingState.Active.Paused -> Icons.Filled.PlayArrow
                },
                // todo(kc): move contentDescription to XML
                contentDescription = "pause resume toggle"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AmplitudeToggleButton(
    modifier: Modifier = Modifier,
    buttonSize: Dp = ButtonDefaults.MediumContainerHeight,
    audioUiState: AudioUiState,
    onToggleAudio: () -> Unit
) {
    val currentUiState = rememberUpdatedState(audioUiState)

    // Tweak the multiplier to amplitude to adjust the visualizer sensitivity
    val animatedAudioAlpha by animateFloatAsState(
        targetValue = EaseOutExpo.transform(
            (currentUiState.value.amplitude.toFloat()).coerceIn(
                0f,
                1f
            )
        ),
        label = "AudioAnimation"
    )
    Box(contentAlignment = Alignment.Center) {
        FilledIconToggleButton(
            modifier = modifier
                .size(buttonSize)
                .apply {
                    if (audioUiState is AudioUiState.Enabled.On) {
                        testTag(AMPLITUDE_HOT_TAG)
                    } else {
                        testTag(AMPLITUDE_NONE_TAG)
                    }
                }
                .drawBehind {
                    if (audioUiState is AudioUiState.Enabled.On) {
                        drawCircle(
                            color = Color.White,
                            radius = size.width * .55f,
                            alpha = animatedAudioAlpha // Animate alpha for a pulsing effect
                        )
                    }
                },
            checked = audioUiState is AudioUiState.Enabled.On,
            onCheckedChange = { onToggleAudio() },
            // todo shapes
            enabled = audioUiState is AudioUiState.Enabled
        ) {
            Icon(
                modifier = Modifier.size(ButtonDefaults.MediumIconSize),
                imageVector = if (currentUiState.value is AudioUiState.Enabled.On) {
                    Icons.Filled.Mic
                } else {
                    Icons.Filled.MicOff
                },
                contentDescription = stringResource(id = R.string.audio_visualizer_icon)
            )
        }
    }
}


@Composable
fun CaptureModeToggleButton(
    uiState: CaptureModeToggleUiState.Available,
    onChangeCaptureMode: (CaptureMode) -> Unit,
    onToggleWhenDisabled: (DisableRationale) -> Unit,
    modifier: Modifier = Modifier
) {
    // Captures hdr image (left) when output format is UltraHdr, else captures hdr video (right).
    val toggleState = remember(uiState.selectedCaptureMode) {
        when (uiState.selectedCaptureMode) {
            CaptureMode.IMAGE_ONLY, CaptureMode.STANDARD -> false //left
            CaptureMode.VIDEO_ONLY -> true //right
        }
    }

    val enabled =
        uiState.isCaptureModeSelectable(CaptureMode.VIDEO_ONLY) &&
                uiState.isCaptureModeSelectable(CaptureMode.IMAGE_ONLY) && uiState.selectedCaptureMode != CaptureMode.STANDARD

    JcaSwitch(
        checked = toggleState,
        onCheckedChange = {
            val newCaptureMode = if (toggleState) CaptureMode.IMAGE_ONLY else CaptureMode.VIDEO_ONLY
            onChangeCaptureMode(newCaptureMode)
        },
        modifier = modifier,
        enabled = enabled,
        // trackColor = TODO(),
        //  thumbColor = TODO(),
        leftIcon = if (uiState.selectedCaptureMode ==
            CaptureMode.IMAGE_ONLY
        ) {
            Icons.Filled.CameraAlt
        } else {
            Icons.Outlined.CameraAlt
        },
        rightIcon = if (uiState.selectedCaptureMode ==
            CaptureMode.VIDEO_ONLY
        ) {
            Icons.Filled.Videocam
        } else {
            Icons.Outlined.Videocam
        },
        //   offIconColor = TODO(),
        //    onIconColor = TODO()
    )
    /*
    ToggleButton(
        leftIcon = if (uiState.selectedCaptureMode ==
            CaptureMode.IMAGE_ONLY
        ) {
            rememberVectorPainter(image = Icons.Filled.CameraAlt)
        } else {
            rememberVectorPainter(image = Icons.Outlined.CameraAlt)
        },
        rightIcon = if (uiState.selectedCaptureMode ==
            CaptureMode.VIDEO_ONLY
        ) {
            rememberVectorPainter(image = Icons.Filled.Videocam)
        } else {
            rememberVectorPainter(image = Icons.Outlined.Videocam)
        },
        toggleState = toggleState,

        onToggle = {
            val newCaptureMode = when (toggleState) {
                ToggleState.Right -> CaptureMode.IMAGE_ONLY
                ToggleState.Left -> CaptureMode.VIDEO_ONLY
            }
            onChangeCaptureMode(newCaptureMode)
        },
        onToggleWhenDisabled = {
            val disabledReason: DisableRationale? =
                (
                        uiState.findSelectableStateFor(CaptureMode.VIDEO_ONLY) as?
                                SingleSelectableUiState.Disabled<CaptureMode>
                        )?.disabledReason
                    ?: (
                            uiState.findSelectableStateFor(CaptureMode.IMAGE_ONLY)
                                    as? SingleSelectableUiState.Disabled<CaptureMode>
                            )
                        ?.disabledReason
            disabledReason?.let { onToggleWhenDisabled(it) }
        },
        // toggle only enabled when both capture modes are available
        enabled = enabled,
        leftIconDescription =
            if (enabled) {
                stringResource(id = R.string.capture_mode_image_capture_content_description)
            } else {
                stringResource(
                    id = R.string.capture_mode_image_capture_content_description_disabled
                )
            },
        rightIconDescription =
            if (enabled) {
                stringResource(id = R.string.capture_mode_video_recording_content_description)
            } else {
                stringResource(
                    id = R.string.capture_mode_video_recording_content_description_disabled
                )
            },
        modifier = modifier
    )*/
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
                            context.getString(snackbarToShow.actionLabelRes!!)
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
    previewDisplayUiState: PreviewDisplayUiState,
    onTapToFocus: (x: Float, y: Float) -> Unit,
    onFlipCamera: () -> Unit,
    onScaleZoom: (Float) -> Unit,
    onRequestWindowColorMode: (Int) -> Unit,
    surfaceRequest: SurfaceRequest?,
    modifier: Modifier = Modifier
) {
    if (previewDisplayUiState.aspectRatioUiState !is AspectRatioUiState.Available) {
        return
    }
    val transformableState = rememberTransformableState(
        onTransformation = { pinchZoomChange, _, _ ->
            onScaleZoom(pinchZoomChange)
        }
    )

    surfaceRequest?.let {
        BoxWithConstraints(
            modifier
                .testTag(PREVIEW_DISPLAY)
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.TopCenter
        ) {
            val aspectRatio = (
                    previewDisplayUiState.aspectRatioUiState as
                            AspectRatioUiState.Available
                    ).selectedAspectRatio
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

            LaunchedEffect(previewDisplayUiState.lastBlinkTimeStamp) {
                if (previewDisplayUiState.lastBlinkTimeStamp != 0L) {
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
                        .pointerInput(onFlipCamera) {
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
fun CaptureButton(
    modifier: Modifier = Modifier,
    captureButtonUiState: CaptureButtonUiState,
    isQuickSettingsOpen: Boolean,
    externalCaptureMode: ExternalCaptureMode,
    onToggleQuickSettings: () -> Unit = {},
    onIncrementZoom: (Float) -> Unit = {},
    onCaptureImageWithUri: (
        ContentResolver,
        Uri?,
        Boolean,
        (ImageCaptureEvent, Int) -> Unit
    ) -> Unit = { _, _, _, _ -> },
    onStartVideoRecording: (
        Uri?,
        Boolean,
        (VideoCaptureEvent) -> Unit
    ) -> Unit = { _, _, _ -> },
    onStopVideoRecording: () -> Unit = {},
    onLockVideoRecording: (Boolean) -> Unit = {}
) {
    val multipleEventsCutter = remember { MultipleEventsCutter() }
    val context = LocalContext.current

    CaptureButton(
        modifier = modifier.testTag(CAPTURE_BUTTON),
        onIncrementZoom = onIncrementZoom,
        onImageCapture = {
            if (captureButtonUiState is CaptureButtonUiState.Enabled) {
                multipleEventsCutter.processEvent {
                    when (externalCaptureMode) {
                        is ExternalCaptureMode.StandardMode -> {
                            onCaptureImageWithUri(
                                context.contentResolver,
                                null,
                                true
                            ) { event: ImageCaptureEvent, _: Int ->
                                externalCaptureMode.onImageCapture(
                                    getImageCaptureEventForExternalCaptureMode(event)
                                )
                            }
                        }

                        is ExternalCaptureMode.ExternalImageCaptureMode -> {
                            onCaptureImageWithUri(
                                context.contentResolver,
                                externalCaptureMode.imageCaptureUri,
                                false
                            ) { event: ImageCaptureEvent, _: Int ->
                                externalCaptureMode.onImageCapture(
                                    getImageCaptureEventForExternalCaptureMode(event)
                                )
                            }
                        }

                        is ExternalCaptureMode.ExternalMultipleImageCaptureMode -> {
                            val ignoreUri =
                                externalCaptureMode.imageCaptureUris.isNullOrEmpty()
                            onCaptureImageWithUri(
                                context.contentResolver,
                                null,
                                externalCaptureMode.imageCaptureUris.isNullOrEmpty() ||
                                        ignoreUri
                            ) { event: ImageCaptureEvent, i: Int ->
                                externalCaptureMode.onImageCapture(
                                    getImageCaptureEventForExternalCaptureMode(event),
                                    i
                                )
                            }
                        }

                        else -> {
                            onCaptureImageWithUri(
                                context.contentResolver,
                                null,
                                false
                            ) { _: ImageCaptureEvent, _: Int -> }
                        }
                    }
                }
            }
            if (isQuickSettingsOpen) {
                onToggleQuickSettings()
            }
        },
        onStartRecording = {
            if (captureButtonUiState is CaptureButtonUiState.Enabled) {
                when (externalCaptureMode) {
                    is ExternalCaptureMode.StandardMode -> {
                        onStartVideoRecording(null, false) {}
                    }

                    is ExternalCaptureMode.ExternalVideoCaptureMode -> {
                        onStartVideoRecording(
                            externalCaptureMode.videoCaptureUri,
                            true

                        ) { event: VideoCaptureEvent ->
                            externalCaptureMode.onVideoCapture(
                                getVideoCaptureEventForExternalCaptureMode(event)
                            )
                        }
                    }

                    else -> {
                        onStartVideoRecording(null, false) {}
                    }
                }
                if (isQuickSettingsOpen) {
                    onToggleQuickSettings()
                }
            }
        },
        onStopRecording = {
            onStopVideoRecording()
        },
        captureButtonUiState = captureButtonUiState,
        onLockVideoRecording = onLockVideoRecording
    )
}

/**
 * Converts an internal [ImageCaptureEvent] to its corresponding [ExternalCaptureMode.ImageCaptureEvent]
 * representation.
 */
private fun getImageCaptureEventForExternalCaptureMode(
    captureEvent: ImageCaptureEvent
): ExternalCaptureMode.ImageCaptureEvent {
    return when (captureEvent) {
        is ImageCaptureEvent.ImageSaved ->
            ExternalCaptureMode.ImageCaptureEvent.ImageSaved(
                captureEvent.savedUri
            )

        is ImageCaptureEvent.ImageCaptureError ->
            ExternalCaptureMode.ImageCaptureEvent.ImageCaptureError(
                captureEvent.exception
            )
    }
}

/**
 * Converts an internal [VideoCaptureEvent] to its corresponding [ExternalCaptureMode.VideoCaptureEvent]
 * representation.
 */
private fun getVideoCaptureEventForExternalCaptureMode(
    captureEvent: VideoCaptureEvent
): ExternalCaptureMode.VideoCaptureEvent {
    return when (captureEvent) {
        is VideoCaptureEvent.VideoSaved ->
            ExternalCaptureMode.VideoCaptureEvent.VideoSaved(
                captureEvent.savedUri
            )

        is VideoCaptureEvent.VideoCaptureError ->
            ExternalCaptureMode.VideoCaptureEvent.VideoCaptureError(
                captureEvent.error
            )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun StabilizationIcon(
    stabilizationUiState: StabilizationUiState,
    modifier: Modifier = Modifier
) {
    if (stabilizationUiState is StabilizationUiState.Enabled) {
        val contentColor = Color.White.let {
            if (!stabilizationUiState.active) it.copy(alpha = 0.38f) else it
        }
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            if (stabilizationUiState.stabilizationMode != StabilizationMode.OFF) {
                Icon(
                    modifier = modifier.size(IconButtonDefaults.smallIconSize),

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
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoQualityIcon(videoQuality: VideoQuality, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalContentColor provides Color.White) {
        if (videoQuality != VideoQuality.UNSPECIFIED) {
            Icon(
                modifier = modifier.size(IconButtonDefaults.smallIconSize),

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
            )
        }
    }
}

@Composable
fun FlipCameraButton(
    enabledCondition: Boolean,
    flipLensUiState: FlipLensUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (flipLensUiState is FlipLensUiState.Available) {
        var rotation by remember { mutableFloatStateOf(0f) }
        val animatedRotation = remember { Animatable(0f) }
        var initialLaunch by remember { mutableStateOf(false) }

        // spin animate whenever lensfacing changes
        LaunchedEffect(flipLensUiState.selectedLensFacing) {
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
}

enum class ToggleState {
    Left,
    Right
}

//todo(kc): may need to recreate image toggle button to be scalable and support drag
@Composable
fun ToggleButton(
    leftIcon: Painter,
    rightIcon: Painter,
    modifier: Modifier = Modifier,
    toggleState: ToggleState? = null,
    onToggle: () -> Unit = {},
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
    val animatedTogglePosition by animateFloatAsState(
        when (toggleState) {
            ToggleState.Left -> 0f
            ToggleState.Right -> 1f
            null -> 0f
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
                    if (enabled && toggleState != null) {
                        onToggle()
                    } else {
                        onToggleWhenDisabled()
                    }
                }
            )
            .semantics {
                stateDescription = when (toggleState) {
                    ToggleState.Left -> leftIconDescription
                    ToggleState.Right -> rightIconDescription
                    null -> "unknown togglestate"
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
                    contentDescription = "leftIcon",
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
                    contentDescription = "rightIcon",
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
            toggleState = ToggleState.Left,
            onToggle = {}
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
            toggleState = ToggleState.Left,
            onToggle = {}
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
            toggleState = ToggleState.Right,
            onToggle = {},
            enabled = false
        )
    }
}
