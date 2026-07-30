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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
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
import kotlinx.coroutines.delay

/**
 * A custom tooltip component that overlays the screen using a [Popup].
 * It should be placed inside a [Box] along with its anchor component.
 *
 * @param expanded Whether the tooltip is currently visible.
 * @param onDismissRequest Called when the user taps outside the tooltip.
 * @param modifier Modifier for the tooltip content.
 * @param beakStyle Configuration for the beak's size, alignment, and direction.
 * @param colors Colors for the tooltip container and content.
 * @param maxWidth The maximum width of the tooltip.
 * @param autoDismissDelayMillis The delay in milliseconds before the tooltip auto-dismisses. Use [TooltipDefaults.NoAutoDismiss] to disable.
 * @param content The content to display inside the tooltip (usually a Text composable).
 */
@Composable
fun Tooltip(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    beakStyle: BeakStyle = BeakStyle(),
    colors: TooltipColors = TooltipDefaults.tooltipColors(),
    maxWidth: Dp = TooltipDefaults.MaxWidth,
    autoDismissDelayMillis: Long = TooltipDefaults.NoAutoDismiss,
    content: @Composable () -> Unit
) {
    if (expanded) {
        if (autoDismissDelayMillis > 0) {
            val currentOnDismissRequest by rememberUpdatedState(onDismissRequest)
            LaunchedEffect(autoDismissDelayMillis) {
                delay(autoDismissDelayMillis)
                currentOnDismissRequest()
            }
        }
        val density = LocalDensity.current
        Popup(
            popupPositionProvider = remember(beakStyle, density) {
                TooltipPositionProvider(beakStyle, density)
            },
            onDismissRequest = onDismissRequest,
            properties = PopupProperties(focusable = true)
        ) {
            val slideDistance = with(density) { 12.dp.roundToPx() }
            val enterTransition = if (beakStyle.direction == BeakDirection.Up) {
                slideInVertically(initialOffsetY = { -slideDistance }) + fadeIn()
            } else {
                slideInVertically(initialOffsetY = { slideDistance }) + fadeIn()
            }

            AnimatedVisibility(
                visible = true,
                enter = enterTransition
            ) {
                TooltipContent(
                    beakStyle = beakStyle,
                    colors = colors,
                    modifier = modifier.widthIn(max = maxWidth),
                    content = content
                )
            }
        }
    }
}

/**
 * An icon button that shows a tooltip when clicked.
 *
 * @param icon The icon to display inside the button.
 * @param tooltipText The text to display inside the tooltip.
 * @param modifier Modifier for the button container.
 * @param beakStyle Configuration for the tooltip's beak.
 * @param colors Colors for the tooltip.
 * @param maxWidth The maximum width of the tooltip.
 * @param isOutlined Whether the icon button should be outlined.
 * @param autoDismissDelayMillis The delay in milliseconds before the tooltip auto-dismisses.
 * @param onClick Optional callback when the button is clicked.
 */
@Composable
fun TooltipIconButton(
    icon: @Composable () -> Unit,
    tooltipText: String,
    modifier: Modifier = Modifier,
    beakStyle: BeakStyle = BeakStyle(),
    colors: TooltipColors = TooltipDefaults.tooltipColors(),
    maxWidth: Dp = TooltipDefaults.MaxWidth,
    isOutlined: Boolean = false,
    autoDismissDelayMillis: Long = TooltipDefaults.NoAutoDismiss,
    onClick: () -> Unit = {}
) {
    var showTooltip by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        if (isOutlined) {
            OutlinedIconButton(
                onClick = {
                    showTooltip = true
                    onClick()
                },
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                icon()
            }
        } else {
            IconButton(onClick = {
                showTooltip = true
                onClick()
            }) {
                icon()
            }
        }
        Tooltip(
            expanded = showTooltip,
            onDismissRequest = { showTooltip = false },
            beakStyle = beakStyle,
            colors = colors,
            maxWidth = maxWidth,
            autoDismissDelayMillis = autoDismissDelayMillis
        ) {
            Text(
                text = tooltipText,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

/**
 * The visual content of the tooltip, including the custom shape.
 */
@Composable
private fun TooltipContent(
    beakStyle: BeakStyle,
    modifier: Modifier = Modifier,
    colors: TooltipColors = TooltipDefaults.tooltipColors(),
    content: @Composable () -> Unit
) {
    val shape = remember(beakStyle) { TooltipShape(beakStyle) }
    val containerColor = colors.containerColor.takeOrElse {
        MaterialTheme.colorScheme.secondaryFixed
    }
    val contentColor = colors.contentColor.takeOrElse {
        MaterialTheme.colorScheme.onSecondaryFixed
    }

    val topPadding = if (beakStyle.direction == BeakDirection.Up) {
        16.dp + beakStyle.height
    } else {
        16.dp
    }
    val bottomPadding = if (beakStyle.direction == BeakDirection.Down) {
        16.dp + beakStyle.height
    } else {
        16.dp
    }

    CompositionLocalProvider(LocalContentColor provides contentColor) {
        Box(
            modifier = modifier
                .background(color = containerColor, shape = shape)
                .padding(
                    start = 16.dp,
                    top = topPadding,
                    end = 16.dp,
                    bottom = bottomPadding
                ),
            contentAlignment = Alignment.Center
        ) {
            // We might need to adjust content color here if it's text
            content()
        }
    }
}

/**
 * Configuration for the tooltip's beak (arrow).
 *
 * @param width The width of the beak base.
 * @param height The height of the beak.
 * @param alignment Horizontal alignment of the beak relative to the tooltip body.
 * @param direction Direction the beak points (Up or Down).
 * @param offset Optional horizontal offset from the alignment position.
 */
data class BeakStyle(
    val width: Dp = 10.dp,
    val height: Dp = 8.dp,
    val alignment: BeakAlignment = BeakAlignment.End,
    val direction: BeakDirection = BeakDirection.Up,
    val offset: Dp = 0.dp
)

enum class BeakAlignment {
    Start,
    Center,
    End
}

enum class BeakDirection {
    Up,
    Down
}

data class TooltipColors(
    val containerColor: Color = Color.Unspecified,
    val contentColor: Color = Color.Unspecified
)

object TooltipDefaults {
    const val NoAutoDismiss = 0L
    val MaxWidth: Dp = 296.dp

    @Composable
    fun tooltipColors(
        containerColor: Color = Color.Unspecified,
        contentColor: Color = Color.Unspecified
    ): TooltipColors = TooltipColors(containerColor, contentColor)
}

/**
 * A custom [Shape] that draws a rounded rectangle with a beak.
 */
private class TooltipShape(
    private val beakStyle: BeakStyle
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val beakWidthPx = with(density) { beakStyle.width.toPx() }
        val beakHeightPx = with(density) { beakStyle.height.toPx() }
        val cornerRadiusPx = with(density) { 28.dp.toPx() } // Default 28dp matching Figma

        val bodyPath = Path().apply {
            val rect = if (beakStyle.direction == BeakDirection.Up) {
                Rect(0f, beakHeightPx, size.width, size.height)
            } else {
                Rect(0f, 0f, size.width, size.height - beakHeightPx)
            }
            addRoundRect(
                RoundRect(rect = rect, cornerRadius = CornerRadius(cornerRadiusPx))
            )
        }

        val beakPath = Path().apply {
            val yBase = if (beakStyle.direction == BeakDirection.Up) {
                beakHeightPx
            } else {
                size.height - beakHeightPx
            }
            val yTip = if (beakStyle.direction == BeakDirection.Up) {
                0f
            } else {
                size.height
            }

            val xCenter = when (beakStyle.alignment) {
                BeakAlignment.Start -> cornerRadiusPx + beakWidthPx / 2
                BeakAlignment.Center -> size.width / 2
                BeakAlignment.End -> size.width - cornerRadiusPx - beakWidthPx / 2
            } + with(density) { beakStyle.offset.toPx() }

            // Clamp beak position to flat edges
            val minX = cornerRadiusPx + beakWidthPx / 2
            val maxX = size.width - cornerRadiusPx - beakWidthPx / 2
            val clampedXCenter = xCenter.coerceIn(minX, maxX)

            val x1 = clampedXCenter - beakWidthPx / 2
            val x2 = clampedXCenter
            val x3 = clampedXCenter + beakWidthPx / 2

            moveTo(x1, yBase)
            lineTo(x2, yTip)
            lineTo(x3, yBase)
            close()
        }

        val combinedPath = Path.combine(
            operation = PathOperation.Union,
            path1 = bodyPath,
            path2 = beakPath
        )
        return Outline.Generic(combinedPath)
    }
}

/**
 * Positions the tooltip relative to the anchor.
 */
private class TooltipPositionProvider(
    private val beakStyle: BeakStyle,
    private val density: Density
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize
    ): IntOffset {
        val beakWidthPx = with(density) { beakStyle.width.toPx() }
        val beakHeightPx = with(density) { beakStyle.height.toPx() }
        val cornerRadiusPx = with(density) { 28.dp.toPx() }

        val y = if (beakStyle.direction == BeakDirection.Up) {
            anchorBounds.bottom
        } else {
            anchorBounds.top - popupContentSize.height
        }

        val beakTipX = when (beakStyle.alignment) {
            BeakAlignment.Start -> cornerRadiusPx + beakWidthPx / 2
            BeakAlignment.Center -> popupContentSize.width.toFloat() / 2
            BeakAlignment.End -> popupContentSize.width.toFloat() - cornerRadiusPx - beakWidthPx / 2
        } + with(density) { beakStyle.offset.toPx() }

        val minX = cornerRadiusPx + beakWidthPx / 2
        val maxX = popupContentSize.width.toFloat() - cornerRadiusPx - beakWidthPx / 2
        val clampedBeakTipX = beakTipX.coerceIn(minX, maxX)

        val anchorCenterX = anchorBounds.left + anchorBounds.width / 2
        val x = anchorCenterX - clampedBeakTipX.toInt()
        val clampedX = x.coerceIn(0, windowSize.width - popupContentSize.width)

        return IntOffset(clampedX, y)
    }
}



@Preview(showBackground = true, backgroundColor = 0)
@Composable
private fun TooltipVariousStylesPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            TooltipContent(
                beakStyle = BeakStyle(direction = BeakDirection.Up, alignment = BeakAlignment.Start)
            ) {
                Text("Beak Up - Start Alignment")
            }
            TooltipContent(
                beakStyle = BeakStyle(
                    direction = BeakDirection.Up,
                    alignment = BeakAlignment.Center
                )
            ) {
                Text("Beak Up - Center Alignment")
            }
            TooltipContent(
                beakStyle = BeakStyle(direction = BeakDirection.Up, alignment = BeakAlignment.End)
            ) {
                Text("Beak Up - End Alignment")
            }
            TooltipContent(
                beakStyle = BeakStyle(
                    direction = BeakDirection.Down,
                    alignment = BeakAlignment.Start
                )
            ) {
                Text("Beak Down - Start Alignment")
            }
            TooltipContent(
                beakStyle = BeakStyle(
                    direction = BeakDirection.Down,
                    alignment = BeakAlignment.Center
                )
            ) {
                Text("Beak Down - Center Alignment")
            }
            TooltipContent(
                beakStyle = BeakStyle(direction = BeakDirection.Down, alignment = BeakAlignment.End)
            ) {
                Text("Beak Down - End Alignment")
            }
        }
    }
}
