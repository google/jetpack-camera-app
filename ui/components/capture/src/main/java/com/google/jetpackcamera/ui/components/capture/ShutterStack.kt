/*
 * Copyright (C) 2026 The Android Open Source Project
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

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.core.camera.AudioStreamState
import com.google.jetpackcamera.core.camera.VideoRecordingState
import com.google.jetpackcamera.model.CaptureMode
import com.google.jetpackcamera.model.LensFacing
import com.google.jetpackcamera.ui.uistate.SingleSelectableUiState
import com.google.jetpackcamera.ui.uistate.capture.CaptureButtonUiState
import com.google.jetpackcamera.ui.uistate.capture.FlipLensUiState

private enum class ShutterSlotId {
    FLIP_CAMERA,
    CAPTURE,
    IMAGE_WELL,
    PAUSE_RESUME
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object ShutterStackTokens {
    val CaptureButtonSize = 86.dp
    val FlankingButtonSize = ButtonDefaults.MediumContainerHeight
    val FlankingIconSize = ButtonDefaults.MediumIconSize
    val VerticalSpacing = 48.dp
    val CompactVerticalSpacing = 20.dp
    val HorizontalSpacing = 32.dp
    val PreviewPadding = 16.dp
    val PreviewWellCornerRadius = 12.dp
    val PreviewBrightOverlapSize = 1000f
}

private val VerticalSlotOrder = listOf(
    ShutterSlotId.FLIP_CAMERA,
    ShutterSlotId.CAPTURE,
    ShutterSlotId.IMAGE_WELL,
    ShutterSlotId.PAUSE_RESUME
)

private val HorizontalSlotOrder = listOf(
    ShutterSlotId.IMAGE_WELL,
    ShutterSlotId.CAPTURE,
    ShutterSlotId.FLIP_CAMERA,
    ShutterSlotId.PAUSE_RESUME
)

/**
 * Adaptive primary shutter control cluster that arranges [captureButton] and optional flanking
 * controls along either a vertical rail ([Orientation.Vertical]) or a horizontal bar
 * ([Orientation.Horizontal]).
 *
 * All non-null slots are composed in a single fixed composition order and reordered strictly at
 * the measurement and placement phase so that posture transitions between [Orientation.Vertical]
 * and [Orientation.Horizontal] never tear down [captureButton] composition state, active press
 * interactions, or in-flight video recording gestures.
 *
 * Each slot is wrapped in its own [OverlapAwareStyleProvider] so individual controls independently
 * adapt their translucent background contrast against [LocalOverlapTargetBounds]. Optional slots
 * that are `null` or emit empty (`0x0`) content collapse completely without leaving extra gaps.
 *
 * Layout order by [orientation]:
 * - [Orientation.Vertical] (`Top -> Bottom`, non-inverted in RTL):
 *   `FlipCamera -> CaptureButton -> ImageWell -> PauseResume`.
 *   TODO(UX): Confirm final `Orientation.Vertical` `Top -> Bottom` slot ordering
 *   (`FlipCamera -> CaptureButton -> ImageWell -> PauseResume`, non-inverted in RTL) with UX.
 * - [Orientation.Horizontal] (`Start -> End`, mirrored in RTL):
 *   `ImageWell -> CaptureButton -> FlipCamera -> PauseResume`.
 *
 * @param captureButton Required slot for the primary shutter button (`86.dp` nominal diameter).
 * @param modifier [Modifier] applied to the outer stack layout.
 * @param orientation Axis along which active controls are arranged.
 * @param compact When `true` in [Orientation.Vertical], reduces default vertical spacing from
 *   `48.dp` to `20.dp` for height-constrained viewports.
 * @param spacing Main-axis gap placed between adjacent non-empty controls.
 * @param flipCameraButton Optional slot for flipping between camera lenses (`56.dp` nominal size).
 * @param imageWell Optional slot for the recent media thumbnail well (`56.dp` nominal size).
 * @param pauseResumeButton Optional slot for pausing and resuming active video recording
 *   (`56.dp` nominal size).
 */
@Composable
fun ShutterStack(
    captureButton: @Composable (Modifier) -> Unit,
    modifier: Modifier = Modifier,
    orientation: Orientation = Orientation.Vertical,
    compact: Boolean = false,
    spacing: Dp = when {
        orientation == Orientation.Horizontal -> ShutterStackTokens.HorizontalSpacing
        compact -> ShutterStackTokens.CompactVerticalSpacing
        else -> ShutterStackTokens.VerticalSpacing
    },
    flipCameraButton: (@Composable (Modifier) -> Unit)? = null,
    imageWell: (@Composable (Modifier) -> Unit)? = null,
    pauseResumeButton: (@Composable (Modifier) -> Unit)? = null
) {
    val measurePolicy = remember(orientation, spacing) {
        MeasurePolicy { measurables, constraints ->
            val spacingPx = spacing.roundToPx()
            val childConstraints = constraints.copy(minWidth = 0, minHeight = 0)
            val measuredBySlot = measurables.associate { measurable ->
                (measurable.layoutId as? ShutterSlotId) to measurable.measure(childConstraints)
            }
            val slotOrder = if (orientation == Orientation.Vertical) {
                VerticalSlotOrder
            } else {
                HorizontalSlotOrder
            }
            val activePlaceables = slotOrder.mapNotNull { slotId ->
                measuredBySlot[slotId]?.takeIf { placeable ->
                    placeable.width > 0 && placeable.height > 0
                }
            }

            if (orientation == Orientation.Vertical) {
                val contentWidth = activePlaceables.maxOfOrNull { it.width } ?: 0
                val contentHeight = if (activePlaceables.isEmpty()) {
                    0
                } else {
                    activePlaceables.sumOf { it.height } + spacingPx * (activePlaceables.size - 1)
                }
                val width = constraints.constrainWidth(contentWidth)
                val height = constraints.constrainHeight(contentHeight)

                layout(width, height) {
                    var currentY = 0
                    for (p in activePlaceables) {
                        p.place(
                            x = (width - p.width) / 2,
                            y = currentY
                        )
                        currentY += p.height + spacingPx
                    }
                }
            } else {
                val contentWidth = if (activePlaceables.isEmpty()) {
                    0
                } else {
                    activePlaceables.sumOf { it.width } + spacingPx * (activePlaceables.size - 1)
                }
                val contentHeight = activePlaceables.maxOfOrNull { it.height } ?: 0
                val width = constraints.constrainWidth(contentWidth)
                val height = constraints.constrainHeight(contentHeight)

                layout(width, height) {
                    var currentX = 0
                    for (p in activePlaceables) {
                        p.placeRelative(
                            x = currentX,
                            y = (height - p.height) / 2
                        )
                        currentX += p.width + spacingPx
                    }
                }
            }
        }
    }

    Layout(
        content = {
            flipCameraButton?.let {
                OverlapAwareStyleProvider(
                    modifier = Modifier.layoutId(ShutterSlotId.FLIP_CAMERA)
                ) {
                    it(Modifier)
                }
            }
            OverlapAwareStyleProvider(modifier = Modifier.layoutId(ShutterSlotId.CAPTURE)) {
                captureButton(Modifier)
            }
            imageWell?.let {
                OverlapAwareStyleProvider(modifier = Modifier.layoutId(ShutterSlotId.IMAGE_WELL)) {
                    it(Modifier)
                }
            }
            pauseResumeButton?.let {
                OverlapAwareStyleProvider(
                    modifier = Modifier.layoutId(ShutterSlotId.PAUSE_RESUME)
                ) {
                    it(Modifier)
                }
            }
        },
        modifier = modifier,
        measurePolicy = measurePolicy
    )
}

@Composable
private fun PreviewMediaWellSlot(modifier: Modifier = Modifier) {
    val backgroundStyle = LocalCameraControlBackgroundStyle.current
    Box(
        modifier = modifier
            .size(ShutterStackTokens.FlankingButtonSize)
            .clip(RoundedCornerShape(ShutterStackTokens.PreviewWellCornerRadius))
            .background(backgroundStyle.containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_camera_alt_filled),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(ShutterStackTokens.FlankingIconSize)
        )
    }
}

@Composable
private fun PreviewShutterStack(
    orientation: Orientation = Orientation.Vertical,
    compact: Boolean = false,
    isRecording: Boolean = false,
    showFlankingControls: Boolean = true,
    isDarkTheme: Boolean = true
) {
    val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()
    val backgroundColors = if (isDarkTheme) {
        listOf(Color.DarkGray, Color.Black)
    } else {
        listOf(Color.LightGray, Color.Gray)
    }
    val overlapBounds = remember(isDarkTheme) {
        mutableStateOf(
            if (isDarkTheme) {
                Rect.Zero
            } else {
                Rect(
                    0f,
                    0f,
                    ShutterStackTokens.PreviewBrightOverlapSize,
                    ShutterStackTokens.PreviewBrightOverlapSize
                )
            }
        )
    }
    val captureUiState = if (isRecording) {
        CaptureButtonUiState.Enabled.Recording.LockedRecording
    } else {
        CaptureButtonUiState.Enabled.Idle(CaptureMode.STANDARD)
    }
    val recordingState = if (isRecording) {
        VideoRecordingState.Active.Recording(
            maxDurationMillis = 0L,
            audioStreamState = AudioStreamState.Disabled,
            elapsedTimeNanos = 0L
        )
    } else {
        VideoRecordingState.Inactive()
    }
    val flipUiState = if (showFlankingControls) {
        FlipLensUiState.Available(
            selectedLensFacing = LensFacing.BACK,
            availableLensFacings = listOf(
                SingleSelectableUiState.SelectableUi(LensFacing.BACK),
                SingleSelectableUiState.SelectableUi(LensFacing.FRONT)
            )
        )
    } else {
        FlipLensUiState.Unavailable
    }

    MaterialTheme(colorScheme = colorScheme) {
        CompositionLocalProvider(
            LocalContentColor provides Color.White,
            LocalOverlapTargetBounds provides overlapBounds
        ) {
            Box(
                modifier = Modifier
                    .background(Brush.verticalGradient(colors = backgroundColors))
                    .padding(ShutterStackTokens.PreviewPadding),
                contentAlignment = Alignment.Center
            ) {
                ShutterStack(
                    orientation = orientation,
                    compact = compact,
                    captureButton = { slotModifier ->
                        CaptureButton(
                            modifier = slotModifier,
                            onImageCapture = {},
                            onStartRecording = {},
                            onStopRecording = {},
                            onLockVideoRecording = {},
                            onIncrementZoom = {},
                            captureButtonUiState = captureUiState,
                            captureButtonSize = ShutterStackTokens.CaptureButtonSize.value
                        )
                    },
                    flipCameraButton = { slotModifier ->
                        FlipCameraButton(
                            flipLensUiState = flipUiState,
                            onClick = {},
                            modifier = slotModifier
                        )
                    },
                    imageWell = { slotModifier ->
                        if (showFlankingControls) {
                            PreviewMediaWellSlot(modifier = slotModifier)
                        }
                    },
                    pauseResumeButton = { slotModifier ->
                        PauseResumeToggleButton(
                            modifier = slotModifier,
                            size = ShutterStackTokens.FlankingButtonSize,
                            onSetPause = {},
                            currentRecordingStateProvider = { recordingState }
                        )
                    }
                )
            }
        }
    }
}

@Preview(name = "Vertical Idle (86x294dp)")
@Composable
private fun ShutterStackVerticalIdlePreview() {
    PreviewShutterStack(orientation = Orientation.Vertical)
}

@Preview(name = "Vertical Recording (86x398dp)")
@Composable
private fun ShutterStackVerticalRecordingPreview() {
    PreviewShutterStack(
        orientation = Orientation.Vertical,
        isRecording = true
    )
}

@Preview(name = "Vertical Compact (86x238dp)")
@Composable
private fun ShutterStackVerticalCompactPreview() {
    PreviewShutterStack(
        orientation = Orientation.Vertical,
        compact = true
    )
}

@Preview(name = "Vertical Single-Control Collapse (86x86dp)")
@Composable
private fun ShutterStackVerticalSingleControlCollapsePreview() {
    PreviewShutterStack(
        orientation = Orientation.Vertical,
        showFlankingControls = false
    )
}

@Preview(name = "Horizontal LTR")
@Composable
private fun ShutterStackHorizontalLtrPreview() {
    PreviewShutterStack(
        orientation = Orientation.Horizontal,
        isRecording = true
    )
}

@Preview(name = "Horizontal RTL", locale = "ar")
@Composable
private fun ShutterStackHorizontalRtlPreview() {
    PreviewShutterStack(
        orientation = Orientation.Horizontal,
        isRecording = true
    )
}

@Preview(name = "Light Theme", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ShutterStackLightThemePreview() {
    PreviewShutterStack(
        orientation = Orientation.Vertical,
        isDarkTheme = false
    )
}

@Preview(name = "Dark Theme", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ShutterStackDarkThemePreview() {
    PreviewShutterStack(
        orientation = Orientation.Vertical,
        isDarkTheme = true
    )
}
