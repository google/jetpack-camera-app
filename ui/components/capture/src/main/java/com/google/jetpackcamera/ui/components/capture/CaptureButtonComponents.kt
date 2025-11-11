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

import android.util.Log
import android.view.KeyEvent
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "CaptureButton"
private const val DEFAULT_CAPTURE_BUTTON_SIZE = 80f

// scales against the size of the capture button
private const val LOCK_SWITCH_PRESSED_NUCLEUS_SCALE = .5f

// scales against the size of the capture button
private const val LOCK_SWITCH_WIDTH_SCALE = 1.375f

// scales against the size of the pressed nucleus
private const val LOCK_SWITCH_HEIGHT_SCALE = 1.4f

// 1f = left, 0f = right
private const val LOCK_SWITCH_POSITION_ON = 1f
private const val LOCK_SWITCH_POSITION_OFF = 0f
private const val MINIMUM_LOCK_THRESHOLD = .65F

private const val LOCK_SWITCH_ALPHA = .37f

private enum class CaptureSource {
    CAPTURE_BUTTON,
    VOLUME_UP,
    VOLUME_DOWN
}

/**
 * Handler for using certain key events buttons as capture buttons.
 */
@Composable
private fun CaptureKeyHandler(
    onPress: (CaptureSource) -> Unit,
    onRelease: (CaptureSource) -> Unit
) {
    val view = LocalView.current
    val currentOnPress by rememberUpdatedState(onPress)
    val currentOnRelease by rememberUpdatedState(onRelease)

    fun keyCodeToCaptureSource(keyCode: Int): CaptureSource = when (keyCode) {
        KeyEvent.KEYCODE_VOLUME_UP -> CaptureSource.VOLUME_UP
        KeyEvent.KEYCODE_VOLUME_DOWN -> CaptureSource.VOLUME_DOWN
        else -> TODO("Keycode not assigned to CaptureSource")
    }

    DisposableEffect(view) {
        // todo call once per keydown
        var keyActionDown: Int? = null
        val keyEventDispatcher = ViewCompat.OnUnhandledKeyEventListenerCompat { _, event ->
            when (event.keyCode) {
                KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN -> {
                    val captureSource = keyCodeToCaptureSource(event.keyCode)
                    // pressed down
                    if (event.action == KeyEvent.ACTION_DOWN && keyActionDown == null) {
                        keyActionDown = event.keyCode
                        currentOnPress(captureSource)
                    }
                    // released
                    if (event.action == KeyEvent.ACTION_UP && keyActionDown == event.keyCode) {
                        keyActionDown = null
                        currentOnRelease(captureSource)
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
fun CaptureButton(
    modifier: Modifier = Modifier,
    onImageCapture: () -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onLockVideoRecording: (Boolean) -> Unit,
    onIncrementZoom: (Float) -> Unit,
    captureButtonUiState: CaptureButtonUiState,
    captureButtonSize: Float = DEFAULT_CAPTURE_BUTTON_SIZE
) {
    val currentUiState = rememberUpdatedState(captureButtonUiState)
    val firstKeyPressed = remember { mutableStateOf<CaptureSource?>(null) }
    val isLongPressing = remember { mutableStateOf(false) }
    var longPressJob by remember { mutableStateOf<Job?>(null) }
    val scope = rememberCoroutineScope()
    val longPressTimeout = LocalViewConfiguration.current.longPressTimeoutMillis

    LaunchedEffect(captureButtonUiState) {
        if (captureButtonUiState is CaptureButtonUiState.Enabled.Idle) {
            onLockVideoRecording(false)
        } else if (captureButtonUiState is CaptureButtonUiState.Enabled.Recording.LockedRecording) {
            longPressJob = null
            isLongPressing.value = false
            firstKeyPressed.value = null
        }
    }
    fun onLongPress() {
        if (!isLongPressing.value) {
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

    fun onPress(captureSource: CaptureSource) {
        if (firstKeyPressed.value == null) {
            firstKeyPressed.value = captureSource
            longPressJob = scope.launch {
                delay(longPressTimeout)
                onLongPress()
            }
        }
    }

    fun onKeyUp(captureSource: CaptureSource, isLocked: Boolean = false) {
        // releasing while pressed recording
        if (firstKeyPressed.value == captureSource) {
            if (isLongPressing.value) {
                if (!isLocked &&
                    currentUiState.value is
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

    CaptureKeyHandler(
        onPress = { captureSource -> onPress(captureSource) },
        onRelease = { captureSource -> onKeyUp(captureSource) }
    )
    CaptureButton(
        modifier = modifier,
        onPress = { onPress(CaptureSource.CAPTURE_BUTTON) },
        onRelease = { onKeyUp(CaptureSource.CAPTURE_BUTTON, it) },
        onLockVideoRecording = onLockVideoRecording,
        onDragZoom = onIncrementZoom,
        captureButtonUiState = captureButtonUiState,
        captureButtonSize = captureButtonSize
    )
}

/**
 * A composable that returns a debounced boolean state for whether the capture button should be
 * visually disabled.
 *
 * While the button's semantics and pointer input are disabled immediately, the visual change
 * to a disabled appearance is delayed. If the button becomes enabled again within this period,
 * the distracting flicker is avoided.
 *
 * @param captureButtonUiState The current UI state of the capture button.
 * @param delayMillis The duration to wait before visually disabling the button.
 * @return A [State] holding `true` if the button should be visually disabled, `false` otherwise.
 */
@Composable
private fun rememberDebouncedVisuallyDisabled(
    captureButtonUiState: CaptureButtonUiState,
    delayMillis: Long = 1000L
): State<Boolean> {
    val isVisuallyDisabled = remember {
        mutableStateOf(captureButtonUiState is CaptureButtonUiState.Unavailable)
    }
    LaunchedEffect(captureButtonUiState) {
        if (captureButtonUiState is CaptureButtonUiState.Unavailable) {
            delay(delayMillis)
            isVisuallyDisabled.value = true
        } else {
            isVisuallyDisabled.value = false
        }
    }
    return isVisuallyDisabled
}

@Composable
private fun CaptureButton(
    modifier: Modifier = Modifier,
    onPress: () -> Unit,
    onRelease: (isLocked: Boolean) -> Unit,
    onDragZoom: (Float) -> Unit,
    onLockVideoRecording: (Boolean) -> Unit,
    captureButtonUiState: CaptureButtonUiState,
    useLockSwitch: Boolean = true,
    captureButtonSize: Float = DEFAULT_CAPTURE_BUTTON_SIZE
) {
    // todo: explore MutableInteractionSource
    var isCaptureButtonPressed by remember {
        mutableStateOf(false)
    }

    var switchPosition by remember {
        mutableFloatStateOf(LOCK_SWITCH_POSITION_OFF)
    }

    val currentUiState = rememberUpdatedState(captureButtonUiState)
    val switchWidth = (captureButtonSize * LOCK_SWITCH_WIDTH_SCALE)

    var relativeCaptureButtonBounds by remember { mutableStateOf<Rect?>(null) }

    val isEnabled = captureButtonUiState !is CaptureButtonUiState.Unavailable

    val isVisuallyDisabled by rememberDebouncedVisuallyDisabled(
        captureButtonUiState = captureButtonUiState
    )

    val animatedColor by animateColorAsState(
        targetValue = if (isVisuallyDisabled) {
            LocalContentColor.current.copy(alpha = 0.38f)
        } else {
            LocalContentColor.current
        },
        animationSpec = tween(durationMillis = if (isVisuallyDisabled) 1000 else 300),
        label = "Capture Button Color"
    )

    fun shouldBeLocked(): Boolean = switchPosition > MINIMUM_LOCK_THRESHOLD

    fun setLockSwitchPosition(positionX: Float, offsetX: Float) {
        relativeCaptureButtonBounds?.let {
            if (useLockSwitch) {
                if (positionX > it.center.x) {
                    switchPosition = LOCK_SWITCH_POSITION_OFF
                } else {
                    val newSwitchPosition =
                        switchPosition - (offsetX / switchWidth)
                    switchPosition =
                        newSwitchPosition.coerceIn(
                            LOCK_SWITCH_POSITION_OFF,
                            LOCK_SWITCH_POSITION_ON
                        )
                }
            }
        }
    }

    fun toggleSwitchPosition() = if (shouldBeLocked()) {
        switchPosition = LOCK_SWITCH_POSITION_OFF
    } else {
        if (!isCaptureButtonPressed) {
            onLockVideoRecording(true)
        } else {
            switchPosition =
                LOCK_SWITCH_POSITION_ON
        }
    }
    val gestureModifier = if (isEnabled) {
        Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    // onLongPress cannot be null, otherwise it won't detect the release if the
                    // touch is dragged off the component
                    onLongPress = {},
                    onPress = {
                        isCaptureButtonPressed = true
                        onPress()
                        awaitRelease()
                        isCaptureButtonPressed = false
                        if (shouldBeLocked()) {
                            onLockVideoRecording(true)
                            onRelease(true)
                        }

                        switchPosition = LOCK_SWITCH_POSITION_OFF
                        onRelease(false)
                    }
                )
            }
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = {},
                    onDragEnd = {},
                    onDragCancel = {},
                    onDrag = { change, deltaOffset ->
                        if (currentUiState.value ==
                            CaptureButtonUiState.Enabled.Recording.PressedRecording
                        ) {
                            val newPoint = change.position

                            // update position of lock switch
                            setLockSwitchPosition(newPoint.x, deltaOffset.x)

                            // update zoom
                            val previousPoint = change.position - deltaOffset
                            val positiveDistance =
                                if (newPoint.y >= 0 && previousPoint.y >= 0) {
                                    // 0 if both points are within bounds
                                    0f
                                } else if (newPoint.y < 0 && previousPoint.y < 0) {
                                    deltaOffset.y
                                } else if (newPoint.y <= 0) {
                                    newPoint.y
                                } else {
                                    previousPoint.y
                                }

                            if (!positiveDistance.isNaN()) {
                                // todo(kc): should check the tuning of this.
                                val zoom = positiveDistance * -0.01f // Adjust sensitivity
                                onDragZoom(zoom)
                            }
                        }
                    }
                )
            }
    } else {
        Modifier
    }
    CaptureButtonRing(
        modifier = modifier
            .onSizeChanged {
                relativeCaptureButtonBounds =
                    Rect(0f, 0f, it.width.toFloat(), it.height.toFloat())
            }
            .semantics {
                if (!isEnabled) {
                    disabled()
                }
            }
            .then(gestureModifier),
        captureButtonSize = captureButtonSize,
        color = animatedColor
    ) {
        if (useLockSwitch) {
            LockSwitchCaptureButtonNucleus(
                captureButtonUiState = captureButtonUiState,
                captureButtonSize = captureButtonSize,
                switchWidth = switchWidth.dp,
                switchPosition = switchPosition,
                onToggleSwitchPosition = { toggleSwitchPosition() },
                shouldBeLocked = { shouldBeLocked() }
            )
        } else {
            CaptureButtonNucleus(
                captureButtonUiState = captureButtonUiState,
                isPressed = isCaptureButtonPressed,
                captureButtonSize = captureButtonSize
            )
        }
    }
}

@Composable
fun CaptureButtonRing(
    modifier: Modifier = Modifier,
    captureButtonSize: Float,
    color: Color,
    borderWidth: Float = 4f,
    contents: (@Composable () -> Unit)? = null
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        contents?.invoke()
        // todo(): use a canvas instead of a box.
        //  the sizing gets funny so the scales need to be completely readjusted
        Box(
            modifier = Modifier
                .size(
                    captureButtonSize.dp
                )
                .border(borderWidth.dp, color, CircleShape)
        )
    }
}

/**
 * A nucleus for the capture button can be dragged to lock the pressed video recording.
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
            // grey cylinder offset to the left and fades in when pressed recording
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
                        .alpha(LOCK_SWITCH_ALPHA)
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

            CaptureButtonUiState.Enabled.Recording.PressedRecording ->
                (captureButtonSize * pressedVideoCaptureScale).dp

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

    // this box contains and centers everything
    Box(modifier = modifier.offset(x = offsetX), contentAlignment = Alignment.Center) {
        // this box is the inner circle
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

@Preview
@Composable
private fun CaptureButtonUnavailablePreview() {
    CaptureButton(
        onImageCapture = {},
        onStartRecording = {},
        onStopRecording = {},
        onLockVideoRecording = {},
        onIncrementZoom = {},
        captureButtonUiState = CaptureButtonUiState.Unavailable
    )
}

@Preview
@Composable
private fun IdleStandardCaptureButtonPreview() {
    CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
        CaptureButtonNucleus(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD),
            isPressed = false,
            captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE
        )
    }
}

@Preview
@Composable
private fun IdleImageCaptureButtonPreview() {
    CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
        CaptureButtonNucleus(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY),
            isPressed = false,
            captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE
        )
    }
}

@Preview
@Composable
private fun PressedImageCaptureButtonPreview() {
    CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
        CaptureButtonNucleus(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.IMAGE_ONLY),
            isPressed = true,
            captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE
        )
    }
}

@Preview
@Composable
private fun IdleRecordingCaptureButtonPreview() {
    CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
        CaptureButtonNucleus(
            captureButtonUiState = CaptureButtonUiState.Enabled.Idle(CaptureMode.VIDEO_ONLY),
            isPressed = false,
            captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE
        )
    }
}

@Preview
@Composable
private fun SimpleNucleusPressedRecordingPreview() {
    CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
        CaptureButtonNucleus(
            captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording,
            isPressed = true,
            captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE
        )
    }
}

@Preview
@Composable
private fun LockedRecordingPreview() {
    CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
        CaptureButtonNucleus(
            captureButtonUiState = CaptureButtonUiState.Enabled.Recording.LockedRecording,
            isPressed = false,
            captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE
        )
    }
}

@Preview
@Composable
private fun LockSwitchUnlockedPressedRecordingPreview() {
    // box is here to account for the offset lock switch
    Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) {
        CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
            LockSwitchCaptureButtonNucleus(
                captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE,
                captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording,
                switchWidth = (DEFAULT_CAPTURE_BUTTON_SIZE * LOCK_SWITCH_WIDTH_SCALE).dp,
                switchPosition = 0f,
                onToggleSwitchPosition = {},
                shouldBeLocked = { false }
            )
        }
    }
}

@Preview
@Composable
private fun LockSwitchLockedAtThresholdPressedRecordingPreview() {
    // box is here to account for the offset lock switch
    Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) {
        CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
            LockSwitchCaptureButtonNucleus(
                captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE,
                captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording,
                switchWidth = (DEFAULT_CAPTURE_BUTTON_SIZE * LOCK_SWITCH_WIDTH_SCALE).dp,
                switchPosition = MINIMUM_LOCK_THRESHOLD,
                onToggleSwitchPosition = {},
                shouldBeLocked = { true }
            )
        }
    }
}

@Preview
@Composable
private fun LockSwitchLockedPressedRecordingPreview() {
    // box is here to account for the offset lock switch
    Box(modifier = Modifier.width(150.dp), contentAlignment = Alignment.CenterEnd) {
        CaptureButtonRing(captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE, color = Color.White) {
            LockSwitchCaptureButtonNucleus(
                captureButtonSize = DEFAULT_CAPTURE_BUTTON_SIZE,
                captureButtonUiState = CaptureButtonUiState.Enabled.Recording.PressedRecording,
                switchWidth = (DEFAULT_CAPTURE_BUTTON_SIZE * LOCK_SWITCH_WIDTH_SCALE).dp,
                switchPosition = 1f,
                onToggleSwitchPosition = {},
                shouldBeLocked = { true }
            )
        }
    }
}
