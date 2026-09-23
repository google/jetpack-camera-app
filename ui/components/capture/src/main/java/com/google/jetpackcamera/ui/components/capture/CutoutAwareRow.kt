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

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LookaheadScope
import androidx.compose.ui.layout.Measured
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * A horizontal row layout for top-bar status indicators that automatically flows items around any
 * intersecting display cutout (such as a corner or centered camera hole-punch).
 *
 * Unlike applying a uniform top inset for the full status bar or cutout height, this layout allows
 * the indicator row to occupy the status bar region (`y = 0`) while shifting individual items
 * horizontally past any cutout bounding rectangles that overlap the row's vertical bounds.
 *
 * This layout wraps its measurement in a [LookaheadScope] and provides a [RowScope] receiver so
 * that [androidx.compose.animation.AnimatedVisibility] children use horizontal expand/shrink
 * transitions without jumping across a cutout mid-animation:
 * - During the lookahead pass (`isLookingAhead == true`), children are measured at their target
 *   widths to determine whether their final resting position lies past a cutout (`lockedMinX`).
 * - During the approach pass (`isLookingAhead == false`), entering and exiting children remain
 *   anchored past any cutout their target or prior visible state cleared, preventing teleportation
 *   while their animated width grows from or shrinks to `0`.
 *
 * @param modifier the [Modifier] to be applied to this row.
 * @param horizontalSpacing horizontal gap between adjacent visible items.
 * @param cutoutClearance additional horizontal padding kept clear on both sides of a display cutout.
 * @param verticalAlignment vertical alignment of children within the row.
 * @param cutoutRectsOverride optional list of cutout bounding rectangles in the row's local
 *   coordinate space, used for deterministic testing. When `null`, cutout rects are queried from
 *   the host window's [android.view.DisplayCutout] and mapped into the row's local coordinates.
 * @param content the row children, scoped to [RowScope].
 */
@Composable
fun CutoutAwareRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    cutoutClearance: Dp = 8.dp,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    cutoutRectsOverride: List<IntRect>? = null,
    content: @Composable RowScope.() -> Unit
) {
    val view = LocalView.current
    val cutoutInsets = WindowInsets.displayCutout
    var rowBoundsInWindow by remember { mutableStateOf<IntRect?>(null) }
    val lockedMinX = remember { mutableMapOf<Int, Int>() }

    LookaheadScope {
        Layout(
            content = { CutoutRowScope.content() },
            modifier = modifier.onGloballyPositioned { coordinates ->
                val bounds = coordinates.boundsInWindow()
                val intRect = IntRect(
                    left = bounds.left.roundToInt(),
                    top = bounds.top.roundToInt(),
                    right = bounds.right.roundToInt(),
                    bottom = bounds.bottom.roundToInt()
                )
                if (rowBoundsInWindow != intRect) {
                    rowBoundsInWindow = intRect
                }
            }
        ) { measurables, constraints ->
            // Read WindowInsets.displayCutout inside measure to subscribe to inset updates.
            val unusedInsetObservation =
                cutoutInsets.getTop(this) +
                    cutoutInsets.getLeft(this, layoutDirection) +
                    cutoutInsets.getRight(this, layoutDirection)

            @Suppress("UNUSED_VARIABLE")
            val observed = unusedInsetObservation

            val spacingPx = horizontalSpacing.roundToPx()
            val clearancePx = cutoutClearance.roundToPx()

            val childConstraints = Constraints(
                minWidth = 0,
                maxWidth = Constraints.Infinity,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
            val placeables = measurables.map { it.measure(childConstraints) }

            val maxChildHeight = placeables.maxOfOrNull { it.height } ?: 0
            val rowHeight = max(maxChildHeight, constraints.minHeight)
                .coerceAtMost(constraints.maxHeight)

            // Resolve cutout rects in the row's local coordinate space.
            val localCutoutRects: List<IntRect> = if (cutoutRectsOverride != null) {
                cutoutRectsOverride
            } else {
                val windowCutoutRects =
                    ViewCompat.getRootWindowInsets(view)?.displayCutout?.boundingRects
                        ?: view.rootWindowInsets?.displayCutout?.boundingRects
                        ?: emptyList()
                val bounds = rowBoundsInWindow
                val offsetX = bounds?.left ?: 0
                val offsetY = bounds?.top ?: 0
                windowCutoutRects.map { rect ->
                    IntRect(
                        left = rect.left - offsetX,
                        top = rect.top - offsetY,
                        right = rect.right - offsetX,
                        bottom = rect.bottom - offsetY
                    )
                }
            }

            // Convert vertically overlapping cutout rects into sorted horizontal keep-out ranges.
            val horizontalCutouts = localCutoutRects
                .filter { rect ->
                    rect.width > 0 && rect.height > 0 && rect.bottom > 0 && rect.top < rowHeight
                }
                .map { rect ->
                    IntRange(
                        start = (rect.left - clearancePx).coerceAtLeast(0),
                        endInclusive = rect.right + clearancePx
                    )
                }
                .sortedBy { it.first }

            val xPositions = IntArray(placeables.size)
            val isActive = BooleanArray(placeables.size)
            var cursor = 0
            var maxRight = 0

            for (i in placeables.indices) {
                val placeable = placeables[i]
                // Query maxIntrinsicWidth so children wrapped in AnimatedVisibility
                // (whose ExpandShrinkModifier passes intrinsics through to the unclipped child)
                // evaluate cutout collisions against their full target width throughout
                // both enter (expandHorizontally) and exit (shrinkHorizontally) animations,
                // including the initial 0-width frame of an enter transition.
                val collisionWidth = max(
                    placeable.width,
                    measurables[i].maxIntrinsicWidth(constraints.maxHeight)
                )
                if (collisionWidth <= 0) {
                    xPositions[i] = cursor
                    if (!isLookingAhead) {
                        lockedMinX.remove(i)
                    }
                    continue
                }
                isActive[i] = true

                if (!isLookingAhead) {
                    val minX = lockedMinX[i] ?: 0
                    if (cursor < minX) {
                        cursor = minX
                    }
                }

                for (cut in horizontalCutouts) {
                    if (cursor + collisionWidth > cut.first && cursor < cut.last) {
                        cursor = max(cursor, cut.last)
                    }
                }

                if (isLookingAhead) {
                    val requiredMinX = horizontalCutouts
                        .filter { cursor >= it.last }
                        .maxOfOrNull { it.last } ?: 0
                    lockedMinX[i] = requiredMinX
                }

                xPositions[i] = cursor
                maxRight = max(maxRight, cursor + placeable.width)
                if (placeable.width > 0) {
                    cursor += placeable.width + spacingPx
                }
            }

            val rowWidth = max(maxRight, constraints.minWidth)
                .coerceAtMost(constraints.maxWidth)

            layout(rowWidth, rowHeight) {
                for (i in placeables.indices) {
                    if (!isActive[i]) continue
                    val placeable = placeables[i]
                    val y = verticalAlignment.align(placeable.height, rowHeight)
                    placeable.placeRelative(xPositions[i], y)
                }
            }
        }
    }
}

private object CutoutRowScope : RowScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier = this
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = this
    override fun Modifier.alignByBaseline(): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}
