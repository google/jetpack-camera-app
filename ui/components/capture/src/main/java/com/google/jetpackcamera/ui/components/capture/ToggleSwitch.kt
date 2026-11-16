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
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalGraphicsContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.google.jetpackcamera.ui.components.capture.layout.CameraLayoutDefaults
import com.google.jetpackcamera.ui.components.capture.layout.LocalCameraLayoutSolution
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private object ToggleSwitchTokens {
    val TrackLength: Dp = 116.dp
    val TrackThickness: Dp = 64.dp
    val MiniTrackLength: Dp = 92.dp
    val MiniTrackThickness: Dp = 48.dp

    /** Smallest allowed cross-axis size so the switch always meets the minimum touch target. */
    val MinTouchTargetSize: Dp = 48.dp
    val ThumbDiameter: Dp = ButtonDefaults.MinHeight
    val IconSize: Dp = IconButtonDefaults.smallIconSize
    val DisabledIconAlpha: Float = 0.38f
    val DisabledThumbAlpha: Float = 0.12f
    val DefaultTrackAlpha: Float = 0.2f
    val ToggleAnimationDurationMillis: Int = 300

    /** Key shadow of the level 2 elevation drop shadow drawn outside the track. */
    val KeyShadow = Shadow(
        radius = 3.dp,
        color = Color.Black,
        offset = DpOffset(x = 0.dp, y = 1.dp),
        alpha = 0.3f
    )

    /** Ambient shadow of the level 2 elevation drop shadow drawn outside the track. */
    val AmbientShadow = Shadow(
        radius = 8.dp,
        color = Color.Black,
        spread = 3.dp,
        offset = DpOffset(x = 0.dp, y = 4.dp),
        alpha = 0.15f
    )
}

/**
 * Main-thread-confined holder tracking active drag gestures without triggering recomposition.
 */
private class SwitchDragState {
    var isDragging: Boolean = false
    var progress: Float = 0f
    var gestureId: Int = 0
}

/**
 * Main-thread-confined holder for the uniform scale applied by [fitSwitchWithin] when incoming
 * constraints are smaller than the requested switch size, read by drag handling without
 * triggering recomposition.
 */
private class SwitchFitScale {
    var value: Float = 1f
}

/**
 * Delegating [MutableInteractionSource] that records synchronously whether a drag ended via
 * [DragInteraction.Cancel] before `Modifier.draggable` invokes `onDragStopped`.
 */
private class DragCancelTrackingInteractionSource(
    private val delegate: MutableInteractionSource = MutableInteractionSource()
) : MutableInteractionSource by delegate {
    var lastDragCancelled: Boolean = false

    override suspend fun emit(interaction: Interaction) {
        record(interaction)
        delegate.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        record(interaction)
        return delegate.tryEmit(interaction)
    }

    private fun record(interaction: Interaction) {
        when (interaction) {
            is DragInteraction.Start, is DragInteraction.Stop -> lastDragCancelled = false
            is DragInteraction.Cancel -> lastDragCancelled = true
        }
    }
}

/**
 * A custom Switch component built with Canvas, supporting horizontal and vertical orientations,
 * standard and mini track sizes, RTL mirroring, drag gestures, keyboard activation, and custom
 * icons for unchecked and checked states.
 *
 * When disabled, the thumb and icons are dimmed. If [onToggleWhenDisabled] is non-null, the switch
 * remains focusable and tapping, pressing Space/Enter, or invoking accessibility click triggers
 * [onToggleWhenDisabled] so callers can explain why toggling is unavailable; when `null`, the
 * switch exposes disabled semantics and is non-focusable.
 *
 * Standard horizontal switches (`orientation == Orientation.Horizontal` with
 * [ControlSize.Standard]) scale uniformly when [LocalCameraLayoutSolution] compacts the bottom
 * toolbar; mini-track and vertical switches preserve their explicit dimensions so they are not
 * shrunk twice.
 *
 * If the incoming layout constraints are smaller than the requested switch size on either axis,
 * the switch scales down uniformly to fit, preserving its aspect ratio so the track, thumb, icons,
 * and thumb travel stay proportional instead of distorting or clipping. Scaling never reduces the
 * cross axis below the `48.dp` minimum touch target; below that, the switch keeps its minimum
 * size and overflows the incoming constraints instead.
 *
 * When [showShadow] is `true`, a level 2 elevation drop shadow is drawn outside the track only, so
 * it never shows through a translucent [trackColor].
 *
 * In [Orientation.Vertical], [startIcon] (unchecked) sits at the top and [endIcon] (checked) sits
 * at the bottom with a downward drag to check, and this top-to-bottom order is not inverted in RTL.
 *
 * @param checked Whether the switch is currently checked (`true` selects [endIcon]).
 * @param onCheckedChange Callback invoked when the user toggles the switch while [enabled].
 * @param modifier [Modifier] applied to the switch canvas.
 * @param onToggleWhenDisabled Optional callback invoked when the user taps or activates the switch
 *   while disabled; when `null`, a disabled switch is inert and non-focusable.
 * @param enabled Whether the switch can change its checked state.
 * @param orientation Primary axis of the track and drag gesture ([Orientation.Horizontal] or
 *   [Orientation.Vertical]).
 * @param controlSize Track size variant: [ControlSize.Standard] uses `116x64.dp` horizontal or
 *   `64x116.dp` vertical; any other size uses the mini track (`92x48.dp` horizontal or `48x92.dp`
 *   vertical).
 * @param iconRotationDegrees Clockwise rotation in degrees applied around each icon's center.
 * @param showShadow Whether to draw the elevation drop shadow around the track.
 * @param trackColor Fill color of the pill track.
 * @param thumbColor Fill color of the circular thumb when [enabled].
 * @param disableColor Base color used to derive dimmed thumb and icon colors when disabled.
 * @param startIcon Optional drawable resource ID for the unchecked (`false`) position.
 * @param endIcon Optional drawable resource ID for the checked (`true`) position.
 * @param contentDescription Optional accessibility label identifying the switch control.
 * @param onClickLabel Optional accessibility action label describing what activating the switch
 *   does.
 * @param startIconDescription Optional accessibility state description when unchecked (`false`).
 * @param endIconDescription Optional accessibility state description when checked (`true`).
 * @param unselectedIconColor Tint color for icon regions outside the thumb bounds when [enabled].
 * @param selectedIconColor Tint color for icon regions overlapping the thumb when [enabled].
 * @param switchWidth Explicit width override; defaults from [orientation] and [controlSize].
 * @param switchHeight Explicit height override; defaults from [orientation] and [controlSize].
 * @param thumbDiameter Diameter of the circular thumb.
 * @param iconSize Rendered width and height of [startIcon] and [endIcon].
 */
// TODO: Confirm final vertical ordering with UX.
@Composable
fun ToggleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onToggleWhenDisabled: (() -> Unit)? = null,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Horizontal,
    controlSize: ControlSize = ControlSize.Standard,
    iconRotationDegrees: Float = 0f,
    showShadow: Boolean = true,
    trackColor: Color = Color.White.copy(alpha = ToggleSwitchTokens.DefaultTrackAlpha),
    thumbColor: Color = MaterialTheme.colorScheme.primaryFixedDim,
    disableColor: Color = Color.White,
    @DrawableRes startIcon: Int? = null,
    @DrawableRes endIcon: Int? = null,
    contentDescription: String? = null,
    onClickLabel: String? = null,
    startIconDescription: String? = null,
    endIconDescription: String? = null,
    unselectedIconColor: Color = MaterialTheme.colorScheme.secondaryFixed,
    selectedIconColor: Color = MaterialTheme.colorScheme.onPrimaryFixed,
    switchWidth: Dp = when (orientation) {
        Orientation.Horizontal -> trackLengthFor(controlSize)
        Orientation.Vertical -> trackThicknessFor(controlSize)
    },
    switchHeight: Dp = when (orientation) {
        Orientation.Horizontal -> trackThicknessFor(controlSize)
        Orientation.Vertical -> trackLengthFor(controlSize)
    },
    thumbDiameter: Dp = ToggleSwitchTokens.ThumbDiameter,
    iconSize: Dp = ToggleSwitchTokens.IconSize
) {
    // --- 1. Dimensions & Direction ---
    // Scale uniformly when the adaptive layout solver compacts the bottom toolbar row so the
    // rounded track edge stays aligned with the thumb. Apply only to standard horizontal switches
    // so mini or vertical switches do not get shrunk twice, and never shrink the cross axis below
    // the minimum touch target.
    val layoutSolution = LocalCameraLayoutSolution.current
    val isToolbarScalable =
        orientation == Orientation.Horizontal && controlSize == ControlSize.Standard
    val toolbarScale = if (isToolbarScalable) {
        layoutSolution?.toolbarHeight?.let { toolbarHeight ->
            (toolbarHeight / CameraLayoutDefaults.BottomToolbarHeight)
                .coerceIn(minimumScaleFor(minOf(switchWidth, switchHeight)), 1f)
        } ?: 1f
    } else {
        1f
    }
    val dims = rememberSwitchDimensions(
        orientation = orientation,
        switchWidth = switchWidth * toolbarScale,
        switchHeight = switchHeight * toolbarScale,
        thumbDiameter = thumbDiameter * toolbarScale,
        iconSize = iconSize * toolbarScale
    )
    val isHorizontalRtl =
        orientation == Orientation.Horizontal &&
            LocalLayoutDirection.current == LayoutDirection.Rtl
    val disableAnimations = LocalDisableAnimations.current
    val isInteractive = enabled || onToggleWhenDisabled != null
    val interactionSource = remember { MutableInteractionSource() }

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
    val fitScale = remember { SwitchFitScale() }
    val shadowContext = LocalGraphicsContext.current.shadowContext
    val shadowPainters = remember(shadowContext, showShadow) {
        if (showShadow) {
            listOf(ToggleSwitchTokens.AmbientShadow, ToggleSwitchTokens.KeyShadow).map { shadow ->
                shadowContext.createDropShadowPainter(CircleShape, shadow)
            }
        } else {
            emptyList()
        }
    }
    val dragInteractionSource = remember { DragCancelTrackingInteractionSource() }
    var dragSettleCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(enabled, orientation, isHorizontalRtl) {
        val target = if (currentChecked) 1f else 0f
        if (!enabled || dragState.isDragging) {
            dragState.isDragging = false
            dragState.gestureId++
            thumbProgress.syncToTarget(
                target = target,
                disableAnimations = currentDisableAnimations
            )
        } else if (!thumbProgress.isRunning && thumbProgress.value != target) {
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
                        dragInteractionSource.lastDragCancelled = false
                        dragState.isDragging = false
                        dragState.gestureId++
                        launch {
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

    LaunchedEffect(checked, disableAnimations, dragSettleCount) {
        if (!dragState.isDragging) {
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
        val fittedDragRange = currentDims.dragRange * fitScale.value
        if (currentEnabled && fittedDragRange > 0f) {
            if (!dragState.isDragging) {
                dragState.isDragging = true
                dragState.gestureId++
                dragState.progress = thumbProgress.value
            }
            val nextProgress =
                (dragState.progress + delta / fittedDragRange).coerceIn(0f, 1f)
            dragState.progress = nextProgress
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                thumbProgress.snapTo(nextProgress)
            }
        }
    }

    val resolvedStateDescription = if (checked) endIconDescription else startIconDescription

    // --- 6. Canvas Drawing ---
    Canvas(
        modifier = modifier
            .then(
                if (toolbarScale < 1f) {
                    Modifier
                } else {
                    Modifier.minimumInteractiveComponentSize()
                }
            )
            .fitSwitchWithin(
                width = dims.switchWidthDp,
                height = dims.switchHeightDp,
                fitScale = fitScale
            )
            .toggleSwitchShadow(shadowPainters)
            .clip(CircleShape)
            .indication(interactionSource, LocalIndication.current)
            .semantics {
                role = Role.Switch
                if (contentDescription != null) {
                    this.contentDescription = contentDescription
                }
                if (!isInteractive) {
                    disabled()
                }
                onClick(label = onClickLabel) {
                    if (currentEnabled) {
                        currentOnCheckedChange(!currentChecked)
                        true
                    } else {
                        val disabledCallback = currentOnToggleWhenDisabled
                        if (disabledCallback != null) {
                            disabledCallback()
                            true
                        } else {
                            false
                        }
                    }
                }
                toggleableState = ToggleableState(checked)
                if (resolvedStateDescription != null) {
                    stateDescription = resolvedStateDescription
                }
            }
            .onKeyEvent { event ->
                val isActivationKey = event.key == Key.Spacebar ||
                    event.key == Key.Enter ||
                    event.key == Key.NumPadEnter ||
                    event.key == Key.DirectionCenter
                val interactive = currentEnabled || currentOnToggleWhenDisabled != null
                if (!isActivationKey || !interactive) {
                    false
                } else if (event.type == KeyEventType.KeyUp) {
                    if (currentEnabled) {
                        currentOnCheckedChange(!currentChecked)
                    } else {
                        currentOnToggleWhenDisabled?.invoke()
                    }
                    true
                } else {
                    event.type == KeyEventType.KeyDown
                }
            }
            .focusable(enabled = isInteractive, interactionSource = interactionSource)
            .pointerInput(isInteractive) {
                if (!isInteractive) return@pointerInput
                detectTapGestures(
                    onTap = {
                        if (currentEnabled) {
                            currentOnCheckedChange(!currentChecked)
                        } else {
                            currentOnToggleWhenDisabled?.invoke()
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
                        dragState.isDragging = true
                        dragState.gestureId++
                        dragState.progress = thumbProgress.value
                    }
                },
                onDragStopped = {
                    val wasDragging = dragState.isDragging
                    val wasCancelled = dragInteractionSource.lastDragCancelled
                    dragInteractionSource.lastDragCancelled = false
                    val finalProgress =
                        if (wasDragging) dragState.progress else thumbProgress.value
                    dragState.isDragging = false
                    val settledGestureId = ++dragState.gestureId
                    if (wasDragging && !wasCancelled && currentEnabled) {
                        val newCheckedState = finalProgress > 0.5f
                        if (newCheckedState != currentChecked) {
                            currentOnCheckedChange(newCheckedState)
                            // Defer reverting for one frame so an asynchronous StateFlow/ViewModel
                            // update to `checked` can arrive without a 1-frame backward bounce,
                            // while still settling back if the caller declines the change.
                            coroutineScope.launch {
                                withFrameNanos {}
                                if (
                                    dragState.gestureId == settledGestureId &&
                                    !dragState.isDragging &&
                                    currentChecked != newCheckedState
                                ) {
                                    dragSettleCount++
                                }
                            }
                        } else {
                            dragSettleCount++
                        }
                    } else {
                        dragSettleCount++
                    }
                }
            )
    ) {
        val progress = thumbProgress.value
        val unscaledWidthPx = if (orientation == Orientation.Horizontal) {
            dims.mainAxisSizePx
        } else {
            dims.crossAxisSizePx
        }
        // Geometry is computed at the requested size and drawn through a uniform scale so a
        // switch squeezed by its parent stays proportional.
        val drawScale = if (unscaledWidthPx > 0f) size.width / unscaledWidthPx else 1f
        scale(scale = drawScale, pivot = Offset.Zero) {
            drawSwitchContent(
                dims = dims,
                orientation = orientation,
                isHorizontalRtl = isHorizontalRtl,
                progress = progress,
                trackColor = trackColorState.value,
                thumbColor = thumbColorState.value,
                iconRotationDegrees = iconRotationDegrees,
                startIconPainter = startIconPainter,
                endIconPainter = endIconPainter,
                offColorFilter = offColorFilter,
                onColorFilter = onColorFilter
            )
        }
    }
}

// --- Helper class and functions ---

private fun trackLengthFor(controlSize: ControlSize): Dp =
    if (controlSize == ControlSize.Standard) {
        ToggleSwitchTokens.TrackLength
    } else {
        ToggleSwitchTokens.MiniTrackLength
    }

private fun trackThicknessFor(controlSize: ControlSize): Dp =
    if (controlSize == ControlSize.Standard) {
        ToggleSwitchTokens.TrackThickness
    } else {
        ToggleSwitchTokens.MiniTrackThickness
    }

/**
 * Returns the smallest uniform scale that keeps a switch whose cross axis is [crossAxisSize] at or
 * above [ToggleSwitchTokens.MinTouchTargetSize]. Switches already at or below the minimum (such as
 * explicitly undersized custom switches) are never shrunk further.
 */
internal fun minimumScaleFor(crossAxisSize: Dp): Float =
    if (crossAxisSize > ToggleSwitchTokens.MinTouchTargetSize) {
        ToggleSwitchTokens.MinTouchTargetSize / crossAxisSize
    } else {
        1f
    }

/**
 * Returns the uniform scale (at most `1`) that fits a switch of [requestedWidthPx] x
 * [requestedHeightPx] within the maximum [constraints], never going below [minimumScale].
 */
internal fun fittedSwitchScale(
    requestedWidthPx: Int,
    requestedHeightPx: Int,
    constraints: Constraints,
    minimumScale: Float
): Float {
    val widthScale = if (constraints.hasBoundedWidth && requestedWidthPx > 0) {
        constraints.maxWidth.toFloat() / requestedWidthPx
    } else {
        1f
    }
    val heightScale = if (constraints.hasBoundedHeight && requestedHeightPx > 0) {
        constraints.maxHeight.toFloat() / requestedHeightPx
    } else {
        1f
    }
    return maxOf(minimumScale, minOf(1f, widthScale, heightScale))
}

/**
 * Draws [shadowPainters] behind the pill track, clipped out of the track's interior so the shadow
 * never shows through a translucent track color. Painters are drawn in list order.
 */
private fun Modifier.toggleSwitchShadow(shadowPainters: List<Painter>): Modifier =
    if (shadowPainters.isEmpty()) {
        this
    } else {
        drawWithCache {
            val trackPath = Path().apply {
                addOutline(CircleShape.createOutline(size, layoutDirection, this@drawWithCache))
            }
            onDrawBehind {
                clipPath(trackPath, clipOp = ClipOp.Difference) {
                    shadowPainters.forEach { painter -> with(painter) { draw(size) } }
                }
            }
        }
    }

/**
 * Measures the switch at its requested [width] x [height], scaling both axes uniformly when the
 * incoming constraints are smaller so the aspect ratio is preserved. The scale never shrinks the
 * cross axis below [ToggleSwitchTokens.MinTouchTargetSize]; at that floor the switch overflows the
 * incoming constraints, centered. The fitted node is also centered when the incoming minimum
 * constraints are larger than the fitted size. The applied scale is written to [fitScale] for drag
 * handling.
 */
private fun Modifier.fitSwitchWithin(width: Dp, height: Dp, fitScale: SwitchFitScale): Modifier =
    layout { measurable, constraints ->
        val requestedWidthPx = width.roundToPx().coerceAtLeast(0)
        val requestedHeightPx = height.roundToPx().coerceAtLeast(0)
        val scale = fittedSwitchScale(
            requestedWidthPx = requestedWidthPx,
            requestedHeightPx = requestedHeightPx,
            constraints = constraints,
            minimumScale = minimumScaleFor(minOf(width, height))
        )
        fitScale.value = scale

        val fittedWidthPx = (requestedWidthPx * scale).roundToInt()
        val fittedHeightPx = (requestedHeightPx * scale).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(fittedWidthPx, fittedHeightPx))
        val layoutWidthPx = fittedWidthPx.coerceIn(constraints.minWidth, constraints.maxWidth)
        val layoutHeightPx = fittedHeightPx.coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(layoutWidthPx, layoutHeightPx) {
            placeable.place(
                x = (layoutWidthPx - fittedWidthPx) / 2,
                y = (layoutHeightPx - fittedHeightPx) / 2
            )
        }
    }

/**
 * Draws the track, thumb, and clipped icons in the switch's unscaled coordinate space.
 */
private fun DrawScope.drawSwitchContent(
    dims: SwitchDimensions,
    orientation: Orientation,
    isHorizontalRtl: Boolean,
    progress: Float,
    trackColor: Color,
    thumbColor: Color,
    iconRotationDegrees: Float,
    startIconPainter: Painter?,
    endIconPainter: Painter?,
    offColorFilter: ColorFilter,
    onColorFilter: ColorFilter
) {
    val trackCornerRadius = CornerRadius(dims.crossAxisSizePx / 2f, dims.crossAxisSizePx / 2f)
    val trackSize = if (orientation == Orientation.Horizontal) {
        Size(dims.mainAxisSizePx, dims.crossAxisSizePx)
    } else {
        Size(dims.crossAxisSizePx, dims.mainAxisSizePx)
    }

    // 1. Draw the Track
    drawSwitchTrack(
        color = trackColor,
        trackSize = trackSize,
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
        color = thumbColor
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
 * Draws the switch track (the background) at [trackSize].
 */
private fun DrawScope.drawSwitchTrack(color: Color, trackSize: Size, cornerRadius: CornerRadius) {
    drawRoundRect(
        color = color,
        topLeft = Offset.Zero,
        size = trackSize,
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

@Preview(
    name = "Horizontal LTR - Image Capture Mode (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ToggleSwitchImageCaptureEnabledPreview() {
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

@Preview(
    name = "Horizontal LTR - Video Capture Mode (Dark)",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ToggleSwitchVideoCaptureEnabledPreview() {
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

@Preview(
    name = "Horizontal LTR - Video Capture Mode (Light)",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun ToggleSwitchLightVideoCaptureEnabledPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .padding(8.dp)
        ) {
            ToggleSwitch(
                startIcon = R.drawable.ic_camera_alt_outline,
                endIcon = R.drawable.ic_videocam_filled,
                checked = true,
                onCheckedChange = {},
                enabled = true
            )
        }
    }
}

@Preview(
    name = "Disabled - Image Capture Mode (Light)",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
@Composable
private fun ToggleSwitchLightImageCaptureDisabledPreview() {
    MaterialTheme(colorScheme = lightColorScheme()) {
        Box(
            modifier = Modifier
                .background(Color.Black)
                .padding(8.dp)
        ) {
            ToggleSwitch(
                startIcon = R.drawable.ic_camera_alt_filled,
                endIcon = R.drawable.ic_videocam_outline,
                checked = false,
                onCheckedChange = {},
                enabled = false
            )
        }
    }
}

@Preview(
    name = "Horizontal RTL - Image Capture Mode",
    locale = "ar",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ToggleSwitchRtlImageCapturePreview() {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
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
}

@Preview(
    name = "Horizontal Mini - Image Capture Mode",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
private fun ToggleSwitchHorizontalMiniPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_filled,
            endIcon = R.drawable.ic_videocam_outline,
            checked = false,
            onCheckedChange = {},
            controlSize = ControlSize.Mini
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

@Preview(name = "Vertical Mini (48x92)", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchVerticalMiniPreview() {
    MaterialTheme(colorScheme = darkColorScheme()) {
        ToggleSwitch(
            startIcon = R.drawable.ic_camera_alt_outline,
            endIcon = R.drawable.ic_videocam_filled,
            checked = true,
            onCheckedChange = {},
            orientation = Orientation.Vertical,
            controlSize = ControlSize.Mini
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

@Preview(name = "Disabled - Video Capture Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchVideoCaptureDisabledPreview() {
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

@Preview(name = "Disabled - Image Capture Mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ToggleSwitchImageCaptureDisabledPreview() {
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
