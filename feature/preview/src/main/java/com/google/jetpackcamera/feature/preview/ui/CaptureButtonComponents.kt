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

package com.google.jetpackcamera.feature.preview.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.feature.preview.CaptureButtonUiState
import com.google.jetpackcamera.settings.model.CaptureMode

private const val DEFAULT_CAPTURE_BUTTON_SIZE = 80f
private const val LOCK_SWITCH_SIZE_RATIO = 2.75f
private const val LOCK_SWITCH_LOCK_THRESHOLD = .4F

private const val LOCK_SWITCH_PRESSED_NUCLEUS_SCALE = .5f
private const val LOCK_SWITCH_HEIGHT_SCALE = 1.4f

@Composable
fun CaptureButton(
    modifier: Modifier = Modifier,
    onCaptureImage: () -> Unit,
    onStartVideoRecording: () -> Unit,
    onStopVideoRecording: () -> Unit,
    onLockVideoRecording: (Boolean) -> Unit,
    captureButtonUiState: CaptureButtonUiState,
    useLockSwitch: Boolean = true,
    captureButtonSize: Float = DEFAULT_CAPTURE_BUTTON_SIZE
) {
    val currentColor = LocalContentColor.current
    LaunchedEffect(captureButtonUiState) {
        if (captureButtonUiState is CaptureButtonUiState.Enabled.Idle) {
            onLockVideoRecording(false)
        }
    }
    val currentUiState = rememberUpdatedState(captureButtonUiState)
    var isPressedDown by remember {
        mutableStateOf(false)
    }
    var isLongPressing by remember {
        mutableStateOf(false)
    }
    var switchPosition by remember { mutableFloatStateOf(0f) } // 1f = left, 0f = right
    val switchWidth = ((captureButtonSize / 2) * LOCK_SWITCH_SIZE_RATIO).dp // todo

    fun shouldBeLocked(): Boolean = switchPosition > LOCK_SWITCH_LOCK_THRESHOLD
    fun toggleSwitchPosition() = if (shouldBeLocked()) {
        switchPosition = 0f
    } else {
        switchPosition =
            1f
    }

    Box(contentAlignment = Alignment.Center) {
        if (useLockSwitch) {
            LockSwitchCaptureButtonNucleus(
                modifier = Modifier.align(Alignment.Center),
                captureButtonUiState = captureButtonUiState,
                captureButtonSize = captureButtonSize,
                switchWidth = switchWidth,
                switchPosition = switchPosition,
                onToggleSwitchPosition = { toggleSwitchPosition() },
                shouldBeLocked = { shouldBeLocked() }
            )
        } else {
            CaptureButtonNucleus(
                captureButtonUiState = captureButtonUiState,
                isPressed = isPressedDown,
                captureButtonSize = captureButtonSize
            )
        }

        // capture button ring
        // todo(): move this out into its own componenet
        Box(
            contentAlignment = Alignment.Center,
            modifier = modifier
                .align(Alignment.Center)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            isLongPressing = true
                            val uiState = currentUiState.value
                            if (uiState is CaptureButtonUiState.Enabled.Idle) {
                                when (uiState.captureMode) {
                                    CaptureMode.STANDARD,
                                    CaptureMode.VIDEO_ONLY -> {
                                        onStartVideoRecording()
                                    }

                                    CaptureMode.IMAGE_ONLY -> {}
                                }
                            }
                        },
                        onPress = {
                            isPressedDown = true
                            awaitRelease()
                            isPressedDown = false
                            isLongPressing = false
                            val uiState = currentUiState.value
                            when (uiState) {
                                // stop recording after button is lifted
                                is CaptureButtonUiState.Enabled.Recording.PressedRecording -> {
                                    if (shouldBeLocked()) {
                                        onLockVideoRecording(true)
                                        switchPosition = 0f
                                    } else {
                                        onStopVideoRecording()
                                    }
                                }

                                is CaptureButtonUiState.Enabled.Idle,
                                CaptureButtonUiState.Unavailable -> {
                                }

                                CaptureButtonUiState.Enabled.Recording.LockedRecording -> {}
                            }
                        },
                        onTap = {
                            val uiState = currentUiState.value
                            when (uiState) {
                                is CaptureButtonUiState.Enabled.Idle -> {
                                    if (!isLongPressing) {
                                        when (uiState.captureMode) {
                                            CaptureMode.STANDARD,
                                            CaptureMode.IMAGE_ONLY -> onCaptureImage()

                                            CaptureMode.VIDEO_ONLY -> {
                                                onLockVideoRecording(true)
                                                onStartVideoRecording()
                                            }
                                        }
                                    }
                                }
                                // stop if locked recording
                                CaptureButtonUiState.Enabled.Recording.LockedRecording -> {
                                    onStopVideoRecording()
                                }

                                CaptureButtonUiState.Unavailable,
                                CaptureButtonUiState.Enabled.Recording.PressedRecording -> {
                                }
                            }
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDragStart = { },
                        onDragEnd = { },
                        onDrag = { change, dragAmount ->
                            val newPosition =
                                switchPosition - (dragAmount.x / switchWidth.toPx())
                            switchPosition = newPosition.coerceIn(0f, 1f)
                            change.consume()
                        }
                    )
                }
                .size(captureButtonSize.dp)
                .border(4.dp, currentColor, CircleShape) // border is the white ring
        ) {}
    }
}

/**
 * A nucleus for the capture button can be dragged to lock the video recording.
 *
 */
@Composable
private fun LockSwitchCaptureButtonNucleus(
    modifier: Modifier = Modifier,
    captureButtonUiState: CaptureButtonUiState,
    captureButtonSize: Float,
    switchWidth: Dp,
    switchPosition: Float,
    onToggleSwitchPosition: () -> Unit,
    shouldBeLocked: () -> Boolean
) {
    val pressedNucleusSize = (captureButtonSize * LOCK_SWITCH_PRESSED_NUCLEUS_SCALE).dp
    val switchHeight = (pressedNucleusSize * LOCK_SWITCH_HEIGHT_SCALE)

    Box(
        modifier = modifier
            .width(switchWidth),
        contentAlignment = Alignment.Center

    ) {
        Box(
            contentAlignment = Alignment.CenterStart,
            modifier = Modifier
                .width(switchWidth)
                .height(switchHeight)
                .offset(x = -(switchWidth - pressedNucleusSize) / 2)
        ) {
            // grey cylinder offset to the left fades in when pressed recording
            AnimatedVisibility(
                visible = captureButtonUiState ==
                    CaptureButtonUiState.Enabled.Recording.PressedRecording,
                enter = fadeIn(),
                exit = ExitTransition.None
            ) {
                // grey cylinder
                Canvas(
                    modifier = Modifier
                        .size(switchWidth, switchHeight)
                        .alpha(.37f)
                ) {
                    drawRoundRect(
                        color = Color.Black,
                        cornerRadius = CornerRadius((switchWidth / 2).toPx())
                    )
                }
            }
        }

        // small moveable Circle remains centered.
        // is behind lock icon but in front of the switch background

        CaptureButtonNucleus(
            offsetX = (-(switchWidth - pressedNucleusSize) * switchPosition),
            captureButtonSize = captureButtonSize,
            captureButtonUiState = captureButtonUiState,
            pressedVideoCaptureScale = LOCK_SWITCH_PRESSED_NUCLEUS_SCALE,
            isPressed = false
        )

        // locked icon, matches cylinder offset
        AnimatedVisibility(
            visible = captureButtonUiState ==
                CaptureButtonUiState.Enabled.Recording.PressedRecording,
            enter = fadeIn(),
            exit = ExitTransition.None
        ) {
            Icon(
                modifier = Modifier
                    .size(switchHeight * .75f)
                    .align(Alignment.CenterStart)
                    .padding(start = 8.dp)
                    .offset(x = -(switchWidth - pressedNucleusSize))
                    .clickable(indication = null, interactionSource = null) {
                        onToggleSwitchPosition()
                    },
                tint = Color.White,
                imageVector = if (shouldBeLocked()) {
                    Icons.Default.Lock
                } else {
                    Icons.Default.LockOpen
                },
                contentDescription = null
            )
        }
    }
}

/**
 * The animated center of the capture button. It serves as a visual indicator of the current capture and recording states.
 *
 * @param captureButtonSize diameter of the capture button ring that this is scaled to
 * @param isPressed true if the capture button is physically pressed on
 * @param offsetX the offset of this component. 0 by default
 * @param idleImageCaptureScale the scale factor for the idle size of the image-only nucleus. Must be between 0 and 1.
 * @param idleVideoCaptureScale the scale factor for the idle size of the video-only nucleus. Must be between 0 and 1.
 * @param pressedVideoCaptureScale the scale factor for the pressed size of the video-only nucleus. Must be between 0 and 1.
 */
@Composable
private fun CaptureButtonNucleus(
    modifier: Modifier = Modifier,
    captureButtonUiState: CaptureButtonUiState,
    isPressed: Boolean,
    captureButtonSize: Float,
    offsetX: Dp = 0.dp,
    recordingColor: Color = Color.Red,
    imageCaptureModeColor: Color = Color.White,
    idleImageCaptureScale: Float = .7f,
    idleVideoCaptureScale: Float = .35f,
    pressedVideoCaptureScale: Float = .7f
) {
    require(idleImageCaptureScale in 0f..1f) {
        "value must be between 0 and 1 to remain within the bounds of the capture button"
    }
    require(idleVideoCaptureScale in 0f..1f) {
        "value must be between 0 and 1 to remain within the bounds of the capture button"
    }
    require(pressedVideoCaptureScale in 0f..1f) {
        "value must be between 0 and 1 to remain within the bounds of the capture button"
    }

    val currentUiState = rememberUpdatedState(captureButtonUiState)

    // smoothly animate between the size changes of the capture button center
    val centerShapeSize by animateDpAsState(
        targetValue = when (val uiState = currentUiState.value) {
            // inner circle fills white ring when locked
            CaptureButtonUiState.Enabled.Recording.LockedRecording -> captureButtonSize.dp
            // using the circle in the switch
            CaptureButtonUiState.Enabled.Recording.PressedRecording -> (
                captureButtonSize *
                    pressedVideoCaptureScale
                ).dp

            CaptureButtonUiState.Unavailable -> 0.dp
            is CaptureButtonUiState.Enabled.Idle -> when (uiState.captureMode) {
                // no inner circle will be visible on STANDARD
                CaptureMode.STANDARD -> 0.dp
                // large white circle will be visible on IMAGE_ONLY
                CaptureMode.IMAGE_ONLY -> (captureButtonSize * idleImageCaptureScale).dp
                // small red circle will be visible on VIDEO_ONLY
                CaptureMode.VIDEO_ONLY -> (captureButtonSize * idleVideoCaptureScale).dp
            }
        },
        animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing)
    )

    // used to fade between red/white in the center of the capture button
    val animatedColor by animateColorAsState(
        targetValue = when (val uiState = currentUiState.value) {
            is CaptureButtonUiState.Enabled.Idle -> when (uiState.captureMode) {
                CaptureMode.STANDARD -> imageCaptureModeColor
                CaptureMode.IMAGE_ONLY -> imageCaptureModeColor
                CaptureMode.VIDEO_ONLY -> recordingColor
            }

            is CaptureButtonUiState.Enabled.Recording -> recordingColor
            is CaptureButtonUiState.Unavailable -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 500)
    )

    Box(modifier = modifier.offset(x = offsetX), contentAlignment = Alignment.Center) {
        // inner circle
        Box(modifier = Modifier) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(centerShapeSize)
                    .clip(CircleShape)
                    .alpha(
                        if (isPressed &&
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
        }
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
