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

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object ToggleSwitchTokens {
    val TrackLength: Dp = 116.dp
    val TrackThickness: Dp = 64.dp
    val CompactTrackLength: Dp = 92.dp
    val CompactTrackThickness: Dp = 48.dp
    val ThumbDiameter: Dp = ButtonDefaults.MinHeight
    val IconSize: Dp = IconButtonDefaults.smallIconSize
    val DisabledIconAlpha: Float = 0.38f
    val DisabledThumbAlpha: Float = 0.12f
    val DefaultTrackAlpha: Float = 0.16f
    val ToggleAnimationDurationMillis: Int = 300
}

/**
 * Main-thread-confined holder tracking active drag gestures without triggering recomposition.
 */
private class SwitchDragState {
    var isDragging: Boolean = false
    var progress: Float = 0f
    var revertCheckJob: Job? = null
}

/**
 * A custom Switch component built with Canvas, supporting horizontal and vertical orientations,
 * standard and compact track sizes, RTL mirroring, drag gestures, and custom icons for unchecked
 * and checked states.
 *
 * When disabled, the thumb and icons are dimmed while tapping or invoking accessibility click
 * triggers [onToggleWhenDisabled] so callers can explain why toggling is unavailable.
 *
 * TODO(UX): Confirm final [Orientation.Vertical] ordering (currently [startIcon]/Photo at top to
 *   [endIcon]/Video at bottom with downward drag, non-inverted in RTL).
 *
 * @param checked Whether the switch is currently checked (`true` selects [endIcon]).
 * @param onCheckedChange Callback invoked when the user toggles the switch while [enabled].
 * @param modifier [Modifier] applied to the switch canvas.
 * @param onToggleWhenDisabled Callback invoked when the user taps or activates the switch while
 *   disabled.
 * @param enabled Whether the switch is interactive.
 * @param orientation Primary axis of the track and drag gesture ([Orientation.Horizontal] or
 *   [Orientation.Vertical]).
 * @param compact Whether to use compact track dimensions (`92x48.dp` horizontal, `48x92.dp`
 *   vertical) instead of standard dimensions (`116x64.dp` horizontal, `64x116.dp` vertical).
 * @param iconRotationDegrees Clockwise rotation in degrees applied around each icon's center.
 * @param trackColor Fill color of the pill track.
 * @param thumbColor Fill color of the circular thumb when [enabled].
 * @param disableColor Base color used to derive dimmed thumb and icon colors when disabled.
 * @param startIcon Optional drawable resource ID for the unchecked (`false`) position.
 * @param endIcon Optional drawable resource ID for the checked (`true`) position.
 * @param startIconDescription Optional accessibility state description when unchecked (`false`).
 * @param endIconDescription Optional accessibility state description when checked (`true`).
 * @param unselectedIconColor Tint color for icon regions outside the thumb bounds when [enabled].
 * @param selectedIconColor Tint color for icon regions overlapping the thumb when [enabled].
 * @param switchWidth Explicit width override; defaults from [orientation] and [compact].
 * @param switchHeight Explicit height override; defaults from [orientation] and [compact].
 * @param thumbDiameter Diameter of the circular thumb.
 * @param iconSize Rendered width and height of [startIcon] and [endIcon].
 */
@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onToggleWhenDisabled: () -> Unit = {},
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Horizontal,
    compact: Boolean = false,
    iconRotationDegrees: Float = 0f,
    trackColor: Color = Color.White.copy(alpha = ToggleSwitchTokens.DefaultTrackAlpha),
    thumbColor: Color = MaterialTheme.colorScheme.primaryFixedDim,
    disableColor: Color = MaterialTheme.colorScheme.onSurface,
    @DrawableRes startIcon: Int? = null,
    @DrawableRes endIcon: Int? = null,
    startIconDescription: String? = null,
    endIconDescription: String? = null,
    unselectedIconColor: Color = MaterialTheme.colorScheme.secondaryFixed,
    selectedIconColor: Color = MaterialTheme.colorScheme.onPrimaryFixed,
    switchWidth: Dp = when (orientation) {
        Orientation.Horizontal ->
            if (compact) ToggleSwitchTokens.CompactTrackLength else ToggleSwitchTokens.TrackLength
        Orientation.Vertical ->
            if (compact) {
                ToggleSwitchTokens.CompactTrackThickness
            } else {
                ToggleSwitchTokens.TrackThickness
            }
    },
    switchHeight: Dp = when (orientation) {
        Orientation.Horizontal ->
            if (compact) {
                ToggleSwitchTokens.CompactTrackThickness
            } else {
                ToggleSwitchTokens.TrackThickness
            }
        Orientation.Vertical ->
            if (compact) ToggleSwitchTokens.CompactTrackLength else ToggleSwitchTokens.TrackLength
    },
    thumbDiameter: Dp = ToggleSwitchTokens.ThumbDiameter,
    iconSize: Dp = ToggleSwitchTokens.IconSize
) {
    // --- 1. Dimensions & Direction ---
    val dims = rememberSwitchDimensions(
        orientation = orientation,
        switchWidth = switchWidth,
        switchHeight = switchHeight,
        thumbDiameter = thumbDiameter,
        iconSize = iconSize
    )
    val isHorizontalRtl =
        orientation == Orientation.Horizontal &&
            LocalLayoutDirection.current == LayoutDirection.Rtl
    val disableAnimations = LocalDisableAnimations.current

    // --- 2. Up-to-date Callback & State References ---
    val currentEnabled by rememberUpdatedState(enabled)
    val currentChecked by rememberUpdatedState(checked)
    val currentOnCheckedChange by rememberUpdatedState(onCheckedChange)
    val currentOnToggleWhenDisabled by rememberUpdatedState(onToggleWhenDisabled)
    val currentDims by rememberUpdatedState(dims)
    val currentDisableAnimations by rememberUpdatedState(disableAnimations)

    // --- 3. Zero-Recomposition Thumb Progress & Color Animations ---
    val thumbProgress = remember { Animatable(if (checked) 1f else 0f) }
    val coroutineScope = rememberCoroutineScope()
    val dragState = remember { SwitchDragState() }
    val dragInteractionSource = remember { MutableInteractionSource() }

    LaunchedEffect(enabled, orientation, isHorizontalRtl) {
        val target = if (currentChecked) 1f else 0f
        if (!enabled || dragState.isDragging) {
            dragState.isDragging = false
            dragState.revertCheckJob?.cancel()
            dragState.revertCheckJob = null
            thumbProgress.syncToTarget(
                target = target,
                disableAnimations = currentDisableAnimations
            )
        } else if (
            dragState.revertCheckJob == null &&
            !thumbProgress.isRunning &&
            thumbProgress.value != target
        ) {
            thumbProgress.syncToTarget(
                target = target,
                disableAnimations = currentDisableAnimations
            )
        }
    }

    LaunchedEffect(dragInteractionSource) {
        val activeDrags = mutableListOf<DragInteraction.Start>()
        dragInteractionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> activeDrags.add(interaction)
                is DragInteraction.Stop -> activeDrags.remove(interaction.start)
                is DragInteraction.Cancel -> {
                    activeDrags.remove(interaction.start)
                    if (activeDrags.isEmpty() && dragState.isDragging) {
                        dragState.isDragging = false
                        dragState.revertCheckJob?.cancel()
                        dragState.revertCheckJob = null
                        coroutineScope.launch {
                            thumbProgress.syncToTarget(
                                target = if (currentChecked) 1f else 0f,
                                disableAnimations = currentDisableAnimations
                            )
                        }
                    }
                }
            }
        }
    }

    LaunchedEffect(checked, disableAnimations) {
        if (!dragState.isDragging) {
            dragState.revertCheckJob?.cancel()
            dragState.revertCheckJob = null
            thumbProgress.syncToTarget(
                target = if (checked) 1f else 0f,
                disableAnimations = disableAnimations
            )
        }
    }

    val trackColorState: State<Color> = animateColorAsState(
        targetValue = trackColor,
        animationSpec = if (disableAnimations) {
            snap()
        } else {
            tween(durationMillis = ToggleSwitchTokens.ToggleAnimationDurationMillis)
        },
        label = "trackColor"
    )
    val thumbColorState: State<Color> = animateColorAsState(
        targetValue = if (!enabled) {
            disableColor.copy(alpha = ToggleSwitchTokens.DisabledThumbAlpha)
        } else {
            thumbColor
        },
        animationSpec = if (disableAnimations) {
            snap()
        } else {
            tween(durationMillis = ToggleSwitchTokens.ToggleAnimationDurationMillis)
        },
        label = "thumbColor"
    )

    // --- 4. Icon Painters & Hoisted ColorFilters ---
    val startIconPainter = startIcon?.let { painterResource(id = it) }
    val endIconPainter = endIcon?.let { painterResource(id = it) }

    val resolvedOffIconColor = if (enabled) {
        unselectedIconColor
    } else {
        disableColor.copy(alpha = ToggleSwitchTokens.DisabledIconAlpha)
    }
    val resolvedOnIconColor = if (enabled) {
        selectedIconColor
    } else {
        disableColor.copy(alpha = ToggleSwitchTokens.DisabledIconAlpha)
    }
    val offColorFilter = remember(resolvedOffIconColor) {
        ColorFilter.tint(resolvedOffIconColor)
    }
    val onColorFilter = remember(resolvedOnIconColor) {
        ColorFilter.tint(resolvedOnIconColor)
    }

    // --- 5. Gesture & Semantics Modifiers ---
    val draggableState = rememberDraggableState { delta ->
        val dimsSnapshot = currentDims
        if (currentEnabled && dimsSnapshot.dragRange > 0f) {
            if (!dragState.isDragging) {
                dragState.revertCheckJob?.cancel()
                dragState.revertCheckJob = null
                dragState.isDragging = true
                dragState.progress = thumbProgress.value
            }
            val nextProgress =
                (dragState.progress + delta / dimsSnapshot.dragRange).coerceIn(0f, 1f)
            dragState.progress = nextProgress
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                thumbProgress.snapTo(nextProgress)
            }
        }
    }

    val activeDescription = if (checked) endIconDescription else startIconDescription
    val gestureModifier = Modifier
        .focusable(enabled = enabled)
        .pointerInput(enabled) {
            detectTapGestures(
                onTap = {
                    if (currentEnabled) {
                        currentOnCheckedChange(!currentChecked)
                    } else {
                        currentOnToggleWhenDisabled()
                    }
                }
            )
        }
        .draggable(
            state = draggableState,
            enabled = enabled,
            orientation = orientation,
            reverseDirection = isHorizontalRtl,
            interactionSource = dragInteractionSource,
            onDragStarted = {
                if (currentEnabled) {
                    dragState.revertCheckJob?.cancel()
                    dragState.revertCheckJob = null
                    dragState.isDragging = true
                    dragState.progress = thumbProgress.value
                }
            },
            onDragStopped = {
                val finalProgress =
                    if (dragState.isDragging) dragState.progress else thumbProgress.value
                dragState.isDragging = false
                dragState.revertCheckJob?.cancel()
                dragState.revertCheckJob = null
                val requestedChecked =
                    if (currentEnabled) finalProgress > 0.5f else currentChecked
                val didRequestChange = currentEnabled && requestedChecked != currentChecked
                if (didRequestChange) {
                    currentOnCheckedChange(requestedChecked)
                }
                val settleTarget = if (requestedChecked) 1f else 0f
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    thumbProgress.syncToTarget(
                        target = settleTarget,
                        disableAnimations = currentDisableAnimations
                    )
                }
                if (didRequestChange) {
                    dragState.revertCheckJob = coroutineScope.launch {
                        withFrameNanos {}
                        withFrameNanos {}
                        yield()
                        dragState.revertCheckJob = null
                        if (!dragState.isDragging && currentChecked != requestedChecked) {
                            thumbProgress.syncToTarget(
                                target = if (currentChecked) 1f else 0f,
                                disableAnimations = currentDisableAnimations
                            )
                        }
                    }
                }
            }
        )
        .semantics {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
            activeDescription?.let { stateDescription = it }
            onClick {
                if (currentEnabled) {
                    currentOnCheckedChange(!currentChecked)
                } else {
                    currentOnToggleWhenDisabled()
                }
                true
            }
        }

    // --- 6. Canvas Drawing ---
    Canvas(
        modifier = modifier
            .size(dims.switchWidthDp, dims.switchHeightDp)
            .then(gestureModifier)
    ) {
        val progress = thumbProgress.value
        val trackCornerRadius = CornerRadius(dims.crossAxisSizePx / 2f, dims.crossAxisSizePx / 2f)

        // 1. Draw the Track
        drawSwitchTrack(
            color = trackColorState.value,
            cornerRadius = trackCornerRadius
        )

        // 2. Compute Thumb & Icon Centers along the Active Axis
        val crossCenterPx = dims.crossAxisSizePx / 2f
        val thumbCenterX: Float
        val thumbCenterY: Float
        val startIconCenterX: Float
        val startIconCenterY: Float
        val endIconCenterX: Float
        val endIconCenterY: Float

        if (orientation == Orientation.Horizontal) {
            if (isHorizontalRtl) {
                startIconCenterX = dims.endCenterMainPx
                endIconCenterX = dims.startCenterMainPx
                thumbCenterX = dims.endCenterMainPx - dims.dragRange * progress
            } else {
                startIconCenterX = dims.startCenterMainPx
                endIconCenterX = dims.endCenterMainPx
                thumbCenterX = dims.startCenterMainPx + dims.dragRange * progress
            }
            thumbCenterY = crossCenterPx
            startIconCenterY = crossCenterPx
            endIconCenterY = crossCenterPx
        } else {
            // Vertical keeps Top -> Bottom non-inverted in both LTR and RTL.
            thumbCenterX = crossCenterPx
            startIconCenterX = crossCenterPx
            endIconCenterX = crossCenterPx
            startIconCenterY = dims.startCenterMainPx
            endIconCenterY = dims.endCenterMainPx
            thumbCenterY = dims.startCenterMainPx + dims.dragRange * progress
        }

        // 3. Draw the Thumb (under Icons)
        drawSwitchThumb(
            center = Offset(thumbCenterX, thumbCenterY),
            radius = dims.thumbRadiusPx,
            color = thumbColorState.value
        )

        // 4. Draw the Clipped Icons
        if (startIconPainter != null || endIconPainter != null) {
            drawClippedSwitchIcons(
                thumbLeft = thumbCenterX - dims.thumbRadiusPx,
                thumbTop = thumbCenterY - dims.thumbRadiusPx,
                thumbRight = thumbCenterX + dims.thumbRadiusPx,
                thumbBottom = thumbCenterY + dims.thumbRadiusPx,
                iconSizePx = dims.iconSizePx,
                iconRotationDegrees = iconRotationDegrees,
                startIconPainter = startIconPainter,
                startIconCenterX = startIconCenterX,
                startIconCenterY = startIconCenterY,
                endIconPainter = endIconPainter,
                endIconCenterX = endIconCenterX,
                endIconCenterY = endIconCenterY,
                offColorFilter = offColorFilter,
                onColorFilter = onColorFilter
            )
        }
    }
}

// --- Helper class and functions ---

private suspend fun Animatable<Float, AnimationVector1D>.syncToTarget(
    target: Float,
    disableAnimations: Boolean
) {
    if (disableAnimations) {
        snapTo(target)
    } else if (isRunning) {
        if (targetValue != target) {
            animateTo(
                targetValue = target,
                animationSpec = tween(ToggleSwitchTokens.ToggleAnimationDurationMillis)
            )
        }
    } else if (value != target) {
        animateTo(
            targetValue = target,
            animationSpec = tween(ToggleSwitchTokens.ToggleAnimationDurationMillis)
        )
    }
}

/**
 * A holder for orientation-aware switch dimensions in both Dp and Px.
 */
private class SwitchDimensions(
    val switchWidthDp: Dp,
    val switchHeightDp: Dp,
    val mainAxisSizePx: Float,
    val crossAxisSizePx: Float,
    val thumbRadiusPx: Float,
    val iconSizePx: Float,
    val startCenterMainPx: Float,
    val endCenterMainPx: Float,
    val dragRange: Float
)

/**
 * Calculates and remembers all necessary pixel dimensions for the switch.
 */
@Composable
private fun rememberSwitchDimensions(
    orientation: Orientation,
    switchWidth: Dp,
    switchHeight: Dp,
    thumbDiameter: Dp,
    iconSize: Dp
): SwitchDimensions {
    val density = LocalDensity.current
    return remember(density, orientation, switchWidth, switchHeight, thumbDiameter, iconSize) {
        val switchWidthPx = with(density) { switchWidth.toPx() }
        val switchHeightPx = with(density) { switchHeight.toPx() }
        val mainAxisSizePx =
            if (orientation == Orientation.Horizontal) switchWidthPx else switchHeightPx
        val crossAxisSizePx =
            if (orientation == Orientation.Horizontal) switchHeightPx else switchWidthPx
        val thumbDiameterPx = with(density) { thumbDiameter.toPx() }
        val thumbRadiusPx = thumbDiameterPx / 2f
        val thumbInsetPx = (crossAxisSizePx - thumbDiameterPx) / 2f
        val iconSizePx = with(density) { iconSize.toPx() }

        val startCenterMainPx = thumbInsetPx + thumbRadiusPx
        val endCenterMainPx = mainAxisSizePx - thumbInsetPx - thumbRadiusPx
        val dragRange = (endCenterMainPx - startCenterMainPx).coerceAtLeast(0f)

        SwitchDimensions(
            switchWidthDp = switchWidth,
            switchHeightDp = switchHeight,
            mainAxisSizePx = mainAxisSizePx,
            crossAxisSizePx = crossAxisSizePx,
            thumbRadiusPx = thumbRadiusPx,
            iconSizePx = iconSizePx,
            startCenterMainPx = startCenterMainPx,
            endCenterMainPx = endCenterMainPx,
            dragRange = dragRange
        )
    }
}

/**
 * Draws the switch track (the background).
 */
private fun DrawScope.drawSwitchTrack(color: Color, cornerRadius: CornerRadius) {
    drawRoundRect(
        color = color,
        topLeft = Offset.Zero,
        size = size,
        cornerRadius = cornerRadius
    )
}

/**
 * Draws the start and end icons, clipping the selected tint region to the 4 edges of the thumb.
 */
private fun DrawScope.drawClippedSwitchIcons(
    thumbLeft: Float,
    thumbTop: Float,
    thumbRight: Float,
    thumbBottom: Float,
    iconSizePx: Float,
    iconRotationDegrees: Float,
    startIconPainter: Painter?,
    startIconCenterX: Float,
    startIconCenterY: Float,
    endIconPainter: Painter?,
    endIconCenterX: Float,
    endIconCenterY: Float,
    offColorFilter: ColorFilter,
    onColorFilter: ColorFilter
) {
    if (startIconPainter != null) {
        drawClippedSwitchIcon(
            painter = startIconPainter,
            iconCenterX = startIconCenterX,
            iconCenterY = startIconCenterY,
            iconSizePx = iconSizePx,
            iconRotationDegrees = iconRotationDegrees,
            thumbLeft = thumbLeft,
            thumbTop = thumbTop,
            thumbRight = thumbRight,
            thumbBottom = thumbBottom,
            offColorFilter = offColorFilter,
            onColorFilter = onColorFilter
        )
    }
    if (endIconPainter != null) {
        drawClippedSwitchIcon(
            painter = endIconPainter,
            iconCenterX = endIconCenterX,
            iconCenterY = endIconCenterY,
            iconSizePx = iconSizePx,
            iconRotationDegrees = iconRotationDegrees,
            thumbLeft = thumbLeft,
            thumbTop = thumbTop,
            thumbRight = thumbRight,
            thumbBottom = thumbBottom,
            offColorFilter = offColorFilter,
            onColorFilter = onColorFilter
        )
    }
}

private fun DrawScope.drawClippedSwitchIcon(
    painter: Painter,
    iconCenterX: Float,
    iconCenterY: Float,
    iconSizePx: Float,
    iconRotationDegrees: Float,
    thumbLeft: Float,
    thumbTop: Float,
    thumbRight: Float,
    thumbBottom: Float,
    offColorFilter: ColorFilter,
    onColorFilter: ColorFilter
) {
    val halfIconPx = iconSizePx / 2f
    val iconLeft = iconCenterX - halfIconPx
    val iconTop = iconCenterY - halfIconPx
    val iconRight = iconLeft + iconSizePx
    val iconBottom = iconTop + iconSizePx
    val iconDrawSize = Size(iconSizePx, iconSizePx)
    val iconPivot = Offset(halfIconPx, halfIconPx)

    // Draw the base (unselected) icon first.
    translate(left = iconLeft, top = iconTop) {
        rotate(degrees = iconRotationDegrees, pivot = iconPivot) {
            with(painter) {
                draw(size = iconDrawSize, colorFilter = offColorFilter)
            }
        }
    }

    // Clip all 4 edges to the intersection of the thumb bounds and icon bounds.
    val clipLeft = maxOf(thumbLeft, iconLeft)
    val clipTop = maxOf(thumbTop, iconTop)
    val clipRight = minOf(thumbRight, iconRight)
    val clipBottom = minOf(thumbBottom, iconBottom)
    if (clipLeft < clipRight && clipTop < clipBottom) {
        clipRect(
            left = clipLeft,
            top = clipTop,
            right = clipRight,
            bottom = clipBottom
        ) {
            translate(left = iconLeft, top = iconTop) {
                rotate(degrees = iconRotationDegrees, pivot = iconPivot) {
                    with(painter) {
                        draw(size = iconDrawSize, colorFilter = onColorFilter)
                    }
                }
            }
        }
    }
}

/**
 * Draws the thumb (the circular handle).
 */
private fun DrawScope.drawSwitchThumb(center: Offset, radius: Float, color: Color) {
    drawCircle(
        color = color,
        radius = radius,
        center = center
    )
}

@Preview(name = "Horizontal LTR - Off (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchOffEnabledPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_filled,
            endIcon = R.drawable.ic_videocam_outline,
            checked = false,
            onCheckedChange = {},
            enabled = true
        )
    }
}

@Preview(name = "Horizontal LTR - On (Dark)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchOnEnabledPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_outline,
            endIcon = R.drawable.ic_videocam_filled,
            checked = true,
            onCheckedChange = {},
            enabled = true
        )
    }
}

@Preview(name = "Horizontal LTR - On (Light)", uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun ToggleSwitchLightEnabledPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_outline,
            endIcon = R.drawable.ic_videocam_filled,
            checked = true,
            onCheckedChange = {},
            enabled = true,
            trackColor = Color.Black.copy(alpha = ToggleSwitchTokens.DefaultTrackAlpha)
        )
    }
}

@Preview(
    name = "Horizontal RTL - Off",
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ToggleSwitchRtlOffPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_filled,
            endIcon = R.drawable.ic_videocam_outline,
            checked = false,
            onCheckedChange = {},
            enabled = true
        )
    }
}

@Preview(name = "Horizontal Compact - Off", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchHorizontalCompactPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_filled,
            endIcon = R.drawable.ic_videocam_outline,
            checked = false,
            onCheckedChange = {},
            compact = true
        )
    }
}

@Preview(name = "Vertical Standard (64x116)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchVerticalStandardPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_filled,
            endIcon = R.drawable.ic_videocam_outline,
            checked = false,
            onCheckedChange = {},
            orientation = Orientation.Vertical
        )
    }
}

@Preview(name = "Vertical Compact (48x92)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchVerticalCompactPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_outline,
            endIcon = R.drawable.ic_videocam_filled,
            checked = true,
            onCheckedChange = {},
            orientation = Orientation.Vertical,
            compact = true
        )
    }
}

@Preview(name = "Rotated Icons (90 deg)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchIconRotation90Preview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_filled,
            endIcon = R.drawable.ic_videocam_outline,
            checked = false,
            onCheckedChange = {},
            iconRotationDegrees = 90f
        )
    }
}

@Preview(name = "Disabled On", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchOnDisabledPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_outline,
            endIcon = R.drawable.ic_videocam_filled,
            checked = true,
            onCheckedChange = {},
            enabled = false
        )
    }
}

@Preview(name = "Disabled Off", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchOffDisabledPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_filled,
            endIcon = R.drawable.ic_videocam_outline,
            checked = false,
            onCheckedChange = {},
            enabled = false
        )
    }
}
