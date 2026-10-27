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
package com.google.jetpackcamera.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

const val TOOLTIP_ANCHOR_BUTTON_TAG = "tooltip_anchor_button"
const val TOOLTIP_CONTENT_TAG = "tooltip_content"

/**
 * Custom Tooltip Implementation Workaround Rationale:
 *
 * Upstream Material 3's [androidx.compose.material3.RichTooltip] and [androidx.compose.material3.TooltipBox]
 * suffer from three primary limitations that prevent accurate placement in custom camera layouts:
 *
 * 1. Misaligned Caret Coordinates: Material 3 computes caret offsets using an internal `Modifier.layoutCaret`
 *    and private `caretX(...)` function. When clamping the popup against window margins, M3's formula incorrectly
 *    assumes the tooltip popup is flush with the anchor edge or hardcoded margins. When the calling button is small
 *    or positioned near the screen edge (such as in an indicator or status bar), this causes the caret to drift
 *    tens of pixels away from the anchor center.
 * 2. Visual Hitch / Asynchronous Delay: M3 calculates caret transformations inside an asynchronous
 *    `onLayoutRectChanged` / `onGloballyPositioned` callback. This causes an unavoidable 1-frame visual jump where
 *    the tooltip first renders with default/stale caret coordinates before snapping to the recalculated position,
 *    triggering a full subtree recreation keyed on `tooltipSide`.
 * 3. Caret Shape Continuity: M3 draws the caret as a detached child rotated via a transformation matrix,
 *    preventing unified background fills, continuous corner-radius clipping, and single-pass elevation shadows.
 *
 * To resolve this, [JcaTooltipBox] uses standard Compose [androidx.compose.ui.window.Popup] paired with
 * [JcaTooltipPositionProvider] and [JcaTooltipBubbleShape]. This synchronously computes both popup window placement
 * and the exact pixel-level local caret coordinates on Frame 0 before draw, guarantees safe screen margins (16dp),
 * protects against corner-radius bleed, and supports flexible [CaretAlignment] configurations.
 */

/**
 * Alignment options for the tooltip caret along the horizontal edge of the tooltip bubble.
 */
sealed interface CaretAlignment {
    /** Positions the caret to point directly to the horizontal center of the anchor component. */
    data object AnchorCenter : CaretAlignment

    /** Positions the caret at the horizontal center of the tooltip bubble. */
    data object BubbleCenter : CaretAlignment

    /** Positions the caret near the start edge (respecting layout direction). */
    data object Start : CaretAlignment

    /** Positions the caret near the end edge (respecting layout direction). */
    data object End : CaretAlignment

    /** Positions the caret at a custom horizontal offset from the start edge. */
    data class Custom(val offset: Dp) : CaretAlignment
}

/**
 * Color configuration for the tooltip bubble.
 */
@Immutable
data class JcaTooltipColors(
    val containerColor: Color,
    val contentColor: Color,
    val titleContentColor: Color
)

/**
 * Default dimensions, styling tokens, and color values for JCA tooltips.
 */
object JcaTooltipDefaults {
    val MinWidth: Dp = 140.dp
    val MaxWidth: Dp = 296.dp
    val CornerRadius: Dp = 12.dp
    val CaretWidth: Dp = 16.dp
    val CaretHeight: Dp = 8.dp
    val ScreenMargin: Dp = 16.dp
    val SpacingBetweenTooltipAndAnchor: Dp = 4.dp
    val DefaultElevation: Dp = 4.dp

    @Composable
    fun tooltipColors(
        containerColor: Color = MaterialTheme.colorScheme.secondaryFixed,
        contentColor: Color = MaterialTheme.colorScheme.onSecondaryFixed,
        titleContentColor: Color = MaterialTheme.colorScheme.onSecondaryFixed
    ): JcaTooltipColors = JcaTooltipColors(
        containerColor = containerColor,
        contentColor = contentColor,
        titleContentColor = titleContentColor
    )
}

/**
 * State holder for dynamic caret positioning.
 */
@Stable
class CaretPositionState(
    initialCaretX: Float = 0f,
    initialIsCaretOnTop: Boolean = true
) {
    var localCaretX: Float by mutableFloatStateOf(initialCaretX)
    var isCaretOnTop: Boolean by mutableStateOf(initialIsCaretOnTop)
}

/**
 * Vector shape that draws a continuous outline combining a rounded rectangle bubble
 * and a triangular caret pointing toward the anchor.
 */
class JcaTooltipBubbleShape(
    val localCaretX: Float,
    val isCaretOnTop: Boolean,
    val cornerRadius: Dp = JcaTooltipDefaults.CornerRadius,
    val caretWidth: Dp = JcaTooltipDefaults.CaretWidth,
    val caretHeight: Dp = JcaTooltipDefaults.CaretHeight
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) {
            return Outline.Rectangle(Rect.Zero)
        }

        val r = with(density) { cornerRadius.toPx() }
        val cw = with(density) { caretWidth.toPx() }
        val ch = with(density) { caretHeight.toPx() }

        val bodyHeight = (h - ch).coerceAtLeast(0f)
        val actualRadius = minOf(r, bodyHeight / 2f, w / 2f).coerceAtLeast(0f)

        // Clamp caret base to ensure the triangle never bleeds into the corner radius arcs
        val minCaretX = actualRadius + cw / 2f
        val maxCaretX = (w - actualRadius - cw / 2f).coerceAtLeast(minCaretX)
        val clampedCaretX = localCaretX.coerceIn(minCaretX, maxCaretX)

        val path = Path()
        if (isCaretOnTop) {
            val bodyTop = ch
            val bodyBottom = h

            path.moveTo(actualRadius, bodyTop)
            path.lineTo(clampedCaretX - cw / 2f, bodyTop)
            path.lineTo(clampedCaretX, 0f)
            path.lineTo(clampedCaretX + cw / 2f, bodyTop)
            path.lineTo(w - actualRadius, bodyTop)
            path.arcTo(
                rect = Rect(w - 2f * actualRadius, bodyTop, w, bodyTop + 2f * actualRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            path.lineTo(w, bodyBottom - actualRadius)
            path.arcTo(
                rect = Rect(w - 2f * actualRadius, bodyBottom - 2f * actualRadius, w, bodyBottom),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            path.lineTo(actualRadius, bodyBottom)
            path.arcTo(
                rect = Rect(0f, bodyBottom - 2f * actualRadius, 2f * actualRadius, bodyBottom),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            path.lineTo(0f, bodyTop + actualRadius)
            path.arcTo(
                rect = Rect(0f, bodyTop, 2f * actualRadius, bodyTop + 2f * actualRadius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            path.close()
        } else {
            val bodyTop = 0f
            val bodyBottom = h - ch

            path.moveTo(actualRadius, bodyTop)
            path.lineTo(w - actualRadius, bodyTop)
            path.arcTo(
                rect = Rect(w - 2f * actualRadius, bodyTop, w, bodyTop + 2f * actualRadius),
                startAngleDegrees = 270f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            path.lineTo(w, bodyBottom - actualRadius)
            path.arcTo(
                rect = Rect(w - 2f * actualRadius, bodyBottom - 2f * actualRadius, w, bodyBottom),
                startAngleDegrees = 0f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            path.lineTo(clampedCaretX + cw / 2f, bodyBottom)
            path.lineTo(clampedCaretX, h)
            path.lineTo(clampedCaretX - cw / 2f, bodyBottom)
            path.lineTo(actualRadius, bodyBottom)
            path.arcTo(
                rect = Rect(0f, bodyBottom - 2f * actualRadius, 2f * actualRadius, bodyBottom),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            path.lineTo(0f, bodyTop + actualRadius)
            path.arcTo(
                rect = Rect(0f, bodyTop, 2f * actualRadius, bodyTop + 2f * actualRadius),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 90f,
                forceMoveTo = false
            )
            path.close()
        }

        return Outline.Generic(path)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JcaTooltipBubbleShape) return false
        return localCaretX == other.localCaretX &&
            isCaretOnTop == other.isCaretOnTop &&
            cornerRadius == other.cornerRadius &&
            caretWidth == other.caretWidth &&
            caretHeight == other.caretHeight
    }

    override fun hashCode(): Int {
        var result = localCaretX.hashCode()
        result = 31 * result + isCaretOnTop.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + caretWidth.hashCode()
        result = 31 * result + caretHeight.hashCode()
        return result
    }
}

/**
 * Position provider for the tooltip popup that calculates screen-clamped coordinates and
 * derives the local caret offset relative to the popup's top-left corner on Frame 0 before draw.
 */
@OptIn(ExperimentalMaterial3Api::class)
class JcaTooltipPositionProvider(
    val positioning: TooltipAnchorPosition,
    val caretAlignment: CaretAlignment = CaretAlignment.AnchorCenter,
    val spacing: Dp = JcaTooltipDefaults.SpacingBetweenTooltipAndAnchor,
    val screenMargin: Dp = JcaTooltipDefaults.ScreenMargin,
    val density: Density,
    val onCaretCalculated: (localCaretX: Float, isCaretOnTop: Boolean) -> Unit = { _, _ -> }
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val spacingPx = with(density) { spacing.roundToPx() }
        val marginPx = with(density) { screenMargin.roundToPx() }

        // Vertical placement: Above vs Below with automatic flipping when constrained
        val spaceAbove = anchorBounds.top - spacingPx
        val spaceBelow = windowSize.height - anchorBounds.bottom - spacingPx

        val placeBelow = when (positioning) {
            TooltipAnchorPosition.Below -> {
                if (spaceBelow >= popupContentSize.height || spaceBelow >= spaceAbove) {
                    true
                } else {
                    false
                }
            }
            TooltipAnchorPosition.Above -> {
                if (spaceAbove >= popupContentSize.height || spaceAbove >= spaceBelow) {
                    false
                } else {
                    true
                }
            }
            else -> spaceBelow >= popupContentSize.height
        }

        val popupY = if (placeBelow) {
            anchorBounds.bottom + spacingPx
        } else {
            anchorBounds.top - spacingPx - popupContentSize.height
        }

        // Horizontal placement: Center over anchor by default, clamped to safe screen margins
        val anchorCenterX = (anchorBounds.left + anchorBounds.right) / 2f
        val idealPopupX = (anchorCenterX - popupContentSize.width / 2f).roundToInt()

        val minX = marginPx
        val maxX = (windowSize.width - marginPx - popupContentSize.width).coerceAtLeast(minX)
        val popupX = idealPopupX.coerceIn(minX, maxX)

        // Local caret X coordinate relative to the popup's top-left corner
        val localCaretX = when (caretAlignment) {
            is CaretAlignment.AnchorCenter -> {
                anchorCenterX - popupX
            }
            is CaretAlignment.BubbleCenter -> {
                popupContentSize.width / 2f
            }
            is CaretAlignment.Start -> {
                val cornerRadiusPx = with(density) { JcaTooltipDefaults.CornerRadius.toPx() }
                val caretWidthPx = with(density) { JcaTooltipDefaults.CaretWidth.toPx() }
                if (layoutDirection == LayoutDirection.Rtl) {
                    popupContentSize.width - cornerRadiusPx - caretWidthPx / 2f
                } else {
                    cornerRadiusPx + caretWidthPx / 2f
                }
            }
            is CaretAlignment.End -> {
                val cornerRadiusPx = with(density) { JcaTooltipDefaults.CornerRadius.toPx() }
                val caretWidthPx = with(density) { JcaTooltipDefaults.CaretWidth.toPx() }
                if (layoutDirection == LayoutDirection.Rtl) {
                    cornerRadiusPx + caretWidthPx / 2f
                } else {
                    popupContentSize.width - cornerRadiusPx - caretWidthPx / 2f
                }
            }
            is CaretAlignment.Custom -> {
                val offsetPx = with(density) { caretAlignment.offset.toPx() }
                if (layoutDirection == LayoutDirection.Rtl) {
                    popupContentSize.width - offsetPx
                } else {
                    offsetPx
                }
            }
        }

        onCaretCalculated(localCaretX, placeBelow)

        return IntOffset(popupX, popupY)
    }
}

/**
 * Events used to trigger programmatic display or dismissal of tooltips.
 */
sealed interface TooltipTriggerEvent {
    data object Show : TooltipTriggerEvent
    data object Dismiss : TooltipTriggerEvent
}

/**
 * Custom tooltip bubble composable rendering styled text and an optional title within
 * a unified [JcaTooltipBubbleShape].
 */
@Composable
fun JcaTooltipBubble(
    tooltipText: String,
    modifier: Modifier = Modifier,
    tooltipTitle: String? = null,
    caretPositionState: CaretPositionState = remember { CaretPositionState() },
    colors: JcaTooltipColors = JcaTooltipDefaults.tooltipColors(),
    elevation: Dp = JcaTooltipDefaults.DefaultElevation
) {
    val isCaretOnTop = caretPositionState.isCaretOnTop
    val localCaretX = caretPositionState.localCaretX

    val bubbleShape = remember(localCaretX, isCaretOnTop) {
        JcaTooltipBubbleShape(
            localCaretX = localCaretX,
            isCaretOnTop = isCaretOnTop
        )
    }

    val topPadding = if (isCaretOnTop) {
        JcaTooltipDefaults.CaretHeight + 10.dp
    } else {
        10.dp
    }
    val bottomPadding = if (!isCaretOnTop) {
        JcaTooltipDefaults.CaretHeight + 10.dp
    } else {
        10.dp
    }

    Surface(
        shape = bubbleShape,
        color = colors.containerColor,
        shadowElevation = elevation,
        modifier = modifier
            .widthIn(min = JcaTooltipDefaults.MinWidth, max = JcaTooltipDefaults.MaxWidth)
            .testTag(TOOLTIP_CONTENT_TAG)
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                paneTitle = tooltipTitle ?: tooltipText
            }
    ) {
        Column(
            modifier = Modifier.padding(
                top = topPadding,
                bottom = bottomPadding,
                start = 12.dp,
                end = 12.dp
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (tooltipTitle != null) {
                Text(
                    text = tooltipTitle,
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.titleContentColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp)
                )
            }
            Text(
                text = tooltipText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.contentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * A generic container that wraps content with a styled tooltip using a bespoke Compose [Popup].
 *
 * @param tooltipText The text to display inside the tooltip.
 * @param modifier Modifier for the container.
 * @param tooltipTitle Optional title displayed above the tooltip text.
 * @param state State controlling tooltip visibility.
 * @param positioning Anchor position for the tooltip (e.g. Above, Below).
 * @param caretAlignment Alignment configuration for the caret.
 * @param autoDismissDuration Optional duration after which the tooltip automatically dismisses.
 * @param triggerFlow Optional flow of [TooltipTriggerEvent] to trigger showing or dismissing.
 * @param enableUserInput Whether long-press gestures on the anchor trigger the tooltip.
 * @param colors Colors applied to the tooltip bubble and text.
 * @param onDismissRequest Optional callback invoked when the tooltip is dismissed.
 * @param content The anchor composable wrapped by the tooltip.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JcaTooltipBox(
    tooltipText: String,
    modifier: Modifier = Modifier,
    tooltipTitle: String? = null,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Above,
    caretAlignment: CaretAlignment = CaretAlignment.AnchorCenter,
    autoDismissDuration: Duration? = null,
    triggerFlow: Flow<TooltipTriggerEvent>? = null,
    enableUserInput: Boolean = true,
    colors: JcaTooltipColors = JcaTooltipDefaults.tooltipColors(),
    onDismissRequest: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val scope = rememberCoroutineScope()
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    var triggerRevision by remember { mutableIntStateOf(0) }

    // External flow triggers using collectLatest to prevent deadlock from show() suspension
    if (triggerFlow != null) {
        LaunchedEffect(triggerFlow, state) {
            triggerFlow.collectLatest { event ->
                when (event) {
                    TooltipTriggerEvent.Show -> {
                        triggerRevision++
                        state.show()
                    }
                    TooltipTriggerEvent.Dismiss -> state.dismiss()
                }
            }
        }
    }

    // Auto-dismiss timer keyed on targetState and triggerRevision
    if (autoDismissDuration != null) {
        LaunchedEffect(state.transition.targetState, triggerRevision, autoDismissDuration) {
            if (state.transition.targetState) {
                delay(autoDismissDuration)
                state.dismiss()
                currentOnDismissRequest?.invoke()
            }
        }
    }

    val density = LocalDensity.current
    val caretPositionState = remember {
        CaretPositionState(
            initialIsCaretOnTop = positioning == TooltipAnchorPosition.Below
        )
    }

    val positionProvider = remember(positioning, caretAlignment, density) {
        JcaTooltipPositionProvider(
            positioning = positioning,
            caretAlignment = caretAlignment,
            density = density,
            onCaretCalculated = { localCaretX, isCaretOnTop ->
                caretPositionState.localCaretX = localCaretX
                caretPositionState.isCaretOnTop = isCaretOnTop
            }
        )
    }

    Box(
        modifier = modifier.tooltipGestures(
            enabled = enableUserInput,
            state = state,
            scope = scope
        )
    ) {
        content()

        if (state.isVisible) {
            Popup(
                popupPositionProvider = positionProvider,
                onDismissRequest = {
                    scope.launch { state.dismiss() }
                    currentOnDismissRequest?.invoke()
                },
                properties = PopupProperties(
                    focusable = false,
                    clippingEnabled = false
                )
            ) {
                JcaTooltipBubble(
                    tooltipText = tooltipText,
                    tooltipTitle = tooltipTitle,
                    caretPositionState = caretPositionState,
                    colors = colors
                )
            }
        }
    }

    DisposableEffect(state) {
        onDispose {
            state.onDispose()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun Modifier.tooltipGestures(
    enabled: Boolean,
    state: TooltipState,
    scope: CoroutineScope
): Modifier = if (enabled) {
    this.pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(pass = PointerEventPass.Initial)
            try {
                withTimeout(viewConfiguration.longPressTimeoutMillis) {
                    waitForUpOrCancellation(pass = PointerEventPass.Initial)
                }
            } catch (e: PointerEventTimeoutCancellationException) {
                scope.launch { state.show() }
            }
        }
    }
} else {
    this
}

/**
 * An icon button that shows a styled tooltip when clicked.
 *
 * @param icon The icon to display inside the button.
 * @param tooltipText The text to display inside the tooltip.
 * @param modifier Modifier for the button container.
 * @param tooltipTitle The optional title to display inside the tooltip.
 * @param state State controlling tooltip visibility.
 * @param positioning Anchor position for the tooltip (e.g. Above, Below). Defaults to Below.
 * @param caretAlignment Alignment configuration for the caret. Defaults to AnchorCenter.
 * @param autoDismissDuration Optional duration after which the tooltip automatically dismisses.
 * @param triggerFlow Optional flow of [TooltipTriggerEvent] to trigger showing or dismissing.
 * @param isOutlined Whether the icon button should be outlined.
 * @param containerColor The background container color for the button.
 * @param contentColor The content/tint color for the icon.
 * @param size The size of the button container, defaulting to [IconButtonDefaults.extraSmallContainerSize] (32.dp).
 * @param onClick Optional callback when the button is clicked.
 * @param onDismissRequest Optional callback invoked when the tooltip is dismissed.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    icon: @Composable () -> Unit,
    tooltipText: String,
    modifier: Modifier = Modifier,
    tooltipTitle: String? = null,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    caretAlignment: CaretAlignment = CaretAlignment.AnchorCenter,
    autoDismissDuration: Duration? = null,
    triggerFlow: Flow<TooltipTriggerEvent>? = null,
    isOutlined: Boolean = false,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    size: Dp = IconButtonDefaults.extraSmallContainerSize().width,
    onClick: () -> Unit = {},
    onDismissRequest: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)

    val toggleTooltip = remember(state, scope) {
        {
            scope.launch {
                if (state.transition.targetState) {
                    state.dismiss()
                    currentOnDismissRequest?.invoke()
                } else {
                    state.show()
                }
            }
            currentOnClick()
        }
    }

    JcaTooltipBox(
        tooltipText = tooltipText,
        modifier = modifier,
        tooltipTitle = tooltipTitle,
        state = state,
        positioning = positioning,
        caretAlignment = caretAlignment,
        autoDismissDuration = autoDismissDuration,
        triggerFlow = triggerFlow,
        enableUserInput = false,
        onDismissRequest = onDismissRequest
    ) {
        val buttonModifier = Modifier
            .size(size)
            .testTag(TOOLTIP_ANCHOR_BUTTON_TAG)

        if (isOutlined) {
            OutlinedIconButton(
                onClick = toggleTooltip,
                modifier = buttonModifier,
                colors = IconButtonDefaults.outlinedIconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                ),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                icon()
            }
        } else {
            IconButton(
                onClick = toggleTooltip,
                modifier = buttonModifier,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = containerColor,
                    contentColor = contentColor
                )
            ) {
                icon()
            }
        }
    }
}
