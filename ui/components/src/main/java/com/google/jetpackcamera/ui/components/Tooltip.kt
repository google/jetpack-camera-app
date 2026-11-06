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

const val TOOLTIP_ANCHOR_BUTTON_TAG = "tooltip_anchor_button"
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
    val CaretWidth: Dp get() = CaretSize.width
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

/**
 * State holder for dynamic caret coordinates computed synchronously during popup placement.
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
 * Shape that unions a rounded rectangle bubble body with a triangular caret pointing toward the
 * anchor.
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
        val bodyTop = if (isCaretOnTop) ch else 0f
        val actualRadius = minOf(r, bodyHeight / 2f, w / 2f).coerceAtLeast(0f)

        // Clamp caret base so the triangle never bleeds into the corner radius arcs
        val minCaretX = actualRadius + cw / 2f
        val maxCaretX = (w - actualRadius - cw / 2f).coerceAtLeast(minCaretX)
        val clampedCaretX = localCaretX.coerceIn(minCaretX, maxCaretX)

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
 * Position provider for the tooltip popup that clamps popup coordinates to safe screen margins and
 * derives the corner-clamped local caret offset relative to the popup's top-left corner before draw.
 */
@OptIn(ExperimentalMaterial3Api::class)
class JcaTooltipPositionProvider(
    val positioning: TooltipAnchorPosition,
    val spacing: Dp = JcaTooltipDefaults.SpacingBetweenTooltipAndAnchor,
    val screenMargin: Dp = JcaTooltipDefaults.ScreenMargin,
    val cornerRadius: Dp = JcaTooltipDefaults.CornerRadius,
    val caretWidth: Dp = JcaTooltipDefaults.CaretWidth,
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
        val cornerRadiusPx = with(density) { cornerRadius.toPx() }
        val caretWidthPx = with(density) { caretWidth.toPx() }
        val actualRadius = minOf(cornerRadiusPx, popupContentSize.width / 2f).coerceAtLeast(0f)
        val minCaretX = actualRadius + caretWidthPx / 2f
        val maxCaretX =
            (popupContentSize.width - actualRadius - caretWidthPx / 2f).coerceAtLeast(minCaretX)
        val localCaretX = (anchorCenterX - popupX).coerceIn(minCaretX, maxCaretX)

        onCaretCalculated(localCaretX, placeBelow)

        return IntOffset(popupX, popupY)
    }
}

/**
 * An icon button that toggles a styled [RichTooltip] when tapped.
 *
 * Uses Material 3's [TooltipBox] and [RichTooltip] for animations, focus, shadow, and state
 * management, paired with [JcaTooltipPositionProvider] and [JcaTooltipBubbleShape] for edge-aware
 * caret placement.
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
    val density = LocalDensity.current
    val caretPositionState = remember {
        CaretPositionState(
            initialIsCaretOnTop = positioning == TooltipAnchorPosition.Below
        )
    }
    val positionProvider = remember(positioning, density) {
        JcaTooltipPositionProvider(
            positioning = positioning,
            density = density,
            onCaretCalculated = { localCaretX, isCaretOnTop ->
                caretPositionState.localCaretX = localCaretX
                caretPositionState.isCaretOnTop = isCaretOnTop
            }
        )
    }

    TooltipIconButton(
        icon = icon,
        modifier = modifier,
        contentDescription = contentDescription ?: tooltipTitle ?: tooltipText,
        state = state,
        positionProvider = positionProvider,
        isOutlined = isOutlined,
        containerColor = containerColor,
        contentColor = contentColor,
        size = size,
        minTouchTargetSize = minTouchTargetSize,
        onClick = onClick,
        onDismissRequest = onDismissRequest,
        tooltip = {
            val isCaretOnTop = caretPositionState.isCaretOnTop
            val bubbleShape = remember(caretPositionState.localCaretX, isCaretOnTop) {
                JcaTooltipBubbleShape(
                    localCaretX = caretPositionState.localCaretX,
                    isCaretOnTop = isCaretOnTop
                )
            }
            val caretTopPadding = if (isCaretOnTop) JcaTooltipDefaults.CaretHeight else 0.dp
            val caretBottomPadding = if (!isCaretOnTop) JcaTooltipDefaults.CaretHeight else 0.dp

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
                shape = bubbleShape,
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
 * An icon button that toggles custom tooltip content scoped to [TooltipScope] when tapped.
 *
 * Allows callers to supply custom [RichTooltip] content (such as actions or styled text) while
 * sharing the camera overlay button styling and pointer-down toggle handling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    icon: @Composable () -> Unit,
    tooltip: @Composable TooltipScope.() -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    state: TooltipState = rememberTooltipState(isPersistent = true),
    positionProvider: PopupPositionProvider =
        TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below),
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

    TooltipBox(
        positionProvider = positionProvider,
        tooltip = tooltip,
        state = state,
        modifier = modifier,
        onDismissRequest = onDismissRequest?.let {
            {
                state.dismiss()
                currentOnDismissRequest?.invoke()
            }
        },
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
                        state.dismiss()
                        currentOnDismissRequest?.invoke()
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
                                Color.White.copy(alpha = TooltipTokens.BORDER_ALPHA)
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
