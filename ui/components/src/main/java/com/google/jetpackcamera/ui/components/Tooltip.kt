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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RichTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.TooltipScope
import androidx.compose.material3.TooltipState
import androidx.compose.material3.rememberTooltipState
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupPositionProvider
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

/** Test tag applied to the interactive anchor button inside [TooltipIconButton]. */
const val TOOLTIP_ANCHOR_BUTTON_TAG = "btn_tooltip_anchor"

/** Test tag applied to the default [RichTooltip] surface rendered by [TooltipIconButton]. */
const val TOOLTIP_CONTENT_TAG = "tooltip_content"

/*
 * [JcaTooltipPositionProvider] and [JcaTooltipBubbleShape] plug into Material 3's [TooltipBox] and
 * [RichTooltip] (currently androidx.compose.material3:material3:1.5.0-alpha27) to work around two
 * upstream caret issues:
 * 1. Near screen edges, M3 clamps the popup window without shifting the caret cleanly to track
 *    edge-aligned anchors within safe screen margins.
 * 2. M3 computes caret coordinates in `onLayoutRectChanged` after placement, causing the caret to
 *    appear a few frames late.
 *
 * TODO: Remove JcaTooltipPositionProvider and JcaTooltipBubbleShape once upstream Material 3
 *   resolves edge-clamped caret placement and initial-frame caret lag.
 */

private object TooltipTokens {
    val BorderWidth = 1.dp
    const val BORDER_ALPHA = 0.2f

    // Added to M3 RichTooltip's 4.dp plain vertical padding when no title/action is present
    // to achieve 16.dp vertical body padding.
    val TextExtraVerticalPadding = 12.dp
}

/**
 * Default dimensions for JCA tooltips, referencing Material 3 component defaults where available.
 */
@OptIn(ExperimentalMaterial3Api::class)
object JcaTooltipDefaults {
    /** Default caret dimensions matching Material 3 [TooltipDefaults.caretSize] (16dp x 8dp). */
    val CaretSize: DpSize = TooltipDefaults.caretSize

    /** Default base width of the tooltip caret (16dp). */
    val CaretWidth: Dp get() = CaretSize.width

    /** Default height of the tooltip caret extending from the bubble body (8dp). */
    val CaretHeight: Dp get() = CaretSize.height

    /** Default spacing between the tooltip bubble and calling anchor. */
    val SpacingBetweenTooltipAndAnchor: Dp = 4.dp

    /** Safe screen edge margins for clamping popup placement against window boundaries. */
    val ScreenMargin: Dp = 16.dp

    /** Large corner radius token (16dp) matching Material 3 ShapeDefaults.Large. */
    val CornerRadius: Dp = 16.dp

    /** Standard Material minimum interactive touch target size (48dp). */
    val TouchTargetSize: Dp = 48.dp

    /** Visual container size (32dp) matching [IconButtonDefaults.extraSmallContainerSize]. */
    val ContainerSize: Dp = IconButtonDefaults.extraSmallContainerSize().width

    /** Medium icon size (24dp) matching [IconButtonDefaults.mediumIconSize]. */
    val IconSize: Dp = IconButtonDefaults.mediumIconSize

    /**
     * Maximum width aligned with design mock (296dp),
     * capped within [TooltipDefaults.richTooltipMaxWidth] (320dp).
     */
    val MaxWidth: Dp = 296.dp
}

@Stable
private class CaretPositionState(
    initialCaretX: Float = 0f,
    initialIsCaretOnTop: Boolean = true
) {
    var localCaretX: Float by mutableFloatStateOf(initialCaretX)
    var isCaretOnTop: Boolean by mutableStateOf(initialIsCaretOnTop)
}

/**
 * Coordinates edge-aware tooltip placement, caret padding, and bubble shape rendering for a
 * [TooltipIconButton] or [TooltipBox].
 *
 * Create an instance via [rememberJcaTooltipCaret], pass [positionProvider] to [TooltipIconButton]
 * (or [TooltipBox]), apply [caretPadding] to the tooltip content so it clears the caret triangle,
 * and pass [bubbleShape] to [RichTooltip]'s `shape` parameter with `caretShape = null`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Stable
class JcaTooltipCaret internal constructor(
    positioning: TooltipAnchorPosition,
    density: Density,
    private val caretWidth: Dp = JcaTooltipDefaults.CaretWidth,
    private val caretHeight: Dp = JcaTooltipDefaults.CaretHeight
) {
    private val caretPositionState = CaretPositionState(
        initialIsCaretOnTop = positioning == TooltipAnchorPosition.Below
    )
    private val localCaretXProvider: () -> Float = { caretPositionState.localCaretX }
    private val topCaretPadding = PaddingValues(top = caretHeight)
    private val bottomCaretPadding = PaddingValues(bottom = caretHeight)

    /**
     * [PopupPositionProvider] that clamps popup placement within safe screen margins and updates
     * the caret coordinates during layout before the first draw frame.
     */
    val positionProvider: PopupPositionProvider = JcaTooltipPositionProvider(
        positioning = positioning,
        caretWidth = caretWidth,
        caretHeight = caretHeight,
        density = density,
        onCaretCalculated = { localCaretX, isCaretOnTop ->
            caretPositionState.localCaretX = localCaretX
            caretPositionState.isCaretOnTop = isCaretOnTop
        }
    )

    /**
     * Padding that reserves [caretHeight] on the edge of the bubble where the caret is drawn
     * (top when positioned below the anchor, bottom when positioned above).
     */
    val caretPadding: PaddingValues
        get() = if (caretPositionState.isCaretOnTop) topCaretPadding else bottomCaretPadding

    /**
     * Returns a [Shape] combining a rounded rectangle of [cornerRadius] with a caret pointing
     * toward the anchor. The horizontal caret offset is read during [Shape.createOutline] in the
     * draw phase so Frame 0 renders at the exact anchor X without an extra recomposition.
     */
    fun bubbleShape(cornerRadius: Dp = JcaTooltipDefaults.CornerRadius): Shape =
        JcaTooltipBubbleShape(
            localCaretXProvider = localCaretXProvider,
            isCaretOnTop = caretPositionState.isCaretOnTop,
            cornerRadius = cornerRadius,
            caretWidth = caretWidth,
            caretHeight = caretHeight
        )
}

/**
 * Remembers a [JcaTooltipCaret] keyed on [positioning] for pairing edge-clamped popup placement
 * with [JcaTooltipCaret.bubbleShape] and [JcaTooltipCaret.caretPadding].
 *
 * @param positioning preferred anchor placement ([TooltipAnchorPosition.Below] or
 *   [TooltipAnchorPosition.Above]); flips automatically when vertical window space is constrained.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun rememberJcaTooltipCaret(
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below
): JcaTooltipCaret {
    val density = LocalDensity.current
    return remember(positioning, density) {
        JcaTooltipCaret(
            positioning = positioning,
            density = density
        )
    }
}

private fun clampCaretX(
    caretX: Float,
    width: Float,
    bodyHeight: Float,
    cornerRadiusPx: Float,
    caretWidthPx: Float
): Float {
    val actualRadius = minOf(cornerRadiusPx, bodyHeight / 2f, width / 2f).coerceAtLeast(0f)
    val minCaretX = actualRadius + caretWidthPx / 2f
    val maxCaretX = (width - actualRadius - caretWidthPx / 2f).coerceAtLeast(minCaretX)
    return caretX.coerceIn(minCaretX, maxCaretX)
}

/**
 * Shape that unions a rounded rectangle bubble body with a triangular caret pointing toward the
 * anchor.
 *
 * @param localCaretXProvider deferred provider for the caret center X in local popup coordinates,
 *   evaluated inside [createOutline] during the draw phase after popup placement has run.
 * @param isCaretOnTop `true` when the caret is along the top edge pointing up at the anchor, or
 *   `false` when along the bottom edge pointing down.
 * @param cornerRadius corner radius of the rounded rectangle bubble body.
 * @param caretWidth base width of the triangular caret.
 * @param caretHeight height of the triangular caret extending from the bubble body.
 */
internal class JcaTooltipBubbleShape(
    private val localCaretXProvider: () -> Float,
    private val isCaretOnTop: Boolean,
    private val cornerRadius: Dp = JcaTooltipDefaults.CornerRadius,
    private val caretWidth: Dp = JcaTooltipDefaults.CaretWidth,
    private val caretHeight: Dp = JcaTooltipDefaults.CaretHeight
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
        val bodyTop = if (isCaretOnTop) ch else 0f
        // Clamp caret base so the triangle never bleeds into the corner radius arcs
        val clampedCaretX = clampCaretX(
            caretX = localCaretXProvider(),
            width = w,
            bodyHeight = bodyHeight,
            cornerRadiusPx = r,
            caretWidthPx = cw
        )

        val bodyOutline = RoundedCornerShape(cornerRadius).createOutline(
            size = Size(w, bodyHeight),
            layoutDirection = layoutDirection,
            density = density
        )
        val bodyPath = Path().apply {
            when (bodyOutline) {
                is Outline.Generic -> addPath(bodyOutline.path)
                is Outline.Rounded -> addRoundRect(bodyOutline.roundRect)
                is Outline.Rectangle -> addRect(bodyOutline.rect)
            }
            translate(Offset(0f, bodyTop))
        }

        val baseY = if (isCaretOnTop) bodyTop else bodyHeight
        val tipY = if (isCaretOnTop) 0f else h
        val caretPath = Path().apply {
            moveTo(clampedCaretX - cw / 2f, baseY)
            lineTo(clampedCaretX, tipY)
            lineTo(clampedCaretX + cw / 2f, baseY)
            close()
        }

        val combinedPath = Path().apply {
            op(bodyPath, caretPath, PathOperation.Union)
        }
        return Outline.Generic(combinedPath)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is JcaTooltipBubbleShape) return false
        return localCaretXProvider == other.localCaretXProvider &&
            isCaretOnTop == other.isCaretOnTop &&
            cornerRadius == other.cornerRadius &&
            caretWidth == other.caretWidth &&
            caretHeight == other.caretHeight
    }

    override fun hashCode(): Int {
        var result = localCaretXProvider.hashCode()
        result = 31 * result + isCaretOnTop.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + caretWidth.hashCode()
        result = 31 * result + caretHeight.hashCode()
        return result
    }
}

/**
 * Position provider for the tooltip popup that clamps popup coordinates to safe screen margins and
 * derives the corner-clamped local caret offset relative to the popup's top-left corner before
 * draw.
 *
 * @param positioning preferred placement relative to the anchor ([TooltipAnchorPosition.Below] or
 *   [TooltipAnchorPosition.Above]), flipping automatically when window space is constrained.
 * @param spacing vertical gap between the anchor bounds and the tooltip popup.
 * @param screenMargin minimum horizontal and vertical distance from window edges.
 * @param cornerRadius bubble corner radius used to keep the caret base clear of corner arcs.
 * @param caretWidth base width of the caret triangle.
 * @param caretHeight height of the caret triangle subtracted from popup height for corner clamping.
 * @param density screen density used to convert Dp dimensions to pixels.
 * @param onCaretCalculated callback invoked during layout with the clamped local caret center X and
 *   whether the caret sits on the top edge (`true` when placed below the anchor).
 */
@OptIn(ExperimentalMaterial3Api::class)
internal class JcaTooltipPositionProvider(
    private val positioning: TooltipAnchorPosition,
    private val spacing: Dp = JcaTooltipDefaults.SpacingBetweenTooltipAndAnchor,
    private val screenMargin: Dp = JcaTooltipDefaults.ScreenMargin,
    private val cornerRadius: Dp = JcaTooltipDefaults.CornerRadius,
    private val caretWidth: Dp = JcaTooltipDefaults.CaretWidth,
    private val caretHeight: Dp = JcaTooltipDefaults.CaretHeight,
    private val density: Density,
    private val onCaretCalculated: (localCaretX: Float, isCaretOnTop: Boolean) -> Unit = { _, _ -> }
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
                spaceBelow >= popupContentSize.height || spaceBelow >= spaceAbove
            }
            TooltipAnchorPosition.Above -> {
                !(spaceAbove >= popupContentSize.height || spaceAbove >= spaceBelow)
            }
            else -> spaceBelow >= popupContentSize.height
        }

        val idealPopupY = if (placeBelow) {
            anchorBounds.bottom + spacingPx
        } else {
            anchorBounds.top - spacingPx - popupContentSize.height
        }
        val minY = marginPx
        val maxY = (windowSize.height - marginPx - popupContentSize.height).coerceAtLeast(minY)
        val popupY = idealPopupY.coerceIn(minY, maxY)

        // Horizontal placement: Center over anchor by default, clamped to safe screen margins
        val anchorCenterX = (anchorBounds.left + anchorBounds.right) / 2f
        val idealPopupX = (anchorCenterX - popupContentSize.width / 2f).roundToInt()

        val minX = marginPx
        val maxX = (windowSize.width - marginPx - popupContentSize.width).coerceAtLeast(minX)
        val popupX = idealPopupX.coerceIn(minX, maxX)

        // Clamp local caret X so the caret base stays outside the rounded corners and matches
        // what JcaTooltipBubbleShape draws
        val bodyHeight =
            (popupContentSize.height - with(density) { caretHeight.toPx() }).coerceAtLeast(0f)
        val localCaretX = clampCaretX(
            caretX = anchorCenterX - popupX,
            width = popupContentSize.width.toFloat(),
            bodyHeight = bodyHeight,
            cornerRadiusPx = with(density) { cornerRadius.toPx() },
            caretWidthPx = with(density) { caretWidth.toPx() }
        )

        onCaretCalculated(localCaretX, placeBelow)

        return IntOffset(popupX, popupY)
    }
}

/**
 * Circular camera-overlay icon button that toggles a styled [RichTooltip] when tapped.
 *
 * Uses Material 3's [TooltipBox] and [RichTooltip] with `secondaryFixed` / `onSecondaryFixed` theme
 * colors, polite live-region announcements, and edge-clamped caret placement via
 * [rememberJcaTooltipCaret]. Renders a circular [size] container centered inside a
 * [minTouchTargetSize] touch target; when [isOutlined] is `true`, draws a `1.dp` circular border at
 * 20% [contentColor] alpha. [onClick] fires on every tap, whereas [onDismissRequest] fires once
 * each time an open tooltip is dismissed.
 *
 * @param icon icon composable centered inside the button container with [LocalContentColor] set to
 *   [contentColor].
 * @param tooltipText body copy displayed inside the tooltip; receives extra vertical padding when
 *   [tooltipTitle] is `null`.
 * @param modifier [Modifier] applied to the outer [TooltipBox] wrapper.
 * @param contentDescription accessibility label for the anchor button; falls back to
 *   `tooltipTitle ?: tooltipText` when `null`.
 * @param tooltipTitle optional centered header displayed above [tooltipText] and preferred for the
 *   popup's accessibility `paneTitle`.
 * @param state controls tooltip visibility; defaults to a persistent [TooltipState] so the bubble
 *   remains open until tapped again or dismissed externally.
 * @param positioning preferred placement relative to the anchor ([TooltipAnchorPosition.Below] or
 *   [TooltipAnchorPosition.Above]), flipping automatically when vertical window space is tight.
 * @param isOutlined when `true`, draws a `1.dp` circular border around the [size] container using
 *   [contentColor] at 20% alpha.
 * @param containerColor background fill of the circular [size] button container.
 * @param contentColor tint provided via [LocalContentColor] to [icon] and used at 20% alpha for the
 *   [isOutlined] border.
 * @param size diameter of the visible circular button container inside the touch target.
 * @param minTouchTargetSize minimum interactive bounds and unbounded ripple diameter of the anchor.
 * @param onClick callback invoked on every anchor tap after toggling tooltip visibility.
 * @param onDismissRequest optional callback invoked once when an open tooltip is dismissed (via
 *   anchor re-tap or outside touch).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    icon: @Composable () -> Unit,
    tooltipText: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    tooltipTitle: String? = null,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    positioning: TooltipAnchorPosition = TooltipAnchorPosition.Below,
    isOutlined: Boolean = false,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    size: Dp = JcaTooltipDefaults.ContainerSize,
    minTouchTargetSize: Dp = JcaTooltipDefaults.TouchTargetSize,
    onClick: () -> Unit = {},
    onDismissRequest: (() -> Unit)? = null
) {
    val caret = rememberJcaTooltipCaret(positioning)

    TooltipIconButton(
        icon = icon,
        contentDescription = contentDescription ?: tooltipTitle ?: tooltipText,
        modifier = modifier,
        state = state,
        positionProvider = caret.positionProvider,
        isOutlined = isOutlined,
        containerColor = containerColor,
        contentColor = contentColor,
        size = size,
        minTouchTargetSize = minTouchTargetSize,
        onClick = onClick,
        onDismissRequest = onDismissRequest,
        tooltip = {
            val caretPadding = caret.caretPadding
            val caretTopPadding = caretPadding.calculateTopPadding()
            val caretBottomPadding = caretPadding.calculateBottomPadding()

            RichTooltip(
                modifier = Modifier
                    .testTag(TOOLTIP_CONTENT_TAG)
                    .semantics {
                        liveRegion = LiveRegionMode.Polite
                        paneTitle = tooltipTitle ?: tooltipText
                    },
                title = tooltipTitle?.let { titleText ->
                    {
                        Text(
                            text = titleText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = caretTopPadding)
                        )
                    }
                },
                caretShape = null,
                maxWidth = JcaTooltipDefaults.MaxWidth,
                shape = caret.bubbleShape(),
                colors = TooltipDefaults.richTooltipColors(
                    containerColor = MaterialTheme.colorScheme.secondaryFixed,
                    contentColor = MaterialTheme.colorScheme.onSecondaryFixed,
                    titleContentColor = MaterialTheme.colorScheme.onSecondaryFixed
                )
            ) {
                Text(
                    text = tooltipText,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = if (tooltipTitle == null) {
                                caretTopPadding + TooltipTokens.TextExtraVerticalPadding
                            } else {
                                0.dp
                            },
                            bottom = if (tooltipTitle == null) {
                                caretBottomPadding + TooltipTokens.TextExtraVerticalPadding
                            } else {
                                caretBottomPadding
                            }
                        )
                )
            }
        }
    )
}

/**
 * Circular camera-overlay icon button that toggles custom tooltip content scoped to [TooltipScope]
 * when tapped.
 *
 * Shares the circular [size] container, [minTouchTargetSize] touch target, `1.dp` border at 20%
 * [contentColor] alpha when [isOutlined] is `true`, and pointer-down toggle tracking that prevents
 * an outside-touch dismissal from immediately reopening the popup on pointer-up. [onClick] fires on
 * every tap, whereas [onDismissRequest] fires once each time an open tooltip is dismissed.
 *
 * To render an edge-clamped bubble with a caret pointing at the anchor, hoist
 * `val caret = rememberJcaTooltipCaret(positioning)`, pass
 * `positionProvider = caret.positionProvider`, configure
 * `RichTooltip(shape = caret.bubbleShape(), caretShape = null)`, and apply
 * `Modifier.padding(caret.caretPadding)` to the inner content.
 *
 * @param icon icon composable centered inside the button container with [LocalContentColor] set to
 *   [contentColor].
 * @param tooltip custom popup content scoped to [TooltipScope] (such as a [RichTooltip] with action
 *   buttons or styled text).
 * @param contentDescription explicit accessibility label for the anchor button; unlike the `String`
 *   overload, no automatic fallback is available, so pass `null` only when semantics are supplied
 *   externally.
 * @param modifier [Modifier] applied to the outer [TooltipBox] wrapper.
 * @param state controls tooltip visibility; defaults to a persistent [TooltipState] so the popup
 *   remains open until tapped again or dismissed externally.
 * @param positionProvider computes popup coordinates relative to the anchor; defaults to
 *   `rememberJcaTooltipCaret().positionProvider`.
 * @param hasAction pass `true` when [tooltip] contains interactive controls so [TooltipBox] adjusts
 *   accessibility focus and dismissal handling accordingly.
 * @param isOutlined when `true`, draws a `1.dp` circular border around the [size] container using
 *   [contentColor] at 20% alpha.
 * @param containerColor background fill of the circular [size] button container.
 * @param contentColor tint provided via [LocalContentColor] to [icon] and used at 20% alpha for the
 *   [isOutlined] border.
 * @param size diameter of the visible circular button container inside the touch target.
 * @param minTouchTargetSize minimum interactive bounds and unbounded ripple diameter of the anchor.
 * @param onClick callback invoked on every anchor tap after toggling tooltip visibility.
 * @param onDismissRequest optional callback invoked once when an open tooltip is dismissed (via
 *   anchor re-tap or outside touch).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    icon: @Composable () -> Unit,
    tooltip: @Composable TooltipScope.() -> Unit,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    positionProvider: PopupPositionProvider = rememberJcaTooltipCaret().positionProvider,
    hasAction: Boolean = false,
    isOutlined: Boolean = false,
    containerColor: Color = Color.Transparent,
    contentColor: Color = Color.White,
    size: Dp = JcaTooltipDefaults.ContainerSize,
    minTouchTargetSize: Dp = JcaTooltipDefaults.TouchTargetSize,
    onClick: () -> Unit = {},
    onDismissRequest: (() -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val currentOnClick by rememberUpdatedState(onClick)
    val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
    var wasVisibleAtDown by remember { mutableStateOf(false) }

    val dismissTooltip = {
        val wasAlreadyDismissing = !state.transition.targetState
        state.dismiss()
        if (!wasAlreadyDismissing) {
            currentOnDismissRequest?.invoke()
        }
    }

    TooltipBox(
        positionProvider = positionProvider,
        tooltip = tooltip,
        state = state,
        modifier = modifier,
        onDismissRequest = dismissTooltip,
        enableUserInput = false,
        hasAction = hasAction
    ) {
        Box(
            modifier = Modifier
                .sizeIn(minWidth = minTouchTargetSize, minHeight = minTouchTargetSize)
                .testTag(TOOLTIP_ANCHOR_BUTTON_TAG)
                .pointerInput(state) {
                    awaitEachGesture {
                        awaitFirstDown(pass = PointerEventPass.Initial)
                        wasVisibleAtDown = state.isVisible
                        val up = waitForUpOrCancellation(pass = PointerEventPass.Initial)
                        val releasedInBounds = up != null &&
                            up.position.x in 0f..this.size.width.toFloat() &&
                            up.position.y in 0f..this.size.height.toFloat()
                        if (!releasedInBounds) {
                            wasVisibleAtDown = false
                        }
                    }
                }
                .clickable(
                    role = Role.Button,
                    onClickLabel = null,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = ripple(bounded = false, radius = minTouchTargetSize / 2)
                ) {
                    val wasVisible = wasVisibleAtDown || state.isVisible
                    wasVisibleAtDown = false
                    if (wasVisible) {
                        dismissTooltip()
                    } else {
                        scope.launch { state.show() }
                    }
                    currentOnClick()
                }
                .then(
                    if (contentDescription != null) {
                        Modifier.semantics {
                            this.contentDescription = contentDescription
                        }
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            val containerModifier = Modifier
                .size(size)
                .then(
                    if (isOutlined) {
                        Modifier.border(
                            border = BorderStroke(
                                TooltipTokens.BorderWidth,
                                contentColor.copy(alpha = TooltipTokens.BORDER_ALPHA)
                            ),
                            shape = CircleShape
                        )
                    } else {
                        Modifier
                    }
                )
                .background(color = containerColor, shape = CircleShape)

            Box(
                modifier = containerModifier,
                contentAlignment = Alignment.Center
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    icon()
                }
            }
        }
    }
}
