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

import android.annotation.SuppressLint
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
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
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import com.google.jetpackcamera.core.common.ignoreResult
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
 * When [pillHorizontalPadding] is positive, contiguous active items that do not cross a cutout are
 * grouped into a shared pill segment with [pillHorizontalPadding] at the start and end of each
 * segment; if a cutout falls between active items, the pill is split into separate segments on
 * each side of the cutout. When no items are visible, no pill segments are drawn.
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
 * @param horizontalSpacing horizontal gap between adjacent visible items (or between split pills).
 * @param cutoutClearance additional horizontal padding kept clear on both sides of a display cutout.
 * @param pillHorizontalPadding horizontal padding applied at the start and end of each connected
 *   pill segment.
 * @param pillMinHeight minimum height of each connected pill segment when at least one child is
 *   visible.
 * @param pillBackgroundColor background color drawn for each connected pill segment, or
 *   [Color.Unspecified] to disable pill background drawing.
 * @param verticalAlignment vertical alignment of children within the row.
 * @param cutoutRectsOverride optional list of cutout bounding rectangles in the row's local
 *   coordinate space, used for deterministic testing. When `null`, cutout rects are queried from
 *   the host window's [android.view.DisplayCutout] and mapped into the row's local coordinates.
 * @param onPillSegmentsPlaced optional callback invoked with the physical local [IntRect] bounds of
 *   each active pill segment after layout, used for testing.
 * @param content the row children, scoped to [RowScope].
 */
@SuppressLint("AvoidNullableCollections", "NullableCollection")
@Composable
fun CutoutAwareRow(
    modifier: Modifier = Modifier,
    horizontalSpacing: Dp = 8.dp,
    cutoutClearance: Dp = 8.dp,
    pillHorizontalPadding: Dp = 0.dp,
    pillMinHeight: Dp = 0.dp,
    pillBackgroundColor: Color = Color.Unspecified,
    verticalAlignment: Alignment.Vertical = Alignment.CenterVertically,
    cutoutRectsOverride: List<IntRect>? = null,
    onPillSegmentsPlaced: ((List<IntRect>) -> Unit)? = null,
    content: @Composable RowScope.() -> Unit
) {
    val view = LocalView.current
    val cutoutInsets = WindowInsets.displayCutout
    var rowBoundsInWindow by remember { mutableStateOf<IntRect?>(null) }
    val lockedMinX = remember { mutableMapOf<Int, Int>() }
    var pillSegments by remember { mutableStateOf<List<IntRect>>(emptyList()) }

    LookaheadScope {
        Layout(
            content = { CutoutRowScope.content() },
            modifier = modifier
                .onGloballyPositioned { coordinates ->
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
                .drawBehind {
                    if (pillBackgroundColor != Color.Unspecified) {
                        for (segment in pillSegments) {
                            if (segment.width > 0 && segment.height > 0) {
                                val radius = segment.height / 2f
                                drawRoundRect(
                                    color = pillBackgroundColor,
                                    topLeft = Offset(
                                        segment.left.toFloat(),
                                        segment.top.toFloat()
                                    ),
                                    size = Size(
                                        segment.width.toFloat(),
                                        segment.height.toFloat()
                                    ),
                                    cornerRadius = CornerRadius(radius, radius)
                                )
                            }
                        }
                    }
                }
        ) { measurables, constraints ->
            // Read WindowInsets.displayCutout inside measure to subscribe to inset updates.
            cutoutInsets.getTop(this).ignoreResult()
            cutoutInsets.getLeft(this, layoutDirection).ignoreResult()
            cutoutInsets.getRight(this, layoutDirection).ignoreResult()

            val spacingPx = horizontalSpacing.roundToPx()
            val clearancePx = cutoutClearance.roundToPx()
            val pillPadPx = pillHorizontalPadding.roundToPx()
            val pillMinHeightPx = pillMinHeight.roundToPx()

            val childConstraints = Constraints(
                minWidth = 0,
                maxWidth = Constraints.Infinity,
                minHeight = 0,
                maxHeight = constraints.maxHeight
            )
            val placeables = measurables.map { it.measure(childConstraints) }

            val maxChildHeight = placeables.maxOfOrNull { it.height } ?: 0
            val hasAnyVisibleChild = placeables.any { it.width > 0 }
            val effectiveMinHeight = if (hasAnyVisibleChild) {
                max(constraints.minHeight, pillMinHeightPx)
            } else {
                constraints.minHeight
            }
            val rowHeight = max(maxChildHeight, effectiveMinHeight)
                .coerceAtMost(constraints.maxHeight)

            // Resolve cutout rects in the row's start-relative coordinate space so that
            // xPositions align with placeRelative in both LTR and RTL layouts.
            val isRtl = layoutDirection == LayoutDirection.Rtl
            val rtlReferenceWidth = if (constraints.hasBoundedWidth) {
                constraints.maxWidth
            } else {
                placeables.sumOf { it.width }
            }
            val localCutoutRects = resolveLocalCutoutRects(
                cutoutRectsOverride = cutoutRectsOverride,
                windowCutoutRects = ViewCompat.getRootWindowInsets(view)
                    ?.displayCutout?.boundingRects.orEmpty(),
                rowBoundsInWindow = rowBoundsInWindow,
                isRtl = isRtl,
                rtlReferenceWidth = rtlReferenceWidth
            )

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
            val startRelativePills = mutableListOf<IntRect>()

            var collisionCursor = 0
            var placeCursor = 0
            var inOpenGroup = false
            var currentGroupStartPx = 0
            var currentGroupVisibleRightPx = -1
            var currentGroupMaxHeightPx = 0
            var maxRight = 0

            fun closeCurrentGroup() {
                if (inOpenGroup) {
                    if (currentGroupVisibleRightPx >= 0 && pillPadPx > 0) {
                        val groupEndPx = currentGroupVisibleRightPx + pillPadPx
                        val pillHeight = max(currentGroupMaxHeightPx, pillMinHeightPx)
                            .coerceAtMost(rowHeight)
                        val pillTop = verticalAlignment.align(pillHeight, rowHeight)
                        startRelativePills.add(
                            IntRect(
                                left = currentGroupStartPx,
                                top = pillTop,
                                right = groupEndPx,
                                bottom = pillTop + pillHeight
                            )
                        )
                        maxRight = max(maxRight, groupEndPx)
                    }
                    collisionCursor += pillPadPx
                    inOpenGroup = false
                    currentGroupVisibleRightPx = -1
                    currentGroupMaxHeightPx = 0
                }
            }

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
                    xPositions[i] = placeCursor
                    if (!isLookingAhead) {
                        lockedMinX.remove(i)
                    }
                    continue
                }
                isActive[i] = true

                val minX = if (!isLookingAhead) (lockedMinX[i] ?: 0) else 0

                if (inOpenGroup) {
                    val candidateCollisionX = collisionCursor + spacingPx
                    val collidesWithCutout = horizontalCutouts.any { cut ->
                        candidateCollisionX + collisionWidth + pillPadPx > cut.first &&
                            collisionCursor < cut.last
                    }
                    if (candidateCollisionX < minX || collidesWithCutout) {
                        closeCurrentGroup()
                    }
                }

                if (!inOpenGroup) {
                    var groupStart = if (collisionCursor > 0) collisionCursor + spacingPx else 0
                    if (!isLookingAhead && minX > 0) {
                        groupStart = max(groupStart, minX - pillPadPx)
                    }
                    val neededGroupWidth = collisionWidth + 2 * pillPadPx
                    for (cut in horizontalCutouts) {
                        if (groupStart + neededGroupWidth > cut.first && groupStart < cut.last) {
                            groupStart = max(groupStart, cut.last)
                        }
                    }
                    inOpenGroup = true
                    currentGroupStartPx = groupStart
                    currentGroupVisibleRightPx = -1
                    currentGroupMaxHeightPx = 0
                    collisionCursor = groupStart + pillPadPx
                    placeCursor = groupStart + pillPadPx
                } else {
                    collisionCursor += spacingPx
                }

                if (isLookingAhead) {
                    val groupStartForCutout = currentGroupStartPx
                    val requiredMinX = horizontalCutouts
                        .filter { groupStartForCutout >= it.last }
                        .maxOfOrNull { it.last + pillPadPx } ?: 0
                    lockedMinX[i] = requiredMinX
                }

                collisionCursor += collisionWidth
                xPositions[i] = placeCursor
                if (placeable.width > 0) {
                    val itemRight = placeCursor + placeable.width
                    currentGroupVisibleRightPx = itemRight
                    currentGroupMaxHeightPx = max(currentGroupMaxHeightPx, placeable.height)
                    maxRight = max(maxRight, itemRight)
                    placeCursor = itemRight + spacingPx
                }
            }
            closeCurrentGroup()

            val rowWidth = if (isRtl && constraints.hasBoundedWidth) {
                max(maxRight, constraints.maxWidth)
            } else {
                max(maxRight, constraints.minWidth).coerceAtMost(constraints.maxWidth)
            }

            val resolvedPillSegments = if (isRtl) {
                startRelativePills.map { rect ->
                    IntRect(
                        left = rowWidth - rect.right,
                        top = rect.top,
                        right = rowWidth - rect.left,
                        bottom = rect.bottom
                    )
                }
            } else {
                startRelativePills
            }

            layout(rowWidth, rowHeight) {
                if (!isLookingAhead) {
                    if (pillSegments != resolvedPillSegments) {
                        pillSegments = resolvedPillSegments
                    }
                    onPillSegmentsPlaced?.invoke(resolvedPillSegments)
                }
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

@SuppressLint("AvoidNullableCollections", "NullableCollection")
internal fun resolveLocalCutoutRects(
    cutoutRectsOverride: List<IntRect>?,
    windowCutoutRects: List<android.graphics.Rect>,
    rowBoundsInWindow: IntRect?,
    isRtl: Boolean,
    rtlReferenceWidth: Int
): List<IntRect> {
    val ltrRects = cutoutRectsOverride ?: run {
        val offsetX = rowBoundsInWindow?.left ?: 0
        val offsetY = rowBoundsInWindow?.top ?: 0
        windowCutoutRects.map { rect ->
            IntRect(
                left = rect.left - offsetX,
                top = rect.top - offsetY,
                right = rect.right - offsetX,
                bottom = rect.bottom - offsetY
            )
        }
    }
    return if (isRtl) {
        val startEdgeX = if (cutoutRectsOverride == null && rowBoundsInWindow != null) {
            rowBoundsInWindow.width
        } else {
            rtlReferenceWidth
        }
        ltrRects.map { rect ->
            IntRect(
                left = startEdgeX - rect.right,
                top = rect.top,
                right = startEdgeX - rect.left,
                bottom = rect.bottom
            )
        }
    } else {
        ltrRects
    }
}

private object CutoutRowScope : RowScope {
    override fun Modifier.weight(weight: Float, fill: Boolean): Modifier = this
    override fun Modifier.align(alignment: Alignment.Vertical): Modifier = this
    override fun Modifier.alignBy(alignmentLine: HorizontalAlignmentLine): Modifier = this
    override fun Modifier.alignByBaseline(): Modifier = this
    override fun Modifier.alignBy(alignmentLineBlock: (Measured) -> Int): Modifier = this
}
